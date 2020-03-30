import java.util.*

data class TypeInfo(
    val name: String,
    val packageName: String = "",
    val artifact: String = "JCLv8",
    val virtual: Boolean = false
) {

    val fullName: String = if (packageName.isNotBlank()) "$packageName.$name" else name



    override fun toString() = buildString {
        if (artifact.isNotBlank() && !artifact.startsWith("JCLv")) {
            append(artifact)
            append(": ")
        }

        append(fullName)
    }

    override fun equals(other: Any?): Boolean = when {
        other !is TypeInfo -> false
        other.virtual -> this === other
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
    val packageName = packageParts.dropLast(1).joinToString(separator = ".")

    if (className.isBlank()) {
        throw TypeInfoParseException(fullString, "Class name is empty")
    }

    return TypeInfo(className, packageName, artifact)
}
