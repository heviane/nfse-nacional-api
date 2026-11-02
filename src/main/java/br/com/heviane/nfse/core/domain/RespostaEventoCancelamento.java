package br.com.heviane.nfse.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Modelo de domínio para o retorno do registro de evento de cancelamento pelo Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaEventoCancelamento(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String idEvento,
        String eventoXmlGZipB64
) {
    public boolean isSucesso() {
        return codigoStatus == 100 || codigoStatus == 135; // 135: Evento registrado e vinculado
    }

    public String obterEventoXmlDescompactado() {
        if (eventoXmlGZipB64 == null || eventoXmlGZipB64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(eventoXmlGZipB64.replaceAll("\\s+", ""));
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descompactar XML do evento de cancelamento: " + e.getMessage(), e);
        }
    }
}
