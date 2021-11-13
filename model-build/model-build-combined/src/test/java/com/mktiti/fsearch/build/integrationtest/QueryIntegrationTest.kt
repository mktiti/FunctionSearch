package com.mktiti.fsearch.build.integrationtest

import com.mktiti.fsearch.core.cache.NopInfoCache
import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.model.build.intermediate.QueryImports
import com.mktiti.fsearch.model.build.service.QueryParser
import com.mktiti.fsearch.model.build.service.TypeInfoConnector
import com.mktiti.fsearch.model.build.service.TypeInfoTypeParamResolver
import com.mktiti.fsearch.model.build.service.TypeParamResolver
import com.mktiti.fsearch.model.build.util.InMemTypeParseLog
import com.mktiti.fsearch.model.connect.function.JavaFunctionConnector
import com.mktiti.fsearch.model.connect.type.JavaTypeInfoConnector
import com.mktiti.fsearch.parser.function.DirectoryFunctionInfoCollector
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.type.DirectoryInfoCollector
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.orElse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.test.assertEquals

@Tag("integrationTest")
class QueryIntegrationTest {

    companion object {
        data class TestCase(
                val expectedPath: String,
                val codeBasePath: String
        )

        @JvmStatic
        @Suppress("unused")
        fun tests(): List<TestCase> = listOf(
                TestCase("/test/verification/java-simple.txt", "/test/codebase/java/"),
                TestCase("/test/verification/generic-nested.txt", "/test/codebase/nested-generic/"),
        )

        data class VerificationEntry(
                val query: String,
                val results: Set<FunctionInfo>
        )

        private fun parseEntry(queryLine: String, resultLine: String) = VerificationEntry(
                query = queryLine,
                results = when (val trimmedRes = resultLine.trim()) {
                    "-" -> emptySet()
                    else -> {
                        trimmedRes.split(';').map { result ->
                            val (fileAndName, sig) = result.split('(')
                            val (file, name) = fileAndName.split(regex = "\\.|(::)".toRegex()).cutLast()
                            val (filePackage, fileNameList) = file.partition {
                                it.first().isLowerCase()
                            }
                            val fileName = fileNameList.joinToString(separator = ".")

                            val relation = when {
                                fileAndName.contains("::") -> FunInstanceRelation.STATIC
                                fileName == name -> FunInstanceRelation.CONSTRUCTOR
                                else -> FunInstanceRelation.INSTANCE
                            }

                            val params = if (sig == ")") {
                                emptyList()
                            } else {
                                sig.dropLast(1).split(',').map { param ->
                                    PrimitiveType.fromNameSafe(param)?.let {
                                        FunIdParam.Type(MapJavaInfoRepo.primitive(it))
                                    }.orElse {
                                        val (pack, type) = param.split('.').cutLast()
                                        FunIdParam.Type(MinimalInfo(pack, type))
                                    }
                                }
                            }

                            FunctionInfo(
                                    file = MinimalInfo(filePackage, fileName),
                                    name = name,
                                    relation = relation,
                                    paramTypes = params
                            )
                        }.toSet()
                    }
                }
        )

        private fun testResource(resource: String): Set<VerificationEntry> {
            return QueryIntegrationTest::class.java.getResource(resource).readText().lines().map {
                it.trimStart()
            }.filter {
                !it.startsWith("#") && it.isNotEmpty()
            }.chunked(2).map { (q, r) ->
                parseEntry(q, r)
            }.toSet()
        }

        fun verify(dataRes: String, codeBase: String) {
            val codebasePath = Paths.get(QueryIntegrationTest::class.java.getResource(codeBase).path)
            val infoRepo: JavaInfoRepo = MapJavaInfoRepo

            val typeInfoCollector = DirectoryInfoCollector(infoRepo)
            val typeInfoConnector: TypeInfoConnector = JavaTypeInfoConnector(infoRepo, NopInfoCache, InMemTypeParseLog())
            val funConnector = JavaFunctionConnector(NopInfoCache)

            val (javaRepo, typeResolver, functions) = CompilerUtil.withCompiled(codebasePath) { path ->
                val (javaRepo, baseResolver) = RepoTestUtil.minimalRepos(infoRepo)

                val typeInfoRes = typeInfoCollector.collectTypeInfo(path)
                val loadedTypeResolver = typeInfoConnector.connectArtifact(typeInfoRes)

                println("Loaded SemiTypes: " + loadedTypeResolver.allSemis().toList())

                val typeResolver = FallbackResolver(loadedTypeResolver, baseResolver)
                val typeParamResolver: TypeParamResolver = TypeInfoTypeParamResolver(typeInfoRes.templateInfos)
                val funInfos = DirectoryFunctionInfoCollector(infoRepo).collectFunctions(path, typeParamResolver)
                val functions = funConnector.connect(funInfos)

                Triple(javaRepo, typeResolver, functions)
            }

            val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, typeResolver)

            testResource(dataRes).forEach { (queryStr, results) ->
                println("Fit testing $queryStr")
                val (query, virtuals) = queryParser.parse(queryStr, QueryImports.none)

                val queryResolver = FallbackResolver.withVirtuals(virtuals, typeResolver)
                val fitter: QueryFitter = JavaQueryFitter(infoRepo, queryResolver)

                val fitting = fitter.findFittings(query, functions.staticFunctions.stream(), functions.instanceMethods).map {
                    it.first.info
                }.collect(Collectors.toSet())

                assertEquals(results, fitting.toSet())
            }
        }

    }

    @ParameterizedTest
    @MethodSource("tests")
    fun `integration tests`(testCase: TestCase) {
        verify(
                dataRes = testCase.expectedPath,
                codeBase = testCase.codeBasePath
        )
    }

}
