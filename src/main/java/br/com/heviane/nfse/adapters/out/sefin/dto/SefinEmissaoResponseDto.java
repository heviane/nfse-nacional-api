package br.com.heviane.nfse.adapters.out.sefin.dto;

import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para desserialização do JSON de resposta da emissão de DPS retornado pelo Sefin Nacional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SefinEmissaoResponseDto(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64,
        @JsonAlias({"protocolo", "nProt"})
        String numeroProtocolo
) {
    public RespostaEmissaoSefin paraDominio() {
        return new RespostaEmissaoSefin(
                tipoAmbiente,
                versaoAplicativo,
                dataHoraProcessamento,
                codigoStatus,
                motivoStatus,
                chaveAcesso,
                nfseXmlGZipB64,
                numeroProtocolo
        );
    }
}

