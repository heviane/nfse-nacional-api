# Glossário — Engenharia de Software & Práticas de IA

> **Documento Vivo de Aprendizado, Alinhamento Conceitual & Engenharia**  
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)  
> *Este glossário reúne termos fundamentais de gestão ágil, engenharia de software assistida por inteligência artificial e padrões arquiteturais.*

---

## 📌 Índice Temático

1. [Gestão de Projetos & Ciclo de Vida (SDLC)](#1-gestão-de-projetos--ciclo-de-vida-sdlc)
2. [Engenharia de Software Assistida por IA](#2-engenharia-de-software-assistida-por-ia)
3. [Qualidade, Testes & Padrões de Código](#3-qualidade-testes--padrões-de-código)

---

## 1. Gestão de Projetos & Ciclo de Vida (SDLC)

### Milestone (Marco de Entrega)
Um **Milestone** é um ponto de referência estratégico que agrupa um conjunto de entregas em direção a uma versão ou objetivo maior (por exemplo: `v0.1.0 - Incepção & Fundação` ou `v1.0.0 - MVP`).  
* **Função prática:** Diferente de uma tarefa isolada (*Issue*), o Milestone atua como uma meta com barra de progresso visual percentual. Conforme as issues e pull requests vinculados são concluídos, o progresso avança até atingir 100%.

### Épico (Epic)
Uma iniciativa ou grande bloco de trabalho que é amplo demais para ser implementado em uma única tarefa. No gerenciamento moderno, o épico é decomposto em histórias de usuário ou fatias verticais menores (ex: *Épico 1: Arquitetura Técnica & Decisões Fundamentais*).

### Walking Skeleton (Esqueleto Funcional)
A menor implementação ponta a ponta (*end-to-end*) possível da arquitetura de um sistema, construída no início do projeto para interligar todas as camadas principais (banco, backend, rotas e deploy). Não possui regras de negócio completas, mas prova que a infraestrutura, o build e a comunicação básica funcionam perfeitamente.

### Docs-as-Code (Documentação como Código)
A prática de gerenciar documentação técnica (backlog, arquitetura, manuais, especificações) utilizando as mesmas ferramentas e processos do código-fonte: arquivos em Markdown (`.md`), controle de versão com Git, revisões via Pull Request e publicação automatizada.

### WIP (Work in Progress)
A quantidade de tarefas que foram iniciadas, mas ainda não estão finalizadas. Um dos princípios centrais do Kanban é limitar o WIP para evitar gargalos, alternância excessiva de contexto (*context switching*) e entregas incompletas.

---

## 2. Engenharia de Software Assistida por IA

### SDD (Spec-Driven Development)
*Desenvolvimento Orientado a Especificações.* Abordagem em que nenhuma linha de funcionalidade é codificada antes de uma especificação clara (`SPEC.md`) descrevendo os requisitos funcionais, critérios de aceite em linguagem ubíqua (Gherkin) e cenários de borda. Isso evita que o modelo de IA deduza ou invente regras de negócio.

### ADR (Architecture Decision Record)
*Registro de Decisão Arquitetural.* Documento curto e padronizado em formato Markdown que captura uma decisão técnica importante, o contexto e as forças que a motivaram, as alternativas avaliadas e os *trade-offs* (consequências positivas e negativas) aceitos pelo time.

### Pair Programming com IA
Metodologia de trabalho colaborativo em que a desenvolvedora humana atua como condutora estratégica / revisora (*Driver/Navigator*) e o agente de inteligência artificial atua como copiloto operacional avançado, sugerindo código, propondo testes e aplicando padrões de forma rigorosa sob supervisão contínua.

### Minimal Diff (Edição Cirúrgica)
Princípio operacional de IA que prega a alteração estrita e pontual das linhas necessárias para resolver um problema, preservando formatações pré-existentes, comentários e estrutura de arquivos sem reescritas desnecessárias.

### Context Window (Janela de Contexto)
O limite máximo de dados (tokens de texto, código e instruções) que um modelo de linguagem (LLM) consegue ler e processar simultaneamente em uma única interação. Técnicas como sumarização e arquivos compactos evitam o esgotamento desse espaço.

---

## 3. Qualidade, Testes & Padrões de Código

### TDD (Test-Driven Development)
*Desenvolvimento Guiado por Testes.* Prática em que o desenvolvedor escreve primeiro o teste automatizado que falha (*Red*), implementa a quantidade mínima de código para fazê-lo passar (*Green*) e, por fim, melhora a estrutura do código sem alterar seu comportamento externo (*Refactor*).

### Conventional Commits
Uma convenção padronizada para escrever mensagens de commit no Git com prefixos semânticos legíveis tanto por humanos quanto por ferramentas automatizadas (ex: `feat:` para funcionalidades, `fix:` para correções, `docs:` para documentação, `chore:` para tarefas de infraestrutura).

### Arquitetura Hexagonal (Ports & Adapters)
Padrão arquitetural que visa isolar a lógica de negócio central (Core de Domínio) de tecnologias externas (frameworks web, bancos de dados, APIs de terceiros). A comunicação entre o Core e o mundo exterior é intermediada por contratos (*Portas*) e implementações técnicas (*Adaptadores*).

### ACL (Anti-Corruption Layer / Camada Anticorrupção)
Padrão de integração que atua como um tradutor intermediário entre dois sistemas ou subsistemas com modelos de dados diferentes. Ele converte requisições legadas para o formato limpo do novo domínio (e vice-versa), garantindo que particularidades ou débitos técnicos de clientes legados não contaminem o Core da aplicação.
