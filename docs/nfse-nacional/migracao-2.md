# Migração

Para migrar a sua emissão de NFS-e do modelo legado municipal (SIL Tecnologia) para a integração direta com o **Ambiente Nacional**, os requisitos e passos técnicos dividem-se em quatro pilares fundamentais:

---

## 1. Requisitos de Infraestrutura e Autenticação

* **Certificado Digital A1:** A conexão direta com o Sistema Nacional exige autenticação mTLS (*mutual Transport Layer Security*) com certificado ICP-Brasil (e-CNPJ padrão A1) da empresa emitente ou procurador fiscal habilitado.
* **Cliente HTTP/REST:** Substituição do cliente SOAP/XML por um cliente HTTP REST moderno capaz de executar requisições `POST` e `GET` com TLS 1.2+ e mTLS configurado no handshake.

---

## 2. Mudança Fundamental no Payload (Envelope de Envio)

* **Envio estrito da `<DPS>`:** Você não monta nem assina mais a casca exterior `<NFSe>` ou `<infNFSe>`.
* **Assinatura da Declaração:** A sua aplicação gera e assina digitalmente (XML Signature - SHA-256) **exclusivamente a tag raiz `<DPS>**` (com seu atributo identificador `infDPS Id="DPS..."` de 46 posições).
* **Geração da Nota:** O Sefin Nacional recebe a DPS assinada, valida o documento, gera a Chave de Acesso Nacional e devolve a `<NFSe>` processada.

---

## 3. Identificadores e Persistência de Dados

* **Chave de Acesso Nacional de 50 dígitos:** A resposta do Sefin retorna a nota com uma chave padronizada de 50 dígitos numéricos (sem prefixo de texto como o antigo `NFS...` de 53 caracteres).
* **Banco de Dados:** Seu sistema deve estar preparado para persistir essa chave de 50 dígitos, pois ela é obrigatória para:
* Consultar a nota gerada.
* Montar os identificadores dos eventos de cancelamento (`e101101` com `infPedReg` de 59 chars e `infEvento` de 62 chars).

---

## 4. Parametrização Fiscal Obrigatória (Simples Nacional e Schemas)

* **Regime Tributário do Emitente:** Obrigatoriedade de preenchimento dos campos no nó `<prest><regTrib>`:
* `opSimpNac = 1` (Optante Simples Nacional).
* `regApTribSN = 1` (Regime de apuração do SN).
* **Tributação Federal:** Informar `tpRetPisCofins = 2` (sem retenção de PIS/COFINS na fonte).
* **Atualização de Schemas (XSDs):** Atualizar as bibliotecas locais com os schemas oficiais mais recentes (ex.: NT 009 / `DPS_v1.01.xsd`), garantindo que:
* O parser aceite nós relacionados à Reforma Tributária (IBS/CBS e meios de pagamento) sem acusar elemento desconhecido.
* Suporte a validações de CNPJ alfanumérico.

---

### 5. Documentação e Ambientes de Teste

Para homologar a integração, utilize os manuais e Swagger oficiais da **RFB**:

* **Manual da API do Emissor Público Nacional (Sefin):** Contém os contratos do `POST /nfse` e tratamento de erros.
* **Swagger de Homologação (Produção Restrita):** [Console Swagger Sefin Nacional](https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional/docs/) para testar a transmissão do JSON/XML da DPS.
* **Swagger ADN Contribuintes:** [Console Swagger ADN Contribuintes](https://adn.producaorestrita.nfse.gov.br/contribuintes/docs/index.html) para testes de eventos e consulta de DFe.

Deseja que eu monte o exemplo do payload HTTP (headers e body serializado) para a chamada de emissão REST no Sefin Nacional?
