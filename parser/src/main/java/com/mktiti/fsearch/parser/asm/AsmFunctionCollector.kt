package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunInstanceRelation.*
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.util.InfoMap
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.intermediate.DefaultFunctionParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureFunctionParser
import com.mktiti.fsearch.parser.service.FunctionCollector.FunctionCollection
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object AsmFunctionCollector {

    fun collect(infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver, load: AsmCollectorView.() -> Unit): FunctionCollection {
        val visitor = AsmFunctionCollectorVisitor(dependencyResolver, infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return FunctionCollection(
                staticFunctions = visitor.staticFuns,
                //instanceMethods = InfoMap.fromPrefix(visitor.instanceFuns)
                instanceMethods = InfoMap.fromMap(visitor.instanceFuns)
        )
    }

}

private class AsmFunParamNamesVisitor(
        private val onEnd: (paramNames: List<String>?) -> Unit
) : MethodVisitor(Opcodes.ASM8) {

    private val paramNames: MutableList<String?> = LinkedList()

    override fun visitParameter(name: String?, access: Int) {
        paramNames += name
    }

    override fun visitEnd() {
        onEnd(paramNames.liftNull())
    }

}

private class AsmFunctionCollectorVisitor(
        private val dependencyResolver: TypeResolver,
        private val infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    private sealed class ContextInfo<T>(
            val info: MinimalInfo,
            val thisType: T
    ) {
        class Direct(
                info: MinimalInfo,
                thisType: CompleteMinInfo.Static
        ) : ContextInfo<CompleteMinInfo.Static>(info, thisType)

        class Template(
                info: MinimalInfo,
                thisType: TypeTemplate
        ) : ContextInfo<TypeTemplate>(info, thisType)
    }

    companion object {
        private const val checkFlag = Opcodes.ACC_PUBLIC

        private fun functionRelation(name: String, access: Int): FunInstanceRelation = when {
            name == "<init>" -> CONSTRUCTOR
            ((access and Opcodes.ACC_STATIC) > 0) -> STATIC
            else -> INSTANCE
        }
    }

    private val funParser: JavaSignatureFunctionParser = DefaultFunctionParser(infoRepo)

    private lateinit var context: ContextInfo<*>

    private val collectedStaticFuns: MutableList<FunctionObj> = ArrayList()
    val staticFuns: List<FunctionObj>
        get() = collectedStaticFuns

    //private val collectedInstanceFuns: MutablePrefixTree<String, MutableCollection<FunctionObj>> = mapMutablePrefixTree()
    //val instanceFuns: PrefixTree<String, Collection<FunctionObj>>
        //get() = collectedInstanceFuns

    private val collectedInstanceFuns: MutableMap<MinimalInfo, MutableCollection<FunctionObj>> = HashMap()
    val instanceFuns: Map<MinimalInfo, Collection<FunctionObj>>
        get() = collectedInstanceFuns

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        context = when (val template = dependencyResolver.template(info)) {
            null -> ContextInfo.Direct(info, info.complete())
            else -> ContextInfo.Template(info, template)
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (checkFlag and access == checkFlag) {
            val instanceRel = functionRelation(name, access)

            with(context) {
                if (info.nameParts.any { it.firstOrNull()?.isDigit() != false } && instanceRel != STATIC) {
                    // Skip anonymous and local types
                    return null
                }

                val toParse = signature ?: descriptor
                AsmFunParamNamesVisitor { paramNames ->
                    try {
                        val parsedSignature: TypeSignature = when (instanceRel) {
                            STATIC -> funParser.parseStaticFunction(name, paramNames, toParse)
                            CONSTRUCTOR -> when (this) {
                                is ContextInfo.Direct -> funParser.parseDirectConstructor(thisType, toParse, paramNames)
                                is ContextInfo.Template -> funParser.parseTemplateConstructor(thisType, toParse, paramNames)
                            }
                            INSTANCE -> when (this) {
                                is ContextInfo.Direct -> funParser.parseDirectFunction(name, paramNames, toParse, thisType)
                                is ContextInfo.Template -> funParser.parseTemplateFunction(name, paramNames, toParse, thisType)
                            }
                        }

                        val typeParams = (when (this) {
                            is ContextInfo.Direct -> emptyList()
                            is ContextInfo.Template -> thisType.typeParams
                        } + parsedSignature.typeParameters).map { it.sign }

                        val funInfo = FunctionInfo.fromSignature(
                                file = info,
                                name = name,
                                signature = parsedSignature,
                                instanceRelation = instanceRel,
                                typeParams = typeParams,
                                infoRepo = infoRepo
                        ) ?: error("Cannot create function info! ($info::$name)")

                        if (instanceRel != INSTANCE) {
                            collectedStaticFuns += FunctionObj(funInfo, parsedSignature)
                        } else {
                            //val id = info.packageName + info.simpleName
                            /*val store: MutableCollection<FunctionObj> = collectedInstanceFuns[id].orElse {
                                ArrayList<FunctionObj>().apply {
                                    collectedInstanceFuns[id] = this
                                }
                            }
                             */
                            collectedInstanceFuns.getOrPut(info) { LinkedList() } += FunctionObj(funInfo, parsedSignature)
                        }
                    } catch (e: NotImplementedError) {
                        System.err.println("Failed to parse $info $name :: ${signature ?: descriptor}")
                        e.printStackTrace()
                    }
                }
            }
        } else {
            null
        }
    }

}