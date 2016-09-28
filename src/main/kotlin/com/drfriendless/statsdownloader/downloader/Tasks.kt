package com.drfriendless.statsdownloader.downloader

import java.io.File

/**
 * Created by john on 28/09/16.
 */
interface Task {
    fun execute()
}

/** Check that the users match users.txt. */
class CheckUsersTask(val config: Config): Task {
    override fun execute() {
        val users = userNamesFromFile()
        ensureUsersInDBFromFile()
        ensureUsersFromFileInDB()
    }

    fun ensureUsersInDBFromFile() {
        // TODO
    }

    fun ensureUsersFromFileInDB() {
        // TODO
    }

    fun userNamesFromFile(): List<String> {
        return File(config.installDir, "usernames.txt").readLines().map { it.trim() }.filter { it.length > 0 }
    }
}