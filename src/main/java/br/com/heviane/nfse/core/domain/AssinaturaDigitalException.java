package br.com.heviane.nfse.core.domain;

/**
 * Exceção lançada em caso de falha criptográfica durante a assinatura digital
 * de documentos fiscais (falha de algoritmo, chave corrompida, tag ausente, etc.).
 */
public class AssinaturaDigitalException extends RuntimeException {

    public AssinaturaDigitalException(String message) {
        super(message);
    }

    public AssinaturaDigitalException(String message, Throwable cause) {
        super(message, cause);
    }
}
