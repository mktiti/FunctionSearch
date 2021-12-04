package com.mktiti.fsearch.modules.util

import com.mktiti.fsearch.modules.ArtifactId
import java.util.regex.Pattern

object DependencyUtil {

    private data class ArtifactCategory(
            val group: List<String>,
            val name: String
    ) {
        companion object {
            fun fromId(id: ArtifactId) = ArtifactCategory(id.group, id.name)
        }

        fun withVersion(version: String) = ArtifactId(group, name, version)
    }

    private val nonDigitPattern = Pattern.compile("\\D+")

    private fun containedNumbers(string: String): List<Int> {
        return string.split(nonDigitPattern).filter {
            it.length in (1..8)
        }.mapNotNull {
            it.toIntOrNull()
        }
    }

    private object NumListComparator: Comparator<List<Int>> {
        override fun compare(a: List<Int>, b: List<Int>): Int {
            a.zip(b).forEach { (an, bn) ->
                val comp = an.compareTo(bn)
                if (comp != 0) {
                    return comp
                }
            }

            return a.size.compareTo(b.size)
        }
    }

    private val pairComp: Comparator<Pair<String, List<Int>>> = Comparator.comparing(
            { it.second }, NumListComparator
    )

    private fun highestVersion(versions: Collection<String>): String {
        val uniqVersions = versions.toSet()
        return when (val single = uniqVersions.singleOrNull()) {
            null -> {
                uniqVersions.mapNotNull { version ->
                    val nums = containedNumbers(version)
                    if (nums.isEmpty()) null else (version to nums)
                }.maxWithOrNull(pairComp)?.first ?: uniqVersions.first()
            }
            else -> single
        }
    }

    // TODO best effort ¯\_(ツ)_/¯
    fun mergeDependencies(dependencies: Collection<Collection<ArtifactId>>): Set<ArtifactId> {
        return dependencies.flatten().groupBy(
                keySelector = ArtifactCategory::fromId,
                valueTransform = ArtifactId::version
        ).map { (artifact, versions) ->
            artifact.withVersion(highestVersion(versions))
        }.toSet()
    }

}