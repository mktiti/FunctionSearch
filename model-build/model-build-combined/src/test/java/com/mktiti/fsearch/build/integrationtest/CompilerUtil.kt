package com.mktiti.fsearch.build.integrationtest

import java.io.IOException
import java.nio.file.Path
import javax.tools.ToolProvider

object CompilerUtil {

    fun <T> withCompiled(location: Path, onPackage: (Path) -> T): T {
        val compiler = ToolProvider.getSystemJavaCompiler()

        val javaFiles = location.toFile().walk().filter {
            it.extension == "java"
        }.toList()

        val outDir = createTempDir("fsearch-test-compile-out").apply {
            deleteOnExit()
        }

        with(compiler.getStandardFileManager(null, null, null)) {
            val javaFileObjects = getJavaFileObjectsFromFiles(javaFiles)
            compiler.getTask(null, this, null, listOf("-d", outDir.absolutePath), null, javaFileObjects).call()
        }

        return onPackage(outDir.toPath()).also {
            try {
                outDir.deleteRecursively()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }
    }

}