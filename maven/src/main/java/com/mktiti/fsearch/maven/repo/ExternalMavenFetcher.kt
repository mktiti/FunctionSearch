package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.maven.util.DependencyUtil
import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.modules.ArtifactId
import java.io.File
import java.io.IOException

class ExternalMavenFetcher(
        private val basePath: File = tempDir("fsearch-external-maven-")
) : MavenFetcher {

    companion object {

        private fun fetchCommand(outputDir: File, markerDir: File, classifier: String?): List<String> {
            val base = listOf("mvn",  "dependency:copy-dependencies", "-DmarkersDirectory=${markerDir.absoluteFile}", "-DoutputDirectory=${outputDir.absolutePath}", "-Dmdep.prependGroupId")
            return if (classifier == null) {
                base
            } else {
                base + "-Dclassifier=$classifier"
            }
        }

        private fun listCommand(output: File, classifier: String?): List<String> {
            val base = listOf("mvn", "dependency:list", "-DoutputFile=${output.absolutePath}")
            return if (classifier == null) {
                base
            } else {
                base + "-Dclassifier=$classifier"
            }
        }

        private fun tempDir(prefix: String, suffix: String? = null, directory: File? = null) = createTempDir(prefix, suffix, directory).apply {
            deleteOnExit()
        }

        private fun parseListOutput(lines: Sequence<String>): List<ArtifactId>? {
            return lines.map { it.trim() }.filter { it.isNotEmpty() }.filterNot {
                it == "The following files have been resolved:"
            }.takeWhile {
                it != "The following files have NOT been resolved:"
            }.map {
                DependencyUtil.parseListedArtifact(it)
            }.toList().liftNull()
        }

    }

    private fun <T> runCommand(command: List<String>, dir: File, code: () -> T): T? {
        return try {
            val process = try {
                ProcessBuilder(command)
                        .directory(dir.absoluteFile)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
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
            ioe.printStackTrace()
            null
        }
    }

    private fun <T> onDependencies(artifacts: Collection<ArtifactId>, classifier: String?, transform: (files: Map<ArtifactId, File>) -> T): T? {
        val combined = tempDir(prefix = "combined-", directory = basePath.absoluteFile)
        return try {
            val project = tempDir(prefix = "project-", directory = combined.absoluteFile)
            val repo = tempDir(prefix = "repo-", directory = combined.absoluteFile)
            val markers = tempDir(prefix = "markers-", directory = combined.absoluteFile)

            project.resolve("pom.xml").outputStream().use { out ->
                MockPomHandler.createMockPom(artifacts, out)
            }

            val listOut = project.resolve("dependencies.txt")
            val list = listCommand(listOut, classifier)
            val depList = runCommand(list, project) {
                println(">>> External maven dependency list process finished")
                listOut.useLines {
                    parseListOutput(it)
                }
            } ?: return null

            val copy = fetchCommand(repo, markers, classifier)
            val files = runCommand(copy, project) {
                println(">>> External maven dependency fetch process finished")

                depList.map {
                    it to repo.resolve(DependencyUtil.shortFilenameForArtifact(it, classifier) + ".jar")
                }.filter { (_, file) ->
                    file.exists()
                }.toMap()
            } ?: return null

            transform(files)

        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        } finally {
            combined.deleteRecursively()
        }
    }

    override fun <R> runOnArtifactWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R? {
        return onDependencies(artifacts = artifacts, classifier = null, transform = transform)
    }

    override fun <R> runOnJavadocWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R? {
        return onDependencies(artifacts = artifacts, classifier = "javadoc", transform = transform)
    }

}