package com.mktiti.fsearch.parser.parse

import java.nio.file.Path

data class JarInfo(
        val name: String,
        val paths: Collection<Path>
) {

    companion object {
        fun single(path: Path) = JarInfo(
                name = path.fileName.toString(),
                paths = listOf(path)
        )
    }

}