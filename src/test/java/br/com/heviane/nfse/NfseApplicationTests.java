package br.com.heviane.nfse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NfseApplicationTests {

    @Test
    @DisplayName("Deve carregar o contexto da aplicação Spring Boot sem falhas")
    void contextLoads() {
        // Teste de fumaça (Smoke Test): garante que todos os beans e configurações básicas iniciam com sucesso.
    }
}

