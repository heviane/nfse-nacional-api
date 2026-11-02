package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.ConsultaNfseResponse;
import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import br.com.heviane.nfse.core.ports.in.ConsultarNfseUseCase;
import br.com.heviane.nfse.core.services.CertificadoProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nfse")
@Tag(name = "Consulta NFS-e", description = "Consulta de Notas Fiscais de Serviços eletrônicas autorizadas por Chave de Acesso no Sefin Nacional")
public class ConsultaNfseController {

    private final ConsultarNfseUseCase consultarNfseUseCase;
    private final CertificadoProvider certificadoProvider;

    public ConsultaNfseController(ConsultarNfseUseCase consultarNfseUseCase, CertificadoProvider certificadoProvider) {
        this.consultarNfseUseCase = consultarNfseUseCase;
        this.certificadoProvider = certificadoProvider;
    }

    @GetMapping(value = "/{chaveAcesso}", produces = "application/json")
    @Operation(
            summary = "Consultar NFS-e por Chave de Acesso",
            description = "Consulta uma NFS-e previamente autorizada no Ambiente Nacional pela sua Chave de Acesso de 50 dígitos numéricos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "NFS-e localizada com sucesso",
                    content = @Content(schema = @Schema(implementation = ConsultaNfseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Chave de acesso com formato inválido ou ausência de certificado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "502", description = "Falha de comunicação mTLS com o Sefin Nacional",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<ConsultaNfseResponse> consultar(
            @Parameter(description = "Chave de Acesso da NFS-e composta por exatamente 50 dígitos numéricos", example = "32052001236030278000159000000003203726110114333510")
            @PathVariable("chaveAcesso") String chaveAcesso,
            @Parameter(description = "Ambiente do Sefin Nacional (HOMOLOGACAO ou PRODUCAO)")
            @RequestParam(value = "ambiente", defaultValue = "HOMOLOGACAO") AmbienteSefin ambiente,
            @Parameter(in = ParameterIn.HEADER, description = "Certificado Digital A1 (.pfx/.p12) codificado em Base64")
            @RequestHeader(value = "X-Certificado-Base64", required = false) String certBase64,
            @Parameter(in = ParameterIn.HEADER, description = "Senha de proteção do Certificado Digital A1")
            @RequestHeader(value = "X-Certificado-Senha", required = false) String certSenha
    ) {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso deve conter exatamente 50 dígitos numéricos.");
        }

        CertificadoA1 certificado = certificadoProvider.obterCertificado(certBase64, certSenha);
        AmbienteSefin amb = ambiente != null ? ambiente : AmbienteSefin.HOMOLOGACAO;

        RespostaConsultaNfse resposta = consultarNfseUseCase.consultar(chaveAcesso, amb, certificado);

        ConsultaNfseResponse response = new ConsultaNfseResponse(
                resposta.isEncontrada(),
                resposta.codigoStatus(),
                resposta.motivoStatus(),
                resposta.chaveAcesso(),
                resposta.obterNfseXmlDescompactado()
        );

        return ResponseEntity.ok(response);
    }
}

