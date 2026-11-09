package br.com.heviane.nfse.adapters.in.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Coletor e gerenciador de métricas operacionais para Actuator e Prometheus via Micrometer.
 * <p>
 * Monitora volumetria, taxas de sucesso/rejeição e tempos de resposta das operações fiscais.
 */
@Component
public class NfseMetrics {

    private final MeterRegistry registry;

    @Autowired
    public NfseMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registra métricas de uma tentativa de emissão de DPS (REST ou SOAP).
     *
     * @param origem REST ou SOAP
     * @param sucesso se a DPS foi autorizada com sucesso (status 100)
     * @param rejeicao se o Sefin rejeitou a DPS com código fiscal != 100
     * @param duracaoMs tempo total de processamento em milissegundos
     */
    public void registrarEmissao(String origem, boolean sucesso, boolean rejeicao, long duracaoMs) {
        if (registry == null) return;

        String resultado = sucesso ? "sucesso" : (rejeicao ? "rejeicao" : "erro");

        Counter.builder("nfse.emissao.requests")
                .description("Total de requisições de emissão de DPS processadas")
                .tag("origem", origem != null ? origem.toUpperCase() : "DESCONHECIDA")
                .tag("resultado", resultado)
                .register(registry)
                .increment();

        Timer.builder("nfse.emissao.tempo")
                .description("Tempo de processamento das requisições de emissão de DPS")
                .tag("origem", origem != null ? origem.toUpperCase() : "DESCONHECIDA")
                .tag("resultado", resultado)
                .register(registry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Registra métricas de uma consulta de NFS-e.
     */
    public void registrarConsulta(boolean sucesso, boolean encontrada, long duracaoMs) {
        if (registry == null) return;

        String resultado = !sucesso ? "erro" : (encontrada ? "encontrada" : "nao_encontrada");

        Counter.builder("nfse.consulta.requests")
                .description("Total de requisições de consulta de NFS-e processadas")
                .tag("resultado", resultado)
                .register(registry)
                .increment();

        Timer.builder("nfse.consulta.tempo")
                .description("Tempo de processamento das consultas de NFS-e")
                .tag("resultado", resultado)
                .register(registry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Registra métricas de um registro de evento de cancelamento de NFS-e.
     */
    public void registrarCancelamento(boolean sucesso, boolean rejeicao, long duracaoMs) {
        if (registry == null) return;

        String resultado = sucesso ? "sucesso" : (rejeicao ? "rejeicao" : "erro");

        Counter.builder("nfse.cancelamento.requests")
                .description("Total de requisições de cancelamento de NFS-e processadas")
                .tag("resultado", resultado)
                .register(registry)
                .increment();

        Timer.builder("nfse.cancelamento.tempo")
                .description("Tempo de processamento dos pedidos de cancelamento de NFS-e")
                .tag("resultado", resultado)
                .register(registry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }
}
