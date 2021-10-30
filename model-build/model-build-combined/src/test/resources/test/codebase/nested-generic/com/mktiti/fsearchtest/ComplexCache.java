package com.mktiti.fsearchtest;

/**
 * Modelled after guava's LocalCache
 * @param <K> Key type
 * @param <V> Value type
 */
class ComplexCache<K, V> {

    abstract class HashIterator<T> implements Iterator<T> {

        int field;

        @Override
        public abstract T next();

        public abstract boolean hasNext();

    }

    final class EntryIterator extends HashIterator<Entry<K, V>> {

        @Override
        public Entry<K, V> next() {
            return null;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

    }

    static class Asd {}

    static class Dsa {

        public Dsa(ComplexCache ignored) {}

    }

    public V get(K key) {
        return null;
    }

}
