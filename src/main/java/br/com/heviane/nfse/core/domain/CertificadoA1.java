package br.com.heviane.nfse.core.domain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Objects;

/**
 * Objeto de domínio que encapsula um Certificado Digital ICP-Brasil A1 (PKCS#12)
 * manipulado estritamente em memória, garantindo que chaves privadas e senhas
 * nunca sejam persistidas em disco desprotegidas.
 */
public class CertificadoA1 {

    private final byte[] arquivoBytes;
    private final char[] senha;

    public CertificadoA1(byte[] arquivoBytes, String senha) {
        this.arquivoBytes = Objects.requireNonNull(arquivoBytes, "Bytes do certificado não podem ser nulos").clone();
        this.senha = Objects.requireNonNull(senha, "Senha não pode ser nula").toCharArray();
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(arquivoBytes);
    }

    public char[] getSenha() {
        return senha.clone();
    }

    /**
     * Carrega a KeyStore PKCS12 diretamente da memória.
     */
    public KeyStore carregarKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream is = getInputStream()) {
                ks.load(is, senha);
            }
            return ks;
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao carregar certificado digital A1 (PKCS#12) em memória. Verifique se o arquivo e a senha informada estão corretos.");
        }
    }

    /**
     * Recupera o alias principal da chave privada no keystore.
     */
    public String getAliasPrincipal() {
        try {
            KeyStore ks = carregarKeyStore();
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
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
     * Obtém a chave privada (PrivateKey) para operação de assinatura digital.
     */
    public PrivateKey getPrivateKey() {
        try {
            KeyStore ks = carregarKeyStore();
            String alias = getAliasPrincipal();
            return (PrivateKey) ks.getKey(alias, senha);
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao extrair chave privada do certificado: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém o certificado público X.509 para inclusão no KeyInfo da assinatura.
     */
    public X509Certificate getX509Certificate() {
        try {
            KeyStore ks = carregarKeyStore();
            String alias = getAliasPrincipal();
            return (X509Certificate) ks.getCertificate(alias);
        } catch (Exception e) {
            throw new AssinaturaDigitalException("Falha ao extrair certificado público X.509: " + e.getMessage(), e);
        }
    }
}

