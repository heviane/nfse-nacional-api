package br.com.heviane.nfse.adapters.in.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FiscalAuditLoggerTest {

    private final FiscalAuditLogger auditLogger = new FiscalAuditLogger();

    @Test
    @DisplayName("Deve mascarar CNPJ corretamente preservando apenas identificação mínima")
    void deveMascararCnpjCorretamente() {
        String cnpj = "12345678000195";
        String mascarado = auditLogger.mascararCnpj(cnpj);

        assertThat(mascarado).isEqualTo("12.***.***/0001-**");
    }

    @Test
    @DisplayName("Deve retornar N/A para CNPJ nulo, vazio ou de tamanho inválido")
    void deveRetornarNaParaCnpjInvalido() {
        assertThat(auditLogger.mascararCnpj(null)).isEqualTo("N/A");
        assertThat(auditLogger.mascararCnpj("")).isEqualTo("N/A");
        assertThat(auditLogger.mascararCnpj("123")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Deve extrair CNPJ do prestador a partir do XML da DPS")
    void deveExtrairCnpjDoXmlDps() {
        String xml = """
                <DPS xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.00">
                    <infDPS>
                        <prest>
                            <CNPJ>00000000000191</CNPJ>
                        </prest>
                    </infDPS>
                </DPS>
                """;

        String cnpj = auditLogger.extrairCnpjDps(xml);

        assertThat(cnpj).isEqualTo("00000000000191");
    }

    @Test
    @DisplayName("Deve registrar emissão, falha, consulta e cancelamento sem lançar exceções")
    void deveRegistrarEventosSemExcecoes() {
        assertDoesNotThrow(() -> {
            auditLogger.registrarEmissao("REST", "32052002601000000000019100001000000000000001123456", "00000000000191", 100, 200, 150L);
            auditLogger.registrarFalhaEmissao("SOAP", "00000000000191", 400, "Falha de validação XSD", 45L);
            auditLogger.registrarConsulta("32052002601000000000019100001000000000000001123456", 100, 200, 80L);
            auditLogger.registrarCancelamento("32052002601000000000019100001000000000000001123456", "00000000000191", 100, 200, 110L);
        });
    }
}
