package com.drfriendless.statsdownloader.downloader

import SECONDS_BETWEEN_POPULATES
import com.drfriendless.statsdb.DBConfig
import mu.KLogging
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class Main {
    companion object: KLogging()

    fun main(args: Array<String>) {
        if (args.size != 2) {
            println("Usage: downloader config.properties db.properties")
            return
        }
        val config = Config(args[0])
        val dbConfig = DBConfig(args[1])
        val dr = DownloaderRecord()
        dr.record {
            logger.info("Downloader starts at ${Date()}")
            if (!config.dbDir.exists()) {
                logger.info("Creating data directory: ${config.dbDir}")
            }
            // TODO - do we need this?
            if (!config.resultDir.exists()) {
                logger.info("Creating results directory: ${config.resultDir}")
            }
            val db = initDatabaseConnection(dbConfig)
            val finishTime = System.currentTimeMillis() + SECONDS_BETWEEN_POPULATES * 1000L
            val tasks = buildTaskList(config, db)
            while (System.currentTimeMillis() < finishTime) {
                if (tasks.isEmpty()) {
                    tasks.addAll(buildTaskList(config, db))
                    if (tasks.isEmpty()) break
                }
                val task = tasks.removeAt(0)
                task.execute()
            }
            // TODO store record to database.
            logger.info(dr.toString())
        }
    }

    fun initDatabaseConnection(dbConfig: DBConfig): DownloaderDatabase {
        // TODO - connect to the DB
        throw RuntimeException()
    }

    fun buildTaskList(config: Config, db: DownloaderDatabase): MutableList<Task> {
        val result = mutableListOf<Task>()
        result.add(CheckUsersTask(config, db))
        return result
    }
}