import java.lang.RuntimeException

open class TypeException(message: String) : RuntimeException(message)

class TypeApplicationException(message: String) : TypeException(message)
