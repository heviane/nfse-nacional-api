package br.com.heviane.nfse.core.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa a resposta da consulta de uma DPS no Sefin Nacional por seu Identificador único (IdDps).
 * Utilizado para reconciliação e garantia de idempotência em caso de timeout de rede ou status 204 (duplicidade).
 *
 * @param idDps                Identificador único da DPS (ex: DPS3205200...)
 * @param chaveAcesso          Chave de acesso de 50 dígitos da NFS-e correspondente gerada pelo Sefin
 * @param dataHoraProcessamento Data e hora do processamento no Sefin Nacional
 * @param tipoAmbiente         Ambiente de processamento (1 = Produção, 2 = Homologação)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaConsultaDps(
        @JsonProperty("idDps") String idDps,
        @JsonProperty("chaveAcesso") String chaveAcesso,
        @JsonProperty("dataHoraProcessamento") String dataHoraProcessamento,
        @JsonProperty("tipoAmbiente") @JsonAlias("ambiente") Integer tipoAmbiente
) {
    public boolean isSucesso() {
        return chaveAcesso != null && !chaveAcesso.isBlank();
    }
}

