package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.safeCutHead
import com.mktiti.fsearch.util.safeCutLast

object DependencyUtil {

    fun parseListedArtifact(line: String): ArtifactId? {
        val parts = line.split(':')
        val (group, artifact) = parts.safeCutHead() ?: return null
        val (name, _) = artifact.safeCutHead() ?: return null
        val (classAndVer, _) = parts.safeCutLast() ?: return null
        val version = classAndVer.lastOrNull() ?: return null

        return ArtifactId(group.split('.'), name, version)
    }

    fun shortFilenameForArtifact(id: ArtifactId, classifier: String?): String {
        val init = (id.group + id.name).joinToString(separator = ".")
        return listOfNotNull(init, id.version, classifier).joinToString(separator = "-")
    }

}