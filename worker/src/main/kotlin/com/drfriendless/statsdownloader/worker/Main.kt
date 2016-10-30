package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.DBConfig
import java.util.*

/**
 * Main program.
 *
 * Created by john on 25/09/16.
 */
fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: downloader config.properties db.properties")
        return
    }
    val config = Config(args[0])
    val dbConfig = DBConfig(args[1])
    val dr = DownloaderRecord()
    logger.info("Downloader starts at ${Date()}")
    if (!config.dbDir.exists()) {
        logger.info("Creating data directory: ${config.dbDir}")
    }
    // TODO - do we need this?
    if (!config.resultDir.exists()) {
        logger.info("Creating results directory: ${config.resultDir}")
    }
    val db = initDatabaseConnection(dbConfig)
    dr.record {
        val finishTime = System.currentTimeMillis() + SECONDS_BETWEEN_POPULATES * 1000L
        val tasks = buildTaskList(config, db, dr, true)
        while (System.currentTimeMillis() < finishTime) {
            if (tasks.isEmpty()) {
                tasks.addAll(buildTaskList(config, db, dr, false))
                if (tasks.isEmpty()) break
            }
            val task = tasks.removeAt(0)
            task.execute()
        }
    }
    logger.info(dr.toString())
    dr.insertToDatabase()
}

fun initDatabaseConnection(dbConfig: DBConfig): DownloaderDatabase {
    return DownloaderDatabase(dbConfig)
}

/**
 * Find all the tasks that need to be done.
 *
 * @param includeStandard whether to do the normal housekeeping tasks we do every run.
 */
fun buildTaskList(config: Config, db: DownloaderDatabase, rec: DownloaderRecord, includeStandard: Boolean): MutableList<Task> {
    val result = mutableListOf<Task>()
    if (includeStandard) {
        result.add(CheckUsersTask(config, db, rec))
        result.add(CheckUsersFileEntries(db))
        result.add(CheckGamesFileEntries(db, rec))
        // TODO add metadata tasks
        val metadata = readMetadata(config)
        result.add(CheckExpansionsTask(metadata.second))
        result.add(CheckMostSeriesTask(metadata.first))
    }
    // TODO - task to update top 50 games series.
    return result
}