# Instruções do GitHub Copilot

Este repositório segue os padrões modernos de **Engenharia de Software Assistida por IA**, **Spec-Driven Development (SDD)** e **Docs-as-Code**.

## Diretrizes Gerais
1. **Regras de Ouro:** Antes de iniciar qualquer desenvolvimento ou refatoração, leia e obedeça rigorosamente ao arquivo [`AGENTS.md`](../AGENTS.md) na raiz do projeto.
2. **Especificações:** Para implementar novas funcionalidades, consulte a especificação correspondente em `docs/specs/`. Não adivinhe requisitos nem deduza regras fiscais.
3. **Stack Tecnológica & Padrão de Código:**
   - Stack Oficial: **Java 17+**, **Maven**, **Spring Boot** (com empacotamento WAR para servlet container Tomcat).
   - Priorize legibilidade, tipagem estrita, Clean Architecture e isolamento completo do **Core Fiscal** (regras de DPS, validação XSD e assinatura digital) em relação aos adaptadores web e controladores REST.
   - Trate erros e exceções fiscais de forma explícita com logs estruturados, sem silenciar ou mascarar exceções.
   - Siga rigorosamente o padrão [Conventional Commits](https://www.conventionalcommits.org/).
4. **Segurança & Dados Fiscais (Crítico):**
   - **NUNCA** versionar, logar ou salvar no disco chaves privadas, certificados digitais ICP-Brasil A1 (`*.pfx`, `*.p12`, `*.jks`, `*.pem`, `*.key`) ou senhas em texto puro.
   - Certificados e senhas devem ser carregados estritamente em memória (`KeyStore`) a partir de streams ou variáveis de ambiente.
