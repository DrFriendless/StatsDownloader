package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.DBConfig
import com.drfriendless.statsdb.database.Database
import com.drfriendless.statsdb.database.Games
import com.drfriendless.statsdb.database.Users
import org.jetbrains.exposed.sql.selectAll

/**
 * Created by john on 1/10/16.
 */
class DownloaderDatabase(config: DBConfig): Database(config) {
    fun getUsers(): List<String> {
        return Users.slice(Users.geek).selectAll().map { row -> row[Users.geek] }
    }

    fun getGameIDs(): List<Int> {
        return Games.slice(Games.bggid).selectAll().map { row -> row[Games.bggid] }
    }
}

