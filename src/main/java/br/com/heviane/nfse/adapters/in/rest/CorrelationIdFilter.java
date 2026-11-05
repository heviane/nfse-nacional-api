package br.com.heviane.nfse.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP para observabilidade e rastreabilidade distribuída (Correlation-ID).
 * <p>
 * Intercepta todas as requisições HTTP recebidas pela API, garantindo que:
 * <ul>
 *     <li>Cada requisição possua um identificador único de rastreamento (Correlation-ID);</li>
 *     <li>O cabeçalho {@code X-Correlation-ID} enviado pelo cliente ou gateway seja respeitado e preservado;</li>
 *     <li>Caso ausente, um novo UUID seja gerado automaticamente;</li>
 *     <li>O identificador seja injetado no contexto {@link MDC} do SLF4J para que apareça em todas as linhas de log;</li>
 *     <li>O identificador seja propagado no cabeçalho de resposta {@code X-Correlation-ID};</li>
 *     <li>O MDC seja obrigatoriamente limpo ao final da execução da requisição para evitar vazamento entre threads do pool.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);

        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (isNotBlank(correlationId)) {
            return correlationId.trim();
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (isNotBlank(requestId)) {
            return requestId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }
}

