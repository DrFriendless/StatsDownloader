package com.drfriendless.statsdownloader

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
        }
    }
}