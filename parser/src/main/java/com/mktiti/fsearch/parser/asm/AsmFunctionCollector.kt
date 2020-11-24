package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.intermediate.DefaultFunctionParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureFunctionParser
import com.mktiti.fsearch.parser.service.FunctionCollector.FunctionCollection
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.*
import kotlin.collections.ArrayList

object AsmFunctionCollector {

    fun collect(infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver, load: AsmCollectorView.() -> Unit): FunctionCollection {
        val visitor = AsmFunctionCollectorVisitor(dependencyResolver, infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return FunctionCollection(
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
    }

    private val funParser: JavaSignatureFunctionParser = DefaultFunctionParser(infoRepo)

    private lateinit var context: ContextInfo<*>

    private val collectedStaticFuns: MutableList<FunctionObj> = ArrayList()
    val staticFuns: List<FunctionObj>
        get() = collectedStaticFuns

    private val collectedInstanceFuns: MutableList<FunctionObj> = ArrayList()
    val instanceFuns: List<FunctionObj>
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
            val isStatic = ((access and Opcodes.ACC_STATIC) > 0)
            with(context) {
                if (info.nameParts.any { it.firstOrNull()?.isDigit() != false } && !isStatic) {
                    // Skip anonymous and local types
                    return null
                }

                val toParse = signature ?: descriptor
                AsmFunParamNamesVisitor { paramNames ->
                    try {
                        val parsedSignature = if (isStatic) {
                            funParser.parseStaticFunction(name, paramNames, toParse)
                        } else {
                            when (this) {
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
                                isStatic = isStatic,
                                typeParams = typeParams,
                                infoRepo = infoRepo
                        ) ?: error("Cannot create function info! ($info::$name)")

                        if (isStatic) {
                            collectedStaticFuns += FunctionObj(funInfo, parsedSignature)
                        } else {
                            collectedInstanceFuns += FunctionObj(funInfo, parsedSignature)
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