package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.safeCutHead
import com.mktiti.fsearch.util.safeCutLast
import java.io.File
import java.io.IOException

class ExternalMavenFetcher(
        private val basePath: File = tempDir("fsearch-external-maven-")
) : MavenFetcher {

    companion object {

        private val fetchCommand = listOf("mvn",  "dependency:copy-dependencies", "-DmarkersDirectory=\$markers", "-DoutputDirectory=\$repo", "-Dmdep.prependGroupId")
        private val javadocFetchCommand = fetchCommand + "-Dclassifier=javadoc"

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
        val dashParts = full.split('-')
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

    private fun parseJavadocFilename(full: String): ArtifactId? {
        val (groupAndName, versionList) = full.removeSuffix("-javadoc").split('-').safeCutHead() ?: return null

        val version = versionList.joinToString(separator = "-")
        val (groupList, name) = groupAndName.split('.').safeCutLast() ?: return null
        return ArtifactId(
                group = groupList,
                name = name,
                version = version
        )
    }

    private fun <T> runCommand(artifacts: Collection<ArtifactId>, command: List<String>, code: (repo: File) -> T): T? {
        val combined = tempDir(prefix = "combined-", directory = basePath.absoluteFile)
        return try {
            val project = tempDir(prefix = "project-", directory = combined.absoluteFile)
            val repo = tempDir(prefix = "repo-", directory = combined.absoluteFile)
            val markers = tempDir(prefix = "markers-", directory = combined.absoluteFile)

            project.resolve("pom.xml").outputStream().use { out ->
                MockPomHandler.createMockPom(artifacts, out)
            }

            val replacedCommand = command.map {
                it.replaceAllFirsts(
                        "\$markers" to markers.absolutePath, "\$repo" to repo.absolutePath
                )
            }

            val process = try {
                ProcessBuilder(replacedCommand)
                        .directory(project.absoluteFile)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                return null
            }

            return try {
                if (process.waitFor() == 0) {
                    code(repo)
                } else {
                    return null
                }
            } finally {
                process.destroyForcibly()
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        } finally {
            combined.deleteRecursively()
        }
    }

    override fun <R> runOnArtifactWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R? {
        return runCommand(artifacts, fetchCommand) { repo ->
            println(">>> External maven process finished")
            val files = (repo.listFiles() ?: return@runCommand null).filter { file ->
                file.extension == "jar"
            }.mapNotNull { file ->
                (parseFilename(file.nameWithoutExtension) ?: return@mapNotNull null) to file
            }.toMap()

            transform(files)
        }
    }

    override fun <R> runOnJavadocWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R? {
        return runCommand(artifacts, javadocFetchCommand) { repo ->
            println(">>> External javadoc maven process finished")
            val files = (repo.listFiles() ?: return@runCommand null).filter { file ->
                file.extension == "jar"
            }.mapNotNull { file ->
                (parseJavadocFilename(file.nameWithoutExtension) ?: return@mapNotNull null) to file
            }.toMap()

            transform(files)
        }
    }

}