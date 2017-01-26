package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.database.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.text.MessageFormat
import java.util.*

const val COLLECTION_FILE = "{0}_collection.xml"
const val PLAYED_FILE = "{0}_played.html"
const val PROFILE_FILE = "{0}_profile.html"
const val COLLECTION_DESCRIPTION = "User collection - owned, ratings, etc"
const val PLAYED_DESCRIPTION = "Months in which user has played games"
const val PROFILE_DESCRIPTION = "User's profile"
const val PROCESS_PROFILE = "processProfile"
const val PROCESS_COLLECTION = "processCollection"
const val PROCESS_PLAYED = "processPlayed"
const val PROCESS_MARKET = "processMarket"
const val PROCESS_GAME = "processGame"
const val PROCESS_TOP50 = "processTop50"
const val PROCESS_ES_TOP100 = "processESTop100"
const val PROCESS_MOST_VOTERS = "processMostVoters"
const val PROCESS_FRONT_PAGE = "processFrontPage"
const val THREE_DAYS = "72:00:00"
const val ONE_DAY = "24:00:00"
const val LONGEST_TIME = "838:00:00"

/**
 * transaction_ prefix on the method name means we are already in a transaction.
 */
fun transaction_RecordFile(filename: String, url: String, processMethod: String, user: String, description: String) {
    if (Files.select { Files.url eq url }.count() == 0) {
        val till = TILL_NEXT_UPDATE[processMethod] ?: throw RuntimeException("Unknown process method: $processMethod")
        Files.insert {
            it[Files.filename] = filename
            it[Files.url] = url
            it[Files.processMethod] = processMethod
            it[Files.geek] = user
            it[Files.tillNextUpdate] = till
            it[Files.description] = description
        }
    }
}

private fun transaction_addEntriesForUser(user: String) {
    Geeks.insert { it[username] = user }
    val urlQuotedUser = urlQuote(user)
    val collectionFile = MessageFormat.format(COLLECTION_FILE, user)
    val collectionURL = MessageFormat.format(COLLECTION_URL, urlQuotedUser)
    val playedFile = MessageFormat.format(PLAYED_FILE, user)
    val playedURL = MessageFormat.format(PLAYED_URL, urlQuotedUser)
    val profileFile = MessageFormat.format(PROFILE_FILE, user)
    val profileUrl = MessageFormat.format(PROFILE_URL, urlQuotedUser)
    transaction_RecordFile(collectionFile, collectionURL, PROCESS_COLLECTION, user, COLLECTION_DESCRIPTION)
    transaction_RecordFile(playedFile, playedURL, PROCESS_PLAYED, user, PLAYED_DESCRIPTION)
    transaction_RecordFile(profileFile, profileUrl, PROCESS_PROFILE, user, PROFILE_DESCRIPTION)
}

fun userNamesFromDatabase(db: DownloaderDatabase): List<String> {
    return db.getUsers()
}

/** Check that the users match users.txt. */
class CheckUsersTask(val config: Config, val db: DownloaderDatabase, val rec: DownloaderRecord): Task {
    override fun execute() {
        val usersFromFile = userNamesFromFile()
        val usersFromDB = userNamesFromDatabase(db)
        val usersToBeAdded = HashSet(usersFromFile).apply { removeAll(usersFromDB) }
        val usersToBeDeleted = HashSet(usersFromDB).apply { removeAll(usersFromFile) }
        addUsersNotInDB(usersToBeAdded.toMutableList())
        removeUsersNotInFile(usersToBeDeleted.toList())
        rec.users(usersFromFile.size)
    }

    fun addUsersNotInDB(users: MutableList<String>) {
        val geeks = Geeks.slice(Geeks.username).select { Geeks.username inList users }.
                map { row -> row[Geeks.username] }
        users.removeAll(geeks)
        transaction {
            users.forEach(::transaction_addEntriesForUser)
        }
    }

    fun removeUsersNotInFile(users: List<String>) {
        GeekGameTags.deleteWhere { GeekGameTags.geek inList users }
        History.deleteWhere { History.geek inList users }
        MonthsPlayed.deleteWhere { MonthsPlayed.geek inList users }
        GeekGames.deleteWhere { GeekGames.geek inList users }
        Files.deleteWhere { Files.geek inList users }
        Geeks.deleteWhere { Geeks.username inList users }
        Users.deleteWhere { Users.geek inList users }
    }

    fun userNamesFromFile(): List<String> {
        return URL(config.usersURL).readText().split('\n').map(String::trim).filter { it.length > 0 }
    }
}

/**
 * Check that we have the entries for a user that we're expecting.
 */
class CheckUsersFileEntries(val db: DownloaderDatabase): Task {
    override fun execute() {
        val usersFromDB = userNamesFromDatabase(db)
        usersFromDB.forEach { user ->
            val broken = (Geeks.select { Geeks.username eq user }.count() == 0) ||
                    (Files.select { (Files.geek eq user) and (Files.processMethod eq PROCESS_COLLECTION) }.count() == 0) ||
                    (Files.select { (Files.geek eq user) and (Files.processMethod eq PROCESS_PLAYED) }.count() == 0) ||
                    (Files.select { (Files.geek eq user) and (Files.processMethod eq PROCESS_PROFILE) }.count() == 0)
            if (broken) {
                // start over again.
                logger.warn("User $user does not have required records, refreshing.")
                transaction {
                    Geeks.deleteWhere { Geeks.username eq user }
                    Files.deleteWhere { Geeks.username eq user }
                    transaction_addEntriesForUser(user)
                }
            }
        }
    }
}