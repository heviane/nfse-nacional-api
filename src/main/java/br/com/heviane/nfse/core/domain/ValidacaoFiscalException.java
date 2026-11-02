package br.com.heviane.nfse.core.domain;

import java.util.Collections;
import java.util.List;

/**
 * Exceção lançada quando um documento fiscal (ex: DPS) falha nas regras
 * sintáticas ou semânticas de validação contra os schemas XSD oficiais.
 */
public class ValidacaoFiscalException extends RuntimeException {

    private final List<String> erros;

    public ValidacaoFiscalException(String mensagem, List<String> erros) {
        super(mensagem + (erros != null && !erros.isEmpty() ? ": " + String.join("; ", erros) : ""));
        this.erros = erros != null ? List.copyOf(erros) : Collections.emptyList();
    }

    public ValidacaoFiscalException(String mensagem) {
        this(mensagem, Collections.emptyList());
    }

    public List<String> getErros() {
        return erros;
    }
}

