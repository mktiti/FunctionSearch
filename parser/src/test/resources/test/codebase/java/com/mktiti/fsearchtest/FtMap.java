package com.mktiti.fsearchtest;

public interface FtMap<K, V> extends FtCollection<FtPair<K, V>> {

    void put(K key, V value);

    V get(K key);

    FtOptional<V> safeGet(K key);

    V getOr(K key, V def);

    V getOr(K key, FtFunction<? super K, ? extends V> creator);

}
