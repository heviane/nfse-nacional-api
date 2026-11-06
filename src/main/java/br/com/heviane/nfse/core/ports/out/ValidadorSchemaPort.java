package br.com.heviane.nfse.core.ports.out;

import java.util.List;

/**
 * Porta de saída para validação sintática e semântica de documentos fiscais
 * contra os esquemas XSD oficiais da NFS-e Nacional.
 * Suporta múltiplas versões de schema com auto-detecção dinâmica (v1.00 e v1.01 / NT-009).
 */
public interface ValidadorSchemaPort {

    /**
     * Valida o conteúdo XML de uma DPS com auto-detecção dinâmica de versão pelo atributo 'versao' do XML.
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
     * Valida o conteúdo XML de uma DPS (com auto-detecção dinâmica de versão) ou lança {@link br.com.heviane.nfse.core.domain.ValidacaoFiscalException}
     * caso haja qualquer desconformidade.
     *
     * @param xmlConteudo Conteúdo XML da DPS
     */
    void validarDpsOuFalhar(String xmlConteudo);

    /**
     * Valida o conteúdo XML de uma DPS contra uma versão específica ou lança {@link br.com.heviane.nfse.core.domain.ValidacaoFiscalException}
     * caso haja qualquer desconformidade.
     *
     * @param xmlConteudo Conteúdo XML da DPS
     * @param versao Versão do schema ("v1.00", "v1.01")
     */
    void validarDpsOuFalhar(String xmlConteudo, String versao);

    /**
     * Valida o conteúdo XML de um Pedido de Registro de Evento (pedRegEvento) com auto-detecção de versão.
     *
     * @param xmlConteudo Conteúdo XML do Pedido de Registro de Evento
     * @return Lista com descrições dos erros encontrados (vazia se o XML for válido)
     */
    List<String> validarPedRegEvento(String xmlConteudo);

    /**
     * Valida o conteúdo XML de um Pedido de Registro de Evento (pedRegEvento) contra uma versão específica.
     *
     * @param xmlConteudo Conteúdo XML do Pedido de Registro de Evento
     * @param versao Versão do schema ("v1.00", "v1.01")
     * @return Lista com descrições dos erros encontrados (vazia se o XML for válido)
     */
    List<String> validarPedRegEvento(String xmlConteudo, String versao);

    /**
     * Valida o conteúdo XML de um Pedido de Registro de Evento ou lança {@link br.com.heviane.nfse.core.domain.ValidacaoFiscalException}
     * caso haja qualquer desconformidade.
     *
     * @param xmlConteudo Conteúdo XML do Pedido de Registro de Evento
     */
    void validarPedRegEventoOuFalhar(String xmlConteudo);

    /**
     * Valida o conteúdo XML de um Pedido de Registro de Evento contra uma versão específica ou lança {@link br.com.heviane.nfse.core.domain.ValidacaoFiscalException}
     * caso haja qualquer desconformidade.
     *
     * @param xmlConteudo Conteúdo XML do Pedido de Registro de Evento
     * @param versao Versão do schema ("v1.00", "v1.01")
     */
    void validarPedRegEventoOuFalhar(String xmlConteudo, String versao);
}
