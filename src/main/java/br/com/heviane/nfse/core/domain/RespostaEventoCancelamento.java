package br.com.heviane.nfse.core.domain;

/**
 * Modelo de domínio para o retorno do registro de evento de cancelamento pelo Sefin Nacional.
 */
public record RespostaEventoCancelamento(
        int tipoAmbiente,
        String versaoAplicativo,
        String dataHoraProcessamento,
        int codigoStatus,
        String motivoStatus,
        String chaveAcesso,
        String idEvento,
        String eventoXmlGZipB64
) {
    public boolean isSucesso() {
        return codigoStatus == 100 || codigoStatus == 135; // 135: Evento registrado e vinculado
    }

    /**
     * Decodifica e descompacta o XML oficial do evento registrado.
     */
    public String obterEventoXmlDescompactado() {
        return GzipUtils.descompactarGzipBase64(eventoXmlGZipB64);
    }
}
