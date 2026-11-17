package br.com.heviane.nfse.adapters.in.observability;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serviço de persistência mínima durável em disco para conciliação fiscal e recuperação de chaves de acesso.
 * <p>
 * Mantém um arquivo append-only (JSON Lines) no disco do servidor com o histórico das NFS-e emitidas
 * e um índice concorrente em memória para consultas O(1) de alta performance sem retenção de locks de arquivo.
 * Caso o ERP cliente perca a resposta da requisição por oscilação de rede, permite recuperar a Chave de Acesso
 * e o protocolo associados ao {@code Id} da DPS localmente, sem necessidade imediata de nova chamada ao Sefin.
 */
@Service
public class RegistroEmissaoDuravelService {

    private static final Logger log = LoggerFactory.getLogger(RegistroEmissaoDuravelService.class);
    private static final String NOME_ARQUIVO_HISTORICO = "registro-emissoes.jsonl";

    private final Path caminhoArquivo;
    private final boolean habilitado;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();

    private final ConcurrentHashMap<String, RegistroEmissaoAuditoria> indicePorIdDps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RegistroEmissaoAuditoria> indicePorChave = new ConcurrentHashMap<>();
    private final AtomicLong contadorFalhasGravacao = new AtomicLong(0);

    @Autowired(required = false)
    private NfseMetrics nfseMetrics;

    @Autowired
    public RegistroEmissaoDuravelService(
            @Value("${nfse.audit.dir:./logs}") String diretorioAudit,
            @Value("${nfse.audit.file-enabled:true}") boolean habilitado,
            @Autowired(required = false) NfseMetrics nfseMetrics
    ) {
        this(Paths.get(diretorioAudit != null && !diretorioAudit.isBlank() ? diretorioAudit.trim() : "./logs")
                .resolve(NOME_ARQUIVO_HISTORICO), habilitado, nfseMetrics);
    }

    public RegistroEmissaoDuravelService(Path caminhoArquivo, boolean habilitado, NfseMetrics nfseMetrics) {
        this.caminhoArquivo = caminhoArquivo;
        this.habilitado = habilitado;
        this.nfseMetrics = nfseMetrics;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        inicializarArquivoEIndice();
    }

    public RegistroEmissaoDuravelService(Path caminhoArquivo, boolean habilitado) {
        this(caminhoArquivo, habilitado, null);
    }

    public RegistroEmissaoDuravelService(Path caminhoArquivo) {
        this(caminhoArquivo, true, null);
    }

    private void inicializarArquivoEIndice() {
        if (!habilitado) return;
        try {
            Path parent = caminhoArquivo.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(caminhoArquivo)) {
                Files.createFile(caminhoArquivo);
            } else {
                carregarIndiceDoDisco();
            }
        } catch (IOException e) {
            log.warn("Não foi possível inicializar o arquivo de histórico durável em {}: {}", caminhoArquivo, e.getMessage());
        }
    }

    private void carregarIndiceDoDisco() {
        try (BufferedReader reader = Files.newBufferedReader(caminhoArquivo, StandardCharsets.UTF_8)) {
            String linha;
            int count = 0;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank()) {
                    try {
                        RegistroEmissaoAuditoria reg = objectMapper.readValue(linha, RegistroEmissaoAuditoria.class);
                        atualizarIndiceEmMemoria(reg);
                        count++;
                    } catch (Exception e) {
                        log.debug("Linha ignorada no carregamento do índice durável: {}", e.getMessage());
                    }
                }
            }
            log.info("Índice de registros duráveis carregado com {} emissões a partir de {}", count, caminhoArquivo);
        } catch (IOException e) {
            log.warn("Erro ao carregar índice de registros duráveis de {}: {}", caminhoArquivo, e.getMessage());
        }
    }

    private void atualizarIndiceEmMemoria(RegistroEmissaoAuditoria reg) {
        if (reg == null) return;
        if (reg.idDps() != null && !reg.idDps().isBlank()) {
            indicePorIdDps.put(reg.idDps().trim().toUpperCase(), reg);
        }
        if (reg.chaveAcesso() != null && !reg.chaveAcesso().isBlank()) {
            indicePorChave.put(reg.chaveAcesso().trim(), reg);
        }
    }

    /**
     * Salva o registro de emissão de forma atômica e thread-safe no arquivo JSONL
     * e atualiza imediatamente o índice em memória.
     */
    public void salvar(RegistroEmissaoAuditoria registro) {
        if (!habilitado || registro == null) return;

        atualizarIndiceEmMemoria(registro);

        lock.lock();
        try {
            Path parent = caminhoArquivo.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(caminhoArquivo)) {
                Files.createFile(caminhoArquivo);
            }
            String jsonLine = objectMapper.writeValueAsString(registro) + System.lineSeparator();
            Files.writeString(
                    caminhoArquivo,
                    jsonLine,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.debug("Registro de emissão persistido localmente para DPS {} (situação: {})", registro.idDps(), registro.situacao());
        } catch (Exception e) {
            contadorFalhasGravacao.incrementAndGet();
            if (nfseMetrics != null) {
                nfseMetrics.registrarFalhaGravacaoAuditoria();
            }
            log.error("Falha ao salvar registro durável da DPS {} em {}: {}", registro.idDps(), caminhoArquivo, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Busca a última emissão registrada para o identificador da DPS especificado.
     * Utiliza primariamente o índice em memória O(1) sem concorrência de lock com gravações.
     */
    public Optional<RegistroEmissaoAuditoria> buscarPorIdDps(String idDps) {
        if (!habilitado || idDps == null || idDps.isBlank()) {
            return Optional.empty();
        }

        RegistroEmissaoAuditoria emMemoria = indicePorIdDps.get(idDps.trim().toUpperCase());
        if (emMemoria != null) {
            return Optional.of(emMemoria);
        }

        String termo = idDps.trim();
        List<RegistroEmissaoAuditoria> correspondencias = buscarPorCriterio(r -> termo.equalsIgnoreCase(r.idDps()));
        if (!correspondencias.isEmpty()) {
            RegistroEmissaoAuditoria ultimo = correspondencias.get(correspondencias.size() - 1);
            atualizarIndiceEmMemoria(ultimo);
            return Optional.of(ultimo);
        }
        return Optional.empty();
    }

    /**
     * Busca o registro da emissão pela Chave de Acesso Nacional de 50 dígitos.
     * Utiliza primariamente o índice em memória O(1) sem concorrência de lock com gravações.
     */
    public Optional<RegistroEmissaoAuditoria> buscarPorChave(String chaveAcesso) {
        if (!habilitado || chaveAcesso == null || chaveAcesso.isBlank()) {
            return Optional.empty();
        }

        RegistroEmissaoAuditoria emMemoria = indicePorChave.get(chaveAcesso.trim());
        if (emMemoria != null) {
            return Optional.of(emMemoria);
        }

        String chave = chaveAcesso.trim();
        List<RegistroEmissaoAuditoria> correspondencias = buscarPorCriterio(r -> chave.equals(r.chaveAcesso()));
        if (!correspondencias.isEmpty()) {
            RegistroEmissaoAuditoria ultimo = correspondencias.get(correspondencias.size() - 1);
            atualizarIndiceEmMemoria(ultimo);
            return Optional.of(ultimo);
        }
        return Optional.empty();
    }

    /**
     * Retorna os últimos N registros de emissão salvos.
     */
    public List<RegistroEmissaoAuditoria> listarUltimas(int limite) {
        if (!habilitado || !Files.exists(caminhoArquivo)) {
            return Collections.emptyList();
        }

        lock.lock();
        try (BufferedReader reader = Files.newBufferedReader(caminhoArquivo, StandardCharsets.UTF_8)) {
            List<RegistroEmissaoAuditoria> lista = new ArrayList<>();
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank()) {
                    try {
                        lista.add(objectMapper.readValue(linha, RegistroEmissaoAuditoria.class));
                    } catch (Exception ignored) {
                    }
                }
            }

            if (lista.size() <= limite) {
                return Collections.unmodifiableList(lista);
            }
            return Collections.unmodifiableList(lista.subList(lista.size() - limite, lista.size()));
        } catch (IOException e) {
            log.warn("Erro ao listar últimas emissões de {}: {}", caminhoArquivo, e.getMessage());
            return Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    private List<RegistroEmissaoAuditoria> buscarPorCriterio(java.util.function.Predicate<RegistroEmissaoAuditoria> filtro) {
        if (!Files.exists(caminhoArquivo)) {
            return Collections.emptyList();
        }

        lock.lock();
        try (BufferedReader reader = Files.newBufferedReader(caminhoArquivo, StandardCharsets.UTF_8)) {
            List<RegistroEmissaoAuditoria> resultado = new ArrayList<>();
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank()) {
                    try {
                        RegistroEmissaoAuditoria reg = objectMapper.readValue(linha, RegistroEmissaoAuditoria.class);
                        if (filtro.test(reg)) {
                            resultado.add(reg);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return resultado;
        } catch (IOException e) {
            log.warn("Erro ao ler registros duráveis de {}: {}", caminhoArquivo, e.getMessage());
            return Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    public long getContadorFalhasGravacao() {
        return contadorFalhasGravacao.get();
    }

    public Path getCaminhoArquivo() {
        return caminhoArquivo;
    }
}
