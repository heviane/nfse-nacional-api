package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.adapters.out.signer.AssinadorDpsService;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: Adaptador HTTP REST Sefin Nacional (COM-02 e COM-03)")
class SefinClientAdapterTest {

    private static HttpServer mockServer;
    private static int mockPort;
    private static String mockBaseUrl;

    private SefinClientAdapter adapter;
    private CertificadoA1 certificado;
    private AssinadorDpsService assinador;
    private ObjectMapper objectMapper;

    private static final String CHAVE_ACESSO_50 = "32052002601000000000019100001000000000000001123456";

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockBaseUrl = "http://127.0.0.1:" + mockPort;
        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        assinador = new AssinadorDpsService();
        objectMapper = new ObjectMapper();
        certificado = CertificadoTesteHelper.obterCertificadoTeste();

        adapter = new SefinClientAdapter(
                assinador,
                objectMapper,
                Duration.ofSeconds(5),
                mockBaseUrl,
                SefinRetryConfig.semEspera(3)
        );
    }

    @Test
    @DisplayName("Deve transmitir DPS com sucesso e processar resposta HTTP 200 com código 100")
    void deveTransmitirDpsComSucesso() {
        String xmlNfseMock = "<NFSe versao=\"1.00\"><infNFSe><nNFSe>100</nNFSe></infNFSe></NFSe>";
        String gzipBase64Mock = compactarGzipBase64(xmlNfseMock);

        String jsonResposta = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
                  "codigoStatus": 100,
                  "motivoStatus": "NFS-e gerada com sucesso",
                  "chaveAcesso": "%s",
                  "nfseXmlGZipB64": "%s"
                }
                """, CHAVE_ACESSO_50, gzipBase64Mock);

        mockServer.createContext("/nfse", exchange -> {
            byte[] responseBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        String xmlDpsAssinado = "<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS></DPS>";
        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao(xmlDpsAssinado);

        RespostaEmissaoSefin resposta = adapter.emitirDps(requisicao, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isTrue();
        assertThat(resposta.codigoStatus()).isEqualTo(100);
        assertThat(resposta.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resposta.obterNfseXmlDescompactado()).isEqualTo(xmlNfseMock);

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("COM-05: Deve receber e processar resposta assíncrona HTTP 200/202 com status 105 sem lançar exceção")
    void deveProcessarRespostaAssincronaStatus105SemLancarExcecao() {
        String jsonRespostaAssincrona = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-05T12:00:00-03:00",
                  "codigoStatus": 105,
                  "motivoStatus": "Lote em processamento",
                  "chaveAcesso": "%s",
                  "numeroProtocolo": "PROT-20261105-001"
                }
                """, CHAVE_ACESSO_50);

        mockServer.createContext("/nfse", exchange -> {
            byte[] responseBytes = jsonRespostaAssincrona.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS></DPS>");
        RespostaEmissaoSefin resposta = adapter.emitirDps(requisicao, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isFalse();
        assertThat(resposta.isEmProcessamento()).isTrue();
        assertThat(resposta.codigoStatus()).isEqualTo(105);
        assertThat(resposta.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resposta.numeroProtocolo()).isEqualTo("PROT-20261105-001");

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("Deve lançar SefinTransmissaoException quando Sefin rejeitar a DPS (codigoStatus != 100)")
    void deveLancarExcecaoAoReceberRejeicaoDoSefin() {
        String jsonRejeicao = """
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
                  "codigoStatus": 204,
                  "motivoStatus": "Rejeição: Duplicidade de DPS"
                }
                """;

        mockServer.createContext("/nfse", exchange -> {
            byte[] responseBytes = jsonRejeicao.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"></DPS>");

        assertThatThrownBy(() -> adapter.emitirDps(requisicao, certificado))
                .isInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("Duplicidade de DPS")
                .hasMessageContaining("204");

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("Deve lançar SefinTransmissaoException quando servidor retornar HTTP 500")
    void deveLancarExcecaoParaErroHttp500() {
        mockServer.createContext("/nfse", exchange -> {
            byte[] responseBytes = "Erro Interno no Sefin".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"></DPS>");

        assertThatThrownBy(() -> adapter.emitirDps(requisicao, certificado))
                .isInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("HTTP 500");

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("Deve consultar NFS-e por Chave de Acesso com sucesso via GET /nfse/{chaveAcesso}")
    void deveConsultarNfsePorChaveComSucesso() {
        String xmlNfseMock = "<NFSe versao=\"1.00\"><infNFSe><nNFSe>200</nNFSe></infNFSe></NFSe>";
        String gzipBase64Mock = compactarGzipBase64(xmlNfseMock);

        String jsonConsulta = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
                  "codigoStatus": 100,
                  "motivoStatus": "NFS-e localizada com sucesso",
                  "chaveAcesso": "%s",
                  "nfseXmlGZipB64": "%s"
                }
                """, CHAVE_ACESSO_50, gzipBase64Mock);

        String contextPath = "/nfse/" + CHAVE_ACESSO_50;
        mockServer.createContext(contextPath, exchange -> {
            byte[] responseBytes = jsonConsulta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RespostaConsultaNfse resposta = adapter.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isEncontrada()).isTrue();
        assertThat(resposta.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resposta.obterNfseXmlDescompactado()).isEqualTo(xmlNfseMock);

        mockServer.removeContext(contextPath);
    }

    @Test
    @DisplayName("Deve rejeitar chave de acesso que não contenha exatamente 50 dígitos numéricos")
    void deveRejeitarChaveAcessoInvalida() {
        assertThatThrownBy(() -> adapter.consultarPorChave("12345", AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 dígitos");

        assertThatThrownBy(() -> adapter.consultarPorChave(null, AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve enviar pedido de cancelamento com sucesso (Evento 101101) assinado e validado contra XSD")
    void deveCancelarNfseComSucesso() {
        String xmlEventoMock = "<evento versao=\"1.00\"><infEvento><tpEvento>101101</tpEvento></infEvento></evento>";
        String gzipBase64Mock = compactarGzipBase64(xmlEventoMock);

        String jsonRespostaCancelamento = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:50:00-03:00",
                  "codigoStatus": 135,
                  "motivoStatus": "Evento registrado e vinculado a NFS-e",
                  "chaveAcesso": "%s",
                  "idEvento": "EVT%s101101001",
                  "eventoXmlGZipB64": "%s"
                }
                """, CHAVE_ACESSO_50, CHAVE_ACESSO_50, gzipBase64Mock);

        java.util.concurrent.atomic.AtomicReference<String> xmlEnviadoCapturado = new java.util.concurrent.atomic.AtomicReference<>();

        mockServer.createContext("/eventos", exchange -> {
            try {
                byte[] requestBytes = exchange.getRequestBody().readAllBytes();
                com.fasterxml.jackson.databind.JsonNode reqJson = objectMapper.readTree(requestBytes);
                String b64 = reqJson.get("pedidoRegistroEventoXmlGZipB64").asText();
                byte[] gzipped = Base64.getDecoder().decode(b64);
                try (java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(gzipped))) {
                    xmlEnviadoCapturado.set(new String(gzis.readAllBytes(), StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
            }

            byte[] responseBytes = jsonRespostaCancelamento.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RequisicaoCancelamentoDps req = new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "1",
                "Cancelamento solicitado devido a erro no preenchimento do tomador",
                AmbienteSefin.HOMOLOGACAO
        );

        RespostaEventoCancelamento resposta = adapter.cancelar(req, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isTrue();
        assertThat(resposta.codigoStatus()).isEqualTo(135);
        assertThat(resposta.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resposta.obterEventoXmlDescompactado()).isEqualTo(xmlEventoMock);

        // Validação estrita do payload capturado enviado ao Sefin
        String xmlEnviado = xmlEnviadoCapturado.get();
        assertThat(xmlEnviado).as("XML enviado deve ser capturado").isNotNull();
        assertThat(xmlEnviado).contains("Id=\"PRE" + CHAVE_ACESSO_50 + "101101\"");
        assertThat(xmlEnviado).contains("<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
        assertThat(assinador.validarAssinatura(xmlEnviado)).as("XML enviado deve conter assinatura matemática válida").isTrue();

        mockServer.removeContext("/eventos");
    }

    @Test
    @DisplayName("Deve validar dados obrigatórios na requisição de cancelamento")
    void deveValidarDadosObrigatoriosNoCancelamento() {
        // Justificativa menor que 15 caracteres
        assertThatThrownBy(() -> new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "1",
                "Curto",
                AmbienteSefin.HOMOLOGACAO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 15 e 255 caracteres");

        // Justificativa maior que 255 caracteres
        assertThatThrownBy(() -> new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "1",
                "A".repeat(256),
                AmbienteSefin.HOMOLOGACAO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 15 e 255 caracteres");

        // Código do motivo inválido (diferente de 1, 2 ou 9)
        assertThatThrownBy(() -> new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "3",
                "Justificativa válida com mais de 15 caracteres",
                AmbienteSefin.HOMOLOGACAO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Código do motivo de cancelamento deve ser 1 (Erro na Emissão), 2 (Serviço não Prestado) ou 9 (Outros).");

        // CNPJ com tamanho inválido
        assertThatThrownBy(() -> new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "123",
                "1",
                "Justificativa válida com mais de 15 caracteres",
                AmbienteSefin.HOMOLOGACAO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 dígitos");
    }

    @Test
    @DisplayName("Deve propagar ValidacaoFiscalException sem envelopar em SefinTransmissaoException quando falhar schema XSD do evento")
    void devePropagarValidacaoFiscalExceptionSemEnveloparEmSefinTransmissaoException() {
        ValidadorSchemaPort validadorMock = org.mockito.Mockito.mock(ValidadorSchemaPort.class);
        org.mockito.Mockito.doThrow(new ValidacaoFiscalException("Falha no schema pedRegEvento", java.util.List.of("cMotivo inválido")))
                .when(validadorMock).validarPedRegEventoOuFalhar(any());

        SefinClientAdapter adapterComValidadorMock = new SefinClientAdapter(
                assinador,
                validadorMock,
                objectMapper,
                Duration.ofSeconds(5),
                mockBaseUrl,
                SefinRetryConfig.semEspera(1)
        );

        RequisicaoCancelamentoDps req = new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "1",
                "Cancelamento solicitado por erro de emissao",
                AmbienteSefin.HOMOLOGACAO
        );

        assertThatThrownBy(() -> adapterComValidadorMock.cancelar(req, certificado))
                .isInstanceOf(ValidacaoFiscalException.class)
                .isNotInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("Falha no schema pedRegEvento");
    }

    @Test
    @DisplayName("Deve propagar AssinaturaDigitalException sem envelopar em SefinTransmissaoException quando falhar assinatura do evento")
    void devePropagarAssinaturaDigitalExceptionSemEnveloparEmSefinTransmissaoException() {
        AssinadorDpsPort assinadorMock = org.mockito.Mockito.mock(AssinadorDpsPort.class);
        org.mockito.Mockito.doThrow(new AssinaturaDigitalException("Falha na assinatura digital"))
                .when(assinadorMock).assinar(any(), any(), any());

        ValidadorSchemaPort validadorMock = org.mockito.Mockito.mock(ValidadorSchemaPort.class);

        SefinClientAdapter adapterComAssinadorMock = new SefinClientAdapter(
                assinadorMock,
                validadorMock,
                objectMapper,
                Duration.ofSeconds(5),
                mockBaseUrl,
                SefinRetryConfig.semEspera(1)
        );

        RequisicaoCancelamentoDps req = new RequisicaoCancelamentoDps(
                CHAVE_ACESSO_50,
                "00000000000191",
                "1",
                "Cancelamento solicitado por erro de emissao",
                AmbienteSefin.HOMOLOGACAO
        );

        assertThatThrownBy(() -> adapterComAssinadorMock.cancelar(req, certificado))
                .isInstanceOf(AssinaturaDigitalException.class)
                .isNotInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("Falha na assinatura digital");
    }

    @Test
    @DisplayName("COM-04: Deve retentar após falha HTTP 503 e obter sucesso na segunda tentativa na emissão")
    void deveRetentarAposFalhaHttp503EObterSucessoNaSegundaTentativa() {
        AtomicInteger tentativas = new AtomicInteger();
        String xmlNfseMock = "<NFSe versao=\"1.00\"><infNFSe><nNFSe>200</nNFSe></infNFSe></NFSe>";
        String gzipBase64Mock = compactarGzipBase64(xmlNfseMock);

        String jsonResposta = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
                  "codigoStatus": 100,
                  "motivoStatus": "NFS-e gerada com sucesso",
                  "chaveAcesso": "%s",
                  "nfseXmlGZipB64": "%s"
                }
                """, CHAVE_ACESSO_50, gzipBase64Mock);

        mockServer.createContext("/nfse", exchange -> {
            int tentativaAtual = tentativas.incrementAndGet();
            if (tentativaAtual == 1) {
                byte[] erroBytes = "Service Unavailable temporário".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, erroBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(erroBytes);
                }
            } else {
                byte[] responseBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS></DPS>");
        RespostaEmissaoSefin resposta = adapter.emitirDps(requisicao, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isTrue();
        assertThat(tentativas.get()).isEqualTo(2);

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("COM-04: Deve retentar após falha HTTP 502 (Bad Gateway) e obter sucesso na emissão")
    void deveRetentarAposFalhaHttp502EObterSucessoNaSegundaTentativa() {
        AtomicInteger tentativas = new AtomicInteger();
        String xmlNfseMock = "<NFSe versao=\"1.00\"><infNFSe><nNFSe>201</nNFSe></infNFSe></NFSe>";
        String gzipBase64Mock = compactarGzipBase64(xmlNfseMock);

        String jsonResposta = String.format("""
                {
                  "tipoAmbiente": 2,
                  "versaoAplicativo": "SefinNacional_v1.0.0",
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
                  "codigoStatus": 100,
                  "motivoStatus": "NFS-e gerada com sucesso",
                  "chaveAcesso": "%s",
                  "nfseXmlGZipB64": "%s"
                }
                """, CHAVE_ACESSO_50, gzipBase64Mock);

        mockServer.createContext("/nfse", exchange -> {
            int tentativaAtual = tentativas.incrementAndGet();
            if (tentativaAtual == 1) {
                byte[] erroBytes = "Bad Gateway".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(502, erroBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(erroBytes);
                }
            } else {
                byte[] responseBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS></DPS>");
        RespostaEmissaoSefin resposta = adapter.emitirDps(requisicao, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isTrue();
        assertThat(tentativas.get()).isEqualTo(2);

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("COM-04: Deve lançar SefinTransmissaoException após esgotar todas as tentativas em HTTP 503")
    void deveLancarExcecaoAposEsgotarTodasTentativasHttp503() {
        AtomicInteger tentativas = new AtomicInteger();

        mockServer.createContext("/nfse", exchange -> {
            tentativas.incrementAndGet();
            byte[] erroBytes = "Servidor Fora do Ar".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, erroBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(erroBytes);
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"></DPS>");

        assertThatThrownBy(() -> adapter.emitirDps(requisicao, certificado))
                .isInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("HTTP 503");

        // O adaptador foi configurado com 3 tentativas
        assertThat(tentativas.get()).isEqualTo(3);

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("COM-04: Não deve retentar quando o erro for de cliente (HTTP 400 ou 422)")
    void naoDeveRetentarEmCasoDeErroHttp400() {
        AtomicInteger tentativas = new AtomicInteger();

        mockServer.createContext("/nfse", exchange -> {
            tentativas.incrementAndGet();
            byte[] erroBytes = "{\"mensagem\":\"Requisição inválida\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, erroBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(erroBytes);
            }
        });

        RequisicaoEnvioDps requisicao = RequisicaoEnvioDps.homologacao("<DPS versao=\"1.00\"></DPS>");

        assertThatThrownBy(() -> adapter.emitirDps(requisicao, certificado))
                .isInstanceOf(SefinTransmissaoException.class)
                .hasMessageContaining("HTTP 400");

        // Falhas 4xx devem falhar imediatamente na 1ª tentativa
        assertThat(tentativas.get()).isEqualTo(1);

        mockServer.removeContext("/nfse");
    }

    @Test
    @DisplayName("COM-04: Deve retentar consulta de NFS-e por chave com sucesso após falha HTTP 504")
    void deveRetentarConsultaNfseComSucessoAposFalha504() {
        AtomicInteger tentativas = new AtomicInteger();
        String jsonResposta = String.format("""
                {
                  "chaveAcesso": "%s",
                  "codigoStatus": 100,
                  "motivoStatus": "NFS-e autorizada",
                  "tipoAmbiente": 2,
                  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00"
                }
                """, CHAVE_ACESSO_50);

        mockServer.createContext("/nfse/" + CHAVE_ACESSO_50, exchange -> {
            int tentativaAtual = tentativas.incrementAndGet();
            if (tentativaAtual == 1) {
                byte[] erroBytes = "Gateway Timeout".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(504, erroBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(erroBytes);
                }
            } else {
                byte[] responseBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });

        RespostaConsultaNfse resposta = adapter.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isEncontrada()).isTrue();
        assertThat(tentativas.get()).isEqualTo(2);

        mockServer.removeContext("/nfse/" + CHAVE_ACESSO_50);
    }

    @Test
    @DisplayName("COM-06: Deve consultar DPS por Id com sucesso via GET /dps/{idDps}")
    void deveConsultarDpsPorIdComSucesso() {
        String jsonConsultaDps = String.format("""
                {
                  "idDps": "DPS123",
                  "chaveAcesso": "%s",
                  "dataHoraProcessamento": "2026-11-05T12:00:00-03:00",
                  "tipoAmbiente": 2
                }
                """, CHAVE_ACESSO_50);

        String contextPath = "/dps/DPS123";
        mockServer.createContext(contextPath, exchange -> {
            byte[] responseBytes = jsonConsultaDps.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RespostaConsultaDps resposta = adapter.consultarPorIdDps("DPS123", AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSucesso()).isTrue();
        assertThat(resposta.idDps()).isEqualTo("DPS123");
        assertThat(resposta.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resposta.tipoAmbiente()).isEqualTo(2);

        mockServer.removeContext(contextPath);
    }

    @Test
    @DisplayName("COM-06: Deve retornar null quando DPS não for encontrada (HTTP 404)")
    void deveRetornarNullQuandoDpsNaoEncontradaHttp404() {
        String contextPath = "/dps/DPS404";
        mockServer.createContext(contextPath, exchange -> {
            byte[] responseBytes = "{\"erros\":[{\"codigo\":\"E01\",\"descricao\":\"DPS não encontrada\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        RespostaConsultaDps resposta = adapter.consultarPorIdDps("DPS404", AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resposta).isNull();

        mockServer.removeContext(contextPath);
    }

    @Test
    @DisplayName("COM-06: Deve validar parâmetros obrigatórios na consulta de DPS por Id")
    void deveValidarParametrosConsultaPorIdDps() {
        assertThatThrownBy(() -> adapter.consultarPorIdDps(null, AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Id da DPS é obrigatório");

        assertThatThrownBy(() -> adapter.consultarPorIdDps("   ", AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Id da DPS é obrigatório");

        assertThatThrownBy(() -> adapter.consultarPorIdDps("DPS123", AmbienteSefin.HOMOLOGACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificado A1 é obrigatório");
    }

    @Test
    @DisplayName("COM-04: Deve validar parâmetros de SefinRetryConfig")
    void deveValidarConfiguracaoDeRetry() {
        assertThatThrownBy(() -> new SefinRetryConfig(0, Duration.ofSeconds(1), 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no mínimo 1");

        assertThatThrownBy(() -> new SefinRetryConfig(3, null, 2.0))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new SefinRetryConfig(3, Duration.ofSeconds(1), 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior ou igual a 1.0");

        SefinRetryConfig padrao = SefinRetryConfig.padrao();
        assertThat(padrao.maxTentativas()).isEqualTo(3);
        assertThat(padrao.delayInicial()).isEqualTo(Duration.ofSeconds(1));
        assertThat(padrao.multiplicador()).isEqualTo(2.0);

        SefinRetryConfig desabilitado = SefinRetryConfig.desabilitado();
        assertThat(desabilitado.maxTentativas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve reutilizar HttpClient em cache em chamadas consecutivas e permitir limpeza")
    void deveReutilizarHttpClientEmChamadasConsecutivasELimparCache() {
        mockServer.createContext("/test-cache/nfse/" + CHAVE_ACESSO_50, exchange -> {
            String respJson = """
                    {
                      "ambiente": 2,
                      "versaoAplicativo": "1.0",
                      "dataHoraProcessamento": "2026-11-08T18:00:00-03:00",
                      "codigoStatus": 100,
                      "motivoStatus": "NFS-e autorizada",
                      "chaveAcesso": "%s",
                      "nfseXmlGZipB64": "%s"
                    }
                    """.formatted(CHAVE_ACESSO_50, compactarGzipBase64("<NFSe></NFSe>"));
            byte[] bytes = respJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SefinClientAdapter testAdapter = new SefinClientAdapter(
                assinador, objectMapper, Duration.ofSeconds(5), mockBaseUrl + "/test-cache"
        );

        RespostaConsultaNfse r1 = testAdapter.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);
        RespostaConsultaNfse r2 = testAdapter.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(r2.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);

        testAdapter.limparHttpClientCache();
    }

    private String compactarGzipBase64(String texto) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(texto.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
