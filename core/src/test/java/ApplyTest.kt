import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
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

            val rootType = repo["TestRoot"]!!.holder()
            val defaultTypeBounds = TypeBounds(
                    upperBounds = setOf(TypeSubstitution(rootType))
            )

            // A<E>
            val base = TypeTemplate(
                    info = info("A").minimal,
                    typeParams = listOf(TypeParameter("E", defaultTypeBounds)),
                    superTypes = listOf(rootType),
                    samType = null
            )

            // Box<T> : A<Box<Box<T>>>

            val boxInfo = info("Box").minimal
            fun boxOf(param: ApplicationParameter) = boxInfo.dynamicComplete(listOf(param)).holder()
            val box = TypeTemplate(
                    info = boxInfo,
                    typeParams = listOf(TypeParameter("T", defaultTypeBounds)),
                    superTypes = listOf(
                            base.forceDynamicApply(
                                    TypeSubstitution(boxOf(
                                            TypeSubstitution(boxOf(ParamSubstitution(0)))
                                    ))
                            ).holder()
                    ),
                    samType = null
            )

            repo += base
            repo += box

            return Triple(repo, base, box)
        }

        private fun funInfo(name: String, className: String) = FunctionInfo(
                file = MinimalInfo(listOf("org", "test"), className),
                name = name,
                isStatic = true,
                paramTypes = emptyList()
        )
    }

    @Test
    fun `test exploding application`() {
        val (repo, base, box) = createSetup()
        val strType = repo["String"]!!
        val strBox = box.forceStaticApply(strType.holder())

        val resolver: TypeResolver = SingleRepoTypeResolver(repo)
        val fitter: QueryFitter = JavaQueryFitter(MapJavaInfoRepo, resolver)

        fun nested(currentBox: Type.NonGenericType): Type.NonGenericType {
            return box.forceStaticApply(currentBox.holder())
        }

        tailrec fun checkArgs(box: CompleteMinInfo.Static, goal: Int) {
            val checkTarget = box.args.first()
            if (goal == 0) {
                assertEquals(strType.completeInfo, checkTarget)
            } else {
                assertEquals("Box", box.base.simpleName)
                checkArgs(checkTarget, goal - 1)
            }
        }

        tailrec fun nestedCheck(box: Type.NonGenericType, goal: Int, depth: Int = 0) {
            if (goal == depth) {
                return
            }

            checkArgs(box.completeInfo, depth)
            nestedCheck(nested(box), goal, depth + 1)
        }
        nestedCheck(strBox, 10_000)

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
                                box.forceStaticApply(repo["String"]!!.holder()).holder()
                        ).holder()
                )
        )

        val fitResult = fitter.fitsOrderedQuery(
                query = querySignature,
                function = FunctionObj(funInfo("fun", "inmem"), funSignature)
        )

        assertNotNull(fitResult)
    }

}