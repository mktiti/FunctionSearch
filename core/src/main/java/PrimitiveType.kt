
enum class PrimitiveType(val javaName: String, val signature: Char) {
    BOOL("boolean", 'Z'),
    BYTE("byte", 'B'),
    CHAR("char", 'C'),
    SHORT("short", 'S'),
    INT("int", 'I'),
    LONG("long", 'J'),
    FLOAT("float", 'F'),
    DOUBLE("double", 'D');

    companion object {
        fun fromSignatureSafe(sign: Char) = values().find { it.signature == sign }

        fun fromSignature(sign: Char) = fromSignatureSafe(sign)!!

        fun fromSignature(sign: String) = fromSignature(sign.first())

        fun fromSignatureSafe(sign: String) = fromSignatureSafe(sign.first())
    }

    override fun toString() = javaName

}