import ApplicationParameter.Substitution.ParamSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import SuperType.DynamicSuper.EagerDynamic
import SuperType.StaticSuper.EagerStatic
import Type.NonGenericType.StaticAppliedType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import repo.TypeRepo
import repo.createTestRepo
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplyTest {

    companion object {
        private fun createSetup(): Triple<TypeRepo, TypeTemplate, TypeTemplate> {
            val repo = createTestRepo()

            // A<E>
            val base = TypeTemplate(
                    info = info("A"),
                    typeParams = listOf(TypeParameter("E", repo.defaultTypeBounds)),
                    superTypes = listOf(EagerStatic(repo.rootType))
            )

            val supers = mutableListOf<SuperType<Type>>()
            // Box<T> : A<Box<Box<T>>>
            val box = TypeTemplate(
                    info = info("Box"),
                    typeParams = listOf(TypeParameter("T", repo.defaultTypeBounds)),
                    superTypes = supers
            )

            val innerBox = box.forceDynamicApply(ParamSubstitution(0))
            val outerBox = box.forceDynamicApply(DynamicTypeSubstitution(innerBox))
            val sub = DynamicTypeSubstitution(outerBox)

            supers += EagerDynamic(
                    Type.DynamicAppliedType(
                            baseType = base,
                            typeArgMapping = listOf(sub)
                    )
            )

            return Triple(repo, base, box)
        }
    }

    @Test
    fun `test exploding application`() {
        val (repo, base, box) = createSetup()
        val strType = repo["String"]!!
        val strBox = box.forceStaticApply(strType)

        fun nested(box: StaticAppliedType): StaticAppliedType {
            val aSuper = box.superTypes.first().type as StaticAppliedType
            return aSuper.typeArgs.first() as StaticAppliedType
        }

        tailrec fun checkArgs(box: StaticAppliedType, goal: Int) {
            if (goal == 0) {
                assertEquals(strType, box.typeArgs.first())
            } else {
                assertEquals("Box", box.baseType.info.name)
                checkArgs(box.typeArgs.first() as StaticAppliedType, goal - 1)
            }
        }

        tailrec fun nestedCheck(box: StaticAppliedType, goal: Int, depth: Int = 0) {
            if (goal == depth) {
                return
            }

            checkArgs(box, depth)
            nestedCheck(nested(box), goal, depth + 1)
        }
        nestedCheck(strBox, 10000)

        // fun :: () -> Box<String>
        val funSignature = TypeSignature.DirectSignature(
                inputParameters = emptyList(),
                output = strBox
        )

        // query :: () -> A<Box<Box<String>>>
        val querySignature = QueryType(
                inputParameters = emptyList(),
                output = base.forceStaticApply(
                        box.forceStaticApply(
                                box.forceStaticApply(repo["String"]!!)
                        )
                )
        )

        val fitResult = fitsOrderedQuery(
                query = querySignature,
                function = FunctionObj(FunctionInfo("fun", "inmem"), funSignature)
        )

        assertNotNull(fitResult)
    }

}