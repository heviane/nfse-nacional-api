package br.com.heviane.nfse.core.domain;

/**
 * Modelo de domínio para a resposta de consulta de NFS-e por Chave de Acesso.
 */
public record RespostaConsultaNfse(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64
) {
    public boolean isEncontrada() {
        return codigoStatus == 100;
    }

    /**
     * Decodifica e descompacta o XML oficial da NFS-e retornada.
     */
    public String obterNfseXmlDescompactado() {
        return GzipUtils.descompactarGzipBase64(nfseXmlGZipB64);
    }
}
