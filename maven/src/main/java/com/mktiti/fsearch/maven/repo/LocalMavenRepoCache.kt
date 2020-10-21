package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.MavenArtifact
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class LocalMavenRepoCache(
        private val repoPath: Path
) {

    constructor(home: String = System.getProperty("user.home")) : this(
            repoPath = Paths.get(home, ".m2", "repository")
    )

    fun <T> onCachedFile(info: MavenArtifact, ext: String, onFile: (File) -> T): T? {
        // E.g: $repo/org/apache/commons/commons-lang/1.0-SNAPSHOT/commons-lang-1.0-SNAPSHOT.jar
        val path = info.group.fold(repoPath) { path, part ->
            path.resolve(part)
        }.resolve(info.name).resolve(info.version).resolve("${info.name}-${info.version}.$ext")

        return with(path.toFile()) {
            if (exists()) {
               onFile(this)
            } else {
                null
            }
        }
    }

}