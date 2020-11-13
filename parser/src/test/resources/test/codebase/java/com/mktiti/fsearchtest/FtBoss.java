package com.mktiti.fsearchtest;

public interface FtBoss extends FtPerson{

    FtString title();

    FtCollection<FtPerson> workers();

}
