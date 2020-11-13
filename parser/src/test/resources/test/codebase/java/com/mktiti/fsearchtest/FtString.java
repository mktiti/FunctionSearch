package com.mktiti.fsearchtest;

public interface FtString extends FtComp<FtString> {

    int length();

    FtString substring(int start, int end);

}
