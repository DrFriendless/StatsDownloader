package com.drfriendless.statsdownloader.downloader

import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class Config(filename: String) {
    val prop = Properties()
    val installDir: String by prop
    val downloaderDir = File(installDir, "downloader")
    val dbDir = File(installDir, "db")
    val resultDir = File(installDir, "static")
    val logFile = File(installDir, "downloader.log")

    init {
        FileInputStream(filename).use {
            prop.load(it)
        }
    }
}