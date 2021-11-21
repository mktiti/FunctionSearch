package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.serialize.ArtifactDocSerializer
import com.mktiti.fsearch.model.build.serialize.ArtifactInfoSerializer
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactInfoStore
import com.mktiti.fsearch.modules.docs.ArtifactDocStore
import com.mktiti.fsearch.modules.store.ArtifactDocStoreWrapper
import com.mktiti.fsearch.modules.store.ArtifactInfoStoreWrapper
import com.mktiti.fsearch.modules.store.GenericArtifactStore
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class GenericFilesystemArtifactStore<T>(
        private val repoRoot: Path,
        private val qualifier: String,
        private val format: String,
        private val serializationService: ArtifactSerializerService<T>
) : GenericArtifactStore<T> {

    companion object {
        fun fsDocsStore(repoRoot: Path): GenericArtifactStore<FunDocMap> {
            return GenericFilesystemArtifactStore(repoRoot, "docs", "json", ArtifactDocSerializer)
        }

        fun forDocsWrapped(repoRoot: Path): ArtifactDocStore = ArtifactDocStoreWrapper(fsDocsStore(repoRoot))

        fun fsInfoStore(repoRoot: Path): GenericArtifactStore<ArtifactInfoResult> {
            return GenericFilesystemArtifactStore(repoRoot, "info", "json", ArtifactInfoSerializer)
        }

        fun forInfoWrapped(repoRoot: Path): ArtifactInfoStore = ArtifactInfoStoreWrapper(fsInfoStore(repoRoot))
    }

    private fun filePath(id: ArtifactId): Path = FilesystemStoreUtil.storedLocation(repoRoot, id, qualifier, format)

    override fun store(artifact: ArtifactId, data: T) {
        try {
            val path = filePath(artifact).apply {
                parent.createDirectories()
            }
            serializationService.writeToFile(data, path)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    override fun getData(artifact: ArtifactId): T? {
        val path = filePath(artifact)
        return try {
            if (Files.exists(path)) {
                serializationService.readFromFile(path)
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