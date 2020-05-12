package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.parser.function.ImParam
import com.mktiti.fsearch.parser.function.ImTypeParam
import com.mktiti.fsearch.parser.service.InfoCollector
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.mapMutablePrefixTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

interface AsmInfoCollectorView {

    fun loadEntry(input: InputStream)

}

object AsmInfoCollector {

    fun collect(artifact: String, infoRepo: JavaInfoRepo, load: AsmInfoCollectorView.() -> Unit): InfoCollector.InitialData {
        val visitor = AsmInfoCollectorVisitor(artifact, infoRepo)
        object : AsmInfoCollectorView {
            override fun loadEntry(input: InputStream) {
                ClassReader(input).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            }
        }.load()
        return InfoCollector.InitialData(visitor.directTypes, visitor.templateTypes)
    }

}

private class AsmInfoCollectorVisitor(
        private val artifact: String,
        private val infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    companion object {
        fun parseNonGeneric(type: String): MinimalInfo {
            val splitName = type.split('/')
            val (packageName, simpleName) = splitName.cutLast()
            return MinimalInfo(packageName, simpleName.replace('$', '.'))
        }
    }

    val directTypes: MutablePrefixTree<String, DirectCreator> = mapMutablePrefixTree()
    val templateTypes: MutablePrefixTree<String, TemplateCreator> = mapMutablePrefixTree()

    private data class TypeMeta(
            val info: TypeInfo,
            val signature: String?,
            val superNames: List<String>,
            val nestDepth: Int? = null
    )

    private var currentType: TypeMeta? = null

    private fun addUnfinishedDirect(
            info: TypeInfo,
            superCount: Int,
            directSupers: List<MinimalInfo>,
            templateSupers: MutableList<DatCreator>
    ) {
        val supers: MutableList<SuperType.StaticSuper> = ArrayList(superCount)
        val created = Type.NonGenericType.DirectType(info, supers)

        directTypes.mutableSubtreeSafe(info.packageName)[info.name] = DirectCreator(
                unfinishedType = created,
                addNonGenericSuper = { superType -> supers += SuperType.StaticSuper.EagerStatic(superType) },
                templateSupers = templateSupers,
                directSupers = directSupers
        )
    }

    private fun addUnfinishedTemplate(
            info: TypeInfo,
            superCount: Int,
            typeParams: List<String>,
            directSupers: List<MinimalInfo>,
            templateSupers: MutableList<DatCreator>
    ) {
        val supers: MutableList<SuperType<Type>> = ArrayList(superCount)
        val created = TypeTemplate(info, typeParams.map { TypeParameter(it, TypeBounds(emptySet())) }, supers)

        templateTypes.mutableSubtreeSafe(info.packageName)[info.name] = TemplateCreator(
                unfinishedType = created,
                directSuperAppender = { superType -> supers += SuperType.StaticSuper.EagerStatic(superType) },
                templateSuperAppender = { superType -> supers += SuperType.DynamicSuper.EagerDynamic(superType) },
                templateSupers = templateSupers,
                directSupers = directSupers
        )
    }

    private fun transformArg(imParam: ImParam, typeParams: List<String>): TypeArgCreator {
        return when (imParam) {
            ImParam.Wildcard -> TypeArgCreator.Wildcard
            is ImParam.Type -> {
                if (imParam.typeArgs.isEmpty()) {
                    TypeArgCreator.Direct(imParam.info)
                } else {
                    TypeArgCreator.Dat(
                            DatCreator(
                                    template = imParam.info,
                                    args = imParam.typeArgs.map { transformArg(it, typeParams) }
                            )
                    )
                }
            }
            is ImParam.TypeParamRef -> TypeArgCreator.Param(
                    typeParams.withIndex().find { it.value == imParam.sign }?.index ?: error("ASD")
            )
            is ImParam.Array -> TypeArgCreator.Dat(DatCreator(
                    template = infoRepo.arrayType,
                    args = listOf(transformArg(imParam.type, typeParams))
            ))
            is ImParam.Primitive -> TypeArgCreator.Direct(infoRepo.primitive(imParam.value))
            is ImParam.UpperWildcard -> TypeArgCreator.UpperWildcard(transformArg(imParam.param, typeParams))
            is ImParam.LowerWildcard -> {
                println("Lower wildcard (? super ...) should not be possible in type declaration")
                TypeArgCreator.Wildcard
            }
            ImParam.Void -> TypeArgCreator.Direct(infoRepo.voidType)
        }
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        // if (access and Opcodes.ACC_PUBLIC == 0) {
        //    return
        // }

        val info = parseNonGeneric(name).full(artifact)
        val lastName = info.name.split('.').last()
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

    override fun visitEnd() {
        currentType?.let {
            createInitials(it)
        }
        currentType = null
    }

    private fun createInitials(meta: TypeMeta) {
        val (info, signature, superNames) = meta
        val superCount = superNames.size

        val nestContext = if (meta.nestDepth != null) {
            val nests = info.name.split(".").dropLast(1)
            val depth = Integer.min(meta.nestDepth, nests.size)

            val nestPackage: PrefixTree<String, TemplateCreator> = templateTypes.mutableSubtreeSafe(info.packageName)

            val affectingNests = nests.takeLast(depth)
            val nestPrefix = when (val prefix = nests.dropLast(depth).joinToString(separator = ".")) {
                "" -> prefix
                else -> "$prefix."
            }

            affectingNests.fold(emptyList<TypeParameter>() to nestPrefix) { (params, name), nest ->
                val newName = name + nest
                val nestParams = nestPackage[newName]?.unfinishedType?.typeParams ?: emptyList()

                (params + nestParams) to "$newName."
            }.first
        } else {
            emptyList()
        }

        if (signature == null && nestContext.isEmpty()) {
            addUnfinishedDirect(
                    info = info,
                    superCount = superCount,
                    directSupers = superNames.map { parseNonGeneric(it) }.toMutableList(),
                    templateSupers = LinkedList()
            )
        } else {
            val nestTypeParams = nestContext.map {
                // TODO param bounds
                ImTypeParam(it.sign, emptyList())
            }

            val type = if (signature == null) {
                ParsedType.Template(
                        typeParams = nestTypeParams,
                        superTypes = superNames.map { ImParam.Type(parseNonGeneric(it), emptyList()) }
                )
            } else {
                parseType(signature, nestTypeParams)
            }
            val (ds, ts) = type.supersTypes.partition { it.typeArgs.isEmpty() }

            when (type) {
                is ParsedType.Direct -> {
                    val templateSupers = ts.map { imSuper ->
                        DatCreator(
                                template = imSuper.info,
                                args = imSuper.typeArgs.map { transformArg(it, emptyList()) }
                        )
                    }

                    addUnfinishedDirect(
                            info = info,
                            superCount = superCount,
                            directSupers = ds.map { it.info },
                            templateSupers = templateSupers.toMutableList()
                    )
                }
                is ParsedType.Template -> {
                    val params = type.typeParams.map { it.sign }

                    val templateSupers = ts.map { imSuper ->
                        DatCreator(
                                template = imSuper.info,
                                args = imSuper.typeArgs.map { transformArg(it, params) }
                        )
                    }

                    addUnfinishedTemplate(
                            info = info,
                            superCount = superCount,
                            typeParams = params,
                            directSupers = ds.map { it.info },
                            templateSupers = templateSupers.toMutableList()
                    )
                }
            }
        }
    }

}