# ADR-001: Escolha da Linguagem e Plataforma de Execução (Java 17+)

> **Status:** `Aprovado`  
> **Data:** 2026-11-02  
> **Decisores:** @heviane  
> **Contexto Técnico:** Escolha da Linguagem & Plataforma de Execução do Middleware Fiscal

---

## 1. 📌 Contexto e Forças do Problema

O projeto **`nfse-nacional-api`** foi concebido como um middleware desacoplado para integrar ERPs legados ao novo **Ambiente Nacional da NFS-e (Sefin Nacional / ADN)**. A solução é responsável por receber os dados de prestação de serviços, validar a estrutura contra schemas oficiais (XSD), assinar digitalmente a DPS (Declaração de Prestação de Serviços) com certificado digital ICP-Brasil A1 e realizar a transmissão segura via mTLS.

A escolha da tecnologia foi condicionada por forças de infraestrutura, negócio e segurança criptográfica:

* **Força 1: Restrição de Infraestrutura e Custos Operacionais do Cliente (Força Primária)**  
  O ambiente de produção existente do cliente já conta com servidores de aplicação gerenciados (**Tomcat na BrasilCloud**). Adoção de uma stack diferente exigiria a contratação, provisionamento e manutenção de instâncias adicionais, elevando os custos de hospedagem e a complexidade operacional da equipe de TI.

* **Força 2: Maturidade Criptográfica e Ecossistema Fiscal Brasileiro**  
  Historicamente, os projetos fiscais brasileiros (NF-e, CT-e, MDF-e, SPED e agora NFS-e Nacional) possuem seu ecossistema mais maduro e testado em produção sobre a plataforma Java. A manipulação de certificados digitais PKCS#12 (A1), assinatura digital de XML sob a especificação W3C XML-DSig (RSA-SHA256 e C14N) e handshake TLS mútuo (mTLS) são suportados nativamente e de forma robusta pela biblioteca padrão do Java (**JCA - Java Cryptography Architecture**).

* **Força 3: Reuso e Compatibilidade com Componentes Fiscais Existentes**  
  A existência de ativos prévios e bibliotecas de referência validadas em produção (como o `assinador-xml-java`) permite acelerar o desenvolvimento do Core Fiscal e mitigar riscos de incompatibilidade na assinatura dos lotes.

* **Força 4: Validação Estrita de Schemas XSD e Tipagem Forte**  
  O fisco brasileiro rejeita qualquer payload que viole milimetricamente os esquemas XSD oficiais. A tipagem estrita do Java e os mecanismos nativos de validação XML/XSD e binding de dados (JAXB) reduzem significativamente bugs de formatação antes do envio da requisição à Receita Federal.

* **Força 5: Suporte de Longo Prazo (LTS)**  
  A necessidade de uma plataforma estável e segura para suportar operações fiscais e financeiras de missão crítica por anos sem quebras de compatibilidade.

---

## 2. 🎯 Decisão Tomada

> **Decidimos adotar o Java 17 (LTS) como linguagem e runtime oficial para o desenvolvimento do middleware `nfse-nacional-api`.**

A versão **Java 17** foi escolhida por ser a versão com suporte de longo prazo (LTS) amplamente suportada pelos servidores Tomcat 10+ modernos, trazendo benefícios de performance da JVM (HotSpot), novas construções de linguagem que reduzem o boilerplate (como *Records*, *Text Blocks* e *Pattern Matching*) e compatibilidade plena com o ecossistema criptográfico e de validação XML.

---

## 3. ⚖️ Consequências

### Consequências Positivas (Ganhos):
* **Custo Zero de Nova Infraestrutura:** O artefato gerado pode ser implantado diretamente nos servidores Tomcat já contratados na BrasilCloud.
* **Segurança Criptográfica Nativa:** Manipulação de certificados A1 estritamente em memória (`KeyStore`) através de `JCA` e APIs padronizadas de `XMLSignatureFactory`, sem dependência de binários externos ou scripts C não portáveis.
* **Estabilidade Fiscal:** Elimina problemas clássicos de canonicalização XML (C14N) e cálculo de `DigestValue` que são frequentes em bibliotecas não oficiais de outras linguagens.
* **Manutenibilidade a Longo Prazo:** Suporte LTS garante atualizações contínuas de segurança e patches da JVM.

### Consequências Negativas (Custos / Trade-offs):
* **Verbosidade Inicial:** Java possui uma estrutura de código e empacotamento mais formal que linguagens de script dinâmicas (mitigado pelo uso de *Records* do Java 17 e Lombok, se aplicável).
* **Consumo de Memória Base:** A JVM tem um consumo inicial de memória (heap) superior ao de binários compilados nativamente em Go ou Rust, porém plenamente compatível com o dimensionamento dos servidores do cliente.

### Riscos Mitigados:
* Mitiga o risco de rejeição em massa de lotes no Sefin Nacional por inconsistências sutis de codificação de caracteres (UTF-8) ou canonicalização de XML.
* Mitiga o risco de atraso no cronograma devido à necessidade de reescrever do zero rotinas complexas de assinatura A1 em memória.

---

## 4. 🔍 Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
| :--- | :--- | :--- | :--- |
| **Node.js / TypeScript** | Desenvolvimento ágil de APIs REST, ecossistema leve. | Bibliotecas de assinatura XML ICP-Brasil e C14N são fragmentadas, pouco mantidas e com frequentes bugs de formatação; exige contratação de novo ambiente de execução. | Insegurança técnica no Core Fiscal e necessidade de novos custos de infraestrutura no cliente. |
| **Python** | Prototipagem rápida, boa manipulação de dados JSON. | Suporte limitado a manipuladores nativos de certificados A1 em memória para os padrões rígidos da Receita Federal; deploy em container Tomcat impraticável. | Desalinhado com a infraestrutura existente e maior complexidade para garantia de thread-safety na assinatura. |
| **.NET (C#)** | Excelente suporte a criptografia e APIs REST robustas. | Incompatível com o ambiente Linux/Tomcat contratado na BrasilCloud sem migração completa da infraestrutura. | Rejeitado pela restrição de infraestrutura e custo operacional do cliente. |
| **Java 8 / Java 11** | Ampla compatibilidade legado. | Versões antigas, sem as melhorias de concorrência, performance e recursos modernos de linguagem do Java 17. | Desnecessário manter versões defasadas, visto que o Tomcat de destino suporta Java 17+. |
