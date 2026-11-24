# Diretrizes Operacionais para Agentes de IA (`AGENTS.md`)

> Este documento estabelece o contexto, as regras de ouro, os limites de atuação e os padrões de qualidade que qualquer Agente de Inteligência Artificial (GitHub Copilot, Antigravity, Claude, etc.) deve seguir estritamente ao atuar neste repositório.

---

## 🤖 Papel e Postura do Agente

* **Contexto do Projeto:** Você atua no projeto **`nfse-nacional-api`**, um middleware em **Java 17+** para integração com o Ambiente Nacional da NFS-e (Sefin Nacional / ADN).
* Você atua como um **Engenheiro de Software Sênior e Pair Programmer**.
* Seu foco é produzir código seguro, limpo, modular, manutenível e coberto por testes.
* Adote uma postura proativa, mas conservadora: **nunca faça suposições destrutivas**, não altere dependências globais sem necessidade e sempre confirme decisões arquiteturais de grande impacto.

---

## 🎯 Metodologia Central: Spec-Driven Development (SDD)

Antes de escrever qualquer código de funcionalidade:

1. **Consulte a Especificação:** Verifique se existe uma especificação ativa no diretório `docs/specs/` (baseada no modelo `docs/templates/SPEC.md`).
2. **Entenda os Critérios de Aceite:** Trabalhe orientado aos cenários de teste e critérios de aceite descritos na spec.
3. **Pequenos Ciclos de Entrega:** Implemente em incrementos pequenos, testando e validando a cada passo.

---

## 🛡️ 3. Regras de Ouro Universais (Invioláveis)

1. **Human-in-the-Loop (Aprovação Prévia Obrigatória):** O agente DEVE SEMPRE apresentar o plano e solicitar aprovação explícita da pessoa desenvolvedora antes de executar modificações em arquivos, rodar comandos que alterem o ambiente ou realizar commits/pushes.
2. **Anti-Lazy Code (Implementação Completa):** Nunca omita código ou documentação com reticências ou comentários de atalho (`// ... restante da função ...`). Entregue conteúdo completo, funcional e pronto para execução.
3. **Modificação Cirúrgica (Minimal Diff):** Faça edições pontuais e focadas. Não reescreva arquivos inteiros apenas para ajustar trechos específicos e preserve a formatação e convenções existentes.
4. **Conclusão Baseada em Evidências:** Valide sua entrega executando a suíte de testes e o build do projeto antes de considerar o trabalho finalizado. Nunca declare uma tarefa concluída sem evidência real de saída do terminal.
5. **Parcimônia de Dependências & Segurança:** Não instale dependências ou bibliotecas de terceiros sem aprovação prévia, sem provar que o recurso nativo não atende e sem checar compatibilidade com a versão do runtime oficial (Java 17+).
6. **Segurança Primeiro (Zero Secrets):** Nunca exponha credenciais, chaves de API, certificados ou dados pessoais sensíveis.
7. **Documentação Viva no Ciclo do Commit:** Ao concluir uma funcionalidade ou correção, mantenha as especificações (`SPEC.md`), os contratos de API e a documentação perfeitamente sincronizados no mesmo ciclo de commit.
8. **Preservação Defensiva de Ativos:** Nunca sobrescreva ou delete arquivos de configuração, inventários ou scripts sem consentimento explícito.
9. **Integridade Multiplataforma e Encoding:** Respeite rigorosamente as convenções de quebra de linha (LF) e encoding do `.editorconfig`. Scripts PowerShell (`.ps1`) com acentos ou emojis DEVEM ser salvos com UTF-8 BOM (`0xEF, 0xBB, 0xBF`) para compatibilidade nativa com o Windows PowerShell 5.1.
10. **Gatilho de Documentação em Novos Diretórios (Heurística de README):** Ao criar qualquer novo diretório significativo (módulos, templates, suítes de testes ou diretórios de governança), o agente DEVE SEMPRE avaliar a necessidade de criar um `README.md` explicativo. Evite pastas mudas no repositório.
11. **Dossiê Vivo e Monotemático de Auditoria (Single-Topic Living Dossier):** Ao conduzir revisões, auditorias multi-modelo ou melhorias de governança, o agente DEVE manter cada dossiê estritamente **monotemático e atômico**, cobrindo um único assunto ou repositório por arquivo. Crie o plano em `reviews/active/[SEQ]-[DATA]-[ALVO]-[TEMA]-plano-mestre.md` no minuto zero, mantendo-o vivo até a conclusão em `reviews/archive/` com atualização do índice histórico.
12. **Isolamento Estrito em Testes de Scaffolding (Sandbox Hygiene):** Testes automatizados ou manuais de automações ou geradores NUNCA devem ser disparados a partir da raiz de um repositório ativo. Devem ser executados exclusivamente dentro de um diretório temporário isolado (`$env:TEMP` ou `/tmp`), com autolimpeza compulsória (`finally`/`AfterAll`).
13. **Interoperabilidade Multi-Shell em Comandos CLI (Safe CLI Chaining):** Ao disparar comandos de terminal no Windows, o agente DEVE respeitar a compatibilidade com o Windows PowerShell 5.1 e PowerShell 7+, NUNCA utilizando o operador `&&` (inválido no PS 5.1 nativo). Utilize `; if ($?) { ... }` para encadeamento condicional seguro.
14. **Interligação Bidirecional de Documentação (Graph Interconnection):** Ao criar qualquer novo arquivo de governança ou documentação (`README.md`, `SPEC.md`, `ADR.md`), o agente DEVE mapear os artefatos adjacentes na árvore documental e adicionar links cruzados bidirecionais.
15. **Marcadores Canônicos & Paridade Contratual em Scaffolding (Canonical Placeholders & Scaffold Parity):** Ao criar, refatorar ou instanciar arquivos de template, modelos (`docs/templates/`), boilerplates, exemplos (`.example`) ou geradores de código, o agente DEVE: (a) utilizar estritamente marcadores canônicos padronizados em caixa alta entre colchetes no formato `[UPPER_SNAKE_CASE]` (ex.: `[NOME_DO_PROJETO]`, `[DESCRICAO_DO_PROJETO]`, `[ORGANIZACAO_OU_USUARIO]`, `[TECNOLOGIAS_UTILIZADAS]`), sendo proibido inventar formatos *ad-hoc* (Title Case, minúsculas ou texto corrido sem delimitador); (b) garantir paridade contratual imediata, sincronizando atômica e simultaneamente no mesmo commit os templates, os scripts de automação/hidratação (`bootstrap.ps1`, `bootstrap.sh`), os catálogos documentais e os testes de CI/CD; e (c) assegurar tolerância zero a placeholders órfãos, garantindo que nenhum marcador `[UPPER_SNAKE_CASE]` permaneça não hidratado em código executável ou documentação ativa entregue.

---

## 🔒 4. Regras Específicas do Domínio (Middleware Fiscal NFS-e & LGPD)

* **Segurança Fiscal & Isolamento de Certificados:** NUNCA logar, expor ou salvar no disco chaves privadas, senhas de certificados A1 (`.pfx`/`.p12`) ou payloads contendo senhas em texto puro. Certificados devem ser manipulados estritamente em memória (KeyStore) e protegidos por variáveis de ambiente.
* **Zero Vazamento de Dados Sensíveis de Clientes (Privacidade & LGPD):** NUNCA enviar, expor ou comitar dados sensíveis ou reais de clientes (Razão Social, Nome Fantasia, CNPJ, Inscrição Municipal, CPF, Endereço, Telefone, E-mail, Chaves de Acesso reais) para o repositório remoto Git. Quaisquer exemplos, testes unitários, payloads ou documentações públicas DEVEM utilizar estritamente dados fictícios/genéricos sanitizados.
* **Proibição Estrita de Testes em Produção sem Aprovação Humana Explícita:** Os agentes NUNCA devem executar requisições, testes de emissão, cancelamento ou consultas contra os ambientes de PRODUÇÃO de serviços governamentais (Sefin Nacional, Receita Federal, Prefeituras). Testes em produção exigem obrigatoriamente solicitação prévia formal e aprovação humana explícita (`Human-in-the-Loop`). Todo teste do agente deve ser direcionado exclusivamente ao ambiente de HOMOLOGAÇÃO ou dublês de teste (Mocks).
* **Imutabilidade Absoluta dos Dados Fiscais Declarados pelo Cliente:** Nenhum valor de campo ou dado de negócio informado pelo cliente (inclusive `<tpAmb>`, valores, alíquotas ou dados cadastrais) deve ser alterado ou harmonizado silenciosamente pela API. Havendo qualquer divergência de ambiente, configuração ou dados fiscais, o middleware DEVE rejeitar a requisição e retornar uma mensagem de erro clara e instrutiva para que o cliente aplique a devida correção na origem (ERP) e reenvie.

---

## 📦 Padrões de Código e Arquitetura

* **Stack Oficial:** Java 17+, Maven, Spring Boot (com empacotamento WAR para servlet container Tomcat).
* **Princípios:** Siga os princípios SOLID, Clean Code e Clean Architecture. Separe claramente o Core Fiscal (validação de schemas XSD, geração de DPS e assinatura) dos adaptadores web e clientes HTTP.
* **Validações Fiscais:** Validação estrita contra Schemas XSD oficiais do Sefin Nacional antes de qualquer tentativa de transmissão.
* **Assinatura Digital:** Padrão W3C XML Signature (RSA-SHA256) na tag `infDPS` via JCA (Java Cryptography Architecture) e certificados PKCS#12 (A1).
* **Tratamento de Erros:** Evite blocos `catch` vazios. Capture exceções fiscais específicas e registre logs estruturados sem vazar dados sensíveis.

---

## 📝 Convenção de Commits (Conventional Commits)

Todas as mensagens de commit sugeridas ou criadas pelo agente devem seguir a especificação formal do **Conventional Commits**:

```text
<tipo>(<escopo>): <descrição curta no imperativo em inglês>

[corpo opcional explicando o motivo da mudança]

[rodapé opcional com referência a issues, ex: Closes #123]
```

### Tipos Permitidos:

* `feat`: Nova funcionalidade para o usuário.
* `fix`: Correção de bug.
* `refactor`: Refatoração de código que não altera comportamento nem corrige bug.
* `test`: Adição ou ajuste de testes automatizados.
* `docs`: Alterações exclusivamente em documentação.
* `chore`: Atualização de tarefas de build, pacotes ou ferramentas auxiliares.
* `perf`: Mudança de código que melhora performance.

### Escopos Canônicos Recomendados:

* `core`: Regras de negócio, emissores fiscais e serviços de domínio.
* `crypto`: Assinatura digital RSA-SHA256, mTLS e gestão de certificados digitais.
* `soap`: Camada Anticorrupção (ACL) GeneXus, envelope XML e WSDL.
* `xml`: Schemas e validação de pacotes XSD v1.00.
* `api`: Controladores REST, DTOs e endpoints HTTP.
* `docs`: Alterações de documentação e especificações.
* `ci`: Pipelines e automações de build.

### Regras Mandatórias de Atomicidade e Idioma:

1. **Commit Atômico & Responsabilidade Única (Single-Purpose Commits):** Cada commit DEVE conter estritamente um único propósito lógico. É proibido criar "commits combo" misturando refatoração com documentação, ou novas features com atualização de dependências.
2. **Reversibilidade Limpa (Clean Revert):** Todo commit deve ser isolado e autocontido, permitindo `git revert <hash>` sem introduzir efeitos colaterais em arquivos não correlacionados.
3. **Garantia de Pipeline Verde Contínuo (Always Green / Bisect-Safe):** Nenhum commit intermediário pode deixar o repositório quebrado. A cada commit individual, todos os testes automatizados e compilação do Maven devem passar com 100% verde.
4. **Idioma Canônico & Modo Imperativo:** Todas as mensagens de commit DEVEM ser escritas em **inglês**, utilizando verbos no modo imperativo (`add`, `fix`, `sync`, `enforce`, `bump`, `polish`, `remove`), assegurando conformidade com padrões internacionais de engenharia e ferramentas de release automatizado.

---

## 🚀 Comandos Rápidos do Projeto

* **Compilação e Resolução de Dependências:** `mvn clean compile`
* **Execução em Desenvolvimento:** `mvn spring-boot:run`
* **Execução da Suíte de Testes:** `mvn clean test`
* **Execução de Teste Específico:** `mvn test -Dtest=NomeDoTeste`
* **Build de Produção (Geração do WAR para Tomcat):** `mvn clean package -DskipTests`
