package com.drfriendless.statsdownloader.httpd

import com.drfriendless.statsdb.database.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Data access methods for the Users table.
 *
 * @author John Farrell
 */
const val HOUR = 3600000L
const val DAY = 24 * HOUR
const val WEEK = 7 * DAY
const val MONTH = 365 * DAY / 12
const val YEAR = 365 * DAY

data class UserAccessRecord(val geek: String, val lastView: Date, val numViews: Int, val country: String) {
    constructor(row: ResultRow): this(
            row[Users.geek],
            row[Users.lastProfileView]?.toDate() ?: Date(0),
            row[Users.profileViews],
            row[Users.country] ?: "none"
    ) {}

    fun toJson(): Map<String, Any> {
        return mapOf(
                Pair("geek", geek),
                Pair("views", numViews),
                Pair("country", country)
        )
    }
}

data class AccessPeriod(val accessPeriod: AccessPeriodDetails): Comparable<AccessPeriod> {
    val users = mutableListOf<UserAccessRecord>()

    override fun compareTo(other: AccessPeriod) = accessPeriod.compareTo(other.accessPeriod)

    fun addUser(user: UserAccessRecord) = users.add(user)

    fun toJson(): Map<String, Any> {
        users.sortByDescending { it.numViews }
        return mapOf(
                Pair("periodName", accessPeriod.name),
                Pair("users", users)
        )
    }
}

data class AccessPeriodDetails(val name: String, val startTimeAgo: Long): Comparable<AccessPeriodDetails> {
    override fun compareTo(other: AccessPeriodDetails) = startTimeAgo.compareTo(other.startTimeAgo)
}

val ALL_ACCESS_PERIODS = listOf(
        AccessPeriodDetails("last day", DAY),
        AccessPeriodDetails("last 2 days", DAY * 2),
        AccessPeriodDetails("last week", WEEK),
        AccessPeriodDetails("last month", MONTH),
        AccessPeriodDetails("last 2 months", MONTH * 2),
        AccessPeriodDetails("last 6 months", MONTH * 6),
        AccessPeriodDetails("last year", YEAR),
        AccessPeriodDetails("last 2 years", YEAR * 2),
        AccessPeriodDetails("last 3 years", YEAR * 3),
        AccessPeriodDetails("last 20 years", YEAR * 20),
        AccessPeriodDetails("never accessed", Long.MAX_VALUE)
)

fun getUsersGroupedByLastAccess(): List<AccessPeriod> {
    val result = TreeMap<AccessPeriodDetails, AccessPeriod>()
    ALL_ACCESS_PERIODS.forEach {
        result.put(it, AccessPeriod(it))
    }
    val now = System.currentTimeMillis()
    val never = ALL_ACCESS_PERIODS[ALL_ACCESS_PERIODS.size-1]
    transaction {
        Users.selectAll().forEach {
            val rec = UserAccessRecord(it)
            val accessAgo = now - rec.lastView.time
            val ap = ALL_ACCESS_PERIODS.firstOrNull { accessAgo <= it.startTimeAgo } ?: never
            result[ap]!!.addUser(rec)
        }
    }
    return result.values.toList()
}

fun  getUsersGroupedByLastAccessJson(): Any {
    val data = getUsersGroupedByLastAccess()
    return toJson(data)
}