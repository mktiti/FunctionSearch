package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.ArtifactRepo
import com.mktiti.fsearch.maven.MavenArtifact
import java.io.File

interface RepoArtifactFetch {

    fun fetchArtifactWithDeps(info: MavenArtifact, transform: (MavenArtifact, File) -> ArtifactRepo): Map<MavenArtifact, ArtifactRepo>?

}