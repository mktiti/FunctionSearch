package com.mktiti.fsearch.parser.maven

data class MavenRepoInfo(
        val name: String,
        val baseUrl: String
)

data class MavenArtifact(
        val group: List<String>,
        val name: String,
        val version: String
) {

    companion object {
        fun parse(simple: String): MavenArtifact? {
            val parts = simple.split(':')
            if (parts.size != 3) {
                return null
            }

            val (groupName, name, version) = parts
            return MavenArtifact(
                    group = groupName.split('.'),
                    name = name,
                    version = version
            )
        }
    }

    override fun toString() = buildString {
        append(group.joinToString(separator = "."))
        append(':')
        append(name)
        append(':')
        append(version)
    }

}