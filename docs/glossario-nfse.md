# Glossário Fiscal — Sistema Nacional da NFS-e

> **Documento Vivo de Domínio Tributário, Conceitos do Sefin Nacional & Regras Fiscais**  
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)  
> *Este glossário reúne termos essenciais sobre a integração com a NFS-e Nacional, criptografia de documentos fiscais e regras da Receita Federal do Brasil.*

---

## 📌 Índice Temático

1. [Ecossistema Nacional & Arquitetura Governamental](#1-ecossistema-nacional--arquitetura-governamental)
2. [Documentos, Estruturas & Identificadores Fiscais](#2-documentos-estruturas--identificadores-fiscais)
3. [Criptografia, Segurança & Transporte Seguro](#3-criptografia-segurança--transporte-seguro)
4. [Regras Tributárias & Legislação](#4-regras-tributárias--legislação)

---

## 1. Ecossistema Nacional & Arquitetura Governamental

### Sefin Nacional (Emissor Público Nacional)
A API de **entrada e emissão síncrona** da NFS-e Nacional. É o serviço autorizador da Receita Federal/Serpro onde a aplicação contribuinte envia o `POST` contendo a `<DPS>` assinada. A Sefin valida regras de negócio, calcula tributos, autoriza o documento e gera a Chave de Acesso definitiva.  
* *Analogia com a NF-e:* Equivale ao webservice autorizador da SEFAZ (`NFeAutorizacao`).

### ADN (Ambiente de Dados Nacional)
O **repositório centralizado de dados, eventos e distribuição** mantido pela Receita Federal. Não serve para a emissão síncrona primária da nota, mas sim para:
* Consultar documentos emitidos/tomados por Chave de Acesso ou por **NSU** (distribuição de DFe).
* Registrar e consultar eventos fiscais (como o cancelamento `e101101`).
* Compartilhar os dados tributários entre União, Estados e Municípios conveniados.  
* *Analogia com a NF-e:* Equivale ao webservice de Distribuição de DFe (`NFeDistribuicaoDFe`).

### Sefin vs. ADN: Diferença Prática

| Aspecto | Sefin Nacional | ADN (Ambiente de Dados Nacional) |
| :--- | :--- | :--- |
| **Papel Principal** | Emissor/Processador síncrono da nota. | Repositório central de consulta e distribuição. |
| **Operação Típica** | `POST /API/SefinNacional/nfse` (envio da DPS). | `GET` de documentos por NSU / Chave e eventos fiscais. |
| **Fluxo** | Você envia a DPS para a **Sefin Nacional** para processar e gerar a nota; a nota autorizada passa a residir no **ADN**. |

---

## 2. Documentos, Estruturas & Identificadores Fiscais

### DPS (Declaração de Prestação de Serviços)
O documento XML gerado e assinado digitalmente pelo contribuinte (`<DPS versao="1.00">`). Contém os dados da prestação de serviços, prestador, tomador e valores. **No Emissor Nacional, o contribuinte nunca gera a tag `<NFSe>`; ele gera exclusivamente a `<DPS>`**.

### NFS-e (Nota Fiscal de Serviços Eletrônica)
O documento fiscal eletrônico oficial emitido e assinado pela autoridade tributária (Sefin Nacional) após a aprovação da DPS enviada pelo contribuinte.

### Chave de Acesso Nacional (50 Dígitos)
Identificador único e nacional da NFS-e autorizada, composto por exatamente **50 dígitos numéricos** (código do município, ano/mês, CNPJ/CPF, modelo, série, número da nota e dígito verificador). Diferente de padrões municipais antigos que utilizavam prefixos de texto (ex: `NFS...` de 53 caracteres).

### NSU (Número Sequencial Único)
Identificador numérico sequencial gerado pelo ADN para cada documento ou evento vinculado a um CNPJ. É utilizado para consultar documentos de forma incremental sem sobrecarregar a base da Receita.

### Evento Fiscal (ex: Cancelamento `e101101`)
Ato ou fato relacionado a uma NFS-e após sua emissão. O cancelamento é registrado via evento `101101`, referenciando obrigatoriamente a Chave de Acesso Nacional de 50 dígitos.

### XSD (XML Schema Definition)
Esquema formal que valida a sintaxe, obrigatoriedade de campos e tipos de dados do XML antes do envio (ex: `DPS_v1.00.xsd` e `DPS_v1.01.xsd`).

### JAXB (Java Architecture for XML Binding)
Padrão da plataforma Java que permite mapear classes Java diretamente para documentos XML e vice-versa, garantindo serialização estrita contra os esquemas XSD.

---

## 3. Criptografia, Segurança & Transporte Seguro

### Certificado Digital ICP-Brasil A1 (PKCS#12)
Arquivo digital criptográfico (`.pfx` ou `.p12`) que armazena a chave pública e a chave privada do contribuinte, com validade de 1 ano. Utilizado para autenticação mTLS e assinatura da DPS.

### mTLS (Mutual Transport Layer Security)
Handshake de segurança em nível de rede onde cliente e servidor trocam e validam seus respectivos certificados SSL/TLS antes de qualquer tráfego HTTP.

### XML Signature (W3C XML-DSig / RSA-SHA256)
Padrão internacional de assinatura digital XML. Na NFS-e Nacional, é obrigatório o uso de **RSA-SHA256** e Digest **SHA-256** com transformações `enveloped-signature` e `Exclusive C14N`, assinando a tag `<infDPS Id="...">`.

### JCA (Java Cryptography Architecture)
Framework nativo da JVM para operações criptográficas de assinatura, certificados e leitura de keystores PKCS#12 em memória.

---

## 4. Regras Tributárias & Legislação

### Resolução CGSN nº 191/2026
Norma do Comitê Gestor do Simples Nacional que determinou a obrigatoriedade de emissão pelo Emissor Nacional da NFS-e para microempresas (ME) e empresas de pequeno porte (EPP) a partir de 1º de novembro de 2026.

### Simples Nacional (`opSimpNac` e `regApTribSN`)
Campos obrigatórios no XML da DPS (`<prest><regTrib>`) para empresas optantes pelo Simples Nacional:
* `opSimpNac = 1` (Optante Simples Nacional).
* `regApTribSN = 1` (Regime de apuração do Simples Nacional).

### Retenções na Fonte (ISSQN e Tributos Federais)
* `tribISSQN = 1` e `tpRetISSQN = 2` (sem retenção de ISSQN pelo tomador).
* `tpRetPisCofins = 2` (sem retenção de PIS/COFINS na fonte para optantes do Simples Nacional).
