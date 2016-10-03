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

fun <A, B, C> doToFirst(f: (A) -> C): (Pair<A,B>) -> Pair<C,B> {
    return fun(pair: Pair<A,B>): Pair<C,B> {
        return Pair(f(pair.first), pair.second)
    }
}

private fun transaction_addEntriesForGame(id: Int) {
    Files.insert {
        it[Files.filename] = MessageFormat.format(GAME_FILE, id)
        it[Files.url] = MessageFormat.format(GAME_URL, id)
        it[Files.processMethod] = PROCESS_GAME
        it[Files.tillNextUpdate] = TILL_NEXT_UPDATE[PROCESS_GAME]
        it[Files.description] = MessageFormat.format(GAME_DESCRIPTION, id)
    }
}

class CheckGamesFileEntries(val db: DownloaderDatabase, val rec: DownloaderRecord): Task {
    override fun execute() {
        val gameIDs = db.getGameIDs()
        rec.games(gameIDs.size)
        fun extractGameId(filename: String) = Integer.parseInt(filename.substring(0, filename.indexOf('.')))
        fun gameURLFor(bggID: Int) = MessageFormat.format(GAME_URL, bggID)
        val allIDsAndURLs: List<Pair<Int, String>> = Files.slice(Files.filename, Files.url).
                select { Files.processMethod eq PROCESS_GAME }.
                map { row -> Pair(row[Files.filename], row[Files.url]) }.
                map(doToFirst(::extractGameId))
        val badGameURLs = allIDsAndURLs.
                filter { it.first in gameIDs }.
                filter { it.second != gameURLFor(it.first) }
        val unwantedGamesURLs = allIDsAndURLs.filter { it.first !in gameIDs }.map { it.second }
        if (badGameURLs.isNotEmpty()) {
            transaction {
                Files.deleteWhere { Files.url inList badGameURLs.map { it.second } }
                badGameURLs.map { it.first }.forEach(::transaction_addEntriesForGame)
            }
        }
        if (unwantedGamesURLs.isNotEmpty()) {
            transaction {
                Files.deleteWhere { Files.url inList unwantedGamesURLs }
            }
        }
    }
}