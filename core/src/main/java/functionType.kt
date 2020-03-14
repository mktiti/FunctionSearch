import Type.NonGenericType
import TypeSignature.DirectSignature
import TypeSignature.GenericSignature

data class FunctionInfo(
    val name: String,
    val fileName: String
)

sealed class TypeSignature {

    abstract val typeParameters: List<TypeParameter>
    abstract val inputParameters: List<Pair<String, Type>>
    abstract val output: Type

    val typeString by lazy {
        buildString {
            val inputsString =
                inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ") { (name, type) ->
                    "$name: ${type.fullName}"
                }
            append(inputsString)
            append(output.fullName)
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
        override val inputParameters: List<Pair<String, NonGenericType>>,
        override val output: NonGenericType
    ) : TypeSignature() {

        override val typeParameters: List<TypeParameter>
            get() = emptyList()

    }

    class GenericSignature(
        override val typeParameters: List<TypeParameter>,
        override val inputParameters: List<Pair<String, Type>>,
        override val output: Type
    ) : TypeSignature()

    override fun toString() = fullString

}

class Function(
    val info: FunctionInfo,
    val signature: TypeSignature
) {

    override fun toString() = "fun ${signature.genericString} ${info.fileName}::${info.name} ${signature.typeString}"

}

private fun <T : Type> List<T>.queryParams(): List<Pair<String, T>> = mapIndexed { i, p -> "\$$i" to p }

fun directQuery(
    inputParameters: List<NonGenericType>,
    output: NonGenericType
) = DirectSignature(inputParameters.queryParams(), output)

fun genericQuery(
    typeParameters: List<TypeParameter>,
    inputParameters: List<Type>,
    output: Type
) = GenericSignature(typeParameters, inputParameters.queryParams(), output)
