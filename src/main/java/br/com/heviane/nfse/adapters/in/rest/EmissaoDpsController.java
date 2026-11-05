package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.EmissaoDpsRequest;
import br.com.heviane.nfse.adapters.in.rest.dto.EmissaoDpsResponse;
import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
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
@RequestMapping("/api/v1/dps")
@Tag(name = "Emissão DPS", description = "Validação, assinatura digital e transmissão síncrona da Declaração de Prestação de Serviços (DPS)")
public class EmissaoDpsController {

    private final EmitirDpsUseCase emitirDpsUseCase;
    private final CertificadoProvider certificadoProvider;

    public EmissaoDpsController(EmitirDpsUseCase emitirDpsUseCase, CertificadoProvider certificadoProvider) {
        this.emitirDpsUseCase = emitirDpsUseCase;
        this.certificadoProvider = certificadoProvider;
    }

    @PostMapping(value = "/emitir", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Emitir DPS no Sefin Nacional",
            description = "Valida a DPS contra o schema XSD oficial v1.00, assina digitalmente com Certificado Digital A1 " +
                    "e transmite de forma síncrona via mTLS para o Sefin Nacional / ADN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPS autorizada com sucesso ou processada pelo Sefin Nacional",
                    content = @Content(schema = @Schema(implementation = EmissaoDpsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou erro na assinatura digital",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Inconsistência de schema XSD na DPS enviada",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "502", description = "Erro ou falha de comunicação mTLS com o Sefin Nacional",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<EmissaoDpsResponse> emitir(
            @RequestBody EmissaoDpsRequest request,
            @Parameter(in = ParameterIn.HEADER, description = "Certificado Digital A1 (.pfx/.p12) codificado em Base64 para envio multi-tenant dinâmico")
            @RequestHeader(value = "X-Certificado-Base64", required = false) String certBase64,
            @Parameter(in = ParameterIn.HEADER, description = "Senha de proteção do Certificado Digital A1")
            @RequestHeader(value = "X-Certificado-Senha", required = false) String certSenha
    ) {
        if (request == null || request.xmlDps() == null || request.xmlDps().isBlank()) {
            throw new IllegalArgumentException("O conteúdo do XML da DPS (xmlDps) é obrigatório.");
        }

        CertificadoA1 certificado = certificadoProvider.obterCertificado(certBase64, certSenha);
        AmbienteSefin ambiente = request.ambiente() != null ? request.ambiente() : resolverAmbiente();

        RespostaEmissaoSefin resposta = emitirDpsUseCase.emitir(request.xmlDps(), ambiente, certificado);

        EmissaoDpsResponse response = new EmissaoDpsResponse(
                resposta.isSucesso(),
                resposta.codigoStatus(),
                resposta.motivoStatus(),
                resposta.chaveAcesso(),
                resposta.obterNfseXmlDescompactado(),
                resposta.numeroProtocolo()
        );

        return ResponseEntity.ok(response);
    }

    private AmbienteSefin resolverAmbiente() {
        String env = System.getenv("NFSE_ENVIRONMENT");
        if (env == null || env.isBlank()) {
            env = System.getProperty("nfse.environment", "HOMOLOGACAO");
        }
        try {
            return AmbienteSefin.valueOf(env.trim().toUpperCase());
        } catch (Exception e) {
            return AmbienteSefin.HOMOLOGACAO;
        }
    }
}

