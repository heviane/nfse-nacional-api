# Plano de ação para atualizar o repositório [github.com/heviane/project-starter-kit](https://github.com/heviane/project-starter-kit)

📂 Repositório local: `E:\Projects\GitHub\heviane\project-starter-kit`.

---

## 📊 Diagnóstico: O que o `project-starter-kit` tem hoje vs. O que criamos

| Componente                        | Estado Atual no `project-starter-kit`                     | Novo Padrão Validado no `nfse-nacional-api`                                                                                                  |
| :-------------------------------- | :-------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------- |
| **Templates** (`docs/templates/`) | Versões preliminares de `SPEC.md`, `ADR.md`, `BACKLOG.md` | **Templates refinados** com fatias verticais, modos de falha, DoD, gatilhos de reavaliação + novos templates `ANALYSIS-AI.md` e `AGENTS.md`. |
| **`AGENTS.md` (Raiz)**            | Genérico e focado apenas em princípios básicos            | **Estrutura em duas camadas:** Núcleo Universal (regras invioláveis) + Seção plugável de domínio/stack.                                      |
| **Ponte Multi-IA**                | Apenas `.github/copilot-instructions.md`                  | Suporte unificado via `AGENTS.md` referenciado por Copilot, Cursor e Windsurf.                                                               |
| **Mecanismo de Bootstrap**        | Inexistente (cópia manual do template)                    | **Scripts `scripts/bootstrap.ps1` e `bootstrap.sh`** para inicialização interativa, hidratação automática e autolimpeza.                     |
| **Playbook Metodológico**         | Rascunho de ideias em `docs/plan.md`                      | **Playbook formal em 5 fases** (_The AI-Aided SWE Playbook_) com papéis, padrões de prompt e gestão de contexto.                             |
| **Segurança & CI**                | Sem workflows de CI nem detecção de segredos              | `.gitleaks.toml`, `.gitleaksignore` e `.github/workflows/ci.yml` com validação de segredos.                                                  |

---

## 🛠️ Plano de Ação para Atualizar o `project-starter-kit`

Como a pasta local `e:\Projects\GitHub\heviane\project-starter-kit` já existe na máquina, podemos atualizá-la de forma segura e incremental:

```text
E:\Projects\GitHub\heviane\project-starter-kit/
├── docs/
│   ├── templates/            ◄── 1. Sincronizar os 6 templates refinados
│   │   ├── AGENTS.md
│   │   ├── SPEC.md
│   │   ├── ADR.md
│   │   ├── BACKLOG.md
│   │   ├── ANALYSIS-AI.md
│   │   └── README-PROJECT.md
│   ├── playbook.md           ◄── 2. Adicionar o Playbook de Engenharia com IA
│   └── plan.md               ◄── 3. Atualizar marcando o plano como executado
├── scripts/
│   ├── bootstrap.ps1         ◄── 4. Criar o script interativo de hidratação (Windows)
│   └── bootstrap.sh          ◄── 5. Criar o script interativo de hidratação (Linux/macOS)
├── .github/
│   ├── workflows/ci.yml      ◄── 6. Adicionar workflow com Gitleaks e Lint
│   └── copilot-instructions  ◄── 7. Atualizar apontando para o AGENTS.md
├── AGENTS.md                 ◄── 8. Atualizar com o novo padrão em duas camadas
├── .gitleaks.toml            ◄── 9. Adicionar regras de segurança preventiva
└── README.md                 ◄── 10. Atualizar a apresentação oficial do template
```

---

## 🚦
