package br.com.heviane.nfse.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Modelo de domínio para a resposta síncrona de emissão retornada pelo Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaEmissaoSefin(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64
) {
    public boolean isSucesso() {
        return codigoStatus == 100;
    }

    /**
     * Decodifica e descompacta o XML oficial da NFS-e autorizada.
     */
    public String obterNfseXmlDescompactado() {
        if (nfseXmlGZipB64 == null || nfseXmlGZipB64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(nfseXmlGZipB64.replaceAll("\\s+", ""));
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descompactar XML da NFS-e autorizada: " + e.getMessage(), e);
        }
    }
}

