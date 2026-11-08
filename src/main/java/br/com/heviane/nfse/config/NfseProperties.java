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

    public NfseProperties() {}

    public NfseProperties(AmbienteSefin environment) {
        this.environment = (environment != null) ? environment : AmbienteSefin.HOMOLOGACAO;
    }

    public AmbienteSefin getEnvironment() {
        return environment;
    }

    public void setEnvironment(AmbienteSefin environment) {
        this.environment = (environment != null) ? environment : AmbienteSefin.HOMOLOGACAO;
    }

    /**
     * Retorna o ambiente configurado, garantindo fallback para HOMOLOGACAO caso nulo.
     */
    public AmbienteSefin resolverAmbiente() {
        return environment != null ? environment : AmbienteSefin.HOMOLOGACAO;
    }
}
