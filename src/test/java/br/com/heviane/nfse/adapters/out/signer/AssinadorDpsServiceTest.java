package br.com.heviane.nfse.adapters.out.signer;

import br.com.heviane.nfse.core.domain.AssinaturaDigitalException;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.services.ValidadorSchemaService;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: Assinador Digital A1 em Memória (CORE-02)")
class AssinadorDpsServiceTest {

    private AssinadorDpsService assinador;
    private ValidadorSchemaService validador;
    private CertificadoA1 certificado;
    private String xmlValido;

    @BeforeEach
    void setUp() throws Exception {
        assinador = new AssinadorDpsService();
        validador = new ValidadorSchemaService();
        certificado = CertificadoTesteHelper.obterCertificadoTeste();

        try (InputStream is = getClass().getResourceAsStream("/samples/dps-exemplo-valido.xml")) {
            assertThat(is).isNotNull();
            xmlValido = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Deve assinar DPS em memória utilizando RSA-SHA256 e posicionar Signature sob raiz DPS")
    void deveAssinarDpsComSucesso() {
        String xmlAssinado = assinador.assinar(xmlValido, certificado);

        assertThat(xmlAssinado).isNotBlank();

        // 1. Deve conter bloco Signature no namespace W3C XML-DSig
        assertThat(xmlAssinado).contains("<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");

        // 2. Algoritmo de assinatura deve ser RSA-SHA256 (exigência do Sefin Nacional)
        assertThat(xmlAssinado).contains("Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"");

        // 3. Digest Method deve ser SHA-256
        assertThat(xmlAssinado).contains("Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"");

        // 4. Transformação C14N Exclusiva presente
        assertThat(xmlAssinado).contains("Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"");

        // 5. Referência deve apontar para o Id da DPS
        assertThat(xmlAssinado).contains("URI=\"#DPS353060710005555555555500900000001903128101\"");

        // 6. Deve conter os dados do Certificado X.509 em KeyInfo
        assertThat(xmlAssinado).contains("<X509Certificate>");

        // 7. A validação matemática da assinatura gerada deve ser 100% íntegra
        boolean valida = assinador.validarAssinatura(xmlAssinado);
        assertThat(valida).as("A assinatura digital gerada deve ser matematicamente válida").isTrue();
    }

    @Test
    @DisplayName("DPS assinada deve ser 100% válida contra o Schema XSD oficial v1.00 (incluindo bloco ds:Signature)")
    void dpsAssinadaDeveContinuarValidaContraXsd() {
        String xmlAssinado = assinador.assinar(xmlValido, certificado);

        List<String> erros = validador.validarDps(xmlAssinado);

        assertThat(erros)
                .as("DPS com assinatura digital anexada deve validar 100% contra o XSD oficial v1.00")
                .isEmpty();
    }

    @Test
    @DisplayName("Deve invalidar a assinatura caso o conteúdo da DPS seja adulterado após assinatura")
    void deveDetectarAdulteracaoNoXmlAssinado() {
        String xmlAssinado = assinador.assinar(xmlValido, certificado);

        // Adulteração maliciosa do valor do serviço após assinado
        String xmlAdulterado = xmlAssinado.replace("<vServ>1000.00</vServ>", "<vServ>9999.00</vServ>");

        boolean valida = assinador.validarAssinatura(xmlAdulterado);
        assertThat(valida)
                .as("Validação matemática deve falhar se o digest do conteúdo assinado for violado")
                .isFalse();
    }

    @Test
    @DisplayName("Deve gerar payload compactado em GZIP e codificado em Base64 para envio ao Sefin")
    void deveGerarPayloadGZipBase64() throws Exception {
        String xmlAssinado = assinador.assinar(xmlValido, certificado);

        String payloadB64 = assinador.gerarPayloadGZipB64(xmlAssinado);
        assertThat(payloadB64).isNotBlank();

        // Validar descompactação
        byte[] gzippedBytes = Base64.getDecoder().decode(payloadB64);
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzippedBytes))) {
            String xmlDescompactado = new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xmlDescompactado).isEqualTo(xmlAssinado);
        }
    }

    @Test
    @DisplayName("Deve rejeitar XML sem a tag infDPS ou sem atributo Id")
    void deveRejeitarXmlSemInfDpsOuSemId() {
        String xmlSemInfDps = "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\"></DPS>";
        assertThatThrownBy(() -> assinador.assinar(xmlSemInfDps, certificado))
                .isInstanceOf(AssinaturaDigitalException.class)
                .hasMessageContaining("infDPS");

        String xmlSemId = "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\"><infDPS></infDPS></DPS>";
        assertThatThrownBy(() -> assinador.assinar(xmlSemId, certificado))
                .isInstanceOf(AssinaturaDigitalException.class)
                .hasMessageContaining("Id");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para entradas nulas ou vazias")
    void deveRejeitarParametrosNulos() {
        assertThatThrownBy(() -> assinador.assinar(null, certificado))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> assinador.assinar("  ", certificado))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> assinador.assinar(xmlValido, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> assinador.gerarPayloadGZipB64(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

