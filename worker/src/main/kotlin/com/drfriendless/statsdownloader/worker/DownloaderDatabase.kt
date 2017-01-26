package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.DBConfig
import com.drfriendless.statsdb.database.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Created by john on 1/10/16.
 */
class DownloaderDatabase(config: DBConfig): Database(config) {

    fun getUsers(): List<String> {
        return transaction {
            Users.slice(Users.geek).selectAll().map { row -> row[Users.geek] }
        }
    }

    fun getGameIDs(): List<Int> {
        return transaction {
            Games.slice(Games.bggid).selectAll().map { row -> row[Games.bggid] }
        }
    }


    fun readSeriesFromDatabase(): List<SeriesMetadata> {
        return transaction {
            Series.selectAll().
                    groupBy({ it[Series.name] }, { it[Series.game] }).
                    entries.
                    map { SeriesMetadata(it.key, it.value.toSet()) }
        }
    }

    fun readMetadataFromDatabase(): List<ExpansionsMetadata> {
        return transaction {
            Metadata.selectAll().map { row ->
                val ruleType = metadataRuleFromInt(row[Metadata.ruletype])
                if (ruleType != null) {
                    ExpansionsMetadata(ruleType, row[Metadata.bggid])
                } else {
                    com.drfriendless.statsdownloader.worker.logger.error("Invalid metadata entry in database: ${row.data}")
                    null
                }
            }.filterNotNull()
        }
    }
}

fun ensureDatabaseExists(config: DBConfig) {
    org.jetbrains.exposed.sql.Database.connect(config.dbInformationURL, config.driver, config.dbUser, config.dbPasswd)
    transaction {
            exec("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'extended'") { results ->
                if (!results.next()) {
                    exec("CREATE DATABASE extended;")
                    // TODO - create the schema
                    println("database created")
                }
            }
    }
}
