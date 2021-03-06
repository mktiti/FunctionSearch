package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.type.MinimalInfo

object InfoHelper {

    fun minimalInfo(name: String): MinimalInfo? {
        val parts = name.split(".").map {
            if (it.isBlank()) null else it
        }.liftNull() ?: return null

        val packageParts = parts.takeWhile { it.first().isLowerCase() }
        val simpleNameParts = parts.drop(packageParts.size)

        if (simpleNameParts.isEmpty() || simpleNameParts.any { it.first().isLowerCase() }) {
            return null
        }

        val simpleName = simpleNameParts.joinToString(separator = ".")
        return MinimalInfo(packageParts, simpleName)
    }

}