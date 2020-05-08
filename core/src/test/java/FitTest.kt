import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.createTestRepo
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard.LowerBound
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard.UpperBound
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.upperBounds
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.core.util.printFit
import com.mktiti.fsearch.core.util.printSemiType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FitTest {

    companion object {
        init {
            println("All Test Types")
            printAll(createTestRepo().allTypes)

            println("All Test Type Templates")
            printAll(createTestRepo().allTemplates)
        }

        private fun printAll(semis: Collection<SemiType>) {
            semis.forEach(::printSemiType)
        }

        private fun printAll(vararg semis: SemiType) {
            printAll(semis.toList())
        }
    }

    private val repo = createTestRepo()

    @Test
    fun `test length success query`() {
        val charSeq = repo["CharSequence"]!!
        val string = repo["String"]!!
        val int = repo["Int"]!!
        val int64 = repo["Int64"]!!
        printAll(
            charSeq,
            string,
            int,
            int64
        )

        val query = QueryType(
                inputParameters = listOf(string),
                output = int
        )

        val length = FunctionObj(
                info = FunctionInfo(
                        name = "length",
                        fileName = "Str"
                ),
                signature = TypeSignature.DirectSignature(
                        inputParameters = listOf("str" to charSeq),
                        output = int64
                )
        )

        printFit(length, query)
        assertNotNull(fitsQuery(query, length))
    }

    @Test
    fun `test sort success query`() {
        val comparable = repo.template("Comparable")!!
        val list = repo.template("List")!!
        val collection = repo.template("Collection")!!
        val int = repo["Int"]!!
        printAll(
            comparable,
            list,
            collection,
            int
        )

        val query = QueryType(
                inputParameters = listOf(collection.forceStaticApply(int)),
                output = list.forceStaticApply(int)
        )

        // <T extends Comparable<? super T>> (Collection<T>) -> List<T>
        val sort = FunctionObj(
                info = FunctionInfo(
                        name = "sort",
                        fileName = "Collections"
                ),
                signature = TypeSignature.GenericSignature(
                        typeParameters = listOf(
                                TypeParameter("T", upperBounds(
                                        DynamicTypeSubstitution(
                                                comparable.forceDynamicApply(
                                                        LowerBound(SelfSubstitution)
                                                )
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "coll" to DynamicTypeSubstitution(
                                        collection.forceDynamicApply(ParamSubstitution(0)) // Collection<T>
                                )
                        ),
                        output = DynamicTypeSubstitution(
                                list.forceDynamicApply(ParamSubstitution(0)) // List<T>
                        )
                )
        )

        printFit(sort, query)
        assertNotNull(fitsQuery(query, sort))
    }

    @Test
    fun `test sort parent comparable query`() {
        val comparable = repo.template("Comparable")!!
        val list = repo.template("List")!!
        val collection = repo.template("Collection")!!
        val boss = repo["Boss"]!!
        printAll(
            comparable,
            list,
            collection,
            boss
        )

        val query = QueryType(
                inputParameters = listOf(collection.forceStaticApply(boss)),
                output = list.forceStaticApply(boss)
        )

        // <T extends Comparable<? super T>> (Collection<T>) -> List<T>
        val sort = FunctionObj(
                info = FunctionInfo(
                        name = "sort",
                        fileName = "Collections"
                ),
                signature = TypeSignature.GenericSignature(
                        typeParameters = listOf(
                                TypeParameter("T", upperBounds(
                                        DynamicTypeSubstitution(
                                                comparable.forceDynamicApply(
                                                        LowerBound(SelfSubstitution)
                                                )
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "coll" to DynamicTypeSubstitution(
                                        collection.forceDynamicApply(ParamSubstitution(0)) // Collection<T>
                                )
                        ),
                        output = DynamicTypeSubstitution(
                                list.forceDynamicApply(ParamSubstitution(0)) // List<T>
                        )
                )
        )

        printFit(sort, query)
        assertNotNull(fitsQuery(query, sort))
    }

    @Test
    fun `test groupingBy query`() {
        val collector = repo.template("Collector")!!
        val supplier = repo.template("Supplier")!!
        val function = repo.functionType(1)
        val list = repo.template("List")!!
        val map = repo.template("Map")!!
        val hashMap = repo.template("HashMap")!!
        val person = repo["Person"]!!
        val boss = repo["Boss"]!!
        val ceo = repo["Ceo"]!!
        printAll(
            collector,
            supplier,
            function,
            list,
            map,
            hashMap,
            person,
            boss,
            ceo
        )

        val personList = list.forceStaticApply(person)
        val bossToPersonMap = hashMap.forceStaticApply(boss, personList)

        val virtType1 = virtualType("downstreamA", listOf(repo.rootType))

        // (Person -> Ceo), (() -> HashMap<Boss, List<Person>>), Collector<Person, _, List<Person>> -> Collector<Boss, _, HashMap<Boss, List<Person>>>
        val query = QueryType(
                inputParameters = listOf(
                        function.forceStaticApply(person, ceo),
                        supplier.forceStaticApply(bossToPersonMap),
                        collector.forceStaticApply(person, virtType1, personList)
                ),
                output = collector.forceStaticApply(boss, boss, bossToPersonMap)
        )

        // <T, K, D, A, M extends Map<K, D>>
        // Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream)
        val sort = FunctionObj(
                info = FunctionInfo(
                        name = "groupingBy",
                        fileName = "Collectors"
                ),
                signature = TypeSignature.GenericSignature(
                        typeParameters = listOf(
                                TypeParameter("T", repo.defaultTypeBounds),
                                TypeParameter("K", repo.defaultTypeBounds),
                                TypeParameter("D", repo.defaultTypeBounds),
                                TypeParameter("A", repo.defaultTypeBounds),
                                TypeParameter("M", upperBounds( // M extends Map<K, D>
                                        DynamicTypeSubstitution(
                                                map.forceDynamicApply(
                                                        ParamSubstitution(1), ParamSubstitution(2)
                                                )
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "classifier" to DynamicTypeSubstitution( // Function<? super T, ? extends K>
                                        function.forceDynamicApply(
                                                LowerBound(ParamSubstitution(0)),
                                                UpperBound(ParamSubstitution(1))
                                        )
                                ),
                                "supplier" to DynamicTypeSubstitution( // Supplier<M>
                                        supplier.forceDynamicApply(ParamSubstitution(4))
                                ),
                                "downstream" to DynamicTypeSubstitution( // Collector<? super T, A, D>
                                        collector.forceDynamicApply(
                                                LowerBound(ParamSubstitution(0)),
                                                ParamSubstitution(3),
                                                ParamSubstitution(2)
                                        )
                                )
                        ),
                        output = DynamicTypeSubstitution( // Collector<T, ?, M>
                                collector.forceDynamicApply(
                                        ParamSubstitution(0),
                                        StaticTypeSubstitution(boss),
                                        ParamSubstitution(4)
                                )
                        )
                )
        )

        printFit(sort, query)
        assertNotNull(fitsOrderedQuery(query, sort))
    }

}
