package br.com.heviane.nfse.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da consulta de NFS-e por Chave de Acesso")
public record ConsultaNfseResponse(
        @Schema(description = "Indica se a nota fiscal foi localizada no Sefin Nacional", example = "true")
        boolean localizada,

        @Schema(description = "Código de status retornado pelo Sefin Nacional", example = "100")
        int codigoStatus,

        @Schema(description = "Descrição/motivo retornado pelo Sefin Nacional", example = "NFS-e localizada com sucesso")
        String motivoStatus,

        @Schema(description = "Chave de Acesso de 50 dígitos da NFS-e", example = "32052001236030278000159000000003203726110114333510")
        String chaveAcesso,

        @Schema(description = "Conteúdo XML oficial da NFS-e")
        String xmlNfse
) {}

