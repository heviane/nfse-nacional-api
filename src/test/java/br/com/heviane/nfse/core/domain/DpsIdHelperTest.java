package br.com.heviane.nfse.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: DpsIdHelper — Regra Oficial TSIdDPS (45 posições)")
class DpsIdHelperTest {

    @Test
    @DisplayName("Deve gerar Id da DPS válido para CNPJ com exatamente 45 posições")
    void deveGerarIdValidoParaCnpjComTamanhoExatoDe45Caracteres() {
        String cMun = "3205200";
        String cnpj = "00.000.000/0001-91";
        String serie = "1";
        String nDPS = "1";

        String idGerado = DpsIdHelper.gerarIdParaCnpj(cMun, cnpj, serie, nDPS);

        assertThat(idGerado).isEqualTo("DPS320520010000000000019100001000000000000001");
        assertThat(idGerado).hasSize(45);
        assertThat(DpsIdHelper.isValido(idGerado)).isTrue();
    }

    @Test
    @DisplayName("Deve gerar Id da DPS válido para CPF preenchendo 3 zeros à esquerda até 14 dígitos")
    void deveGerarIdValidoParaCpfPreenchendoZerosAEsquerda() {
        String cMun = "3205200";
        String cpf = "123.456.789-01";
        String serie = "2";
        String nDPS = "500";

        String idGerado = DpsIdHelper.gerarIdParaCpf(cMun, cpf, serie, nDPS);

        assertThat(idGerado).isEqualTo("DPS320520020001234567890100002000000000000500");
        assertThat(idGerado).hasSize(45);
        assertThat(DpsIdHelper.isValido(idGerado)).isTrue();
    }

    @Test
    @DisplayName("Deve validar corretamente com isValido()")
    void deveValidarCorretamenteComIsValido() {
        // Válido: 45 posições exatas ("DPS" + 42 dígitos)
        assertThat(DpsIdHelper.isValido("DPS320520010000000000019100001000000000000001")).isTrue();

        // Inválido: apenas 44 caracteres
        assertThat(DpsIdHelper.isValido("DPS32052001000000000001910000100000000000000")).isFalse();

        // Inválido: 46 caracteres
        assertThat(DpsIdHelper.isValido("DPS3205200100000000000191000010000000000000001")).isFalse();

        // Inválido: prefixo incorreto
        assertThat(DpsIdHelper.isValido("NFS320520010000000000019100001000000000000001")).isFalse();

        // Inválido: caracteres não numéricos após o prefixo
        assertThat(DpsIdHelper.isValido("DPS32052001000000000001910000100000000000000X")).isFalse();

        // Inválido: nulo ou vazio
        assertThat(DpsIdHelper.isValido(null)).isFalse();
        assertThat(DpsIdHelper.isValido("")).isFalse();
        assertThat(DpsIdHelper.isValido("   ")).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção quando o código do município não possuir 7 dígitos IBGE")
    void deveLancarExcecaoParaMunicipioComTamanhoInvalido() {
        assertThatThrownBy(() -> DpsIdHelper.gerarIdParaCnpj("123456", "00000000000191", "1", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente 7 dígitos");
    }

    @Test
    @DisplayName("Deve lançar exceção quando o tipo de inscrição não for 1 nem 2")
    void deveLancarExcecaoParaTipoInscricaoInvalido() {
        assertThatThrownBy(() -> DpsIdHelper.gerarId("3205200", "3", "00000000000191", "1", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de inscrição inválido");
    }

    @Test
    @DisplayName("Deve lançar exceção quando CNPJ não tiver 14 dígitos")
    void deveLancarExcecaoParaCnpjIncompleto() {
        assertThatThrownBy(() -> DpsIdHelper.gerarIdParaCnpj("3205200", "123456780001", "1", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 dígitos numéricos");
    }

    @Test
    @DisplayName("Deve lançar exceção quando parâmetros obrigatórios forem nulos")
    void deveLancarExcecaoParaParametrosNulos() {
        assertThatThrownBy(() -> DpsIdHelper.gerarId(null, "1", "00000000000191", "1", "1"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DpsIdHelper.gerarId("3205200", null, "00000000000191", "1", "1"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DpsIdHelper.gerarId("3205200", "1", null, "1", "1"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DpsIdHelper.gerarId("3205200", "1", "00000000000191", null, "1"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DpsIdHelper.gerarId("3205200", "1", "00000000000191", "1", null))
                .isInstanceOf(NullPointerException.class);
    }
}

