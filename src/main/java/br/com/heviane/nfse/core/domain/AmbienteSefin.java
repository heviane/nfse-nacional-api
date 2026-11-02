package br.com.heviane.nfse.core.domain;

/**
 * Ambientes de execução e transmissão do Sefin Nacional / ADN.
 */
public enum AmbienteSefin {
    HOMOLOGACAO("https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional", 2),
    PRODUCAO("https://sefin.nfse.gov.br/API/SefinNacional", 1);

    private final String baseUrl;
    private final int codigo;

    AmbienteSefin(String baseUrl, int codigo) {
        this.baseUrl = baseUrl;
        this.codigo = codigo;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getCodigo() {
        return codigo;
    }
}

