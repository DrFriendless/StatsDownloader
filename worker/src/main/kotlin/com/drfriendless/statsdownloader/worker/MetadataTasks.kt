package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.database.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.net.URL

/**
 * Tasks about the metadata file.
 *
 * @author John Farrell
 */
fun metadataFromFile(config: Config): List<String> {
    return URL(config.metadataURL).readText().split('\n').map(String::trim).filter(String::isNotEmpty)
}


fun readMetadata(config: Config): Pair<List<SeriesMetadata>, List<ExpansionsMetadata>> {
    val lines = metadataFromFile(config)
    val series = mutableListOf<SeriesMetadata>()
    val expansions = mutableListOf<ExpansionsMetadata>()
    lines.filterNot { it.startsWith("#") }.
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

class CheckExpansionsTask(val fromFile: List<ExpansionsMetadata>, val db: DownloaderDatabase): Task {
    override fun execute() {
        val fromDB = db.readMetadataFromDatabase().toMutableList()
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

/** We don't check the top 50 series here, that's more difficult. */
class CheckMostSeriesTask(val fromFile: List<SeriesMetadata>, val db: DownloaderDatabase): Task {
    override fun execute() {
        val fromDB = db.readSeriesFromDatabase().toMutableList()
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

class CheckMetadataTasks: Task {
    override fun execute() {
        if (Files.select { Files.processMethod eq PROCESS_TOP50 }.count() == 0) {
            Files.insert {
                it[filename] = "top50.html"
                it[url] = TOP50_URL
                it[processMethod] = PROCESS_TOP50
                it[description] = SERIES_TOP_50
                it[tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_TOP50]
            }
        }
        if (Files.select { Files.processMethod eq PROCESS_ES_TOP100 }.count() == 0) {
            Files.insert {
                it[filename] = "top100.html"
                it[processMethod] = PROCESS_ES_TOP100
                it[description] = SERIES_ES_TOP_100
                it[tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_ES_TOP100]
            }
        }
        if (Files.select { Files.processMethod eq PROCESS_MOST_VOTERS }.count() == 0) {
            Files.insert {
                it[filename] = "mostVoters.html"
                it[url] = MOST_VOTERS_URL
                it[processMethod] = PROCESS_MOST_VOTERS
                it[description] = SERIES_MOST_VOTERS
                it[tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_MOST_VOTERS]
            }
        }
        if (Files.select { Files.processMethod eq PROCESS_FRONT_PAGE }.count() == 0) {
            Files.insert {
                it[filename] = "mostVoters.html"
                it[processMethod] = PROCESS_FRONT_PAGE
                it[description] = "Front Page"
                it[tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_FRONT_PAGE]
            }
        }
    }
}