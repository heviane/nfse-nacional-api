package br.com.heviane.nfse.core.ports.in;

import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RequisicaoCancelamentoDps;
import br.com.heviane.nfse.core.domain.RespostaEventoCancelamento;

/**
 * Caso de uso: Envio de pedido de registro de cancelamento de NFS-e (Evento 101101).
 */
public interface CancelarNfseUseCase {

    /**
     * Registra o evento de cancelamento da nota no Sefin Nacional.
     *
     * @param requisicao Dados do cancelamento (chave de 50 dígitos, CNPJ, motivo)
     * @param certificado Certificado digital A1 em memória
     * @return Dados do evento registrado
     */
    RespostaEventoCancelamento cancelar(RequisicaoCancelamentoDps requisicao, CertificadoA1 certificado);
}

