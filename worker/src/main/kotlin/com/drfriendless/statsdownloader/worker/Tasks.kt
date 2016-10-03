package com.drfriendless.statsdownloader.worker

import java.net.URI

/**
 * A Task is a set of updates to the database, usually preceded by a download of a particular file.
 *
 * @author John Farrell
 */
interface Task {
    fun execute()
}

fun urlQuote(s: String): String {
    val uri = URI(null, null, s)
    val result = uri.toString()
    println("URL quoted $s to $result")
    return result
}

