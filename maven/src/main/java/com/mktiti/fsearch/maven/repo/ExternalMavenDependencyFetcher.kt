package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.util.IoUtil
import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.maven.util.parseDependencyTgfGraph
import com.mktiti.fsearch.modules.ArtifactDependencyFetcher
import com.mktiti.fsearch.modules.ArtifactId
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.IOException
import java.nio.file.Path

class ExternalMavenDependencyFetcher(
        private val basePath: Path = IoUtil.tempDir("fsearch-external-maven-info-")
) : ArtifactDependencyFetcher {

    companion object {

        private fun depTreeCommand(output: File): List<String> {
            return listOf("mvn", "dependency:tree", "-DoutputFile=${output.absolutePath}", "-DoutputType=tgf", "-DoutputEncoding=UTF-8")
        }

    }

    private val log = logger()

    override fun dependencies(artifact: ArtifactId): Set<ArtifactId>? {
        return dependencies(listOf(artifact))?.get(artifact)
    }

    override fun dependencies(artifacts: Collection<ArtifactId>): Map<ArtifactId, Set<ArtifactId>>? {
        if (artifacts.isEmpty()) {
            return emptyMap()
        }

        return try {
            val project = IoUtil.tempDir(prefix = "project-", directory = basePath).toFile()
            try {
                val processOuts = ProcessBuilder.Redirect.to(project.resolve("mvn-output.txt"))

                project.resolve("pom.xml").outputStream().use { out ->
                    MockPomHandler.createMockPom(artifacts, out)
                }

                val treeOut = project.resolve("dependency-tree.tgf")
                val treeCommand = depTreeCommand(treeOut)

                IoUtil.runCommand(treeCommand, project, processOuts) {
                    log.debug("External maven dependency tree process finished")
                    parseDependencyTgfGraph(treeOut.toPath())
                }?.filterKeys {
                    it != MockPomHandler.mockArtifactId
                }?.mapValues { (_, depInfo) ->
                    depInfo.map { it.dependency }.toSet()
                } ?: emptyMap()

            } finally {
                project.deleteRecursively()
            }
        } catch (ioe: IOException) {
            log.error("IOException while fetching artifact dependency info", ioe)
            null
        }
    }

}