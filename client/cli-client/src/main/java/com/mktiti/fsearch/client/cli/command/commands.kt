package com.mktiti.fsearch.client.cli.command

import com.mktiti.fsearch.client.cli.BackgroundJob
import com.mktiti.fsearch.client.cli.KotlinCompleter
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.Service
import com.mktiti.fsearch.dto.ArtifactIdDto

class CommandStore(service: Service) {

    private val commands = createCommands {
        command("load") {
            help = {
                "Loads a given maven artifact given by format \${group}:\${name}:\${version}"
            }

            handler = { args ->
                if (args.size != 1) {
                    printer.println("Requires artifact id")
                } else {
                    val parts = args.first().split(":")
                    if (parts.size != 3) {
                        printer.println("Artifact id must be in format \$group:\$name:\$version")
                    } else {
                        val id = ArtifactIdDto(parts[0], parts[1], parts[2])
                        when (val callRes = service.artifactApi.load(id)) {
                            is ApiCallResult.Success -> {
                                printer.println("Successfully loaded artifact $id")
                            }
                            is ApiCallResult.Exception -> {
                                printer.println("Error while loading artifact - [${callRes.code}] ${callRes.message}")
                            }
                        }
                    }
                }
            }
        }

        command("list-artifacts") {
            help = {
                "Lists available artifacts, optionally filtered by name (first parameter)"
            }

            handler = { args ->
                if (args.size !in (0..1)) {
                    printer.println("Requires one or no arguments (optional name filer)")
                } else {
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
            }
        }

        command("types") {
            help = {
                "Lists available types, optionally filtered by name (first parameter)"
            }

            handler = { args ->
                when (val callRes = service.infoApi.types(contextManager.dto, args.firstOrNull())) {
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
            help = {
                "Lists available functions, optionally filtered by name (first parameter)"
            }
        }

        command("quit") {
            help = {
                "Quits JvmSearch"
            }
        }
    }

    val completer: KotlinCompleter
        get() = commands.completer

    fun handle(args: List<String>): BackgroundJob {
        return commands.handle(args)
    }

}
