# Emissão

Aqui está a especificação completa da requisição HTTP REST para envio ao Sefin Nacional.

---

## 1. Definição do Endpoint e Cabeçalhos HTTP

* **Método:** `POST`
* **Ambiente (Homologação / Produção Restrita):**
`[https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional/nfse](https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional/nfse)`
* **Ambiente (Produção):**
`[https://sefin.nfse.gov.br/API/SefinNacional/nfse](https://sefin.nfse.gov.br/API/SefinNacional/nfse)`
* **Autenticação de Transporte:** **mTLS (TLS 1.2+)** utilizando o Certificado Digital A1 da empresa emitente ou procurador.
* **Headers obrigatórios:**
```http
POST /API/SefinNacional/nfse HTTP/1.1
Host: sefin.producaorestrita.nfse.gov.br
Content-Type: application/json; charset=utf-8
Accept: application/json
```

---

## 2. Formato do Payload JSON de Envio

A API do Sefin Nacional espera um objeto JSON com o campo `dpsXmlGZipB64` (ou serialização padrão do XML da `<DPS>` assinada compactado em GZIP e convertido em Base64, evitando quebras de encoding e caracteres especiais na transmissão):

```json
{
  "dpsXmlGZipB64": "H4sICAAAAAAC/3htbF9hc3NpbmFkby54bWwAzVbRbtswDH33VwTdB..."
}
```

> **Regra do XML contido no Base64:** O conteúdo desse XML deve ser **estritamente a raiz `<DPS>**` com o seu bloco `<infDPS Id="...">` assinado. Não envie `<NFSe>` nem `<infNFSe>`.

---

## 3. XML da `<DPS>` que deve ser assinado e serializado

Abaixo está a estrutura do XML assinado da DPS (com base nos dados cadastrais da sua empresa no Simples Nacional e tomador dos seus testes):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
  <infDPS Id="DPS320520023603027800015900001000000000032037">
    <tpAmb>2</tpAmb>
    <dhEmi>2026-11-01T22:45:00-03:00</dhEmi>
    <verAplic>LkSistemas_v1.00</verAplic>
    <serie>00001</serie>
    <nDPS>32037</nDPS>
    <dCompet>2026-11-01</dCompet>
    <tpEmit>1</tpEmit>
    <cLocEmi>3205200</cLocEmi>
    <prest>
      <CNPJ>36030278000159</CNPJ>
      <IM>9657</IM>
      <xNome>LK ASSESSORIA LTDA</xNome>
      <fone>2734346700</fone>
      <email>LKVIX@LKVIX.COM.BR</email>
      <regTrib>
        <opSimpNac>1</opSimpNac>
        <regApTribSN>1</regApTribSN>
        <regEspTrib>0</regEspTrib>
      </regTrib>
    </prest>
    <toma>
      <CNPJ>01211121000145</CNPJ>
      <xNome>PALLASCRED FACTORING FOMENTO MERC LTDA</xNome>
      <end>
        <endNac>
          <cMun>3205309</cMun>
          <CEP>29010364</CEP>
        </endNac>
        <xLgr>AV PRINCESA ISABEL, 574 SL 1111 BL A</xLgr>
        <nro>5741111</nro>
        <xBairro>CENTRO - EDF. PALLAS CENTER</xBairro>
      </end>
    </toma>
    <serv>
      <locPrest>
        <cLocPrestacao>3205200</cLocPrestacao>
      </locPrest>
      <cServ>
        <cTribNac>010301</cTribNac>
        <cTribMun>700</cTribMun>
        <xDescServ>Servicos de assessoria e desenvolvimento de sistemas de informacao.</xDescServ>
      </cServ>
    </serv>
    <valores>
      <vServPrest>
        <vServ>100.00</vServ>
      </vServPrest>
      <trib>
        <tribMun>
          <tribISSQN>1</tribISSQN>
          <tpRetISSQN>2</tpRetISSQN>
          <pAliq>2.50</pAliq>
        </tribMun>
        <tribFed>
          <piscofins>
            <CST>07</CST>
            <vBCPisCofins>0.00</vBCPisCofins>
            <pAliqPis>0.00</pAliqPis>
            <pAliqCofins>0.00</pAliqCofins>
            <vPis>0.00</vPis>
            <vCofins>0.00</vCofins>
            <tpRetPisCofins>2</tpRetPisCofins>
          </piscofins>
        </tribFed>
        <totTrib>
          <indTotTrib>0</indTotTrib>
        </totTrib>
      </trib>
    </valores>
  </infDPS>
  <!-- Assinatura digital da tag infDPS via XMLDSig com SHA-256 e Enveloped Signature -->
  <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
    <SignedInfo>
      <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
      <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
      <Reference URI="#DPS320520023603027800015900001000000000032037">
        <Transforms>
          <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
          <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
        </Transforms>
        <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
        <DigestValue>...</DigestValue>
      </Reference>
    </SignedInfo>
    <SignatureValue>...</SignatureValue>
    <KeyInfo>
      <X509Data>
        <X509Certificate>...</X509Certificate>
      </X509Data>
    </KeyInfo>
  </Signature>
</DPS>
```

---

## 4. Exemplo de Resposta do Sefin Nacional (HTTP 200 OK)

Quando autorizada, a resposta síncrona retorna o JSON contendo o XML completo da NFS-e autorizada (codificado em Base64/GZIP):

```json
{
  "tipoAmbiente": 2,
  "versaoAplicativo": "SefinNacional_v1.0.0",
  "dataHoraProcessamento": "2026-11-01T22:45:02-03:00",
  "codigoStatus": 100,
  "motivoStatus": "NFS-e gerada com sucesso",
  "chaveAcesso": "32052001236030278000159000000003203726110114333510",
  "nfseXmlGZipB64": "H4sICAAAAAAC/3htbF9hdXRvcml6YWRv..."
}
```

* **`chaveAcesso`:** Aqui vem a chave definitiva de **50 dígitos numéricos**.
* Ao decodificar `nfseXmlGZipB64`, você obtém o XML completo `<NFSe>` assinado pela autoridade geradora com o grupo totalizador e os dados oficiais para armazenamento no seu banco de dados e impressão do DANFSe.
