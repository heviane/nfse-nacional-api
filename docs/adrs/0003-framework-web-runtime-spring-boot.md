# ADR-003: Framework Web & Runtime (Spring Boot 3 / WAR para Tomcat)

> **Status:** `Aprovado`  
> **Data:** 2026-11-02  
> **Decisores:** @heviane  
> **Contexto Técnico:** Escolha do Framework Web, Modelo de Empacotamento e Execução em Produção

---

## 1. 📌 Contexto e Forças do Problema

Com a plataforma **Java 17 (LTS)** definida na [ADR-001](./0001-escolha-linguagem-plataforma-java.md) e o **Apache Maven** estabelecido na [ADR-002](./0002-escolha-build-tool-maven-vs-gradle.md), fez-se necessário escolher o framework web e a estratégia de runtime para o middleware **`nfse-nacional-api`**.

A decisão é orientada pelas seguintes forças técnicas e operacionais:

* **Força 1: Implantação em Servlet Container Gerenciado Tomcat (BrasilCloud)**  
  O ambiente de produção do cliente opera servidores Apache Tomcat (versão 10+) em nuvem gerenciada. A aplicação deve ser entregue em formato **WAR** (*Web Application Archive*) para ser executada sob o ciclo de vida do servlet container do host, sem a necessidade de provisionar instâncias isoladas ou contêineres Docker independentes.

* **Força 2: Compatibilidade com o Namespace `jakarta.*` (Tomcat 10+)**  
  A partir da especificação Jakarta EE 9 / Tomcat 10, o ecossistema Java migrou do namespace legado `javax.servlet` para `jakarta.servlet`. O framework escolhido deve ter suporte nativo e maduro à especificação Jakarta EE, eliminando a necessidade de plugins de transformação de bytecode em tempo de deploy.

* **Força 3: Suporte Híbrido a Protocolos (XML/SOAP Legado + REST/JSON Moderno)**  
  Conforme definição de negócio, o sistema deve atender imediatamente um cliente legado com zero alterações no ERP (recebendo requisições XML/SOAP que antes iam para o GeneXus 18) e, futuramente, expor endpoints REST padronizados em JSON para novos clientes. O framework web deve manipular requisições com diferentes `Content-Type` (`text/xml`, `application/soap+xml`, `application/json`) de forma nativa e sem necessidade de bibliotecas pesadas e obsoletas de SOAP (como Apache CXF complexo).

* **Força 4: Produtividade Local e Agilidade em Testes Automatizados**  
  Embora o artefato final seja um WAR para deploy externo, o desenvolvimento no dia a dia e os testes de integração de fumaça (*smoke tests*) não devem exigir que a desenvolvedora instale e configure um servidor Tomcat manual na sua máquina. A aplicação deve suportar execução direta via terminal (`./mvnw spring-boot:run`) com container embutido para desenvolvimento local e suíte de testes com `MockMvc` e `@SpringBootTest`.

---

## 2. 🎯 Decisão Tomada

> **Decidimos adotar o Spring Boot 3.x como framework web oficial do projeto `nfse-nacional-api`, configurado com empacotamento em formato WAR através da extensão de `SpringBootServletInitializer`.**

Essa abordagem permite um **modelo híbrido de execução**:
1. **Em Desenvolvimento Local:** A aplicação roda standalone utilizando o Tomcat embutido fornecido pelo `spring-boot-starter-web`, permitindo depuração imediata com F5 no VS Code e suíte de testes automatizados com `MockMvc`.
2. **Em Produção (Tomcat BrasilCloud):** As dependências do Tomcat embutido são marcadas como `provided` (`providedRuntime`), e a classe principal estende `SpringBootServletInitializer`, delegando a inicialização do contexto Spring ao servidor Tomcat gerenciado da BrasilCloud sem qualquer atrito de compatibilidade.

---

## 3. ⚖️ Consequências

### Consequências Positivas (Ganhos):
* **Suporte Nativo a Jakarta EE / Tomcat 10+:** O Spring Boot 3 foi concebido sobre a base Jakarta EE, garantindo compatibilidade plena com os servidores Tomcat modernos da BrasilCloud.
* **Flexibilidade Total de Endpoints:** Permite receber requisições XML legadas via `@PostMapping(consumes = MediaType.TEXT_XML_VALUE)` no mesmo aplicativo onde futuramente estarão os controladores REST em JSON (`@RestController`).
* **Testabilidade Robusta:** Acesso nativo a ferramentas como `@SpringBootTest`, `@WebMvcTest` e `MockMvc`, possibilitando simular requisições de ponta a ponta sem necessidade de subir um servidor real durante a compilação.
* **Gestão Coesa de Dependências:** O *Bill of Materials* (BOM) do Spring Boot gerencia automaticamente as versões compatíveis de bibliotecas de validação, parsing e logging (Jackson, Logback, SLF4J, etc.).

### Consequências Negativas (Custos / Trade-offs):
* **Tamanho do Artefato WAR:** O arquivo WAR final incorpora os módulos do Spring Framework, resultando em um artefato ligeiramente maior (entre 30MB e 50MB) do que uma aplicação Jakarta EE pura sem dependências externas. No entanto, isso não representa impedimento para os canais de deploy atuais.
* **Consumo de Memória na Inicialização:** O Spring Boot realiza varredura de componentes (*component scanning*) e injeção de dependências no startup, consumindo um pouco mais de memória que frameworks estáticos, plenamente comportado pela capacidade do host na BrasilCloud.

### Riscos Mitigados:
* Mitiga o risco de incompatibilidade de classes do tipo `ClassNotFoundException: javax.servlet.Filter` ao realizar o deploy em servidores Tomcat 10+, visto que o Spring Boot 3 usa exclusivamente o pacote `jakarta.*`.
* Elimina o risco de retrabalho na criação de testes automatizados de integração.

---

## 4. 🔍 Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
| :--- | :--- | :--- | :--- |
| **Spring Boot 2.x** | Amplo material legado na internet. | EOL (*End of Life*) com encerramento de patches de segurança; acoplado ao pacote legado `javax.servlet`, exigindo gambiarras para rodar em Tomcat 10+. | Inviável para projetos novos que exigem suporte a longo prazo e compatibilidade com Tomcat moderno. |
| **Jakarta EE Puro (Servlets / JAX-RS sem Spring)** | Artefatos extremamente leves (KB em vez de MB). | Alta complexidade para testes locais de integração; menor produtividade no desenvolvimento diário; injeção de dependências (CDI) mais rígida e burocrática. | Reduz drasticamente a velocidade de entrega e a conveniência de testes automatizados. |
| **Quarkus** | Inicialização ultrarrápida e baixíssimo consumo de memória; excelente para GraalVM. | O foco primário do Quarkus é a geração de contêineres e imagens nativas; o suporte ao empacotamento em formato WAR tradicional para Tomcat externo é secundário e possui restrições. | Risco de incompatibilidade e atrito com a infraestrutura tradicional de servlets da BrasilCloud. |
| **Micronaut** | Injeção de dependência em tempo de compilação sem reflexão. | Menor adoção em cenários de deploy WAR para Tomcat clássico; comunidade menor para integração com bibliotecas fiscais legadas. | O Spring Boot oferece maior estabilidade e maturidade comprovada para este cenário. |

