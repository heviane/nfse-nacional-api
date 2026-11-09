# ADR-002: Escolha do Gerenciador de Build (Maven vs Gradle)

> **Status:** `Aprovado`
> **Data:** 2026-11-02
> **Decisores:** @heviane
> **Contexto Técnico:** Escolha do Gerenciador de Build & Automação de Dependências

---

## 1. 📌 Contexto e Forças do Problema

Com a decisão de utilizar **Java 17 (LTS)** formalizada na [ADR-001](./0001-escolha-linguagem-plataforma-java.md), tornou-se necessário definir a ferramenta oficial para automação de compilação, gestão de dependências, geração de código a partir de schemas fiscais e empacotamento do middleware **`nfse-nacional-api`**.

A decisão considerou as seguintes forças técnicas e operacionais:

- **Força 1: Geração de Código a partir de Schemas Fiscais (XSD / JAXB)**
  O Ambiente Nacional da NFS-e (Sefin Nacional / ADN) disponibiliza schemas oficiais complexos em formato XSD (ex: `DPS_v1.00.xsd`, schemas de assinaturas e eventos). É indispensável utilizar plugins maduros capazes de compilar esses XSDs diretamente em classes Java com anotações JAXB (via `jaxb2-maven-plugin` ou compatível) de forma determinística no pipeline de compilação.

- **Força 2: Compatibilidade com Ativos de Software Fiscais Existentes**
  A biblioteca de referência e validação de assinatura digital em produção (`assinador-xml-java`) já foi estruturada e homologada utilizando o Apache Maven. Alinhar o gerenciador de build mitiga riscos de conflitos de dependências transitivas e garante compatibilidade de importação.

- **Força 3: Empacotamento Estável em Formato WAR para Tomcat (BrasilCloud)**
  O modelo de implantação em produção exige a entrega de um artefato em formato **WAR** (_Web Application Archive_) para execução em container Tomcat gerenciado. O build tool precisa suportar nativamente a separação de dependências providas pelo servlet container (`provided`) daquelas embutidas no pacote.

- **Força 4: Reprodutibilidade Sem Instalação Manual (Build Wrapper)**
  Qualquer desenvolvedor ou agente de IA deve conseguir clonar o repositório e compilar a aplicação de forma 100% idêntica e imediata, sem necessidade de configurar previamente variáveis de ambiente ou instalar binários globais no sistema operacional.

- **Força 5: Filosofia Declarativa vs. Complexidade Imperativa**
  Projetos fiscais exigem determinismo e estabilidade. Estruturas declarativas evitam que scripts de build customizados em linguagens de programação (Groovy/Kotlin) introduzam comportamentos imprevisíveis ou quebrem em atualizações de versão.

---

## 2. 🎯 Decisão Tomada

> **Decidimos adotar o Apache Maven (3.9+) acompanhado do Maven Wrapper (`mvnw` / `mvnw.cmd`) como o gerenciador de build oficial do projeto `nfse-nacional-api`.**

O Maven foi escolhido por representar o padrão _de facto_ do ecossistema fiscal e contábil brasileiro em Java. O ciclo de vida declarativo (`clean`, `compile`, `test`, `package`) e o ecossistema maduro de plugins para XSD/JAXB e WAR garantem total previsibilidade.

O uso do **Maven Wrapper** é mandatório no repositório, garantindo que o comando `./mvnw clean compile` ou `./mvnw clean package` funcione universalmente em qualquer ambiente sem necessidade do Maven instalado localmente.

---

## 3. ⚖️ Consequências

### Consequências Positivas (Ganhos)

- **Sinergia Fiscal Absoluta:** Compatibilidade nativa com o padrão histórico de ferramentas da Receita Federal, SPED e ecossistema de NF-e/NFS-e no Brasil.
- **Maturidade na Compilação de XSD:** O plugin `jaxb2-maven-plugin` é altamente estável, documentado e amplamente utilizado na comunidade contábil para compilar schemas da Receita Federal.
- **Empacotamento WAR Simplificado:** Configuração direta e padronizada via `maven-war-plugin` para geração de artefatos para Tomcat na nuvem.
- **Zero Configuração de Máquina:** O `mvnw` baixa a versão exata do Maven na primeira execução, garantindo isolamento completo de ambiente.

### Consequências Negativas (Custos / Trade-offs)

- **Verbosidade do XML (`pom.xml`):** O arquivo de configuração em XML é mais extenso e estático que as DSLs de outros gerenciadores.
- **Builds Limpos Ligeiramente Mais Lentos:** O Maven não mantém um daemon residente na memória como o Gradle, tornando compilações completas um pouco mais lentas. No entanto, por se tratar de um microserviço/middleware sem dezenas de submódulos, o impacto de tempo é insignificante (segundos).

### Riscos Mitigados

- Mitiga o risco de quebras frequentes em builds por descontinuação de APIs em DSLs de script (problema comum no ecossistema Gradle em atualizações de versão maior).
- Elimina divergências de comportamento entre a máquina de desenvolvimento local (Windows) e o ambiente de CI ou servidor de produção (Linux).

---

## 4. 🔍 Alternativas Consideradas

| Alternativa                      | Prós                                                                                 | Contras                                                                                                                                                                                                  | Motivo da Rejeição                                                                                                           |
| :------------------------------- | :----------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------- |
| **Gradle (Groovy / Kotlin DSL)** | Builds incrementais velozes através de daemon; sintaxe concisa e altamente flexível. | Plugins para geração de código JAXB a partir de XSD sofrem com frequentes descontinuidades e menor padronização no ecossistema fiscal brasileiro; maior risco de scripts imperativos complexos no build. | Custo de manutenção e instabilidade nos plugins XSD não compensam o ganho marginal de performance em um projeto deste porte. |
| **Apache Ant + Apache Ivy**      | Alta flexibilidade de tarefas manuais em XML.                                        | Tecnologia legada e obsoleta para projetos novos; gestão arcaica de dependências e sem convenção moderna de ciclo de vida.                                                                               | Não atende os padrões de engenharia de software modernos.                                                                    |

---

## 5. 🔄 Adendo / Revisão Arquitetural: Validação de Schema XSD sem JAXB

> **Data do Adendo:** 2026-11-09  
> **Status:** `Ratificado`

Na formulação inicial desta ADR (Força 1 e Consequências Positivas), considerou-se a adoção do `jaxb2-maven-plugin` para geração automática de classes Java (POJOs/DTOs) a partir dos arquivos XSD fornecidos pela Receita Federal (`DPS_v1.00.xsd`, `DPS_v1.01.xsd`, `pedRegEvento_v1.00.xsd`, etc.).

Durante o desenvolvimento do middleware, **optou-se conscientemente por não incluir o `jaxb2-maven-plugin` no `pom.xml`**, adotando em seu lugar:
1. **Validação Estrutural e Semântica Direta via JAXP:** Implementada em `ValidadorSchemaService` utilizando `javax.xml.validation.SchemaFactory` e `Validator` nativos do Java 17 com suporte a XML Schema 1.0 (`XMLConstants.W3C_XML_SCHEMA_NS_URI`) e proteção contra XXE (`FEATURE_SECURE_PROCESSING`, desativação explícita de `ACCESS_EXTERNAL_DTD` e `ACCESS_EXTERNAL_SCHEMA`).
2. **Manipulação e Assinatura Digital Baseadas em DOM/W3C XML-DSig:** Assinatura digital da tag `<infDPS>` realizada via Java Cryptography Architecture (JCA) e `XMLSignatureFactory` sobre a árvore DOM, sem necessidade de serialização/deserialização intermediária via JAXB.
3. **Modelagem de Domínio Pura com Java Records:** O Core Domain (`br.com.heviane.nfse.core.domain`) opera com *Records* imutáveis, independentes de anotações de frameworks de serialização (zero acoplamento com Jackson ou JAXB no Core).

### Vantagens Desta Abordagem:
- **Build Mais Rápido e Determinístico:** Elimina o tempo e a complexidade de compilar dezenas de schemas XSD em centenas de classes geradas a cada build do Maven.
- **Isolamento e Pureza Arquitetural:** O Core não é poluído por centenas de classes JAXB aninhadas e heterogêneas geradas por compiladores externos.
- **Fidelidade Criptográfica:** A manipulação direta do XML/DOM preserva a integridade de espaços, quebras de linha e canonicalização C14N necessária para a assinatura digital do padrão Sefin/ADN.

