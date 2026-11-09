# SPEC-003: Exposição da API REST & Camada Anticorrupção (ACL GeneXus)

> **Status:** `Aprovado`  
> **Autor(a):** @heviane  
> **Data de Criação:** 2026-11-02 &bull; **Última Atualização:** 2026-11-02  
> **Épico Relacionado:** [Épico 6 — Exposição da API REST, Camada Anticorrupção & Deploy BrasilCloud](https://github.com/heviane/nfse-nacional-api/issues/7)  
> **Milestone:** [`v0.4.0 - API REST, Camada Anticorrupção & Deploy BrasilCloud`](https://github.com/heviane/nfse-nacional-api/milestone/4)

---

## 1. 🎯 Contexto e Objetivo de Negócio

A aplicação atua como um middleware desacoplado com dois propósitos fundamentais:
1. **API REST Moderna (Inovação):** Expor rotas padronizadas em JSON com DTOs limpos e documentação OpenAPI 3 (Swagger UI), permitindo que novos clientes ou sistemas modernos consumam o serviço sem conhecer complexidades de criptografia XML ou mTLS.
2. **Camada Anticorrupção (ACL - Continuidade do Cliente 001):** Emular o WebService SOAP legado (`/prefeitura/notafiscal/nfse.asmx`) consumido pelo ERP desenvolvido em **GeneXus 18**. O ERP apenas altera a URL de destino da prefeitura para a API, mantendo a operação transparente e sem necessidade de alteração de código no ERP.

---

## 2. 📋 Rotas da API REST (`/api/v1`)

### 2.1. Emissão de DPS: `POST /api/v1/dps/emitir`
* **Descrição:** Recebe o XML da DPS, valida sintática e semanticamente contra o XSD oficial, assina digitalmente com certificado A1 e transmite síncronamente ao Sefin Nacional.
* **Corpo da Requisição (JSON):**
  ```json
  {
    "xmlDps": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\">...</DPS>",
    "ambiente": "HOMOLOGACAO"
  }
  ```
* **Headers Opcionais (Multi-Tenant):**
  - `X-Certificado-Base64`: Conteúdo do arquivo `.pfx` em Base64.
  - `X-Certificado-Senha`: Senha do certificado digital.
  - *Nota:* Se não informados, a API utiliza o certificado padrão configurado no Tomcat via variáveis de ambiente (`NFSE_CERT_BASE64` e `NFSE_CERT_PASSWORD`).
* **Resposta de Sucesso (HTTP 200):**
  ```json
  {
    "sucesso": true,
    "codigoStatus": 100,
    "motivoStatus": "NFS-e gerada com sucesso",
    "chaveAcesso": "32052002601000000000019100001000000000000001123456",
    "xmlNfse": "<NFSe versao=\"1.00\">...</NFSe>"
  }
  ```

### 2.2. Consulta de NFS-e: `GET /api/v1/nfse/{chaveAcesso}`
* **Descrição:** Consulta uma NFS-e autorizada pela chave de acesso de 50 dígitos.
* **Parâmetro:** `{chaveAcesso}` (50 dígitos numéricos).
* **Query Param:** `ambiente=HOMOLOGACAO` (opcional, padrão `HOMOLOGACAO`).
* **Resposta (HTTP 200):** Objeto JSON com dados da nota e o XML oficial decodificado.

### 2.3. Cancelamento de NFS-e: `POST /api/v1/nfse/cancelar`
* **Descrição:** Registra o evento de cancelamento (Evento `101101`) no Sefin Nacional.
* **Corpo da Requisição (JSON):**
  ```json
  {
    "chaveAcesso": "32052002601000000000019100001000000000000001123456",
    "cnpjAutor": "00000000000191",
    "codigoMotivo": "1",
    "motivo": "Cancelamento solicitado devido a erro no preenchimento do tomador"
  }
  ```

---

## 3. 🔄 Camada Anticorrupção (ACL): Controlador SOAP GeneXus

### 3.1. Endpoint: `/prefeitura/notafiscal/nfse.asmx`
* **Método `GET` (com parâmetro `?WSDL`):** Retorna a definição WSDL XML que permite ao GeneXus 18 reconhecer o serviço como o antigo WebService municipal da SIL Tecnologia.
* **Método `POST` (Envelope SOAP):**
  - Recebe o envelope SOAP contendo a declaração de serviço.
  - Extrai a `<DPS>` contida no nó XML do payload (`<GerarNfseEnvio>` ou `<DPS>` direta, com suporte a texto claro, entidades escapadas `&lt;DPS...&gt;` ou CDATA). Operações de lote RPS legado (`<EnviarLoteRpsEnvio>`) não são aplicáveis ao fluxo síncrono da NFS-e Nacional.
  - Aciona o Core Fiscal (`EmitirDpsUseCase`) para validar, assinar e transmitir.
  - Empacota o retorno (chave de 50 dígitos e protocolo) em um envelope SOAP de resposta idêntico ao formato esperado pelo GeneXus.

---

## 4. ⚙️ Provedor de Certificado Digital em Memória

A resolução do certificado A1 segue a hierarquia:
1. **Prioridade 1:** Headers da requisição HTTP (`X-Certificado-Base64` / `X-Certificado-Senha`).
2. **Prioridade 2:** Variáveis de ambiente do servidor configuradas no `setenv.sh` do Tomcat:
   - `NFSE_CERT_BASE64`: Conteúdo do `.pfx` codificado em Base64.
   - `NFSE_CERT_PASSWORD`: Senha do certificado.

---

## 5. 📚 Documentação Interativa (OpenAPI 3 / Swagger)

* **Swagger UI:** Acessível em `/swagger-ui/index.html` (e redirecionamento em `/swagger-ui.html`).
* **OpenAPI JSON:** Acessível em `/v3/api-docs`.

