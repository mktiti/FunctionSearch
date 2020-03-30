import ApplicationParameter.Substitution
import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import Type.NonGenericType

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

    val fullString by lazy {
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
            inputParameters = inputParameters.map { (name, type) -> name to StaticTypeSubstitution(type) },
            output = StaticTypeSubstitution(output)
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
