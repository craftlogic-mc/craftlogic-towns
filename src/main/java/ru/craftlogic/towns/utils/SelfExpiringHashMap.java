package ru.craftlogic.towns.utils;/*
 * Copyright (c) 2017 Pierantonio Cangianiello
 * 
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A thread-safe implementation of a HashMap which entries expires after the specified life time.
 * The life-time can be defined on a per-key basis, or using a default one, that is passed to the 
 * constructor.
 * 
 * @author Pierantonio Cangianiello
 * @param <K> the Key type
 * @param <V> the Value type
 */
public class SelfExpiringHashMap<K, V> implements SelfExpiringMap<K, V> {

    private final Map<K, V> internalMap;

    private final Map<K, ExpiringKey<K>> expiringKeys;

    /**
     * Holds the map keys using the given life time for expiration.
     */
    private final DelayQueue<ExpiringKey<K>> delayQueue = new DelayQueue<>();

    /**
     * The default max life time in milliseconds.
     */
    private final long maxLifeTimeMillis;
    private final Consumer<K> expirationListener;

    public SelfExpiringHashMap(Consumer<K> expirationListener) {
        this.expirationListener = expirationListener;
        this.internalMap = new ConcurrentHashMap<>();
        this.expiringKeys = new WeakHashMap<>();
        this.maxLifeTimeMillis = Long.MAX_VALUE;
    }

    public SelfExpiringHashMap(long defaultMaxLifeTimeMillis, Consumer<K> expirationListener) {
        this.expirationListener = expirationListener;
        this.internalMap = new ConcurrentHashMap<>();
        this.expiringKeys = new WeakHashMap<>();
        this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
    }

    public SelfExpiringHashMap(long defaultMaxLifeTimeMillis, int initialCapacity, Consumer<K> expirationListener) {
        this.internalMap = new ConcurrentHashMap<>(initialCapacity);
        this.expiringKeys = new WeakHashMap<>(initialCapacity);
        this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
        this.expirationListener = expirationListener;
    }

    public SelfExpiringHashMap(long defaultMaxLifeTimeMillis, int initialCapacity, float loadFactor, Consumer<K> expirationListener) {
        this.expirationListener = expirationListener;
        this.internalMap = new ConcurrentHashMap<>(initialCapacity, loadFactor);
        this.expiringKeys = new WeakHashMap<>(initialCapacity, loadFactor);
        this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        cleanup();
        return internalMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        cleanup();
        return internalMap.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        cleanup();
        return internalMap.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object value) {
        cleanup();
        return internalMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        cleanup();
        renewKey((K) key);
        return internalMap.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value) {
        return this.put(key, value, maxLifeTimeMillis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value, long lifeTimeMillis) {
        cleanup();
        ExpiringKey<K> delayedKey = new ExpiringKey<>(key, lifeTimeMillis);
        ExpiringKey<K> oldKey = expiringKeys.put(key, delayedKey);
        if(oldKey != null) {
            expireKey(oldKey);
            expiringKeys.put(key, delayedKey);
        }
        delayQueue.offer(delayedKey);
        return internalMap.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object key) {
        V removedValue = internalMap.remove(key);
        expireKey(expiringKeys.remove(key));
        return removedValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean renewKey(K key) {
        ExpiringKey<K> delayedKey = expiringKeys.get(key);
        if (delayedKey != null) {
            delayedKey.renew();
            return true;
        }
        return false;
    }

    private void expireKey(ExpiringKey<K> delayedKey) {
        if (delayedKey != null) {
            delayedKey.expire();
            cleanup();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        delayQueue.clear();
        expiringKeys.clear();
        internalMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return new HashSet<>(this.internalMap.keySet());
    }

    @Override
    public Collection<V> values() {
        return new ArrayList<>(this.internalMap.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new HashSet<>(this.internalMap.entrySet());
    }

    private void cleanup() {
        ExpiringKey<K> delayedKey = delayQueue.poll();
        while (delayedKey != null) {
            internalMap.remove(delayedKey.getKey());
            expiringKeys.remove(delayedKey.getKey());
            delayedKey = delayQueue.poll();
        }
    }

    private class ExpiringKey<K> implements Delayed {

        private long startTime = System.currentTimeMillis();
        private final long maxLifeTimeMillis;
        private final K key;

        public ExpiringKey(K key, long maxLifeTimeMillis) {
            this.maxLifeTimeMillis = maxLifeTimeMillis;
            this.key = key;
        }

        public K getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ExpiringKey<K> other = (ExpiringKey<K>) obj;
            if (this.key != other.key && (this.key == null || !this.key.equals(other.key))) {
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + (this.key != null ? this.key.hashCode() : 0);
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
        }

        private long getDelayMillis() {
            return (startTime + maxLifeTimeMillis) - System.currentTimeMillis();
        }

        public void renew() {
            startTime = System.currentTimeMillis();
        }

        public void expire() {
            startTime =  System.currentTimeMillis() - maxLifeTimeMillis - 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Delayed that) {
            return Long.compare(this.getDelayMillis(), ((ExpiringKey) that).getDelayMillis());
        }
    }
}
