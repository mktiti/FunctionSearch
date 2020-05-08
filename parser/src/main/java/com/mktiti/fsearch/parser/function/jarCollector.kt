package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipFile

class MethodCollector(
        private val file: String,
        private val funBuilder: JavaFunctionBuilder
) : ClassVisitor(ASM8) {

    companion object {
        private const val checkFlag = ACC_STATIC or ACC_PUBLIC
    }

    private val collectedMethods: MutableList<FunctionObj> = ArrayList()
    val methods: List<FunctionObj>
        get() = collectedMethods

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (checkFlag and access == checkFlag) {
            //println("\t\tMethod visited: $name :: ${signature ?: descriptor} throws $exceptions")
            try {
                val parsedSignature = funBuilder.buildFunction(parseFunction(name, signature ?: descriptor))
                if (parsedSignature == null) {
                    println("Failed to parse method $name :: ${signature ?: descriptor} throws $exceptions")
                } else {
                    collectedMethods += FunctionObj(
                            info = FunctionInfo(name, file),
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

class AsmParser(
        javaRepo: JavaRepo,
        typeRepo: TypeRepo
) {

    private val funBuilder = JavaFunctionBuilder(typeRepo, javaRepo)

    private fun loadFunctions(file: String, input: InputStream): Collection<FunctionObj> {
        //println("Loading functions from $file")
        val classReader = ClassReader(input)
        val methodCollector = MethodCollector(file, funBuilder)

        classReader.accept(methodCollector, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
        return methodCollector.methods
    }

    fun loadFunctions(jarPath: Path): Collection<FunctionObj> {
        return ZipFile(jarPath.toFile()).use { jar ->
            val entries = jar.entries().toList()
            println("Entries in $jarPath (${entries.size}):")

            entries.toList().filter {
                it.name.endsWith(".class")
            }.flatMap { entry ->
                loadFunctions(entry.name, jar.getInputStream(entry))
            }
        }
    }

}

