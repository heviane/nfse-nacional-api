package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.ValidacaoFiscalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: Validador de Schemas XSD (CORE-01)")
class ValidadorSchemaServiceTest {

    private ValidadorSchemaService validador;
    private String xmlValido;

    @BeforeEach
    void setUp() throws Exception {
        validador = new ValidadorSchemaService();

        try (InputStream is = getClass().getResourceAsStream("/samples/dps-exemplo-valido.xml")) {
            assertThat(is).as("Arquivo de exemplo dps-exemplo-valido.xml deve existir").isNotNull();
            xmlValido = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Deve validar com sucesso uma DPS 100% conforme o schema XSD v1.00 oficial")
    void deveValidarDpsComSucessoContraXsdV100() {
        List<String> erros = validador.validarDps(xmlValido);

        assertThat(erros)
                .as("DPS válida não deve apresentar nenhum erro de validação contra o XSD v1.00")
                .isEmpty();
    }

    @Test
    @DisplayName("Deve validar com sucesso sem lançar exceção ao chamar validarDpsOuFalhar")
    void deveValidarDpsOuFalharSemExcecaoParaXmlValido() {
        validador.validarDpsOuFalhar(xmlValido);
    }

    @Test
    @DisplayName("Deve rejeitar DPS sem o campo obrigatório cTribNac detalhando o elemento ausente")
    void deveRejeitarDpsSemCampoObrigatorio() {
        String xmlInvalido = xmlValido.replace("<cTribNac>010101</cTribNac>", "");

        List<String> erros = validador.validarDps(xmlInvalido);

        assertThat(erros)
                .as("Deve conter erros informando a ausência do cTribNac")
                .isNotEmpty();
        assertThat(erros.get(0)).contains("cTribNac");
    }

    @Test
    @DisplayName("Deve lançar ValidacaoFiscalException com a lista de erros ao chamar validarDpsOuFalhar")
    void deveLancarValidacaoFiscalExceptionParaXmlInvalido() {
        String xmlInvalido = xmlValido.replace("<cTribNac>010101</cTribNac>", "");

        assertThatThrownBy(() -> validador.validarDpsOuFalhar(xmlInvalido))
                .isInstanceOf(ValidacaoFiscalException.class)
                .hasMessageContaining("cTribNac");
    }

    @Test
    @DisplayName("Deve rejeitar DPS com formato inválido de CNPJ no prestador")
    void deveRejeitarDpsComCnpjInvalido() {
        String xmlInvalido = xmlValido.replace("<CNPJ>36030278000159</CNPJ>", "<CNPJ>123</CNPJ>");

        List<String> erros = validador.validarDps(xmlInvalido);

        assertThat(erros)
                .as("Deve acusar violação de padrão regex no CNPJ do prestador")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar XML malformado ou nulo")
    void deveRejeitarXmlNuloOuMalformado() {
        List<String> errosNulo = validador.validarDps(null);
        assertThat(errosNulo).isNotEmpty();

        List<String> errosVazio = validador.validarDps("   ");
        assertThat(errosVazio).isNotEmpty();

        List<String> errosMalformado = validador.validarDps("<DPS><infDPS>aberto");
        assertThat(errosMalformado).isNotEmpty();
    }
}
