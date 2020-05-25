package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.util.cutLast

object AsmUtil {

    fun parseName(type: String): MinimalInfo {
        val splitName = type.split('/')
        val (packageName, simpleName) = splitName.cutLast()
        return MinimalInfo(packageName, simpleName.replace('$', '.'))
    }

}
