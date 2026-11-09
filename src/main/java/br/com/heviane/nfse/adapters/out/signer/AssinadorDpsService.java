package br.com.heviane.nfse.adapters.out.signer;

import br.com.heviane.nfse.core.domain.AssinaturaDigitalException;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Adaptador de saída para assinatura digital da DPS utilizando certificados ICP-Brasil A1 em memória.
 * Implementa estritamente o padrão W3C XML-DSig requerido pelo Sefin Nacional (RSA-SHA256, SHA-256 Digest,
 * Exclusive Canonicalization e posicionamento da tag Signature sob a raiz DPS).
 */
public class AssinadorDpsService implements AssinadorDpsPort {

    private static final String SIGNATURE_METHOD_RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String NFSE_NS = "http://www.sped.fazenda.gov.br/nfse";

    private final XMLSignatureFactory signatureFactory;

    public AssinadorDpsService() {
        this.signatureFactory = XMLSignatureFactory.getInstance("DOM");
    }

    @Override
    public String assinar(String xml, CertificadoA1 certificado) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("O conteúdo XML não pode ser nulo ou vazio.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("O Certificado A1 não pode ser nulo.");
        }

        try {
            Document doc = parseDocument(xml);
            Element root = doc.getDocumentElement();
            String rootName = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();

            String tagAlvo;
            if ("DPS".equals(rootName)) {
                tagAlvo = "infDPS";
            } else if ("pedRegEvento".equals(rootName)) {
                tagAlvo = "infPedReg";
            } else if (doc.getElementsByTagNameNS(NFSE_NS, "infDPS").getLength() > 0 || doc.getElementsByTagName("infDPS").getLength() > 0) {
                tagAlvo = "infDPS";
            } else if (doc.getElementsByTagNameNS(NFSE_NS, "infPedReg").getLength() > 0 || doc.getElementsByTagName("infPedReg").getLength() > 0) {
                tagAlvo = "infPedReg";
            } else {
                throw new AssinaturaDigitalException("Elemento fiscal a ser assinado não reconhecido na raiz <" + rootName + ">.");
            }

            return assinarDocumento(doc, root, tagAlvo, certificado);
        } catch (AssinaturaDigitalException e) {
            throw e;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao assinar digitalmente o XML: " + e.getMessage(), e);
        }
    }

    @Override
    public String assinar(String xml, String tagElementoAlvo, CertificadoA1 certificado) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("O conteúdo XML não pode ser nulo ou vazio.");
        }
        if (tagElementoAlvo == null || tagElementoAlvo.isBlank()) {
            return assinar(xml, certificado);
        }
        if (certificado == null) {
            throw new IllegalArgumentException("O Certificado A1 não pode ser nulo.");
        }

        try {
            Document doc = parseDocument(xml);
            return assinarDocumento(doc, doc.getDocumentElement(), tagElementoAlvo, certificado);
        } catch (AssinaturaDigitalException e) {
            throw e;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao assinar digitalmente o XML: " + e.getMessage(), e);
        }
    }

    private String assinarDocumento(Document doc, Element rootElement, String tagElementoAlvo, CertificadoA1 certificado) throws Exception {
        Element targetElement = obterElementoAlvo(doc, tagElementoAlvo);
        String idAttr = targetElement.getAttribute("Id");
        if (idAttr == null || idAttr.isBlank()) {
            idAttr = targetElement.getAttribute("id");
        }
        if (idAttr == null || idAttr.isBlank()) {
            throw new AssinaturaDigitalException("Atributo 'Id' obrigatório não encontrado no elemento <" + tagElementoAlvo + ">.");
        }

        // Registra formalmente no DOM o atributo Id como identificador único
        targetElement.setIdAttribute("Id", true);

        // 1. Configurar transformações (Enveloped + C14N Exclusive)
        List<Transform> transforms = new ArrayList<>();
        transforms.add(signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(signatureFactory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));

        // 2. Digest Method SHA-256 referenciando a tag alvo (#Id)
        DigestMethod digestMethod = signatureFactory.newDigestMethod(DigestMethod.SHA256, null);
        Reference reference = signatureFactory.newReference(
                "#" + idAttr,
                digestMethod,
                transforms,
                null,
                null
        );

        // 3. Signature Method RSA-SHA256 + Canonicalization Method Exclusive C14N
        CanonicalizationMethod canonicalizationMethod = signatureFactory.newCanonicalizationMethod(
                CanonicalizationMethod.EXCLUSIVE,
                (C14NMethodParameterSpec) null
        );
        SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(SIGNATURE_METHOD_RSA_SHA256, null);

        SignedInfo signedInfo = signatureFactory.newSignedInfo(
                canonicalizationMethod,
                signatureMethod,
                Collections.singletonList(reference)
        );

        // 4. Construir KeyInfo com o Certificado X.509
        KeyInfoFactory kif = signatureFactory.getKeyInfoFactory();
        X509Certificate x509Cert = certificado.getX509Certificate();
        X509Data x509Data = kif.newX509Data(Collections.singletonList(x509Cert));
        KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data));

        // 5. Configurar contexto de assinatura: o nó pai da <Signature> é o elemento raiz do documento
        DOMSignContext signContext = new DOMSignContext(certificado.getPrivateKey(), rootElement);

        XMLSignature xmlSignature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
        xmlSignature.sign(signContext);

        // 6. Converter documento DOM assinado para String
        return serializeDocument(doc);
    }

    /**
     * Valida matematicamente a assinatura e os digests do XML contra a chave pública presente no próprio KeyInfo.
     * Não valida a cadeia de certificação ICP-Brasil nem o status de revogação (LCR/OCSP).
     */
    @Override
    public boolean validarAssinatura(String xmlAssinado) {
        if (xmlAssinado == null || xmlAssinado.isBlank()) {
            return false;
        }

        try {
            Document doc = parseDocument(xmlAssinado);

            registrarAtributoIdAssinado(doc);

            NodeList signatureNodes = doc.getElementsByTagNameNS(XMLDSIG_NS, "Signature");
            if (signatureNodes.getLength() == 0) {
                signatureNodes = doc.getElementsByTagName("Signature");
            }
            if (signatureNodes.getLength() == 0) {
                return false;
            }

            Element signatureElement = (Element) signatureNodes.item(0);

            // Extrair certificado X.509 contido no KeyInfo da própria assinatura
            NodeList certNodes = signatureElement.getElementsByTagNameNS(XMLDSIG_NS, "X509Certificate");
            if (certNodes.getLength() == 0) {
                certNodes = signatureElement.getElementsByTagName("X509Certificate");
            }
            if (certNodes.getLength() == 0) {
                return false;
            }

            String certBase64 = certNodes.item(0).getTextContent().replaceAll("\\s+", "");
            byte[] certBytes = Base64.getDecoder().decode(certBase64);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

            DOMValidateContext valContext = new DOMValidateContext(
                    KeySelector.singletonKeySelector(cert.getPublicKey()),
                    signatureElement
            );

            XMLSignature signature = signatureFactory.unmarshalXMLSignature(valContext);
            return signature.validate(valContext);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String gerarPayloadGZipB64(String xmlAssinado) {
        if (xmlAssinado == null || xmlAssinado.isBlank()) {
            throw new IllegalArgumentException("O conteúdo XML assinado não pode ser nulo ou vazio.");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(xmlAssinado.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao compactar e codificar payload fiscal em GZIP+Base64: " + e.getMessage(), e);
        }
    }

    private Element obterElementoAlvo(Document doc, String tagElementoAlvo) {
        NodeList nodes = doc.getElementsByTagNameNS(NFSE_NS, tagElementoAlvo);
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagName(tagElementoAlvo);
        }
        if (nodes.getLength() == 0) {
            throw new AssinaturaDigitalException("Elemento obrigatório <" + tagElementoAlvo + "> não encontrado no documento XML.");
        }
        return (Element) nodes.item(0);
    }

    private Element obterElementoInfDps(Document doc) {
        return obterElementoAlvo(doc, "infDPS");
    }

    private void registrarAtributoIdAssinado(Document doc) {
        String[] tags = {"infDPS", "infPedReg", "infEvento", "infNFSe"};
        for (String tag : tags) {
            NodeList nodes = doc.getElementsByTagNameNS(NFSE_NS, tag);
            if (nodes.getLength() == 0) {
                nodes = doc.getElementsByTagName(tag);
            }
            if (nodes.getLength() > 0) {
                Element el = (Element) nodes.item(0);
                if (el.hasAttribute("Id")) {
                    el.setIdAttribute("Id", true);
                    return;
                } else if (el.hasAttribute("id")) {
                    el.setIdAttribute("id", true);
                    return;
                }
            }
        }
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
        }
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String serializeDocument(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
        }
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}

