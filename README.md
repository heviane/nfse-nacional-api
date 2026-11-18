# NFS-e Nacional API

> **API REST para integração direta com o Ambiente Nacional da NFS-e (Sefin Nacional / ADN): emissão, consulta e cancelamento de DPS com assinatura A1 e mTLS.**

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![CI](https://github.com/heviane/nfse-nacional-api/actions/workflows/ci.yml/badge.svg)](https://github.com/heviane/nfse-nacional-api/actions/workflows/ci.yml)
[![Status](https://img.shields.io/badge/Status-MVP%20Conclu%C3%ADdo%20(v0.1.0)-success?style=flat-square)](#)
[![Arquitetura](https://img.shields.io/badge/Arquitetura-Spec--Driven%20(SDD)-purple?style=flat-square)](#)
[![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-green?style=flat-square)](./LICENSE)

---

## 🎯 Sobre o Projeto

O **`nfse-nacional-api`** é um middleware de integração fiscal projetado para atuar como ponte intermediária e desacoplada entre sistemas ERP legados e os servidores do **Ambiente Nacional da NFS-e (Sefin Nacional e ADN - Ambiente de Dados Nacional)** da Receita Federal do Brasil.

Ele centraliza a complexidade da legislação e da segurança tributária brasileira:
* Validação semântica e estrutural contra os **Schemas Oficiais (XSD)**.
* **Assinatura Digital XML** da Declaração de Prestação de Serviços (DPS) com Certificado Digital ICP-Brasil A1 (PKCS#12).
* Comunicação segura via **mTLS (Mutual TLS)** com os WebServices/APIs nacionais.
* Exposição de endpoints REST simples em JSON para que qualquer ERP possa emitir notas sem lidar diretamente com certificados e criptografia XML.

---

## 🏛️ Fluxo de Integração

```mermaid
flowchart LR
    ERP["🏢 ERP do Cliente\n(Legado / Qualquer Stack)"] 
    API["🧾 NFS-e Nacional API\n(Middleware Java)"]
    SEFIN["🏛️ Sefin Nacional / ADN\n(Ambiente Nacional de Dados)"]

    ERP -->|"1. POST Payload JSON/XML"| API
    API -->|"2. Validação XSD"| API
    API -->|"3. Assinatura A1 (infDPS)"| API
    API -->|"4. Transmissão mTLS"| SEFIN
    SEFIN -->|"5. Protocolo / NFS-e Gerada"| API
    API -->|"6. Retorno Estruturado"| ERP
```

---

## 🚀 Funcionalidades do MVP

- [x] **Emissão de DPS:** Envio e transmissão síncrona/assíncrona de DPS individual gerando a NFS-e oficial nacional.
- [x] **Consulta de NFS-e & DPS:** Consulta de situação por Id da DPS e recuperação de NFS-e autorizada por Chave de Acesso (50 dígitos).
- [x] **Eventos da Nota:** Registro de evento de cancelamento oficial da NFS-e (Evento 101101).
- [x] **Gerenciamento de Certificado A1:** Carga e leitura segura de certificados em memória sem persistência desprotegida.
- [x] **Camada Anticorrupção (ACL SOAP):** Emulação transparente do WebService municipal da SIL Tecnologia para ERPs legados (GeneXus 18).

---

## 🛠️ Stack Tecnológica & Pré-requisitos

* **Linguagem & Runtime:** Java 17+ (compatível com Tomcat 10.1+ / Servlet Container 6.0 na BrasilCloud).
* **Segurança & Criptografia:** Java Cryptography Architecture (JCA), PKCS#12 e RSA-SHA256.
* **Comunicação Segura:** Handshake TLS mútuo (mTLS) com `SSLContext` dedicado.
* **Padrões de Projeto:** Spec-Driven Development (SDD), Clean Architecture e TDD.

---

## 📂 Documentação & Governança

Toda a evolução do projeto é gerida via **Docs-as-Code**:

* **[Backlog do Projeto](./docs/backlog.md):** Épicos, fatias verticais e status das tarefas.
* **[Especificações Técnicas (`docs/specs/`)](./docs/specs/):** Regras de negócio e contratos de cada fatia antes da codificação.
* **[Decisões Arquiteturais (`docs/adrs/`)](./docs/adrs/):** Registros formais de decisões técnicas (ADRs).
* **[Diretrizes de IA (`AGENTS.md`)](./AGENTS.md):** Regras e restrições operacionais para agentes de pair programming.

---

## 📄 Licença

Este projeto é distribuído sob a licença [MIT](./LICENSE).
