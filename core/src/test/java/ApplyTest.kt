import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.repo.createTestRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplyTest {

    companion object {
        private fun createSetup(): Triple<TypeRepo, TypeTemplate, TypeTemplate> {
            val repo = createTestRepo()

            // A<E>
            val base = TypeTemplate(
                    info = info("A").minimal,
                    typeParams = listOf(TypeParameter("E", repo.defaultTypeBounds)),
                    superTypes = listOf(repo.rootType.completeInfo),
                    samType = null
            )

            val supers = mutableListOf<CompleteMinInfo<*>>()
            // Box<T> : A<Box<Box<T>>>
            val box = TypeTemplate(
                    info = info("Box").minimal,
                    typeParams = listOf(TypeParameter("T", repo.defaultTypeBounds)),
                    superTypes = supers,
                    samType = null
            )

            val innerBox = box.forceDynamicApply(ParamSubstitution(0))
            val outerBox = box.forceDynamicApply(DynamicTypeSubstitution(innerBox.type))
            val sub = DynamicTypeSubstitution(outerBox.type)

            supers += base.forceDynamicApply(listOf(sub)).type

            return Triple(repo, base, box)
        }
    }
/*
    @Test
    fun `test exploding application`() {
        val (repo, base, box) = createSetup()
        val strType = repo["String"]!!
        val strBox = box.forceStaticApply(strType)

        fun nested(box: StaticAppliedType): StaticAppliedType {
            val aSuper = box.superTypes.first()
            return aSuper.typeArgs.first()
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

        // com.mktiti.fsearch.parser.query :: () -> A<Box<Box<String>>>
        val querySignature = QueryType(
                inputParameters = emptyList(),
                output = base.forceStaticApply(
                        box.forceStaticApply(
                                box.forceStaticApply(repo["String"]!!)
                        )
                )
        )

        val resolver: TypeResolver = SingleRepoTypeResolver(repo)
        val fitter: QueryFitter = JavaQueryFitter(resolver)

        val fitResult = fitter.fitsOrderedQuery(
                query = querySignature,
                function = FunctionObj(FunctionInfo("fun", "inmem"), funSignature)
        )

        assertNotNull(fitResult)
    }

 */

}