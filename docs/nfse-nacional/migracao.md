# Migração

## Mensagem da Prefeitura

Mensagem que apareceu no site da PMVV , após login:

```textplain
"A Prefeitura de Vila Velha, por meio da Secretaria Municipal de Finanças (SEMFI), informa que, a partir de 1º de novembro de 2026, as microempresas (ME) e empresas de pequeno porte (EPP) optantes pelo Simples Nacional deverão emitir suas Notas Fiscais de Serviços eletrônicas (NFS-e) pelo Emissor Nacional, conforme a Resolução CGSN nº 191/2026.

A emissão poderá ser realizada diretamente pelo Emissor Web ou por integração via API, mediante sistemas próprios ou softwares de gestão. A medida visa padronizar os procedimentos fiscais em âmbito nacional e preparar os contribuintes para a Reforma Tributária sobre o Consumo.

A SEMFI orienta as empresas a consultarem o [Portal Nacional da NFS-e](https://www.gov.br/nfse/pt-br) e a adotarem as providências necessárias para se adequar à nova exigência. As regras relativas ao IBS e à CBS para optantes pelo Simples Nacional deverão ser observadas conforme o cronograma e a regulamentação aplicável a partir de 2027."
```

## Análise

A mensagem exibida no portal da Prefeitura de Vila Velha (SEMFI) confirma formalmente a migração e traz um impacto decisivo para a arquitetura do seu sistema: **a partir de 1º de novembro de 2026, a emissão via webservice próprio municipal deixa de ser a via aceita para ME/EPP do Simples Nacional**, passando a ser exigida a transmissão direta ao **Emissor Nacional da NFS-e** (Ambiente de Dados Nacional - ADN / Receita Federal).

Abaixo está o detalhamento prático do que muda na sua integração para atender a essa exigência.

---

### 1. Mudança de Protocolo: Transição do Web Service

* **Situação Atual (SIL Tecnologia):** Você realizava requisições via **SOAP / XML** apontando para o endpoint local da prefeitura (`/prefeitura/notafiscal/nfse.asmx` ou `.svc`).
* **Nova Exigência (Emissor Nacional / ADN):**
* A integração direta oficial com a API do Sistema Nacional da NFS-e (gov.br/nfse) opera via **API REST** (com autenticação mTLS via Certificado Digital A1).
* O envio de requisições de emissão ao Ambiente Nacional é feito por chamadas **REST (HTTP POST)** trafegando JSON ou XML da DPS.

---

### 2. O que muda no Payload de Emissão: Enviar somente a `<DPS>`

Nos testes de homologação na SIL, você montava e assinava o invólucro completo da nota autorizada (`<NFSe><infNFSe>...<DPS>...</DPS></infNFSe></NFSe>`).

No Emissor Nacional, **o contribuinte nunca gera a tag `<NFSe>` nem o bloco `<infNFSe>**`:

* **O que sua aplicação deve gerar:** Exclusivamente o bloco raiz **`<DPS>`** com o seu identificador (`<infDPS Id="...">`) assinado com o Certificado Digital.
* **Papel do Emissor Nacional:** O servidor da RFB/ADN processa a DPS recebida, gera o número da nota, apura os tributos e devolve a casca `<NFSe>` contendo a Chave de Acesso definitiva e a assinatura da autoridade tributária.

---

### 3. Identificadores e Chaves de Acesso

| Elemento | Padrão Local SIL (Vila Velha) | Padrão Emissor Nacional (ADN) |
| --- | --- | --- |
| **Id da DPS no envio** | `infDPS Id="DPS..."` (46 posições)
 | **Mantido**: `infDPS Id="DPS..."` (46 posições).
 |
| **Identificador da Nota (`infNFSe`)** | Formato próprio local de 53 posições iniciado por `NFS...`
 | Chave de Acesso Nacional padronizada de **50 posições numéricas**.
 |
| **Eventos / Cancelamento** | O evento aceitava a referência interna municipal ou chave local.
 | Cancelamento (evento `101101`) exige a tag `<chNFSe>` contendo exatamente a **Chave de Acesso Nacional de 50 dígitos** retornada na emissão.
 |

---

### 4. Parametrizações de Tributos e Simples Nacional

Como Vila Velha determinou o cumprimento da Resolução CGSN nº 191/2026, seu emissor deve observar as seguintes regras já validadas no ADN:

* **Identificação do Simples Nacional:** Preenchimento obrigatório de `opSimpNac = 1` e `regApTribSN = 1` dentro do nó `<prest><regTrib>`.
* **PIS/COFINS:** Informar `tpRetPisCofins = 2` (sem retenção de PIS/COFINS na fonte).
* **Reforma Tributária (IBS/CBS):** Conforme a própria nota da SEMFI, as regras de cálculo e retenção efetiva de IBS/CBS para optantes do Simples Nacional passam a produzir efeitos a partir de **2027**. Para o envio em 2026, seu sistema deve apenas validar esquemas atualizados (como NT 009) sem exigir a liquidação ou destaque das alíquotas cheias desses tributos no momento da emissão.

---
