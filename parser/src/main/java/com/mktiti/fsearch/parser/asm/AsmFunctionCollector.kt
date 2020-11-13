package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.parser.function.FunctionBuilder
import com.mktiti.fsearch.parser.function.JavaFunctionBuilder
import com.mktiti.fsearch.parser.intermediate.DefaultFunctionParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureFunctionParser
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object AsmFunctionCollector {

    fun collect(javaRepo: JavaRepo, infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver, load: AsmCollectorView.() -> Unit): Collection<FunctionObj> {
        val funBuilder = JavaFunctionBuilder(javaRepo, dependencyResolver)
        val visitor = AsmFunctionCollectorVisitor(funBuilder, dependencyResolver, infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return visitor.methods
    }

}

private class AsmFunctionCollectorVisitor(
        private val funBuilder: FunctionBuilder,
        private val dependencyResolver: TypeResolver,
        infoRepo: JavaInfoRepo
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

    private val collectedMethods: MutableList<FunctionObj> = ArrayList()
    val methods: List<FunctionObj>
        get() = collectedMethods

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        context = when (val template = dependencyResolver.template(info)) {
            null -> ContextInfo.Direct(info, info.complete())
            else -> ContextInfo.Template(info, template)
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (checkFlag and access == checkFlag) {
            val isStatic = ((access and Opcodes.ACC_STATIC) > 0)

            val info = context.info

            if (info.nameParts.any { it.firstOrNull()?.isDigit() != false } && !isStatic) {
                // Skip anonymous and local types
                return null
            }

            try {
                val toParse = signature ?: descriptor
                val parsedSignature = if (isStatic) {
                    funParser.parseStaticFunction(name, null, toParse)
                } else {
                    context.let {
                        when (it) {
                            is ContextInfo.Direct -> funParser.parseDirectFunction(name, null, toParse, it.thisType)
                            is ContextInfo.Template -> funParser.parseTemplateFunction(name, null, toParse, it.thisType)
                        }
                    }
                }

                collectedMethods += FunctionObj(
                        info = FunctionInfo(name, info.toString()),
                        signature = parsedSignature
                )

            } catch (e: NotImplementedError) {
                System.err.println("Failed to parse $info $name :: ${signature ?: descriptor}")
                e.printStackTrace()
            }
        }

        return null
    }

}