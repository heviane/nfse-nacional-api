package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador de saída para comunicação HTTP REST com o Sefin Nacional / ADN.
 * Realiza autenticação mTLS via {@link HttpClient} nativo do Java 17 com o {@link CertificadoA1}
 * fornecido em memória.
 */
public class SefinClientAdapter implements TransmissorSefinPort {

    private final AssinadorDpsPort assinadorPort;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final String baseUrlOverride;

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ObjectMapper objectMapper) {
        this(assinadorPort, objectMapper, Duration.ofSeconds(30), null);
    }

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ObjectMapper objectMapper, Duration timeout, String baseUrlOverride) {
        this.assinadorPort = Objects.requireNonNull(assinadorPort, "AssinadorDpsPort não pode ser nulo.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper não pode ser nulo.");
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
        this.baseUrlOverride = baseUrlOverride;
    }

    @Override
    public RespostaEmissaoSefin emitirDps(RequisicaoEnvioDps requisicao, CertificadoA1 certificado) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Requisição de envio de DPS não pode ser nula.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para emissão mTLS.");
        }

        try {
            // 1. Compactar o XML assinado em GZIP e converter para Base64
            String dpsGZipB64 = assinadorPort.gerarPayloadGZipB64(requisicao.dpsXmlAssinado());

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("dpsXmlGZipB64", dpsGZipB64);
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            // 2. Determinar endpoint
            String baseUrl = (baseUrlOverride != null) ? baseUrlOverride : requisicao.ambiente().getBaseUrl();
            String url = baseUrl.replaceAll("/+$", "") + "/nfse";

            // 3. Inicializar SSLContext mTLS em memória
            SSLContext sslContext = SefinSSLContextFactory.criarSslContext(certificado);
            HttpClient client = criarHttpClient(sslContext);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return processarRespostaEmissao(response);
        } catch (SefinTransmissaoException e) {
            throw e;
        } catch (Exception e) {
            throw new SefinTransmissaoException("Erro durante a transmissão da DPS ao Sefin Nacional: " + e.getMessage(), e);
        }
    }

    @Override
    public RespostaConsultaNfse consultarPorChave(String chaveAcesso, AmbienteSefin ambiente, CertificadoA1 certificado) {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso deve conter exatamente 50 dígitos numéricos.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para consulta mTLS.");
        }

        AmbienteSefin amb = (ambiente != null) ? ambiente : AmbienteSefin.HOMOLOGACAO;

        try {
            String baseUrl = (baseUrlOverride != null) ? baseUrlOverride : amb.getBaseUrl();
            String url = baseUrl.replaceAll("/+$", "") + "/nfse/" + chaveAcesso;

            SSLContext sslContext = SefinSSLContextFactory.criarSslContext(certificado);
            HttpClient client = criarHttpClient(sslContext);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return processarRespostaConsulta(response);
        } catch (SefinTransmissaoException e) {
            throw e;
        } catch (Exception e) {
            throw new SefinTransmissaoException("Erro ao consultar NFS-e no Sefin Nacional: " + e.getMessage(), e);
        }
    }

    @Override
    public RespostaEventoCancelamento cancelar(RequisicaoCancelamentoDps requisicao, CertificadoA1 certificado) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Requisição de cancelamento não pode ser nula.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para cancelamento mTLS.");
        }

        try {
            String xmlEvento = montarXmlPedidoCancelamento(requisicao);
            String eventoGZipB64 = assinadorPort.gerarPayloadGZipB64(xmlEvento);

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("pedidoRegistroEventoXmlGZipB64", eventoGZipB64);
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            String baseUrl = (baseUrlOverride != null) ? baseUrlOverride : requisicao.ambiente().getBaseUrl();
            String url = baseUrl.replaceAll("/+$", "") + "/eventos";

            SSLContext sslContext = SefinSSLContextFactory.criarSslContext(certificado);
            HttpClient client = criarHttpClient(sslContext);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return processarRespostaCancelamento(response);
        } catch (SefinTransmissaoException e) {
            throw e;
        } catch (Exception e) {
            throw new SefinTransmissaoException("Erro ao enviar pedido de cancelamento ao Sefin: " + e.getMessage(), e);
        }
    }

    private String montarXmlPedidoCancelamento(RequisicaoCancelamentoDps req) {
        String dataHoraAtual = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <pedRegEvento xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <infPedReg Id="EVT%s101101001">
                        <tpAmb>%d</tpAmb>
                        <verAplic>NfseApi_v1.0</verAplic>
                        <dhEvento>%s</dhEvento>
                        <CNPJAutor>%s</CNPJAutor>
                        <chNFSe>%s</chNFSe>
                        <e101101>
                            <xDesc>Cancelamento de NFS-e</xDesc>
                            <cMotivo>%s</cMotivo>
                            <xMotivo>%s</xMotivo>
                        </e101101>
                    </infPedReg>
                </pedRegEvento>
                """,
                req.chaveAcesso(),
                req.ambiente().getCodigo(),
                dataHoraAtual,
                req.cnpjAutor(),
                req.chaveAcesso(),
                req.codigoMotivo(),
                req.motivo().trim()
        );
    }

    private RespostaEventoCancelamento processarRespostaCancelamento(HttpResponse<String> response) throws Exception {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            RespostaEventoCancelamento resposta = objectMapper.readValue(body, RespostaEventoCancelamento.class);
            if (!resposta.isSucesso()) {
                throw new SefinTransmissaoException(
                        String.format("Rejeição de cancelamento pelo Sefin (Status %d): %s", resposta.codigoStatus(), resposta.motivoStatus()),
                        status,
                        resposta.codigoStatus(),
                        body
                );
            }
            return resposta;
        }

        throw new SefinTransmissaoException(
                String.format("Erro HTTP %d ao registrar cancelamento no Sefin: %s", status, body),
                status,
                null,
                body
        );
    }

    private HttpClient criarHttpClient(SSLContext sslContext) {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private RespostaEmissaoSefin processarRespostaEmissao(HttpResponse<String> response) throws Exception {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            RespostaEmissaoSefin resposta = objectMapper.readValue(body, RespostaEmissaoSefin.class);
            if (!resposta.isSucesso()) {
                throw new SefinTransmissaoException(
                        String.format("Rejeição do Sefin Nacional (Status %d): %s", resposta.codigoStatus(), resposta.motivoStatus()),
                        status,
                        resposta.codigoStatus(),
                        body
                );
            }
            return resposta;
        }

        throw new SefinTransmissaoException(
                String.format("Erro HTTP %d retornado pelo Sefin Nacional: %s", status, body),
                status,
                null,
                body
        );
    }

    private RespostaConsultaNfse processarRespostaConsulta(HttpResponse<String> response) throws Exception {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            RespostaConsultaNfse resposta = objectMapper.readValue(body, RespostaConsultaNfse.class);
            if (!resposta.isEncontrada()) {
                throw new SefinTransmissaoException(
                        String.format("Consulta retornou status não autorizado (%d): %s", resposta.codigoStatus(), resposta.motivoStatus()),
                        status,
                        resposta.codigoStatus(),
                        body
                );
            }
            return resposta;
        }

        throw new SefinTransmissaoException(
                String.format("Erro HTTP %d ao consultar NFS-e no Sefin: %s", status, body),
                status,
                null,
                body
        );
    }
}
