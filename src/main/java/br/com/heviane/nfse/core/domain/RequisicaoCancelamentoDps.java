package br.com.heviane.nfse.core.domain;

/**
 * Objeto de comando para solicitar o cancelamento de uma NFS-e no Sefin Nacional (Evento 101101).
 */
public record RequisicaoCancelamentoDps(
        String chaveAcesso,
        String cnpjAutor,
        String codigoMotivo,
        String motivo,
        AmbienteSefin ambiente
) {
    public RequisicaoCancelamentoDps {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso deve conter exatamente 50 dígitos numéricos.");
        }
        if (cnpjAutor == null || !cnpjAutor.matches("\\d{14}")) {
            throw new IllegalArgumentException("CNPJ do autor deve conter 14 dígitos numéricos.");
        }
        if (codigoMotivo == null || !codigoMotivo.matches("^[129]$")) {
            throw new IllegalArgumentException("Código do motivo de cancelamento deve ser 1 (Erro na Emissão), 2 (Serviço não Prestado) ou 9 (Outros).");
        }
        if (motivo == null || motivo.trim().length() < 15 || motivo.trim().length() > 255) {
            throw new IllegalArgumentException("Justificativa de cancelamento deve conter entre 15 e 255 caracteres.");
        }
        if (ambiente == null) {
            ambiente = AmbienteSefin.HOMOLOGACAO;
        }
    }
}

