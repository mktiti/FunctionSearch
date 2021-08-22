package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunInstanceRelation.*
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.function.FunctionInfoUtil
import com.mktiti.fsearch.parser.intermediate.DefaultJavaSamInfoParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureFunctionInfoParser
import com.mktiti.fsearch.parser.service.indirect.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object AsmFunctionInfoCollector {

    fun collect(infoRepo: JavaInfoRepo, typeParamResolver: TypeParamResolver, load: AsmCollectorView.() -> Unit): FunctionInfoCollector.FunctionInfoCollection {
        val visitor = AsmFunctionInfoCollectorVisitor(infoRepo, typeParamResolver)
        DefaultAsmCollectorView(visitor).load()
        return FunctionInfoCollector.FunctionInfoCollection(
                staticFunctions = visitor.staticFuns,
                instanceMethods = visitor.instanceFuns
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

private class AsmFunctionInfoCollectorVisitor(
        private val infoRepo: JavaInfoRepo,
        private val typeParamResolver: TypeParamResolver
) : ClassVisitor(Opcodes.ASM8) {

    private sealed class ContextInfo(
            val thisInfo: MinimalInfo
    ) {

        abstract val thisTypeParams: List<TemplateTypeParamInfo>

        class Direct(
                info: MinimalInfo
        ) : ContextInfo(info) {
            override val thisTypeParams: List<TemplateTypeParamInfo>
                get() = emptyList()
        }

        class Template(
                info: MinimalInfo,
                override val thisTypeParams: List<TemplateTypeParamInfo>
        ) : ContextInfo(info)
    }

    companion object {
        private const val checkFlag = Opcodes.ACC_PUBLIC

        private fun functionRelation(name: String, access: Int): FunInstanceRelation = when {
            name == "<init>" -> CONSTRUCTOR
            ((access and Opcodes.ACC_STATIC) > 0) -> STATIC
            else -> INSTANCE
        }
    }

    private val funParser: JavaSignatureFunctionInfoParser = DefaultJavaSamInfoParser(infoRepo)

    private lateinit var context: ContextInfo

    private val collectedStaticFuns: MutableList<RawFunInfo<*>> = ArrayList()
    val staticFuns: List<RawFunInfo<*>>
        get() = collectedStaticFuns

    //private val collectedInstanceFuns: MutablePrefixTree<String, MutableCollection<FunctionObj>> = mapMutablePrefixTree()
    //val instanceFuns: PrefixTree<String, Collection<FunctionObj>>
        //get() = collectedInstanceFuns

    private val collectedInstanceFuns: MutableMap<MinimalInfo, MutableCollection<RawFunInfo<*>>> = HashMap()
    val instanceFuns: Map<MinimalInfo, Collection<RawFunInfo<*>>>
        get() = collectedInstanceFuns

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        context = when (val typeParams = typeParamResolver[info]) {
            null -> ContextInfo.Direct(info)
            else -> ContextInfo.Template(info, typeParams)
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (checkFlag and access == checkFlag) {
            val instanceRel = functionRelation(name, access)

            with(context) {
                if (thisInfo.nameParts.any { it.firstOrNull()?.isDigit() != false } && instanceRel != STATIC) {
                    // Skip anonymous and local types
                    return null
                }

                val toParse = signature ?: descriptor
                AsmFunParamNamesVisitor { paramNames ->
                    try {
                        val parsedSignature: FunSignatureInfo<*> = when (instanceRel) {
                            STATIC -> funParser.parseStaticFunction(name, paramNames, toParse)
                            CONSTRUCTOR -> when (this) {
                                is ContextInfo.Direct -> funParser.parseDirectConstructor(thisInfo, toParse, paramNames)
                                is ContextInfo.Template -> funParser.parseTemplateConstructor(thisInfo, thisTypeParams, toParse, paramNames)
                            }
                            INSTANCE -> when (this) {
                                is ContextInfo.Direct -> funParser.parseDirectFunction(name, paramNames, toParse, thisInfo)
                                is ContextInfo.Template -> funParser.parseTemplateFunction(name, paramNames, toParse, thisInfo, thisTypeParams)
                            }
                        }

                        val funInfo = FunctionInfoUtil.fromSignatureInfo(
                                file = thisInfo,
                                name = name,
                                signature = parsedSignature,
                                instanceRelation = instanceRel,
                                infoRepo = infoRepo
                        ) ?: error("Cannot create function info! ($thisInfo::$name)")

                        if (instanceRel != INSTANCE) {
                            collectedStaticFuns += RawFunInfo(funInfo, parsedSignature)
                        } else {
                            //val id = info.packageName + info.simpleName
                            /*val store: MutableCollection<FunctionObj> = collectedInstanceFuns[id].orElse {
                                ArrayList<FunctionObj>().apply {
                                    collectedInstanceFuns[id] = this
                                }
                            }
                             */
                            collectedInstanceFuns.getOrPut(thisInfo) { LinkedList() } += RawFunInfo(funInfo, parsedSignature)
                        }
                    } catch (e: NotImplementedError) {
                        System.err.println("Failed to parse $thisInfo $name :: ${signature ?: descriptor}")
                        e.printStackTrace()
                    }
                }
            }
        } else {
            null
        }
    }

}
