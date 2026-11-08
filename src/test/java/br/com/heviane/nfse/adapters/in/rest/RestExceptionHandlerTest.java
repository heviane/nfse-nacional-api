package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.ErroResponse;
import br.com.heviane.nfse.core.domain.AssinaturaDigitalException;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;
import br.com.heviane.nfse.core.domain.ValidacaoFiscalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários: RestExceptionHandler")
class RestExceptionHandlerTest {

    private RestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
    }

    @Test
    @DisplayName("Deve mapear ValidacaoFiscalException para HTTP 422 Unprocessable Entity")
    void deveMapearValidacaoFiscalPara422() {
        ValidacaoFiscalException ex = new ValidacaoFiscalException("Erro XSD", List.of("Campo obrigatório ausente"));
        ResponseEntity<ErroResponse> response = handler.handleValidacaoFiscal(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(422);
        assertThat(response.getBody().detalhes()).contains("Campo obrigatório ausente");
    }

    @Test
    @DisplayName("Deve mapear SefinTransmissaoException com timeout (504) para HTTP 504 Gateway Timeout")
    void deveMapearTimeoutPara504() {
        SefinTransmissaoException ex = new SefinTransmissaoException("Timeout no Sefin", 504, null, "timeout");
        ResponseEntity<ErroResponse> response = handler.handleSefinTransmissao(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(504);
    }

    @Test
    @DisplayName("Deve mapear SefinTransmissaoException com duplicidade (status 204) para HTTP 409 Conflict")
    void deveMapearDuplicidadePara409() {
        SefinTransmissaoException ex = new SefinTransmissaoException("Duplicidade de DPS", 200, 204, "{\"codigoStatus\":204}");
        ResponseEntity<ErroResponse> response = handler.handleSefinTransmissao(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(409);
    }

    @Test
    @DisplayName("Deve mapear SefinTransmissaoException com erro 400 do Sefin para HTTP 400 Bad Request")
    void deveMapearErro400ParaBadRequest() {
        SefinTransmissaoException ex = new SefinTransmissaoException("Bad Request no Sefin", 400, null, "erro");
        ResponseEntity<ErroResponse> response = handler.handleSefinTransmissao(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve mapear SefinTransmissaoException com erro 500 do Sefin para HTTP 502 Bad Gateway")
    void deveMapearErro500ParaBadGateway() {
        SefinTransmissaoException ex = new SefinTransmissaoException("Internal Server Error no Sefin", 500, null, "erro");
        ResponseEntity<ErroResponse> response = handler.handleSefinTransmissao(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(502);
    }

    @Test
    @DisplayName("Deve mapear AssinaturaDigitalException para HTTP 400 Bad Request")
    void deveMapearAssinaturaDigitalPara400() {
        AssinaturaDigitalException ex = new AssinaturaDigitalException("Erro de assinatura", new RuntimeException());
        ResponseEntity<ErroResponse> response = handler.handleAssinaturaDigital(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve mapear SecurityException para HTTP 403 Forbidden")
    void deveMapearSecurityExceptionPara403() {
        SecurityException ex = new SecurityException("Acesso negado");
        ResponseEntity<ErroResponse> response = handler.handleSecurity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(403);
    }

    @Test
    @DisplayName("Deve mapear IllegalArgumentException para HTTP 400 Bad Request")
    void deveMapearIllegalArgumentPara400() {
        IllegalArgumentException ex = new IllegalArgumentException("Parâmetro inválido");
        ResponseEntity<ErroResponse> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve mapear Exception genérica não tratada para HTTP 500")
    void deveMapearExceptionGenericaPara500() {
        Exception ex = new RuntimeException("Erro inesperado");
        ResponseEntity<ErroResponse> response = handler.handleGeneralException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusHttp()).isEqualTo(500);
    }
}
