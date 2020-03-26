import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import Type.NonGenericType
import TypeSignature.DirectSignature

data class FunctionInfo(
    val name: String,
    val fileName: String
)

sealed class TypeSignature {

    abstract val typeParameters: List<TypeParameter>
    abstract val inputParameters: List<Pair<String, ApplicationParameter>>
    abstract val output: ApplicationParameter

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
        override val inputParameters: List<Pair<String, ApplicationParameter>>,
        override val output: ApplicationParameter
    ) : TypeSignature()

    override fun toString() = fullString

}

class FunctionObj(
    val info: FunctionInfo,
    val signature: TypeSignature
) {

    override fun toString() = "fun ${signature.genericString} ${info.fileName}::${info.name}${signature.typeString}"

}

private fun <T : Type> List<T>.namelessParams(): List<Pair<String, T>> = mapIndexed { i, p -> "\$$i" to p }

fun directQuery(
    inputParameters: List<NonGenericType>,
    output: NonGenericType
) = DirectSignature(inputParameters.namelessParams(), output)
