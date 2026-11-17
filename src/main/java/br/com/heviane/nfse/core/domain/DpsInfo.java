package br.com.heviane.nfse.core.domain;

/**
 * Registro de domínio contendo os metadados estruturados extraídos do elemento {@code infDPS}.
 */
public record DpsInfo(
        String id,
        Integer tpAmb,
        String cLocEmi,
        String serie,
        String nDps,
        String cnpjPrestador,
        String cpfPrestador
) {
    public boolean temPrestadorCnpj() {
        return cnpjPrestador != null && !cnpjPrestador.isBlank();
    }

    public boolean temPrestadorCpf() {
        return cpfPrestador != null && !cpfPrestador.isBlank();
    }

    public String getDocumentoPrestador() {
        return temPrestadorCnpj() ? cnpjPrestador : cpfPrestador;
    }

    public String getTipoInscPrestador() {
        if (temPrestadorCnpj()) {
            return DpsIdHelper.TIPO_INSC_CNPJ;
        }
        if (temPrestadorCpf()) {
            return DpsIdHelper.TIPO_INSC_CPF;
        }
        return null;
    }
}
