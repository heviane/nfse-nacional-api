package br.com.heviane.nfse.core.ports.out;

import br.com.heviane.nfse.core.domain.*;

/**
 * Porta de saída para transmissão de documentos e comunicação HTTP REST (mTLS)
 * com o Ambiente Nacional da NFS-e (Sefin Nacional / ADN).
 */
public interface TransmissorSefinPort {

    /**
     * Transmite a DPS assinada ao endpoint de emissão síncrona do Sefin Nacional.
     *
     * @param requisicao Objeto contendo o XML da DPS assinada e ambiente alvo
     * @param certificado Certificado Digital A1 em memória para handshake mTLS
     * @return Resposta do Sefin contendo status, chave de acesso e XML da NFS-e autorizada
     */
    RespostaEmissaoSefin emitirDps(RequisicaoEnvioDps requisicao, CertificadoA1 certificado);

    /**
     * Consulta a situação de uma NFS-e no Sefin Nacional pela Chave de Acesso de 50 dígitos.
     *
     * @param chaveAcesso Chave de acesso de 50 dígitos numéricos
     * @param ambiente Ambiente (Homologação ou Produção)
     * @param certificado Certificado Digital A1 em memória para handshake mTLS
     * @return Dados da NFS-e consultada
     */
    RespostaConsultaNfse consultarPorChave(String chaveAcesso, AmbienteSefin ambiente, CertificadoA1 certificado);

    /**
     * Consulta a Chave de Acesso gerada a partir do Identificador único da DPS (IdDps).
     * Utilizado para reconciliação automática e garantia de idempotência em caso de timeout de rede ou status 204.
     *
     * @param idDps Identificador único da DPS (ex: DPS3205200...)
     * @param ambiente Ambiente (Homologação ou Produção)
     * @param certificado Certificado Digital A1 em memória para handshake mTLS
     * @return Resposta contendo a chave de acesso vinculada à DPS, ou null caso não encontrada
     */
    RespostaConsultaDps consultarPorIdDps(String idDps, AmbienteSefin ambiente, CertificadoA1 certificado);

    /**
     * Envia o pedido de cancelamento de uma NFS-e (Evento 101101) ao Sefin Nacional.
     *
     * @param requisicao Parâmetros de cancelamento (chave, motivo, justificativa)
     * @param certificado Certificado Digital A1 em memória para handshake mTLS
     * @return Resposta do registro do evento de cancelamento
     */
    RespostaEventoCancelamento cancelar(RequisicaoCancelamentoDps requisicao, CertificadoA1 certificado);
}
