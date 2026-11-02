package br.com.heviane.nfse.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do processamento e autorização da DPS no Sefin Nacional")
public record EmissaoDpsResponse(
        @Schema(description = "Indica se a NFS-e foi autorizada com sucesso (status 100)", example = "true")
        boolean sucesso,

        @Schema(description = "Código de status retornado pelo Sefin Nacional", example = "100")
        int codigoStatus,

        @Schema(description = "Descrição/motivo retornado pelo Sefin Nacional", example = "NFS-e gerada com sucesso")
        String motivoStatus,

        @Schema(description = "Chave de Acesso de 50 dígitos da NFS-e autorizada", example = "32052002601000000000019100001000000000000001123456")
        String chaveAcesso,

        @Schema(description = "Conteúdo XML oficial e assinado da NFS-e autorizada")
        String xmlNfse
) {}

