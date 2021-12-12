package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.util.logger
import java.io.File
import java.io.IOException
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path

object IoUtil {

    private val log = logger()

    private fun setupTempDir(creator: () -> Path) = creator().apply {
        toFile().deleteOnExit()
    }

    fun tempDir(prefix: String) = setupTempDir {
        createTempDirectory(prefix)
    }

    fun tempDir(prefix: String, directory: Path) = setupTempDir {
        createTempDirectory(directory, prefix)
    }

    fun <T> runCommand(command: List<String>, dir: File, redirect: ProcessBuilder.Redirect, code: () -> T): T? {
        return try {
            val process = try {
                ProcessBuilder(command)
                        .directory(dir.absoluteFile)
                        .redirectOutput(redirect)
                        .redirectError(redirect)
                        .start()
            } catch (ioe: IOException) {
                log.error("IOException while running external command", ioe)
                return null
            }

            return try {
                if (process.waitFor() == 0) {
                    code()
                } else {
                    return null
                }
            } finally {
                process.destroyForcibly()
            }
        } catch (ioe: IOException) {
            log.error("IOException while waiting for/stopping external command", ioe)
            null
        }
    }

}