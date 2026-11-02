# Fase Zero

Analisar, verificar e organizar o repositório antes de iniciar de fato o projeto em si.

Na engenharia de software moderna, esse momento que precede os requisitos formais é conhecido como **Fase Zero**, **Higienização do Repositório (*Repository Hygiene*)** ou **Setup de Incepção**.

Começar organizando a "casa" antes de mergulhar na engenharia de requisitos traz vantagens críticas — especialmente no desenvolvimento assistido por IA:

---

## 💡 Por que essa organização prévia é tão vantajosa?

1. **Elimina o "Débito do Template" (*Template Debt*):**
   * Como o repositório acabou de nascer a partir do `project-starter-kit`, ele herda arquivos com textos genéricos, instruções de modelo e placeholders (`[Nome do Projeto]`). Limpar e customizar isso agora evita carregar lixo e ruído ao longo do projeto.

2. **Evita Alucinações da IA (Context Window Limpo):**
   * Agentes de IA (como eu, Copilot ou Claude) leem os arquivos do repositório para entender o contexto. Se houver templates genéricos ou arquivos conflitantes, a IA pode se confundir sobre a stack real e as regras do projeto.

3. **Segurança Crítica desde o "Minuto Zero" (Inviolável para NFS-e):**
   * Como estamos criando um projeto que lidará com **Certificados Digitais A1** em um **repositório público no GitHub**, a regra número um de segurança é garantir que o `.gitignore` bloqueie sumariamente qualquer extensão de certificado (`*.pfx`, `*.p12`, `*.jks`, `*.key`). Deixar isso para depois gera risco real de vazamento acidental de chaves privadas de clientes.

4. **Clareza de Propósito:**
   * Um `README.md` inicial bem redigido e um `AGENTS.md` adaptado para Java já posicionam o projeto com clareza profissional para qualquer pessoa (ou cliente) que visitar o repositório no GitHub.

---

## 📋 Checklist Recomendado para a "Fase Zero" (Organização Prévia)

Antes de passarmos à Engenharia de Requisitos (`SPEC-001`), recomendo fecharmos os seguintes 4 pontos de higiene:

| Etapa | Ação | Por que fazer agora |
| :--- | :--- | :--- |
| **1. Segurança no `.gitignore`** | Incluir regras para Java/Maven/Gradle e blindagem de certificados (`*.pfx`, `*.p12`, `*.pem`, `*.jks`). | Evitar vazamento de chaves privadas e arquivos de build no repositório público. |
| **2. Padronização `.editorconfig`** | Configurar espaçamento e UTF-8 para arquivos `.java`, `.xml` e `.xsd`. | Garantir que o formato de arquivos XML fiscais não quebre por encode incorreto. |
| **3. README.md Inicial do Projeto** | Substituir o README do template pelo README oficial do `nfse-nacional-api`. | Apresentar a visão e a identidade do produto no GitHub. |
| **4. Ajuste do `AGENTS.md` Local** | Definir as regras de stack (Java) e o fluxo SDD específico para este repositório. | Garantir que qualquer IA que opere aqui saiba que a stack é Java e siga a governança. |

---

Percebi inclusive que você já organizou com maestria materiais de referência técnica na pasta [`docs/nfse-nacional/`](file:///E:/Projects/GitHub/heviane/nfse-nacional-api/docs/nfse-nacional/) e um rascunho de [backlog.md](file:///E:/Projects/GitHub/heviane/nfse-nacional-api/docs/backlog.md). Isso mostra uma excelente maturidade arquitetural!

Se concordar, podemos começar executando o **Item 1 e 2 (Blindagem do `.gitignore` e `.editorconfig` para Java e Certificados)**. O que acha
