package br.com.heviane.nfse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Classe principal de inicialização da API NFS-e Nacional.
 * 
 * Estende {@link SpringBootServletInitializer} para suportar o modelo híbrido de execução:
 * 1. Em desenvolvimento local: execução standalone com Tomcat embutido (main).
 * 2. Em produção: deploy em formato WAR no Tomcat gerenciado da BrasilCloud (configure).
 */
@SpringBootApplication
public class NfseApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NfseApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NfseApplication.class, args);
    }
}
