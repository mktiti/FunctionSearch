package com.mktiti.fsearch.build.integrationtest

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.absolutePathString

object CompilerUtil {

    fun <T> withCompiled(location: Path, onPackage: (Path) -> T): T {
        val compiler = ToolProvider.getSystemJavaCompiler()

        val javaFiles = location.toFile().walk().filter {
            it.extension == "java"
        }.toList()

        val outDir = Files.createTempDirectory("fsearch-test-compile-out").apply {
            toFile().deleteOnExit()
        }

        with(compiler.getStandardFileManager(null, null, null)) {
            val javaFileObjects = getJavaFileObjectsFromFiles(javaFiles)
            compiler.getTask(null, this, null, listOf("-d", outDir.absolutePathString()), null, javaFileObjects).call()
        }

        return onPackage(outDir).also {
            try {
                outDir.toFile().deleteRecursively()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }
    }

}