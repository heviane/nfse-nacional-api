# Backlog do Projeto — NFS-e Nacional API

> **Docs-as-Code — Planejamento Ágil, Especificações & Roadmap**
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)
> Quadro de Acompanhamento: [AI Engineering Hub — Portfolio & Roadmap (Project 14)](https://github.com/users/heviane/projects/14)
> Versão Atual: `v0.1.0-alpha` (Incepção & Fase Zero Concluída)

---

## 📌 Sumário Executivo

Este backlog organiza a evolução do projeto **NFS-e Nacional API**, concebido como middleware desacoplado entre ERPs e o **Ambiente Nacional da NFS-e (Sefin Nacional / ADN)**.
O desenvolvimento é orientado a especificações (**Spec-Driven Development — SDD**), testes antecipados (**TDD**), decisões arquiteturais documentadas (**ADRs**) e fatias verticais incrementais.

---

## 🚦 Legenda de Status e Prioridades

- **Status:** 🟢 `[Concluído]` &bull; 🟡 `[Em Andamento]` &bull; ⚪ `[A Fazer]` &bull; 💡 `[Ideação / Em Análise]`
- **Prioridade:** `P0` (Crítica / Bloqueante) &bull; `P1` (Alta) &bull; `P2` (Média) &bull; `P3` (Baixa)

---

## 🎯 Épicos e Iniciativas

### Épico 0: Governança & Higiene Inicial do Repositório (Fase Zero) — 🟢 100% Concluído

| Status |    ID    | Prioridade | Item                                          | Descrição / Critérios de Aceite                                                                                                      |
| :----: | :------: | :--------: | :-------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------- |
|   🟢   | `SET-01` |    `P0`    | **Segurança no `.gitignore` & Configuração**  | Blindar o `.gitignore` contra certificados A1 (`*.pfx`, `*.p12`, `*.pem`, `*.jks`) e padronizar o `.editorconfig` para Java/XML/XSD. |
|   🟢   | `SET-02` |    `P0`    | **README Oficial do Projeto**                 | Criar [`README.md`](../README.md) com identidade da API, badges, diagrama de arquitetura Mermaid e pré-requisitos técnicos.          |
|   🟢   | `SET-03` |    `P1`    | **Ajuste do `AGENTS.md` Local**               | Adequar o contrato de IA para a stack Java, padrão de testes e regras estritas de segurança fiscal.                                  |
|   🟢   | `SET-04` |    `P1`    | **Higiene do Editor & Diretrizes do Copilot** | Padronizar `.vscode/extensions.json` e `.vscode/settings.json` para Java/XML e alinhar `.github/copilot-instructions.md`.            |

---

### Épico 1: Arquitetura Técnica & Decisões Fundamentais (ADRs) — 🟢 100% Concluído

| Status |    ID    | Prioridade | Item                                                                  | Descrição / Critérios de Aceite                                                                                                 |
| :----: | :------: | :--------: | :-------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------ |
|   🟢   | `ARQ-01` |    `P0`    | **ADR-001: Escolha da Linguagem & Plataforma de Execução (Java 17+)** | Documentar a escolha técnica do Java 17 com base no ambiente do cliente (Tomcat BrasilCloud) e maturidade fiscal/criptográfica. |
|   🟢   | `ARQ-02` |    `P0`    | **ADR-002: Escolha do Build Tool (Maven vs Gradle)**                  | Formalizar a escolha do gerenciador de build (Maven) considerando compatibilidade com ecossistema fiscal e plugins XSD.         |
|   🟢   | `ARQ-03` |    `P0`    | **ADR-003: Framework Web & Runtime (Spring Boot / WAR)**              | Definir a stack e modelo de empacotamento (WAR para servlet container Tomcat na BrasilCloud).                                   |
|   🟢   | `ARQ-04` |    `P1`    | **ADR-004: Padrão Arquitetural (Hexagonal / Ports & Adapters com ACL)**| Estabelecer o isolamento do Core Fiscal e desacoplamento do cliente via Camada Anticorrupção (ACL para SOAP GeneXus).          |

---

### Épico 2: Setup do Ambiente Java & Walking Skeleton

| Status |    ID    | Prioridade | Item                                                         | Descrição / Critérios de Aceite                                                                                                |
| :----: | :------: | :--------: | :----------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------- |
|   ⚪   | `JAV-01` |    `P0`    | **Inicialização do Projeto (Build Tool & Wrapper `mvnw`)**   | Gerar estrutura inicial orientada pelas ADRs com `pom.xml`, wrapper executável e compilação limpa via terminal.                |
|   ⚪   | `JAV-02` |    `P0`    | **Configuração de Dependências & JVM (Java 17)**             | Configurar plugins de compilação, encoding UTF-8, suíte de testes (JUnit 5, AssertJ) e dependências básicas de runtime.        |
|   ⚪   | `JAV-03` |    `P1`    | **Walking Skeleton & Health Check Endpoint (`GET /health`)** | Implementar endpoint mínimo funcional para verificação de subida da aplicação e testes automatizados de fumaça (_smoke test_). |
|   ⚪   | `JAV-04` |    `P2`    | **Perfis de Execução no VS Code (`launch.json` / `tasks`)**  | Configurar perfis de depuração com F5 para Java e tarefas integradas de compilação/execução no editor.                         |

---

### Épico 3: Engenharia de Requisitos & Especificação Técnica (SDD)

| Status |    ID     | Prioridade | Item                                                   | Descrição / Critérios de Aceite                                                                                             |
| :----: | :-------: | :--------: | :----------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------- |
|   ⚪   | `SPEC-01` |    `P0`    | **SPEC-001: Validação de DPS e Assinatura Digital**    | Elaborar a especificação da Fatia 1 (regras de validação de schemas XSD da DPS e assinatura XML A1).                        |
|   ⚪   | `SPEC-02` |    `P1`    | **Curadoria de Schemas e Exemplos Oficiais**           | Mapear e armazenar os schemas XSD oficiais do Sefin Nacional e amostras reais de DPS em `docs/schemas/`.                    |
|   ⚪   | `SPEC-03` |    `P1`    | **Análise de Compatibilidade do `assinador-xml-java`** | Avaliar os métodos da biblioteca atual de assinatura em relação aos requisitos da NFS-e Nacional (RSA-SHA256, DigestValue). |

---

### Épico 4: Core Fiscal — Validação e Assinatura Digital (TDD)

| Status |    ID     | Prioridade | Item                                                | Descrição / Critérios de Aceite                                                                      |
| :----: | :-------: | :--------: | :-------------------------------------------------- | :--------------------------------------------------------------------------------------------------- |
|   ⚪   | `CORE-01` |    `P0`    | **Suite de Testes Unitários de Validação XSD**      | Criar testes automatizados para validar payloads de DPS válidos e rejeitar estruturas malformadas.   |
|   ⚪   | `CORE-02` |    `P0`    | **Módulo de Assinatura Digital com Certificado A1** | Implementar o fluxo de assinatura da tag `infDPS` usando chave privada do certificado A1 em memória. |

---

### Épico 5: Transmissão & Comunicação com o Sefin Nacional / ADN (mTLS)

| Status |    ID    | Prioridade | Item                                            | Descrição / Critérios de Aceite                                                                   |
| :----: | :------: | :--------: | :---------------------------------------------- | :------------------------------------------------------------------------------------------------ |
|   ⚪   | `COM-01` |    `P1`    | **Autenticação mTLS com Certificado A1**        | Configurar `SSLContext` com keystore PKCS#12 para handshake TLS mútuo com o Sefin Nacional.       |
|   ⚪   | `COM-02` |    `P1`    | **Cliente de Transmissão de DPS (Homologação)** | Implementar envio da DPS assinada e tratamento síncrono/assíncrono da resposta do Sefin Nacional. |
|   ⚪   | `COM-03` |    `P1`    | **Módulo de Consulta e Cancelamento**           | Implementar os serviços de consulta de situação da nota e envio de eventos de cancelamento.       |

---

### Épico 6: Exposição da API REST & Integração com o ERP

| Status |    ID    | Prioridade | Item                                                | Descrição / Critérios de Aceite                                                                          |
| :----: | :------: | :--------: | :-------------------------------------------------- | :------------------------------------------------------------------------------------------------------- |
|   ⚪   | `API-01` |    `P1`    | **Endpoints REST para o ERP**                       | Criar rotas padronizadas (`POST /api/v1/dps/emitir`, `GET /api/v1/nfse/{chave}`, etc.) com DTOs limpos.  |
|   ⚪   | `API-02` |    `P2`    | **Documentação Interativa (Swagger/OpenAPI)**       | Configurar interface Swagger UI para que desenvolvedores de ERP testem a integração rapidamente.         |
|   ⚪   | `API-03` |    `P1`    | **Build e Empacotamento para Tomcat (BrasilCloud)** | Configurar geração de artefato WAR/JAR otimizado para deploy em servlet container Tomcat na BrasilCloud. |

---

## 💡 Banco de Ideias & Melhorias Futuras

Propostas técnicas para avaliação nas próximas iterações:

1. **Dashboard Administrativo / Web UI:**
   Painel visual leve (React/Vite ou UI embutida) para que o cliente ou sua equipe contábil visualize notas transmitidas, logs de rejeição da Receita e status dos certificados A1.
2. **Suporte Multi-Tenant / Multi-Certificados:**
   Capacidade de a mesma API gerenciar certificados A1 de diferentes filiais/empresas roteados por CNPJ.
3. **Melhorias no Repositório Template (`project-starter-kit`):**
   - Revisar o template oficial para mantê-lo agnóstico de stack e enriquecê-lo com o template padrão de `BACKLOG.md`.
   - O ideal seria que ao criar um repositório a partir deste template, o readme fosse criado customizado com base nos metadados informados para a criação, e não simplesmente copiar o README do template, pois isso é inútil.
   - Existem arquivos no repositório template que são específicos dele, e não devemos importar para o novo repositório. O que podemos fazer?
4. **Pipeline de CI/CD Automatizado para Tomcat (BrasilCloud):**
   Criar workflow no GitHub Actions para automatizar a compilação do `.war` e deploy contínuo via SSH/SFTP no servidor do cliente, reduzindo o trabalho manual após a fase de estabilização.
5. **Estudo de Modelo de Negócio para Futuros Clientes:**
   Avaliar a viabilidade de transformar a API em um SaaS centralizado (hospedagem própria com cobrança por volume de notas via REST) versus distribuição self-hosted (instâncias dedicadas por cliente).

## 📝 Notas & Registro de Pensamentos

Notas para não esquecer, para análise futura:

- [x] Analisar a necessidade de criar uma documentação específica para explicar o contexto, o problema e a solução pensada para o caso do cliente 001. &rarr; *(Concluído em [`docs/clients/cliente-001.md`](./clients/cliente-001.md))*
- [x] Incluir no glossário arquitetura hexagonal e camada anticorrupção. &rarr; *(Concluído em [`docs/glossario.md`](./glossario.md))*
