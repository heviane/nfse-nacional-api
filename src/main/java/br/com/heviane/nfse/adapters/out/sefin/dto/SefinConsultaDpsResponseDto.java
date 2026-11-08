package br.com.heviane.nfse.adapters.out.sefin.dto;

import br.com.heviane.nfse.core.domain.RespostaConsultaDps;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para desserialização do JSON de resposta da consulta de DPS por Id no Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SefinConsultaDpsResponseDto(
        @JsonProperty("idDps") String idDps,
        @JsonProperty("chaveAcesso") String chaveAcesso,
        @JsonProperty("dataHoraProcessamento") String dataHoraProcessamento,
        @JsonProperty("tipoAmbiente") @JsonAlias("ambiente") Integer tipoAmbiente
) {
    public RespostaConsultaDps paraDominio() {
        return new RespostaConsultaDps(
                idDps,
                chaveAcesso,
                dataHoraProcessamento,
                tipoAmbiente
        );
    }
}

