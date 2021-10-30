package com.mktiti.fsearchtest;

public interface FtOptional<T> {

    boolean isPresent();

    boolean isEmpty();

    T force();

    <R> FtOptional<R> map(FtFunction<? super T, ? extends R> mapper);

    <R> FtOptional<R> flat(FtFunction<? super T, FtOptional<? extends R>> mapper);

}
