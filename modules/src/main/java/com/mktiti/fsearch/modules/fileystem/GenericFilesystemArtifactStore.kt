package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.serialize.ArtifactDocSerializer
import com.mktiti.fsearch.model.build.serialize.ArtifactInfoSerializer
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.serialize.ArtifactDepsSerializer
import com.mktiti.fsearch.modules.store.GenericArtifactStore
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class GenericFilesystemArtifactStore<T>(
        private val repoRoot: Path,
        private val serializationService: ArtifactSerializerService<T>
) : GenericArtifactStore<T> {

    companion object {
        fun fsDocsStore(repoRoot: Path): GenericArtifactStore<FunDocMap> {
            return GenericFilesystemArtifactStore(repoRoot, ArtifactDocSerializer)
        }

        fun fsInfoStore(repoRoot: Path): GenericArtifactStore<ArtifactInfoResult> {
            return GenericFilesystemArtifactStore(repoRoot, ArtifactInfoSerializer)
        }

        fun fsDepsStore(repoRoot: Path): GenericArtifactStore<List<ArtifactId>> {
            return GenericFilesystemArtifactStore(repoRoot, ArtifactDepsSerializer)
        }
    }

    private fun filePath(id: ArtifactId): Path = FilesystemStoreUtil.storedLocation(repoRoot, id)

    override fun store(artifact: ArtifactId, data: T) {
        try {
            serializationService.writeToDir(data, artifact.name, filePath(artifact))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    override fun getData(artifact: ArtifactId): T? {
        val path = filePath(artifact)
        return try {
            if (Files.exists(path)) {
                serializationService.readFromDir(path, artifact.name)
            } else {
                null
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        } catch (se: IOException) {
            se.printStackTrace()
            null
        }
    }

    override fun remove(artifact: ArtifactId) {
        try {
            Files.delete(filePath(artifact))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }
}