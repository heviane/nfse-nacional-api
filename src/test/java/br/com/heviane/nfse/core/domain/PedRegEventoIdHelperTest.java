package br.com.heviane.nfse.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: Gerador e Validador de Id de Pedido de Registro de Eventos (TSIdPedRefEvt)")
class PedRegEventoIdHelperTest {

    private static final String CHAVE_50 = "32052002601000000000019100001000000000000001123456";

    @Test
    @DisplayName("Deve gerar com sucesso o Id para cancelamento (Evento 101101) com 59 caracteres")
    void deveGerarIdCancelamentoComSucesso() {
        String id = PedRegEventoIdHelper.gerarIdCancelamento(CHAVE_50);

        assertThat(id).isNotNull();
        assertThat(id).hasSize(59);
        assertThat(id).startsWith("PRE");
        assertThat(id).isEqualTo("PRE" + CHAVE_50 + "101101");
        assertThat(PedRegEventoIdHelper.isValido(id)).isTrue();
    }

    @Test
    @DisplayName("Deve gerar Id para outros tipos de evento de 6 dígitos")
    void deveGerarIdParaOutrosEventos() {
        String idSubstituicao = PedRegEventoIdHelper.gerarId(CHAVE_50, PedRegEventoIdHelper.TIPO_EVENTO_CANCELAMENTO_SUBSTITUICAO);

        assertThat(idSubstituicao).hasSize(59);
        assertThat(idSubstituicao).endsWith("105102");
        assertThat(PedRegEventoIdHelper.isValido(idSubstituicao)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar chave de acesso com tamanho diferente de 50 dígitos")
    void deveRejeitarChaveInvalida() {
        assertThatThrownBy(() -> PedRegEventoIdHelper.gerarIdCancelamento("123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 dígitos");

        assertThatThrownBy(() -> PedRegEventoIdHelper.gerarIdCancelamento(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar tipo de evento inválido")
    void deveRejeitarTipoEventoInvalido() {
        assertThatThrownBy(() -> PedRegEventoIdHelper.gerarId(CHAVE_50, "1011"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 dígitos");

        assertThatThrownBy(() -> PedRegEventoIdHelper.gerarId(CHAVE_50, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve validar adequadamente strings usando isValido")
    void deveValidarComIsValido() {
        String valido = "PRE" + CHAVE_50 + "101101";
        assertThat(PedRegEventoIdHelper.isValido(valido)).isTrue();

        // 62 caracteres (tentando sufixar 001)
        assertThat(PedRegEventoIdHelper.isValido(valido + "001")).isFalse();

        // Prefixo EVT (retorno, não pedido)
        assertThat(PedRegEventoIdHelper.isValido("EVT" + CHAVE_50 + "101101")).isFalse();

        // Nulo ou vazio
        assertThat(PedRegEventoIdHelper.isValido(null)).isFalse();
        assertThat(PedRegEventoIdHelper.isValido("")).isFalse();
        assertThat(PedRegEventoIdHelper.isValido("PRE123")).isFalse();
    }
}

