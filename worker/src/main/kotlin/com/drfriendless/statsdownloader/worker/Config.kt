package com.drfriendless.statsdownloader.worker

import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class Config(filename: String) {
    val prop = Properties()
    val usersURL: String by prop
    val metadataURL: String by prop

    init {
        FileInputStream(filename).use {
            prop.load(it)
        }
    }
}