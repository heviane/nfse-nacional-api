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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de validação de esquemas XSD para documentos fiscais da NFS-e Nacional.
 * Compila e armazena em cache esquemas XSD oficiais, garantindo isolamento e performance.
 * Suporta auto-detecção dinâmica entre v1.00 e v1.01 (NT-009 / IBS-CBS).
 */
public class ValidadorSchemaService implements ValidadorSchemaPort {

    public static final String VERSAO_PADRAO = "v1.00";
    public static final String VERSAO_1_01 = "v1.01";
    private static final String PREFIXO_SCHEMAS = "/schemas/";
    private static final Pattern VERSAO_PATTERN = Pattern.compile("versao=[\"']([^\"']+)[\"']");

    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

    @Override
    public List<String> validarDps(String xmlConteudo) {
        return validarDps(xmlConteudo, extrairVersao(xmlConteudo));
    }

    @Override
    public List<String> validarDps(String xmlConteudo, String versao) {
        String versaoAlvo = normalizarVersao(versao != null && !versao.isBlank() ? versao : extrairVersao(xmlConteudo));
        return validarContraSchema(xmlConteudo, versaoAlvo, "DPS_" + versaoAlvo + ".xsd", "DPS");
    }

    @Override
    public void validarDpsOuFalhar(String xmlConteudo) {
        validarDpsOuFalhar(xmlConteudo, extrairVersao(xmlConteudo));
    }

    @Override
    public void validarDpsOuFalhar(String xmlConteudo, String versao) {
        String versaoAlvo = normalizarVersao(versao != null && !versao.isBlank() ? versao : extrairVersao(xmlConteudo));
        List<String> erros = validarDps(xmlConteudo, versaoAlvo);
        if (!erros.isEmpty()) {
            throw new ValidacaoFiscalException(
                    "A DPS não atende aos requisitos do schema XSD da NFS-e Nacional (" + versaoAlvo + ")",
                    erros
            );
        }
    }

    @Override
    public List<String> validarPedRegEvento(String xmlConteudo) {
        return validarPedRegEvento(xmlConteudo, extrairVersao(xmlConteudo));
    }

    @Override
    public List<String> validarPedRegEvento(String xmlConteudo, String versao) {
        String versaoAlvo = normalizarVersao(versao != null && !versao.isBlank() ? versao : extrairVersao(xmlConteudo));
        return validarContraSchema(xmlConteudo, versaoAlvo, "pedRegEvento_" + versaoAlvo + ".xsd", "Pedido de Registro de Evento");
    }

    @Override
    public void validarPedRegEventoOuFalhar(String xmlConteudo) {
        validarPedRegEventoOuFalhar(xmlConteudo, extrairVersao(xmlConteudo));
    }

    @Override
    public void validarPedRegEventoOuFalhar(String xmlConteudo, String versao) {
        String versaoAlvo = normalizarVersao(versao != null && !versao.isBlank() ? versao : extrairVersao(xmlConteudo));
        List<String> erros = validarPedRegEvento(xmlConteudo, versaoAlvo);
        if (!erros.isEmpty()) {
            throw new ValidacaoFiscalException(
                    "O Pedido de Registro de Evento não atende aos requisitos do schema XSD da NFS-e Nacional (" + versaoAlvo + ")",
                    erros
            );
        }
    }

    private List<String> validarContraSchema(String xmlConteudo, String versaoAlvo, String xsdFile, String tipoDocumento) {
        if (xmlConteudo == null || xmlConteudo.isBlank()) {
            return List.of("Conteúdo XML do documento (" + tipoDocumento + ") não pode ser nulo ou vazio.");
        }

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

    /**
     * Extrai a versão declarada na tag raiz do XML da DPS (ex: versao="1.00" ou versao="1.01").
     *
     * @param xmlConteudo Conteúdo XML
     * @return Versão normalizada ("v1.00" ou "v1.01")
     */
    public static String extrairVersao(String xmlConteudo) {
        if (xmlConteudo == null) {
            return VERSAO_PADRAO;
        }
        // Se contiver nós de IBS/CBS, obrigatoriamente pertence ao schema v1.01 (Reforma Tributária)
        if (xmlConteudo.contains("<IBSCBS") || xmlConteudo.contains(":IBSCBS")) {
            return VERSAO_1_01;
        }
        Matcher matcher = VERSAO_PATTERN.matcher(xmlConteudo);
        if (matcher.find()) {
            return normalizarVersao(matcher.group(1));
        }
        return VERSAO_PADRAO;
    }

    /**
     * Normaliza a identificação da versão para o formato canônico de diretório ("v1.00" ou "v1.01").
     *
     * @param versao Versão de entrada (ex: "1.01", "v1.01", "1.00", "v1.00")
     * @return Versão canônica normalizada
     */
    public static String normalizarVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            return VERSAO_PADRAO;
        }
        String limpa = versao.trim().toLowerCase();
        if (limpa.startsWith("v")) {
            limpa = limpa.substring(1);
        }
        if ("1.01".equals(limpa)) {
            return VERSAO_1_01;
        }
        return VERSAO_PADRAO;
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

