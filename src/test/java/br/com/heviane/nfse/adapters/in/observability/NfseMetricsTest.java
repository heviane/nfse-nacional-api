package br.com.heviane.nfse.adapters.in.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NfseMetricsTest {

    private MeterRegistry registry;
    private NfseMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new NfseMetrics(registry);
    }

    @Test
    @DisplayName("Deve registrar métricas de emissão com sucesso")
    void deveRegistrarEmissaoSucesso() {
        metrics.registrarEmissao("REST", true, false, 120L);

        Counter counter = registry.find("nfse.emissao.requests")
                .tag("origem", "REST")
                .tag("resultado", "sucesso")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        Timer timer = registry.find("nfse.emissao.tempo")
                .tag("origem", "REST")
                .tag("resultado", "sucesso")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve registrar métricas de emissão com rejeição e falha")
    void deveRegistrarEmissaoRejeicaoEFalha() {
        metrics.registrarEmissao("SOAP", false, true, 80L);
        metrics.registrarEmissao("SOAP", false, false, 50L);

        Counter counterRejeicao = registry.find("nfse.emissao.requests")
                .tag("origem", "SOAP")
                .tag("resultado", "rejeicao")
                .counter();

        assertThat(counterRejeicao).isNotNull();
        assertThat(counterRejeicao.count()).isEqualTo(1.0);

        Counter counterErro = registry.find("nfse.emissao.requests")
                .tag("origem", "SOAP")
                .tag("resultado", "erro")
                .counter();

        assertThat(counterErro).isNotNull();
        assertThat(counterErro.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Deve registrar métricas de consulta de NFS-e")
    void deveRegistrarConsulta() {
        metrics.registrarConsulta(true, true, 95L);

        Counter counter = registry.find("nfse.consulta.requests")
                .tag("resultado", "encontrada")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Deve registrar métricas de cancelamento de NFS-e")
    void deveRegistrarCancelamento() {
        metrics.registrarCancelamento(true, false, 110L);

        Counter counter = registry.find("nfse.cancelamento.requests")
                .tag("resultado", "sucesso")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Não deve quebrar se o registry for nulo")
    void naoDeveQuebrarComRegistryNulo() {
        NfseMetrics nullMetrics = new NfseMetrics(null);
        nullMetrics.registrarEmissao("REST", true, false, 100L);
        nullMetrics.registrarConsulta(true, true, 50L);
        nullMetrics.registrarCancelamento(true, false, 80L);
    }
}
