package br.com.heviane.nfse.adapters.out.sefin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Cache LRU (Least Recently Used) thread-safe e limitado em capacidade,
 * para evitar crescimento indefinido de instâncias pesadas (como SSLContext e HttpClient).
 */
public class BoundedLruCache<K, V> {

    private final int maxCapacity;
    private final Map<K, V> map;

    public BoundedLruCache(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacidade máxima do cache deve ser maior que zero.");
        }
        this.maxCapacity = maxCapacity;
        this.map = new LinkedHashMap<K, V>(maxCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "Função de mapeamento não pode ser nula");
        V val = map.get(key);
        if (val == null) {
            val = mappingFunction.apply(key);
            if (val != null) {
                map.put(key, val);
            }
        }
        return val;
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
