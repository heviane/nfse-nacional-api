package br.com.heviane.nfse.core.ports.out;

import br.com.heviane.nfse.core.domain.CertificadoA1;

/**
 * Porta de saída para assinatura digital de documentos fiscais da NFS-e Nacional
 * segundo o padrão W3C XML-DSig ICP-Brasil (RSA-SHA256).
 */
public interface AssinadorDpsPort {

    /**
     * Assina digitalmente o XML fiscal com o certificado digital A1 fornecido.
     * Auto-detecta a tag alvo a ser assinada (ex: infDPS para <DPS>, infPedReg para <pedRegEvento>).
     *
     * @param xml Conteúdo XML do documento fiscal com elemento identificável por atributo Id
     * @param certificado Certificado A1 contendo chave privada e certificado público em memória
     * @return XML assinado contendo o bloco Signature
     */
    String assinar(String xml, CertificadoA1 certificado);

    /**
     * Assina digitalmente o XML fiscal especificando explicitamente o nome da tag do elemento alvo a ser assinado.
     *
     * @param xml Conteúdo XML do documento fiscal com elemento identificável por atributo Id
     * @param tagElementoAlvo Nome da tag alvo a ser assinada (ex: "infDPS", "infPedReg")
     * @param certificado Certificado A1 contendo chave privada e certificado público em memória
     * @return XML assinado contendo o bloco Signature
     */
    String assinar(String xml, String tagElementoAlvo, CertificadoA1 certificado);

    /**
     * Valida matematicamente a assinatura digital contida em um XML de DPS assinado.
     *
     * @param xmlDpsAssinado Conteúdo XML da DPS contendo o elemento Signature
     * @return true se a assinatura for matematicamente válida contra a chave pública contida no KeyInfo
     */
    boolean validarAssinatura(String xmlDpsAssinado);

    /**
     * Compacta o XML assinado em GZIP e codifica em Base64 para envio ao Sefin Nacional (campo dpsXmlGZipB64).
     *
     * @param xmlDpsAssinado XML da DPS assinado
     * @return String codificada em Base64 do conteúdo compactado em GZIP
     */
    String gerarPayloadGZipB64(String xmlDpsAssinado);
}

