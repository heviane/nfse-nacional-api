package br.com.heviane.nfse.adapters.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador de teste de fumaça e verificação de saúde da API.
 * 
 * Permite que balanceadores de carga, scripts de deploy e o desenvolvedor
 * verifiquem se a aplicação subiu com sucesso tanto no Tomcat embutido local
 * quanto no Tomcat gerenciado na BrasilCloud.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "nfse-nacional-api",
                "version", "0.1.0-SNAPSHOT"
        ));
    }
}

