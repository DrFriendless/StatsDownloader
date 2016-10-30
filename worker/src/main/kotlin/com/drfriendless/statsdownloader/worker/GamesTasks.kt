package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.database.Files
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.MessageFormat

/**
 * Tasks to do with the list of games.
 *
 * @author John Farrell
 */
const val GAME_FILE = "{0}.xml"
const val GAME_DESCRIPTION = "Game {0}"

private fun transaction_addEntriesForGame(id: Int) {
    Files.insert {
        it[Files.filename] = MessageFormat.format(GAME_FILE, id)
        it[Files.url] = MessageFormat.format(GAME_URL, id)
        it[Files.processMethod] = PROCESS_GAME
        it[Files.tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_GAME]
        it[Files.description] = MessageFormat.format(GAME_DESCRIPTION, id)
    }
}

data class FileAndURL(val filename: String?, val url: String?) {
    val gameID = filename?.let {
        Integer.parseInt(it.substring(0, it.indexOf('.')))
    }
}

class CheckGamesFileEntries(val db: DownloaderDatabase, val rec: DownloaderRecord): Task {
    override fun execute() {
        val gameIDs = db.getGameIDs()
        rec.games(gameIDs.size)
        fun gameURLFor(bggID: Int) = MessageFormat.format(GAME_URL, bggID)
        val allIDsAndURLs: List<FileAndURL> = Files.slice(Files.filename, Files.url).
                select { Files.processMethod eq PROCESS_GAME }.
                map { row -> FileAndURL(row[Files.filename], row[Files.url]) }
        val badGameURLs = allIDsAndURLs.
                filter { it.gameID in gameIDs }.
                filter { it.gameID != null && it.url != gameURLFor(it.gameID) }
        val unwantedGamesURLs = allIDsAndURLs.filter { it.gameID !in gameIDs }.map { it.url }
        if (badGameURLs.isNotEmpty()) {
            transaction {
                Files.deleteWhere { Files.url inList badGameURLs.map { it.url } }
                badGameURLs.map { it.gameID }.filterNotNull().forEach(::transaction_addEntriesForGame)
            }
        }
        if (unwantedGamesURLs.isNotEmpty()) {
            transaction {
                Files.deleteWhere { Files.url inList unwantedGamesURLs }
            }
        }
    }
}