package com.drfriendless.statsdownloader.httpd

import com.drfriendless.statsdb.DBConfig
import com.drfriendless.statsdb.database.Database
import org.slf4j.LoggerFactory
import org.wasabifx.wasabi.app.AppConfiguration
import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.protocol.http.Response
import org.wasabifx.wasabi.protocol.http.StatusCodes
import org.wasabifx.wasabi.routing.RouteHandler
import java.io.File

/**
 * Created by john on 2/10/16.
 */
class Thing: Any() {}

fun AppServer.getLogError(path: kotlin.String, vararg handlers: RouteHandler.() -> kotlin.Unit): kotlin.Unit {
    val logger = org.slf4j.LoggerFactory.getLogger("handler")
    fun wrap(f: RouteHandler.() -> kotlin.Unit): RouteHandler.() -> kotlin.Unit {
        return {
            try {
                f()
            } catch (e: Throwable) {
                logger.error("Broken", e)
            }
        }
    }
    val wrapped = handlers.map { wrap(it) }.toTypedArray()
    this.get(path, *wrapped)
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: downloader db.properties")
        return
    }
    val httpdConfig = AppConfiguration()
    httpdConfig.port = 8084
    val server = AppServer(httpdConfig)
    val logger = LoggerFactory.getLogger("main")
    val db = Database(DBConfig(args[0]))
    server.getLogError("/", {
        response.redirect("/worker")
    })
    server.getLogError("/log", {
        val f = File("/home/john/subjects/geek/downloader.log")
        if (f.exists()) {
            val url = f.toURI().toURL()
            response.send(url.readText(), "text/plain")
        } else {
            response.returnFileContents("/html/noDownloadLog.html", "text/html")
        }
    })
    server.getLogError("/worker", {
        response.returnFileContents("/html/workerChart.html", "text/html")
    })
    server.getLogError("/access", {
        response.returnFileContents("/html/access.html", "text/html")
    })
    server.getLogError("/js/access.js", {
        response.returnFileContents("/js/access.js", "application/javascript")
    })
    server.getLogError("/css/access.css", {
        response.returnFileContents("/css/access.css", "text/css")
    })
    server.getLogError("/css/flags.css", {
        response.returnFileContents("/css/flags.css", "text/css")
    })
    server.getLogError("/images/blank.gif", {
        response.returnBinaryFileContents("/images/blank.gif", "image/gif")
    })
    server.getLogError("/images/flags.png", {
        response.returnBinaryFileContents("/images/flags.png", "image/png")
    })
    server.getLogError("/json/timeUsage", {
        val s = getDownloaderDataLast24HoursJson().toString()
        response.send(s, "application/json")
    })
    server.getLogError("/json/counts", {
        val s = getDownloaderCountsLast24HoursJson().toString()
        response.send(s, "application/json")
    })
    server.getLogError("/json/filecounts", {
        val s = getDownloaderFileCountsLast24HoursJson().toString()
        response.send(s, "application/json")
    })
    server.getLogError("/json/access", {
        val s = getUsersGroupedByLastAccessJson().toString()
        response.send(s, "application/json")
    })
    logger.info("Starting Downloader httpd")
    server.start()
}

fun Response.returnFileContents(path: String, contentType: String) {
    val u = Thing::class.java.getResource(path)
    if (u != null) {
        val text = u.readText()
        this.contentLength = text.length.toLong()
        send(text, contentType)
    } else {
        setStatus(StatusCodes.NotFound)
    }
}

fun Response.returnBinaryFileContents(path: String, contentType: String) {
    val u = Thing::class.java.getResource(path)
    if (u != null) {
        setFileResponseHeaders(u.file, contentType)
    } else {
        setStatus(StatusCodes.NotFound)
    }
}