package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.observability.FiscalAuditLogger;
import br.com.heviane.nfse.adapters.in.observability.NfseMetrics;
import br.com.heviane.nfse.adapters.in.observability.RegistroEmissaoAuditoria;
import br.com.heviane.nfse.adapters.in.observability.RegistroEmissaoDuravelService;
import br.com.heviane.nfse.adapters.in.rest.dto.EmissaoDpsRequest;
import br.com.heviane.nfse.adapters.in.rest.dto.EmissaoDpsResponse;
import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.adapters.in.security.CertificadoProvider;
import br.com.heviane.nfse.config.NfseProperties;
import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/dps")
@Tag(name = "Emissão DPS", description = "Validação, assinatura digital e transmissão síncrona da Declaração de Prestação de Serviços (DPS)")
public class EmissaoDpsController {

    private final EmitirDpsUseCase emitirDpsUseCase;
    private final CertificadoProvider certificadoProvider;
    private final NfseProperties nfseProperties;
    private final NfseMetrics nfseMetrics;
    private final FiscalAuditLogger fiscalAuditLogger;
    private final RegistroEmissaoDuravelService registroDuravelService;

    @Autowired
    public EmissaoDpsController(EmitirDpsUseCase emitirDpsUseCase,
                                CertificadoProvider certificadoProvider,
                                NfseProperties nfseProperties,
                                @Autowired(required = false) NfseMetrics nfseMetrics,
                                @Autowired(required = false) FiscalAuditLogger fiscalAuditLogger,
                                @Autowired(required = false) RegistroEmissaoDuravelService registroDuravelService) {
        this.emitirDpsUseCase = emitirDpsUseCase;
        this.certificadoProvider = certificadoProvider;
        this.nfseProperties = (nfseProperties != null) ? nfseProperties : new NfseProperties();
        this.nfseMetrics = nfseMetrics;
        this.fiscalAuditLogger = fiscalAuditLogger;
        this.registroDuravelService = registroDuravelService;
    }

    public EmissaoDpsController(EmitirDpsUseCase emitirDpsUseCase,
                                CertificadoProvider certificadoProvider,
                                NfseProperties nfseProperties) {
        this(emitirDpsUseCase, certificadoProvider, nfseProperties, null, null, null);
    }

    public EmissaoDpsController(EmitirDpsUseCase emitirDpsUseCase, CertificadoProvider certificadoProvider) {
        this(emitirDpsUseCase, certificadoProvider, new NfseProperties(), null, null, null);
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
        long inicio = System.currentTimeMillis();
        String cnpjEmitente = (fiscalAuditLogger != null && request != null) ? fiscalAuditLogger.extrairCnpjDps(request.xmlDps()) : null;
        String idDps = (fiscalAuditLogger != null && request != null) ? fiscalAuditLogger.extrairIdDps(request.xmlDps()) : null;

        try {
            if (request == null || request.xmlDps() == null || request.xmlDps().isBlank()) {
                throw new IllegalArgumentException("O conteúdo do XML da DPS (xmlDps) é obrigatório.");
            }

            CertificadoA1 certificado = certificadoProvider.obterCertificado(certBase64, certSenha);
            AmbienteSefin ambiente = request.ambiente() != null ? request.ambiente() : nfseProperties.resolverAmbiente();

            RespostaEmissaoSefin resposta = emitirDpsUseCase.emitir(request.xmlDps(), ambiente, certificado);
            long duracao = System.currentTimeMillis() - inicio;

            if (fiscalAuditLogger != null) {
                fiscalAuditLogger.registrarEmissao("REST", idDps, resposta.chaveAcesso(), cnpjEmitente, resposta.numeroProtocolo(), resposta.codigoStatus(), 200, duracao);
            }
            if (nfseMetrics != null) {
                nfseMetrics.registrarEmissao("REST", resposta.isSucesso(), !resposta.isSucesso(), duracao);
            }
            if (registroDuravelService != null) {
                registroDuravelService.salvar(new RegistroEmissaoAuditoria(
                        Instant.now(),
                        idDps,
                        resposta.chaveAcesso(),
                        resposta.numeroProtocolo(),
                        resposta.codigoStatus(),
                        fiscalAuditLogger != null ? fiscalAuditLogger.mascararCnpj(cnpjEmitente) : cnpjEmitente,
                        ambiente.name(),
                        "REST",
                        MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)
                ));
            }

            EmissaoDpsResponse response = new EmissaoDpsResponse(
                    resposta.isSucesso(),
                    resposta.codigoStatus(),
                    resposta.motivoStatus(),
                    resposta.chaveAcesso(),
                    resposta.obterNfseXmlDescompactado(),
                    resposta.numeroProtocolo()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            if (fiscalAuditLogger != null) {
                fiscalAuditLogger.registrarFalhaEmissao("REST", idDps, cnpjEmitente, 400, e.getMessage(), duracao);
            }
            if (nfseMetrics != null) {
                nfseMetrics.registrarEmissao("REST", false, false, duracao);
            }
            throw e;
        }
    }
}

