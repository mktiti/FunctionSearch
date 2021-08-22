package com.mktiti.fsearch.parser.intermediate.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import java.io.InputStream

interface AsmCollectorView {

    fun loadEntry(input: InputStream)

}

class DefaultAsmCollectorView(
        private val classVisitor: ClassVisitor,
        private val flag: Int = ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES
) : AsmCollectorView {

    override fun loadEntry(input: InputStream) {
        ClassReader(input).accept(classVisitor, flag)
    }

}
