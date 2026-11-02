package br.com.heviane.nfse.core.domain;

/**
 * Objeto de comando para envio e transmissão da DPS assinada ao Sefin Nacional.
 */
public record RequisicaoEnvioDps(
        String dpsXmlAssinado,
        AmbienteSefin ambiente
) {
    public RequisicaoEnvioDps {
        if (dpsXmlAssinado == null || dpsXmlAssinado.isBlank()) {
            throw new IllegalArgumentException("XML da DPS assinado não pode ser nulo ou vazio.");
        }
        if (ambiente == null) {
            ambiente = AmbienteSefin.HOMOLOGACAO;
        }
    }

    public static RequisicaoEnvioDps homologacao(String dpsXmlAssinado) {
        return new RequisicaoEnvioDps(dpsXmlAssinado, AmbienteSefin.HOMOLOGACAO);
    }

    public static RequisicaoEnvioDps producao(String dpsXmlAssinado) {
        return new RequisicaoEnvioDps(dpsXmlAssinado, AmbienteSefin.PRODUCAO);
    }
}
