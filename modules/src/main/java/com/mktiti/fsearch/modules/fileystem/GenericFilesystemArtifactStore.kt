package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.modules.ArtifactId
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class GenericFilesystemArtifactStore<T>(
        private val repoRoot: Path,
        private val qualifier: String,
        private val format: String,
        private val serializationService: ArtifactSerializerService<T>
) {

    private fun filePath(id: ArtifactId): Path = FilesystemStoreUtil.storedLocation(repoRoot, id, qualifier, format)

    fun store(artifact: ArtifactId, data: T) {
        try {
            val path = filePath(artifact).apply {
                parent.createDirectories()
            }
            serializationService.writeToFile(data, path)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    fun getData(artifact: ArtifactId): T? {
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

    fun remove(artifact: ArtifactId) {
        try {
            Files.delete(filePath(artifact))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }
}