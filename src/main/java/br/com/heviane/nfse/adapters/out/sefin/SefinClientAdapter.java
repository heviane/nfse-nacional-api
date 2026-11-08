package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.adapters.out.sefin.dto.*;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
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
 * fornecido em memória e possui resiliência de transporte com política de retentativas (retries).
 */
public class SefinClientAdapter implements TransmissorSefinPort {

    private static final Logger log = LoggerFactory.getLogger(SefinClientAdapter.class);

    private final AssinadorDpsPort assinadorPort;
    private final ValidadorSchemaPort validadorSchemaPort;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final String baseUrlOverride;
    private final SefinRetryConfig retryConfig;

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ObjectMapper objectMapper) {
        this(assinadorPort, new br.com.heviane.nfse.core.services.ValidadorSchemaService(), objectMapper, Duration.ofSeconds(30), null, SefinRetryConfig.padrao());
    }

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ValidadorSchemaPort validadorSchemaPort, ObjectMapper objectMapper) {
        this(assinadorPort, validadorSchemaPort, objectMapper, Duration.ofSeconds(30), null, SefinRetryConfig.padrao());
    }

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ObjectMapper objectMapper, Duration timeout, String baseUrlOverride) {
        this(assinadorPort, new br.com.heviane.nfse.core.services.ValidadorSchemaService(), objectMapper, timeout, baseUrlOverride, SefinRetryConfig.padrao());
    }

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ObjectMapper objectMapper, Duration timeout, String baseUrlOverride, SefinRetryConfig retryConfig) {
        this(assinadorPort, new br.com.heviane.nfse.core.services.ValidadorSchemaService(), objectMapper, timeout, baseUrlOverride, retryConfig);
    }

    public SefinClientAdapter(AssinadorDpsPort assinadorPort, ValidadorSchemaPort validadorSchemaPort, ObjectMapper objectMapper, Duration timeout, String baseUrlOverride, SefinRetryConfig retryConfig) {
        this.assinadorPort = Objects.requireNonNull(assinadorPort, "AssinadorDpsPort não pode ser nulo.");
        this.validadorSchemaPort = Objects.requireNonNull(validadorSchemaPort, "ValidadorSchemaPort não pode ser nulo.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper não pode ser nulo.");
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
        this.baseUrlOverride = baseUrlOverride;
        this.retryConfig = retryConfig != null ? retryConfig : SefinRetryConfig.padrao();
    }

    @Override
    public RespostaEmissaoSefin emitirDps(RequisicaoEnvioDps requisicao, CertificadoA1 certificado) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Requisição de envio de DPS não pode ser nula.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para emissão mTLS.");
        }

        return executarComRetry("Emissão de DPS", () -> {
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
        });
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

        return executarComRetry("Consulta de NFS-e", () -> {
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
        });
    }

    @Override
    public RespostaConsultaDps consultarPorIdDps(String idDps, AmbienteSefin ambiente, CertificadoA1 certificado) {
        if (idDps == null || idDps.isBlank()) {
            throw new IllegalArgumentException("Id da DPS é obrigatório para consulta.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para consulta mTLS.");
        }

        AmbienteSefin amb = (ambiente != null) ? ambiente : AmbienteSefin.HOMOLOGACAO;

        return executarComRetry("Consulta de DPS por Id", () -> {
            String baseUrl = (baseUrlOverride != null) ? baseUrlOverride : amb.getBaseUrl();
            String url = baseUrl.replaceAll("/+$", "") + "/dps/" + idDps.trim();

            SSLContext sslContext = SefinSSLContextFactory.criarSslContext(certificado);
            HttpClient client = criarHttpClient(sslContext);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return processarRespostaConsultaDps(response, idDps);
        });
    }

    @Override
    public RespostaEventoCancelamento cancelar(RequisicaoCancelamentoDps requisicao, CertificadoA1 certificado) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Requisição de cancelamento não pode ser nula.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para cancelamento mTLS.");
        }

        return executarComRetry("Cancelamento de NFS-e", () -> {
            String xmlEvento = montarXmlPedidoCancelamento(requisicao);

            // 1. Validação estrita do schema XSD do pedido antes do envio
            validadorSchemaPort.validarPedRegEventoOuFalhar(xmlEvento);

            // 2. Assinatura digital ICP-Brasil sobre infPedReg com o certificado A1 em memória
            String xmlEventoAssinado = assinadorPort.assinar(xmlEvento, "infPedReg", certificado);

            // 3. Compactação GZIP e codificação Base64
            String eventoGZipB64 = assinadorPort.gerarPayloadGZipB64(xmlEventoAssinado);

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
        });
    }

    @FunctionalInterface
    private interface OperacaoSefin<T> {
        T executar() throws Exception;
    }

    private <T> T executarComRetry(String operacao, OperacaoSefin<T> acao) {
        int tentativa = 1;
        long delayAtualMs = retryConfig.delayInicial().toMillis();

        while (true) {
            try {
                return acao.executar();
            } catch (SefinTransmissaoException e) {
                if (isStatusRetentavel(e.getHttpStatusCode()) && tentativa < retryConfig.maxTentativas()) {
                    log.warn("[{}] Falha transitória HTTP {} na tentativa {}/{}. Retentando em {}ms... Motivo: {}",
                            operacao, e.getHttpStatusCode(), tentativa, retryConfig.maxTentativas(), delayAtualMs, e.getMessage());
                    aplicarDelay(delayAtualMs);
                    tentativa++;
                    delayAtualMs = (long) (delayAtualMs * retryConfig.multiplicador());
                } else {
                    throw e;
                }
            } catch (IOException e) {
                if (tentativa < retryConfig.maxTentativas()) {
                    log.warn("[{}] Falha transitória de I/O na tentativa {}/{}. Retentando em {}ms... Motivo: {}",
                            operacao, tentativa, retryConfig.maxTentativas(), delayAtualMs, e.getMessage());
                    aplicarDelay(delayAtualMs);
                    tentativa++;
                    delayAtualMs = (long) (delayAtualMs * retryConfig.multiplicador());
                } else {
                    throw new SefinTransmissaoException(
                            String.format("Erro de comunicação com o Sefin Nacional após %d tentativas: %s", tentativa, e.getMessage()),
                            e
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SefinTransmissaoException("Execução interrompida durante retentativa com o Sefin Nacional.", e);
            } catch (Exception e) {
                throw new SefinTransmissaoException(
                        String.format("Erro durante %s no Sefin Nacional: %s", operacao, e.getMessage()),
                        e
                );
            }
        }
    }

    private boolean isStatusRetentavel(Integer status) {
        return status != null && (status == 502 || status == 503 || status == 504);
    }

    private void aplicarDelay(long delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SefinTransmissaoException("Thread interrompida durante espera de retentativa com o Sefin Nacional.", e);
            }
        }
    }

    private String montarXmlPedidoCancelamento(RequisicaoCancelamentoDps req) {
        String dataHoraAtual = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        String idPedido = PedRegEventoIdHelper.gerarIdCancelamento(req.chaveAcesso());

        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <pedRegEvento xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <infPedReg Id="%s">
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
                idPedido,
                req.ambiente().getCodigo(),
                dataHoraAtual,
                req.cnpjAutor(),
                req.chaveAcesso(),
                req.codigoMotivo(),
                escapeXml(req.motivo().trim())
        );
    }

    private String escapeXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private RespostaEventoCancelamento processarRespostaCancelamento(HttpResponse<String> response) throws Exception {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            SefinEventoCancelamentoResponseDto dto = objectMapper.readValue(body, SefinEventoCancelamentoResponseDto.class);
            RespostaEventoCancelamento resposta = dto.paraDominio();
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
            SefinEmissaoResponseDto dto = objectMapper.readValue(body, SefinEmissaoResponseDto.class);
            RespostaEmissaoSefin resposta = dto.paraDominio();
            if (!resposta.isSucesso() && !resposta.isEmProcessamento()) {
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
            SefinConsultaNfseResponseDto dto = objectMapper.readValue(body, SefinConsultaNfseResponseDto.class);
            RespostaConsultaNfse resposta = dto.paraDominio();
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

    private RespostaConsultaDps processarRespostaConsultaDps(HttpResponse<String> response, String idDps) throws Exception {
        int status = response.statusCode();
        String body = response.body();

        if (status == 404) {
            log.info("DPS {} não localizada no Sefin Nacional (HTTP 404). Documento não gerado ou pendente de processamento.", idDps);
            return null;
        }

        if (status >= 200 && status < 300) {
            SefinConsultaDpsResponseDto dto = objectMapper.readValue(body, SefinConsultaDpsResponseDto.class);
            return dto.paraDominio();
        }

        throw new SefinTransmissaoException(
                String.format("Erro HTTP %d ao consultar DPS por Id no Sefin: %s", status, body),
                status,
                null,
                body
        );
    }
}
