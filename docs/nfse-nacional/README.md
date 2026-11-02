# Sistema Nacional da NFS-e (gov.br/nfse e ADN)

> **Documentação de Referência Oficial, Manuais Técnicos & Consoles Swagger**  
> Repositório: [`heviane/nfse-nacional-api`](https://github.com/heviane/nfse-nacional-api)  
> Documento Relacionado: [`Glossário Fiscal & Domínio da NFS-e`](../glossario-nfse.md)

---

## 📌 1. Portal Principal e Documentação Técnica

* **Repositório Central da Receita Federal:**  
  [https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica)  
  *(Contém os schemas XSD oficiais, manuais de integração, notas técnicas vigentes e planilhas de regras de negócio/RTC)*.
* **Página de APIs (Produção e Homologação / Produção Restrita):**  
  [https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/producao-restrita](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/producao-restrita)

---

## 🚀 2. Consoles Interativos de API (Swagger)

Para inspeção dos contratos de endpoints de emissão síncrona (`POST /nfse`), consulta de eventos (`/eventos`) e distribuição por NSU:

* **Swagger — Sefin Nacional (Emissor Público / Emissão Síncrona de DPS):**  
  [Console Swagger Sefin Nacional (Produção Restrita)](https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional/docs/)
* **Swagger — ADN Contribuintes (Distribuição e Consulta de Eventos/DFe por NSU):**  
  [Console Swagger ADN Contribuintes (Produção Restrita)](https://adn.producaorestrita.nfse.gov.br/contribuintes/docs/index.html)

---

## 📄 3. Manuais Oficiais em PDF

* **Manual de Integração do Emissor Público Nacional (API REST da DPS/NFS-e):**  
  [Manual dos Contribuintes — APIs do Emissor Público Nacional (PDF)](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/documentacao-atual/manual-contribuintes-emissor-publico-api-sistema-nacional-nfs-e-v1-2-out2025.pdf)
* **Manual de APIs do Ambiente de Dados Nacional (ADN / Distribuição):**  
  [Manual dos Contribuintes — APIs do ADN (PDF)](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/documentacao-atual/manual-contribuintes-apis-adn-sistema-nacional-nfse.pdf)

---

## 🏛️ 4. Documentos Internos Relacionados

* **[Glossário Fiscal & Domínio da NFS-e](../glossario-nfse.md):** Conceitos tributários, diferença prática entre Sefin e ADN, DPS e chave de 50 dígitos.
* **[Especificação da DPS e Assinatura A1 (SPEC-001)](../specs/0001-validacao-dps-assinatura-digital.md):** Regras de validação XSD e assinatura digital RSA-SHA256.
* **[Contexto do Cliente 001](../clients/cliente-001.md):** Especificidades da migração de Vila Velha (SIL Tecnologia) para o Sefin Nacional.

