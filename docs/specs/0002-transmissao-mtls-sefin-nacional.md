# SPEC-002: Transmissão HTTP REST & Handshake mTLS com o Sefin Nacional

> **Status:** `Aprovado`  
> **Autor(a):** @heviane  
> **Data de Criação:** 2026-11-02 &bull; **Última Atualização:** 2026-11-02  
> **Épico Relacionado:** [Épico 5 — Transmissão & Comunicação com o Sefin Nacional / ADN (mTLS)](https://github.com/heviane/nfse-nacional-api/issues/6)  
> **Milestone:** [`v0.3.0 - Transmissão & Comunicação mTLS Sefin`](https://github.com/heviane/nfse-nacional-api/milestone/3)

---

## 1. 🎯 Contexto e Objetivo de Negócio

No modelo do **Ambiente Nacional da NFS-e**, a comunicação com os servidores centrais da Receita Federal (**Sefin Nacional / ADN**) é realizada exclusivamente via chamadas **HTTP REST** protegidas por **autenticação mTLS (Mutual TLS 1.2+)**. 

O servidor do Sefin exige que o cliente apresente um Certificado Digital ICP-Brasil A1 válido durante o aperto de mãos (*TLS Handshake*), autenticando simultaneamente a identidade da empresa emitente ou procurador fiscal antes de liberar qualquer tráfego de dados.

Esta especificação define os contratos de transporte, modelos de dados, tratamentos de status síncrono e arquitetura da porta de transmissão (`TransmissorSefinPort`) para o **Épico 5**.

---

## 2. 🌐 Ambientes e Endpoints Oficiais

| Ambiente | Base URL | Finalidade |
| :--- | :--- | :--- |
| **Homologação** (Produção Restrita) | `https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional` | Validação de ponta a ponta, testes de integração do ERP e homologação fiscal sem validade jurídica. |
| **Produção** | `https://sefin.nfse.gov.br/API/SefinNacional` | Emissão real de notas fiscais com efeito jurídico e fiscal pleno. |

### Rotas REST

1. **Emissão de DPS (Síncrona):** `POST /nfse`
   - **Headers:** `Content-Type: application/json; charset=utf-8`, `Accept: application/json`
   - **Payload:** `{"dpsXmlGZipB64": "<base64 do XML da DPS assinado e compactado em GZIP>"}`
2. **Consulta de NFS-e por Chave:** `GET /nfse/{chaveAcesso}`
   - **Headers:** `Accept: application/json`
   - **Parâmetro:** `{chaveAcesso}` com 50 dígitos numéricos.
3. **Registro de Eventos (Cancelamento):** `POST /eventos`
   - **Headers:** `Content-Type: application/json; charset=utf-8`, `Accept: application/json`
   - **Payload:** `{"pedidoRegistroEventoXmlGZipB64": "<base64 do XML pedRegEvento compactado>"}`

---

## 3. 📋 Requisitos Funcionais (RF)

| ID | Requisito | Prioridade | Critérios de Aceite (Gherkin / Cenários) |
| :--- | :--- | :---: | :--- |
| **RF-01** | **Handshake mTLS em Memória** | `P0` | **Dado** um `CertificadoA1` carregado em memória,<br>**Quando** instanciar a conexão HTTP para o Sefin,<br>**Então** deve configurar o `SSLContext` com TLSv1.2+ e injetar as chaves diretamente da memória sem tocar o disco. |
| **RF-02** | **Transmissão Síncrona de DPS** | `P0` | **Dado** uma DPS assinada válida,<br>**Quando** enviada via `POST /nfse` no ambiente configurado,<br>**Então** deve compactar em GZIP, converter em Base64 e despachar com cabeçalhos HTTP adequados. |
| **RF-03** | **Tratamento de Sucesso (HTTP 200 / Status 100)** | `P0` | **Dado** retorno HTTP 200 com `codigoStatus = 100`,<br>**Quando** processar o payload de resposta,<br>**Então** deve extrair a `chaveAcesso` (50 dígitos), descompactar o `nfseXmlGZipB64` e retornar o objeto de domínio `RespostaEmissaoSefin`. |
| **RF-04** | **Tratamento de Rejeições e Falhas Fiscais** | `P0` | **Dado** retorno do Sefin com código de erro ou status != 100,<br>**Quando** analisar a resposta,<br>**Então** deve lançar `SefinTransmissaoException` contendo o código de status e descrição detalhada da inconsistência. |
| **RF-05** | **Consulta por Chave de Acesso (50 dígitos)** | `P1` | **Dado** uma chave de acesso válida de 50 dígitos,<br>**Quando** consultar via `GET /nfse/{chaveAcesso}`,<br>**Então** deve retornar a situação da nota e o XML da NFS-e autorizada. |
| **RF-06** | **Tratamento de Resiliência e Timeouts** | `P1` | **Dado** lentidão ou indisponibilidade temporária do Sefin Nacional,<br>**Quando** atingir o timeout configurado (padrão 30s),<br>**Então** deve capturar a falha e informar erro de timeout tratável pelo ERP. |

---

## 4. 🏛️ Design das Portas e Contratos do Core Fiscal

### 4.1. Enum de Ambiente (`core.domain.AmbienteSefin`)

```java
package br.com.heviane.nfse.core.domain;

public enum AmbienteSefin {
    HOMOLOGACAO("https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional", 2),
    PRODUCAO("https://sefin.nfse.gov.br/API/SefinNacional", 1);

    private final String baseUrl;
    private final int codigo;

    AmbienteSefin(String baseUrl, int codigo) {
        this.baseUrl = baseUrl;
        this.codigo = codigo;
    }

    public String getBaseUrl() { return baseUrl; }
    public int getCodigo() { return codigo; }
}
```

### 4.2. Porta de Saída do Transmissor (`core.ports.out.TransmissorSefinPort`)

```java
package br.com.heviane.nfse.core.ports.out;

import br.com.heviane.nfse.core.domain.*;

public interface TransmissorSefinPort {
    /**
     * Transmite síncronamente a DPS assinada ao Sefin Nacional.
     */
    RespostaEmissaoSefin emitirDps(RequisicaoEnvioDps requisicao, CertificadoA1 certificado);

    /**
     * Consulta a situação de uma NFS-e a partir de sua Chave de Acesso de 50 dígitos.
     */
    RespostaConsultaNfse consultarPorChave(String chaveAcesso, AmbienteSefin ambiente, CertificadoA1 certificado);
}
```

---

## 5. 🧪 Estratégia de Testes Automatizados (TDD para o Épico 5)

- [ ] **Teste 1 (Handshake & Criação do SSLContext):** Validar que `SSLContextFactory` instancia o contexto TLS com a chave privada e certificado público do `CertificadoA1`.
- [ ] **Teste 2 (Emissão Autorizada — Status 100):** Simular resposta HTTP 200 com JSON contendo `codigoStatus = 100`, `chaveAcesso` de 50 dígitos e `nfseXmlGZipB64`, validando a descompactação correta do XML da NFS-e.
- [ ] **Teste 3 (Rejeição Fiscal):** Simular retorno HTTP 200 com `codigoStatus = 204` ("Duplicidade de DPS") e garantir que `SefinTransmissaoException` é lançada com os detalhes do erro.
- [ ] **Teste 4 (Erro HTTP 400/500):** Simular falha de rede ou HTTP 500 do Sefin, garantindo tratamento seguro sem quebra da JVM.
- [ ] **Teste 5 (Consulta por Chave):** Simular chamada `GET /nfse/{chaveAcesso}` e verificar parse da resposta com descompactação da nota autorizada.

