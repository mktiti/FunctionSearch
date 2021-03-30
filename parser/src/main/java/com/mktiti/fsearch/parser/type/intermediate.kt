package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo

sealed class IntermediateTypeData {

    abstract val info: MinimalInfo
    abstract val superTypes: List<CompleteMinInfo<*>>
    abstract val samSignature: String?

    data class Direct(
            override val info: MinimalInfo,
            override val superTypes: List<CompleteMinInfo.Static>,
            override val samSignature: String?
    ) : IntermediateTypeData()

    data class Template(
            override val info: MinimalInfo,
            val typeParams: List<Pair<String, CompleteMinInfo<*>>>,
            override val superTypes: List<CompleteMinInfo<*>>,
            override val samSignature: String?
    ) : IntermediateTypeData()

}
