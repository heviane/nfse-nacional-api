package br.com.heviane.nfse.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: GzipUtils")
class GzipUtilsTest {

    @Test
    @DisplayName("Deve descompactar payload GZIP codificado em Base64 com sucesso")
    void deveDescompactarGzipBase64ComSucesso() throws Exception {
        String textoOriginal = "<NFSe><infNFSe><nNFSe>123</nNFSe></infNFSe></NFSe>";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(textoOriginal.getBytes(StandardCharsets.UTF_8));
        }
        String gzipBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        String resultado = GzipUtils.descompactarGzipBase64(gzipBase64);

        assertThat(resultado).isEqualTo(textoOriginal);
    }

    @Test
    @DisplayName("Deve retornar null quando payload for nulo ou em branco")
    void deveRetornarNullParaPayloadNuloOuBranco() {
        assertThat(GzipUtils.descompactarGzipBase64(null)).isNull();
        assertThat(GzipUtils.descompactarGzipBase64("   ")).isNull();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando payload contiver dados corrompidos")
    void deveLancarExcecaoParaDadosCorrompidos() {
        assertThatThrownBy(() -> GzipUtils.descompactarGzipBase64("dados_invalidos_nao_gzip"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao descompactar payload GZIP/Base64");
    }
}

