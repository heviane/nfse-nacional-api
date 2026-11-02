package br.com.heviane.nfse.core.ports.in;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;

/**
 * Caso de uso: Consulta de NFS-e por Chave de Acesso no Sefin Nacional.
 */
public interface ConsultarNfseUseCase {

    /**
     * Consulta uma NFS-e autorizada pela sua chave de 50 dígitos.
     *
     * @param chaveAcesso Chave de acesso de 50 dígitos
     * @param ambiente Ambiente (Homologação ou Produção)
     * @param certificado Certificado digital A1 em memória
     * @return Dados e XML da nota fiscal
     */
    RespostaConsultaNfse consultar(String chaveAcesso, AmbienteSefin ambiente, CertificadoA1 certificado);
}

