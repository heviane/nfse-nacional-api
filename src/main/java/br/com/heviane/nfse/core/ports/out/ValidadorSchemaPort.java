package br.com.heviane.nfse.core.ports.out;

import java.util.List;

/**
 * Porta de saída para validação sintática e semântica de documentos fiscais
 * contra os esquemas XSD oficiais da NFS-e Nacional.
 */
public interface ValidadorSchemaPort {

    /**
     * Valida o conteúdo XML de uma DPS contra o schema XSD oficial padrão (v1.00).
     *
     * @param xmlConteudo Conteúdo XML da DPS
     * @return Lista com descrições dos erros encontrados (vazia se o XML for válido)
     */
    List<String> validarDps(String xmlConteudo);

    /**
     * Valida o conteúdo XML de uma DPS contra uma versão específica do schema (ex: "v1.00", "v1.01").
     *
     * @param xmlConteudo Conteúdo XML da DPS
     * @param versao Versão do schema ("v1.00", "v1.01")
     * @return Lista com descrições dos erros encontrados (vazia se o XML for válido)
     */
    List<String> validarDps(String xmlConteudo, String versao);

    /**
     * Valida o conteúdo XML de uma DPS ou lança {@link br.com.heviane.nfse.core.domain.ValidacaoFiscalException}
     * caso haja qualquer desconformidade.
     *
     * @param xmlConteudo Conteúdo XML da DPS
     */
    void validarDpsOuFalhar(String xmlConteudo);
}
