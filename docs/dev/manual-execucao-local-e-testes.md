# Manual de Execução Local & Testes de Ponta a Ponta

> **Status:** `Ativo / Documentação Operacional`
> **Público:** Desenvolvedor(a) & Engenharia de Integração
> **Data:** 2026-11-02
> **Objetivo:** Orientar a execução local da API em modo standalone e via container, permitindo a realização de testes 100% reais de emissão de DPS contra o **Sefin Nacional (Ambiente de Homologação / Produção Restrita)** e homologação com o **ERP GeneXus 18** antes do deploy em nuvem.

---

## 1. ❓ A Execução Local Permite Testes Reais com o Sefin Nacional?

> [!IMPORTANT]
> **SIM, 100% REAL!**
> O Sefin Nacional (SERPRO / Receita Federal) **não exige IP fixo, nem VPN, nem pré-cadastro de firewall**.
> O único requisito de segurança do ambiente nacional para aceitar a conexão é a autenticação mTLS (**Mutual TLS com Certificado Digital A1 emitido sob a ICP-Brasil**).

Ao executar a API localmente com o arquivo `.pfx` real do cliente:

1. A API negocia a conexão TLS 1.2+ diretamente com `https://sefin.producaorestrita.nfse.gov.br`.
2. A DPS é validada pelo XSD oficial, assinada digitalmente com a chave privada em memória e transmitida.
3. O Sefin Nacional processa a declaração em seus servidores fiscais de homologação e retorna a **Chave de Acesso oficial de 50 dígitos** e a NFS-e autorizada.
4. Para o ERP GeneXus 18, a API pode ser exposta via rede local ou via **Túnel HTTPS Seguro (Cloudflare Tunnel ou ngrok)**, permitindo que o GeneXus emita notas reais sem perceber que a API está rodando na sua máquina.

---

## 2. 📋 Pré-Requisitos

1. **Java Development Kit (JDK 17)**: OpenJDK 17 instalado e configurado no `PATH` (`java -version`).
2. **Artefato WAR Compilado**: Arquivo `target/nfse-nacional-api.war` gerado pelo comando:

   ```bash
   ./mvnw clean package -DskipTests
   ```

3. **Certificado Digital A1 do Cliente**: Arquivo `.pfx` ou `.p12` e a respectiva senha.

---

## 3. 🔐 Conversão do Certificado A1 para Base64

A API foi projetada para receber o certificado em memória em formato Base64, evitando salvar senhas ou arquivos desprotegidos em disco.

### No Windows (PowerShell)

```powershell
# Converte o arquivo .pfx para Base64 sem quebras de linha
$pfxPath = "C:\caminho\do\certificado.pfx"
$certBase64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($pfxPath))

# Copia para a área de transferência para facilitar uso
$certBase64 | Set-Clipboard
Write-Host "Certificado convertido e copiado para a área de transferência!"
```

### No Linux / macOS (Bash)

```bash
base64 -w 0 /caminho/do/certificado.pfx | pbcopy # ou xclip / wl-copy
```

---

## 4. 🚀 Inicialização da API

Existem duas formas recomendadas de executar a aplicação localmente:

### Modo A: Executável Standalone Spring Boot (Mais Rápido) ⚡

Como o arquivo gerado é um WAR empacotado pelo Spring Boot, ele possui um servidor Apache Tomcat 10 embutido:

```powershell
# 1. Definir as variáveis de ambiente na sessão do terminal
$env:NFSE_CERT_BASE64 = "<CONTEUDO_BASE64_GERADO_NO_PASSO_ANTERIOR>"
$env:NFSE_CERT_PASSWORD = "<SENHA_DO_CERTIFICADO>"
$env:NFSE_ENVIRONMENT = "HOMOLOGACAO"
$env:SERVER_PORT = "8080"

# 2. Iniciar a aplicação
java -jar target/nfse-nacional-api.war
```

### Modo B: Container Docker com Apache Tomcat 10 (Espelho 1:1 BrasilCloud) 🐳

Se você deseja testar exatamente como o Apache Tomcat 10 descompacta e roda o arquivo WAR:

```bash
docker run -d --name nfse-local \
  -p 8080:8080 \
  -e NFSE_CERT_BASE64="<CONTEUDO_BASE64>" \
  -e NFSE_CERT_PASSWORD="<SENHA>" \
  -e NFSE_ENVIRONMENT="HOMOLOGACAO" \
  -v $(pwd)/target/nfse-nacional-api.war:/usr/local/tomcat/webapps/ROOT.war \
  tomcat:10-jdk17
```

---

## 5. 🔍 Verificação de Inicialização & Documentação Interativa

Com a aplicação rodando, acesse no navegador:

1. **Smoke Test / Health Check:**
   - URL: `http://localhost:8080/health`
   - Retorno esperado (HTTP 200):

     ```json
     {
       "status": "UP",
       "service": "nfse-nacional-api",
       "version": "0.1.0-SNAPSHOT"
     }
     ```

2. **Swagger UI (Documentação Interativa OpenAPI 3):**
   - URL: `http://localhost:8080/swagger-ui/index.html`
   - Permite testar interativamente todos os endpoints REST:
     - `POST /api/v1/dps/emitir`
     - `GET /api/v1/nfse/{chaveAcesso}`
     - `POST /api/v1/nfse/cancelar`

3. **Definição WSDL da Camada Anticorrupção GeneXus:**
   - URL: `http://localhost:8080/prefeitura/notafiscal/nfse.asmx?WSDL`
   - Retorna o XML WSDL emulando o WebService municipal da SIL Tecnologia.

---

## 6. 🌐 Exposição para o ERP GeneXus 18 via Túnel HTTPS Seguro

Se o ERP GeneXus 18 estiver rodando em outra máquina, em rede local ou na infraestrutura do cliente, você pode expor sua porta `8080` local através de um túnel com certificado SSL válido:

### Opção 1: Cloudflare Tunnel (Sem necessidade de cadastro / Gratuito)

```powershell
# Executar túnel temporário
cloudflared tunnel --url http://localhost:8080
```

O console exibirá uma URL como:
`https://abc-123.trycloudflare.com`

### Opção 2: ngrok

```powershell
ngrok http 8080
```

O console exibirá uma URL como:
`https://meu-tunnel.ngrok-free.app`

### Configuração no GeneXus 18

No cadastro de parâmetros da Prefeitura no GeneXus do Cliente 001, configure a URL do WebService para:

```text
https://<url-do-tunnel>/prefeitura/notafiscal/nfse.asmx
```

---

## 7. 🧪 Cenário 1: Teste Direto (Como Novo Cliente / API REST Moderna)

Este cenário valida o comportamento da API para **novos clientes ou sistemas modernos** que se integram via REST/JSON com DTOs limpos, sem precisar conhecer SOAP ou detalhes de criptografia XML.

> [!TIP]
> Um arquivo pronto com este payload está disponível em:
> [`src/test/resources/samples/payload-emissao-rest.json`](../../src/test/resources/samples/payload-emissao-rest.json)

> [!IMPORTANT]
> **Atenção para o Teste Real com o Certificado do Cliente:**  
> O Sefin Nacional valida se o CNPJ informado em `<prest><CNPJ>` e no atributo `Id` de `<infDPS Id="DPS...">` é **exatamente o mesmo CNPJ titular do Certificado Digital A1 (`.pfx`)**.  
> Por isso, para o seu teste local real, substitua o CNPJ genérico `00000000000191` e a razão social pelos dados reais do cliente diretamente no Swagger UI ou no corpo da requisição.  
> Como essas variáveis e o payload rodam na memória da sua máquina, **nenhum dado sensível do cliente precisa (ou deve) ser comitado no Git**.

### Opção 1.1: Via Swagger UI (Interativo no Navegador — Sem ferramentas externas)

1. Com a API rodando, acesse no navegador:
   `http://localhost:8080/swagger-ui/index.html`
2. Localize a seção **`Emissão DPS`** e clique em **`POST /api/v1/dps/emitir`**.
3. Clique no botão **`Try it out`**.
4. _(Opcional)_ Se não tiver configurado as variáveis de ambiente no servidor, preencha os headers:
   - `X-Certificado-Base64`: String Base64 do `.pfx`
   - `X-Certificado-Senha`: Senha do certificado
5. No corpo da requisição (`Request body`), preencha o JSON com a DPS de homologação:

   ```json
   {
     "ambiente": "HOMOLOGACAO",
     "xmlDps": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\"><infDPS Id=\"DPS320520020000000000019100001000000000000001\"><tpAmb>2</tpAmb><dhEmi>2026-11-02T17:00:00-03:00</dhEmi><verAplic>ERP_v1.00</verAplic><serie>00001</serie><nDPS>1</nDPS><dCompet>2026-11-02</dCompet><tpEmit>1</tpEmit><cLocEmi>3205200</cLocEmi><prest><CNPJ>00000000000191</CNPJ><IM>12345</IM><xNome>EMPRESA PRESTADORA DE SERVICOS LTDA</xNome><fone>27999998888</fone><email>contato@prestadordeexemplo.com.br</email><regTrib><opSimpNac>1</opSimpNac><regApTribSN>1</regApTribSN><regEspTrib>0</regEspTrib></regTrib></prest><toma><CNPJ>99999999000191</CNPJ><xNome>TOMADOR DE SERVICOS TESTE LTDA</xNome><end><endNac><cMun>3205309</cMun><CEP>29010000</CEP></endNac><xLgr>AVENIDA EXEMPLO</xLgr><nro>100</nro><xBairro>CENTRO</xBairro></end></toma><serv><locPrest><cLocPrestacao>3205200</cLocPrestacao></locPrest><cServ><cTribNac>010301</cTribNac><cTribMun>700</cTribMun><xDescServ>Servicos de desenvolvimento e licencamento de programas de computador.</xDescServ></cServ></serv><valores><vServPrest><vServ>100.00</vServ></vServPrest><trib><tribMun><tribISSQN>1</tribISSQN><tpRetISSQN>2</tpRetISSQN><pAliq>2.50</pAliq></tribMun><tribFed><piscofins><CST>07</CST><vBCPisCofins>0.00</vBCPisCofins><pAliqPis>0.00</pAliqPis><pAliqCofins>0.00</pAliqCofins><vPis>0.00</vPis><vCofins>0.00</vCofins><tpRetPisCofins>2</tpRetPisCofins></piscofins></tribFed><totTrib><indTotTrib>0</indTotTrib></totTrib></trib></valores></infDPS></DPS>"
   }
   ```

6. Clique no botão azul **`Execute`**.
7. **Resultado esperado (HTTP 200):**

   ```json
   {
     "sucesso": true,
     "codigoStatus": 100,
     "motivoStatus": "NFS-e gerada com sucesso",
     "chaveAcesso": "32052002601000000000019100001000000000000001123456",
     "xmlNfse": "<NFSe versao=\"1.00\">...</NFSe>"
   }
   ```

### Opção 1.2: Via Terminal (`curl` / Postman / Insomnia)

```bash
curl -X POST http://localhost:8080/api/v1/dps/emitir \
  -H "Content-Type: application/json" \
  -d '{
    "ambiente": "HOMOLOGACAO",
    "xmlDps": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DPS xmlns=\"http://www.sped.fazenda.gov.br/nfse\" versao=\"1.00\">...</DPS>"
  }'
```

---

## 8. 🔄 Cenário 2: Teste via ERP Legado (Cliente 001 GeneXus 18 / SOAP ACL)

Este cenário valida a **Camada Anticorrupção (ACL)**: o ERP GeneXus 18 continua disparando as requisições SOAP no padrão municipal antigo da SIL Tecnologia, e a nossa API atua como um emulador transparente, convertendo tudo para o padrão nacional do Sefin.

> [!TIP]
> Uma amostra do envelope SOAP real enviado pelo GeneXus 18 está salva em:
> [`src/test/resources/samples/envelope-soap-genexus.xml`](../../src/test/resources/samples/envelope-soap-genexus.xml)

> [!NOTE]
> No ERP GeneXus, a emissão utilizará naturalmente os dados reais cadastrados no banco de dados local do cliente (que coincidem com o certificado `.pfx`). A Camada Anticorrupção receberá esses dados em memória, fará a ponte com a Receita Federal e retornará a autorização sem persistir nenhuma informação sensível no repositório Git.

### Passo 2.1: Teste de Reconhecimento do WSDL

Antes de emitir, garanta que o GeneXus ou o navegador consiga ler o WSDL emulado:

- Abra no navegador: `http://localhost:8080/prefeitura/notafiscal/nfse.asmx?WSDL`
- Confirme se o XML retornado contém as tags `<wsdl:definitions>`, os métodos `<s:element name="GerarNfse">` e a namespace `http://www.siltecnologia.com.br/`.

### Passo 2.2: Configuração de Apontamento no GeneXus 18

No ERP GeneXus do Cliente 001, acesse as configurações de parâmetros municipais de emissão da NFS-e:

- **Se o GeneXus estiver na mesma máquina de desenvolvimento:**
  Altere a URL do WebService da prefeitura para:

  ```text
  http://localhost:8080/prefeitura/notafiscal/nfse.asmx
  ```

- **Se o GeneXus estiver em outra máquina ou na rede do cliente:**
  Abra o túnel seguro na sua máquina (conforme Seção 6):

  ```powershell
  cloudflared tunnel --url http://localhost:8080
  # ou
  ngrok http 8080
  ```

  E configure no GeneXus a URL HTTPS gerada:

  ```text
  https://<sua-url-do-tunnel>/prefeitura/notafiscal/nfse.asmx
  ```

### Passo 2.3: Disparar a Emissão no GeneXus 18

1. No ERP GeneXus, gere uma nota de serviço de teste normalmente e clique na ação de transmissão/emissão.
2. O GeneXus empacotará o XML no envelope SOAP tradicional e enviará via HTTP POST para a rota `/prefeitura/notafiscal/nfse.asmx`.

### Passo 2.4: O que você verá no console da API em tempo real

No terminal onde a API está rodando, você verá o ciclo completo sendo executado:

```text
INFO  [LegacySoapController] Recebida requisição SOAP de emissão legada via GeneXus 18
INFO  [LegacySoapController] Tag <DPS> extraída com sucesso do envelope SOAP
INFO  [EmitirDpsService] Validando schema XSD da DPS contra schema oficial v1.00...
INFO  [AssinadorDpsService] Assinando tag infDPS via RSA-SHA256 em memória...
INFO  [SefinClientAdapter] Estabelecendo handshake mTLS com https://sefin.producaorestrita.nfse.gov.br...
INFO  [SefinClientAdapter] Transmissão concluída com sucesso! HTTP 200 - Status Sefin 100
INFO  [LegacySoapController] Retorno nacional empacotado em envelope SOAP legado para o GeneXus
```

### Passo 2.5: O que o GeneXus 18 receberá

O GeneXus receberá um HTTP 200 contendo o envelope SOAP idêntico ao que a prefeitura retornava:

```xml
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <GerarNfseResponse xmlns="http://www.siltecnologia.com.br/">
      <GerarNfseResult>
        <RetornoEmissao>
          <Sucesso>true</Sucesso>
          <CodigoStatus>100</CodigoStatus>
          <MotivoStatus>NFS-e gerada com sucesso</MotivoStatus>
          <ChaveAcesso>32052002601000000000019100001000000000000001123456</ChaveAcesso>
          <XmlNfse>&lt;NFSe...&gt;</XmlNfse>
        </RetornoEmissao>
      </GerarNfseResult>
    </GerarNfseResponse>
  </soap:Body>
</soap:Envelope>
```

O GeneXus processará o retorno, gravará a chave de acesso e marcará a nota como emitida no banco de dados do ERP.

---

## 9. 🛠️ Diagnóstico & Resolução de Problemas (Troubleshooting)

| Sintoma                                                     | Causa Mais Provável                                         | Como Resolver                                                                                                |
| :---------------------------------------------------------- | :---------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------- |
| `Falha ao carregar certificado PKCS#12: password incorrect` | Senha incorreta informada na variável `NFSE_CERT_PASSWORD`. | Conferir a senha digitada ou verificar se há espaços em branco acidentais.                                   |
| `Falha ao carregar certificado: pad block corrupted`        | Arquivo Base64 corrompido ou truncado.                      | Gerar novamente o Base64 garantindo que o comando não quebrou linhas (`-w 0` no Linux ou script PowerShell). |
| `SefinTransmissaoException: HTTP 403 / 401`                 | Certificado rejeitado pela Receita Federal.                 | Verificar se o certificado é um e-CNPJ / e-CPF válido da ICP-Brasil e se não está revogado ou expirado.      |
| `ValidacaoFiscalException: cvc-complex-type...`             | Campos obrigatórios ausentes no XML da DPS.                 | Verificar se o XML possui todos os nós exigidos para o Simples Nacional (`opSimpNac`, `regApTribSN`, etc.).  |
