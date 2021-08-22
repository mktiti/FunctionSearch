package com.mktiti.fsearch.parser.intermediate.asm

object JavaSamUtil {

    /**
     * Methods of java.lang.Object
     * Some are excluded because they are ~internal (not to be overrode)
     */
    @Suppress("SpellCheckingInspection")
    private val objectMethods = listOf(
            "clone" to "()Ljava/lang/Object;",
            "equals" to "(Ljava/lang/Object;)Z",
            "finalize" to "()V",
            "hashCode" to "()I",
            "toString" to "()Ljava/lang/String;"
    )

    /**
     * FunctionalInterfaces may have more than one abstract methods, provided that the extra method
     *  is always implemented, i.e. matches a method of java.lang.Object
     *
     *  For example: java.util.Comparator<T> has two abstract methods:
     *      - int compare(T var1, T var2); <-- the "SAM"
     *      - boolean equals(Object var1); <-- always overrode, mainly here as a sign that it may be useful
     *
     *  For such cases, during parsing these should be excluded when trying for SAM compatibility.
     *  This method determines whether the method can be excluded.
     *
     *  Could be reworked to use the parsed functions, but Object is unlikely to receive new methods anytime soon.
     */
    fun isObjectMethod(name: String, signature: String): Boolean {
        return (name to signature) in objectMethods
    }

}
