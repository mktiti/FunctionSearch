package com.mktiti.fsearchtest;

public class FtPredicates {

    // Based on guava predicates
    public static <T> FtPredicate<T> and(FtCollection<? extends FtPredicate<? super T>> components) {
        return null;
    }

    // Based on guava predicates
    public static <T> FtPredicate<T> andArr(FtPredicate<? super T>... components) {
        return null;
    }

    public static <T> FtPredicate<T> alwaysTrue() {
        return null;
    }

    public static <T> FtPredicate<T> alwaysFalse() {
        return null;
    }

}