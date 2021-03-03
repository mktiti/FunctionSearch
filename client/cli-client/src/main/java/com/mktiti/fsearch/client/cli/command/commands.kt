package com.mktiti.fsearch.client.cli.command

import com.mktiti.fsearch.client.cli.command.dsl.createCommands
import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.tui.KotlinCompleter
import com.mktiti.fsearch.client.cli.util.parseArtifactId
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.Service
import com.mktiti.fsearch.dto.TypeDto

class CommandStore(service: Service) {

    private val commands = createCommands {
        command("load") {
            help {
                "Loads a given maven artifact given by format \${group}:\${name}:\${version}"
            }

            handle(paramCount = 1) { args ->
                val artifact = parseArtifactId(args.first())
                if (artifact == null) {
                    printer.println("Artifact id must be in format \${group}:\${name}:\${version}")
                } else {
                    when (val callRes = service.artifactApi.load(artifact)) {
                        is ApiCallResult.Success -> {
                            printer.println("Successfully loaded artifact $artifact")
                        }
                        is ApiCallResult.Exception -> {
                            printer.println("Error while loading artifact - [${callRes.code}] ${callRes.message}")
                        }
                    }
                }
            }

            handle {
                printer.println("Requires artifact id")
            }
        }

        command("list-artifacts") {
            help {
                "Lists available artifacts, optionally filtered by name (first parameter)"
            }

            handleRange(0..1) { _ ->
                // TODO
                when (val callRes = service.artifactApi.all()) {
                    is ApiCallResult.Success -> {
                        callRes.result.forEach(printer::println)
                    }
                    is ApiCallResult.Exception -> {
                        printer.println("Error while fetching artifacts - [${callRes.code}] ${callRes.message}")
                    }
                }
            }

            handle {
                printer.println("Requires one or no arguments (optional name filer)")
            }
        }

        command("import") {
            help {
                """
                    |Imports a type so that it can be referenced without fully qualified name in queries.
                    |For imported types, see :imports list 
                """.trimMargin()
            }

            handleTransform(paramCount = 1) { args ->
                val typeName = args.first()
                // TODO - very naive
                val (packageParts, nameParts) = typeName.split(".").partition {
                    it.first().isLowerCase()
                }

                fun List<String>.joined() = joinToString(prefix = "", separator = ".", postfix = "")

                val data = TypeDto(
                        packageName = packageParts.joined(),
                        simpleName = nameParts.joined()
                )
                context.import(data)
            }

            handle {
                printer.println("Requires the qualified name of the type to import.")
            }
        }

        command("imports") {

            help {
                "Commands to manipulate and query imported types"
            }

            subCommand("list") {
                help {
                    "List the imported types which can be referenced safely without full qualifications"
                }

                handle(paramCount = 0) {
                    if (context.imports.importMap.isEmpty()) {
                        printer.println("No type imported")
                    } else {
                        context.imports.importMap.forEach { (type, full) ->
                            printer.println("${full.packageName}.${full.simpleName} as $type")
                        }
                    }
                }
            }

        }

        command("types") {
            help {
                "Lists available types, optionally filtered by name (first parameter)"
            }

            handleRange(0..1) { args ->
                when (val callRes = service.infoApi.types(context.artifactsDto, args.firstOrNull())) {
                    is ApiCallResult.Success -> {
                        callRes.result.forEach {
                            printer.println(it.type.packageName + "." + it.type.simpleName)
                        }
                    }
                    is ApiCallResult.Exception -> {
                        printer.println("Error while fetching artifacts - [${callRes.code}] ${callRes.message}")
                    }
                }
            }
        }

        command("functions") {
            help {
                "Lists available functions, optionally filtered by name (first parameter)"
            }
        }

        command("quit") {
            help {
                "Quits JvmSearch"
            }

            handle {
                quit()
            }
        }
    }

    val completer: KotlinCompleter
        get() = commands.completer

    fun handle(args: List<String>): BackgroundJob {
        return commands.handle(args)
    }

}