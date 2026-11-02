# Prompt inicial: Resposta Claude

Seu rascunho já tem uma base boa: contexto, requisitos separados em funcionais e não funcionais, e o que você quer como entrega. Abaixo estão os pontos de melhoria, os riscos que encontrei nos arquivos anexados e uma versão revisada do prompt.

## 1. Feedback sobre o prompt

**O que está bom**
- Você diz o objetivo, o fluxo (ERP → sua API → ambiente nacional) e as restrições de infraestrutura.
- Você avisa que o repositório é público e genérico. Isso muda a forma de escrever exemplos e documentação.
- Você menciona a biblioteca existente e já sinaliza que a compatibilidade precisa ser verificada.

**O que melhorar**

1. **Falta dizer qual é o papel da IA e qual é o resultado esperado de cada etapa.** Hoje o prompt mistura três pedidos (nome do repo, README e documentação) com decisões de arquitetura que ainda não foram tomadas. Em projetos assistidos por IA, funciona melhor trabalhar em fases: primeiro alinhar a arquitetura e as decisões, depois gerar os documentos.

2. **Faltam decisões técnicas que a IA vai precisar tomar (ou perguntar).** Por exemplo:
   - Qual framework Java? Servlet puro, Spring Boot em WAR ou Jakarta EE?
   - Qual versão do Tomcat? Até o Tomcat 9 o pacote é `javax.*`, a partir do 10 é `jakarta.*`, e isso afeta a escolha de bibliotecas.
   - A API do seu ERP-cliente será síncrona? Haverá fila ou retentativa?
   - Onde ficam o certificado e a senha (variáveis de ambiente, cofre, arquivo)? Uma API que guarda certificado A1 de clientes precisa de uma decisão clara aqui, ainda mais num projeto público.
   - O projeto atende um cliente por instância ou vários clientes (multi-tenant) na mesma instância? Você diz que quer reutilizar para outros clientes, mas isso muda muito o desenho.

3. **Você não diz o que a IA deve fazer com os arquivos anexados nem em que confiar.** Os `.md` do projeto parecem ser respostas de outras conversas com IA, não documentação oficial. Diga explicitamente algo como "estes arquivos são rascunhos não verificados; a fonte de verdade é o manual oficial, e quando houver divergência, sinalize". Isso evita que erros se propaguem (veja a seção 2).

4. **A numeração tem um deslize e o item 4 não é uma entrega.** Há dois itens "3", e o "4" ("o repositório vai ser público e genérico") é uma restrição, não algo a ser entregue. Mova-o para a seção de restrições.

5. **Falta o critério de pronto ("definition of done") e o formato de saída.** Diga, por exemplo: "responda em português", "entregue o README em Markdown", "liste as dúvidas antes de gerar".

6. **Pequenos ajustes de redação.** Há "((Ambiente de Dados Nacional))" com parênteses duplicados, "devolvar" (devolver), "ponta pé" (pontapé) e "meu cliente" misturado com "o cliente". Num prompt, ambiguidade de quem é quem custa caro. Defina uma vez: "ERP do cliente" e "nossa API".

7. **Técnicas que valem a pena para você aprender agora:**
   - **Pedir perguntas antes de executar**: "antes de gerar, liste suas dúvidas e premissas".
   - **Separar fatos de hipóteses**: o que já está decidido e o que está em aberto.
   - **Registrar decisões em ADRs** (Architecture Decision Records), documentos curtos de uma página por decisão. Eles funcionam muito bem com IA porque dão memória ao projeto.
   - **Ter um arquivo de contexto permanente no repositório** (por exemplo `CLAUDE.md` ou `AGENTS.md`) com stack, convenções e regras, para você não repetir tudo em cada conversa.
   - **Desenvolvimento guiado por especificação**: escrever primeiro o contrato (OpenAPI da sua API) e só depois o código.

## 2. Riscos que encontrei nos arquivos anexados

Eles são importantes porque, se entrarem no prompt como verdade, a IA vai construir em cima deles.

**Sobre a sua biblioteca de assinatura (`AssinadorXml`)**
- **Algoritmos:** ela usa `SHA1` e `RSA_SHA1` fixos no código. Os próprios documentos do projeto dizem que o ambiente nacional exige SHA-256. Hoje o algoritmo não é parametrizável.
- **Onde a assinatura é inserida:** o método assina o nó indicado e coloca a `<Signature>` dentro dele (o `signatureParent` é o próprio nó alvo). Na DPS, o esperado é que a assinatura fique como irmã de `infDPS`, dentro de `<DPS>`, e não dentro de `infDPS`. É preciso separar "nó referenciado" de "nó onde a assinatura entra".
- **Pós-processamento específico da SIL:** o código remove `\r`, `\n` e `standalone="no"` do XML final. Isso foi feito para a prefeitura e deve virar uma opção, não o comportamento padrão.
- **Transformações:** a lista contém só `enveloped`. O exemplo em `emissao-sefin.md` usa também a canonicalização exclusiva como transform. Valide contra o manual.
- **README e guia desatualizados:** o README fala só em SHA1, e o guia cita `assinador-1.0.0.jar` enquanto o `pom.xml` está na versão 2.0.0.
- **Segurança do parser XML:** o `DocumentBuilderFactory` não desativa DTD nem entidades externas. Numa API que recebe XML de terceiros, isso precisa ser configurado (proteção contra XXE).
- **Teste:** `AssinadorXmlTeste` é um `main`, não um teste automatizado (JUnit). Para um projeto que vai ser reutilizado, vale evoluir isso.

**Sobre os documentos de contexto**
- **Dados reais em `emissao-sefin.md`:** o XML de exemplo contém CNPJs, razão social, endereço, e-mail e telefone que parecem reais. Como o repositório será público, troque por dados fictícios válidos (CNPJs de teste) antes de qualquer commit. Verifique também o histórico do Git.
- **Contradições entre os documentos:** `migracao-2.md` diz que se assina a tag raiz `<DPS>`, enquanto os outros dizem que é o `infDPS` (com `Id`). Há também afirmações diferentes sobre o tamanho do `Id` da DPS (46 posições) que valem ser conferidas.
- **Onde ocorre o cancelamento:** `sefin_vs_adn.md` coloca o registro de eventos no ADN. Confirme nos manuais oficiais em qual API cada operação (consulta, eventos, cancelamento) é de fato exposta aos contribuintes, porque isso define a estrutura do seu cliente HTTP.
- **Exemplo de resposta da API:** o JSON de retorno em `emissao-sefin.md` parece ilustrativo. Não o trate como contrato. O contrato real está no Swagger e nos manuais PDF listados em `docs.md`.
- **Prazo:** a mensagem da prefeitura fala em 1º de novembro de 2026, que é hoje. Se o seu cliente precisa emitir já, vale decidir se haverá um caminho provisório (Emissor Web) enquanto a integração é construída.

## 3. Sugestão de prompt revisado

Eu dividiria em dois prompts. Este é o primeiro, voltado a alinhar antes de gerar documentos:

```
# Papel
Você é um engenheiro de software sênior em Java, com experiência em integrações
fiscais brasileiras (NFS-e, XML-DSig, mTLS). Estou começando um projeto e sou
iniciante em desenvolvimento assistido por IA, então explique brevemente o porquê
das suas recomendações.

# Objetivo
Criar uma API Java que faça a integração direta com o Ambiente Nacional da NFS-e,
servindo de ponte entre o ERP de um cliente e as APIs nacionais.

# Fluxo
ERP do cliente -> Nossa API -> Sefin Nacional / ADN -> resposta devolvida ao ERP.
A nossa API deve:
1. Receber os dados da nota (XML ou JSON).
2. Validar contra o schema oficial (XSD).
3. Assinar digitalmente a DPS (certificado A1).
4. Transmitir ao ambiente nacional com mTLS.
5. Devolver a resposta ao ERP.

# Operações em escopo
- Emissão (Sefin Nacional)
- Consulta (ADN)
- Cancelamento via evento (confirmar na documentação oficial qual API atende)

# Restrições
- Runtime: Java [versão], Tomcat [versão], hospedagem BrasilCloud.
- Certificado A1 (.pfx) já disponível.
- Repositório público e genérico: nenhum dado de cliente, CNPJ real ou
  credencial pode aparecer em código, exemplos ou documentação. Use dados fictícios.
- Reutilizável para outros clientes (modelo: [uma instância por cliente | multi-tenant]).

# Ativo existente
Biblioteca própria de assinatura: https://github.com/heviane/assinador-xml-java
(em produção para o modelo legado SIL). Analise a compatibilidade com o ambiente
nacional, considerando algoritmos de digest/assinatura, local de inserção da
Signature e canonicalização. Diga o que adaptar ou reescrever.

# Fontes de verdade
- Os arquivos anexados (.md) são rascunhos de conversas anteriores, NÃO verificados.
- A fonte de verdade é a documentação oficial (links em docs.md).
- Quando houver divergência ou dúvida, sinalize explicitamente em vez de assumir.

# O que quero agora (ainda não gere os documentos finais)
1. Liste as perguntas e premissas que você precisa confirmar comigo.
2. Aponte riscos e inconsistências nos materiais anexados.
3. Proponha 2 opções de arquitetura (com prós e contras) e recomende uma.
4. Sugira 3 a 5 decisões que devem virar ADRs.

# Formato
Responda em português, em Markdown, com seções curtas e objetivas.
```

Depois que as perguntas forem respondidas, o segundo prompt fica bem mais simples: "Com as decisões acima, gere: (1) nome e descrição do repositório (3 opções), (2) README inicial, (3) estrutura de documentação em `docs/` (visão geral, arquitetura, ADRs, contrato da API, guia de segurança, guia de homologação) e (4) um `CLAUDE.md` ou `AGENTS.md` com as convenções do projeto."

## 4. Próximos passos

Responda às perguntas da seção 1, item 2 (framework, versão do Tomcat, modelo de multi-cliente e onde ficará o certificado). Com isso, posso montar a versão final do prompt e já fazer o primeiro passo do projeto, incluindo o diagnóstico detalhado da compatibilidade da sua biblioteca.