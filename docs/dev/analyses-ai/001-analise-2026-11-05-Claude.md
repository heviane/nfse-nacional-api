# Análise do projeto `nfse-nacional-api` por Claude em 05/11/2026

O projeto está bem acima da média para esse tipo de integração. A arquitetura hexagonal é respeitada, a documentação (ADRs, specs, backlog) é consistente, o `AGENTS.md` é um bom contrato, e a cobertura de testes unitários é boa. Abaixo estão os problemas que encontrei, em ordem de gravidade.

---

## 🔴 Crítico (corrigir antes de produção)

### 1. Divergência entre o XSD e o formato de Id da DPS e da chave

Os schemas definem `TSIdDPS` como `DPS[0-9]{42}` (45 posições), mas o `cliente-001.md` fala em Id de **46 posições**. Já o exemplo `DPS320520020000000000019100001000000000000001` tem só 44 caracteres: **o exemplo usado em quase todos os testes/documentos não passaria no XSD real**. O `dps-exemplo-valido.xml` tem 45 e passa; os demais (`envelope-soap-genexus.xml`, `payload-emissao-rest.json`, `emissao-sefin.md`) estão inválidos.
**Sugestão:** gerar o Id da DPS no middleware (cMun 7 + tipoInsc 1 + CNPJ 14 + série 5 + nDPS 15) em vez de confiar no ERP, ou ao menos validar/avisar. Corrigir os samples e criar um teste que valide **todos** os samples contra o XSD.

### 2. Id do evento de cancelamento incorreto

Em `SefinClientAdapter.montarXmlPedidoCancelamento`, o `infPedReg` usa `Id="EVT%s101101001"`. Pelo XSD (`TSIdPedRefEvt`), o Id do pedido deve ser `PRE` + chave(50) + tipo(6) + nPedRegEvento(3) = `PRE[0-9]{56}`. O prefixo `EVT` é do Id do evento retornado, não do pedido. **O Sefin rejeitará o cancelamento.**
Além disso, o XML **não é assinado** (`assinadorPort.gerarPayloadGZipB64(xmlEvento)` só compacta). O pedido de registro de evento exige assinatura do autor. Faltam também: o assinador só sabe assinar `infDPS`, e o pedido precisa de assinatura sobre `infPedReg`.
**Sugestão:** generalizar o `AssinadorDpsPort` (ex.: `assinar(xml, nomeElementoAlvo, certificado)`), assinar `infPedReg` e usar o Id correto. Além disso, `xDesc`, `cMotivo` e `xMotivo` devem ser validados contra o XSD antes do envio, como é feito com a DPS.

### 3. Dados sensíveis em logs do SOAP e erro vazando para o ERP

`LegacySoapController` captura **qualquer** `Exception` e devolve `e.getMessage()` ao ERP, além de logar com stack trace completo (`log.error(..., e)`). Mensagens de `CertificadoA1`/PKCS#12 podem expor detalhes internos. Em `CertificadoProvider`, a mensagem de erro de decodificação também é repassada. Isso conflita com a regra 7 do `AGENTS.md`.
**Sugestão:** mapear exceções conhecidas para mensagens controladas; mensagens genéricas para o resto, com um correlation id para rastrear no log.

### 4. Endpoint legado e certificado via headers

O endpoint SOAP e a API REST estão **abertos, sem autenticação**. Qualquer um que descubra a URL pública da BrasilCloud pode emitir notas com o certificado do cliente (configurado em env). Isso é risco fiscal real.
**Sugestão mínima:** autenticação por API key/Basic no `/prefeitura/**` e `/api/**` (Spring Security), restrição por IP/firewall, e HTTPS obrigatório. O GeneXus consegue enviar Basic Auth/headers customizados no consumo de WebService. Também reavalie a rota de `X-Certificado-Base64`/`Senha` via header: mesmo com TLS, ela aparece em logs de proxy, e o certificado vai e volta pela rede a cada requisição. Para o Cliente 001 (single-tenant) eu desabilitaria por padrão via propriedade.

---

## 🟠 Alto

### 5. Inconsistências entre código e documentação do `setenv.sh`

O `cliente-001.md` documenta `NFSE_CERT_PATH` (caminho do `.pfx`), mas o código só lê `NFSE_CERT_BASE64`/`NFSE_CERT_PASSWORD`. Quem seguir a doc do cliente não conseguirá subir. Escolha uma abordagem e alinhe. Para o Tomcat, ler de arquivo com permissão 600 costuma ser mais prático do que Base64 em variável (que aparece em `/proc/<pid>/environ` e em dumps de configuração).

### 6. `context-path` quebra o contrato do ERP

`application.properties` define `server.servlet.context-path=/nfse-nacional-api`. No WAR no Tomcat o contexto vem do nome do arquivo, ok, mas no modo standalone a URL SOAP vira `/nfse-nacional-api/prefeitura/notafiscal/nfse.asmx`, enquanto o manual de testes e o `cliente-001-servico-legado` citam `localhost:8080/prefeitura/...` sem o contexto. Além disso o WSDL emulado devolve `soap:address location="http://localhost:8080/..."` **fixo**. Alguns clientes SOAP do GeneXus usam o `location` do WSDL. Gere-o dinamicamente a partir da requisição (`ServletUriComponentsBuilder`) ou de uma propriedade.

### 7. O ACL só cobre `GerarNfse`

Se o ERP legado também usa consulta e cancelamento SIL (ex.: `ConsultarNfse`, `CancelarNfse`), eles não existem no WSDL emulado. Antes de produção, confirme com o cliente quais operações o GeneXus realmente chama (a pendência "obter amostra do XML SOAP real" no `cliente-001.md` ainda está aberta). Sem cancelamento no ACL, o cliente terá que sair do ERP para cancelar.

### 8. Mapeamento de campos legado → nacional

O ACL apenas extrai a `<DPS>` do envelope, partindo da premissa de que o GeneXus **já envia uma DPS nacional**. Isso contradiz a motivação do documento (o ERP enviava o formato SIL, com `<NFSe><infNFSe>` e Id de 53 posições). Se o payload real ainda for o antigo, tudo falha no XSD. Vale validar essa premissa com um payload real **antes** de seguir; é o maior risco de negócio do projeto. Também faltam: geração de `nDPS` sequencial/idempotência, `dhEmi` dentro da janela aceita, `tpAmb` coerente com o ambiente configurado, e `cLocEmi` (hoje o ERP controla tudo).

### 9. `tpAmb` do XML vs. ambiente da requisição

`EmitirDpsService` não verifica se `<tpAmb>` do XML bate com o `AmbienteSefin` usado. Um XML com `tpAmb=2` enviado à produção (ou o inverso) gera rejeição confusa ou, pior, emissão no ambiente errado. Valide e/ou sobrescreva.

### 10. Sem idempotência nem tratamento de timeout

A emissão é um POST não idempotente: se o timeout de 30s ocorrer **depois** do Sefin autorizar, o ERP recebe erro e pode reenviar, gerando duplicidade (status 204 do Sefin ajuda, mas o ERP não ficará com a chave). Em caso de timeout/erro de rede, o fluxo deveria consultar por Id da DPS (o Sefin tem `GET /dps/{id}`) antes de dar erro final. A SPEC-002 prevê RF-06, mas o código só envelopa a exceção.

---

## 🟡 Médio

### 11. Duplicação e pequenos desvios de arquitetura

- Os `resolverAmbiente()` estão duplicados em `EmissaoDpsController` e `LegacySoapController`. Mova para uma `NfseProperties` (`@ConfigurationProperties`) ou para o `CertificadoProvider`/serviço de configuração.
- `RespostaEmissaoSefin` & cia no `core.domain` têm `@JsonIgnoreProperties`, violando a regra do próprio `estrutura-de-pastas.md` ("core nunca importa Jackson"). Crie DTOs no adaptador `sefin` e mapeie.
- `RespostaConsultaNfse` e outras repetem o método de descompactar GZIP em três records. Extraia um utilitário.
- `CertificadoProvider` fica em `core.services` mas lê `System.getenv` e `System.getProperty`; isso é configuração de infraestrutura e pertence a um adaptador/config.

### 12. Performance e segurança criptográfica

- `SefinClientAdapter` recria `SSLContext` e `HttpClient` **a cada chamada**, e `CertificadoA1.getPrivateKey()`/`getX509Certificate()` recarregam o KeyStore PKCS#12 a cada uso (o assinador chama várias vezes). Faça cache do `KeyStore`/`SSLContext`/`HttpClient` por certificado (hash do conteúdo) e carregue as chaves uma vez. O RNF da SPEC-001 pede <150ms.
- `TrustManagerFactory.init((KeyStore) null)` usa o truststore padrão da JVM. Verifique se a cadeia ICP-Brasil/gov.br do servidor Sefin é confiada pelo JDK 17 da BrasilCloud; é uma falha comum em produção ("PKIX path building failed"). Considere um truststore dedicado.
- `SSLContext.getInstance("TLSv1.2")` fixa o protocolo; prefira `"TLS"` com `jdk.tls.client.protocols` restrito a TLSv1.2/1.3.
- `CertificadoA1` mantém a senha como `char[]` sem limpeza e sem `close()`/zeroing.
- `validarAssinatura` aceita a chave pública do próprio XML (válido só para integridade, não confiança). OK para testes, mas deixe claro no Javadoc que não valida a cadeia nem a revogação.

### 13. Mais pontos de código

- `ValidadorSchemaService.validarDps` usa sempre v1.00; a v1.01 (IBS/CBS) já está no repo e `versao` existe no port. Decida a estratégia de versão (detectar pelo atributo `versao` da `<DPS>` ou configurável), pois o Sefin passa a exigir v1.01 conforme o calendário da reforma.
- `validator.validate` com `SchemaFactory` com `FEATURE_SECURE_PROCESSING`: ok, mas desabilite explicitamente `ACCESS_EXTERNAL_DTD`/`ACCESS_EXTERNAL_SCHEMA` no `Validator`. Também há risco de XXE em `extrairXmlDps` se algum dia for parseado (hoje é string; ok).
- `extrairXmlDps` usa `indexOf("<DPS")`, que casa com `<DPSxyz` e também pode capturar `<DPS` dentro do CDATA. Use um parser (StAX) ou regex com limite de palavra.
- `montarRespostaSoapSucesso` escapa o XML da NFS-e uma vez, mas `<MotivoStatus>` e a chave estão no resultado já dentro de string escapada; confirme com o parser do GeneXus que o duplo escape (`&amp;lt;`) é o esperado. O `.stripIndent()` num `String.format` com text block é redundante.
- `montarXmlPedidoCancelamento` usa `String.format` com texto do usuário **sem escapar XML** (`req.motivo()`): um motivo contendo `<` ou `&` produz XML inválido (e é um vetor de injeção de XML). Escape ou monte via DOM.
- `ErroResponse`: o `RestExceptionHandler` mapeia qualquer 4xx do Sefin para 400 e o resto para 502; considere 504 para timeout e 409 para duplicidade.
- `EmissaoDpsControllerTest` tem um teste 422/502 que não passa certificado e depende de `certificadoProvider` mockado com `any()`: ok, mas o teste 400 não mocka o certificado e passa só porque a validação vem primeiro. Frágil.
- Faltam testes de: `CancelarNfseService`/`ConsultarNfseService`, `CertificadoProvider`, `RestExceptionHandler`, `HealthController` com contexto sem certificado e, sobretudo, um teste de contrato validando `pedRegEvento` gerado contra `pedRegEvento_v1.00.xsd`. Esse teste teria pegado o erro do item 2.
- `CertificadoTesteHelper` chama `keytool` via `ProcessBuilder`: quebra se `keytool` não estiver no PATH (comum em CI/IDE). Prefira gerar o par de chaves via `KeyPairGenerator` + certificado auto-assinado (ou usar um `.p12` de teste checado, que é seguro por ser descartável).

---

## 🟢 Melhorias de processo e repositório

1. **Dependências do `pom.xml`:** o Spring Boot 3.3.5 está desatualizado; confira patches de segurança e atualize. Há dois pontos de atenção: `spring-boot-starter-tomcat` como `provided` precisa que o Tomcat da BrasilCloud seja **≥10.1** (Servlet 6.0) para o Spring Boot 3.3; confirme a versão exata com o cliente. `jaxb2-maven-plugin` citado na ADR-002 não está no pom (o projeto optou por validação sem JAXB): atualize a ADR para refletir a realidade, ou registre uma ADR de revisão.
2. **Inconsistências de docs:** o README e o backlog marcam o MVP como `[ ]` e `v0.1.0-alpha`, enquanto os épicos 0-5 estão 100%. A SPEC-002 mostra a interface sem `cancelar`. `ADR-004` cita `adapters.out.crypto` e o código usa `signer`. `docs/adrs` referencia glossário e specs corretamente, mas `README` aponta `docs/adr/` (singular) no template. O `SPEC` 0003 menciona `EnviarLoteRpsEnvio`, que o código não trata. Corrija ou crie uma SPEC-004 para o ACL de consulta/cancelamento.
3. **`AGENTS.md`/LGPD:** os samples usam CNPJ `00000000000191` (é o CNPJ do Banco do Brasil, real e público). Prefira um CNPJ fictício com DV válido que não seja de uma instituição real. O `.gitignore` bloqueia `*.zip`, `*.tar.gz` e `*.war` globalmente, o que pode atrapalhar anexar fixtures de teste futuras; use `!src/test/resources/**`.
4. **`bug_report.md`** menciona "Microserviço" e ".NET": é resto de template. Ajuste.
5. **CI:** nenhum workflow no repositório. Mesmo antes do CD, um GitHub Actions rodando `./mvnw verify` em cada PR (JDK 17) vale muito e é barato. Inclua `dependency-check` (OWASP) e `gitleaks` para reforçar a regra de não vazar certificados.
6. **Observabilidade:** adicione um `correlationId` (MDC) por requisição, métricas simples (Actuator + Micrometer) para emissões/rejeições e um log de auditoria estruturado (chave, status, CNPJ mascarado, sem payload). Em fiscal, isso economiza horas de suporte. Em produção, limite `management.endpoint.health.show-details=always`, que hoje expõe detalhes a qualquer um.
7. **Persistência mínima do que foi emitido:** o middleware é stateless, então se o ERP perder a resposta, não há como recuperar a chave sem consultar o Sefin por Id da DPS (item 10). Pelo menos um log de auditoria durável (chave + Id da DPS + horário) ajuda muito.
8. **Swagger em produção:** a documentação interativa fica pública no deploy do cliente; desabilite com `springdoc.api-docs.enabled=false` em produção, ou proteja.
9. **Deploy:** o procedimento manual do `cliente-001.md` omite backup do WAR anterior e plano de rollback. Acrescente. E o `run-local.ps1.example` roda `java -jar target/nfse-nacional-api.war` mas o `.gitignore` bloqueia `run-local.ps1` (correto).
10. **Roteiro de homologação antes de produção:** ainda não há registro de uma emissão real bem-sucedida em produção restrita. Os testes usam mocks do Sefin; a assinatura é validada só contra si mesma (`validarAssinatura`). Priorize um teste real em homologação com certificado de teste, validando o C14N e o `Reference` (ex.: o transform `enveloped` seguido de `exc-c14n` e `DigestValue`), pois pequenos desvios de canonicalização só aparecem no servidor da Receita.

---

## ✅ Pontos fortes (manter)

- Separação clara core/adapters e injeção manual no `NfseCoreConfiguration`.
- Certificados só em memória e `.gitignore` bem blindado.
- Records com validação no construtor (`RequisicaoCancelamentoDps`, `RequisicaoEnvioDps`).
- Testes usando servidor HTTP local para o adaptador Sefin, sem dependências extras.
- Documentação viva (ADRs, specs, glossário, manual de testes) rara em projetos desse porte.
- Tratamento do erro SOAP sem `SoapFault`, bem pensado para o GeneXus.

---

## 📋 Ordem sugerida de ação

1. Validar o payload SOAP **real** do GeneXus (item 8), pois pode mudar o desenho do ACL.
2. Corrigir Id/assinatura do cancelamento + teste XSD do `pedRegEvento` (item 2).
3. Corrigir Id da DPS e os samples; teste de todos os samples contra o XSD (item 1).
4. Autenticação nos endpoints e saneamento de mensagens de erro (itens 3 e 4).
5. Homologação real ponta a ponta (item 10 de processo), cobrindo truststore.
6. Idempotência/consulta por Id da DPS em caso de timeout (item 10).
7. Cache de `SSLContext`/KeyStore, CI e limpeza de documentação.

Posso detalhar qualquer item (por exemplo, o código corrigido para o evento de cancelamento assinado, ou um SPEC-004 para o ACL de consulta/cancelamento) se quiser. Todas as alterações de arquivo, como pede o `AGENTS.md`, só faço mediante sua aprovação.
