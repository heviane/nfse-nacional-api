package br.com.heviane.nfse.adapters.out.signer;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Verificação Estrita de Canonicalização (C14N) e DigestValue para Sefin Nacional")
class CanonicalizationAndDigestVerificationTest {

    private AssinadorDpsService assinador;
    private CertificadoA1 certificado;
    private String xmlDpsValido;

    @BeforeEach
    void setUp() throws Exception {
        assinador = new AssinadorDpsService();
        certificado = CertificadoTesteHelper.obterCertificadoTeste();

        try (InputStream is = getClass().getResourceAsStream("/samples/dps-exemplo-valido.xml")) {
            assertThat(is).isNotNull();
            xmlDpsValido = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Deve validar a ordem estrita de transformações: 1º Enveloped, 2º Exclusive C14N")
    void deveValidarOrdemEstritaDeTransformacoes() throws Exception {
        String xmlAssinado = assinador.assinar(xmlDpsValido, certificado);

        Document doc = parseDocument(xmlAssinado);
        NodeList transformNodes = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Transform");

        assertThat(transformNodes.getLength())
                .as("A assinatura deve conter exatamente 2 transformações")
                .isEqualTo(2);

        Element firstTransform = (Element) transformNodes.item(0);
        Element secondTransform = (Element) transformNodes.item(1);

        assertThat(firstTransform.getAttribute("Algorithm"))
                .as("A primeira transformação DEVE ser Enveloped Signature")
                .isEqualTo(Transform.ENVELOPED);

        assertThat(secondTransform.getAttribute("Algorithm"))
                .as("A segunda transformação DEVE ser Exclusive Canonicalization (C14N)")
                .isEqualTo("http://www.w3.org/2001/10/xml-exc-c14n#");
    }

    @Test
    @DisplayName("Deve validar que o DigestValue declarado coincide exatamente com o Digest calculado sobre infDPS canonicalizado")
    void deveValidarDigestValueDeclaradoECalculadoSobreInfDps() throws Exception {
        String xmlAssinado = assinador.assinar(xmlDpsValido, certificado);

        Document doc = parseDocument(xmlAssinado);
        Element root = doc.getDocumentElement();
        Element infDps = (Element) doc.getElementsByTagNameNS("http://www.sped.fazenda.gov.br/nfse", "infDPS").item(0);
        infDps.setIdAttribute("Id", true);

        Element sigElement = (Element) doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DOMValidateContext valContext = new DOMValidateContext(
                KeySelector.singletonKeySelector(certificado.getX509Certificate().getPublicKey()),
                sigElement
        );

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        Reference ref = (Reference) signature.getSignedInfo().getReferences().get(0);

        // Valida que a referência do XML-DSig aponta para #Id da infDPS
        assertThat(ref.getURI()).isEqualTo("#" + infDps.getAttribute("Id"));

        // Validação da referência (aplica transforms, calcula SHA-256 e compara com DigestValue)
        boolean refValid = ref.validate(valContext);
        assertThat(refValid)
                .as("O Reference da infDPS deve ser 100% válido")
                .isTrue();

        byte[] calculatedDigest = ref.getCalculatedDigestValue();
        byte[] declaredDigest = ref.getDigestValue();

        assertThat(calculatedDigest)
                .as("DigestValue calculado e declarado devem ser idênticos byte a byte")
                .isNotNull()
                .isEqualTo(declaredDigest);

        String declaredDigestB64 = Base64.getEncoder().encodeToString(declaredDigest);
        NodeList digestValueNodes = sigElement.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "DigestValue");
        assertThat(digestValueNodes.item(0).getTextContent().trim())
                .isEqualTo(declaredDigestB64);
    }

    @Test
    @DisplayName("Deve validar a integridade de C14N e DigestValue para pedRegEvento (Cancelamento)")
    void deveValidarDigestValueParaPedRegEvento() throws Exception {
        String chave50 = "32052002601000000000019100001000000000000001123456";
        String idPedido = "PRE" + chave50 + "101101";
        String xmlEvento = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <pedRegEvento xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <infPedReg Id="%s">
                        <tpAmb>2</tpAmb>
                        <verAplic>NfseApi_v1.0</verAplic>
                        <dhEvento>2026-11-09T12:00:00-03:00</dhEvento>
                        <CNPJAutor>00000000000191</CNPJAutor>
                        <chNFSe>%s</chNFSe>
                        <e101101>
                            <xDesc>Cancelamento de NFS-e</xDesc>
                            <cMotivo>1</cMotivo>
                            <xMotivo>Cancelamento de teste em homologacao</xMotivo>
                        </e101101>
                    </infPedReg>
                </pedRegEvento>
                """, idPedido, chave50);

        String xmlAssinado = assinador.assinar(xmlEvento, certificado);

        Document doc = parseDocument(xmlAssinado);
        Element infPedReg = (Element) doc.getElementsByTagNameNS("http://www.sped.fazenda.gov.br/nfse", "infPedReg").item(0);
        infPedReg.setIdAttribute("Id", true);

        Element sigElement = (Element) doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DOMValidateContext valContext = new DOMValidateContext(
                KeySelector.singletonKeySelector(certificado.getX509Certificate().getPublicKey()),
                sigElement
        );

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        Reference ref = (Reference) signature.getSignedInfo().getReferences().get(0);

        assertThat(ref.getURI()).isEqualTo("#" + idPedido);
        assertThat(ref.validate(valContext)).isTrue();
        assertThat(ref.getCalculatedDigestValue()).isEqualTo(ref.getDigestValue());
        assertThat(signature.validate(valContext)).isTrue();
    }

    @Test
    @DisplayName("Canonicalização exclusiva não deve quebrar mesmo se DPS tiver atributos adicionais no nó raiz")
    void canonicalizacaoNaoDeveQuebrarComAtributosNoNoRaiz() throws Exception {
        String xmlModificado = xmlDpsValido.replace(
                "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\">",
                "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" xmlns:extra=\"http://extra.com/ns\" versao=\"1.00\">"
        );

        String xmlAssinado = assinador.assinar(xmlModificado, certificado);

        Document doc = parseDocument(xmlAssinado);
        Element infDps = (Element) doc.getElementsByTagNameNS("http://www.sped.fazenda.gov.br/nfse", "infDPS").item(0);
        infDps.setIdAttribute("Id", true);

        Element sigElement = (Element) doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DOMValidateContext valContext = new DOMValidateContext(
                KeySelector.singletonKeySelector(certificado.getX509Certificate().getPublicKey()),
                sigElement
        );

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        Reference ref = (Reference) signature.getSignedInfo().getReferences().get(0);

        assertThat(ref.validate(valContext)).isTrue();
        assertThat(signature.validate(valContext)).isTrue();
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
