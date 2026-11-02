# Prompt Inicial

## Rascunho incial para refinamento

```textplain
Segue abaixo um rascunho de prompt que montei para dar o ponta pé inicial em um projeto novo.
Como sou novata na engenharia de software assitida por IA e na engenharia de prompts, preciso que você analise, forneça feedback e sugestões de melhorias para criarmos juntos um prompt inicial mais assertivo, eficiente e seguindo as boas práticas e técnicas atuais voltadas para a engenharia de software.
Retorne a resposta em texto.

"Preciso criar um novo projeto para realizar a integração direta com o **Ambiente Nacional**.

Requisitos funcionais:
1. Emissão direta ao ambiente nacional (Sefin Nacional).
2. Consulta e Cancelamento direta ao ambiente nacional ((Ambiente de Dados Nacional)).
3. O cliente já possui um ERP, ele vai chamar a nossa API, que vai ser responsável por:
* Receber os dados do meu cliente (payload em XML ou JSON).
* Validar contra o schema
* Assinar digitalmente: Já tenho uma biblioteca em java para xml (https://github.com/heviane/assinador-xml-java) que está atualmente sendo usada em produção para a emissão de NFS-e do modelo legado municipal (SIL Tecnologia), mas é necessário verificar a compatibilidade para esta nova integração direta com o ambiente nacional.
* Transmitir para o ambiente nacional
* Receber o response e devolvar para o chamador (meu cliente)

Requisitos não funcionais:
1. O ambiente do cliente é java com Tomcat.
2. A hospedagem dele é na [BrasilCloud](https://brasilcloud.com.br/)
3. Já tenho o certificado A1 dele.

O que eu preciso de você agora:
1. Sugestão de nome e descrição para a criação do repositório no github.
2. Sugestão de um README inicial para o projeto.
3. Sugestão das documentações técnicas iniciais do projeto.
3. O repositório vai ser público e genérico (sem dados do cliente), pois no futuro quero poder reutilizar para outros clientes."
```

--- 
