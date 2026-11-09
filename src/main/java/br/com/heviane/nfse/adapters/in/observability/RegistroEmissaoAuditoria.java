package br.com.heviane.nfse.adapters.in.observability;

import java.time.Instant;

/**
 * Registro durável e imutável de uma emissão de NFS-e concluída.
 * <p>
 * Armazena exclusivamente metadados essenciais para reconciliação fiscal e recuperação
 * de chaves em caso de queda de conexão com o ERP, sem conter o payload XML completo (conformidade LGPD).
 */
public record RegistroEmissaoAuditoria(
        Instant timestamp,
        String idDps,
        String chaveAcesso,
        String numeroProtocolo,
        Integer statusFiscal,
        String cnpjEmitente,
        String ambiente,
        String origem,
        String correlationId
) {
    public RegistroEmissaoAuditoria {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
