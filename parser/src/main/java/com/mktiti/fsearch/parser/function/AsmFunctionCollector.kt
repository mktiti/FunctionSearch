package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
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
        val visitor = AsmFunctionCollectorVisitor(funBuilder)
        object : AsmFunctionCollectorView {
            override fun loadEntry(input: InputStream) {
                ClassReader(input).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            }
        }.load()
        return visitor.methods
    }

}

private class AsmFunctionCollectorVisitor(
        private val funBuilder: JavaFunctionBuilder
) : ClassVisitor(Opcodes.ASM8) {

    companion object {
        private const val checkFlag = Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC
    }

    private lateinit var filename: String

    private val collectedMethods: MutableList<FunctionObj> = ArrayList()
    val methods: List<FunctionObj>
        get() = collectedMethods

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        filename = name.replace('/', '.').replace('$', '.')
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (checkFlag and access == checkFlag) {
            //println("\t\tMethod visited: $name :: ${signature ?: descriptor} throws $exceptions")
            try {
                val parsedSignature = funBuilder.buildFunction(parseFunction(name, signature
                        ?: descriptor))
                if (parsedSignature == null) {
                    println("Failed to parse method $name :: ${signature ?: descriptor} throws $exceptions")
                } else {
                    collectedMethods += FunctionObj(
                            info = FunctionInfo(name, filename),
                            signature = parsedSignature
                    )
                }
            } catch (e: NotImplementedError) {
                System.err.println("Failed to parse $name :: ${signature ?: descriptor}")
                e.printStackTrace()
            }
        }

        return null
    }

}