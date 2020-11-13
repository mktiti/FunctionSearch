package com.mktiti.fsearchtest;

public interface FtList<T> extends FtCollection<T> {

    T first();

    T last();

    T get();

    FtOptional<T> safeGet();

    <R> FtList<R> map(FtFunction<? super T, ? extends R> mapper);

    FtList<T> filter(FtFunction<? super T, Boolean> pred);

    <A, R> R fold(A init, FtBiFun<? super A, ? super T, ? extends R> append);

    <A, R> R foldl(A init, FtBiFun<? super A, ? super T, ? extends R> append);

}
