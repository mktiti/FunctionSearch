package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.util.AsmUtil
import com.mktiti.fsearch.util.map
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.InputStream

interface AsmFunctionCollectorView {

    fun loadEntry(input: InputStream)

}

object AsmFunctionCollector {

    fun collect(javaRepo: JavaRepo, depsRepos: Collection<TypeRepo>, load: AsmFunctionCollectorView.() -> Unit): Collection<FunctionObj> {
        val funBuilder = JavaFunctionBuilder(javaRepo, depsRepos)
        val visitor = AsmFunctionCollectorVisitor(funBuilder, depsRepos)
        object : AsmFunctionCollectorView {
            override fun loadEntry(input: InputStream) {
                ClassReader(input).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            }
        }.load()
        return visitor.methods
    }

}

private class AsmFunctionCollectorVisitor(
        private val funBuilder: JavaFunctionBuilder,
        private val depsRepos: Collection<TypeRepo>
) : ClassVisitor(Opcodes.ASM8) {

    companion object {
        private const val checkFlag = /*Opcodes.ACC_STATIC or*/ Opcodes.ACC_PUBLIC
    }

    private lateinit var currentType: FunctionBuilder.ImplicitThis

    private val collectedMethods: MutableList<FunctionObj> = ArrayList()
    val methods: List<FunctionObj>
        get() = collectedMethods

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val info = AsmUtil.parseName(name)

        currentType = FunctionBuilder.ImplicitThis(
                info = info,
                isGeneric = (signature != null || info.nameParts.isNotEmpty()) && depsRepos.any { it.template(info) != null }
        )
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (checkFlag and access == checkFlag) {
            val isStatic = ((access and Opcodes.ACC_STATIC) > 0)

            if (currentType.info.nameParts.any { it.first().isDigit() } && !isStatic) {
                // Skip anonymous and local types
                return null
            }

            //println("\t\tMethod visited: $name :: ${signature ?: descriptor} throws $exceptions")
            try {
                val parsed = parseFunction(name, signature ?: descriptor)
                val parsedSignature = funBuilder.buildFunction(parsed, isStatic.map(onTrue = null, onFalse = currentType))
                if (parsedSignature == null) {
                    println("Failed to parse method ${currentType.info} $name :: ${signature ?: descriptor} throws $exceptions")
                } else {
                    collectedMethods += FunctionObj(
                            info = FunctionInfo(name, currentType.info.toString()),
                            signature = parsedSignature
                    )
                }
            } catch (e: NotImplementedError) {
                System.err.println("Failed to parse ${currentType.info} $name :: ${signature ?: descriptor}")
                e.printStackTrace()
            }
        }

        return null
    }

}