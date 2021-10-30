package com.mktiti.fsearchtest;

public interface FtComp<C extends FtComp<C>> {

    int compare(C other);

}
