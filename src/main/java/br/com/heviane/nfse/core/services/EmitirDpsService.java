package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RequisicaoEnvioDps;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Orquestrador do fluxo central de emissão da DPS:
 * 1. Validação prévia contra o XSD oficial (v1.00 / v1.01)
 * 2. Assinatura digital A1 em memória (RSA-SHA256)
 * 3. Transmissão com autenticação mTLS ao Sefin Nacional
 * 4. Resolução transparente de respostas assíncronas (status 105) via polling curto
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

        // 1. Validação estrita do schema XSD
        validadorSchemaPort.validarDpsOuFalhar(xmlDps);

        // 2. Assinatura digital ICP-Brasil A1 (RSA-SHA256)
        String xmlDpsAssinado = assinadorDpsPort.assinar(xmlDps, certificado);

        // 3. Transmissão com handshake mTLS
        RequisicaoEnvioDps requisicaoEnvio = new RequisicaoEnvioDps(xmlDpsAssinado, amb);
        RespostaEmissaoSefin resposta = transmissorSefinPort.emitirDps(requisicaoEnvio, certificado);

        // 4. Tratamento de resposta assíncrona / em fila (status 105)
        if (resposta.isEmProcessamento() && resposta.chaveAcesso() != null && pollingConfig.maxTentativas() > 0) {
            return tentarResolverProcessamentoAssincrono(resposta, amb, certificado);
        }

        return resposta;
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
}
