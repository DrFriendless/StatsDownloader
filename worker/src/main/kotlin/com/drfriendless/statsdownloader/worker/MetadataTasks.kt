package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.database.Metadata
import com.drfriendless.statsdb.database.Series
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.io.File

/**
 * Tasks about the metadata file.
 *
 * @author John Farrell
 */
data class SeriesMetadata(val key: String, val ids: Set<Int>) {}

data class ExpansionsMetadata(val type: MetadataRuleType, val game: Int) {}

fun readMetadata(config: Config): Pair<List<SeriesMetadata>, List<ExpansionsMetadata>> {
    val lines = File(config.installDir, "metadata.txt").readLines()
    val series = mutableListOf<SeriesMetadata>()
    val expansions = mutableListOf<ExpansionsMetadata>()
    lines.map { it.trim() }.
            filterNot { it.startsWith("#") }.
            filterNot { it.length == 0 }.
            forEach {
                if (it.contains(":")) {
                    val fields = it.split(delimiters = ':', limit = 2)
                    val key = fields[0].trim()
                    val ids = fields[1].trim().split(Regex("\\s+")).map { Integer.parseInt(it) }
                    series.add(SeriesMetadata(key, ids.toSet()))
                } else {
                    val fields = it.split(Regex("\\s+"))
                    if (fields.size == 2) {
                        val ruleType = metadataRuleFromString(fields[0])
                        if (ruleType != null) {
                            expansions.add(ExpansionsMetadata(ruleType, Integer.parseInt(fields[1])))
                        } else {
                            logger.error("Invalid entry in metadata file: $it")
                        }
                    }
                }
            }
    return Pair(series, expansions)
}

enum class MetadataRuleType(val intKey: Int, val stringKey: String) {
    EXPANSION(1, "expansion"),
    BASEGAME(2, "basegame")
}

private fun metadataRuleFromString(s: String): MetadataRuleType? {
    return MetadataRuleType.values().firstOrNull { it.stringKey == s }
}

private fun metadataRuleFromInt(i: Int): MetadataRuleType? {
    return MetadataRuleType.values().firstOrNull { it.intKey == i }
}

fun readMetadataFromDatabase(): List<ExpansionsMetadata> {
    return Metadata.selectAll().map { row ->
        val ruleType = metadataRuleFromInt(row[Metadata.ruletype])
        if (ruleType != null) {
            ExpansionsMetadata(ruleType, row[Metadata.bggid])
        } else {
            logger.error("Invalid metadata entry in database: ${row.data}")
            null
        }
    }.filterNotNull()
}

class CheckExpansionsTask(val fromFile: List<ExpansionsMetadata>): Task {
    override fun execute() {
        val fromDB = readMetadataFromDatabase().toMutableList()
        val addFromFile = mutableListOf<ExpansionsMetadata>()
        fromFile.forEach {
            if (fromDB.contains(it)) {
                fromDB.remove(it)
            } else {
                addFromFile.add(it)
            }
        }
        addFromFile.forEach { md ->
            Metadata.insert {
                it[ruletype] = md.type.intKey
                it[bggid] = md.game
            }
        }
        fromDB.forEach { md ->
            Metadata.deleteWhere { (Metadata.ruletype eq md.type.intKey) and (Metadata.bggid eq md.game) }
        }
    }
}

fun readSeriesFromDatabase(): List<SeriesMetadata> {
    return Series.selectAll().
            groupBy({ it[Series.name] }, { it[Series.game] }).
            entries.
            map { SeriesMetadata(it.key, it.value.toSet()) }
}

/** We don't check the top 50 series here, that's more difficult. */
class CheckMostSeriesTask(val fromFile: List<SeriesMetadata>): Task {
    override fun execute() {
        val fromDB = readSeriesFromDatabase().toMutableList()
        fromDB.removeAll { it.key in SERIES_NOT_FROM_METADATA  }
        val addFromFile = mutableListOf<SeriesMetadata>()
        fromFile.forEach {
            if (fromDB.contains(it)) {
                fromDB.remove(it)
            } else {
                addFromFile.add(it)
            }
        }
        fromDB.forEach { md ->
            Series.deleteWhere { Series.name eq md.key }
        }
        addFromFile.forEach { md ->
            md.ids.forEach { bggid ->
                Series.insert {
                    it[name] = md.key
                    it[game] = bggid
                }
            }
        }
    }
}