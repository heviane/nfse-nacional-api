package br.com.heviane.nfse.core.domain;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Utilitário puro Java para manipulação e descompactação de payloads GZIP codificados em Base64,
 * padrão adotado nas comunicações fiscais com o Sefin Nacional / ADN.
 */
public final class GzipUtils {

    private GzipUtils() {}

    /**
     * Decodifica o Base64 e descompacta o fluxo GZIP em texto UTF-8.
     *
     * @param gzipBase64 Conteúdo Base64 do payload compactado em GZIP
     * @return Conteúdo textual descompactado (ex: XML) ou null se nulo/em branco
     */
    public static String descompactarGzipBase64(String gzipBase64) {
        if (gzipBase64 == null || gzipBase64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(gzipBase64.replaceAll("\\s+", ""));
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descompactar payload GZIP/Base64: " + e.getMessage(), e);
        }
    }
}

