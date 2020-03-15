import org.apache.bcel.Repository

interface TypeParser {

    fun loadType(name: String): Type?

}

class BcelTypeParser : TypeParser {

    override fun loadType(name: String): Type? {
        val loaded = Repository.lookupClass(name) ?: return null
        return null
    }

}
