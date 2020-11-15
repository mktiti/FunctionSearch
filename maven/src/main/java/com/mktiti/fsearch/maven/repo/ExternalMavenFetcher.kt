package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.cutLast
import java.io.File
import java.io.IOException

class ExternalMavenFetcher(
        private val basePath: File = tempDir("fsearch-external-maven-")
) : MavenFetcher {

    companion object {

        private val mavenCommand = listOf("mvn",  "dependency:copy-dependencies", "-DmarkersDirectory=\$markers", "-DoutputDirectory=\$repo", "-Dmdep.prependGroupId")

        private fun tempDir(prefix: String, suffix: String? = null, directory: File? = null) = createTempDir(prefix, suffix, directory).apply {
            deleteOnExit()
        }

        private fun String.replaceAllFirsts(vararg variables: Pair<String, String>): String = replaceAllFirsts(variables.toMap())

        private fun String.replaceAllFirsts(variables: Map<String, String>): String {
            return variables.entries.fold(this) { acc, (k, v) ->
                acc.replaceFirst(k, v)
            }
        }

    }

    private fun parseFilename(full: String): ArtifactId? {
        val dashParts = full.removeSuffix(".jar").split('-')
        if (dashParts.size < 2) return null

        val version = dashParts.last()
        val periodParts = dashParts.dropLast(1).joinToString(separator = "-").split('.')
        return if (periodParts.size < 2) {
            null
        } else {
            val (group, name) = periodParts.cutLast()
            ArtifactId(
                    group = group,
                    name = name,
                    version = version
            )
        }
    }

    override fun <R> runOnArtifactWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R? {
        return try {
            val combined = tempDir(prefix = "combined-", directory = basePath.absoluteFile)
            val project = tempDir(prefix = "project-", directory = combined.absoluteFile)
            val repo = tempDir(prefix = "repo-", directory = combined.absoluteFile)
            val markers = tempDir(prefix = "markers-", directory = combined.absoluteFile)

            project.resolve("pom.xml").outputStream().use { out ->
                MockPomHandler.createMockPom(artifacts, out)
            }

            val command = mavenCommand.map {
                it.replaceAllFirsts(
                        "\$markers" to markers.absolutePath, "\$repo" to repo.absolutePath
                )
            }

            println(">>> Starting external maven process")
            val process = try {
                ProcessBuilder(command)
                        .directory(project.absoluteFile)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                return null
            }

            try {
                if (process.waitFor() == 0) {
                    println(">>> External maven process finished")
                    val files = (repo.listFiles() ?: return null).filter { file ->
                        file.extension == "jar"
                    }.mapNotNull { file ->
                        (parseFilename(file.nameWithoutExtension) ?: return@mapNotNull null) to file
                    }.toMap()

                    transform(files)
                } else {
                    return null
                }
            } finally {
                process.destroyForcibly()
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }
    }

}