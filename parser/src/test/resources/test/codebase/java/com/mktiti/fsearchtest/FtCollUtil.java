package com.mktiti.fsearchtest;

public interface FtCollUtil {

    static <T extends FtComp<T>> T max(FtList<T> list) {
        return null;
    }

    static <T extends FtComp<T>> T min(FtList<T> list) {
        return null;
    }

    static <T> T max(FtList<T> list, FtComparator<? super T> comparator) {
        return null;
    }

    static <T> T min(FtList<T> list, FtComparator<? super T> comparator) {
        return null;
    }

    static <T> FtCollection<FtList<T>> permutations(FtCollection<T> coll) {
        return null;
    }

    static <T> FtList<FtList<T>> chunks(FtList<T> list, int size) {
        return null;
    }

}
