package br.com.heviane.nfse.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Estrutura padronizada para retorno de erros da API")
public record ErroResponse(
        @Schema(description = "Mensagem descritiva do erro", example = "Erro de validação fiscal do documento")
        String mensagem,

        @Schema(description = "Lista detalhada de inconsistências ou erros encontrados")
        List<String> detalhes,

        @Schema(description = "Código de status HTTP correspondente", example = "422")
        int statusHttp,

        @Schema(description = "Data e hora da ocorrência do erro")
        Instant timestamp
) {
    public ErroResponse(String mensagem, List<String> detalhes, int statusHttp) {
        this(mensagem, detalhes != null ? detalhes : List.of(), statusHttp, Instant.now());
    }

    public ErroResponse(String mensagem, int statusHttp) {
        this(mensagem, List.of(), statusHttp, Instant.now());
    }
}

