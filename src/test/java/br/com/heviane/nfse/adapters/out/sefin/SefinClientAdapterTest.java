package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.adapters.out.signer.AssinadorDpsService;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
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
                mockBaseUrl
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
    @DisplayName("Deve enviar pedido de cancelamento com sucesso (Evento 101101)")
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

        mockServer.createContext("/eventos", exchange -> {
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
                .hasMessageContaining("no mínimo 15 caracteres");

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
