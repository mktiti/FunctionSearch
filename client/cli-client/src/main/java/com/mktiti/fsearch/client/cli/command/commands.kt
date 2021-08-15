package com.mktiti.fsearch.client.cli.command

import com.mktiti.fsearch.client.cli.ProjectInfo
import com.mktiti.fsearch.client.cli.command.dsl.createCommands
import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.job.BackgroundJobContext
import com.mktiti.fsearch.client.cli.tui.KotlinCompleter
import com.mktiti.fsearch.client.cli.util.onResults
import com.mktiti.fsearch.client.cli.util.parseArtifactId
import com.mktiti.fsearch.client.cli.util.runHealthCheck
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.ClientFactory
import com.mktiti.fsearch.client.rest.ClientFactory.Config.GrpcConfig
import com.mktiti.fsearch.client.rest.ClientFactory.Config.RestConfig
import com.mktiti.fsearch.client.rest.Service
import com.mktiti.fsearch.client.rest.nop.NopService
import com.mktiti.fsearch.dto.FunRelationDto
import com.mktiti.fsearch.dto.TypeDto

object CommandStore {

    private val commands = createCommands {

        unknownCommand { args ->
            "Unknown command (${args.firstOrNull()}), see :help for available commands!"
        }

        command("load") {
            help {
                "Loads a given maven artifact given by format \${group}:\${name}:\${version}"
            }

            handle(paramCount = 1) { args ->
                val artifact = parseArtifactId(args.first())
                if (artifact == null) {
                    printer.println("Artifact id must be in format \${group}:\${name}:\${version}")
                } else {
                    when (val callRes = artifactApi.load(artifact)) {
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

            handleRange(0..1) {
                // TODO
                when (val callRes = artifactApi.all()) {
                    is ApiCallResult.Success -> {
                        onResults(callRes.result, printer::println)
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
                    Imports a type so that it can be referenced without fully qualified name in queries.
                    For imported types, see :imports list 
                """.trimMargin()
            }

            handleTransform(paramCount = 1) { args ->
                val typeName = args.first()
                // TODO - very naive
                val (packageParts, nameParts) = typeName.split(".").partition {
                    it.first().isLowerCase()
                }

                fun List<String>.joined() = joinToString(prefix = "", separator = ".", postfix = "")

                val simpleName = when (val parsed = nameParts.joined()) {
                    "" -> "*"
                    else -> parsed
                }

                val data = TypeDto(
                        packageName = packageParts.joined(),
                        simpleName = simpleName
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
                    if (context.imports.isEmpty()) {
                        printer.println("No type imported")
                    } else {
                        context.imports.forEach { type ->
                            printer.print("${type.packageName}.${type.simpleName}")
                            if (type.simpleName == "*") {
                                printer.println()
                            } else {
                                printer.print(" as ${type.simpleName}")
                            }
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
                when (val callRes = infoApi.types(context.asDto(), args.firstOrNull())) {
                    is ApiCallResult.Success -> {
                        onResults(callRes) {
                            printer.println(it.type)
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

            handleRange(0..1) { args ->
                when (val callRes = infoApi.functions(context.asDto(), args.firstOrNull())) {
                    is ApiCallResult.Success -> {
                        onResults(callRes) {
                            printer.print(it.type)
                            val typedName = when (it.relation) {
                                FunRelationDto.STATIC -> "::${it.name}"
                                FunRelationDto.CONSTRUCTOR -> "::<init>"
                                FunRelationDto.INSTANCE -> ".${it.name}"
                            }
                            printer.println(typedName)
                        }
                    }
                    is ApiCallResult.Exception -> {
                        printer.println("Error while fetching artifacts - [${callRes.code}] ${callRes.message}")
                    }
                }
            }
        }

        command("version") {
            help {
                "Prints version info about JvmSearch"
            }

            handle(paramCount = 0) {
                printer.println(ProjectInfo.versionedName)
            }
        }

        command("service") {
            help {
                """
                    Backing service information and configuration
                    By default prints the currently used service.
                """.trimIndent()
            }

            handle(paramCount = 0) {
                printer.println(context.service.address ?: "Not set!")
            }

            subCommand("clear") {
                help { "Unsets the backing service path" }

                handleTransform(paramCount = 0) {
                    context.copy(service = NopService)
                }
            }

            fun BackgroundJobContext.setService(newService: Service): Context {
                val check = runHealthCheck(newService, printer)
                return if (check) {
                    printer.println("Health check passed, service updated")
                    context.copy(service = newService)
                } else {
                    printer.println("Health check failed, service not updated")
                    context
                }
            }

            subCommand("set-rest") {
                help { "Sets the backing RESTful service path" }

                handleTransform(paramCount = 1) { args ->
                    val newService = ClientFactory.create(RestConfig(args.first()))
                    setService(newService)
                }
            }

            subCommand("set-grpc") {
                help { "Sets the backing gRPC service path (address port)" }

                handleTransform(paramCount = 2) { args ->
                    val port = args[1].toIntOrNull()
                    if (port == null) {
                        printer.println("gRPC port must be an integer!")
                        context
                    } else {
                        val newService = ClientFactory.create(GrpcConfig(args.first(), port))
                        setService(newService)
                    }
                }
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
