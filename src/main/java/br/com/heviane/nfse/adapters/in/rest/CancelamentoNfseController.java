package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.observability.FiscalAuditLogger;
import br.com.heviane.nfse.adapters.in.observability.NfseMetrics;
import br.com.heviane.nfse.adapters.in.rest.dto.CancelamentoNfseRequest;
import br.com.heviane.nfse.adapters.in.rest.dto.CancelamentoNfseResponse;
import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.adapters.in.security.CertificadoProvider;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.in.CancelarNfseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nfse")
@Tag(name = "Cancelamento NFS-e", description = "Registro de Evento de Cancelamento de NFS-e (Evento 101101) no Sefin Nacional")
public class CancelamentoNfseController {

    private final CancelarNfseUseCase cancelarNfseUseCase;
    private final CertificadoProvider certificadoProvider;
    private final NfseMetrics nfseMetrics;
    private final FiscalAuditLogger fiscalAuditLogger;

    @Autowired
    public CancelamentoNfseController(CancelarNfseUseCase cancelarNfseUseCase,
                                      CertificadoProvider certificadoProvider,
                                      @Autowired(required = false) NfseMetrics nfseMetrics,
                                      @Autowired(required = false) FiscalAuditLogger fiscalAuditLogger) {
        this.cancelarNfseUseCase = cancelarNfseUseCase;
        this.certificadoProvider = certificadoProvider;
        this.nfseMetrics = nfseMetrics;
        this.fiscalAuditLogger = fiscalAuditLogger;
    }

    public CancelamentoNfseController(CancelarNfseUseCase cancelarNfseUseCase, CertificadoProvider certificadoProvider) {
        this(cancelarNfseUseCase, certificadoProvider, null, null);
    }

    @PostMapping(value = "/cancelar", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Cancelar NFS-e",
            description = "Gera, assina digitalmente e transmite o Evento de Cancelamento (Evento 101101) para a NFS-e especificada pela chave de acesso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento de cancelamento registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = CancelamentoNfseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou chave de acesso incorreta",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "502", description = "Falha de comunicação mTLS com o Sefin Nacional",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<CancelamentoNfseResponse> cancelar(
            @RequestBody CancelamentoNfseRequest request,
            @Parameter(in = ParameterIn.HEADER, description = "Certificado Digital A1 (.pfx/.p12) codificado em Base64")
            @RequestHeader(value = "X-Certificado-Base64", required = false) String certBase64,
            @Parameter(in = ParameterIn.HEADER, description = "Senha de proteção do Certificado Digital A1")
            @RequestHeader(value = "X-Certificado-Senha", required = false) String certSenha
    ) {
        long inicio = System.currentTimeMillis();
        String chave = request != null ? request.chaveAcesso() : null;
        String cnpjAutor = request != null ? request.cnpjAutor() : null;

        try {
            if (request == null) {
                throw new IllegalArgumentException("Corpo da requisição de cancelamento é obrigatório.");
            }

            CertificadoA1 certificado = certificadoProvider.obterCertificado(certBase64, certSenha);

            RequisicaoCancelamentoDps comando = new RequisicaoCancelamentoDps(
                    request.chaveAcesso(),
                    request.cnpjAutor(),
                    request.codigoMotivo(),
                    request.motivo(),
                    request.ambiente()
            );

            RespostaEventoCancelamento resposta = cancelarNfseUseCase.cancelar(comando, certificado);
            long duracao = System.currentTimeMillis() - inicio;

            if (fiscalAuditLogger != null) {
                fiscalAuditLogger.registrarCancelamento(resposta.chaveAcesso(), cnpjAutor, resposta.codigoStatus(), 200, duracao);
            }
            if (nfseMetrics != null) {
                nfseMetrics.registrarCancelamento(resposta.isSucesso(), !resposta.isSucesso(), duracao);
            }

            CancelamentoNfseResponse response = new CancelamentoNfseResponse(
                    resposta.isSucesso(),
                    resposta.codigoStatus(),
                    resposta.motivoStatus(),
                    resposta.chaveAcesso(),
                    resposta.idEvento(),
                    resposta.obterEventoXmlDescompactado()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            if (fiscalAuditLogger != null) {
                fiscalAuditLogger.registrarCancelamento(chave, cnpjAutor, null, 400, duracao);
            }
            if (nfseMetrics != null) {
                nfseMetrics.registrarCancelamento(false, false, duracao);
            }
            throw e;
        }
    }
}

