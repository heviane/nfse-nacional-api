package br.com.heviane.nfse.adapters.in.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroEmissaoDuravelServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve salvar registro e recuperar por Id da DPS")
    void deveSalvarERecuperarPorIdDps() {
        Path arquivo = tempDir.resolve("registro-emissoes.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, true);

        RegistroEmissaoAuditoria registro = new RegistroEmissaoAuditoria(
                Instant.now(),
                "DPS32052002601000000000019100001000000000000001",
                "32052002601000000000019100001000000000000001123456",
                "202611090001",
                100,
                "00.***.***/0001-**",
                "HOMOLOGACAO",
                "REST",
                "corr-12345"
        );

        service.salvar(registro);

        Optional<RegistroEmissaoAuditoria> opt = service.buscarPorIdDps("DPS32052002601000000000019100001000000000000001");
        assertThat(opt).isPresent();
        assertThat(opt.get().chaveAcesso()).isEqualTo("32052002601000000000019100001000000000000001123456");
        assertThat(opt.get().numeroProtocolo()).isEqualTo("202611090001");
        assertThat(opt.get().statusFiscal()).isEqualTo(100);
        assertThat(opt.get().origem()).isEqualTo("REST");
    }

    @Test
    @DisplayName("Deve buscar registro por Chave de Acesso de 50 dígitos")
    void deveBuscarPorChaveAcesso() {
        Path arquivo = tempDir.resolve("registro-emissoes.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, true);

        RegistroEmissaoAuditoria registro = new RegistroEmissaoAuditoria(
                Instant.now(),
                "DPS123",
                "32052002601000000000019100001000000000000001123456",
                "PROT-999",
                100,
                "00.***.***/0001-**",
                "HOMOLOGACAO",
                "SOAP",
                "corr-soap"
        );

        service.salvar(registro);

        Optional<RegistroEmissaoAuditoria> opt = service.buscarPorChave("32052002601000000000019100001000000000000001123456");
        assertThat(opt).isPresent();
        assertThat(opt.get().idDps()).isEqualTo("DPS123");
        assertThat(opt.get().origem()).isEqualTo("SOAP");
    }

    @Test
    @DisplayName("Deve listar últimas emissões respeitando o limite solicitado")
    void deveListarUltimasEmissoesComLimite() {
        Path arquivo = tempDir.resolve("registro-emissoes.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, true);

        service.salvar(new RegistroEmissaoAuditoria(Instant.now(), "DPS1", "CHAVE1", "PROT1", 100, "CNPJ1", "HOMOLOGACAO", "REST", "c1"));
        service.salvar(new RegistroEmissaoAuditoria(Instant.now(), "DPS2", "CHAVE2", "PROT2", 100, "CNPJ2", "HOMOLOGACAO", "REST", "c2"));
        service.salvar(new RegistroEmissaoAuditoria(Instant.now(), "DPS3", "CHAVE3", "PROT3", 100, "CNPJ3", "HOMOLOGACAO", "REST", "c3"));

        List<RegistroEmissaoAuditoria> ultimas = service.listarUltimas(2);
        assertThat(ultimas).hasSize(2);
        assertThat(ultimas.get(0).idDps()).isEqualTo("DPS2");
        assertThat(ultimas.get(1).idDps()).isEqualTo("DPS3");
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando o registro não existir ou parâmetros forem nulos")
    void deveRetornarVazioQuandoNaoEncontrado() {
        Path arquivo = tempDir.resolve("registro-emissoes.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, true);

        assertThat(service.buscarPorIdDps("DPS_INEXISTENTE")).isEmpty();
        assertThat(service.buscarPorIdDps(null)).isEmpty();
        assertThat(service.buscarPorIdDps("   ")).isEmpty();
        assertThat(service.buscarPorChave(null)).isEmpty();
    }

    @Test
    @DisplayName("Não deve persistir registros quando o serviço estiver desabilitado")
    void naoDeveSalvarQuandoDesabilitado() {
        Path arquivo = tempDir.resolve("desabilitado.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, false);

        service.salvar(new RegistroEmissaoAuditoria(Instant.now(), "DPS1", "CHAVE1", "PROT1", 100, "CNPJ1", "HOMOLOGACAO", "REST", "c1"));

        assertThat(service.buscarPorIdDps("DPS1")).isEmpty();
        assertThat(service.listarUltimas(10)).isEmpty();
    }

    @Test
    @DisplayName("Deve gravar registro PENDENTE e posteriormente atualizar para CONCLUIDO")
    void deveGravarRegistroPendenteEAtualizarParaConcluido() {
        Path arquivo = tempDir.resolve("registro-pendente.jsonl");
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivo, true);

        // 1. Salva pré-transmissão como PENDENTE
        RegistroEmissaoAuditoria pendente = new RegistroEmissaoAuditoria(
                Instant.now(),
                "DPS999",
                null,
                null,
                null,
                "00.***.***/0001-**",
                "HOMOLOGACAO",
                "REST",
                "corr-pendente-1",
                RegistroEmissaoAuditoria.SITUACAO_PENDENTE
        );
        service.salvar(pendente);

        Optional<RegistroEmissaoAuditoria> optPendente = service.buscarPorIdDps("DPS999");
        assertThat(optPendente).isPresent();
        assertThat(optPendente.get().situacao()).isEqualTo(RegistroEmissaoAuditoria.SITUACAO_PENDENTE);
        assertThat(optPendente.get().chaveAcesso()).isNull();

        // 2. Transmissão com sucesso atualiza para CONCLUIDO
        RegistroEmissaoAuditoria concluido = new RegistroEmissaoAuditoria(
                Instant.now(),
                "DPS999",
                "32052002601000000000019100001000000000000001123456",
                "PROT-SUCCESS",
                100,
                "00.***.***/0001-**",
                "HOMOLOGACAO",
                "REST",
                "corr-pendente-1",
                RegistroEmissaoAuditoria.SITUACAO_CONCLUIDO
        );
        service.salvar(concluido);

        Optional<RegistroEmissaoAuditoria> optConcluido = service.buscarPorIdDps("DPS999");
        assertThat(optConcluido).isPresent();
        assertThat(optConcluido.get().situacao()).isEqualTo(RegistroEmissaoAuditoria.SITUACAO_CONCLUIDO);
        assertThat(optConcluido.get().chaveAcesso()).isEqualTo("32052002601000000000019100001000000000000001123456");
        assertThat(optConcluido.get().numeroProtocolo()).isEqualTo("PROT-SUCCESS");

        // Busca por chave também deve encontrar
        Optional<RegistroEmissaoAuditoria> optPorChave = service.buscarPorChave("32052002601000000000019100001000000000000001123456");
        assertThat(optPorChave).isPresent();
        assertThat(optPorChave.get().idDps()).isEqualTo("DPS999");
    }

    @Test
    @DisplayName("Deve carregar índice em memória na inicialização a partir de arquivo existente no disco")
    void deveCarregarIndiceDoDiscoNaInicializacao() {
        Path arquivo = tempDir.resolve("registro-reinicializacao.jsonl");
        RegistroEmissaoDuravelService service1 = new RegistroEmissaoDuravelService(arquivo, true);

        service1.salvar(new RegistroEmissaoAuditoria(
                Instant.now(), "DPS_INIT_1", "CHAVE_INIT_1", "PROT1", 100, "CNPJ1", "HOMOLOGACAO", "REST", "c1"
        ));
        service1.salvar(new RegistroEmissaoAuditoria(
                Instant.now(), "DPS_INIT_2", "CHAVE_INIT_2", "PROT2", 100, "CNPJ2", "HOMOLOGACAO", "SOAP", "c2"
        ));

        // Cria nova instância do serviço apontando para o mesmo arquivo (simulando restart do middleware)
        RegistroEmissaoDuravelService service2 = new RegistroEmissaoDuravelService(arquivo, true);

        Optional<RegistroEmissaoAuditoria> opt1 = service2.buscarPorIdDps("DPS_INIT_1");
        assertThat(opt1).isPresent();
        assertThat(opt1.get().chaveAcesso()).isEqualTo("CHAVE_INIT_1");

        Optional<RegistroEmissaoAuditoria> opt2 = service2.buscarPorChave("CHAVE_INIT_2");
        assertThat(opt2).isPresent();
        assertThat(opt2.get().idDps()).isEqualTo("DPS_INIT_2");
    }

    @Test
    @DisplayName("Deve contabilizar falha de gravação e acionar métrica quando I/O falhar")
    void deveContabilizarFalhaDeGravacaoQuandoIoFalhar() {
        // Aponta para um diretório que impede criação de arquivo com mesmo nome
        Path arquivoInvalido = tempDir.resolve("diretorio_como_arquivo");
        java.io.File dir = arquivoInvalido.toFile();
        dir.mkdir();
        dir.setReadOnly();

        io.micrometer.core.instrument.simple.SimpleMeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        NfseMetrics metrics = new NfseMetrics(meterRegistry);
        RegistroEmissaoDuravelService service = new RegistroEmissaoDuravelService(arquivoInvalido, true, metrics);

        service.salvar(new RegistroEmissaoAuditoria(Instant.now(), "DPS_FALHA", "CHAVE", "PROT", 100, "CNPJ", "HOMOLOGACAO", "REST", "c1"));

        assertThat(service.getContadorFalhasGravacao()).isGreaterThanOrEqualTo(1);
    }
}
