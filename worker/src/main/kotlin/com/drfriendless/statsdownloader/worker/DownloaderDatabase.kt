package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.DBConfig
import com.drfriendless.statsdb.database.*
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


    fun readSeriesFromDatabase(): List<SeriesMetadata> {
        return Series.selectAll().
                groupBy({ it[Series.name] }, { it[Series.game] }).
                entries.
                map { SeriesMetadata(it.key, it.value.toSet()) }
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
}

