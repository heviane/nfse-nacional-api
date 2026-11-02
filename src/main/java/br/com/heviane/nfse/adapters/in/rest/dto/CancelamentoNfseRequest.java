package br.com.heviane.nfse.adapters.in.rest.dto;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requisição para registro de cancelamento de NFS-e")
public record CancelamentoNfseRequest(
        @Schema(description = "Chave de acesso de 50 dígitos da NFS-e a cancelar", example = "32052001236030278000159000000003203726110114333510")
        String chaveAcesso,

        @Schema(description = "CNPJ de 14 dígitos do emitente/autor do pedido", example = "36030278000159")
        String cnpjAutor,

        @Schema(description = "Código do motivo de cancelamento (ex: 1 - Erro na Emissão)", example = "1")
        String codigoMotivo,

        @Schema(description = "Justificativa detalhada do cancelamento (mínimo 15 caracteres)", example = "Cancelamento solicitado devido a erro no preenchimento do tomador")
        String motivo,

        @Schema(description = "Ambiente do Sefin Nacional (HOMOLOGACAO ou PRODUCAO)", example = "HOMOLOGACAO")
        AmbienteSefin ambiente
) {}

