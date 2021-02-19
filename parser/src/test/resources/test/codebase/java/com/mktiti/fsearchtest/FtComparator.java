package com.mktiti.fsearchtest;

// Functional Interface
public interface FtComparator<T> {

    // SAM
    int compare(T a, T b);

}
