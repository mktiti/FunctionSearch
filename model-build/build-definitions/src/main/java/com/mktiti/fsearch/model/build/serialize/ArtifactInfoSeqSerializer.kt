package com.mktiti.fsearch.model.build.serialize

import ArtifactInfoSeqResult
import FunctionInfoSeqResult
import TypeInfoSeqResult
import com.mktiti.fsearch.model.build.intermediate.IntInstanceFunEntry
import com.mktiti.fsearch.model.build.intermediate.RawFunInfo
import com.mktiti.fsearch.model.build.intermediate.SemiInfo
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ArtifactInfoSeqSerializer : ArtifactSerializerService<ArtifactInfoSeqResult> {

    private val directInfoSerializer = JacksonLineSerializer.forClass<SemiInfo.DirectInfo>()
    private val templateInfoSerializer = JacksonLineSerializer.forClass<SemiInfo.TemplateInfo>()

    private val staticFunSerializer = JacksonLineSerializer.forClass<RawFunInfo>()
    private val instanceFunSerializer = JacksonLineSerializer.forClass<IntInstanceFunEntry>()

    private fun Path.file(name: String, type: String) = resolve("$name-$type.jsonl").toFile()
    private fun Path.directsFile(name: String) = file(name, "direct-types")
    private fun Path.templatesFile(name: String) = file(name, "template-types")
    private fun Path.staticsFile(name: String) = file(name, "static-funs")
    private fun Path.instancesFile(name: String) = file(name, "instance-funs")

    @Throws(IOException::class)
    override fun writeToDir(data: ArtifactInfoSeqResult, name: String, dir: Path) {
        dir.createDirectories()

        directInfoSerializer.serializeToFile(data.typeInfo.directInfos, dir.directsFile(name))
        templateInfoSerializer.serializeToFile(data.typeInfo.templateInfos, dir.templatesFile(name))

        staticFunSerializer.serializeToFile(data.funInfo.staticFunctions, dir.staticsFile(name))
        instanceFunSerializer.serializeToFile(data.funInfo.instanceMethods, dir.instancesFile(name))
    }

    @Throws(IOException::class, SerializationException::class)
    override fun readFromDir(dir: Path, name: String): ArtifactInfoSeqResult {
        return ArtifactInfoSeqResult.simple(
                typeInfo = TypeInfoSeqResult.simple(
                        directInfos = directInfoSerializer.deserializeFromFileAsStream(dir.directsFile(name)),
                        templateInfos = templateInfoSerializer.deserializeFromFileAsStream(dir.templatesFile(name))
                ), funInfo = FunctionInfoSeqResult.simple(
                        staticFunctions = staticFunSerializer.deserializeFromFileAsStream(dir.staticsFile(name)),
                        instanceMethods = instanceFunSerializer.deserializeFromFileAsStream(dir.instancesFile(name))
                )
        )
    }

}

