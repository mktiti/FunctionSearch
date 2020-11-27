package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.intermediate.DefaultFunctionParser
import com.mktiti.fsearch.parser.intermediate.DefaultTypeParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureFunctionParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureTypeParser
import com.mktiti.fsearch.parser.service.IndirectInfoCollector
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.math.max

object AsmInfoCollector {

    fun collect(infoRepo: JavaInfoRepo, load: AsmCollectorView.() -> Unit): IndirectInfoCollector.IndirectInitialData {
        val visitor = AsmInfoCollectorVisitor(infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return IndirectInfoCollector.IndirectInitialData(visitor.loadedDirectTypes, visitor.loadedTemplates)
    }

}

private class AsmInfoCollectorVisitor(
        infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    private val typeParser: JavaSignatureTypeParser = DefaultTypeParser(infoRepo)

    private val funParser: JavaSignatureFunctionParser = DefaultFunctionParser(infoRepo)

    private val directTypes: MutableMap<MinimalInfo, DirectType> = HashMap()
    val loadedDirectTypes: Map<MinimalInfo, DirectType>
        get() = directTypes

    private val typeTemplates: MutableMap<MinimalInfo, TypeTemplate> = HashMap()
    val loadedTemplates: Map<MinimalInfo, TypeTemplate>
        get() = typeTemplates

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
            val signature: String?,
            val superNames: List<String>,
            val nestDepth: Int? = null,
            val abstractCount: AbstractCount = AbstractCount.NoAbstract
    )

    private var currentType: TypeMeta? = null

    private fun addDirect(type: DirectType) {
        directTypes[type.info] = type
    }

    private fun addDirect(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo.Static>,
            samType: SamType.DirectSam?
    ) {
        addDirect(
            DirectType(
                minInfo = info,
                superTypes = TypeHolder.staticIndirects(superTypes),
                samType = samType,
                virtual = false
            )
        )
    }

    private fun addTemplate(template: TypeTemplate) {
        typeTemplates[template.info] = template
    }

    private fun addTemplate(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo<*>>,
            typeParams: List<TypeParameter>,
            samType: SamType.GenericSam?
    ) {
        addTemplate(
            TypeTemplate(
                info = info,
                superTypes = TypeHolder.anyIndirects(superTypes),
                typeParams = typeParams,
                samType = samType,
                virtual = false
            )
        )
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

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

        currentType = TypeMeta(info, signature, superNames)
    }

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        // TODO
        currentType?.let { current ->
            val parsedName = AsmUtil.parseName(name ?: return)
            if (parsedName == current.info) {
                currentType = current.copy(nestDepth = max(current.nestDepth ?: 0, 1))
            }
        }
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
            currentType?.let {
                currentType = it.copy(
                        abstractCount = it.abstractCount + AbstractCount.OneAbstract(signature ?: descriptor)
                )
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
        val (info, signature, superNames, nestDepth, abstractCount) = meta

        val nestContext: List<TypeParameter> = if (nestDepth != null) {
            val nests = info.simpleName.split(".").dropLast(1)
            val depth = Integer.min(nestDepth, nests.size)

            val affectingNests = nests.takeLast(depth)
            val nestPrefix = when (val prefix = nests.dropLast(depth).joinToString(separator = ".")) {
                "" -> prefix
                else -> "$prefix."
            }

            affectingNests.fold(emptyList<TypeParameter>() to nestPrefix) { (params, name), nest ->
                val newName = name + nest
                val nestInfo = info.copy(simpleName = newName)
                val nestParams = typeTemplates[nestInfo]?.typeParams ?: emptyList()

                (params + nestParams) to "$newName."
            }.first
        } else {
            emptyList()
        }

        fun directSam(): SamType.DirectSam? = when (abstractCount) {
            is AbstractCount.OneAbstract -> funParser.parseDirectSam(null, abstractCount.signature)
            else -> null
        }

        fun genericSam(typeParams: List<TypeParameter>): SamType.GenericSam?  {
            return when (abstractCount) {
                is AbstractCount.OneAbstract -> funParser.parseGenericSam(null, abstractCount.signature, info, typeParams)
                else -> null
            }
        }

        if (signature == null && nestContext.isEmpty()) {
            addDirect(
                    info = info,
                    superTypes = superNames.map(AsmUtil::parseCompleteStaticName),
                    samType = directSam()
            )
        } else {

            if (signature == null) {
                addTemplate(
                        info = info,
                        typeParams = nestContext,
                        superTypes = superNames.map { directSuper ->
                            AsmUtil.parseName(directSuper).complete()
                        },
                        samType = genericSam(nestContext)
                )
            } else {
                when (val direct = typeParser.parseDirectTypeSignature(info, signature, /*TODO*/ try {directSam()}catch(e: Exception){null})) {
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

}