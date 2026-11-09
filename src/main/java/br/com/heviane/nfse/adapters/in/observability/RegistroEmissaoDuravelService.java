package br.com.heviane.nfse.adapters.in.observability;

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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serviço de persistência mínima durável em disco para conciliação fiscal e recuperação de chaves de acesso.
 * <p>
 * Mantém um arquivo append-only (JSON Lines) no disco do servidor com o histórico das NFS-e emitidas.
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

    @Autowired
    public RegistroEmissaoDuravelService(
            @Value("${nfse.audit.dir:./logs}") String diretorioAudit,
            @Value("${nfse.audit.file-enabled:true}") boolean habilitado
    ) {
        this(Paths.get(diretorioAudit != null && !diretorioAudit.isBlank() ? diretorioAudit.trim() : "./logs")
                .resolve(NOME_ARQUIVO_HISTORICO), habilitado);
    }

    public RegistroEmissaoDuravelService(Path caminhoArquivo, boolean habilitado) {
        this.caminhoArquivo = caminhoArquivo;
        this.habilitado = habilitado;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        inicializarArquivo();
    }

    public RegistroEmissaoDuravelService(Path caminhoArquivo) {
        this(caminhoArquivo, true);
    }

    private void inicializarArquivo() {
        if (!habilitado) return;
        try {
            Path parent = caminhoArquivo.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(caminhoArquivo)) {
                Files.createFile(caminhoArquivo);
            }
        } catch (IOException e) {
            log.warn("Não foi possível inicializar o arquivo de histórico durável em {}: {}", caminhoArquivo, e.getMessage());
        }
    }

    /**
     * Salva o registro de emissão de forma atômica e thread-safe no arquivo JSONL.
     */
    public void salvar(RegistroEmissaoAuditoria registro) {
        if (!habilitado || registro == null) return;

        lock.lock();
        try {
            inicializarArquivo();
            String jsonLine = objectMapper.writeValueAsString(registro) + System.lineSeparator();
            Files.writeString(
                    caminhoArquivo,
                    jsonLine,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.debug("Registro de emissão persistido localmente para DPS {}", registro.idDps());
        } catch (Exception e) {
            log.error("Falha ao salvar registro durável da DPS {} em {}: {}", registro.idDps(), caminhoArquivo, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Busca a última emissão registrada para o identificador da DPS especificado.
     */
    public Optional<RegistroEmissaoAuditoria> buscarPorIdDps(String idDps) {
        if (!habilitado || idDps == null || idDps.isBlank()) {
            return Optional.empty();
        }

        String termo = idDps.trim();
        List<RegistroEmissaoAuditoria> correspondencias = buscarPorCriterio(r -> termo.equalsIgnoreCase(r.idDps()));
        return correspondencias.isEmpty() ? Optional.empty() : Optional.of(correspondencias.get(correspondencias.size() - 1));
    }

    /**
     * Busca o registro da emissão pela Chave de Acesso Nacional de 50 dígitos.
     */
    public Optional<RegistroEmissaoAuditoria> buscarPorChave(String chaveAcesso) {
        if (!habilitado || chaveAcesso == null || chaveAcesso.isBlank()) {
            return Optional.empty();
        }

        String chave = chaveAcesso.trim();
        List<RegistroEmissaoAuditoria> correspondencias = buscarPorCriterio(r -> chave.equals(r.chaveAcesso()));
        return correspondencias.isEmpty() ? Optional.empty() : Optional.of(correspondencias.get(correspondencias.size() - 1));
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

    public Path getCaminhoArquivo() {
        return caminhoArquivo;
    }
}
