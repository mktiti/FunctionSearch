package com.mktiti.fsearch.util

import java.nio.file.Path

tailrec fun Path.resolveNested(subDirs: List<String>): Path = when (val cut = subDirs.safeCutHead()) {
    null -> this
    else -> resolve(cut.first).resolveNested(cut.second)
}