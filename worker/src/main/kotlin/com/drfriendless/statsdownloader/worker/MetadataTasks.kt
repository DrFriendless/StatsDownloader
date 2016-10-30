package com.drfriendless.statsdownloader.worker

import java.io.File

/**
 * Created by john on 3/10/16.
 */
sealed class Metadata {
    class SeriesMetadata(val key: String, val ids: List<Int>): Metadata() {}

    class ExpansionsMetadata(val line: String): Metadata() {}
}

fun readMetadata(config: Config): List<Metadata> {
    val lines = File(config.installDir, "metadata.txt").readLines()
    return lines.map { it.trim() }.
            filterNot { it.startsWith("#") }.
            filterNot { it.length == 0 }.
            map {
                if (it.contains(":")) {
                    val fields = it.split(delimiters = ':', limit = 2)
                    val key = fields[0].trim()
                    val ids = fields[1].trim().split(Regex("\\s+")).map { Integer.parseInt(it) }
                    Metadata.SeriesMetadata(key, ids)
                } else {
                    Metadata.ExpansionsMetadata(it)
                }
            }
}