package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.util.cutLast

object AsmUtil {

    fun parseName(type: String): MinimalInfo {
        val splitName = type.split('/')
        val (packageName, simpleName) = splitName.cutLast()
        return MinimalInfo(packageName, simpleName.replace('$', '.'))
    }

    fun parseCompleteStaticName(type: String): CompleteMinInfo.Static = parseName(type).complete()

}
