package com.mktiti.fsearch.model.build.util

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeInfo

interface JavaTypeParseLog {

    fun rawUsage(user: TypeInfo, used: MinimalInfo)

    fun typeNotFound(user: TypeInfo, used: MinimalInfo)

    fun applicationError(location: TypeInfo, applicable: MinimalInfo)

}

class InMemTypeParseLog : JavaTypeParseLog {

    data class EntryData(val user: TypeInfo, val used: MinimalInfo)

    private val modRawUsages = mutableListOf<EntryData>()
    private val modTypeNotFounds = mutableListOf<EntryData>()
    private val modApplicationErrors = mutableListOf<EntryData>()

    val rawUsages: List<EntryData>
        get() = modRawUsages

    val typeNotFounds: List<EntryData>
        get() = modTypeNotFounds

    val applicableErrors: List<EntryData>
        get() = modApplicationErrors

    val allCount
        get() = modRawUsages.size + modTypeNotFounds.size + modApplicationErrors.size

    override fun rawUsage(user: TypeInfo, used: MinimalInfo) {
        modRawUsages += EntryData(user, used)
    }

    override fun typeNotFound(user: TypeInfo, used: MinimalInfo) {
        modTypeNotFounds += EntryData(user, used)
    }

    override fun applicationError(location: TypeInfo, applicable: MinimalInfo) {
        modApplicationErrors += EntryData(location, applicable)
    }

}
