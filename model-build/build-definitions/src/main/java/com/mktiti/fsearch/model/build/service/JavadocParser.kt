package com.mktiti.fsearch.model.build.service

import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import java.nio.file.Path

interface JavadocParser<I> {

    fun parseInput(input: I): FunDocMap?

}

interface JarHtmlJavadocParser : JavadocParser<Path> {

    fun parseJar(jarPath: Path): FunDocMap?

    override fun parseInput(input: Path): FunDocMap? = parseJar(input)

}