# Cliente 001 — Ambiente Municipal Legado (SIL) vs Ambiente Nacional (Migração)

Documentação da análise técnica e identificação das mudanças entre o ambiente municipal legado (SIL Tecnologia) e o ambiente nacional (Sefin Nacional / Receita Federal — agora obrigatório pela Resolução CGSN nº 191/2026).

---

## 🎯 Perfil do Cliente

* **Quem é o cliente?** Sócio de uma empresa que não é de TI, mas ele próprio é **desenvolvedor GeneXus**.
* **Tom da comunicação:** Não precisamos ter receio de usar termos técnicos (SOAP, REST, WSDL, XML, JSON, mTLS, Certificado A1, endpoints), porém devemos focar na **solução prática** e no **valor gerado**: poupar esforço de programação no ERP dele e garantir zero impacto no faturamento da empresa.

---

## 🔍 Análise Técnica Comparativa

### 1. Diferenças Críticas de Ambiente & Protocolo

| Aspecto | Ambiente Municipal Legado (SIL Tecnologia) | Ambiente Nacional (Sefin Nacional / ADN) | O que o nosso Middleware resolveu |
| :--- | :--- | :--- | :--- |
| **Protocolo de Comunicação** | WebService SOAP 1.1 / XML com WSDL (`.asmx`) | **API REST / JSON** síncrona com verbos HTTP | Emula o SOAP para o GeneXus e traduz para REST para a Receita Federal. |
| **Segurança de Transporte** | HTTPS / TLS padrão de servidor | **mTLS (Mutual TLS)** obrigatório via Certificado Digital A1 no Handshake | Gerencia a autenticação mútua via `SSLContext` em memória sem exigir suporte a mTLS nativo no GeneXus. |
| **Estrutura do Envio** | Envelope complexo gerando a casca `<NFSe>` | Transmissão estrita da declaração **`<DPS>`** assinada | O middleware descasca o XML do GeneXus e transmite apenas a DPS. |
| **Identificação da Nota** | Chave municipal de 53 caracteres (`NFS...`) | **Chave de Acesso Nacional de 50 dígitos numéricos** | Captura a chave nacional da Receita e devolve no mesmo campo do XML SOAP legado. |
| **Impacto no ERP GeneXus** | Exigiria reescrever do zero o módulo de NFS-e em REST | **Zero alterações de código:** apenas trocar a URL no cadastro | Economia estimada de 40 a 80 horas de desenvolvimento e testes no ERP. |

---

### 2. Diferenças Críticas de Assinatura Digital & Criptografia

| Modificações | `assinador-xml-java` (Legado SIL) | Sefin Nacional (NFS-e Nacional / ADN) |
| :--- | :--- | :--- |
| **Algoritmo de Assinatura** | `RSA-SHA1` | **`RSA-SHA256`** obrigatório (`http://www.w3.org/2001/04/xmldsig-more#rsa-sha256`). O Sefin rejeita SHA-1 imediatamente. |
| **Digest Method (Hash)** | `SHA-1` (`DigestMethod.SHA1`) | **`SHA-256`** obrigatório (`DigestMethod.SHA256`). |
| **Transformações** | Apenas `ENVELOPED` | **`ENVELOPED` + `CanonicalizationMethod.EXCLUSIVE`**. |
| **Posição da `<Signature>`** | Inserida dentro do nó assinado (`signatureParent`) | Inserida como filha direta da raiz `<DPS>`, referenciando o ID de `<infDPS>` (`URI="#DPS..."`). |
| **Carregamento do Certificado** | Arquivo físico lido do disco (`FileInputStream`) | **100% em memória** a partir de `byte[]` / Base64 (`KeyStore`), sem salvar arquivos em disco por segurança e LGPD. |
| **Serialização do Payload** | Replaces específicos de strings da SIL (`&#13;`) | Padrão W3C canônico + compactação **`GZIP + Base64`** exigida no corpo da API HTTP do Sefin. |

---

## 💼 Retorno para o Cliente

### 💰 Estratégia de Precificação & Orçamento

#### 1. Ancoragem de Valor (Custo de Oportunidade)
Se o cliente optasse por reescrever o módulo de emissão diretamente dentro do GeneXus 18:
* **Complexidade no GeneXus:** Implementar mTLS nativo, assinatura digital W3C RSA-SHA256 e serialização GZIP Base64 no GeneXus é notoriamente custoso e limitado por dependências externas do gerador.
* **Esforço Estimado:** Pelo menos **40 a 80 horas de desenvolvimento**, testes de homologação e validação contábil.
* **Custo de Mercado:** Um consultor/desenvolvedor GeneXus sênior cobraria facilmente entre **R$ 6.000,00 e R$ 12.000,00**, com alto risco de paralisação da emissão durante o período de desenvolvimento.

#### 2. Composição da Proposta Recomendada
A proposta pode ser apresentada dividida em escopos claros:

| Item / Módulo | Descrição do Entregável | Benefício para o Cliente |
| :--- | :--- | :--- |
| **1. Middleware de Integração & Camada Anticorrupção (ACL)** | Código da API Java 17 com core fiscal (validação XSD, assinatura RSA-SHA256, mTLS com a Receita e emulador SOAP SIL para GeneXus). | **Zero alteração de código no ERP.** Mantém a operação funcionando sem retrabalho. |
| **2. Homologação & Implantação no Servidor (BrasilCloud)** | Configuração do Apache Tomcat 10, injeção segura de variáveis de ambiente com o `.pfx` e testes com o GeneXus. | Deploy pronto para faturamento real em ambiente isolado e seguro de posse do cliente. |
| **3. Acompanhamento & Garantia (30 dias)** | Suporte técnico direto nas primeiras emissões em produção e resolução de eventuais inconsistências da Receita. | Segurança e tranquilidade jurídica/fiscal para o faturamento da empresa. |

---

### 📱 Mensagem para Envio via WhatsApp

A mensagem abaixo foi redigida para equilibrar o reconhecimento técnico do cliente (desenvolvedor GeneXus) com a proposta de valor direta (o problema está resolvido sem retrabalho):

```textplain
Fala, [Nome do Cliente], bom dia! Tudo bem?

Concluímos a análise técnica e o desenvolvimento da integração com o novo Emissor Nacional da NFS-e (Sefin Nacional).

Como você também é desenvolvedor, vale te adiantar o cenário: a Receita Federal mudou completamente o padrão em relação ao WebService da SIL que usávamos em Vila Velha. Eles abandonaram o SOAP clássico e adotaram uma API REST moderna com autenticação mTLS no handshake, assinatura RSA-SHA256 e payloads compactados em GZIP.

Se fôssemos reescrever essa rotina nativamente dentro do GeneXus, levaria semanas de refatoração de código e testes com risco de travar a emissão. Pensando nisso, desenhamos uma solução muito mais inteligente:

Construímos um middleware em Java que atua como uma 'camada de tradução' transparente. Ele emula exatamente o WebService SOAP da SIL que o seu GeneXus já conhece. Ou seja: você não precisará alterar absolutamente nenhuma linha de código no seu ERP. O seu GeneXus continuará enviando a mesma requisição de sempre, e a nossa API se encarrega de assinar em SHA-256, autenticar mTLS e autorizar a nota direto na Receita Federal.

Já preparei o ambiente de homologação e a suíte de testes locais. Gostaria de alinhar com você para fazermos um primeiro teste prático de emissão com o seu certificado e combinarmos a implantação no seu Tomcat da BrasilCloud. 

Como está sua agenda hoje para alinharmos?
```

---

#### 📱 Opção Alternativa (Mais Direta / Sucinta)

```textplain
Fala, [Nome do Cliente], bom dia!

Finalizamos a integração com a NFS-e Nacional! 

A Receita Federal mudou tudo em relação à prefeitura: agora é REST, mTLS e assinatura SHA-256. Mas para você não precisar mexer em nada no GeneXus, criamos um middleware que emula o SOAP antigo da SIL. 

Resultado: seu ERP continua disparando exatamente o mesmo XML de antes, sem alterar nenhuma linha de código. Apenas apontamos a URL para a nossa API e ela cuida de toda a comunicação com a Receita.

A aplicação já está pronta e testada em homologação. Podemos marcar um horário rápido hoje para eu te mostrar funcionando e combinarmos a subida para o seu Tomcat na BrasilCloud?
```
