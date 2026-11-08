# Estrutura de Pastas & Organização de Código

> **Docs-as-Code — Guia de Arquitetura e Estrutura de Código**  
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)  
> Referência Técnica: [`ADR-004: Padrão Arquitetural Hexagonal com ACL`](../adrs/0004-padrao-arquitetural-hexagonal-acl.md) &bull; [`ADR-003: Framework Web Spring Boot`](../adrs/0003-framework-web-runtime-spring-boot.md)  
> Versão: `1.0.0`

---

## 1. 📂 Visão Geral da Raiz do Repositório (Padrão Maven + Governança)

```text
nfse-nacional-api/
├── .github/                  # Templates de issues, PRs e instruções do Copilot
├── .vscode/                  # Extensões recomendadas e settings de formatação
├── docs/                     # Governança Docs-as-Code (backlog, adrs, specs, clients, glossario)
├── .editorconfig             # Padrão universal de indentação e charset
├── .gitignore                # Blindagem contra certificados e pastas de compilação
├── AGENTS.md                 # Contrato de atuação dos agentes de IA
├── README.md                 # Visão geral e badges do projeto
├── pom.xml                   # Configuração mestre do Maven, plugins e dependências
├── mvnw / mvnw.cmd           # Maven Wrapper (permite compilar sem ter Maven instalado)
├── .mvn/                     # Configurações internas do Maven Wrapper
└── src/                      # Código-fonte e recursos da aplicação
```

---

## 2. 🏛️ Estrutura do Código-Fonte (`src/`) — Arquitetura Hexagonal

A pasta `src/` segue a convenção universal do ecossistema Java, subdividida em `main` (código de produção) e `test` (testes automatizados):

```text
src/
├── main/
│   ├── java/
│   │   └── br/com/heviane/nfse/
│   │       │
│   │       ├── NfseApplication.java          # Classe principal (Spring Boot + ServletInitializer para Tomcat)
│   │       │
│   │       ├── core/                         # 🛡️ Hexágono Interno (Domínio Puro & Regras Fiscais)
│   │       │   ├── domain/                   # Entidades ricas (Dps, Nfse, CertificadoDigital)
│   │       │   ├── ports/
│   │       │   │   ├── in/                   # Interfaces dos Casos de Uso (ex: EmitirDpsUseCase)
│   │       │   │   └── out/                  # Interfaces de Saída (ex: AssinadorPort, TransmissorSefinPort)
│   │       │   └── services/                 # Orquestração pura das regras fiscais (sem dependência web)
│   │       │
│   │       └── adapters/                     # 🔌 Adaptadores Externos (Entrada e Saída)
│   │           ├── in/
│   │           │   ├── legacy/               # 🔄 Camada Anticorrupção (ACL): Controlador SOAP do GeneXus 18
│   │           │   ├── rest/                 # ⚡ Controladores REST JSON modernos (futuros clientes)
│   │           │   └── security/             # 🛡️ Resolução de certificados A1 e segurança de transporte
│   │           └── out/
│   │               ├── signer/               # 🔐 Assinatura XML A1 em memória via JCA nativo
│   │               └── sefin/                # 🏛️ Cliente HTTP mTLS para o Sefin Nacional / ADN
│   │                   └── dto/              # 📦 DTOs de integração e mapeamento JSON do Sefin
│   │
│   ├── config/                               # ⚙️ Configurações Spring, propriedades e DI Hexagonal
│   └── resources/
│       ├── application.properties            # Propriedades padrão (porta, encoding, logs)
│       └── schemas/                          # Schemas oficiais da Receita (.xsd) da NFS-e Nacional
│
└── test/
    ├── java/
    │   └── br/com/heviane/nfse/
    │       ├── NfseApplicationTests.java     # Teste de fumaça (valida subida do contexto)
    │       └── core/                         # Testes unitários do Core Fiscal (TDD)
    └── resources/                            # Amostras reais de XML de teste para testes automatizados
```

---

## 3. 🛡️ Regra Fundamental de Dependências (O Invariante Hexagonal)

A integridade da arquitetura baseia-se em uma regra inegociável de direção de dependências:

> **As dependências sempre apontam para dentro.**
>
> - Os `adapters` conhecem e dependem do `core`.
> - O `core` **NUNCA** conhece nem importa classes de `adapters` ou bibliotecas de terceiros (Spring Boot, Jackson, Apache CXF, etc.). Ele é construído em **Java 17 puro**.

```text
[ Adaptador SOAP GeneXus ] ───┐
                              ▼
[ Adaptador REST JSON    ] ───► [ Core Fiscal (Domain & Use Cases) ] ◄─── [ Adaptador mTLS Sefin ]
                              ▲
[ Adaptador de Testes    ] ───┘
```

---

## 4. 📋 Matriz de Responsabilidades por Pacote

| Pacote                     | O que DEVE conter                                                                                          | O que NUNCA deve conter                                                                                |
| :------------------------- | :--------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------- |
| **`core/domain/`**         | Classes de negócio ricas, _Records_, regras de integridade fiscal e validação estrutural da DPS.           | Anotações de frameworks (`@Service`, `@Component`), anotações de persistência ou de serialização JSON. |
| **`core/ports/in/`**       | Interfaces dos Casos de Uso que a aplicação expõe (ex: `EmitirDpsUseCase`, `ConsultarNfseUseCase`).        | Implementações concretas ou dependências de transporte.                                                |
| **`core/ports/out/`**      | Interfaces de serviços externos necessários ao Core (ex: `TransmissorSefinPort`, `AssinadorDigitalPort`).  | Detalhes de como a conexão HTTP ou criptografia em disco é executada.                                  |
| **`core/services/`**       | Implementação dos casos de uso, orquestrando validação de schema, assinatura e transmissão.                | Manipulação direta de `HttpServletRequest`, XML SOAP de clientes ou DTOs de tela.                      |
| **`adapters/in/legacy/`**  | Controlador SOAP (`@PostMapping`), DTOs do GeneXus 18 e mappers de tradução da Camada Anticorrupção (ACL). | Lógica de negócio da Receita Federal ou cálculos fiscais federais.                                     |
| **`adapters/in/rest/`**    | Controladores REST (`@RestController`), anotações OpenAPI/Swagger e DTOs JSON modernos.                    | Lógica de negócio do Core Fiscal.                                                                      |
| **`adapters/out/signer/`** | Implementação de assinatura digital com certificado A1 via JCA nativo (`XMLSignatureFactory`).             | Regras de negócio de quando assinar ou acoplamento com o controller.                                   |
| **`adapters/out/sefin/`**  | Implementação de envio HTTP mTLS com a Receita Federal / Serpro.                                           | Regras de negócio do ERP do cliente.                                                                   |

---

## 5. 🚀 Estratégia de Implementação em Fases

- **Fase 1 (Épico 2 — Walking Skeleton):** Criação da infraestrutura Maven, classe principal `NfseApplication.java`, teste de fumaça e endpoint básico `/health`.
- **Fase 2 (Épico 3 & 4 — Core Fiscal Nacional):** Implementação dos pacotes `core/` e `adapters/out/` (schemas XSD, JAXB, assinatura A1 em memória e testes unitários via TDD).
- **Fase 3 (Épico 5 & 6 — Integração com Sefin e Adaptador do Cliente 001):** Implementação de `adapters/out/sefin/` (mTLS) e `adapters/in/legacy/` (ACL GeneXus 18).
