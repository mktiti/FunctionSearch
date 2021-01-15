package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.util.cutLast

class MapTypeRepo(
        private val directs: Map<MinimalInfo, DirectType>,
        private val templates: Map<MinimalInfo, TypeTemplate>
) : TypeRepo {

    companion object {
        private fun namePred(name: String): (SemiType) -> Boolean = { it.info.simpleName == name }

        private fun namePred(name: String, paramCount: Int): (TypeTemplate) -> Boolean = {
            it.info.simpleName == name && it.typeParamCount == paramCount
        }

        private fun namePredChoose(name: String, paramCount: Int?) = if (paramCount == null) {
            namePred(name)
        } else {
            namePred(name, paramCount)
        }

        private fun canBeSimple(name: String, allowSimple: Boolean): Boolean {
            if (allowSimple) {
                return if ("." !in name) {
                    true
                } else {
                    name.split(".").all {
                        it.isNotBlank() && it.first().isUpperCase()
                    }
                }
            }

            return false
        }
    }

    override val allTypes: Collection<DirectType> = directs.values
    override val allTemplates: Collection<TypeTemplate> = templates.values

    override fun get(name: String, allowSimple: Boolean): DirectType? {
        if (canBeSimple(name, allowSimple)) {
            allTypes.find { it.info.simpleName == name }?.let {
                return it
            }
        }

        val (packages, onlyName) = name.split('.').cutLast()
        return directs[MinimalInfo(packages, onlyName)]
    }

    override fun get(info: MinimalInfo): DirectType? = directs[info]

    override fun template(name: String, allowSimple: Boolean, paramCount: Int?): TypeTemplate? {
        if (canBeSimple(name, allowSimple)) {
            allTemplates.find(namePredChoose(name, paramCount))?.let {
                return it
            }
        }

        val (packages, onlyName) = name.split('.').cutLast()
        return templates[MinimalInfo(packages, onlyName)]
    }

    override fun template(info: MinimalInfo): TypeTemplate? = templates[info]

}
