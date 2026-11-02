# Sefin vs ADN

**Sefin Nacional** e **ADN** são duas camadas distintas do Sistema Nacional da NFS-e (Receita Federal/Comitê Gestor):

---

## 1. O que é "Sefin" (ou Sefin Nacional)?

* **Conceito:** É o **Emissor Público Nacional** e motor de processamento.
* **Função prática:** É a API de **entrada e emissão síncrona**. É nela que a sua aplicação envia o `POST` contendo a `<DPS>` assinada. A Sefin valida as regras de negócio, autoriza o documento, gera a Chave de Acesso e devolve a `<NFSe>` pronta.

---

## 2. O que é "ADN" (Ambiente de Dados Nacional)?

* **Conceito:** É o **repositório centralizado de dados e distribuição** da Receita Federal.
* **Função prática:** É a API de **guarda, eventos e sincronização**. Não serve primariamente para emitir a nota síncrona pelo contribuinte, mas sim para:
* Consultar notas emitidas/tomadas por Chave de Acesso ou por **NSU** (distribuição de DFe).
* Registrar e sincronizar eventos (como o cancelamento `e101101`).
* Compartilhar os dados fiscais entre União, Estados e Municípios.

---

## Qual é a diferença na prática da integração?

| Aspecto | Sefin Nacional | ADN (Ambiente de Dados Nacional) |
| --- | --- | --- |
| **Papel principal** | Emissor/Processador síncrono.
 | Repositório central e distribuidor.
 |
| **Operação típica** | `POST` da DPS para emissão da NFS-e.
 | `GET` de notas por NSU / Chave e eventos fiscais.
 |
| **Analogia com NF-e** | O webservice autorizador da SEFAZ (`NFeAutorizacao`). | O serviço de Distribuição de DFe (`NFeDistribuicaoDFe`). |

Você envia a DPS para a **Sefin Nacional** para processar e gerar a nota; a nota autorizada passa a residir no **ADN**, de onde ela é consultada e distribuída aos participantes.

---
