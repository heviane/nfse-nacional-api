package br.com.heviane.nfse.adapters.in.rest.dto;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requisição para validação, assinatura e emissão da DPS")
public record EmissaoDpsRequest(
        @Schema(description = "Conteúdo XML completo da DPS no namespace oficial", example = "<DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\">...</DPS>")
        String xmlDps,

        @Schema(description = "Ambiente do Sefin Nacional (HOMOLOGACAO ou PRODUCAO)", example = "HOMOLOGACAO")
        AmbienteSefin ambiente
) {}

