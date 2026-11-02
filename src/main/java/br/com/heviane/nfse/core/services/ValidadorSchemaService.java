package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.ValidacaoFiscalException;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de validação de esquemas XSD para documentos fiscais da NFS-e Nacional.
 * Compila e armazena em cache esquemas XSD oficiais, garantindo isolamento e performance.
 */
public class ValidadorSchemaService implements ValidadorSchemaPort {

    private static final String VERSAO_PADRAO = "v1.00";
    private static final String PREFIXO_SCHEMAS = "/schemas/";

    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

    @Override
    public List<String> validarDps(String xmlConteudo) {
        return validarDps(xmlConteudo, VERSAO_PADRAO);
    }

    @Override
    public List<String> validarDps(String xmlConteudo, String versao) {
        if (xmlConteudo == null || xmlConteudo.isBlank()) {
            return List.of("Conteúdo XML da DPS não pode ser nulo ou vazio.");
        }

        String versaoAlvo = (versao != null && !versao.isBlank()) ? versao : VERSAO_PADRAO;
        String xsdFile = "DPS_" + versaoAlvo + ".xsd";

        try {
            Schema schema = obterSchema(versaoAlvo, xsdFile);
            Validator validator = schema.newValidator();

            List<String> erros = new ArrayList<>();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Avisos não invalidam o schema fiscal
                }

                @Override
                public void error(SAXParseException exception) {
                    erros.add(formatarErro("Erro", exception));
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    erros.add(formatarErro("Erro Fatal", exception));
                }
            });

            Source source = new StreamSource(new StringReader(xmlConteudo));
            validator.validate(source);

            return erros;
        } catch (SAXException e) {
            return List.of("Falha sintática no XML: " + e.getMessage());
        } catch (Exception e) {
            return List.of("Erro inesperado durante a validação XSD: " + e.getMessage());
        }
    }

    @Override
    public void validarDpsOuFalhar(String xmlConteudo) {
        List<String> erros = validarDps(xmlConteudo);
        if (!erros.isEmpty()) {
            throw new ValidacaoFiscalException("A DPS não atende aos requisitos do schema XSD da NFS-e Nacional", erros);
        }
    }

    private Schema obterSchema(String versao, String xsdFile) throws SAXException {
        String chaveCache = versao + "/" + xsdFile;
        Schema cached = schemaCache.get(chaveCache);
        if (cached != null) {
            return cached;
        }

        String basePath = PREFIXO_SCHEMAS + versao + "/";
        String xsdPath = basePath + xsdFile;

        URL xsdUrl = getClass().getResource(xsdPath);
        if (xsdUrl == null) {
            throw new IllegalArgumentException("Arquivo de schema XSD não encontrado no classpath: " + xsdPath);
        }

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
        }

        factory.setResourceResolver(new ClasspathResourceResolver(basePath));
        Schema schema = factory.newSchema(xsdUrl);
        schemaCache.put(chaveCache, schema);
        return schema;
    }

    private String formatarErro(String nivel, SAXParseException ex) {
        return String.format("[%s] Linha %d, Coluna %d: %s",
                nivel,
                ex.getLineNumber(),
                ex.getColumnNumber(),
                ex.getMessage());
    }

    /**
     * Resolvedor de recursos XML para carregar xs:include e xs:import diretamente do classpath.
     */
    private static class ClasspathResourceResolver implements LSResourceResolver {

        private final String basePath;

        public ClasspathResourceResolver(String basePath) {
            this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            if (systemId == null) {
                return null;
            }

            String filename = systemId.contains("/") ? systemId.substring(systemId.lastIndexOf('/') + 1) : systemId;
            String resourcePath = basePath + filename;

            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                return null;
            }

            return new ClasspathLSInput(publicId, systemId, is);
        }
    }

    private static class ClasspathLSInput implements LSInput {
        private final String publicId;
        private final String systemId;
        private final InputStream byteStream;

        public ClasspathLSInput(String publicId, String systemId, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.byteStream = byteStream;
        }

        @Override public Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(Reader characterStream) {}
        @Override public InputStream getByteStream() { return byteStream; }
        @Override public void setByteStream(InputStream byteStream) {}
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String stringData) {}
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String systemId) {}
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String publicId) {}
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String baseURI) {}
        @Override public String getEncoding() { return StandardCharsets.UTF_8.name(); }
        @Override public void setEncoding(String encoding) {}
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean certifiedText) {}
    }
}
