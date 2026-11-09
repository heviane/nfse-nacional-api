package br.com.heviane.nfse.adapters.in.legacy;

import br.com.heviane.nfse.adapters.in.rest.CorrelationIdFilter;
import br.com.heviane.nfse.adapters.in.security.CertificadoProvider;
import br.com.heviane.nfse.config.NfseProperties;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Camada Anticorrupção (ACL): Controlador SOAP legado que emula o WebService
 * municipal da SIL Tecnologia (/prefeitura/notafiscal/nfse.asmx) consumido pelo GeneXus 18.
 *
 * Permite ao Cliente 001 apontar a URL base para o Tomcat da BrasilCloud mantendo
 * 100% de transparência e sem alterações de código no ERP GeneXus.
 */
@RestController
@RequestMapping("/prefeitura/notafiscal/nfse.asmx")
public class LegacySoapController {

    private static final Logger log = LoggerFactory.getLogger(LegacySoapController.class);

    private final EmitirDpsUseCase emitirDpsUseCase;
    private final CertificadoProvider certificadoProvider;
    private final NfseProperties nfseProperties;

    @org.springframework.beans.factory.annotation.Autowired
    public LegacySoapController(EmitirDpsUseCase emitirDpsUseCase,
                                CertificadoProvider certificadoProvider,
                                NfseProperties nfseProperties) {
        this.emitirDpsUseCase = emitirDpsUseCase;
        this.certificadoProvider = certificadoProvider;
        this.nfseProperties = (nfseProperties != null) ? nfseProperties : new NfseProperties();
    }

    public LegacySoapController(EmitirDpsUseCase emitirDpsUseCase, CertificadoProvider certificadoProvider) {
        this(emitirDpsUseCase, certificadoProvider, new NfseProperties());
    }

    /**
     * Retorna a definição WSDL para que o GeneXus 18 e ferramentas SOAP identifiquem os métodos do serviço.
     * O endereço soap:address location é resolvido dinamicamente a partir da requisição HTTP (suportando
     * proxy reverso/HTTPS da BrasilCloud) ou de configuração explícita (NFSE_SOAP_ADDRESS_LOCATION).
     */
    @GetMapping(produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> obterWsdl(HttpServletRequest request) {
        log.info("Recebida requisição GET de WSDL para o WebService legado GeneXus");
        String soapAddress = resolverSoapAddress(request);
        String wsdl = gerarWsdl(soapAddress);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(wsdl);
    }

    private String resolverSoapAddress(HttpServletRequest request) {
        String customLocation = System.getenv("NFSE_SOAP_ADDRESS_LOCATION");
        if (customLocation == null || customLocation.isBlank()) {
            customLocation = System.getProperty("nfse.soap.address-location");
        }
        if (customLocation != null && !customLocation.isBlank()) {
            return customLocation.trim();
        }

        if (request != null) {
            try {
                return ServletUriComponentsBuilder
                        .fromRequestUri(request)
                        .replaceQuery(null)
                        .build()
                        .toUriString();
            } catch (Exception e) {
                log.warn("Falha ao resolver dinamicamente o soap:address location a partir da requisição: {}", e.getMessage());
            }
        }
        return "http://localhost:8080/prefeitura/notafiscal/nfse.asmx";
    }

    private String gerarWsdl(String soapAddress) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <wsdl:definitions xmlns:s="http://www.w3.org/2001/XMLSchema"
                                  xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
                                  xmlns:http="http://schemas.xmlsoap.org/wsdl/http/"
                                  xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/"
                                  xmlns:tns="http://www.siltecnologia.com.br/"
                                  xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                                  targetNamespace="http://www.siltecnologia.com.br/"
                                  xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">
                  <wsdl:types>
                    <s:schema elementFormDefault="qualified" targetNamespace="http://www.siltecnologia.com.br/">
                      <s:element name="GerarNfse">
                        <s:complexType>
                          <s:sequence>
                            <s:element minOccurs="0" maxOccurs="1" name="xmlEnvio" type="s:string" />
                          </s:sequence>
                        </s:complexType>
                      </s:element>
                      <s:element name="GerarNfseResponse">
                        <s:complexType>
                          <s:sequence>
                            <s:element minOccurs="0" maxOccurs="1" name="GerarNfseResult" type="s:string" />
                          </s:sequence>
                        </s:complexType>
                      </s:element>
                    </s:schema>
                  </wsdl:types>
                  <wsdl:message name="GerarNfseSoapIn">
                    <wsdl:part name="parameters" element="tns:GerarNfse" />
                  </wsdl:message>
                  <wsdl:message name="GerarNfseSoapOut">
                    <wsdl:part name="parameters" element="tns:GerarNfseResponse" />
                  </wsdl:message>
                  <wsdl:portType name="NfseSoap">
                    <wsdl:operation name="GerarNfse">
                      <wsdl:input message="tns:GerarNfseSoapIn" />
                      <wsdl:output message="tns:GerarNfseSoapOut" />
                    </wsdl:operation>
                  </wsdl:portType>
                  <wsdl:binding name="NfseSoap" type="tns:NfseSoap">
                    <soap:binding transport="http://schemas.xmlsoap.org/soap/http" />
                    <wsdl:operation name="GerarNfse">
                      <soap:operation soapAction="http://www.siltecnologia.com.br/GerarNfse" style="document" />
                      <wsdl:input>
                        <soap:body use="literal" />
                      </wsdl:input>
                      <wsdl:output>
                        <soap:body use="literal" />
                      </wsdl:output>
                    </wsdl:operation>
                  </wsdl:binding>
                  <wsdl:service name="Nfse">
                    <wsdl:port name="NfseSoap" binding="tns:NfseSoap">
                      <soap:address location="%s" />
                    </wsdl:port>
                  </wsdl:service>
                </wsdl:definitions>
                """.formatted(soapAddress).stripIndent();
    }

    /**
     * Processa a chamada SOAP de emissão do GeneXus 18, extrai a DPS,
     * aciona o Core Fiscal Nacional e formata a resposta no envelope SOAP legado.
     */
    @PostMapping(
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "application/soap+xml"},
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "application/soap+xml"}
    )
    public ResponseEntity<String> processarSoap(
            @RequestBody String envelopeSoap,
            @RequestHeader(value = "X-Certificado-Base64", required = false) String certBase64,
            @RequestHeader(value = "X-Certificado-Senha", required = false) String certSenha
    ) {
        log.info("Recebida requisição SOAP de emissão legada via GeneXus 18");
        try {
            String xmlDps = extrairXmlDps(envelopeSoap);
            CertificadoA1 certificado = certificadoProvider.obterCertificado(certBase64, certSenha);
            AmbienteSefin ambiente = nfseProperties.resolverAmbiente();

            RespostaEmissaoSefin resposta = emitirDpsUseCase.emitir(xmlDps, ambiente, certificado);

            String soapResponse = montarRespostaSoapSucesso(
                    resposta.isSucesso(),
                    resposta.codigoStatus(),
                    resposta.motivoStatus(),
                    resposta.chaveAcesso(),
                    resposta.obterNfseXmlDescompactado()
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(soapResponse);

        } catch (Exception e) {
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            if (e instanceof ValidacaoFiscalException || e instanceof IllegalArgumentException) {
                log.warn("Falha de validação na requisição SOAP [CorrelationId: {}]: {}", correlationId, e.getMessage());
            } else if (e instanceof AssinaturaDigitalException) {
                log.error("Falha na assinatura digital da requisição SOAP [CorrelationId: {}]: {}", correlationId, e.getMessage());
            } else {
                log.error("Erro interno ao processar envelope SOAP legado do GeneXus [CorrelationId: {}]", correlationId, e);
            }

            String mensagemControlada = mapearMensagemErro(e, correlationId);
            String soapErrorResponse = montarRespostaSoapErro(mensagemControlada);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(soapErrorResponse);
        }
    }

    private String mapearMensagemErro(Exception e, String correlationId) {
        if (e instanceof ValidacaoFiscalException valEx) {
            String errosDetalhados = (valEx.getErros() != null && !valEx.getErros().isEmpty())
                    ? " Detalhes: " + String.join(" | ", valEx.getErros())
                    : "";
            return "Falha de validação fiscal: " + valEx.getMessage() + errosDetalhados;
        }

        if (e instanceof SecurityException secEx) {
            return secEx.getMessage();
        }

        if (e instanceof IllegalArgumentException argEx) {
            return "Requisição SOAP inválida: " + argEx.getMessage();
        }

        if (e instanceof AssinaturaDigitalException) {
            return "Falha na assinatura digital da DPS. Verifique a validade do certificado digital A1 e da senha informada.";
        }

        if (e instanceof SefinTransmissaoException sefinEx) {
            String detalheHttp = sefinEx.getHttpStatusCode() != null ? " (HTTP " + sefinEx.getHttpStatusCode() + ")" : "";
            return "Falha na comunicação com o Sefin Nacional" + detalheHttp + ". Tente novamente em instantes.";
        }

        if (e instanceof IllegalStateException stateEx && stateEx.getMessage() != null && stateEx.getMessage().toLowerCase().contains("certificado")) {
            return "Certificado digital A1 não configurado no servidor ou não fornecido na requisição.";
        }

        return "Erro interno ao processar a emissão da NFS-e. Identificador de rastreamento: " + correlationId;
    }

    /**
     * Extrai o XML da DPS contido dentro do payload SOAP.
     * Suporta XML em texto claro, CDATA, escapado em entidades HTML (&lt;DPS...&gt;)
     * ou encapsulado em casca de NFS-e (<NFSe><infNFSe>...<DPS>...</infNFSe></NFSe>).
     */
    String extrairXmlDps(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Envelope SOAP recebido está vazio.");
        }

        String decodificado = payload;
        if (decodificado.contains("&lt;")) {
            decodificado = decodificado
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&amp;", "&");
        }

        // Tenta extrair a tag <DPS...>...</DPS>
        java.util.regex.Matcher matcherDps = java.util.regex.Pattern.compile("<DPS[\\s>]").matcher(decodificado);
        int inicioDps = matcherDps.find() ? matcherDps.start() : -1;
        int fimDps = decodificado.indexOf("</DPS>");
        if (inicioDps != -1 && fimDps != -1 && fimDps > inicioDps) {
            String dps = decodificado.substring(inicioDps, fimDps + "</DPS>".length()).trim();
            return normalizarXmlDps(dps, decodificado);
        }

        // Tenta extrair <infDPS...>...</infDPS> e envelopar na raiz <DPS>
        java.util.regex.Matcher matcherInfDps = java.util.regex.Pattern.compile("<infDPS[\\s>]").matcher(decodificado);
        int inicioInfDps = matcherInfDps.find() ? matcherInfDps.start() : -1;
        int fimInfDps = decodificado.indexOf("</infDPS>");
        if (inicioInfDps != -1 && fimInfDps != -1 && fimInfDps > inicioInfDps) {
            String infDps = decodificado.substring(inicioInfDps, fimInfDps + "</infDPS>".length()).trim();
            String versao = (infDps.contains("<IBSCBS") || decodificado.contains("versao=\"1.01\"") || decodificado.contains("versao='1.01'")) ? "1.01" : "1.00";
            return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"%s\">%s</DPS>", versao, infDps);
        }

        if (decodificado.contains("<GerarNfseEnvio") || decodificado.contains("<Rps") || decodificado.contains("<PrestadorServico") || decodificado.contains("<IdentificacaoRps")) {
            throw new IllegalArgumentException("O XML recebido no envelope SOAP está no formato municipal legado da SIL Tecnologia (<Rps> / <GerarNfseEnvio>) e não no padrão da NFS-e Nacional (<DPS> / <infDPS>). Registre o payload correspondente para mapeamento De/Para.");
        }

        throw new IllegalArgumentException("Não foi possível localizar o elemento <DPS> ou <infDPS> no envelope SOAP.");
    }

    private String normalizarXmlDps(String dps, String contextoEnvelope) {
        String resultado = dps;

        // 1. Garante a declaração do namespace oficial da NFS-e Nacional se ausente na tag <DPS>
        if (!resultado.contains("xmlns=")) {
            if (resultado.startsWith("<DPS ")) {
                resultado = "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" " + resultado.substring(5);
            } else if (resultado.startsWith("<DPS>")) {
                resultado = "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\">" + resultado.substring(5);
            }
        }

        // 2. Garante compatibilidade de versão: se contiver IBS/CBS ou vier de documento 1.01, deve usar versao="1.01"
        boolean temIbsCbs = resultado.contains("<IBSCBS") || (contextoEnvelope != null && contextoEnvelope.contains("versao=\"1.01\""));
        if (temIbsCbs) {
            if (resultado.contains("versao=\"1.00\"")) {
                resultado = resultado.replace("versao=\"1.00\"", "versao=\"1.01\"");
            } else if (resultado.contains("versao='1.00'")) {
                resultado = resultado.replace("versao='1.00'", "versao='1.01'");
            } else if (!resultado.contains("versao=")) {
                resultado = resultado.replace("<DPS ", "<DPS versao=\"1.01\" ");
            }
        } else if (!resultado.contains("versao=")) {
            resultado = resultado.replace("<DPS ", "<DPS versao=\"1.00\" ");
        }

        // 3. Garante o prólogo XML se ausente
        if (!resultado.startsWith("<?xml")) {
            resultado = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + resultado;
        }

        return resultado.trim();
    }

    /**
     * Monta o envelope SOAP de resposta com o resultado serializado dentro de {@code <GerarNfseResult>}.
     * Como o contrato WSDL define {@code GerarNfseResult} como {@code xsd:string}, o corpo XML {@code <RetornoEmissao>}
     * é codificado como string XML (entidades &lt; e &gt;). O conteúdo interno de {@code <XmlNfse>} é escapado uma
     * única vez para que o parser do GeneXus obtenha a string XML da NFS-e íntegra após a desserialização do envelope.
     */
    private String montarRespostaSoapSucesso(boolean sucesso, int status, String motivo, String chaveAcesso, String xmlNfse) {
        String chave = chaveAcesso != null ? chaveAcesso : "";
        String xmlEscape = xmlNfse != null ? xmlNfse.replace("<", "&lt;").replace(">", "&gt;") : "";

        return String.format("""
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                  <soap:Body>
                    <GerarNfseResponse xmlns="http://www.siltecnologia.com.br/">
                      <GerarNfseResult>
                        &lt;RetornoEmissao&gt;
                          &lt;Sucesso&gt;%s&lt;/Sucesso&gt;
                          &lt;CodigoStatus&gt;%d&lt;/CodigoStatus&gt;
                          &lt;MotivoStatus&gt;%s&lt;/MotivoStatus&gt;
                          &lt;ChaveAcesso&gt;%s&lt;/ChaveAcesso&gt;
                          &lt;XmlNfse&gt;%s&lt;/XmlNfse&gt;
                        &lt;/RetornoEmissao&gt;
                      </GerarNfseResult>
                    </GerarNfseResponse>
                  </soap:Body>
                </soap:Envelope>
                """, sucesso, status, escapeXml(motivo), chave, xmlEscape);
    }

    private String montarRespostaSoapErro(String mensagem) {
        return String.format("""
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                  <soap:Body>
                    <GerarNfseResponse xmlns="http://www.siltecnologia.com.br/">
                      <GerarNfseResult>
                        &lt;RetornoEmissao&gt;
                          &lt;Sucesso&gt;false&lt;/Sucesso&gt;
                          &lt;CodigoStatus&gt;999&lt;/CodigoStatus&gt;
                          &lt;MotivoStatus&gt;%s&lt;/MotivoStatus&gt;
                        &lt;/RetornoEmissao&gt;
                      </GerarNfseResult>
                    </GerarNfseResponse>
                  </soap:Body>
                </soap:Envelope>
                """, escapeXml(mensagem));
    }

    private String escapeXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

