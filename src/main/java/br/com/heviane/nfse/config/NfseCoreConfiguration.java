package br.com.heviane.nfse.config;

import br.com.heviane.nfse.adapters.in.security.CertificadoProvider;
import br.com.heviane.nfse.adapters.out.sefin.SefinClientAdapter;
import br.com.heviane.nfse.adapters.out.signer.AssinadorDpsService;
import br.com.heviane.nfse.core.ports.in.CancelarNfseUseCase;
import br.com.heviane.nfse.core.ports.in.ConsultarNfseUseCase;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import br.com.heviane.nfse.core.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Injeção de Dependências da Arquitetura Hexagonal.
 * Instancia os adaptadores e casos de uso mantendo o desacoplamento do Core.
 */
@Configuration
@EnableConfigurationProperties(NfseProperties.class)
public class NfseCoreConfiguration {

    @Bean
    public ValidadorSchemaPort validadorSchemaPort() {
        return new ValidadorSchemaService();
    }

    @Bean
    public AssinadorDpsPort assinadorDpsPort() {
        return new AssinadorDpsService();
    }

    @Bean
    public TransmissorSefinPort transmissorSefinPort(AssinadorDpsPort assinadorDpsPort,
                                                     ValidadorSchemaPort validadorSchemaPort,
                                                     ObjectMapper objectMapper) {
        return new SefinClientAdapter(assinadorDpsPort, validadorSchemaPort, objectMapper);
    }

    @Bean
    public EmitirDpsUseCase emitirDpsUseCase(ValidadorSchemaPort validadorSchemaPort,
                                             AssinadorDpsPort assinadorDpsPort,
                                             TransmissorSefinPort transmissorSefinPort) {
        return new EmitirDpsService(validadorSchemaPort, assinadorDpsPort, transmissorSefinPort);
    }

    @Bean
    public ConsultarNfseUseCase consultarNfseUseCase(TransmissorSefinPort transmissorSefinPort) {
        return new ConsultarNfseService(transmissorSefinPort);
    }

    @Bean
    public CancelarNfseUseCase cancelarNfseUseCase(TransmissorSefinPort transmissorSefinPort) {
        return new CancelarNfseService(transmissorSefinPort);
    }

    @Bean
    public CertificadoProvider certificadoProvider() {
        return new CertificadoProvider();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NFS-e Nacional API & Middleware")
                        .version("1.0.0")
                        .description("API REST e Middleware de Integração com o Ambiente Nacional da NFS-e (Sefin Nacional / ADN). " +
                                "Inclui Camada Anticorrupção (ACL) para emulação de WebService SOAP para ERPs legados.")
                        .contact(new Contact().name("Heviane").url("https://github.com/heviane/nfse-nacional-api"))
                        .license(new License().name("Apache 2.0")));
    }
}

