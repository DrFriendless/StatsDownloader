package com.drfriendless.statsdownloader.downloader

import SECONDS_BETWEEN_POPULATES
import mu.KLogging
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class Main {
    companion object: KLogging()

    fun main(args: Array<String>) {
        val config = Config("downloader.properties")
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
            initDatabaseConnection()
            val finishTime = System.currentTimeMillis() + SECONDS_BETWEEN_POPULATES * 1000L
            val tasks = buildTaskList(config)
            while (System.currentTimeMillis() < finishTime) {
                if (tasks.isEmpty()) break
                val task = tasks.removeAt(0)
                task.execute()
            }
            // TODO store record to database.
            logger.info(dr.toString())
        }
    }

    fun initDatabaseConnection() {
        // TODO - connect to the DB
    }

    fun buildTaskList(config: Config): MutableList<Task> {
        val result = mutableListOf<Task>()
        result.add(CheckUsersTask(config))
        return result
    }
}