import java.util.*

typealias PackageName = List<String>

data class TypeInfo(
        val name: String,
        val packageName: PackageName = emptyList(),
        val artifact: String = "JCLv8",
        val virtual: Boolean = false
) {

    companion object {
        val uniqueVirtual = TypeInfo("_", emptyList(), "", true)

        val anyWildcard = TypeInfo("?", emptyList(), "", true)
    }

    val fullName
        get() = if (packageName.isNotEmpty()) {
            packageName.joinToString(prefix = "", separator = ".", postfix = ".")
        } else {
            ""
        } + name

    override fun toString() = buildString {
        if (artifact.isNotBlank() && !artifact.startsWith("JCLv")) {
            append(artifact)
            append(": ")
        }

        append(fullName)
    }

    override fun equals(other: Any?): Boolean = when {
        other !is TypeInfo -> false
        this === anyWildcard -> true
        other === anyWildcard -> true
        virtual -> this === other && this !== uniqueVirtual
        else -> {
            other.name == name && other.packageName == packageName && other.artifact == artifact
        }
    }

    override fun hashCode(): Int = Objects.hash(name, packageName, artifact, virtual)
}

fun info(fullString: String): TypeInfo {
    val (artifact, fullName) = if (!fullString.contains(":")) {
        "JCLv8" to fullString
    } else {
        val split = fullString.split(':').map(String::trim)
        if (split.size == 2) {
            split[0] to split[1]
        } else {
            throw TypeInfoParseException(fullString, "More than one ':' defined")
        }
    }

    val innerSplit = fullName.split('$', limit = 2)
    val (outerFullName, innerParts) = if (innerSplit.size == 2) {
        innerSplit[0] to ("." + innerSplit[1].replace('$', '.'))
    } else {
        fullName to ""
    }

    val packageParts = outerFullName.split('.')
    val className = packageParts.last() + innerParts
    val packageName = packageParts.dropLast(1)

    if (className.isBlank()) {
        throw TypeInfoParseException(fullString, "Class name is empty")
    }

    return TypeInfo(className, packageName, artifact)
}
