package br.com.heviane.nfse.core.domain;

import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: CertificadoA1 (Cache, Hash e Higiene de Senha)")
class CertificadoA1Test {

    @Test
    @DisplayName("Deve calcular hash SHA-256 consistente para os mesmos bytes")
    void deveCalcularHashSha256Consistente() {
        CertificadoA1 cert1 = CertificadoTesteHelper.obterCertificadoTeste();
        String hash1 = cert1.getHashConteudo();

        assertThat(hash1).isNotNull().matches("[a-f0-9]{64}");
    }

    @Test
    @DisplayName("Deve fazer cache de KeyStore, PrivateKey e X509Certificate sem recarregar PKCS#12")
    void deveFazerCacheDeChaves() {
        CertificadoA1 cert = CertificadoTesteHelper.obterCertificadoTeste();

        KeyStore ks1 = cert.carregarKeyStore();
        KeyStore ks2 = cert.carregarKeyStore();
        assertThat(ks1).isSameAs(ks2);

        String alias1 = cert.getAliasPrincipal();
        String alias2 = cert.getAliasPrincipal();
        assertThat(alias1).isEqualTo(alias2);

        PrivateKey pk1 = cert.getPrivateKey();
        PrivateKey pk2 = cert.getPrivateKey();
        assertThat(pk1).isSameAs(pk2);

        X509Certificate x509_1 = cert.getX509Certificate();
        X509Certificate x509_2 = cert.getX509Certificate();
        assertThat(x509_1).isSameAs(x509_2);
    }

    @Test
    @DisplayName("Deve efetuar zeroing de buffers e invalidar caches ao chamar limpar()")
    void deveEfetuarZeroingELimparBuffers() throws Exception {
        CertificadoA1 certBase = CertificadoTesteHelper.obterCertificadoTeste();
        byte[] bytesCopia = certBase.getInputStream().readAllBytes();

        CertificadoA1 cert = new CertificadoA1(bytesCopia, "testpassword123");

        assertThat(cert.getSenha()).isEqualTo("testpassword123".toCharArray());
        assertThat(cert.getPrivateKey()).isNotNull();

        cert.limpar();

        char[] senhaAposLimpeza = cert.getSenha();
        for (char c : senhaAposLimpeza) {
            assertThat(c).isEqualTo('\0');
        }
    }

    @Test
    @DisplayName("Deve implementar AutoCloseable e limpar ao fechar no try-with-resources")
    void deveImplementarAutoCloseable() throws Exception {
        CertificadoA1 certBase = CertificadoTesteHelper.obterCertificadoTeste();
        byte[] bytesCopia = certBase.getInputStream().readAllBytes();

        CertificadoA1 cert;
        try (CertificadoA1 c = new CertificadoA1(bytesCopia, "testpassword123")) {
            cert = c;
            assertThat(cert.getPrivateKey()).isNotNull();
        }

        char[] senhaAposFechamento = cert.getSenha();
        for (char c : senhaAposFechamento) {
            assertThat(c).isEqualTo('\0');
        }
    }

    @Test
    @DisplayName("Deve rejeitar inicialização com bytes nulos ou senha nula")
    void deveRejeitarParametrosNulos() {
        assertThatThrownBy(() -> new CertificadoA1(null, "senha"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Bytes do certificado não podem ser nulos");

        assertThatThrownBy(() -> new CertificadoA1(new byte[]{1, 2, 3}, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Senha não pode ser nula");
    }
}

