import SuperType.DynamicSuper
import SuperType.StaticSuper
import Type.DynamicAppliedType
import Type.NonGenericType
import Type.NonGenericType.DirectType

interface TypeRepo {

    operator fun get(name: String): Type?

}

interface MutableTypeRepo : TypeRepo {

    operator fun plusAssign(type: Type)

    fun createDirect(fullName: String, vararg superTypes: NonGenericType): DirectType {
        superTypes.forEach {
            this += it
        }
        return DirectType(info(fullName), superTypes.map { StaticSuper(it) }).also {
            this += it
        }
    }

    fun createTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type>): TypeTemplate {
        superTypes.forEach {
            this += it
        }
        return TypeTemplate(
            info = info(fullName),
            typeParams = typeParams,
            superTypes = superTypes.map {
                when (it) {
                    is NonGenericType -> StaticSuper(it)
                    is DynamicAppliedType -> DynamicSuper(it)
                }
            }
        )
    }

}

class SetTypeRepo : MutableTypeRepo {

    private val types: MutableMap<String, Type> = HashMap()

    override fun get(name: String): Type? = types[name]

    override fun plusAssign(type: Type) {
        types[type.info.fullName] = type
    }

}
