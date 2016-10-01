package com.drfriendless.statsdownloader.downloader

import com.drfriendless.statsdb.database.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URI
import java.text.MessageFormat
import java.util.*

/**
 * A Task is a set of updates to the database, usually preceded by a download of a particular file.
 *
 * @author John Farrell
 */
interface Task {
    fun execute()
}

/** Check that the users match users.txt. */
class CheckUsersTask(val config: Config, val db: DownloaderDatabase): Task {
    val COLLECTION_FILE = "{0}_collection.xml"
    val PLAYED_FILE = "{0}_played.html"
    val PROFILE_FILE = "{0}_profile.html"
    val COLLECTION_DESCRIPTION = "User collection - owned, ratings, etc"
    val PLAYED_DESCRIPTION = "Months in which user has played games"
    val PROFILE_DESCRIPTION = "User's profile"
    val PROCESS_COLLECTION = "processCollection"
    val PROCESS_PLAYED = "processPlayed"
    val PROCESS_PROFILE = "processProfile"
    val PROCESS_MARKET = "processMarket"
    val PROCESS_GAME = "processGame"
    val PROCESS_TOP50 = "processTop50"
    val PROCESS_FRONT_PAGE = "processFrontPage"
    val THREE_DAYS = "72:00:00"
    val ONE_DAY = "24:00:00"
    val LONGEST_TIME = "838:00:00"
    val TILL_NEXT_UPDATE = mapOf(
            Pair(PROCESS_COLLECTION, THREE_DAYS),
            Pair(PROCESS_MARKET, THREE_DAYS),
            Pair(PROCESS_PLAYED, THREE_DAYS),
            Pair(PROCESS_GAME, LONGEST_TIME),
            Pair(PROCESS_TOP50, THREE_DAYS),
            Pair(PROCESS_FRONT_PAGE, ONE_DAY)
    )

    override fun execute() {
        val usersFromFile = userNamesFromFile()
        val usersFromDB = userNamesFromDatabase(db)
        val usersToBeAdded = HashSet(usersFromFile).apply { removeAll(usersFromDB) }
        val usersToBeDeleted = HashSet(usersFromDB).apply { removeAll(usersFromFile) }
        addUsersNotInDB(usersToBeAdded.toMutableList())
        removeUsersNotInFile(usersToBeDeleted.toList())
    }

    fun addUsersNotInDB(users: MutableList<String>) {
        val geeks = Geeks.slice(Geeks.username).select { Geeks.username inList users }.
                map { row -> row[Geeks.username] }
        users.removeAll(geeks)
        transaction {
            users.forEach { user ->
                Geeks.insert { it[Geeks.username] = user }
                val urlQuotedUser = urlQuote(user)
                val collectionFile = MessageFormat.format(COLLECTION_FILE, user)
                val collectionURL = MessageFormat.format(COLLECTION_URL, urlQuotedUser)
                val playedFile = MessageFormat.format(PLAYED_FILE, user)
                val playedURL = MessageFormat.format(PLAYED_URL, urlQuotedUser)
                val profileFile = MessageFormat.format(PROFILE_FILE, user)
                val profileUrl = MessageFormat.format(PROFILE_URL, urlQuotedUser)
                transactionRecordFile(collectionFile, collectionURL, PROCESS_COLLECTION, user, COLLECTION_DESCRIPTION)
                transactionRecordFile(playedFile, playedURL, PROCESS_PLAYED, user, PLAYED_DESCRIPTION)
                transactionRecordFile(profileFile, profileUrl, PROCESS_PROFILE, user, PROFILE_DESCRIPTION)
            }
        }
    }

    /**
     * transaction prefix on the method name means we are already in a transaction.
     */
    fun transactionRecordFile(filename: String, url: String, processMethod: String, user: String, description: String) {
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

    fun urlQuote(s: String): String {
        val uri = URI(null, null, s)
        val result = uri.toString()
        println("URL quoted $s to $result")
        return result
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
        return File(config.installDir, "usernames.txt").readLines().map { it.trim() }.filter { it.length > 0 }
    }

    fun userNamesFromDatabase(db: DownloaderDatabase): List<String> {
        return db.getUsers()
    }
}