package br.com.heviane.nfse.core.domain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

/**
 * Objeto de domínio que encapsula um Certificado Digital ICP-Brasil A1 (PKCS#12)
 * manipulado estritamente em memória, garantindo que chaves privadas e senhas
 * nunca sejam persistidas em disco desprotegidas.
 * <p>
 * Implementa cache interno thread-safe das chaves e {@link AutoCloseable} para
 * higiene de memória e zeroing de senhas.
 */
public class CertificadoA1 implements AutoCloseable {

    private final byte[] arquivoBytes;
    private final char[] senha;
    private final String hashConteudo;

    private volatile KeyStore keyStoreCache;
    private volatile String aliasPrincipalCache;
    private volatile PrivateKey privateKeyCache;
    private volatile X509Certificate x509CertificateCache;

    public CertificadoA1(byte[] arquivoBytes, String senha) {
        this.arquivoBytes = Objects.requireNonNull(arquivoBytes, "Bytes do certificado não podem ser nulos").clone();
        this.senha = Objects.requireNonNull(senha, "Senha não pode ser nula").toCharArray();
        this.hashConteudo = calcularSha256(this.arquivoBytes);
    }

    private static String calcularSha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash SHA-256 do certificado: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna o fingerprint SHA-256 único do arquivo PKCS#12 para indexação em cache.
     */
    public String getHashConteudo() {
        return hashConteudo;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(arquivoBytes);
    }

    public char[] getSenha() {
        return senha.clone();
    }

    /**
     * Carrega a KeyStore PKCS12 diretamente da memória, reutilizando a instância em cache se já carregada.
     */
    public synchronized KeyStore carregarKeyStore() {
        if (keyStoreCache != null) {
            return keyStoreCache;
        }
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream is = getInputStream()) {
                ks.load(is, senha);
            }
            this.keyStoreCache = ks;
            return ks;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao carregar certificado digital A1 (PKCS#12) em memória. Verifique se o arquivo e a senha informada estão corretos.");
        }
    }

    /**
     * Recupera o alias principal da chave privada no keystore (reutiliza cache se já inspecionado).
     */
    public synchronized String getAliasPrincipal() {
        if (aliasPrincipalCache != null) {
            return aliasPrincipalCache;
        }
        try {
            KeyStore ks = carregarKeyStore();
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    this.aliasPrincipalCache = alias;
                    return alias;
                }
            }
            throw new AssinaturaDigitalException("Nenhuma entrada de chave privada encontrada no certificado PKCS#12.");
        } catch (AssinaturaDigitalException e) {
            throw e;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Erro ao inspecionar aliases do certificado: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém a chave privada (PrivateKey) para operação de assinatura digital (reutiliza cache se já extraída).
     */
    public synchronized PrivateKey getPrivateKey() {
        if (privateKeyCache != null) {
            return privateKeyCache;
        }
        try {
            KeyStore ks = carregarKeyStore();
            String alias = getAliasPrincipal();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, senha);
            this.privateKeyCache = pk;
            return pk;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao extrair chave privada do certificado: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém o certificado público X.509 para inclusão no KeyInfo da assinatura (reutiliza cache se já extraído).
     */
    public synchronized X509Certificate getX509Certificate() {
        if (x509CertificateCache != null) {
            return x509CertificateCache;
        }
        try {
            KeyStore ks = carregarKeyStore();
            String alias = getAliasPrincipal();
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            this.x509CertificateCache = cert;
            return cert;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao extrair certificado público X.509: " + e.getMessage(), e);
        }
    }

    /**
     * Limpa os dados sensíveis (senha e arquivo PKCS#12) da memória sobrescrevendo seus buffers.
     */
    public synchronized void limpar() {
        if (senha != null) {
            Arrays.fill(senha, '\0');
        }
        if (arquivoBytes != null) {
            Arrays.fill(arquivoBytes, (byte) 0);
        }
        this.keyStoreCache = null;
        this.aliasPrincipalCache = null;
        this.privateKeyCache = null;
        this.x509CertificateCache = null;
    }

    @Override
    public void close() {
        limpar();
    }
}

