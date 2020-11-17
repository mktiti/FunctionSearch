package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.javadoc.DocStore

interface DocManager {

    companion object {
        fun nop(): DocManager = object : DocManager {
            override fun forArtifacts(artifacts: Set<ArtifactId>) = DocStore.nop()
        }
    }

    fun forArtifact(artifact: ArtifactId): DocStore = forArtifacts(setOf(artifact))

    fun forArtifacts(artifacts: Set<ArtifactId>): DocStore

}