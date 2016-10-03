package com.drfriendless.statsdownloader.worker

import java.text.MessageFormat
import java.util.*

/**
 * Created by john on 25/09/16.
 */
class DownloaderRecord {
    var filesProcessed = 0
    var waitTime = 0.0F
    var pauseTime = 0.0F
    var nothing = 0.0F
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

    fun users(u: Int) {
        users = u
    }

    fun games(g: Int) {
        games = g
    }

    fun failure() = failures++

    fun wait(howlong: Int) {
        waitTime += howlong / 1000.0F
    }

    fun pause(howlong: Int) {
        pauseTime += howlong / 1000.0F
    }

    fun nothing(howlong: Int) {
        nothing += howlong / 1000.0F
    }

    fun processFiles(howmany: Int) {
        filesProcessed += howmany
    }

    private val TIME_FORMAT = "{0,date,%Y-%m-%d %H:%M:%S}"

    private fun f(d: Date?) = if (d == null) throw RuntimeException() else MessageFormat.format(TIME_FORMAT, d)

    // TODO - should do this with Exposed.
    fun toSQL() = "insert into downloader (starttime, endtime, filesprocessed, waittime, pausetime, nothing, failures, users, games) values ('${f(startTime)}', '${f(endTime)}', $filesProcessed, $waitTime, $pauseTime, $nothing, $failures, $users, $games)"

    override fun toString(): String {
        return "From $startTime to $endTime, $users users $games games $filesProcessed files $failures failures $waitTime wait $pauseTime pause $nothing nothing to do."
    }
}