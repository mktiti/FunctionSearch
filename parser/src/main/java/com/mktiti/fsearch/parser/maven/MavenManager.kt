package com.mktiti.fsearch.parser.maven

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

object MavenManager {

    val central = MavenRepoInfo(
            name = "Maven central",
            baseUrl = "https://repo1.maven.org/maven2/"
    )

    fun <R> onArtifact(repo: MavenRepoInfo, info: MavenArtifact, onFile: (File) -> R): R {
        val tempFile = File.createTempFile("fsearch-maven-artifact-$info-", ".jar").apply {
            deleteOnExit()
        }

        val fileName = "${info.name}-${info.version}.jar"
        val url = repo.baseUrl + (info.group + info.name + info.version + fileName).joinToString(separator = "/")
        println(">>> Downloading artifact $info from ${repo.name} ($url) to $tempFile")
        Channels.newChannel(URL(url).openStream()).use { downloadChannel ->
            FileOutputStream(tempFile).use { fileOut ->
                fileOut.channel.transferFrom(downloadChannel, 0, Long.MAX_VALUE)
            }
        }

        return onFile(tempFile).also {
            tempFile.delete()
        }
    }

}
