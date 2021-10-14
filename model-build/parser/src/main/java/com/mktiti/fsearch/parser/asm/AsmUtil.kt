package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.model.build.intermediate.IntMinInfo
import com.mktiti.fsearch.model.build.intermediate.IntStaticCmi
import com.mktiti.fsearch.util.cutLast

object AsmUtil {

    fun annotationDescriptor(info: MinimalInfo) = (info.packageName + info.fullName)
            .joinToString(prefix = "L", separator = "/", postfix = ";")

    fun parseName(type: String): IntMinInfo {
        val splitName = type.split('/')
        val (packageName, simpleName) = splitName.cutLast()
        return IntMinInfo(packageName, simpleName.replace('$', '.'))
    }

    fun parseCompleteStaticName(type: String): IntStaticCmi = parseName(type).complete()

}
