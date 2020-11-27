package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate

class VirtualTypeRepo(
        private val virtualTypes: Map<MinimalInfo, DirectType>
) : TypeRepo {

    override val allTypes: Collection<DirectType>
        get() = virtualTypes.values

    override val allTemplates: Collection<TypeTemplate>
        get() = emptyList()

    override fun get(name: String, allowSimple: Boolean): DirectType? = virtualTypes.values.find { type ->
        type.info.fullName == name || (allowSimple && type.info.simpleName == name)
    }

    override fun get(info: MinimalInfo): DirectType? = virtualTypes[info]

    override fun template(name: String, allowSimple: Boolean, paramCount: Int?): TypeTemplate? = null

    override fun template(info: MinimalInfo): TypeTemplate? = null

}