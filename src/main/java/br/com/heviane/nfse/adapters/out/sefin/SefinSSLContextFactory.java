package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fábrica responsável por instanciar, inicializar e gerenciar o cache de instâncias de {@link SSLContext}
 * TLS diretamente a partir das chaves do {@link CertificadoA1} em memória, permitindo autenticação mTLS
 * (Mutual TLS) estritamente sem persistência de certificados em disco e com suporte a TrustStore dedicado.
 */
public final class SefinSSLContextFactory {

    private static final Map<String, SSLContext> SSL_CONTEXT_CACHE = new ConcurrentHashMap<>();

    private SefinSSLContextFactory() {
    }

    /**
     * Retorna uma instância em cache ou cria um novo {@link SSLContext} para o certificado informado.
     */
    public static SSLContext obterOuCriarSslContext(CertificadoA1 certificado) {
        return obterOuCriarSslContext(certificado, null);
    }

    /**
     * Retorna uma instância em cache ou cria um novo {@link SSLContext} para o certificado e TrustStore informados.
     */
    public static SSLContext obterOuCriarSslContext(CertificadoA1 certificado, KeyStore trustStore) {
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para inicialização do SSLContext.");
        }
        String cacheKey = certificado.getHashConteudo() + (trustStore != null ? "_" + trustStore.hashCode() : "_default");
        return SSL_CONTEXT_CACHE.computeIfAbsent(cacheKey, k -> criarSslContext(certificado, trustStore));
    }

    public static SSLContext criarSslContext(CertificadoA1 certificado) {
        return criarSslContext(certificado, (KeyStore) null);
    }

    public static SSLContext criarSslContext(CertificadoA1 certificado, KeyStore trustStore) {
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para inicialização do SSLContext.");
        }

        try {
            KeyStore keyStore = certificado.carregarKeyStore();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, certificado.getSenha());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = (trustStore != null) ? trustStore : resolverTrustStoreConfigurado();
            tmf.init(ts); // Se ts for null, o Java utiliza o cacerts padrão da JVM

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            return sslContext;
        } catch (Exception e) {
            throw new SefinTransmissaoException("Falha ao inicializar contexto mTLS com o certificado A1: " + e.getMessage(), e);
        }
    }

    public static void limparCache() {
        SSL_CONTEXT_CACHE.clear();
    }

    private static KeyStore resolverTrustStoreConfigurado() {
        String path = System.getenv("NFSE_TRUSTSTORE_PATH");
        if (path == null || path.isBlank()) {
            path = System.getProperty("nfse.truststore.path");
        }
        if (path == null || path.isBlank()) {
            return null; // fallback para cacerts padrão da JVM via tmf.init((KeyStore) null)
        }

        Path trustPath = Path.of(path.trim());
        if (!Files.exists(trustPath)) {
            throw new IllegalStateException("Arquivo de TrustStore configurado em NFSE_TRUSTSTORE_PATH não encontrado: " + path);
        }

        String password = System.getenv("NFSE_TRUSTSTORE_PASSWORD");
        if (password == null) {
            password = System.getProperty("nfse.truststore.password", "changeit");
        }

        String type = System.getenv("NFSE_TRUSTSTORE_TYPE");
        if (type == null || type.isBlank()) {
            type = System.getProperty("nfse.truststore.type", "JKS");
        }

        try (InputStream is = Files.newInputStream(trustPath)) {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(is, password.toCharArray());
            return ks;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar TrustStore configurado em " + path + ": " + e.getMessage(), e);
        }
    }
}

