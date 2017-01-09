package com.drfriendless.statsdownloader.httpd

import com.drfriendless.statsdb.database.Downloader
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonElement
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

/**
 * Data access methods for the downloader table.
 *
 * @author John Farrell
 */
fun getDownloaderDataLast24HoursJson(): JsonElement {
    val raw = getDownloaderDataLast24Hours(::processTimeUsage)
    val result = toJson(raw)
    return result
}

fun getDownloaderCountsLast24HoursJson(): JsonElement {
    val raw = getDownloaderDataLast24Hours(::processCounts)
    val result = toJson(raw)
    return result
}

fun getDownloaderFileCountsLast24HoursJson(): JsonElement {
    val raw = getDownloaderDataLast24Hours(::processFileCounts)
    val result = toJson(raw)
    return result
}

fun getDownloaderDataLast24Hours(process: (Iterable<ResultRow>) -> Map<String, Any>): Map<String, Any> {
    val start = Date(System.currentTimeMillis() - 1000L * 3600 * 24)
    return transaction {
        val dbData = Downloader.slice(Downloader.columns).select { Downloader.starttime greaterEq DateTime(start) }
        process(dbData)
    }
}

private fun processCounts(data: Iterable<ResultRow>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val failures = mutableListOf<List<Long>>()
    val filesProcessed = mutableListOf<List<Long>>()
    val games = mutableListOf<List<Long>>()
    val users = mutableListOf<List<Long>>()
    data.forEach { row ->
        val f = row[Downloader.failures].toLong()
        val fp = row[Downloader.filesprocessed].toLong()
        val g = row[Downloader.games].toLong()
        val u = row[Downloader.users].toLong()
        val ts = row[Downloader.starttime].toDate().time
        failures.add(listOf(ts, f))
        filesProcessed.add(listOf(ts, fp))
        games.add(listOf(ts, g))
        users.add(listOf(ts, u))
    }
    result["series"] = listOf(
            mapOf(Pair("name", "Failures"), Pair("data", failures)),
            mapOf(Pair("name", "Files Processed"), Pair("data", filesProcessed)),
            mapOf(Pair("name", "Games"), Pair("data", games)),
            mapOf(Pair("name", "Users"), Pair("data", users))
    )
    return result
}

private fun processFileCounts(data: Iterable<ResultRow>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val files1 = mutableListOf<List<Long>>()
    val files2 = mutableListOf<List<Long>>()
    val files3 = mutableListOf<List<Long>>()
    data.forEach { row ->
        val f1 = row[Downloader.files1].toLong()
        val f2 = row[Downloader.files2].toLong()
        val f3 = row[Downloader.files3].toLong()
        val ts = row[Downloader.starttime].toDate().time
        files1.add(listOf(ts, f1))
        files2.add(listOf(ts, f2))
        files3.add(listOf(ts, f3))
    }
    result["series"] = listOf(
            mapOf(Pair("name", "Priority 1"), Pair("data", files1)),
            mapOf(Pair("name", "Priority 2"), Pair("data", files2)),
            mapOf(Pair("name", "Priority 3"), Pair("data", files3))
    )
    return result
}

private fun processTimeUsage(data: Iterable<ResultRow>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val remainders = mutableListOf<List<Long>>()
    val pauses = mutableListOf<List<Long>>()
    val waits = mutableListOf<List<Long>>()
    val nothings = mutableListOf<List<Long>>()
    val timestamps = mutableListOf<Date>()
    data.forEach { row ->
        val t = (row[Downloader.endtime].toDate().time - row[Downloader.starttime].toDate().time).toInt()
        val n = (row[Downloader.nothing] * 1000).toLong()
        val p = (row[Downloader.pausetime] * 1000).toLong()
        val w = (row[Downloader.waittime] * 1000).toLong()
        val ts = row[Downloader.starttime].toDate().time
        pauses.add(listOf(ts, p))
        waits.add(listOf(ts, w))
        // chart stacks the 4 together, so we send t as just the remainder.
        remainders.add(listOf(ts, t-p-w))
        timestamps.add(row[Downloader.starttime].toDate())
        nothings.add(listOf(ts, n))
    }
    result["series"] = listOf(
            mapOf(Pair("name", "nothing"), Pair("data", nothings)),
            mapOf(Pair("name", "busy"), Pair("data", remainders)),
            mapOf(Pair("name", "pauses"), Pair("data", pauses)),
            mapOf(Pair("name", "waits"), Pair("data", waits))
    )
    return result
}

