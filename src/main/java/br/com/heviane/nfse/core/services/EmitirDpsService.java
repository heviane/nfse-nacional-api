package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestrador do fluxo central de emissão da DPS:
 * 1. Validação prévia contra o XSD oficial (v1.00 / v1.01)
 * 2. Assinatura digital A1 em memória (RSA-SHA256)
 * 3. Transmissão com autenticação mTLS ao Sefin Nacional
 * 4. Resolução transparente de respostas assíncronas (status 105) via polling curto
 * 5. Reconciliação automática e garantia de idempotência via GET /dps/{id} em caso de timeout ou status 204
 */
public class EmitirDpsService implements EmitirDpsUseCase {

    private static final Logger log = LoggerFactory.getLogger(EmitirDpsService.class);

    private final ValidadorSchemaPort validadorSchemaPort;
    private final AssinadorDpsPort assinadorDpsPort;
    private final TransmissorSefinPort transmissorSefinPort;
    private final AsyncPollingConfig pollingConfig;

    public EmitirDpsService(ValidadorSchemaPort validadorSchemaPort,
                            AssinadorDpsPort assinadorDpsPort,
                            TransmissorSefinPort transmissorSefinPort) {
        this(validadorSchemaPort, assinadorDpsPort, transmissorSefinPort, AsyncPollingConfig.padrao());
    }

    public EmitirDpsService(ValidadorSchemaPort validadorSchemaPort,
                            AssinadorDpsPort assinadorDpsPort,
                            TransmissorSefinPort transmissorSefinPort,
                            AsyncPollingConfig pollingConfig) {
        this.validadorSchemaPort = Objects.requireNonNull(validadorSchemaPort, "ValidadorSchemaPort não pode ser nulo.");
        this.assinadorDpsPort = Objects.requireNonNull(assinadorDpsPort, "AssinadorDpsPort não pode ser nulo.");
        this.transmissorSefinPort = Objects.requireNonNull(transmissorSefinPort, "TransmissorSefinPort não pode ser nulo.");
        this.pollingConfig = pollingConfig != null ? pollingConfig : AsyncPollingConfig.padrao();
    }

    @Override
    public RespostaEmissaoSefin emitir(String xmlDps, AmbienteSefin ambiente, CertificadoA1 certificado) {
        if (xmlDps == null || xmlDps.isBlank()) {
            throw new IllegalArgumentException("Conteúdo XML da DPS não pode ser nulo ou vazio.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para emissão fiscal.");
        }

        AmbienteSefin amb = (ambiente != null) ? ambiente : AmbienteSefin.HOMOLOGACAO;

        // 1. Validação estrita de coerência do ambiente fiscal (<tpAmb>) - Princípio de Imutabilidade
        validarTipoAmbiente(xmlDps, amb);

        // 2. Validação estrita do schema XSD
        validadorSchemaPort.validarDpsOuFalhar(xmlDps);

        // 3. Assinatura digital ICP-Brasil A1 (RSA-SHA256)
        String xmlDpsAssinado = assinadorDpsPort.assinar(xmlDps, certificado);

        // 4. Transmissão com handshake mTLS e resiliência de reconciliação
        RequisicaoEnvioDps requisicaoEnvio = new RequisicaoEnvioDps(xmlDpsAssinado, amb);
        RespostaEmissaoSefin resposta;
        try {
            resposta = transmissorSefinPort.emitirDps(requisicaoEnvio, certificado);
        } catch (Exception ex) {
            if (isFalhaRecuperavel(ex)) {
                log.warn("Falha de transmissão na emissão da DPS ({}) detectada. Iniciando reconciliação via GET /dps/{id} para garantir idempotência...",
                        ex.getMessage());
                RespostaEmissaoSefin recuperada = tentarReconciliarPorIdDps(xmlDps, amb, certificado);
                if (recuperada != null) {
                    return recuperada;
                }
            }
            throw ex;
        }

        // 5. Garantia de idempotência caso o Sefin retorne status 204 diretamente
        if (resposta.codigoStatus() == 204) {
            log.info("Sefin retornou status 204 (Duplicidade). Reconciliando DPS via GET /dps/{id} para resgatar a NFS-e autorizada...");
            RespostaEmissaoSefin recuperada = tentarReconciliarPorIdDps(xmlDps, amb, certificado);
            if (recuperada != null) {
                return recuperada;
            }
        }

        // 6. Tratamento de resposta assíncrona / em fila (status 105)
        if (resposta.isEmProcessamento() && resposta.chaveAcesso() != null && pollingConfig.maxTentativas() > 0) {
            return tentarResolverProcessamentoAssincrono(resposta, amb, certificado);
        }

        return resposta;
    }

    private RespostaEmissaoSefin tentarReconciliarPorIdDps(String xmlDps, AmbienteSefin ambiente, CertificadoA1 certificado) {
        String idDps = extrairIdDps(xmlDps);
        if (idDps == null || idDps.isBlank()) {
            log.debug("Não foi possível extrair o atributo Id da tag <infDPS> para reconciliação.");
            return null;
        }

        try {
            log.info("Reconciliação: consultando situação da DPS {} no Sefin Nacional via GET /dps/{}...", idDps, idDps);
            RespostaConsultaDps consultaDps = transmissorSefinPort.consultarPorIdDps(idDps, ambiente, certificado);

            if (consultaDps != null && consultaDps.isSucesso()) {
                log.info("DPS {} confirmada como autorizada no Sefin. Chave de acesso: {}. Consultando NFS-e autorizada...",
                        idDps, consultaDps.chaveAcesso());

                try {
                    RespostaConsultaNfse consultaNfse = transmissorSefinPort.consultarPorChave(
                            consultaDps.chaveAcesso(), ambiente, certificado
                    );
                    if (consultaNfse != null && consultaNfse.isEncontrada()) {
                        log.info("NFS-e autorizada recuperada com sucesso para DPS {}. Chave: {}", idDps, consultaNfse.chaveAcesso());
                        return new RespostaEmissaoSefin(
                                consultaNfse.tipoAmbiente(),
                                "SefinNacional_v1.0",
                                consultaNfse.dataHoraProcessamento(),
                                consultaNfse.codigoStatus(),
                                consultaNfse.motivoStatus(),
                                consultaNfse.chaveAcesso(),
                                consultaNfse.nfseXmlGZipB64(),
                                null
                        );
                    }
                } catch (Exception e) {
                    log.warn("Consulta complementar por chave {} falhou: {}. Retornando chave da DPS recuperada.",
                            consultaDps.chaveAcesso(), e.getMessage());
                }

                return new RespostaEmissaoSefin(
                        consultaDps.tipoAmbiente() != null ? consultaDps.tipoAmbiente() : ambiente.getCodigo(),
                        "SefinNacional_v1.0",
                        consultaDps.dataHoraProcessamento(),
                        100,
                        "NFS-e gerada com sucesso (recuperada por reconciliação)",
                        consultaDps.chaveAcesso(),
                        null,
                        null
                );
            }
        } catch (Exception e) {
            log.warn("Reconciliação não localizou NFS-e autorizada para DPS {}: {}", idDps, e.getMessage());
        }

        return null;
    }

    private boolean isFalhaRecuperavel(Exception ex) {
        if (ex instanceof SefinTransmissaoException sefinEx) {
            if (Integer.valueOf(204).equals(sefinEx.getCodigoStatusSefin())) {
                return true;
            }
            if (sefinEx.getHttpStatusCode() != null &&
                    (sefinEx.getHttpStatusCode() == 504 || sefinEx.getHttpStatusCode() == 502 || sefinEx.getHttpStatusCode() == 503)) {
                return true;
            }
            if (sefinEx.getCause() instanceof java.io.IOException) {
                return true;
            }
        }
        if (ex instanceof java.io.IOException) {
            return true;
        }
        return false;
    }

    private String extrairIdDps(String xmlDps) {
        if (xmlDps == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("<infDPS[^>]*\\bId=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(xmlDps);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private RespostaEmissaoSefin tentarResolverProcessamentoAssincrono(RespostaEmissaoSefin respostaOriginal,
                                                                      AmbienteSefin ambiente,
                                                                      CertificadoA1 certificado) {
        log.info("DPS recebida pelo Sefin em processamento assíncrono (status 105). Iniciando resolução transparente via polling (até {} tentativas)...",
                pollingConfig.maxTentativas());

        for (int i = 1; i <= pollingConfig.maxTentativas(); i++) {
            aplicarDelay(pollingConfig.intervalo().toMillis());
            try {
                RespostaConsultaNfse consulta = transmissorSefinPort.consultarPorChave(
                        respostaOriginal.chaveAcesso(),
                        ambiente,
                        certificado
                );
                if (consulta.isEncontrada()) {
                    log.info("DPS em processamento assíncrono autorizada com sucesso na consulta {}/{}. Chave: {}",
                            i, pollingConfig.maxTentativas(), consulta.chaveAcesso());
                    return new RespostaEmissaoSefin(
                            consulta.tipoAmbiente(),
                            "SefinNacional_v1.0",
                            consulta.dataHoraProcessamento(),
                            consulta.codigoStatus(),
                            consulta.motivoStatus(),
                            consulta.chaveAcesso(),
                            consulta.nfseXmlGZipB64(),
                            respostaOriginal.numeroProtocolo()
                    );
                }
            } catch (Exception e) {
                log.debug("Polling {}/{} para chave {} retornou: {}",
                        i, pollingConfig.maxTentativas(), respostaOriginal.chaveAcesso(), e.getMessage());
            }
        }

        log.warn("DPS permanece em processamento após {} tentativas de polling. Retornando protocolo/chave pendente ao cliente.",
                pollingConfig.maxTentativas());
        return respostaOriginal;
    }

    private void aplicarDelay(long delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void validarTipoAmbiente(String xmlDps, AmbienteSefin amb) {
        Pattern pattern = Pattern.compile("<tpAmb>(\\d+)</tpAmb>");
        Matcher matcher = pattern.matcher(xmlDps);
        if (matcher.find()) {
            int tpAmbXml = Integer.parseInt(matcher.group(1));
            if (tpAmbXml != amb.getCodigo()) {
                throw new ValidacaoFiscalException(
                        String.format("Inconsistência de ambiente fiscal: O XML da DPS declara <tpAmb>%d</tpAmb>, mas o serviço está configurado para operar em %s (código %d). Corrija o campo <tpAmb> no ERP ou aponte a requisição para o ambiente correspondente.",
                                tpAmbXml, amb, amb.getCodigo())
                );
            }
        }
    }
}
