package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.util.genericString

data class FunctionInfo(
    val name: String,
    val fileName: String
)

sealed class TypeSignature {

    abstract val typeParameters: List<TypeParameter>
    abstract val inputParameters: List<Pair<String, Substitution>>
    abstract val output: Substitution

    val typeString by lazy {
        buildString {
            val inputsString =
                inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ") { (name, type) ->
                    if (name.startsWith("$")) type.toString() else "$name: $type"
                }
            append(inputsString)
            append(output)
        }
    }

    val genericString
        get() =  if (typeParameters.isNotEmpty()) typeParameters.genericString() else ""

    private val fullString by lazy {
        buildString {
            genericString.apply {
                if (isNotBlank()) {
                    append(this)
                    append(' ')
                }
            }
            append(typeString)
        }
    }

    class DirectSignature(
        override val inputParameters: List<Pair<String, StaticTypeSubstitution>>,
        override val output: StaticTypeSubstitution
    ) : TypeSignature() {

        constructor(
            inputParameters: List<Pair<String, NonGenericType>>,
            output: NonGenericType
        ) : this(
            inputParameters = inputParameters.map { (name, type) -> name to StaticTypeSubstitution(type.completeInfo) },
            output = StaticTypeSubstitution(output.completeInfo)
        )

        override val typeParameters: List<TypeParameter>
            get() = emptyList()

    }

    class GenericSignature(
            override val typeParameters: List<TypeParameter>,
            override val inputParameters: List<Pair<String, Substitution>>,
            override val output: Substitution
    ) : TypeSignature()

    override fun toString() = fullString

}

class FunctionObj(
        val info: FunctionInfo,
        val signature: TypeSignature
) {

    override fun toString() = "fun ${signature.genericString} ${info.fileName}::${info.name}${signature.typeString}"

}
