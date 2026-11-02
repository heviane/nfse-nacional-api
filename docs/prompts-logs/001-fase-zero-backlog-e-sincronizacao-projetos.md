# Como funciona a sincronização com o Project 14? Ela é automática?

A resposta direta é: **Ela não é automática por "mágica" do Git, ela é "Assistida por IA" (via GitHub CLI `gh`).**

## Como foi feito no `heviane/meta-hub`?

Quando implementamos a tarefa `GOV-04` há pouco, fui **eu (agente) executando comandos do `gh project item-create` via terminal** após sua aprovação explícita. Não existe um robô rodando no Git que lê o arquivo Markdown e atualiza o quadro sozinho.

### O que o `AGENTS.md` diz sobre isso?

No [`E:\Projects\GitHub\AGENTS.md`](file:///E:/Projects/GitHub/AGENTS.md#L34), está definido:

> _"Quadro Executivo: As tarefas priorizadas são acompanhadas no GitHub Project #14 (AI Engineering Hub)."_

E no próprio [`meta-hub/backlog.md`](file:///e:/Projects/GitHub/heviane/heviane/meta-hub/backlog.md#L78) existe uma ideia futura:

> _"Automação GitHub Actions com GitHub CLI (gh): Criar um workflow simples que lê seções deste backlog.md e gera/atualiza Issues automaticamente no repositório de destino e no Project 14."_

### A regra dos Flight Levels (Por que nem tudo vai para o Project 14?)

- **Flight Level 3 (Project 14 - Visão Estratégica):** Ficam os grandes épicos e entregas de alto nível (ex: _"Lançamento da NFS-e Nacional API"_ ou _"Padronização @heviane-labs"_).
- **Flight Level 1 (Backlog Local do Repositório):** Ficam as microtarefas técnicas do dia a dia (ex: _"Ajustar .gitignore"_, _"Configurar plugin XSD"_). Colocar cada microtarefa local no Project 14 sobrecarregaria o quadro executivo.

Sempre que você quiser que um épico ou tarefa deste repositório seja cadastrado ou atualizado no **Project 14**, basta me dizer: _"cadastre o Épico X no Project 14"_ e eu faço a sincronização imediatamente via CLI!

---
