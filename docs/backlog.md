# Backlog do Projeto — NFS-e Nacional API

> **Docs-as-Code — Planejamento Ágil, Especificações & Roadmap**
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)
> Quadro de Acompanhamento: [AI Engineering Hub — Portfolio & Roadmap (Project 14)](https://github.com/users/heviane/projects/14)
> Versão Atual: `v0.1.0-alpha` (Incepção & Fase Zero)

---

## 📌 Sumário Executivo

Este backlog organiza a evolução do projeto **NFS-e Nacional API**, concebido como middleware desacoplado entre ERPs e o **Ambiente Nacional da NFS-e (Sefin Nacional / ADN)**.
O desenvolvimento é orientado a especificações (**Spec-Driven Development — SDD**), testes antecipados (**TDD**) e fatias verticais incrementais.

---

## 🚦 Legenda de Status e Prioridades

- **Status:** 🟢 `[Concluído]` &bull; 🟡 `[Em Andamento]` &bull; ⚪ `[A Fazer]` &bull; 💡 `[Ideação / Em Análise]`
- **Prioridade:** `P0` (Crítica / Bloqueante) &bull; `P1` (Alta) &bull; `P2` (Média) &bull; `P3` (Baixa)

---

## 🎯 Épicos e Iniciativas

### Épico 0: Setup, Higiene & Governança do Repositório (Fase Zero)

| Status |    ID    | Prioridade | Item                                         | Descrição / Critérios de Aceite                                                                                                      |
| :----: | :------: | :--------: | :------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------- |
|   🟢   | `SET-01` |    `P0`    | **Segurança no `.gitignore` & Configuração** | Blindar o `.gitignore` contra certificados A1 (`*.pfx`, `*.p12`, `*.pem`, `*.jks`) e padronizar o `.editorconfig` para Java/XML/XSD. |
|   🟢   | `SET-02` |    `P0`    | **README Oficial do Projeto**                | Criar [`README.md`](../README.md) com identidade da API, badges, diagrama de arquitetura Mermaid e pré-requisitos técnicos.          |
|   🟢   | `SET-03` |    `P1`    | **Ajuste do `AGENTS.md` Local**              | Adequar o contrato de IA para a stack Java, padrão de testes e regras de segurança fiscal.                                           |
|   ⚪   | `SET-04` |    `P1`    | **Walking Skeleton Java**                    | Inicializar a estrutura base do projeto Java (Maven/Gradle) com teste de compilação executável via terminal.                         |

---

### Épico 1: Arquitetura Técnica & Decisões Fundamentais (ADRs)

| Status |    ID    | Prioridade | Item                                                            | Descrição / Critérios de Aceite                                                                                         |
| :----: | :------: | :--------: | :-------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------- |
|   ⚪   | `ARQ-01` |    `P0`    | **ADR-001: Escolha do Build Tool (Maven vs Gradle)**            | Formalizar a escolha do gerenciador de build (Maven) considerando compatibilidade com ecossistema fiscal e plugins XSD. |
|   ⚪   | `ARQ-02` |    `P0`    | **ADR-002: Framework Web & Runtime (Spring Boot / WAR)**        | Definir a stack e modelo de empacotamento (WAR para servlet container Tomcat na BrasilCloud).                           |
|   ⚪   | `ARQ-03` |    `P1`    | **ADR-003: Padrão Arquitetural (Hexagonal / Ports & Adapters)** | Estabelecer o isolamento do Core Fiscal (regras de DPS, validação e assinatura) desacoplado do framework web.           |

---

### Épico 2: Engenharia de Requisitos & Especificação Técnica (SDD)

| Status |    ID     | Prioridade | Item                                                   | Descrição / Critérios de Aceite                                                                                             |
| :----: | :-------: | :--------: | :----------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------- |
|   ⚪   | `SPEC-01` |    `P0`    | **SPEC-001: Validação de DPS e Assinatura Digital**    | Elaborar a especificação da Fatia 1 (regras de validação de schemas XSD da DPS e assinatura XML A1).                        |
|   ⚪   | `SPEC-02` |    `P1`    | **Curadoria de Schemas e Exemplos Oficiais**           | Mapear e armazenar os schemas XSD oficiais do Sefin Nacional e amostras reais de DPS em `docs/schemas/`.                    |
|   ⚪   | `SPEC-03` |    `P1`    | **Análise de Compatibilidade do `assinador-xml-java`** | Avaliar os métodos da biblioteca atual de assinatura em relação aos requisitos da NFS-e Nacional (RSA-SHA256, DigestValue). |

---

### Épico 3: Core Fiscal — Validação e Assinatura Digital (TDD)

| Status |    ID     | Prioridade | Item                                                | Descrição / Critérios de Aceite                                                                      |
| :----: | :-------: | :--------: | :-------------------------------------------------- | :--------------------------------------------------------------------------------------------------- |
|   ⚪   | `CORE-01` |    `P0`    | **Suite de Testes Unitários de Validação XSD**      | Criar testes automatizados para validar payloads de DPS válidos e rejeitar estruturas malformadas.   |
|   ⚪   | `CORE-02` |    `P0`    | **Módulo de Assinatura Digital com Certificado A1** | Implementar o fluxo de assinatura da tag `infDPS` usando chave privada do certificado A1 em memória. |

---

### Épico 4: Transmissão & Comunicação com o Sefin Nacional / ADN

| Status |    ID    | Prioridade | Item                                            | Descrição / Critérios de Aceite                                                                   |
| :----: | :------: | :--------: | :---------------------------------------------- | :------------------------------------------------------------------------------------------------ |
|   ⚪   | `COM-01` |    `P1`    | **Autenticação mTLS com Certificado A1**        | Configurar `SSLContext` com keystore PKCS#12 para handshake TLS mútuo com o Sefin Nacional.       |
|   ⚪   | `COM-02` |    `P1`    | **Cliente de Transmissão de DPS (Homologação)** | Implementar envio da DPS assinada e tratamento síncrono/assíncrono da resposta do Sefin Nacional. |
|   ⚪   | `COM-03` |    `P1`    | **Módulo de Consulta e Cancelamento**           | Implementar os serviços de consulta de situação da nota e envio de eventos de cancelamento.       |

---

### Épico 5: Exposição da API REST & Integração com o ERP

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
   - O ideal seria que ao criar um repositório a partir deste template, o readme fosse criado customizado com base nos metadados informados para a criação, e não simplesmente copiar o README do template, pois isso é inutil.
   - Existem arquivos no repositório template que são especificos dele, e não devemos importar para o novo repositório. O que podemos fazer?

   ## Notes

   A tarefa SET-04 deve vir após as decisões de arquitetura.
   O épico zero deve ser sobre um repositório padrão, onde as definições até o momento são apenas "deve ser gerado em java".
