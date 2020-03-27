import SuperType.StaticSuper
import Type.NonGenericType
import Type.NonGenericType.DirectType

data class QueryType(
    val inputParameters: List<NonGenericType>,
    val output: NonGenericType
) {

    val allParams by lazy {
        inputParameters + output
    }

    override fun toString() = buildString {
        append(inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ", transform = NonGenericType::fullName))
        append(output.fullName)
    }
}

fun virtualType(name: String, supers: List<NonGenericType>): DirectType = DirectType(
    info = TypeInfo(
        name = "_$name",
        packageName = "",
        artifact = ""
    ),
    superTypes = supers.map { StaticSuper(it) }
)
