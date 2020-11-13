package com.mktiti.fsearchtest;

public interface FtFunction<T, R> {

    static <T, I, R> FtFunction<T, R> combine(FtFunction<? super R, I> inner, FtFunction<? super I, T> outer) {
        return null;
    }

    default <CR> FtFunction<T, CR> then(FtFunction<? super R, CR> next) {
        return null;
    }

    R apply(T input);

}
