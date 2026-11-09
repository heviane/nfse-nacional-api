package br.com.heviane.nfse.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("Testes de Integração: Desativação do Swagger e OpenAPI em Produção")
class SwaggerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve retornar HTTP 404 para /v3/api-docs quando OpenAPI estiver desabilitado")
    void deveRetornar404ParaApiDocsQuandoDesabilitado() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 para /swagger-ui/index.html quando Swagger UI estiver desabilitado")
    void deveRetornar404ParaSwaggerUiQuandoDesabilitado() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }
}
