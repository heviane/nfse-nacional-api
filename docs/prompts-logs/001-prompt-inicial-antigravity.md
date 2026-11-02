# Prompt inicial: Resposta Antigravity

Seu rascunho já está **muito bom**! A maioria dos desenvolvedores iniciantes comete o erro de escrever prompts vagos como *"Faça uma API para NFS-e em Java"*, enquanto você já forneceu:
* Contexto de negócio claro (integração direta com o Ambiente Nacional da NFS-e).
* Papel da API (middleware entre ERP e Sefin Nacional/ADN).
* Regras técnicas reais (assinatura A1, validação de schema, reaproveitamento da lib existente).
* Restrição de segurança e governança (código genérico, sem dados sensíveis do cliente).

Abaixo estão o feedback detalhado e a versão aprimorada aplicando técnicas modernas de **Engenharia de Prompts** e **Spec-Driven Development (SDD)**.

---

## 🔍 Feedback e Sugestões de Melhoria

### 1. Definir o Papel da IA (*Role Prompting*)
Quando você diz à IA qual é a "especialidade" dela, ela ativa o vocabulário e os padrões arquiteturais corretos.  
* **Melhoria:** Definir a persona como *Arquiteto de Software Sênior especialista em integrações fiscais brasileiras (SPED/NFS-e Nacional) e ecossistema Java*.

### 2. Conectar com o padrão que você já definiu (SDD & Starter Kit)
Como você acabou de oficializar na **ADR-001** e no **`project-starter-kit`**, um novo projeto deve nascer orientado a especificações.  
* **Melhoria:** Pedir explicitamente a criação de uma **`SPEC.md`** e estrutura base antes de gerar código.

### 3. Especificar os termos do Padrão Nacional da NFS-e
A integração com o Sefin Nacional tem particularidades técnicas que a IA precisa considerar desde o minuto zero:
* Autenticação mútua via **mTLS** (usando o certificado A1).
* O conceito de **DPS (Declaração de Prestação de Serviços)** e geração da **NFS-e**.
* A transição entre os endpoints do **Sefin Nacional** e do **ADN (Ambiente de Dados Nacional)**.
* **Melhoria:** Citar esses termos para que a IA não confunda com o modelo antigo de prefeituras (ABRASF/SOAP legado).

### 4. Estruturar a Saída em Etapas Claras (*Structured Output*)
Pedir tudo em um bloco só pode fazer a IA resumir demais o README ou a documentação. Ao dividir a resposta em seções numeradas e com escopo delimitado, ela entrega com muito mais profundidade.

---

## 🚀 Versão Otimizada do Prompt (Pronta para Usar)

Você pode copiar e utilizar o prompt estruturado abaixo:

```markdown
Atue como um Arquiteto de Software Sênior especialista no ecossistema Java e em integrações fiscais brasileiras (SPED e Padrão Nacional de NFS-e).

Estou iniciando um novo projeto open-source / white-label para atuar como middleware de integração direta com o Ambiente Nacional da NFS-e (Sefin Nacional e ADN - Ambiente de Dados Nacional).

### 🎯 Contexto e Arquitetura do Sistema
* **Papel da Solução:** API REST intermediária entre ERPs legados e os WebServices/APIs do Ambiente Nacional da NFS-e.
* **Fluxo Operacional:**
  1. Receber payload do ERP chamador (JSON ou XML da DPS - Declaração de Prestação de Serviços).
  2. Validar payload contra os Schemas Oficiais (XSD/JSON Schema do Padrão Nacional).
  3. Assinar digitalmente o documento usando Certificado Digital ICP-Brasil A1 (PKCS#12).
  4. Transmitir via mTLS (TLS Mútuo) aos servidores do Sefin Nacional / ADN.
  5. Processar o retorno (síncrono/assíncrono), tratar erros ou protocolos e devolver a resposta estruturada ao ERP.
* **Serviços Contemplados no MVP:**
  * Emissão de NFS-e / Envio de DPS.
  * Consulta de situação / lote e eventos da nota.
  * Cancelamento e Substituição de NFS-e.

### ⚙️ Restrições e Ambiente Técnico
* **Linguagem & Runtime:** Java (compatível com Tomcat 10+ / Servlet Containers).
* **Biblioteca Existente:** Possuo a biblioteca de assinatura XML em Java (https://github.com/heviane/assinador-xml-java), atualmente utilizada em produção para emissão municipal legada (SIL Tecnologia). Precisamos avaliar e adaptar sua compatibilidade com o padrão XML/DPS nacional.
* **Hospedagem Alvo do Primeiro Cliente:** Servidor Cloud gerenciado (BrasilCloud) com Tomcat.
* **Governança & Privacidade:** O repositório será público no GitHub, portanto deve ser estritamente genérico (arquitetura agnóstica a clientes, sem dados sensíveis, credenciais ou nomes de empresas hardcoded).

### 📋 O que você deve entregar agora:

1. **Naming & Identidade:**
   * 3 sugestões de nomes concisos e profissionais para o repositório (estilo open-source/kebab-case, ex: `nfse-nacional-api`, `sefin-bridge-java`).
   * Uma descrição curta (one-liner) para o header do repositório no GitHub.

2. **Visão da Arquitetura & Compatibilidade:**
   * Breve análise dos requisitos de assinatura e transmissão do Padrão Nacional (XML/DPS com RSA-SHA256, mTLS e endpoints REST/SOAP) em relação ao modelo legado municipal.

3. **Estrutura de Documentação Técnica Inicial (Spec-Driven Development):**
   * Esboço da estrutura de documentação sugerida para a pasta `docs/` (ex: `SPEC.md`, guia de certificados A1, mapeamento de endpoints e schemas XSD).

4. **README.md Inicial (Modelo Profissional):**
   * Elabore a proposta de um `README.md` completo, incluindo:
     * Badges de tecnologia e status.
     * Visão geral da proposta de valor da API.
     * Diagrama de arquitetura simplificado em Mermaid (ERP <-> Nossa API <-> Sefin Nacional).
     * Pré-requisitos (Java, Certificado A1, Schemas).
     * Guia rápido de configuração e roadmap do projeto.
```

---

## 💡 Por que este prompt funciona melhor?

1. **Evita alucinação:** Deixa explícito o padrão nacional (DPS, mTLS, Sefin/ADN), impedindo que a IA gere código antigo de prefeituras (que usam outros formatos de XML e endpoints SOAP obsoletos).
2. **Entrega profissional:** O README gerado já virá com diagrama Mermaid, badges e arquitetura pronta para ser comitada.
3. **Respeita o seu fluxo:** Prepara o terreno para o **`SPEC.md`** e encaixa diretamente na governança de engenharia que você já estruturou!
