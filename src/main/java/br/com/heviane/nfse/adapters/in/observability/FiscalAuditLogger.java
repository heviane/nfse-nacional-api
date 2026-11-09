package br.com.heviane.nfse.adapters.in.observability;

import br.com.heviane.nfse.adapters.in.rest.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registrador de auditoria estruturada para operações fiscais da NFS-e.
 * <p>
 * Projetado para conformidade com a LGPD e segurança tributária:
 * <ul>
 *     <li>Registra metadados essenciais de conciliação fiscal em linha única (fácil indexação e grep);</li>
 *     <li>Aplica mascaramento de segurança a números de CNPJ (preserva apenas identificação mínima);</li>
 *     <li>NUNCA registra corpos de payloads XML, chaves privadas ou certificados;</li>
 *     <li>Emite logs no canal dedicado {@code audit.fiscal}.</li>
 * </ul>
 */
@Component
public class FiscalAuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("audit.fiscal");
    private static final Pattern CNPJ_XML_PATTERN = Pattern.compile("<(?:prest|emit)>[\\s\\S]*?<CNPJ>(\\d{14})</CNPJ>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_CNPJ_PATTERN = Pattern.compile("<CNPJ>(\\d{14})</CNPJ>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_DPS_PATTERN = Pattern.compile("<infDPS[\\s]+[^>]*?Id=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    /**
     * Registra auditoria detalhada de uma operação de emissão de DPS (autorizada ou rejeitada pelo Sefin).
     */
    public void registrarEmissao(String origem, String idDps, String chaveAcesso, String cnpjEmitente, String numeroProtocolo, Integer statusSefin, int statusHttp, long duracaoMs) {
        String id = isNotBlank(idDps) ? idDps.trim() : "N/A";
        String chave = isNotBlank(chaveAcesso) ? chaveAcesso.trim() : "N/A";
        String protocolo = isNotBlank(numeroProtocolo) ? numeroProtocolo.trim() : "N/A";
        String cnpjMascarado = mascararCnpj(cnpjEmitente);
        String statusFiscal = statusSefin != null ? String.valueOf(statusSefin) : "N/A";
        String correlationId = obterCorrelationId();

        auditLog.info("AUDIT-FISCAL: operacao=EMISSAO | origem={} | statusFiscal={} | statusHttp={} | idDps={} | chave={} | protocolo={} | cnpjEmitente={} | duracaoMs={} | correlationId={}",
                origem, statusFiscal, statusHttp, id, chave, protocolo, cnpjMascarado, duracaoMs, correlationId);
    }

    /**
     * Registra auditoria de uma operação de emissão de DPS (assinatura compatível).
     */
    public void registrarEmissao(String origem, String chaveAcesso, String cnpjEmitente, Integer statusSefin, int statusHttp, long duracaoMs) {
        registrarEmissao(origem, "N/A", chaveAcesso, cnpjEmitente, "N/A", statusSefin, statusHttp, duracaoMs);
    }

    /**
     * Registra auditoria de falha ou erro de validação/comunicação na emissão de DPS.
     */
    public void registrarFalhaEmissao(String origem, String idDps, String cnpjEmitente, int statusHttp, String motivo, long duracaoMs) {
        String id = isNotBlank(idDps) ? idDps.trim() : "N/A";
        String cnpjMascarado = mascararCnpj(cnpjEmitente);
        String correlationId = obterCorrelationId();
        String motivoSanitizado = isNotBlank(motivo) ? motivo.replace('\n', ' ').replace('\r', ' ').trim() : "Erro desconhecido";

        auditLog.warn("AUDIT-FISCAL: operacao=EMISSAO_FALHA | origem={} | statusHttp={} | idDps={} | cnpjEmitente={} | motivo=\"{}\" | duracaoMs={} | correlationId={}",
                origem, statusHttp, id, cnpjMascarado, motivoSanitizado, duracaoMs, correlationId);
    }

    public void registrarFalhaEmissao(String origem, String cnpjEmitente, int statusHttp, String motivo, long duracaoMs) {
        registrarFalhaEmissao(origem, "N/A", cnpjEmitente, statusHttp, motivo, duracaoMs);
    }

    /**
     * Registra auditoria de consulta de situação de NFS-e.
     */
    public void registrarConsulta(String chaveAcesso, Integer statusSefin, int statusHttp, long duracaoMs) {
        String chave = isNotBlank(chaveAcesso) ? chaveAcesso.trim() : "N/A";
        String statusFiscal = statusSefin != null ? String.valueOf(statusSefin) : "N/A";
        String correlationId = obterCorrelationId();

        auditLog.info("AUDIT-FISCAL: operacao=CONSULTA | statusFiscal={} | statusHttp={} | chave={} | duracaoMs={} | correlationId={}",
                statusFiscal, statusHttp, chave, duracaoMs, correlationId);
    }

    /**
     * Registra auditoria de registro de evento de cancelamento de NFS-e.
     */
    public void registrarCancelamento(String chaveAcesso, String cnpjAutor, Integer statusSefin, int statusHttp, long duracaoMs) {
        String chave = isNotBlank(chaveAcesso) ? chaveAcesso.trim() : "N/A";
        String cnpjMascarado = mascararCnpj(cnpjAutor);
        String statusFiscal = statusSefin != null ? String.valueOf(statusSefin) : "N/A";
        String correlationId = obterCorrelationId();

        auditLog.info("AUDIT-FISCAL: operacao=CANCELAMENTO | statusFiscal={} | statusHttp={} | chave={} | cnpjAutor={} | duracaoMs={} | correlationId={}",
                statusFiscal, statusHttp, chave, cnpjMascarado, duracaoMs, correlationId);
    }

    /**
     * Extrai o CNPJ do emitente contido no XML da DPS de forma leve via regex, sem parser DOM.
     */
    public String extrairCnpjDps(String xmlDps) {
        if (!isNotBlank(xmlDps)) {
            return null;
        }

        Matcher matcher = CNPJ_XML_PATTERN.matcher(xmlDps);
        if (matcher.find()) {
            return matcher.group(1);
        }

        Matcher fallback = GENERIC_CNPJ_PATTERN.matcher(xmlDps);
        if (fallback.find()) {
            return fallback.group(1);
        }

        return null;
    }

    /**
     * Extrai o identificador da DPS (atributo Id de infDPS) de forma leve via regex.
     */
    public String extrairIdDps(String xmlDps) {
        if (!isNotBlank(xmlDps)) {
            return null;
        }

        Matcher matcher = ID_DPS_PATTERN.matcher(xmlDps);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Aplica máscara de proteção LGPD ao CNPJ (ex: 12.xxx.xxx/0001-xx).
     */
    public String mascararCnpj(String cnpj) {
        if (!isNotBlank(cnpj)) {
            return "N/A";
        }

        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            return "N/A";
        }

        return digitos.substring(0, 2) + ".***.***/" + digitos.substring(8, 12) + "-**";
    }

    private String obterCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        return isNotBlank(correlationId) ? correlationId : "sistema";
    }

    private boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
