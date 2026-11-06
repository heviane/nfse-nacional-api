package br.com.heviane.nfse.adapters.in.legacy;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import br.com.heviane.nfse.core.domain.ValidacaoFiscalException;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import br.com.heviane.nfse.core.services.CertificadoProvider;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LegacySoapController.class)
class LegacySoapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmitirDpsUseCase emitirDpsUseCase;

    @MockBean
    private CertificadoProvider certificadoProvider;

    private CertificadoA1 certificadoA1;

    @BeforeEach
    void setUp() throws Exception {
        certificadoA1 = CertificadoTesteHelper.obterCertificadoTeste();
    }

    private String compactarGZipBase64(String conteudo) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(conteudo.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    @DisplayName("Deve retornar WSDL válido no GET /prefeitura/notafiscal/nfse.asmx")
    void deveRetornarWsdlValido() throws Exception {
        mockMvc.perform(get("/prefeitura/notafiscal/nfse.asmx?WSDL"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wsdl:definitions")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("targetNamespace=\"http://www.siltecnologia.com.br/\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"GerarNfse\"")));
    }

    @Test
    @DisplayName("Deve processar envelope SOAP com DPS em texto claro e retornar SOAP com sucesso")
    void deveProcessarEnvelopeSoapComDpsEmTextoClaro() throws Exception {
        String chave = "32052002601000000000019100001000000000000001123456";
        String xmlNfse = "<NFSe versao=\"1.00\"><infNFSe Id=\"NFS" + chave + "\"></infNFSe></NFSe>";
        String gzipB64 = compactarGZipBase64(xmlNfse);

        RespostaEmissaoSefin respostaSefin = new RespostaEmissaoSefin(
                2,
                "Sefin_v1.0",
                "2026-11-02T16:00:00-03:00",
                100,
                "NFS-e gerada com sucesso",
                chave,
                gzipB64
        );

        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(contains("DPS"), eq(AmbienteSefin.HOMOLOGACAO), any(CertificadoA1.class)))
                .thenReturn(respostaSefin);

        String soapRequest = """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:sil="http://www.siltecnologia.com.br/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <sil:GerarNfse>
                      <sil:xmlEnvio>
                        <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                          <infDPS Id="DPS320520020000000000019100001000000000000001"></infDPS>
                        </DPS>
                      </sil:xmlEnvio>
                    </sil:GerarNfse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.stripIndent();

        mockMvc.perform(post("/prefeitura/notafiscal/nfse.asmx")
                        .contentType(MediaType.TEXT_XML)
                        .content(soapRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GerarNfseResponse")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;Sucesso&gt;true&lt;/Sucesso&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;CodigoStatus&gt;100&lt;/CodigoStatus&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(chave)));
    }

    @Test
    @DisplayName("Deve processar envelope SOAP com XML escapado em entidades HTML")
    void deveProcessarEnvelopeSoapComXmlEscapado() throws Exception {
        String chave = "32052002601000000000019100001000000000000001123456";
        String xmlNfse = "<NFSe versao=\"1.00\"></NFSe>";
        String gzipB64 = compactarGZipBase64(xmlNfse);

        RespostaEmissaoSefin respostaSefin = new RespostaEmissaoSefin(
                2,
                "Sefin_v1.0",
                "2026-11-02T16:00:00-03:00",
                100,
                "NFS-e gerada com sucesso",
                chave,
                gzipB64
        );

        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(contains("DPS"), eq(AmbienteSefin.HOMOLOGACAO), any(CertificadoA1.class)))
                .thenReturn(respostaSefin);

        String soapRequest = """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:sil="http://www.siltecnologia.com.br/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <sil:GerarNfse>
                      <sil:xmlEnvio>&lt;DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00"&gt;&lt;infDPS Id="DPS1"&gt;&lt;/infDPS&gt;&lt;/DPS&gt;</sil:xmlEnvio>
                    </sil:GerarNfse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.stripIndent();

        mockMvc.perform(post("/prefeitura/notafiscal/nfse.asmx")
                        .contentType(MediaType.TEXT_XML)
                        .content(soapRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;Sucesso&gt;true&lt;/Sucesso&gt;")));
    }

    @Test
    @DisplayName("Deve retornar envelope SOAP com Sucesso=false quando ocorrer erro no Core Fiscal")
    void deveRetornarEnvelopeSoapComErroQuandoCoreFalhar() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(any(), any(), any()))
                .thenThrow(new ValidacaoFiscalException("Schema XSD inválido", List.of("Elemento incorreto")));

        String soapRequest = """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00"></DPS>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.stripIndent();

        mockMvc.perform(post("/prefeitura/notafiscal/nfse.asmx")
                        .contentType(MediaType.TEXT_XML)
                        .content(soapRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;Sucesso&gt;false&lt;/Sucesso&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;CodigoStatus&gt;999&lt;/CodigoStatus&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Schema XSD inv&aacute;lido".replace("&aacute;", "á"))));
    }

    @Test
    @DisplayName("Deve testar extração direta do XML da DPS")
    void deveTestarExtracaoXmlDpsDireta() {
        LegacySoapController controller = new LegacySoapController(emitirDpsUseCase, certificadoProvider);

        String envelopeRaw = "<soap><Body><DPS versao=\"1.00\"><infDPS Id=\"1\"></infDPS></DPS></Body></soap>";
        assertThat(controller.extrairXmlDps(envelopeRaw)).isEqualTo("<DPS versao=\"1.00\"><infDPS Id=\"1\"></infDPS></DPS>");

        String envelopeInfDps = "<soap><Body><infDPS Id=\"DPS123\"><tpAmb>2</tpAmb></infDPS></Body></soap>";
        assertThat(controller.extrairXmlDps(envelopeInfDps)).contains("<DPS").contains("DPS123");

        assertThatThrownBy(() -> controller.extrairXmlDps("<soap><Body>SemDps</Body></soap>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NÃO deve vazar mensagem de erro interna ou stack trace para o ERP quando ocorrer erro inesperado")
    void deveRetornarMensagemControladaComCorrelationIdQuandoOcorrerErroInternoInesperado() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(any(), any(), any()))
                .thenThrow(new RuntimeException("SQLConnectionException: Host 192.168.1.50 connection failed, user=admin, pass=secret"));

        String soapRequest = """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00"></DPS>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.stripIndent();

        mockMvc.perform(post("/prefeitura/notafiscal/nfse.asmx")
                        .header("X-Correlation-ID", "corr-test-12345")
                        .contentType(MediaType.TEXT_XML)
                        .content(soapRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;Sucesso&gt;false&lt;/Sucesso&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;CodigoStatus&gt;999&lt;/CodigoStatus&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("corr-test-12345")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQLConnectionException"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    @DisplayName("NÃO deve vazar detalhes do provedor PKCS#12 ou keystore quando falhar a assinatura digital")
    void deveRetornarMensagemControladaQuandoFalharAssinaturaDigital() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(any(), any(), any()))
                .thenThrow(new br.com.heviane.nfse.core.domain.AssinaturaDigitalException("keystore password was incorrect for key alias"));

        String soapRequest = """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00"></DPS>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.stripIndent();

        mockMvc.perform(post("/prefeitura/notafiscal/nfse.asmx")
                        .contentType(MediaType.TEXT_XML)
                        .content(soapRequest))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;Sucesso&gt;false&lt;/Sucesso&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;CodigoStatus&gt;999&lt;/CodigoStatus&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Falha na assinatura digital da DPS")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("keystore password was incorrect"))));
    }
}

