# Refinamento dos Templates

O refinamento dos **Templates** é a base mais concreta para garantir que os próximos projetos comecem com qualidade sênior desde o "commit zero".

Abaixo apresento a análise crítica e as propostas de evolução para os **5 templates essenciais**, incorporando as lições aprendidas no `nfse-nacional-api`:

1. **`AGENTS.md` (Template Universal + Camada de Domínio)**
2. **`SPEC.md` (Spec-Driven Development com Fatias Verticais e DoD)**
3. **`ADR.md` (Architecture Decision Record com Gatilhos de Reavaliação)**
4. **`BACKLOG.md` (Docs-as-Code com Taxonomia de IDs e Métricas)**
5. **`ANALYSIS-AI.md` (Novo Template: Auditoria e Revisão Multi-IA)**

---

## 1. `AGENTS.md` — O Contrato Operacional Universal

### 🔍 Diagnóstico do Uso Real

- **O que funcionou:** As regras de _Human-in-the-Loop_, _Anti-Lazy Code_, _Minimal Diff_ e _Conclusão Baseada em Evidências_ foram o pilar que impediu alucinações destrutivas.
- **Oportunidade de Evolução:** Separar o arquivo em **2 camadas claras**:
  1. **Núcleo Universal (Cross-Stack):** Comportamento ético, limites do agente, segurança genérica e fluxo de aprovação (imutável para qualquer linguagem).
  2. **Plug-in de Domínio & Stack:** Variáveis específicas do projeto (Java/Node/Python, regras de LGPD, políticas de produção, comandos de build/testes).

### 📐 Anatomia Proposta do Template (`docs/templates/AGENTS.md`)

```markdown
# Diretrizes Operacionais para Agentes de IA (`AGENTS.md`)

## 🤖 1. Papel e Postura do Agente

- Contexto do Projeto: [Nome do Projeto] — [Linguagem/Stack Principal]
- Papel: Engenheiro de Software Sênior e Pair Programmer rigoroso.
- Postura: Proativo na análise, estritamente conservador na execução.

## 🛡️ 2. Regras de Ouro Universais (Invioláveis)

1. Human-in-the-Loop (Aprovação Prévia Obrigatória): Apresentar plano e pedir aprovação explícita antes de editar arquivos, executar comandos que alterem o ambiente ou comitar/empurrar código.
2. Anti-Lazy Code (Implementação Completa): Proibição de comentários de atalho (`// ... restante`) ou código incompleto.
3. Modificação Cirúrgica (Minimal Diff): Foco na alteração estritamente necessária, preservando formatação e código adjacente.
4. Conclusão Baseada em Evidências: Validar toda alteração executando a suíte de testes antes de considerar o item concluído.
5. Parcimônia de Dependências: Não adicionar bibliotecas sem justificativa técnica e autorização explícita.
6. Segurança Primeiro: Credenciais via variáveis de ambiente; suporte a `.env.example`.

## 🔒 3. Regras Específicas do Domínio do Projeto (Customizável)

- [Regra de Domínio 1 - ex: Privacidade & LGPD: Proibição de dados reais em testes]
- [Regra de Domínio 2 - ex: Proibição de testes contra ambiente de PRODUÇÃO]
- [Regra de Domínio 3 - ex: Imutabilidade de dados de entrada do cliente]

## 📦 4. Padrões de Código e Arquitetura

- Stack Oficial: [Linguagem, Framework, Runtime]
- Princípios: [SOLID, Clean Code, Clean Architecture / Hexagonal]
- Gestão de Erros: Tipagem forte de exceções e proibição de blocos catch vazios.

## 📝 5. Convenção de Commits (Conventional Commits)

- Formato: `<tipo>(<escopo>): <descrição no imperativo>`

## 🚀 6. Comandos Rápidos do Projeto

- Compilar: `[comando]`
- Testar Tudo: `[comando]`
- Teste Específico: `[comando]`
- Rodar Localmente: `[comando]`
```

---

## 2. `SPEC.md` — Especificação Orientada a Fatias Verticais (SDD)

### 🔍 Diagnóstico do Uso Real

- **O que funcionou:** Definir RF, RNF e contratos de API reduziu o retrabalho a quase zero.
- **Oportunidade de Evolução:** Adicionar seções que forçam o agente a pensar em **casos de falha (_Failure Modes_)** e **fatias verticais (_Vertical Slicing_)**, evitando que a IA tente implementar a funcionalidade inteira em uma única etapa monolítica.

### 📐 Anatomia Proposta do Template (`docs/templates/SPEC.md`)

```markdown
# SPEC-[NUM]: [Título da Funcionalidade / Épico]

> **Status:** `Rascunho` | `Em Revisão` | `Aprovado` | `Implementado`
> **Item do Backlog Relacionado:** `[ID-DO-BACKLOG]` &bull; **ADRs Vinculadas:** `[ADR-XXX]`
> **Data:** AAAA-MM-DD &bull; **Autor(a):** [Nome]

---

## 1. 🎯 Contexto e Valor de Negócio

- Qual problema real esta spec resolve?
- Quem é o usuário/consumidor direto?

## 2. 📋 Requisitos Funcionais & Critérios de Aceite (Gherkin)

| ID        | Requisito           | Prioridade | Cenário / Critério de Aceite (BDD)                                      |
| :-------- | :------------------ | :--------: | :---------------------------------------------------------------------- |
| **RF-01** | [Nome do Requisito] |    `P0`    | **Dado** [contexto], **Quando** [ação], **Então** [resultado esperado]. |

## 3. ⚠️ Casos de Borda e Modos de Falha (Failure Modes)

_Como o sistema reage quando coisas dão errado?_

- Timeout / Serviço indisponível: [comportamento esperado]
- Payload inválido / Parcial: [código de status / mensagem de erro]
- Conflito / Duplicidade: [estratégia de idempotência]

## 4. ⚙️ Requisitos Não-Funcionais (RNF)

- Performance / Latência máxima tolerada.
- Segurança / Autenticação / Sanitização.
- Observabilidade (MDC / Correlation-ID / Métricas).

## 5. 🏛️ Contratos de Interface (API / Schemas)

- Endpoints (REST/SOAP/GraphQL) ou Eventos (Kafka/RabbitMQ).
- Exemplos de Request e Response (sucesso e falha).

## 6. 🍰 Plano de Execução em Fatias Verticais (Vertical Slices)

- [ ] **Fatia 1 (Contrato & Domínio):** Records/DTOs e validações isoladas com testes unitários.
- [ ] **Fatia 2 (Core Business Logic):** Caso de uso central com dublês de teste (Mocks).
- [ ] **Fatia 3 (Adaptador & Transporte):** Controller/Filtro HTTP e tratamento de exceções.
- [ ] **Fatia 4 (End-to-End & Smoke):** Teste integrado de fumaça e documentação da API.

## 7. ✅ Checklist de Fechamento (Definition of Done - DoD)

- [ ] Suíte de testes automatizados verde.
- [ ] Nenhuma credencial ou dado sensível exposto.
- [ ] Documentação viva (OpenAPI / Backlog) sincronizada.
```

---

## 3. `ADR.md` — Registro de Decisão Arquitetural

### 🔍 Diagnóstico do Uso Real

- **O que funcionou:** As 4 ADRs do projeto (Java 17, Maven, Spring Boot/WAR e Hexagonal/ACL) blindaram o projeto contra mudanças de rumo arbitrárias sugeridas por IAs.
- **Oportunidade de Evolução:** Incluir **Gatilhos de Reavaliação (_Review Triggers_)** para formalizar em que momento futuro uma decisão deve ser revista (ex: volume de dados, custo de nuvem, mudança de framework).

### 📐 Anatomia Proposta do Template (`docs/templates/ADR.md`)

```markdown
# ADR-[NUM]: [Título da Decisão Arquitetural]

> **Status:** `Proposto` | `Aprovado` | `Substituído por ADR-XXX` | `Deprecado`
> **Data:** AAAA-MM-DD &bull; **Decisores:** [Nomes]
> **Contexto Técnico:** [ex: Persistência, Mensageria, Runtime]

---

## 1. 📌 Contexto e Forças do Problema

Qual desafio ou restrição originou esta decisão? Quais forças influenciam a escolha (tempo, custo, equipe, escala, segurança)?

## 2. 🎯 Decisão

> **Decidimos adotar [Solução X] para [Problema Y], utilizando [Abordagem Z].**

Detalhes técnicos da implementação.

## 3. ⚖️ Consequências & Trade-offs

- **Positivas (Ganhos):** O que ganhamos de imediato?
- **Negativas (Custos/Restrições):** O que abrimos mão ou que complexidade aceitamos?
- **Riscos Mitigados:** Quais riscos foram eliminados por essa escolha?

## 4. 🔍 Alternativas Avaliadas e Rejeitadas

| Alternativa | Prós   | Contras   | Motivo da Rejeição       |
| :---------- | :----- | :-------- | :----------------------- |
| **Opção A** | [Prós] | [Contras] | [Por que foi descartada] |

## 5. 🔄 Gatilhos de Reavaliação (Review Triggers)

Sob quais condições esta decisão deve ser reconsiderada?

- Ex: _Se o volume de emissões diárias ultrapassar 50.000 requisições._
- Ex: _Se a compatibilidade com o sistema legado deixar de ser necessária._
```

---

## 4. `BACKLOG.md` — Planejamento Ágil Docs-as-Code

### 🔍 Diagnóstico do Uso Real

- **O que funcionou:** A separação em Épicos numerados (0 a 9) com identificadores únicos (`SET-01`, `CORE-01`, `OPS-01`) facilitou rastrear o progresso e associar aos commits.
- **Oportunidade de Evolução:** Criar um padrão de taxonomia de IDs explícito e um resumo executivo com indicadores de maturidade do projeto.

### 📐 Anatomia Proposta do Template (`docs/templates/BACKLOG.md`)

```markdown
# Backlog do Projeto — [Nome do Projeto]

> **Docs-as-Code — Planejamento Ágil, Especificações & Roadmap**
> Repositório: [`[usuario]/[nome-repositorio]`](https://github.com/[usuario]/[nome-repositorio])
> Versão Atual: `v0.1.0` &bull; **Última Atualização:** AAAA-MM-DD

---

## 📌 Sumário Executivo & Maturidade

Breve visão sobre o estado atual do software (ex: MVP em homologação, protótipo, produção).

### 📊 Indicadores de Progresso

- **Fase Atual:** [ex: MVP Concluído / Preparação para Homologação]
- **Épicos Entregues:** [X] de [Y] ([Z]%)

---

## 🚦 Legenda de Status e Prioridades

- **Status:** 🟢 `[Concluído]` &bull; 🟡 `[Em Andamento]` &bull; ⚪ `[A Fazer]` &bull; 💡 `[Ideação / Em Análise]`
- **Prioridade:** `P0` (Bloqueante) &bull; `P1` (Alta) &bull; `P2` (Média) &bull; `P3` (Baixa)

---

## 🎯 Épicos e Fatias Verticais

### Épico 0: Fundação & Higiene Inicial do Repositório — 🟢 100% Concluído

| Status |    ID    | Prioridade | Item                      | Descrição / Critérios de Aceite                              |
| :----: | :------: | :--------: | :------------------------ | :----------------------------------------------------------- |
|   🟢   | `SET-01` |    `P0`    | **Segurança & Gitignore** | Blindagem contra credenciais e padronização `.editorconfig`. |

### Épico 1: [Nome do Épico Central] — ⚪ A Fazer

| Status |    ID    | Prioridade | Item                   | Descrição / Critérios de Aceite         |
| :----: | :------: | :--------: | :--------------------- | :-------------------------------------- |
|   ⚪   | `MOD-01` |    `P0`    | **[Título da Tarefa]** | [Descrição clara e critério de aceite]. |

---

## 💡 Banco de Ideias & Melhorias Futuras

Propostas técnicas e melhorias para iterações posteriores:

1. **[Ideia 1]:** [Descrição e impacto potencial].
```

---

## 5. `ANALYSIS-AI.md` — Novo Template: Auditoria & Revisão Multi-IA

### 🔍 Por que este template é indispensável?

Durante o projeto geramos as análises `001` a `004` (Claude, Antigravity, ChatGPT). No entanto, cada uma utilizou uma formatação diferente. Ter um template padrão para **Revisões de IA** permite que você colete auditorias de qualquer modelo externo (Claude 3.5, GPT-4o, Gemini 1.5 Pro, DeepSeek) sob a mesma régua métrica e com um ciclo de fechamento documentado.

### 📐 Anatomia Proposta do Template (`docs/templates/ANALYSIS-AI.md`)

```markdown
# Análise Técnica do `[Nome do Projeto]` por [Nome da IA] ([Data])

> **Avaliador:** [ex: Claude 3.5 Sonnet / GPT-4o / Antigravity]
> **Escopo da Análise:** [ex: Código-fonte, Schemas, Segurança, Resiliência]
> **Versão / Commit Avaliado:** `[hash-do-commit]`
> **Status:** `Aberta` | `Em Resolução` | `Concluída`

---

## 1. 📊 Avaliação Geral de Maturidade

| Dimensão                       | Nota (1-10) | Diagnóstico Resumido                                     |
| :----------------------------- | :---------: | :------------------------------------------------------- |
| **Arquitetura & Design**       |      -      | [Separação de camadas, desacoplamento, SOLID]            |
| **Segurança & Dados**          |      -      | [Proteção de credenciais, injeção, isolamento de chaves] |
| **Resiliência & Concorrência** |      -      | [Timeouts, retries, locks, integridade de threads]       |
| **Qualidade & Testes**         |      -      | [Cobertura, cenários de borda, legibilidade dos testes]  |
| **Documentação & Contratos**   |      -      | [Aderência das specs e clareza das instruções]           |

---

## 2. 🔍 Matriz de Achados e Riscos

### 🔴 Alto Impacto / Crítico

_Problemas que causam parada em produção, falhas de segurança ou violação de integridade._

- **`[ACH-01]`: [Título do Achado]**
  - **Arquivo / Trecho:** `[caminho/arquivo.ext:L10-L20]`
  - **Problema Identificado:** [Explicação técnica do risco]
  - **Ação Recomendada:** [Sugestão cirúrgica de correção]

### 🟠 Médio Impacto

_Gargalos de performance, fragilidades no tratamento de erros ou inconsistências de contrato._

- **`[ACH-02]`: [Título do Achado]**
  - **Arquivo:** `[caminho]`
  - **Problema / Ação:** [Descrição e recomendação]

### 🟡 Baixo Impacto / Melhorias de Código

_Refatorações cosméticas, limpeza de código morto ou otimizações menores._

---

## 3. 📋 Plano de Resolução Sugerido

1. [Passo 1 com prioridade máxima]
2. [Passo 2 com prioridade média]
3. [Passo 3 de polimento documental]

---

## 4. ✅ Acompanhamento de Conclusão (Para o arquivo `-concluded.md`)

|    ID    | Descrição             |    Status    | Commit da Solução |
| :------: | :-------------------- | :----------: | :---------------- |
| `ACH-01` | [Descrição do Achado] | 🟢 Concluído | `[hash]`          |
| `ACH-02` | [Descrição do Achado] | 🟢 Concluído | `[hash]`          |
```

---
