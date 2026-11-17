package br.com.heviane.nfse.adapters.in.observability;

import java.time.Instant;

/**
 * Registro durável e imutável de uma emissão de NFS-e (pendente, concluída ou em processamento).
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
        String correlationId,
        String situacao
) {
    public static final String SITUACAO_PENDENTE = "PENDENTE";
    public static final String SITUACAO_CONCLUIDO = "CONCLUIDO";
    public static final String SITUACAO_PROCESSADO = "PROCESSADO";
    public static final String SITUACAO_FALHA = "FALHA";

    public RegistroEmissaoAuditoria {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (situacao == null || situacao.isBlank()) {
            situacao = (statusFiscal != null && statusFiscal == 100)
                    ? SITUACAO_CONCLUIDO
                    : (statusFiscal != null ? SITUACAO_PROCESSADO : SITUACAO_PENDENTE);
        }
    }

    /**
     * Construtor de compatibilidade para chamadas que não informam explicitamente o estado da emissão.
     */
    public RegistroEmissaoAuditoria(
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
        this(timestamp, idDps, chaveAcesso, numeroProtocolo, statusFiscal, cnpjEmitente, ambiente, origem, correlationId, null);
    }
}
