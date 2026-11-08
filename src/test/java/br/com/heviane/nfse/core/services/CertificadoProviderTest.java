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
    @DisplayName("Deve carregar certificado a partir de arquivo em disco configurado em nfse.cert.path")
    void deveCarregarCertificadoAPartirDeArquivoEmDiscoViaCertPath() throws Exception {
        CertificadoA1 certMock = CertificadoTesteHelper.obterCertificadoTeste();
        byte[] certBytes = certMock.getInputStream().readAllBytes();

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("cert-teste-", ".pfx");
        java.nio.file.Files.write(tempFile, certBytes);

        System.setProperty("nfse.cert.path", tempFile.toAbsolutePath().toString());
        System.setProperty("nfse.cert.password", "123456");

        try {
            CertificadoProvider provider = new CertificadoProvider(false);
            CertificadoA1 resolvido = provider.obterCertificado(null, null);

            assertThat(resolvido).isNotNull();
            assertThat(resolvido.getSenha()).isEqualTo("123456".toCharArray());
        } finally {
            System.clearProperty("nfse.cert.path");
            System.clearProperty("nfse.cert.password");
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando arquivo configurado em nfse.cert.path não existir")
    void deveLancarExcecaoQuandoArquivoCertPathNaoExistir() {
        System.setProperty("nfse.cert.path", java.nio.file.Path.of("target", "caminho_inexistente_cert.pfx").toString());

        try {
            CertificadoProvider provider = new CertificadoProvider(false);

            assertThatThrownBy(() -> provider.obterCertificado(null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não encontrado no caminho configurado (NFSE_CERT_PATH)");
        } finally {
            System.clearProperty("nfse.cert.path");
        }
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando nenhum certificado for configurado")
    void deveLancarExcecaoQuandoNenhumCertificadoConfigurado() {
        System.clearProperty("nfse.cert.path");
        System.clearProperty("nfse.cert.base64");
        System.clearProperty("nfse.cert.password");

        CertificadoProvider provider = new CertificadoProvider(false);

        assertThatThrownBy(() -> provider.obterCertificado(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum Certificado Digital A1 foi configurado");
    }
}

