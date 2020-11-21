package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.maven.util.JarHtmlJavadocParser
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.core.javadoc.DocStore
import com.mktiti.fsearch.core.javadoc.SimpleMultiDocStore
import com.mktiti.fsearch.modules.DocManager
import com.mktiti.fsearch.util.orElse
import java.io.File

class MavenMapJavadocManager(
        infoRepo: JavaInfoRepo,
        private val mavenFetcher: MavenFetcher,
        private val jclDocs: DocStore = DocStore.nop()
) : DocManager {

    private val docParser = JarHtmlJavadocParser(infoRepo)
    private val stored = mutableMapOf<ArtifactId, DocStore>()

    private fun load(artifact: ArtifactId, file: File): DocStore {
        return stored.getOrPut(artifact) {
            docParser.parseJar(file)
        }
    }

    override fun forArtifacts(artifacts: Set<ArtifactId>): DocStore {
        val stores = artifacts.map { stored[it] }.liftNull().orElse {
            mavenFetcher.runOnJavadocWithDeps(artifacts) { files ->
                artifacts.map { artifact ->
                    files[artifact]?.let { jar ->
                        load(artifact, jar)
                    }.orElse {
                        println("Failed to load javadoc for artifact $artifact")
                        DocStore.nop()
                    }
                }
            } ?: error("Failed to load javadoc for $artifacts")
        }

        return SimpleMultiDocStore(stores + jclDocs)
    }

}