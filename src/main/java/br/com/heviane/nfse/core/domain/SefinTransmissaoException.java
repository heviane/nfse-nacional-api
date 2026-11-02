package br.com.heviane.nfse.core.domain;

/**
 * Exceção lançada em caso de falha de transporte HTTP, rejeição do Sefin Nacional
 * ou erro no handshake mTLS.
 */
public class SefinTransmissaoException extends RuntimeException {

    private final Integer httpStatusCode;
    private final Integer codigoStatusSefin;
    private final String corpoResposta;

    public SefinTransmissaoException(String message, Integer httpStatusCode, Integer codigoStatusSefin, String corpoResposta) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.codigoStatusSefin = codigoStatusSefin;
        this.corpoResposta = corpoResposta;
    }

    public SefinTransmissaoException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
        this.codigoStatusSefin = null;
        this.corpoResposta = null;
    }

    public SefinTransmissaoException(String message) {
        this(message, null, null, null);
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public Integer getCodigoStatusSefin() {
        return codigoStatusSefin;
    }

    public String getCorpoResposta() {
        return corpoResposta;
    }
}

