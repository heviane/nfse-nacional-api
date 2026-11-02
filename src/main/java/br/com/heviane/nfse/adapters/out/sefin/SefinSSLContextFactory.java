package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Fábrica responsável por instanciar e inicializar o {@link SSLContext} TLSv1.2+
 * diretamente a partir das chaves do {@link CertificadoA1} em memória, permitindo
 * autenticação mTLS (Mutual TLS) estritamente sem persistência de certificados em disco.
 */
public final class SefinSSLContextFactory {

    private SefinSSLContextFactory() {
    }

    public static SSLContext criarSslContext(CertificadoA1 certificado) {
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para inicialização do SSLContext.");
        }

        try {
            KeyStore keyStore = certificado.carregarKeyStore();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, certificado.getSenha());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            return sslContext;
        } catch (Exception e) {
            throw new SefinTransmissaoException("Falha ao inicializar contexto mTLS com o certificado A1: " + e.getMessage(), e);
        }
    }
}
