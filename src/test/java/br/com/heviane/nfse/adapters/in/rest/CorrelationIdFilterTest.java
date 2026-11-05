package br.com.heviane.nfse.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve gerar um novo Correlation-ID em formato UUID quando o cabeçalho não for enviado")
    void shouldGenerateAndSetCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotBlank();
        assertThat(UUID.fromString(generatedId)).isNotNull();
        assertThat(mdcCorrelationId.get()).isEqualTo(generatedId);
        // Garante que o MDC foi limpo após a execução do filtro
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("Deve preservar o X-Correlation-ID enviado pelo cliente na requisição")
    void shouldPreserveExistingCorrelationIdFromRequestHeader() throws ServletException, IOException {
        String existingId = "client-trace-123456";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(mdcCorrelationId.get()).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("Deve utilizar o X-Request-ID caso o X-Correlation-ID não esteja presente")
    void shouldFallbackToXRequestIdIfCorrelationIdMissing() throws ServletException, IOException {
        String requestId = "req-service-abc-999";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(requestId);
        assertThat(mdcCorrelationId.get()).isEqualTo(requestId);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("Deve gerar um novo UUID se o cabeçalho X-Correlation-ID contiver apenas espaços em branco")
    void shouldHandleBlankCorrelationIdHeaderByGeneratingNewOne() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotBlank().isNotEqualTo("   ");
        assertThat(UUID.fromString(generatedId)).isNotNull();
        assertThat(mdcCorrelationId.get()).isEqualTo(generatedId);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("Deve limpar o MDC obrigatoriamente mesmo quando a cadeia de filtros lançar exceção")
    void shouldClearMdcEvenIfFilterChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, res) -> {
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNotNull();
            throw new ServletException("Erro simulado no processamento da requisição");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("Erro simulado");

        // MDC deve estar limpo mesmo após exceção
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }
}

