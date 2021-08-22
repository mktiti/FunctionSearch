package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.parser.intermediate.DefaultJavaSamInfoParser
import com.mktiti.fsearch.parser.intermediate.DefaultTypeParser
import com.mktiti.fsearch.parser.intermediate.JavaSamInfoParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureTypeParser
import com.mktiti.fsearch.parser.service.indirect.*
import com.mktiti.fsearch.parser.type.JavaSamUtil
import org.objectweb.asm.*
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.addAll
import kotlin.collections.dropLast
import kotlin.collections.emptyList
import kotlin.collections.fold
import kotlin.collections.joinToString
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.takeLast

object AsmRawTypeInfoCollector {

    fun collect(infoRepo: JavaInfoRepo, load: AsmCollectorView.() -> Unit): RawTypeInfoResult {
        val visitor = AsmRawTypeInfoCollectorVisitor(infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return RawTypeInfoResult(visitor.loadedDirectInfos, visitor.loadedTemplateInfos)
    }

}

private class AsmRawTypeInfoCollectorVisitor(
        infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    private val samAnnotations = infoRepo.explicitSamAnnotations.map(AsmUtil::annotationDescriptor)

    private val typeParser: JavaSignatureTypeParser = DefaultTypeParser(infoRepo)
    //private val funParser: JavaSignatureFunctionParser = DefaultFunctionParser(infoRepo)
    private val samParser: JavaSamInfoParser = DefaultJavaSamInfoParser(infoRepo)

    private val directInfos: MutableMap<MinimalInfo, SemiInfo.DirectInfo> = HashMap()
    val loadedDirectInfos: Collection<SemiInfo.DirectInfo>
        get() = directInfos.values

    private val templateInfos: MutableMap<MinimalInfo, SemiInfo.TemplateInfo> = HashMap()
    val loadedTemplateInfos: Collection<SemiInfo.TemplateInfo>
        get() = templateInfos.values

    private sealed class AbstractCount {
        object NoAbstract : AbstractCount() {
            override fun plus(newAbstract: OneAbstract) = newAbstract
        }

        data class OneAbstract(
                val signature: String
        ) : AbstractCount() {
            override fun plus(newAbstract: OneAbstract) = TooMany
        }

        object TooMany : AbstractCount() {
            override fun plus(newAbstract: OneAbstract) = TooMany
        }

        abstract operator fun plus(newAbstract: OneAbstract): AbstractCount
    }

    private data class TypeMeta(
            val info: MinimalInfo,
            val isInterface: Boolean,
            val signature: String?,
            val superNames: List<String>,
            val nestDepth: Int? = null,
            val abstractCount: AbstractCount = AbstractCount.NoAbstract,
            val explicitSam: Boolean = false
    )

    private var currentType: TypeMeta? = null

    private fun addDirect(
            directInfo: SemiInfo.DirectInfo
    ) {
        directInfos[directInfo.info] = directInfo
    }

    private fun addDirect(
            info: MinimalInfo,
            directSupers: List<MinimalInfo>,
            samType: SamInfo.Direct?
    ) {
        addDirect(
            SemiInfo.DirectInfo(
                    info = info,
                    directSupers = directSupers,
                    satSupers = emptyList(),
                    samInfo = samType
            )
        )
    }

    private fun addTemplate(templateInfo: SemiInfo.TemplateInfo) {
        templateInfos[templateInfo.info] = templateInfo
    }

    private fun addTemplate(
            info: MinimalInfo,
            typeParams: List<TemplateTypeParamInfo>,
            directSupers: Collection<MinimalInfo>,
            satSupers: Collection<CompleteMinInfo.Static>,
            datSupers: Collection<DatInfo>,
            samType: SamInfo.Generic?
    ) {
        addTemplate(
            SemiInfo.TemplateInfo(
                    info = info,
                    typeParams = typeParams,
                    directSupers = directSupers,
                    satSupers = satSupers,
                    datSupers = datSupers,
                    samInfo = samType
            )
        )
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        val isInterface = access and Opcodes.ACC_INTERFACE > 0

        val lastName = info.simpleName.split('.').last()
        if (lastName.first().isDigit()) {
            // Skip anonymous nested and local class
            return
        }

        val superCount = (if (superName == null) 0 else 1) + (interfaces?.size ?: 0)
        val superNames: List<String> = ArrayList<String>(superCount).apply {
            superName?.let(this::add)
            interfaces?.let(this::addAll)
        }

        currentType = TypeMeta(info, isInterface, signature, superNames)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        currentType?.let {
            if (descriptor in samAnnotations) {
                currentType = it.copy(explicitSam = true)
            }
        }
        return null
    }

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        // TODO
        /*
        currentType?.let { current ->
            val parsedInfo = AsmUtil.parseName(name ?: return)
            if (parsedInfo == current.info) {
                val newNest = if (current.isInterface) {
                    0
                } else {
                    max(current.nestDepth ?: 0, 1)
                }
                currentType = current.copy(isInterface = false, nestDepth = newNest)
            }
        }

         */
    }

    override fun visitField(access: Int, name: String, descriptor: String?, signature: String?, value: Any?): FieldVisitor? {
        if (name.startsWith("this$")) {
            val depth = name.removePrefix("this$").toIntOrNull()?.let { it + 1 } ?: return null
            currentType?.let {
                if (it.nestDepth == null || depth > it.nestDepth) {
                    currentType = it.copy(nestDepth = depth)
                }
            }
        }

        return null
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if ((access and Opcodes.ACC_ABSTRACT) > 0 && (access and Opcodes.ACC_STATIC) == 0) {
            // Skipping always realized abstract methods
            if (!JavaSamUtil.isObjectMethod(name, descriptor)) {
                currentType?.let {
                    currentType = it.copy(
                            abstractCount = it.abstractCount + AbstractCount.OneAbstract(signature ?: descriptor)
                    )
                }
            }
        }

        return null
    }

    override fun visitEnd() {
        currentType?.let {
            createInitials(it)
        }
        currentType = null
    }

    private fun createInitials(meta: TypeMeta) {
        val (info, _, signature, superNames, nestDepth, abstractCount, explicitSam) = meta

        val nestContext: List<TemplateTypeParamInfo> = if (nestDepth != null) {
            val nests = info.simpleName.split(".").dropLast(1)
            val depth = Integer.min(nestDepth, nests.size)

            val affectingNests = nests.takeLast(depth)
            val nestPrefix = when (val prefix = nests.dropLast(depth).joinToString(separator = ".")) {
                "" -> prefix
                else -> "$prefix."
            }

            affectingNests.fold(emptyList<TemplateTypeParamInfo>() to nestPrefix) { (params, name), nest ->
                val newName = name + nest
                val nestInfo = info.copy(simpleName = newName)
                val nestParams = templateInfos[nestInfo]?.typeParams ?: emptyList()

                (params + nestParams) to "$newName."
            }.first
        } else {
            emptyList()
        }

        fun directSam(): SamInfo.Direct? = when (abstractCount) {
            is AbstractCount.OneAbstract -> {
                samParser.parseDirectSam(null, abstractCount.signature)?.let { signature ->
                    SamInfo.Direct(explicitSam, signature)
                }
            }
            else -> null
        }

        fun genericSam(typeParams: List<TemplateTypeParamInfo>): SamInfo.Generic?  {
            return when (abstractCount) {
                is AbstractCount.OneAbstract -> {
                    samParser.parseGenericSam(null, abstractCount.signature, info, typeParams)?.let { signature ->
                        SamInfo.Generic(explicitSam, signature)
                    }
                }
                else -> null
            }
        }

        if (signature == null) {
            val directSupers = superNames.map(AsmUtil::parseName)

            // No explicit type parameters defined
            if (nestContext.isEmpty()) {
                // Direct type
                addDirect(
                        info = info,
                        directSupers = directSupers,
                        samType = directSam()
                )
            } else {
                // Nested in template parent
                addTemplate(
                        info = info,
                        typeParams = nestContext,
                        directSupers = directSupers,
                        satSupers = emptyList(),
                        datSupers = emptyList(),
                        samType = genericSam(nestContext)
                )
            }
        } else {
            when (val direct = typeParser.parseDirectTypeSignature(info, signature, directSam())) {
                null -> {
                    addTemplate(typeParser.parseTemplateSignature(info, signature, nestContext, ::genericSam))
                }
                else -> {
                    addDirect(direct)
                }
            }
        }
    }

}
