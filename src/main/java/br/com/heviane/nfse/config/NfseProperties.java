package br.com.heviane.nfse.config;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades centralizadas do ecossistema NFS-e Nacional via Spring Boot {@link ConfigurationProperties}.
 * Mapeia propriedades prefixadas com {@code nfse.*} (ex: {@code nfse.environment}).
 */
@ConfigurationProperties(prefix = "nfse")
public class NfseProperties {

    /**
     * Ambiente alvo do Sefin Nacional (HOMOLOGACAO ou PRODUCAO).
     */
    private AmbienteSefin environment = AmbienteSefin.HOMOLOGACAO;

    /**
     * Configurações de infraestrutura do Certificado Digital ICP-Brasil A1.
     */
    private CertificadoProperties cert = new CertificadoProperties();

    /**
     * Configurações de TrustStore dedicado para validação da cadeia de confiança ICP-Brasil/gov.br.
     */
    private TruststoreProperties truststore = new TruststoreProperties();

    public NfseProperties() {}

    public NfseProperties(AmbienteSefin environment) {
        this(environment, new CertificadoProperties(), new TruststoreProperties());
    }

    public NfseProperties(AmbienteSefin environment, CertificadoProperties cert) {
        this(environment, cert, new TruststoreProperties());
    }

    public NfseProperties(AmbienteSefin environment, CertificadoProperties cert, TruststoreProperties truststore) {
        this.environment = (environment != null) ? environment : AmbienteSefin.HOMOLOGACAO;
        this.cert = (cert != null) ? cert : new CertificadoProperties();
        this.truststore = (truststore != null) ? truststore : new TruststoreProperties();
    }

    public AmbienteSefin getEnvironment() {
        return environment;
    }

    public void setEnvironment(AmbienteSefin environment) {
        this.environment = (environment != null) ? environment : AmbienteSefin.HOMOLOGACAO;
    }

    public CertificadoProperties getCert() {
        return cert;
    }

    public void setCert(CertificadoProperties cert) {
        this.cert = (cert != null) ? cert : new CertificadoProperties();
    }

    public TruststoreProperties getTruststore() {
        return truststore;
    }

    public void setTruststore(TruststoreProperties truststore) {
        this.truststore = (truststore != null) ? truststore : new TruststoreProperties();
    }

    /**
     * Retorna o ambiente configurado, garantindo fallback para HOMOLOGACAO caso nulo.
     */
    public AmbienteSefin resolverAmbiente() {
        return environment != null ? environment : AmbienteSefin.HOMOLOGACAO;
    }

    /**
     * Propriedades tipadas para resolução do Certificado Digital A1.
     */
    public static class CertificadoProperties {
        private boolean allowHeaders = false;
        private String path;
        private String base64;
        private String password = "";

        public CertificadoProperties() {}

        public CertificadoProperties(boolean allowHeaders, String path, String base64, String password) {
            this.allowHeaders = allowHeaders;
            this.path = path;
            this.base64 = base64;
            this.password = (password != null) ? password : "";
        }

        public boolean isAllowHeaders() {
            return allowHeaders;
        }

        public void setAllowHeaders(boolean allowHeaders) {
            this.allowHeaders = allowHeaders;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = (password != null) ? password : "";
        }
    }

    /**
     * Propriedades para configuração de TrustStore dedicado (ex: cadeias de certificados ICP-Brasil/gov.br).
     */
    public static class TruststoreProperties {
        private String path;
        private String password = "";
        private String type = "JKS";

        public TruststoreProperties() {}

        public TruststoreProperties(String path, String password, String type) {
            this.path = path;
            this.password = (password != null) ? password : "";
            this.type = (type != null && !type.isBlank()) ? type : "JKS";
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = (password != null) ? password : "";
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = (type != null && !type.isBlank()) ? type : "JKS";
        }

        public boolean isConfigurado() {
            return path != null && !path.isBlank();
        }
    }
}
