package br.com.heviane.nfse.adapters.out.sefin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes Unitários: BoundedLruCache")
class BoundedLruCacheTest {

    @Test
    @DisplayName("Deve descartar a entrada mais antiga (LRU) quando a capacidade for excedida")
    void deveDescartarEntradaMaisAntigaAoExcederCapacidade() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("k1")).isEqualTo("v1");

        // Acesso a k1 renovou sua recência; ao inserir k3, k2 deve ser descartado
        cache.put("k3", "v3");

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.containsKey("k2")).isFalse();
        assertThat(cache.get("k1")).isEqualTo("v1");
        assertThat(cache.get("k3")).isEqualTo("v3");
    }

    @Test
    @DisplayName("Deve executar computeIfAbsent apenas uma vez para a mesma chave")
    void deveExecutarComputeIfAbsentApenasUmaVez() {
        BoundedLruCache<String, Integer> cache = new BoundedLruCache<>(5);
        int[] invocacoes = new int[1];

        Integer v1 = cache.computeIfAbsent("item", k -> {
            invocacoes[0]++;
            return 42;
        });

        Integer v2 = cache.computeIfAbsent("item", k -> {
            invocacoes[0]++;
            return 99;
        });

        assertThat(v1).isEqualTo(42);
        assertThat(v2).isEqualTo(42);
        assertThat(invocacoes[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve limpar o cache corretamente")
    void deveLimparCache() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");

        cache.clear();

        assertThat(cache.size()).isZero();
        assertThat(cache.containsKey("a")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar capacidade menor ou igual a zero")
    void deveRejeitarCapacidadeInvalida() {
        assertThatThrownBy(() -> new BoundedLruCache<>(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedLruCache<>(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
