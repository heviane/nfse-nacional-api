package br.com.heviane.nfse.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Modelo de domínio para a resposta de consulta de NFS-e por Chave de Acesso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaConsultaNfse(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64
) {
    public boolean isEncontrada() {
        return codigoStatus == 100;
    }

    /**
     * Decodifica e descompacta o XML oficial da NFS-e retornada.
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
            throw new RuntimeException("Falha ao descompactar XML da NFS-e consultada: " + e.getMessage(), e);
        }
    }
}

