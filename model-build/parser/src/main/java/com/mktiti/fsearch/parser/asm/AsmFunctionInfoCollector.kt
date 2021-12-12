package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.intermediate.IntFunInstanceRelation.*
import com.mktiti.fsearch.model.build.service.TypeParamResolver
import com.mktiti.fsearch.parser.asm.AsmUtil.isFlagged
import com.mktiti.fsearch.parser.function.FunctionInfoUtil
import com.mktiti.fsearch.parser.parse.DefaultFunctionSignatureInfoBuilder
import com.mktiti.fsearch.parser.parse.JavaFunctionSignatureInfoBuilder
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logTrace
import com.mktiti.fsearch.util.logger
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object AsmFunctionInfoCollector {

    fun collect(infoRepo: JavaInfoRepo, typeParamResolver: TypeParamResolver, load: AsmCollectorView.() -> Unit): FunctionInfoResult {
        val visitor = AsmFunctionInfoCollectorVisitor(infoRepo, typeParamResolver)
        DefaultAsmCollectorView(visitor).load()
        return FunctionInfoResult(
                staticFunctions = visitor.staticFuns,
                instanceMethods = visitor.instanceFuns.map { (i, fs) -> IntInstanceFunEntry(i, fs) }
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
            val thisInfo: IntMinInfo
    ) {

        abstract val thisTypeParams: List<TemplateTypeParamInfo>

        class Direct(
                info: IntMinInfo
        ) : ContextInfo(info) {
            override val thisTypeParams: List<TemplateTypeParamInfo>
                get() = emptyList()
        }

        class Template(
                info: IntMinInfo,
                override val thisTypeParams: List<TemplateTypeParamInfo>
        ) : ContextInfo(info)
    }

    companion object {
        private const val checkFlag = Opcodes.ACC_PUBLIC

        private fun functionRelation(name: String, access: Int): IntFunInstanceRelation = when {
            name == "<init>" -> CONSTRUCTOR
            access.isFlagged(Opcodes.ACC_STATIC) -> STATIC
            else -> INSTANCE
        }
    }

    private val funParser: JavaFunctionSignatureInfoBuilder = DefaultFunctionSignatureInfoBuilder(infoRepo)

    private val log = logger()

    private var context: ContextInfo? = null

    private val collectedStaticFuns: MutableList<RawFunInfo> = ArrayList()
    val staticFuns: List<RawFunInfo>
        get() = collectedStaticFuns

    private val collectedInstanceFuns: MutableMap<IntMinInfo, MutableList<RawFunInfo>> = HashMap()
    val instanceFuns: Map<IntMinInfo, List<RawFunInfo>>
        get() = collectedInstanceFuns

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        context = if (AsmUtil.wrapContainsAnonymousOrInner(info)) {
            // Skip anonymous nested and local class
            null
        } else {
            when (val typeParams = typeParamResolver[info]) {
                null -> ContextInfo.Direct(info)
                else -> ContextInfo.Template(info, typeParams)
            }
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return context?.let { setContext ->
            if (access.isFlagged(checkFlag)) {
                val instanceRel = functionRelation(name, access)
                val thisInfo = setContext.thisInfo
                val toParse = signature ?: descriptor

                AsmFunParamNamesVisitor { paramNames ->
                    try {
                        val parsedSignature: FunSignatureInfo<*> = when (instanceRel) {
                            STATIC -> funParser.parseStaticFunction(name, paramNames, toParse)
                            CONSTRUCTOR -> when (setContext) {
                                is ContextInfo.Direct -> funParser.parseDirectConstructor(thisInfo, toParse, paramNames)
                                is ContextInfo.Template -> funParser.parseTemplateConstructor(thisInfo, setContext.thisTypeParams, toParse, paramNames)
                            }
                            INSTANCE -> when (setContext) {
                                is ContextInfo.Direct -> funParser.parseDirectFunction(name, paramNames, toParse, thisInfo)
                                is ContextInfo.Template -> funParser.parseTemplateFunction(name, paramNames, toParse, thisInfo, setContext.thisTypeParams)
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
                            collectedStaticFuns += RawFunInfo.of(funInfo, parsedSignature)
                        } else {
                            collectedInstanceFuns.getOrPut(thisInfo) { LinkedList() } += RawFunInfo.of(funInfo, parsedSignature)
                        }
                    } catch (e: NotImplementedError) {
                        log.logError(e) { "Error while parsing function $thisInfo.$name " }
                    }
                }
            } else {
                null
            }
        }
    }

    override fun visitEnd() {
        log.logTrace { "Parsed functions in file ${context?.thisInfo}" }
    }

}
