package br.com.heviane.nfse.core.domain;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Parser seguro e namespace-aware para extração de metadados fundamentais do XML da DPS (infDPS).
 * Protegido contra ataques de injeção XXE e desacoplado de regex frágeis.
 */
public final class DpsXmlParser {

    private DpsXmlParser() {
    }

    /**
     * Extrai os metadados estruturados de {@code infDPS} a partir do XML da DPS.
     *
     * @param xmlDps Conteúdo XML da DPS
     * @return Instância de {@link DpsInfo} ou {@code null} se o documento não contiver {@code infDPS}
     */
    public static DpsInfo extrairInfo(String xmlDps) {
        if (xmlDps == null || xmlDps.isBlank()) {
            return null;
        }

        try {
            Document doc = parseDocument(xmlDps);
            Element infDps = localizarElementoInfDps(doc);
            if (infDps == null) {
                return null;
            }

            String id = infDps.getAttribute("Id");
            if (id != null && id.isBlank()) {
                id = null;
            }

            Integer tpAmb = extrairTextoComoInteiro(obterFilhoDireto(infDps, "tpAmb"));
            String cLocEmi = extrairTexto(obterFilhoDireto(infDps, "cLocEmi"));
            String serie = extrairTexto(obterFilhoDireto(infDps, "serie"));
            String nDps = extrairTexto(obterFilhoDireto(infDps, "nDPS"));

            Element prest = obterFilhoDireto(infDps, "prest");
            String cnpjPrestador = null;
            String cpfPrestador = null;
            if (prest != null) {
                cnpjPrestador = extrairTexto(obterFilhoDireto(prest, "CNPJ"));
                cpfPrestador = extrairTexto(obterFilhoDireto(prest, "CPF"));
            }

            return new DpsInfo(id, tpAmb, cLocEmi, serie, nDps, cnpjPrestador, cpfPrestador);
        } catch (Exception e) {
            return null;
        }
    }

    private static Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
        }
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static Element localizarElementoInfDps(Document doc) {
        NodeList list = doc.getElementsByTagNameNS("*", "infDPS");
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        list = doc.getElementsByTagName("infDPS");
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        return null;
    }

    private static Element obterFilhoDireto(Element pai, String nomeLocalDesejado) {
        if (pai == null) return null;
        NodeList filhos = pai.getChildNodes();
        for (int i = 0; i < filhos.getLength(); i++) {
            Node filho = filhos.item(i);
            if (filho.getNodeType() == Node.ELEMENT_NODE) {
                String localName = filho.getLocalName();
                if (localName == null) {
                    localName = filho.getNodeName();
                    int colonIndex = localName.indexOf(':');
                    if (colonIndex >= 0) {
                        localName = localName.substring(colonIndex + 1);
                    }
                }
                if (nomeLocalDesejado.equalsIgnoreCase(localName)) {
                    return (Element) filho;
                }
            }
        }
        return null;
    }

    private static String extrairTexto(Element elemento) {
        if (elemento == null) return null;
        String text = elemento.getTextContent();
        return text != null ? text.trim() : null;
    }

    private static Integer extrairTextoComoInteiro(Element elemento) {
        String texto = extrairTexto(elemento);
        if (texto != null && !texto.isBlank()) {
            try {
                return Integer.parseInt(texto.replaceAll("\\D", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
