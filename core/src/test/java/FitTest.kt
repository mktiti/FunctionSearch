import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.repo.createTestRepo
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.UPPER
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.upperBounds
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.core.util.show.TypePrint
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FitTest {

    private val repo = createTestRepo()
    private val rootType = repo["TestRoot"]!!.holder()
    private val defaultTypeBounds = TypeBounds(
            upperBounds = setOf(TypeSubstitution(rootType))
    )
    private val resolver: TypeResolver = SingleRepoTypeResolver(repo)
    private val fitter: QueryFitter = JavaQueryFitter(resolver)
    private val printer: TypePrint = JavaTypePrinter(resolver, MapJavaInfoRepo)

    init {
        println("All Test Types")
        printAll(createTestRepo().allTypes)

        println("All Test Type Templates")
        printAll(createTestRepo().allTemplates)
    }

    private fun printAll(semis: Collection<SemiType>) {
        semis.forEach(printer::printSemiType)
    }

    private fun printAll(vararg semis: SemiType) {
        printAll(semis.toList())
    }

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

        printer.printFit(fitter, length, query)
        assertNotNull(fitter.fitsQuery(query, length))
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
                inputParameters = listOf(collection.forceStaticApply(int.holder())),
                output = list.forceStaticApply(int.holder())
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
                                        TypeSubstitution(
                                                comparable.forceDynamicApply(
                                                        BoundedWildcard.Dynamic(SelfSubstitution, LOWER)
                                                ).holder()
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "coll" to TypeSubstitution(
                                        collection.forceDynamicApply(ParamSubstitution(0)).holder() // Collection<T>
                                )
                        ),
                        output = TypeSubstitution(
                                list.forceDynamicApply(ParamSubstitution(0)).holder() // List<T>
                        )
                )
        )

        printer.printFit(fitter, sort, query)
        assertNotNull(fitter.fitsQuery(query, sort))
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
                inputParameters = listOf(collection.forceStaticApply(boss.holder())),
                output = list.forceStaticApply(boss.holder())
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
                                        TypeSubstitution(
                                                comparable.forceDynamicApply(
                                                        BoundedWildcard.Dynamic(SelfSubstitution, LOWER)
                                                ).holder()
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "coll" to TypeSubstitution(
                                        collection.forceDynamicApply(ParamSubstitution(0)).holder() // Collection<T>
                                )
                        ),
                        output = TypeSubstitution(
                                list.forceDynamicApply(ParamSubstitution(0)).holder() // List<T>
                        )
                )
        )

        printer.printFit(fitter, sort, query)
        assertNotNull(fitter.fitsQuery(query, sort))
    }

    @Test
    fun `test groupingBy query`() {
        val collector = repo.template("Collector")!!
        val supplier = repo.template("Supplier")!!
        val function = repo.template("Function")!!
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

        val personList = list.forceStaticApply(person.holder())
        val bossToPersonMap = hashMap.forceStaticApply(boss.holder(), personList.holder())

        val virtType1 = QueryType.virtualType("downstreamA", listOf(rootType.with(resolver)!!))
        repo += virtType1

        // (Person -> Ceo), (() -> HashMap<Boss, List<Person>>), Collector<Person, _, List<Person>> -> Collector<Boss, _, HashMap<Boss, List<Person>>>
        val query = QueryType(
                inputParameters = listOf(
                        function.forceStaticApply(person.holder(), ceo.holder()),
                        supplier.forceStaticApply(bossToPersonMap.holder()),
                        collector.forceStaticApply(person.holder(), virtType1.holder(), personList.holder())
                ),
                output = collector.forceStaticApply(boss.holder(), boss.holder(), bossToPersonMap.holder())
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
                                TypeParameter("T", defaultTypeBounds),
                                TypeParameter("K", defaultTypeBounds),
                                TypeParameter("D", defaultTypeBounds),
                                TypeParameter("A", defaultTypeBounds),
                                TypeParameter("M", upperBounds( // M extends Map<K, D>
                                        TypeSubstitution(
                                                map.forceDynamicApply(
                                                        ParamSubstitution(1), ParamSubstitution(2)
                                                ).holder()
                                        )
                                ))
                        ),
                        inputParameters = listOf(
                                "classifier" to TypeSubstitution( // Function<? super T, ? extends K>
                                        function.forceDynamicApply(
                                                BoundedWildcard.Dynamic(ParamSubstitution(0), LOWER),
                                                BoundedWildcard.Dynamic(ParamSubstitution(1), UPPER)
                                        ).holder()
                                ),
                                "supplier" to TypeSubstitution( // Supplier<M>
                                        supplier.forceDynamicApply(ParamSubstitution(4)).holder()
                                ),
                                "downstream" to TypeSubstitution( // Collector<? super T, A, D>
                                        collector.forceDynamicApply(
                                                BoundedWildcard.Dynamic(ParamSubstitution(0), LOWER),
                                                ParamSubstitution(3),
                                                ParamSubstitution(2)
                                        ).holder()
                                )
                        ),
                        output = TypeSubstitution( // Collector<T, ?, M>
                                collector.forceDynamicApply(
                                        ParamSubstitution(0),
                                        TypeSubstitution.unboundedWildcard,
                                        ParamSubstitution(4)
                                ).holder()
                        )
                )
        )

        printer.printOrderedFit(fitter, sort, query)
        assertNotNull(fitter.fitsOrderedQuery(query, sort))
    }

}
