package br.com.heviane.nfse.core.services;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuração de polling e resolução transparente para respostas assíncronas do Sefin Nacional (status 105).
 *
 * @param maxTentativas número de tentativas de consulta à chave gerada antes de retornar o status pendente
 * @param intervalo     intervalo de espera entre cada consulta
 */
public record AsyncPollingConfig(
        int maxTentativas,
        Duration intervalo
) {
    public AsyncPollingConfig {
        if (maxTentativas < 0) {
            throw new IllegalArgumentException("Número de tentativas não pode ser negativo.");
        }
        Objects.requireNonNull(intervalo, "Intervalo de polling não pode ser nulo.");
    }

    /**
     * Política padrão: até 3 tentativas de consulta com intervalo de 1.5 segundos.
     */
    public static AsyncPollingConfig padrao() {
        return new AsyncPollingConfig(3, Duration.ofMillis(1500));
    }

    /**
     * Política sem espera (delay zero), ideal para testes unitários rápidos.
     */
    public static AsyncPollingConfig semEspera(int maxTentativas) {
        return new AsyncPollingConfig(maxTentativas, Duration.ZERO);
    }

    /**
     * Desabilita a tentativa de resolução imediata via polling, retornando diretamente a resposta assíncrona.
     */
    public static AsyncPollingConfig desabilitado() {
        return new AsyncPollingConfig(0, Duration.ZERO);
    }
}

