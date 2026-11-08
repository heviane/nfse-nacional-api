package br.com.heviane.nfse.adapters.out.sefin.dto;

import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para desserialização do JSON de resposta da consulta de NFS-e por chave no Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SefinConsultaNfseResponseDto(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64
) {
    public RespostaConsultaNfse paraDominio() {
        return new RespostaConsultaNfse(
                tipoAmbiente,
                versaoAplicativo,
                dataHoraProcessamento,
                codigoStatus,
                motivoStatus,
                chaveAcesso,
                nfseXmlGZipB64
        );
    }
}

