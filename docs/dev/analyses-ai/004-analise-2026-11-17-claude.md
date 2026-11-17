# Análise do `nfse-nacional-api` (17/11/2026)

Revisei o código, os XSDs, os testes e a documentação. A maior parte dos itens das análises anteriores (001, 002, 003) já foi resolvida: retry sem engolir exceções de domínio, ambiente autoritativo, cancelamento assinado com Id `PRE…`, coerência do Id da DPS, reconciliação por `GET /dps/{id}`, cache de `CertificadoA1`. Abaixo estão os problemas que **ainda existem**, em ordem de gravidade.

---

## 🔴 Alto

### 1. O contrato de resposta SOAP está documentado de forma inconsistente (e o teste "prova" o contrário)

`montarRespostaSoapSucesso` escapa os valores internos (`escapeXml(xml)`) e depois escapa o conjunto todo (`escapeXml(interno)`). Isso gera **escape duplo** no `XmlNfse`, o que bate com o teste `deveRetornarEnvelopeSoapComEscapeDuploPreservandoCaracteresEspeciaisParaDesserializacaoGeneXus`. Dois problemas:

- `ChaveAcesso` é escapada uma vez a mais do que a análise 003 sugeria, mas isso é inofensivo (só dígitos).
- O `cliente-001-servico-legado-genexus.md` §4.2 mostra o exemplo com `&amp;lt;NFSe` e o `manual-execucao-local` §8 (Passo 2.5) mostra `<RetornoEmissao>` **sem nenhum escape**. Os dois documentos se contradizem e nenhum foi validado contra o retorno real da SIL.

**Ação:** o teste simula o parser do GeneXus, mas é uma _suposição sua_ sobre o parser. Antes da virada, valide contra o `retorno_SUCESSO.xml` histórico da SIL (ou uma resposta real capturada) e transforme esse arquivo em fixture de teste. Atualize o manual para refletir o formato real.

### 2. `EmitirDpsService`: o regex de `validarCoerenciaIdDps` falha silenciosamente e pode dar falso positivo

```java
Pattern.compile("<prest>[\\s\\S]*?<CNPJ>(\\d+)</CNPJ>[\\s\\S]*?</prest>")
```

- Se o XML tiver prefixo de namespace (`<ns:prest>`) ou atributos em `<prest>`, nada casa, `temCnpj=false` e **a validação é pulada sem aviso**.
- `<serie>` e `<nDPS>` são buscados no documento inteiro: um `<nDPS>` dentro de `<subst>` ou de um documento dedutível poderia ser capturado antes do correto.
- O mesmo vale para `validarTipoAmbiente` (`<tpAmb>` sem escopo) e para `extrairIdDps` no `FiscalAuditLogger`.

Como o projeto já tem `ValidadorSchemaService` com DOM/JAXP, o ideal é parsear **uma vez** (DOM namespace-aware, com XXE desligado) e extrair `tpAmb`, `Id`, `cLocEmi`, `serie`, `nDPS`, `prest/CNPJ|CPF` por caminho. Isso elimina três conjuntos de regex duplicados (`EmitirDpsService`, `FiscalAuditLogger`, `LegacySoapController`) e o risco de divergência entre eles.

### 3. `LegacySoapController.extrairXmlDps` ainda decodifica entidades incondicionalmente

O item 9 da análise 003 segue aberto:

```java
if (decodificado.contains("&lt;")) { ... .replace("&amp;", "&") }
```

Se o envelope vier com a DPS em CDATA/claro e um `xDescServ` legítimo contendo `&lt;` ou `&amp;`, o `replace` global **corrompe o conteúdo fiscal** (viola a regra 11 do AGENTS.md: imutabilidade). Além disso, `&amp;` → `&` deve ser o **último** replace, e aqui é, mas só após `&lt;`... o que ainda gera dupla decodificação em `&amp;lt;`.

**Ação:** só decodificar se **não** houver `<DPS`/`<infDPS` no payload bruto. Melhor ainda: parsear o envelope SOAP com StAX/DOM, pegar o texto de `xmlEnvio` (o parser já resolve CDATA e entidades corretamente) e só então localizar a `<DPS>`.

Relacionado: `normalizarXmlDps` **reescreve** `versao` e injeta namespace. A reescrita de `1.00`→`1.01` por presença de `IBSCBS` é um ajuste silencioso do conteúdo do cliente. Se for mantida (o `cliente-001.md` justifica), registre isso em log de auditoria quando acontecer.

### 4. Assinatura: `assinar(xml, cert)` aceita DPS com `<Signature>` pré-existente

Se o ERP já enviar uma `<Signature>` dentro de `<DPS>` (o XSD permite `minOccurs="0"`), o assinador anexa uma **segunda**. O Sefin deve rejeitar. Como o `tpAmb`/Id são validados, vale também validar: _"DPS recebida já contém Signature"_ → `ValidacaoFiscalException`, ou remover explicitamente (decisão sua, mas documente).

---

## 🟠 Médio

### 5. Reconciliação: uma falha de reconciliação perde a causa raiz e o registro durável ainda só grava sucesso

- Em `EmitirDpsService`, se a reconciliação lançar/retornar `null`, o erro original é relançado (bom). Mas o **registro durável** (item 8 da análise 003, `PENDENTE` antes de transmitir) não foi implementado: em timeout + reconciliação falha, nada fica em `registro-emissoes.jsonl`. É exatamente o caso em que o ERP mais precisa recuperar.
- `RegistroEmissaoDuravelService.buscarPorCriterio` varre o arquivo inteiro segurando o lock das gravações. Com milhares de notas, `salvar` fica bloqueado durante consultas. Um índice `Map<idDps, registro>` carregado na subida resolve.
- `salvar` engole exceções (`log.error`), correto para não derrubar a emissão, mas então a emissão "bem-sucedida" pode não ter registro. Pelo menos exponha uma métrica/contador de falhas de gravação.

### 6. `FiscalAuditLogger.registrarFalhaEmissao` recebe `e.getMessage()` bruto no REST

`EmissaoDpsController` passa `e.getMessage()` direto ao log de auditoria. Para `SefinTransmissaoException` isso inclui o corpo/mensagem do Sefin; para `ValidacaoFiscalException` pode incluir trechos do XML do cliente (via erros do XSD). Sanitize ou use só o tipo + código.

### 7. Statuses HTTP e SOAP

- `ApiKeyAuthFilter` responde **401** no SOAP, mas o pilar do projeto é "sempre HTTP 200 para o GeneXus". Confirme com o cliente como o GeneXus reage a 401; se for exceção não tratada, retorne 200 com `Sucesso=false`/`CodigoStatus=401`.
- Status 105 (em processamento) sai como `Sucesso=false` no SOAP; o ERP pode interpretar como falha e **reenviar**. A idempotência por Id cobre o reenvio, mas a tela do ERP vai mostrar erro. Considere um `CodigoStatus=105` com `MotivoStatus` explícito ("em processamento, aguarde") e documente o contrato.
- `RestExceptionHandler`: `SefinTransmissaoException` com HTTP 4xx do Sefin vira 400. Um 401/403 do Sefin (certificado rejeitado) é erro de **configuração do servidor**, não do cliente da API. Mapear 401/403 para 502/500 evita o ERP "corrigir" algo que não é dele.

### 8. `CertificadoProvider`

- Lê o arquivo em disco **a cada requisição** (`Files.readAllBytes`) e cria um novo `CertificadoA1`. Isso descarta o cache de KeyStore/chave e, indiretamente, o cache de `HttpClient`/`SSLContext` só funciona porque a chave é o **hash do conteúdo** (recalculado toda vez). Cacheie a instância e invalide por `lastModified` (renovação anual do A1).
- Certificados via header nunca são `close()`d. Com `allowHeaders=true`, use try-with-resources no controller, e limite os caches estáticos (`SSL_CONTEXT_CACHE`, `httpClientCache`) com tamanho máximo, senão viram vazamento (cada certificado distinto fica para sempre).
- `resolverAllowCertHeadersDefault()` e `resolverSoapAddress()` leem `System.getenv` fora do `NfseProperties`, o que contraria o próprio esforço de centralização. `SefinSSLContextFactory` também ignora `nfse.truststore.*` do `NfseProperties` (a propriedade existe e não é usada).

### 9. Hardening pendente

- `ApiKeyAuthFilter`: `providedKey.equals(configuredApiKey)` é comparação não constante; use `MessageDigest.isEqual` sobre bytes.
- Sem `NFSE_API_KEY`, a API roda aberta em produção. No perfil `prod`, falhe a subida ou emita WARN no startup.
- `logging.level.br.com.heviane.nfse=DEBUG` em `application.properties` vale também em produção; ponha `INFO` em `application-prod.properties`. Com DEBUG, `SefinClientAdapter` loga o **corpo bruto** das respostas do Sefin.
- `CorrelationIdFilter` aceita qualquer valor do cliente, e esse valor vai para log, JSONL e **mensagem SOAP**. Valide `[A-Za-z0-9._-]{1,64}` (evita log injection e quebra de XML, mesmo que o SOAP já escape).
- `/health` custom duplica `/actuator/health` e tem versão hardcoded (`0.1.0-SNAPSHOT`).
- `<Signature>` de pedido de evento: o `validarPedRegEvento` valida **antes** de assinar. O XSD exige `ds:Signature` em `TCEvento` mas é `minOccurs="0"` em `TCPedRegEvt`, então está correto; só garanta um teste que valide o XML **já assinado** contra o XSD (o `AssinadorDpsServiceTest` já faz, ótimo).

---

## 🟡 Qualidade de código e testes

- **`AssinadorDpsService.assinar(String, CertificadoA1)`** tem ramificações redundantes (`getElementsByTagNameNS` e `getElementsByTagName` repetidos) e um método morto (`obterElementoInfDps`). Remova código morto.
- **`assinarDocumento` usa `getElementsByTagNameNS(...).item(0)`**: se houver dois `infDPS` (DPS de substituição aninhada, NFS-e com DPS dentro), assina o primeiro. Para o fluxo atual está ok, mas o `LegacySoapController` já lida com `<NFSe><infNFSe><DPS>`; garanta que o que chega ao assinador é sempre a DPS "pura".
- **`dps-exemplo-valido.xml`** usa Id com `cLocEmi=3530607`, série `00001` e `nDPS=1903128101`: coerente. Mas `envelope-soap-genexus.xml`, `payload-emissao-rest.json` e `emissao-sefin.md` usam `DPS3205200100000000000191…` com `nDPS=1` e `serie=00001` → o Id tem `…00001000000000000001`, coerente com a regra de 45. 👍 Apenas confirme que o teste que valida **todos** os samples contra o XSD também passa por `validarCoerenciaIdDps` (hoje só passa pelo XSD).
- **Teste do `LegacySoapControllerTest`** (`deveExtrairDpsAninhadaEmCascaDeNFSe…`) usa `<IBSCBS><totCIBS><vTotNF>100.00</vTotNF></IBSCBS>` com tag **malformada** (`<totCIBS>` não fechada) e nomes que não existem no XSD (`vTotNF` fica dentro de `totCIBS`, mas o XSD define `gIBS`, `gCBS` etc. como obrigatórios). Funciona porque o use case é mockado, mas o sample não representa um payload real; ajuste para um trecho válido da v1.01.
- **`NfseMetrics` em `@WebMvcTest`**: `MockBean` para `NfseProperties` (classe concreta de configuração) funciona, mas é frágil. Prefira `@Import` de uma `NfseProperties` real configurada.
- **`CertificadoTesteHelper`**: tenta `/certs/teste_a1.p12`, mas `.gitignore` bloqueia `*.p12` e `certs/`. No CI cai sempre no fallback `keytool` (que depende de `ProcessBuilder`). Gere o par de chaves em memória com `KeyPairGenerator` + certificado auto-assinado (ex.: via `sun.security.x509` é interno; a alternativa pura é um `.p12` descartável versionado com `!src/test/resources/certs/` + `git add -f`).
- **CI**: o OWASP Dependency-Check roda com `continue-on-error: true`, ou seja, nunca bloqueia. Defina critério (ex.: falhar com CVSS ≥ 9) depois de estabilizar.

---

## 📄 Documentação e repositório

- **Versão do Spring Boot**: `pom.xml` usa **3.3.13**, mas o `README`/pilares falam em "Spring Boot 3.3". A linha 3.3.x já saiu de suporte OSS; planeje a subida para 3.5.x (mantém Java 17 e Tomcat 10.1).
- **`README.md`** promete "substituição de NFS-e e DANFSe" e "lotes", que **não estão implementados** (só emissão, consulta e cancelamento). Ajuste para não prometer o que não existe.
- **`SPEC-002` §4.2** ainda lista `TransmissorSefinPort` coerente com o código (inclui `cancelar`), ok. Mas **`SPEC-003` §4** só descreve `NFSE_CERT_BASE64`/headers; atualize para `NFSE_CERT_PATH`.
- **`SPEC-004`** (consulta/cancelamento SOAP) está aprovada, mas o WSDL e o controller só cobrem `GerarNfse`. Está correto condicioná-la às amostras reais do cliente; só deixe o status como `Aprovado (aguardando amostras)` para não parecer pendência esquecida. O erro SOAP 401 do filtro já cita `GerarNfseResponse` fixo, o que não serviria a `ConsultarNfse`/`CancelarNfse` quando forem ativadas.
- **`docs/dev/pilares-arquitetura…`** referencia `docs/glossary.md`; o real é `docs/glossario-nfse.md`.
- **`cliente-001.md`** §4 usa `POST /ws/nfse` e §8 cita "harmonização de `tpAmb`" (o código rejeita divergência). Corrija os dois.
- **`docs/dev/roteiro-homologacao-sefin-nacional.md`**: o `curl` do Passo 3B usa `SOAPAction: "http://www.abrasf.org.br/nfse.xsd/GerarNfse"` e `scripts/samples/envelope-soap-envio-dps.xml`; o WSDL emulado declara `http://www.siltecnologia.com.br/GerarNfse`. Alinhe. E o `.gitleaks.toml` exclui `docs/` inteiro da varredura, o que enfraquece justamente a regra de "zero vazamento": prefira allowlist por regra/arquivo, como já faz o `.gitleaksignore`.
- **CNPJ `00000000000191`** é o do Banco do Brasil (CNPJ real e público) e está em todos os samples. O próprio AGENTS.md (regra 9) pede dados fictícios. Troque por um fictício com DV válido (ex.: `11222333000181`) e atualize os Ids derivados (o teste de coerência exigirá isso nos samples).
- **`.gitignore`** bloqueia `*.zip`, `*.war`, `*.tar.gz` globalmente: pode atrapalhar fixtures de teste futuras. Use `!src/test/resources/**`.
- **Licença**: `NfseCoreConfiguration.openAPI()` declara `License("Apache 2.0")`, mas o repositório é **MIT**. Corrija.
- **Versão da API no OpenAPI** (`1.0.0`) vs `0.1.0-SNAPSHOT` do `pom`: alinhe via propriedade.

---

## 📋 Ordem sugerida

1. Validar o formato de resposta SOAP contra um retorno real da SIL e criar fixture (item 1).
2. Parsear a DPS uma única vez (DOM) para extrair `tpAmb`/Id/campos, substituindo os regex; corrigir a decodificação de entidades no SOAP (itens 2 e 3).
3. Rejeitar/definir comportamento para `<Signature>` pré-existente (item 4).
4. Registro `PENDENTE` antes da transmissão + índice em memória (item 5).
5. Mapear 401/403 do Sefin como erro de servidor; revisar 401/105 no SOAP com o cliente (item 7).
6. Cache de `CertificadoA1` com invalidação, limites nos caches estáticos, `MessageDigest.isEqual`, perfil `prod` mais rígido (itens 8 e 9).
7. Homologação real ponta a ponta (ainda não há registro de uma emissão real bem-sucedida; é o maior risco residual, especialmente C14N e truststore ICP-Brasil).
8. Limpeza de documentação, CNPJ fictício, licença, versão do Spring Boot.

O ponto mais valioso agora não é mais código: é **uma emissão real em homologação** e **uma amostra real do retorno da SIL**. Os dois resolvem as maiores incertezas que os testes com mock não conseguem cobrir.

Posso detalhar qualquer item (por exemplo, o parser DOM único para extração dos campos da DPS, ou o registro `PENDENTE`). Como pede o `AGENTS.md`, não altero arquivos sem sua aprovação.
