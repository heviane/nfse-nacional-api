# 📋 Resumo das Modificações Realizadas no `project-starter-kit`

1. **Biblioteca de Templates (`docs/templates/`):**
   - **[SPEC.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/SPEC.md):** Atualizado com fatias verticais (_Vertical Slices_), casos de borda (_Failure Modes_) e Definition of Done (DoD).
   - **[ADR.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/ADR.md):** Atualizado com tabela de alternativas e gatilhos de reavaliação (_Review Triggers_).
   - **[BACKLOG.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/BACKLOG.md):** Atualizado com taxonomia de IDs (`SET-`, `CORE-`, `API-`, `OPS-`).
   - **[README-PROJECT.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/README-PROJECT.md):** Template limpo e moderno para o README do novo projeto.
   - **[AGENTS.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/AGENTS.md) (Novo):** Template universal com camada de regras invioláveis e seção plugável de regras de domínio.
   - **[ANALYSIS-AI.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/templates/ANALYSIS-AI.md) (Novo):** Template padronizado para auditorias e revisões multi-IA.

2. **Playbook Metodológico de Engenharia com IA:**
   - **[docs/playbook-engenharia-ia.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/playbook-engenharia-ia.md) (Novo):** Guia completo em 5 fases (_The AI-Aided SWE Playbook_), com modelo Tech Lead + Pair Programmer, boas práticas de prompts e gestão de contexto.
   - **[docs/plan.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/docs/plan.md):** Atualizado com status de implementado.

3. **Scripts de Bootstrapping Inteligente (`/scripts`):**
   - **[scripts/bootstrap.ps1](file:///e:/Projects/GitHub/heviane/project-starter-kit/scripts/bootstrap.ps1) (Windows / PowerShell):** Coleta interativa de metadados, hidratação automática do `README.md`, `AGENTS.md` e `docs/backlog.md`, com opção de autolimpeza ao final.
   - **[scripts/bootstrap.sh](file:///e:/Projects/GitHub/heviane/project-starter-kit/scripts/bootstrap.sh) (Linux / macOS / Bash):** Script equivalente em Bash.

4. **Governança, Segurança & CI:**
   - **[AGENTS.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/AGENTS.md) (Raiz):** Atualizado com o padrão em duas camadas.
   - **[.gitignore](file:///e:/Projects/GitHub/heviane/project-starter-kit/.gitignore):** Blindagem multi-stack (Java, Node, Python, .NET, segredos e certificados).
   - **[.gitleaks.toml](file:///e:/Projects/GitHub/heviane/project-starter-kit/.gitleaks.toml):** Regras de detecção de segredos e credenciais.
   - **[.github/workflows/ci.yml](file:///e:/Projects/GitHub/heviane/project-starter-kit/.github/workflows/ci.yml):** Pipeline de CI com verificação preventiva via Gitleaks.
   - **[README.md](file:///e:/Projects/GitHub/heviane/project-starter-kit/README.md) (Raiz):** Reposicionamento oficial do repositório como o Starter Kit de Engenharia com IA.

---
