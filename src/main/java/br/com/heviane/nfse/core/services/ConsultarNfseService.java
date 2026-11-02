package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import br.com.heviane.nfse.core.ports.in.ConsultarNfseUseCase;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;

import java.util.Objects;

/**
 * Serviço de caso de uso para consulta de NFS-e por Chave de Acesso.
 */
public class ConsultarNfseService implements ConsultarNfseUseCase {

    private final TransmissorSefinPort transmissorSefinPort;

    public ConsultarNfseService(TransmissorSefinPort transmissorSefinPort) {
        this.transmissorSefinPort = Objects.requireNonNull(transmissorSefinPort, "TransmissorSefinPort não pode ser nulo.");
    }

    @Override
    public RespostaConsultaNfse consultar(String chaveAcesso, AmbienteSefin ambiente, CertificadoA1 certificado) {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso deve conter exatamente 50 dígitos numéricos.");
        }
        if (certificado == null) {
            throw new IllegalArgumentException("Certificado A1 é obrigatório para consulta fiscal.");
        }

        AmbienteSefin amb = (ambiente != null) ? ambiente : AmbienteSefin.HOMOLOGACAO;
        return transmissorSefinPort.consultarPorChave(chaveAcesso, amb, certificado);
    }
}

