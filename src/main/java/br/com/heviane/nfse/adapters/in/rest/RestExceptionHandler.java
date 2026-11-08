package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.core.domain.AssinaturaDigitalException;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;
import br.com.heviane.nfse.core.domain.ValidacaoFiscalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice(basePackages = "br.com.heviane.nfse.adapters.in.rest")
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ValidacaoFiscalException.class)
    public ResponseEntity<ErroResponse> handleValidacaoFiscal(ValidacaoFiscalException ex) {
        log.warn("Erro de validação fiscal: {}", ex.getMessage());
        ErroResponse erro = new ErroResponse(
                ex.getMessage(),
                ex.getErros(),
                HttpStatus.UNPROCESSABLE_ENTITY.value()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }

    @ExceptionHandler(SefinTransmissaoException.class)
    public ResponseEntity<ErroResponse> handleSefinTransmissao(SefinTransmissaoException ex) {
        log.error("Erro na comunicação com Sefin Nacional: HTTP {} - Status Sefin {} - {}",
                ex.getHttpStatusCode(), ex.getCodigoStatusSefin(), ex.getMessage());

        HttpStatus status;
        if (ex.getHttpStatusCode() != null) {
            if (ex.getHttpStatusCode() == 504) {
                status = HttpStatus.GATEWAY_TIMEOUT;
            } else if (ex.getHttpStatusCode() == 409 || (ex.getCodigoStatusSefin() != null && ex.getCodigoStatusSefin() == 204)) {
                status = HttpStatus.CONFLICT;
            } else if (ex.getHttpStatusCode() >= 400 && ex.getHttpStatusCode() < 500) {
                status = HttpStatus.BAD_REQUEST;
            } else {
                status = HttpStatus.BAD_GATEWAY;
            }
        } else if (ex.getCodigoStatusSefin() != null && ex.getCodigoStatusSefin() == 204) {
            status = HttpStatus.CONFLICT;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }

        List<String> detalhes = new ArrayList<>();
        if (ex.getHttpStatusCode() != null) {
            detalhes.add("Código retornado pelo Sefin: " + ex.getHttpStatusCode());
        }
        if (ex.getCodigoStatusSefin() != null) {
            detalhes.add("Status fiscal Sefin: " + ex.getCodigoStatusSefin());
        }

        ErroResponse erro = new ErroResponse(
                ex.getMessage(),
                detalhes,
                status.value()
        );
        return ResponseEntity.status(status).body(erro);
    }

    @ExceptionHandler(AssinaturaDigitalException.class)
    public ResponseEntity<ErroResponse> handleAssinaturaDigital(AssinaturaDigitalException ex) {
        log.error("Erro na assinatura digital do documento fiscal: {}", ex.getMessage());
        ErroResponse erro = new ErroResponse(
                "Falha na assinatura digital do documento fiscal. Verifique a validade do certificado digital A1 e da senha informada.",
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErroResponse> handleSecurity(SecurityException ex) {
        log.warn("Bloqueio de segurança na requisição: {}", ex.getMessage());
        ErroResponse erro = new ErroResponse(
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Parâmetro inválido na requisição: {}", ex.getMessage());
        ErroResponse erro = new ErroResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado ilegal / configuração ausente: {}", ex.getMessage());
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("certificado")
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.INTERNAL_SERVER_ERROR;

        ErroResponse erro = new ErroResponse(
                ex.getMessage(),
                status.value()
        );
        return ResponseEntity.status(status).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneralException(Exception ex) {
        String correlationId = org.slf4j.MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.error("Erro interno não tratado [CorrelationId: {}]: {}", correlationId, ex.getMessage(), ex);
        ErroResponse erro = new ErroResponse(
                "Erro interno ao processar a requisição no middleware. Identificador de rastreamento: " + (correlationId != null ? correlationId : "N/A"),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
