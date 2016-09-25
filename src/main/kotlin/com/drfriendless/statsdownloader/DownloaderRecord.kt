package com.drfriendless.statsdownloader

import java.text.MessageFormat
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class DownloaderRecord {
    var filesProcessed = 0
    var waitTime = 0.0
    var pauseTime = 0.0
    var failures = 0
    var startTime: Date? = null
    var endTime: Date? = null
    var users = 0
    var games = 0

    fun record(op: () -> Unit) {
        startTime = Date()
        op()
        endTime = Date()
    }

    fun usersAndGames(u: Int, g: Int) {
        users = u
        games = g
    }

    fun failure() = failures++

    fun wait(howlong: Int) { waitTime += howlong }

    fun pause(howlong: Int) { pauseTime += howlong }

    fun processFiles(howmany: Int) { filesProcessed += howmany }

    private val TIME_FORMAT = "{0,date,%Y-%m-%d %H:%M:%S}"

    private fun f(d: Date?) = if (d == null) throw RuntimeException() else MessageFormat.format(TIME_FORMAT, d)

    fun toSQL() = "insert into downloader (starttime, endtime, filesprocessed, waittime, pausetime, failures, users, games) values ('${f(startTime)}', '${f(endTime)}', $filesProcessed, $waitTime, $pauseTime, $failures, $users, $games)"
}