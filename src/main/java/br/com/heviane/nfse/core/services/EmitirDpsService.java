package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RequisicaoEnvioDps;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;

import java.util.Objects;

/**
 * Orquestrador do fluxo central de emissão da DPS:
 * 1. Validação prévia contra o XSD oficial (v1.00 / v1.01)
 * 2. Assinatura digital A1 em memória (RSA-SHA256)
 * 3. Transmissão síncrona com autenticação mTLS ao Sefin Nacional
 */
public class EmitirDpsService implements EmitirDpsUseCase {

    private final ValidadorSchemaPort validadorSchemaPort;
    private final AssinadorDpsPort assinadorDpsPort;
    private final TransmissorSefinPort transmissorSefinPort;

    public EmitirDpsService(ValidadorSchemaPort validadorSchemaPort,
                            AssinadorDpsPort assinadorDpsPort,
                            TransmissorSefinPort transmissorSefinPort) {
        this.validadorSchemaPort = Objects.requireNonNull(validadorSchemaPort, "ValidadorSchemaPort não pode ser nulo.");
        this.assinadorDpsPort = Objects.requireNonNull(assinadorDpsPort, "AssinadorDpsPort não pode ser nulo.");
        this.transmissorSefinPort = Objects.requireNonNull(transmissorSefinPort, "TransmissorSefinPort não pode ser nulo.");
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

        // 3. Transmissão síncrona com handshake mTLS
        RequisicaoEnvioDps requisicaoEnvio = new RequisicaoEnvioDps(xmlDpsAssinado, amb);
        return transmissorSefinPort.emitirDps(requisicaoEnvio, certificado);
    }
}

