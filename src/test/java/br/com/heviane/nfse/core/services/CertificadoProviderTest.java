package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: Provedor de Certificado A1 e Blindagem de Headers")
class CertificadoProviderTest {

    @Test
    @DisplayName("Por padrão (allowCertHeaders=false), DEVE rejeitar o envio de certificado via headers HTTP")
    void deveRejeitarEnvioDeCertificadoViaHeadersPorPadrao() {
        CertificadoProvider provider = new CertificadoProvider(false);

        assertThatThrownBy(() -> provider.obterCertificado("MII...", "123456"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("desabilitado neste servidor por diretriz de segurança fiscal");
    }

    @Test
    @DisplayName("Quando explicitamente habilitado (allowCertHeaders=true), permite decodificação via headers")
    void devePermitirCertificadoViaHeadersQuandoHabilitado() throws Exception {
        CertificadoProvider provider = new CertificadoProvider(true);
        CertificadoA1 certMock = CertificadoTesteHelper.obterCertificadoTeste();

        byte[] certBytes = certMock.getInputStream().readAllBytes();
        String certB64 = Base64.getEncoder().encodeToString(certBytes);

        CertificadoA1 resolvido = provider.obterCertificado(certB64, "123456");

        assertThat(resolvido).isNotNull();
        assertThat(resolvido.getSenha()).isEqualTo("123456".toCharArray());
    }

    @Test
    @DisplayName("Deve carregar certificado a partir de propriedade de sistema quando headers não forem usados")
    void deveCarregarCertificadoDeVariaveisDeAmbiente() throws Exception {
        CertificadoA1 certMock = CertificadoTesteHelper.obterCertificadoTeste();
        byte[] certBytes = certMock.getInputStream().readAllBytes();
        String certB64 = Base64.getEncoder().encodeToString(certBytes);

        System.setProperty("nfse.cert.base64", certB64);
        System.setProperty("nfse.cert.password", "123456");

        try {
            CertificadoProvider provider = new CertificadoProvider(false);
            CertificadoA1 resolvido = provider.obterCertificado(null, null);

            assertThat(resolvido).isNotNull();
            assertThat(resolvido.getSenha()).isEqualTo("123456".toCharArray());
        } finally {
            System.clearProperty("nfse.cert.base64");
            System.clearProperty("nfse.cert.password");
        }
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando nenhum certificado for configurado")
    void deveLancarExcecaoQuandoNenhumCertificadoConfigurado() {
        System.clearProperty("nfse.cert.base64");
        System.clearProperty("nfse.cert.password");

        CertificadoProvider provider = new CertificadoProvider(false);

        assertThatThrownBy(() -> provider.obterCertificado(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum Certificado Digital A1 foi configurado");
    }
}

