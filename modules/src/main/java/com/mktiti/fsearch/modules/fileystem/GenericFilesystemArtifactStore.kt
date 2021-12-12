package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.serialize.ArtifactDocSerializer
import com.mktiti.fsearch.model.build.serialize.ArtifactInfoSerializer
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.serialize.ArtifactDepsSerializer
import com.mktiti.fsearch.modules.store.GenericArtifactStore
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logger
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

    private val log = logger()

    private fun filePath(id: ArtifactId): Path = FilesystemStoreUtil.storedLocation(repoRoot, id)

    override fun store(artifact: ArtifactId, data: T) {
        try {
            serializationService.writeToDir(data, artifact.name, filePath(artifact))
        } catch (ioe: IOException) {
            log.logError(ioe) { "IOException while storing cache data for artifact $artifact" }
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
            log.logError(ioe) { "IOException while loading cache data for artifact $artifact" }
            null
        } catch (se: SerializationException) {
            log.logError(se) { "Deserialization failed while loading cache data for artifact $artifact" }
            null
        }
    }

    override fun remove(artifact: ArtifactId) {
        try {
            Files.delete(filePath(artifact))
        } catch (ioe: IOException) {
            log.logError(ioe) { "IOException while removing cache data for artifact $artifact" }
        }
    }
}