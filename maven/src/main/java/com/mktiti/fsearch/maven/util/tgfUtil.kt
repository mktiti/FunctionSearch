package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.safeCutHead
import com.mktiti.fsearch.util.split
import java.io.IOException
import java.nio.file.Path

data class ArtifactDependency(
        val dependency: ArtifactId,
        val scope: String
)

private fun Collection<String>.splitLines(partCount: Int): List<List<String>>? {
    return map {
        it.split(" ").also { parts ->
            if (parts.size != partCount) {
                return null
            }
        }
    }
}

private fun List<String>.int(index: Int): Int? = getOrNull(index)?.toIntOrNull()

fun <N : Any, E : Any> parseTgfGraph(
        lines: List<String>,
        rootNodeMapper: (value: String) -> N?,
        nodeMapper: (value: String) -> N?,
        edgeValueMapper: (to: N, value: String) -> E,
): Map<N, List<E>>? {
    val (nodes, edges) = lines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .split { it == "#" }

    val (rootNodeDef, tailNodeDefs) = nodes.splitLines(2)?.safeCutHead() ?: return null

    fun parseNodeDef(value: List<String>, mapper: (String) -> N?): Pair<Int, N>? {
        val node = value.int(0) ?: return null
        val id = value.getOrNull(1)?.let(mapper) ?: return null
        return node to id
    }

    val rootNode = parseNodeDef(rootNodeDef, rootNodeMapper) ?: return null
    val restNodes = tailNodeDefs.mapNotNull { parseNodeDef(it, nodeMapper) }

    val nodeDefs = (restNodes + rootNode).toMap()

    fun List<String>.resolve(index: Int) = int(index)?.let { nodeDefs[it] }

    val connections = edges.splitLines(3)
            ?.mapNotNull {
                val from = it.resolve(0) ?: return@mapNotNull null
                val to = it.resolve(1) ?: return@mapNotNull null
                val edgeString = it.getOrNull(2) ?: return@mapNotNull null
                from to edgeValueMapper(to, edgeString)
            }?.groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
            ) ?: return null

    return nodeDefs.values.map {
        it to connections.getOrDefault(it, emptyList())
    }.toMap()
}

@Throws(IOException::class)
fun <N : Any, E : Any> parseTgfGraph(
        path: Path,
        rootNodeMapper: (value: String) -> N?,
        nodeMapper: (value: String) -> N?,
        edgeValueMapper: (to: N, value: String) -> E,
): Map<N, List<E>>? = parseTgfGraph(path.toFile().readLines(), rootNodeMapper, nodeMapper, edgeValueMapper)

fun parseDependencyTgfGraph(lines: List<String>): Map<ArtifactId, List<ArtifactDependency>>?
        = parseTgfGraph(lines, DependencyUtil::parseTreeRootArtifact, DependencyUtil::parseListedArtifact, ::ArtifactDependency)

fun parseDependencyTgfGraph(path: Path): Map<ArtifactId, List<ArtifactDependency>>?
        = parseTgfGraph(path, DependencyUtil::parseTreeRootArtifact, DependencyUtil::parseListedArtifact, ::ArtifactDependency)
