package br.com.heviane.nfse.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários: Filtro de Autenticação por Chave de API (ApiKeyAuthFilter)")
class ApiKeyAuthFilterTest {

    private static final String API_KEY_SECRETA = "segredo-cliente001-api-key-123456";

    @Test
    @DisplayName("Quando NFSE_API_KEY não estiver configurada, deve operar em modo permissivo")
    void deveOperarEmModoPermissivoQuandoChaveNaoConfigurada() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nfse/emitir");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve permitir requisição GET de WSDL (/nfse.asmx) mesmo com API Key ativada")
    void devePermitirGetWsdlSemAutenticacao() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prefeitura/notafiscal/nfse.asmx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve permitir requisições de monitoramento (/actuator) sem autenticação")
    void devePermitirActuatorSemAutenticacao() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve bloquear com HTTP 401 e JSON requisição REST sem chave quando API Key estiver ativa")
    void deveBloquearRestSemChaveCom401() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nfse/emitir");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Acesso não autorizado");
    }

    @Test
    @DisplayName("Deve bloquear com HTTP 401 e Envelope SOAP requisição GeneXus sem chave quando API Key estiver ativa")
    void deveBloquearSoapSemChaveComEnvelopeSoap401() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/prefeitura/notafiscal/nfse.asmx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("text/xml");
        assertThat(response.getContentAsString()).contains("GerarNfseResponse");
        assertThat(response.getContentAsString()).contains("&lt;CodigoStatus&gt;401&lt;/CodigoStatus&gt;");
    }

    @Test
    @DisplayName("Deve autenticar com sucesso via cabeçalho X-API-Key")
    void deveAutenticarComSucessoViaHeaderXApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nfse/emitir");
        request.addHeader("X-API-Key", API_KEY_SECRETA);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso via Authorization Bearer")
    void deveAutenticarComSucessoViaAuthorizationBearer() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nfse/emitir");
        request.addHeader("Authorization", "Bearer " + API_KEY_SECRETA);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso via Authorization Basic (GeneXus WebService credentials)")
    void deveAutenticarComSucessoViaAuthorizationBasic() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        String basicAuth = Base64.getEncoder().encodeToString(("genexus:" + API_KEY_SECRETA).getBytes(StandardCharsets.UTF_8));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/prefeitura/notafiscal/nfse.asmx");
        request.addHeader("Authorization", "Basic " + basicAuth);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve bloquear quando a chave informada for incorreta")
    void deveBloquearChaveIncorreta() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nfse/emitir");
        request.addHeader("X-API-Key", "chave-errada");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Deve bloquear Swagger UI e OpenAPI docs com 401 quando API Key estiver ativa e Swagger não for público")
    void deveBloquearSwaggerSemChaveQuandoApiKeyAtivaESwaggerNaoPublico() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA, false);

        MockHttpServletRequest requestSwagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse responseSwagger = new MockHttpServletResponse();
        FilterChain filterChainSwagger = mock(FilterChain.class);

        filter.doFilter(requestSwagger, responseSwagger, filterChainSwagger);

        verify(filterChainSwagger, never()).doFilter(any(), any());
        assertThat(responseSwagger.getStatus()).isEqualTo(401);

        MockHttpServletRequest requestApiDocs = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse responseApiDocs = new MockHttpServletResponse();
        FilterChain filterChainApiDocs = mock(FilterChain.class);

        filter.doFilter(requestApiDocs, responseApiDocs, filterChainApiDocs);

        verify(filterChainApiDocs, never()).doFilter(any(), any());
        assertThat(responseApiDocs.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Deve permitir Swagger UI e OpenAPI docs quando explicitamente configurado como público")
    void devePermitirSwaggerQuandoExplicitamenteConfiguradoComoPublico() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA, true);

        MockHttpServletRequest requestSwagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse responseSwagger = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(requestSwagger, responseSwagger, filterChain);

        verify(filterChain, times(1)).doFilter(requestSwagger, responseSwagger);
        assertThat(responseSwagger.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso via parâmetro de consulta api_key ou apiKey")
    void deveAutenticarSwaggerViaQueryParamApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_SECRETA, false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setParameter("api_key", API_KEY_SECRETA);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}

