package br.com.heviane.nfse.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários: DpsXmlParser (Parser DOM Namespace-Aware e Anti-XXE)")
class DpsXmlParserTest {

    @Test
    @DisplayName("Deve extrair metadados estruturados de DPS padrão com namespace padrão")
    void deveExtrairMetadadosDpsPadrao() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <infDPS Id="DPS353060710000000000019100001000001903128101">
                        <tpAmb>2</tpAmb>
                        <dhEmi>2026-11-01T10:00:00-03:00</dhEmi>
                        <verAplic>1.0</verAplic>
                        <serie>00001</serie>
                        <nDPS>1903128101</nDPS>
                        <dCompet>2026-11-01</dCompet>
                        <tpEmit>1</tpEmit>
                        <cLocEmi>3530607</cLocEmi>
                        <prest>
                            <CNPJ>00000000000191</CNPJ>
                        </prest>
                    </infDPS>
                </DPS>
                """;

        DpsInfo info = DpsXmlParser.extrairInfo(xml);

        assertThat(info).isNotNull();
        assertThat(info.id()).isEqualTo("DPS353060710000000000019100001000001903128101");
        assertThat(info.tpAmb()).isEqualTo(2);
        assertThat(info.cLocEmi()).isEqualTo("3530607");
        assertThat(info.serie()).isEqualTo("00001");
        assertThat(info.nDps()).isEqualTo("1903128101");
        assertThat(info.cnpjPrestador()).isEqualTo("00000000000191");
        assertThat(info.cpfPrestador()).isNull();
        assertThat(info.temPrestadorCnpj()).isTrue();
        assertThat(info.temPrestadorCpf()).isFalse();
        assertThat(info.getTipoInscPrestador()).isEqualTo("1");
        assertThat(info.getDocumentoPrestador()).isEqualTo("00000000000191");
    }

    @Test
    @DisplayName("Deve extrair metadados de DPS com prefixo de namespace (ex: ns:infDPS e ns:prest)")
    void deveExtrairMetadadosComPrefixoNamespace() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfse:DPS xmlns:nfse="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <nfse:infDPS Id="DPS353060710000000000019100001000001903128101">
                        <nfse:tpAmb>1</nfse:tpAmb>
                        <nfse:serie>1</nfse:serie>
                        <nfse:nDPS>123</nfse:nDPS>
                        <nfse:cLocEmi>3530607</nfse:cLocEmi>
                        <nfse:prest autor="true">
                            <nfse:CNPJ>00000000000191</nfse:CNPJ>
                        </nfse:prest>
                    </nfse:infDPS>
                </nfse:DPS>
                """;

        DpsInfo info = DpsXmlParser.extrairInfo(xml);

        assertThat(info).isNotNull();
        assertThat(info.id()).isEqualTo("DPS353060710000000000019100001000001903128101");
        assertThat(info.tpAmb()).isEqualTo(1);
        assertThat(info.serie()).isEqualTo("1");
        assertThat(info.nDps()).isEqualTo("123");
        assertThat(info.cnpjPrestador()).isEqualTo("00000000000191");
    }

    @Test
    @DisplayName("Deve extrair prestador com CPF e ignorar tags homônimas em outros elementos aninhados")
    void deveExtrairPrestadorComCpfEIsolarEscopo() {
        String xml = """
                <DPS xmlns="http://www.sped.fazenda.gov.br/nfse">
                    <infDPS Id="DPS123">
                        <tpAmb>2</tpAmb>
                        <serie>00002</serie>
                        <nDPS>555</nDPS>
                        <cLocEmi>3530607</cLocEmi>
                        <prest>
                            <CPF>12345678901</CPF>
                        </prest>
                        <subst>
                            <nDPS>999999</nDPS>
                            <serie>99999</serie>
                        </subst>
                        <toma>
                            <CPF>98765432100</CPF>
                        </toma>
                    </infDPS>
                </DPS>
                """;

        DpsInfo info = DpsXmlParser.extrairInfo(xml);

        assertThat(info).isNotNull();
        assertThat(info.id()).isEqualTo("DPS123");
        assertThat(info.tpAmb()).isEqualTo(2);
        assertThat(info.serie()).isEqualTo("00002");
        assertThat(info.nDps()).isEqualTo("555");
        assertThat(info.cpfPrestador()).isEqualTo("12345678901");
        assertThat(info.cnpjPrestador()).isNull();
        assertThat(info.getTipoInscPrestador()).isEqualTo("2");
        assertThat(info.getDocumentoPrestador()).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("Deve retornar null quando XML for nulo, vazio ou sem infDPS")
    void deveRetornarNullParaXmlInvalido() {
        assertThat(DpsXmlParser.extrairInfo(null)).isNull();
        assertThat(DpsXmlParser.extrairInfo("")).isNull();
        assertThat(DpsXmlParser.extrairInfo("<outroElemento/>")).isNull();
        assertThat(DpsXmlParser.extrairInfo("não é xml")).isNull();
    }

    @Test
    @DisplayName("Deve bloquear injeção XXE com segurança")
    void deveBloquearInjecaoXXE() {
        String xmlXXE = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [
                  <!ELEMENT foo ANY >
                  <!ENTITY xxe SYSTEM "file:///etc/passwd" >]>
                <DPS xmlns="http://www.sped.fazenda.gov.br/nfse">
                    <infDPS Id="&xxe;">
                        <tpAmb>2</tpAmb>
                    </infDPS>
                </DPS>
                """;

        // Com disallow-doctype-decl ativado, o parser rejeita com exception interna e retorna null com segurança
        DpsInfo info = DpsXmlParser.extrairInfo(xmlXXE);
        assertThat(info).isNull();
    }
}
