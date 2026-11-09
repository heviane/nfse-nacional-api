package br.com.heviane.nfse.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Filtro de autenticação por Chave de API (API Key) para proteção de endpoints REST e SOAP.
 * <p>
 * Executado com prioridade logo após {@link CorrelationIdFilter}, garantindo rastreamento via MDC.
 * <ul>
 *     <li>Se a variável {@code NFSE_API_KEY} estiver configurada no servidor, a autenticação
 *         é <b>obrigatoriamente exigida</b> em todos os endpoints mutantes (/api/** e POST /prefeitura/**).</li>
 *     <li>Suporta envio de credenciais via cabeçalho {@code X-API-Key}, {@code Authorization: Bearer <key>},
 *         {@code Authorization: Basic <base64>} ou parâmetro de consulta {@code ?api_key=<key>}.</li>
 *     <li>Rotas públicas isentas: {@code GET} no WSDL (/prefeitura/notafiscal/nfse.asmx) e
 *         health checks (/actuator/**).</li>
 *     <li>Documentação OpenAPI (/swagger-ui/** e /v3/api-docs) exige autenticação quando a API Key estiver
 *         ativada, a menos que {@code nfse.security.swagger-public=true} (ou {@code NFSE_SWAGGER_PUBLIC=true}).</li>
 *     <li>Caso a variável {@code NFSE_API_KEY} não esteja definida, a API opera em modo permissivo
 *         (recomendado apenas para ambientes isolados por firewall de rede/whitelist de IP).</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BASIC_PREFIX = "Basic ";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final String configuredApiKey;
    private final boolean swaggerPublic;

    @org.springframework.beans.factory.annotation.Autowired
    public ApiKeyAuthFilter(
            @org.springframework.beans.factory.annotation.Value("${nfse.security.api-key:}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${nfse.security.swagger-public:false}") boolean swaggerPublic
    ) {
        String key = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : resolverApiKeyConfigurada();
        this.configuredApiKey = key;
        this.swaggerPublic = swaggerPublic || resolverSwaggerPublic();
    }

    public ApiKeyAuthFilter() {
        this(resolverApiKeyConfigurada(), resolverSwaggerPublic());
    }

    public ApiKeyAuthFilter(String configuredApiKey) {
        this(configuredApiKey, false);
    }

    private static String resolverApiKeyConfigurada() {
        String key = System.getenv("NFSE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("nfse.api.key");
        }
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    private static boolean resolverSwaggerPublic() {
        String prop = System.getProperty("nfse.security.swagger-public");
        if (prop == null || prop.isBlank()) {
            prop = System.getenv("NFSE_SWAGGER_PUBLIC");
        }
        return "true".equalsIgnoreCase(prop);
    }

    public boolean isSecurityEnabled() {
        return configuredApiKey != null;
    }

    public boolean isSwaggerPublic() {
        return swaggerPublic;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Se nenhuma chave foi configurada no servidor, o filtro opera em modo permissivo
        if (configuredApiKey == null) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Permite requisições pre-flight CORS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. Permite inspeção de WSDL do GeneXus (GET em nfse.asmx)
        if ("GET".equalsIgnoreCase(method) && path != null && path.contains("/nfse.asmx")) {
            return true;
        }

        // 3. Permite endpoints públicos de observabilidade (/actuator)
        if (path != null && path.startsWith("/actuator")) {
            return true;
        }

        // 4. Documentação interativa OpenAPI / Swagger UI:
        // Só é isenta de autenticação se explicitamente configurada como pública
        if (swaggerPublic && path != null && (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs"))) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = extrairApiKey(request);

        if (providedKey != null && providedKey.equals(configuredApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bloqueio de acesso não autorizado
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.warn("Tentativa de acesso não autorizado bloqueada [Path: {}, Method: {}, CorrelationId: {}]",
                path, method, correlationId);

        rejeitarAcesso(request, response);
    }

    private String extrairApiKey(HttpServletRequest request) {
        // 1. Cabeçalho direto X-API-Key
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return apiKeyHeader.trim();
        }

        // 2. Cabeçalho Authorization
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && !authHeader.isBlank()) {
            String auth = authHeader.trim();

            // 2a. Bearer token
            if (auth.startsWith(BEARER_PREFIX)) {
                return auth.substring(BEARER_PREFIX.length()).trim();
            }

            // 2b. Basic Auth (GeneXus suporta envio de credenciais Basic em WebServices)
            if (auth.startsWith(BASIC_PREFIX)) {
                try {
                    String base64Credentials = auth.substring(BASIC_PREFIX.length()).trim();
                    byte[] decoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(decoded, StandardCharsets.UTF_8);

                    // Formatos possíveis: "usuario:chave", ":chave", "chave:" ou apenas "chave"
                    if (credentials.contains(":")) {
                        String[] parts = credentials.split(":", 2);
                        String user = parts[0].trim();
                        String pass = parts.length > 1 ? parts[1].trim() : "";
                        if (configuredApiKey.equals(pass)) {
                            return pass;
                        }
                        if (configuredApiKey.equals(user)) {
                            return user;
                        }
                    } else if (configuredApiKey.equals(credentials.trim())) {
                        return credentials.trim();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 3. Parâmetro de consulta na URL (?api_key=... ou ?apiKey=...)
        // Útil para permitir que operadores/desenvolvedores acessem /swagger-ui pelo navegador
        String queryApiKey = request.getParameter("api_key");
        if (queryApiKey == null || queryApiKey.isBlank()) {
            queryApiKey = request.getParameter("apiKey");
        }
        if (queryApiKey != null && !queryApiKey.isBlank()) {
            return queryApiKey.trim();
        }

        return null;
    }

    private void rejeitarAcesso(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String path = request.getRequestURI();
        boolean isSoap = path != null && path.contains("/prefeitura/");

        if (isSoap) {
            response.setContentType("text/xml; charset=utf-8");
            String soapError = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body>
                        <GerarNfseResponse xmlns="http://www.siltecnologia.com.br/">
                          <GerarNfseResult>
                            &lt;RetornoEmissao&gt;
                              &lt;Sucesso&gt;false&lt;/Sucesso&gt;
                              &lt;CodigoStatus&gt;401&lt;/CodigoStatus&gt;
                              &lt;MotivoStatus&gt;Acesso não autorizado. Chave de API inválida ou ausente (X-API-Key ou Authorization).&lt;/MotivoStatus&gt;
                            &lt;/RetornoEmissao&gt;
                          </GerarNfseResult>
                        </GerarNfseResponse>
                      </soap:Body>
                    </soap:Envelope>
                    """;
            response.getWriter().write(soapError);
        } else {
            response.setContentType("application/json; charset=utf-8");
            String jsonError = """
                    {"mensagem":"Acesso não autorizado. Chave de API inválida ou ausente no cabeçalho X-API-Key ou Authorization.","statusHttp":401}
                    """;
            response.getWriter().write(jsonError);
        }
    }
}

