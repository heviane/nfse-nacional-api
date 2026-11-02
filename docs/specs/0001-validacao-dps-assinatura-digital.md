# SPEC-001: Validação de DPS e Assinatura Digital A1

> **Status:** `Aprovado`  
> **Autor(a):** @heviane  
> **Data de Criação:** 2026-11-02 &bull; **Última Atualização:** 2026-11-02  
> **Épico Relacionado:** [Épico 3 — Engenharia de Requisitos & Especificação Técnica (SDD)](https://github.com/heviane/nfse-nacional-api/issues/4)  
> **Milestone:** [`v0.2.0 - Core Fiscal & Validação XSD`](https://github.com/heviane/nfse-nacional-api/milestone/2)

---

## 1. 🎯 Contexto e Objetivo de Negócio

No novo **Ambiente Nacional da NFS-e (Sefin Nacional / ADN)**, a emissão de uma nota fiscal é precedida pelo envio de uma **DPS (Declaração de Prestação de Serviços)**. O Sefin Nacional atua de forma síncrona, validando e rejeitando imediatamente qualquer documento que não obedeça:
1. Às regras estritas de formato definidas no **Schema Oficial XSD (`DPS_v1.00.xsd`)**.
2. Aos padrões criptográficos do ICP-Brasil para **Assinatura Digital XML (W3C XML-DSig)** utilizando **RSA-SHA256**.

Esta especificação define os requisitos funcionais, critérios de aceite em Gherkin, regras de criptografia e contratos de interface para a **Fatia 1 (Core Fiscal: Validação e Assinatura da DPS)**.

---

## 2. 📋 Requisitos Funcionais (RF)

| ID | Requisito | Prioridade | Critérios de Aceite (Gherkin / Cenários) |
| :--- | :--- | :---: | :--- |
| **RF-01** | **Validação Prévia contra Schema XSD Oficial** | `P0` | **Dado** um XML de DPS gerado,<br>**Quando** submetido ao validador XSD interno,<br>**Então** deve validar contra `DPS_v1.00.xsd` e retornar lista vazia de erros se válido, ou lançar `XsdValidationException` detalhando as tags e linhas inconsistentes. |
| **RF-02** | **Identificação do Nó Alvo (`infDPS`)** | `P0` | **Dado** o XML da DPS com namespace `http://www.sped.fazenda.gov.br/nfse`,<br>**Quando** for preparado para assinatura,<br>**Então** deve localizar o elemento `<infDPS>` e referenciar seu identificador único no atributo `Id` (`URI="#DPS..."`). |
| **RF-03** | **Assinatura Digital com Certificado A1 (RSA-SHA256)** | `P0` | **Dado** um certificado ICP-Brasil A1 (PKCS#12) carregado em memória,<br>**Quando** assinar o elemento `<infDPS>`,<br>**Então** deve gerar o bloco `<Signature>` como filho direto da raiz `<DPS>` (irmão de `<infDPS>`), utilizando algoritmo de assinatura **RSA-SHA256** e Digest **SHA-256**. |
| **RF-04** | **Transformações C14N e Enveloped** | `P0` | **Dado** a geração da referência da assinatura,<br>**Quando** aplicar as transformações W3C,<br>**Então** deve aplicar rigorosamente: (1) `enveloped-signature` e (2) `xml-exc-c14n#` (Exclusive Canonicalization). |
| **RF-05** | **Geração do Payload de Envio (GZIP + Base64)** | `P0` | **Dado** o XML completo da `<DPS>` assinado,<br>**Quando** preparar o payload para o Sefin Nacional,<br>**Então** deve compactar o XML UTF-8 em GZIP e encodar em Base64, gerando a string para o campo `dpsXmlGZipB64`. |
| **RF-06** | **Segurança de Certificado em Memória** | `P0` | **Dado** o carregamento de um certificado digital A1,<br>**Quando** instanciar a `KeyStore`,<br>**Então** o certificado e a senha devem ser processados exclusivamente via streams de memória, sendo terminantemente proibida a persistência em arquivos temporários desprotegidos. |

---

## 3. ⚙️ Requisitos Não-Funcionais (RNF)

* **Performance Criptográfica:** O tempo total para carregar o certificado (se já em cache de sessão), validar o XSD, assinar a DPS e gerar o Base64 GZIP deve ser **inferior a 150ms**.
* **Imutabilidade e Thread-Safety:** Os componentes de assinatura e validação devem ser *thread-safe*, suportando emissões concorrentes sem interferência de estado.
* **Compatibilidade Normativa:** Alinhamento estrito com o **Manual de Integração do Emissor Público Nacional (v1.2+)** e notas técnicas vigentes da Receita Federal.
* **Isolamento Tecnológico:** O Core Fiscal não deve ter dependências do Spring MVC ou servlets, residindo no módulo `core` da Arquitetura Hexagonal.

---

## 4. 🏛️ Design das Portas e Contratos do Core Fiscal

Seguindo a [ADR-004](../adrs/0004-padrao-arquitetural-hexagonal-acl.md), os contratos são estruturados em interfaces puras:

### 4.1. Porta de Saída do Assinador (`core.ports.out.AssinadorDpsPort`)
```java
package br.com.heviane.nfse.core.ports.out;

import br.com.heviane.nfse.core.domain.CertificadoA1;

public interface AssinadorDpsPort {
    /**
     * Assina digitalmente o XML da DPS com certificado ICP-Brasil A1.
     *
     * @param xmlDpsConteudo XML da DPS montado com a tag <infDPS Id="...">
     * @param certificado Objeto de domínio contendo bytes da keystore PKCS#12 e senha
     * @return XML completo da <DPS> contendo a tag <Signature> gerada
     */
    String assinar(String xmlDpsConteudo, CertificadoA1 certificado);
}
```

### 4.2. Porta de Saída do Validador XSD (`core.ports.out.ValidadorSchemaPort`)
```java
package br.com.heviane.nfse.core.ports.out;

import java.util.List;

public interface ValidadorSchemaPort {
    /**
     * Valida um documento XML contra o schema XSD oficial da DPS.
     *
     * @param xmlConteudo String com o conteúdo XML a validar
     * @return Lista de mensagens de erro de validação (vazia se válido)
     */
    List<String> validarDps(String xmlConteudo);
}
```

---

## 5. 🔍 Análise de Compatibilidade do `assinador-xml-java` (Gap Analysis)

Inspecionamos a implementação pré-existente em `E:\Projects\GitHub\heviane\assinador-xml-java\src\main\java\com\assinador\AssinadorXml.java`. 

### Diagnóstico de Diferenças & Ajustes Obrigatórios:

| Característica | `assinador-xml-java` (Atual) | Requisito Sefin Nacional (NFS-e) | Ação de Adaptação |
| :--- | :--- | :--- | :--- |
| **Algoritmo de Assinatura** | `RSA_SHA1` (Obsoleto) | `RSA-SHA256` (`http://www.w3.org/2001/04/xmldsig-more#rsa-sha256`) | **Alterar para SHA-256.** O Sefin Nacional rejeita imediatamente assinaturas SHA-1. |
| **Digest Method** | `DigestMethod.SHA1` | `http://www.w3.org/2001/04/xmlenc#sha256` | **Alterar para SHA-256.** |
| **Transformações** | Apenas `ENVELOPED` | `ENVELOPED` + `Exclusive C14N` | **Incluir a transformação C14N exclusiva** na lista de transformações da referência. |
| **Nó Pai da Assinatura** | Insere `<Signature>` dentro de `signatureParent` | A `<Signature>` é filha direta da raiz `<DPS>` | **Corrigir ponto de inserção:** A assinatura deve ser anexada como filha de `<DPS>`, referenciando o `URI="#DPS..."`. |
| **Origem do Certificado** | Lê arquivo físico do disco via `FileInputStream` | Certificados manipulados em memória a partir de `byte[]` | **Sobrecarga em memória:** Adicionar método que recebe `byte[]` ou `InputStream` direto da `KeyStore`. |
| **Filtros de String Específicos** | Hardcoded para provedor SIL Tecnologia (`replace("&#13;", "")`) | Padrão SPED Nacional W3C canônico | **Limpar sanitizações desnecessárias:** Utilizar serialização XML limpa e padrão W3C. |

---

## 6. 🧪 Estratégia de Testes Automatizados (TDD para o Épico 4)

- [ ] **Teste 1 (Validação XSD Válida):** Submeter XML de DPS sintética válida e garantir retorno sem erros.
- [ ] **Teste 2 (Validação XSD Inválida):** Omitir campos obrigatórios (ex: `<cTribNac>`, `<CNPJ>`) e garantir que a exceção detalhe a regra violada.
- [ ] **Teste 3 (Assinatura RSA-SHA256):** Assinar a DPS com certificado A1 de teste e validar matematicamente a assinatura contra a chave pública contida no `KeyInfo`.
- [ ] **Teste 4 (Serialização GZIP Base64):** Testar descompactação e decodificação do Base64 garantindo que o XML retornado é idêntico ao original assinado em UTF-8.
