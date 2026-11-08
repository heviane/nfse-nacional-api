package br.com.heviane.nfse.config;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários: NfseProperties")
class NfsePropertiesTest {

    @Test
    @DisplayName("Deve inicializar com ambiente padrão HOMOLOGACAO")
    void deveInicializarComPadraoHomologacao() {
        NfseProperties properties = new NfseProperties();

        assertThat(properties.getEnvironment()).isEqualTo(AmbienteSefin.HOMOLOGACAO);
        assertThat(properties.resolverAmbiente()).isEqualTo(AmbienteSefin.HOMOLOGACAO);
    }

    @Test
    @DisplayName("Deve permitir configurar ambiente PRODUCAO")
    void deveConfigurarAmbienteProducao() {
        NfseProperties properties = new NfseProperties(AmbienteSefin.PRODUCAO);

        assertThat(properties.getEnvironment()).isEqualTo(AmbienteSefin.PRODUCAO);
        assertThat(properties.resolverAmbiente()).isEqualTo(AmbienteSefin.PRODUCAO);
    }

    @Test
    @DisplayName("Deve fazer fallback seguro para HOMOLOGACAO quando ambiente for nulo")
    void deveFazerFallbackParaHomologacaoQuandoNulo() {
        NfseProperties properties = new NfseProperties();
        properties.setEnvironment(null);

        assertThat(properties.resolverAmbiente()).isEqualTo(AmbienteSefin.HOMOLOGACAO);
    }

    @Test
    @DisplayName("Deve inicializar certProperties com valores padrão seguros")
    void deveInicializarCertPropertiesComValoresPadrao() {
        NfseProperties properties = new NfseProperties();

        assertThat(properties.getCert()).isNotNull();
        assertThat(properties.getCert().isAllowHeaders()).isFalse();
        assertThat(properties.getCert().getPath()).isNull();
        assertThat(properties.getCert().getBase64()).isNull();
        assertThat(properties.getCert().getPassword()).isEmpty();
    }

    @Test
    @DisplayName("Deve permitir configurar e recuperar certProperties")
    void deveConfigurarERecuperarCertProperties() {
        NfseProperties properties = new NfseProperties();
        NfseProperties.CertificadoProperties cert = new NfseProperties.CertificadoProperties(
                true, "/tmp/cert.pfx", "MII...", "secret"
        );
        properties.setCert(cert);

        assertThat(properties.getCert().isAllowHeaders()).isTrue();
        assertThat(properties.getCert().getPath()).isEqualTo("/tmp/cert.pfx");
        assertThat(properties.getCert().getBase64()).isEqualTo("MII...");
        assertThat(properties.getCert().getPassword()).isEqualTo("secret");
    }
}

