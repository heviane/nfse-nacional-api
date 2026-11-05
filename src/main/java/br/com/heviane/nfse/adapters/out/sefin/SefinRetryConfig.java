package br.com.heviane.nfse.adapters.out.sefin;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuração de resiliência e política de retentativas (retries) para o {@link SefinClientAdapter}.
 * <p>
 * Define o número máximo de tentativas e o cálculo de backoff exponencial para falhas transitórias
 * de rede (I/O) e indisponibilidades momentâneas dos servidores do Sefin Nacional / ADN (HTTP 502/503/504).
 *
 * @param maxTentativas número total de tentativas (1 tentativa inicial + retentativas)
 * @param delayInicial  tempo de espera antes da primeira retentativa
 * @param multiplicador fator de multiplicação aplicado ao delay a cada retentativa subsequente
 */
public record SefinRetryConfig(
        int maxTentativas,
        Duration delayInicial,
        double multiplicador
) {
    public SefinRetryConfig {
        if (maxTentativas < 1) {
            throw new IllegalArgumentException("Número máximo de tentativas deve ser no mínimo 1.");
        }
        Objects.requireNonNull(delayInicial, "Delay inicial não pode ser nulo.");
        if (multiplicador < 1.0) {
            throw new IllegalArgumentException("Multiplicador de backoff deve ser maior ou igual a 1.0.");
        }
    }

    /**
     * Política padrão de produção: 3 tentativas com backoff exponencial inicial de 1 segundo (1x, 2x...).
     */
    public static SefinRetryConfig padrao() {
        return new SefinRetryConfig(3, Duration.ofSeconds(1), 2.0);
    }

    /**
     * Política sem espera (delay zero), ideal para suítes de testes unitários automatizados rápidos.
     */
    public static SefinRetryConfig semEspera(int maxTentativas) {
        return new SefinRetryConfig(maxTentativas, Duration.ZERO, 1.0);
    }

    /**
     * Política desabilitada: executa apenas uma tentativa sem nenhum retry.
     */
    public static SefinRetryConfig desabilitado() {
        return new SefinRetryConfig(1, Duration.ZERO, 1.0);
    }
}

