package br.com.heviane.nfse.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do pedido de cancelamento da NFS-e")
public record CancelamentoNfseResponse(
        @Schema(description = "Indica se o evento de cancelamento foi homologado/vinculado", example = "true")
        boolean sucesso,

        @Schema(description = "Código de status retornado pelo Sefin Nacional (100 ou 135)", example = "135")
        int codigoStatus,

        @Schema(description = "Descrição/motivo retornado pelo Sefin Nacional", example = "Evento registrado e vinculado a NFS-e")
        String motivoStatus,

        @Schema(description = "Chave de Acesso de 50 dígitos da NFS-e", example = "32052002601000000000019100001000000000000001123456")
        String chaveAcesso,

        @Schema(description = "Identificador único do evento gerado", example = "E1011013205200260100000000001910000100000000000000112345601")
        String idEvento,

        @Schema(description = "Conteúdo XML oficial do evento de cancelamento")
        String xmlEvento
) {}

