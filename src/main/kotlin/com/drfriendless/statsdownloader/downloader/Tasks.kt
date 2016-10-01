package com.drfriendless.statsdownloader.downloader

import com.drfriendless.statsdb.database.*
import org.jetbrains.exposed.sql.deleteWhere
import java.io.File
import java.util.*

/**
 * Created by john on 28/09/16.
 */
interface Task {
    fun execute()
}

/** Check that the users match users.txt. */
class CheckUsersTask(val config: Config, val db: DownloaderDatabase): Task {
    override fun execute() {
        val usersFromFile = userNamesFromFile()
        val usersFromDB = userNamesFromDatabase(db)
        val usersToBeAdded = HashSet(usersFromFile).apply { removeAll(usersFromDB) }
        val usersToBeDeleted = HashSet(usersFromDB).apply { removeAll(usersFromFile) }
        ensureUsersInDBFromFile(usersToBeDeleted)
        ensureUsersFromFileInDB(usersToBeAdded.toList())
    }

    fun ensureUsersInDBFromFile(users: Collection<String>) {
        // TODO
    }

    fun ensureUsersFromFileInDB(users: List<String>) {
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