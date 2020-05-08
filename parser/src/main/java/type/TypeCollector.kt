package type

import MutablePrefixTree
import org.objectweb.asm.ClassReader
import repo.JavaInfoRepo
import repo.JavaRepo
import repo.MapJavaInfoRepo
import repo.TypeRepo
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipFile

// TODO kotlinify
class TypeCollector(
        private val infoRepo: JavaInfoRepo
) {

    private fun loadEntry(collector: InfoCollector, input: InputStream) {
        val classReader = ClassReader(input)

        classReader.accept(collector, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
    }

    private fun collectInitial(name: String, jarPath: Path): Pair<MutablePrefixTree<String, DirectCreator>, MutablePrefixTree<String, TemplateCreator>> {
        val typeCollector = InfoCollector(name, infoRepo)

        ZipFile(jarPath.toFile()).use { jar ->
            val entries = jar.entries().toList()

            entries.toList().filter {
                it.name.endsWith(".class")
            }.forEach { entry ->
                loadEntry(typeCollector, jar.getInputStream(entry))
            }
        }

        return typeCollector.directTypes to typeCollector.templateTypes
    }

    fun collectJcl(name: String, jarPath: Path): Pair<JavaRepo, TypeRepo> {
        val (directs, templates) = collectInitial(name, jarPath)

        val connector = TypeConnector(infoRepo, directs, templates)
        return connector.connectJcl(name)
    }

    fun collectArtifact(name: String, jarPath: Path, javaRepo: JavaRepo): TypeRepo {
        val (directs, templates) = collectInitial(name, jarPath)

        val connector = TypeConnector(infoRepo, directs, templates)
        return connector.connectArtifact(javaRepo)
/*
        val connector = TypeConnector(
                infoRepo,
                typeCollector.directTypes,
                typeCollector.templateTypes
        )
        connector.connect()

        println("=== Collected im direct types:")
        typeCollector.directTypes.forEach { direct ->
            println(direct.unfinishedType)
        }

        println("=== Collected im templates:")
        typeCollector.templateTypes.forEach { template ->
            println(template.unfinishedType.fullName)
        }


        println("======================")
        println("Collected Types:")
        println("======================")
        connector.finishedDirects.forEach { direct ->
            util.printType(direct)
        }
        connector.finishedTemplates.forEach { template ->
            util.printSemiType(template)
        }
        TODO()
        */
    }

}
