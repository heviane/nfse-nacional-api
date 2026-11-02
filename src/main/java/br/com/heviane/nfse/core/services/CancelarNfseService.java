package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RequisicaoCancelamentoDps;
import br.com.heviane.nfse.core.domain.RespostaEventoCancelamento;
import br.com.heviane.nfse.core.ports.in.CancelarNfseUseCase;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;

import java.util.Objects;

/**
 * Serviço de caso de uso para cancelamento de NFS-e (Evento 101101).
 */
public class CancelarNfseService implements CancelarNfseUseCase {

    private final TransmissorSefinPort transmissorSefinPort;

    public CancelarNfseService(TransmissorSefinPort transmissorSefinPort) {
        this.transmissorSefinPort = Objects.requireNonNull(transmissorSefinPort, "TransmissorSefinPort não pode ser nulo.");
    }

    @Override
    public RespostaEventoCancelamento cancelar(RequisicaoCancelamentoDps requisicao, CertificadoA1 certificado) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Requisição de cancelamento não pode ser nula.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para cancelamento fiscal.");
        }

        return transmissorSefinPort.cancelar(requisicao, certificado);
    }
}

