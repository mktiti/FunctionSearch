package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeHolder

interface InfoFitter {

    fun fit(parInfo: MinimalInfo, subInfo: MinimalInfo): Boolean

    fun fit(parInfo: CompleteMinInfo<*>, subInfo: CompleteMinInfo<*>): Boolean = fit(parInfo.base, subInfo.base)

    fun fit(parInfo: TypeHolder<*, *>, subInfo: TypeHolder<*, *>): Boolean = fit(parInfo.info, subInfo.info)

}

class JavaInfoFitter(
        private val infoRepo: JavaInfoRepo
) : InfoFitter {

    private fun anyPrimitive(info: MinimalInfo) = infoRepo.ifPrimitive(info) ?: infoRepo.ifBoxed(info)

    override fun fit(parInfo: MinimalInfo, subInfo: MinimalInfo): Boolean {
        val parPrimitive = anyPrimitive(parInfo) ?: return parInfo == subInfo
        val subPrimitive = anyPrimitive(subInfo) ?: return parInfo == subInfo

        return subPrimitive == parPrimitive
    }

}
