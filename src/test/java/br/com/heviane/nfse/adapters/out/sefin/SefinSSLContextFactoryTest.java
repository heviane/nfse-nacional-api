package br.com.heviane.nfse.adapters.out.sefin;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: SSLContext mTLS em Memória (COM-01)")
class SefinSSLContextFactoryTest {

    @Test
    @DisplayName("Deve inicializar SSLContext TLS com chaves do Certificado A1 em memória")
    void deveInicializarSslContextComCertificadoA1() {
        CertificadoA1 certificado = CertificadoTesteHelper.obterCertificadoTeste();

        SSLContext sslContext = SefinSSLContextFactory.criarSslContext(certificado);

        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");
        assertThat(sslContext.getClientSessionContext()).isNotNull();
    }

    @Test
    @DisplayName("Deve reutilizar instância de SSLContext em cache para o mesmo certificado")
    void deveReutilizarInstanciaSslContextEmCache() {
        CertificadoA1 certificado = CertificadoTesteHelper.obterCertificadoTeste();

        SSLContext ctx1 = SefinSSLContextFactory.obterOuCriarSslContext(certificado);
        SSLContext ctx2 = SefinSSLContextFactory.obterOuCriarSslContext(certificado);

        assertThat(ctx1).isSameAs(ctx2);
    }

    @Test
    @DisplayName("Deve rejeitar inicialização caso o certificado seja nulo")
    void deveRejeitarCertificadoNulo() {
        assertThatThrownBy(() -> SefinSSLContextFactory.criarSslContext(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatório");
    }
}

