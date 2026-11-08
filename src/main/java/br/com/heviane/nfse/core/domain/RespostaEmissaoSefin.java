package br.com.heviane.nfse.core.domain;

/**
 * Modelo de domínio para a resposta de emissão retornada pelo Sefin Nacional.
 * Suporta retornos síncronos (autorização imediata, status 100) e assíncronos (em fila/lote, status 105).
 */
public record RespostaEmissaoSefin(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String nfseXmlGZipB64,
        String numeroProtocolo
) {
    public RespostaEmissaoSefin(int tipoAmbiente,
                                String versaoAplicativo,
                                String dataHoraProcessamento,
                                int codigoStatus,
                                String motivoStatus,
                                String chaveAcesso,
                                String nfseXmlGZipB64) {
        this(tipoAmbiente, versaoAplicativo, dataHoraProcessamento, codigoStatus, motivoStatus, chaveAcesso, nfseXmlGZipB64, null);
    }

    /**
     * Indica se a NFS-e foi autorizada de forma síncrona com sucesso (status 100).
     */
    public boolean isSucesso() {
        return codigoStatus == 100;
    }

    /**
     * Indica se a DPS/Lote foi recebido pelo Sefin Nacional e colocado em fila de processamento assíncrono (status 105).
     */
    public boolean isEmProcessamento() {
        return codigoStatus == 105;
    }

    /**
     * Decodifica e descompacta o XML oficial da NFS-e autorizada.
     */
    public String obterNfseXmlDescompactado() {
        return GzipUtils.descompactarGzipBase64(nfseXmlGZipB64);
    }
}
