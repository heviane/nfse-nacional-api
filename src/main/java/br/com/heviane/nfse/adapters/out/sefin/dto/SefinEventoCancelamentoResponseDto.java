package br.com.heviane.nfse.adapters.out.sefin.dto;

import br.com.heviane.nfse.core.domain.RespostaEventoCancelamento;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para desserialização do JSON de resposta do evento de cancelamento no Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SefinEventoCancelamentoResponseDto(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String idEvento,
        String eventoXmlGZipB64
) {
    public RespostaEventoCancelamento paraDominio() {
        return new RespostaEventoCancelamento(
                tipoAmbiente,
                versaoAplicativo,
                dataHoraProcessamento,
                codigoStatus,
                motivoStatus,
                chaveAcesso,
                idEvento,
                eventoXmlGZipB64
        );
    }
}

