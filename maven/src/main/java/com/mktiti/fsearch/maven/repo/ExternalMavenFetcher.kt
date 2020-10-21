package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.ArtifactRepo
import com.mktiti.fsearch.maven.MavenArtifact
import com.mktiti.fsearch.util.cutLast
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class ExternalMavenFetcher(
        private val basePath: File = tempDir("fsearch-external-maven-")
) : RepoArtifactFetch {

    companion object {
        private const val pomTemplateLoc = "/test-pom.xml"

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

    private val pomTemplate = ExternalMavenFetcher::class.java.getResource(pomTemplateLoc).readText()

    private fun parseFilename(full: String): MavenArtifact? {
        val dashParts = full.removeSuffix(".jar").split('-')
        if (dashParts.size < 2) return null

        val version = dashParts.last()
        val periodParts = dashParts.dropLast(1).joinToString(separator = "-").split('.')
        return if (periodParts.size < 2) {
            null
        } else {
            val (group, name) = periodParts.cutLast()
            MavenArtifact(
                    group = group,
                    name = name,
                    version = version
            )
        }
    }

    override fun fetchArtifactWithDeps(info: MavenArtifact, transform: (MavenArtifact, File) -> ArtifactRepo): Map<MavenArtifact, ArtifactRepo>? {
        return try {
            val combined = tempDir(prefix = "combined-", directory = basePath.absoluteFile)
            val project = tempDir(prefix = "project-", directory = combined.absoluteFile)
            val repo = tempDir(prefix = "repo-", directory = combined.absoluteFile)
            val markers = tempDir(prefix = "markers-", directory = combined.absoluteFile)

            val pomFile = project.resolve("pom.xml")
            val pomContent = pomTemplate.replaceAllFirsts(
                "\$groupId" to info.group.joinToString(separator = "."),
                "\$artifactId" to info.name,
                "\$version" to info.version
            )

            pomFile.absoluteFile.writeText(pomContent, StandardCharsets.UTF_8)

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
                    repo.listFiles()?.mapNotNull { file ->
                        if (file.extension == "jar") {
                            val depInfo = parseFilename(file.nameWithoutExtension) ?: return@mapNotNull null
                            depInfo to transform(depInfo, file)
                        } else {
                            null
                        }
                    }?.toMap()
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