package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ArtifactInfoSerializer : ArtifactSerializerService<ArtifactInfoResult> {

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
    override fun writeToDir(data: ArtifactInfoResult, name: String, dir: Path) {
        dir.createDirectories()

        directInfoSerializer.serializeToFile(data.typeInfo.directInfos, dir.directsFile(name))
        templateInfoSerializer.serializeToFile(data.typeInfo.templateInfos, dir.templatesFile(name))

        staticFunSerializer.serializeToFile(data.funInfo.staticFunctions, dir.staticsFile(name))
        instanceFunSerializer.serializeToFile(data.funInfo.instanceMethods, dir.instancesFile(name))
    }

    @Throws(IOException::class, SerializationException::class)
    override fun readFromDir(dir: Path, name: String): ArtifactInfoResult {
        return ArtifactInfoResult(
                typeInfo = TypeInfoResult(
                        directInfos = directInfoSerializer.deserializeFromFile(dir.directsFile(name)),
                        templateInfos = templateInfoSerializer.deserializeFromFile(dir.templatesFile(name))
                ), funInfo = FunctionInfoResult(
                        staticFunctions = staticFunSerializer.deserializeFromFile(dir.staticsFile(name)),
                        instanceMethods = instanceFunSerializer.deserializeFromFile(dir.instancesFile(name))
                )
        )
    }

}

