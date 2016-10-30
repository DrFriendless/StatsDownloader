package com.drfriendless.statsdownloader.worker

import com.drfriendless.statsdb.database.Downloader
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime
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

    fun insertToDatabase() {
        Downloader.insert {
            it[filesprocessed] = this@DownloaderRecord.filesProcessed
            it[waittime] = this@DownloaderRecord.waitTime
            it[pausetime] = this@DownloaderRecord.pauseTime
            it[nothing] = this@DownloaderRecord.nothing
            it[failures] = this@DownloaderRecord.failures
            it[starttime] = DateTime(this@DownloaderRecord.startTime)
            it[endtime] = DateTime(this@DownloaderRecord.endTime)
            it[users] = this@DownloaderRecord.users
            it[games] = this@DownloaderRecord.games
        }
    }

    override fun toString(): String {
        return "From $startTime to $endTime, $users users $games games $filesProcessed files $failures failures $waitTime wait $pauseTime pause $nothing nothing to do."
    }
}