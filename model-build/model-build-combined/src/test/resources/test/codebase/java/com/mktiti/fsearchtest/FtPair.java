package com.mktiti.fsearchtest;

public interface FtPair<F, S> {

    static <F, S> FtPair<F, S> of(F first, S second) {
        return null;
    }

    F first();

    S second();

}
