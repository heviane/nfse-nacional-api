package br.com.heviane.nfse.core.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Modelo de domínio para a resposta de emissão retornada pelo Sefin Nacional.
 * Suporta retornos síncronos (autorização imediata, status 100) e assíncronos (em fila/lote, status 105).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaEmissaoSefin(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64,
        @JsonAlias({"protocolo", "nProt"})
        String numeroProtocolo
) {
    public RespostaEmissaoSefin(int tipoAmbiente,
                                String versaoAplicativo,
                                String dataHoraProcessamento,
                                int codigoStatus,
                                String motivoStatus,
                                String chaveAcesso,
                                String nfseXmlGZipB64) {
        this(tipoAmbiente, versaoAplicativo, dataHoraProcessamento, codigoStatus, motivoStatus, chaveAcesso, nfseXmlGZipB64, null);
    }

    /**
     * Indica se a NFS-e foi autorizada de forma síncrona com sucesso (status 100).
     */
    public boolean isSucesso() {
        return codigoStatus == 100;
    }

    /**
     * Indica se a DPS/Lote foi recebido pelo Sefin Nacional e colocado em fila de processamento assíncrono (status 105).
     */
    public boolean isEmProcessamento() {
        return codigoStatus == 105;
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
