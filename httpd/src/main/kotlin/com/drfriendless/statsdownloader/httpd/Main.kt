package com.drfriendless.statsdownloader.httpd

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
class Thing: Object() {}

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
    val httpdConfig = AppConfiguration()
    httpdConfig.port = 8084
    val server = AppServer(httpdConfig)
    val logger = LoggerFactory.getLogger("main")
    server.getLogError("/", {
//        response.contentType = "text/html"
//        response.send(renderPuzzle(puzzle))
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
    logger.info("Starting Downloader httpd")
    server.start()
}

fun Response.returnFileContents(path: String, contentType: String) {
    val u = Thing::class.java.getResource(path)
    println("returnFileContents $path $u")
    if (u != null) {
        val text = u.readText()
        this.contentLength = text.length.toLong()
        send(text, contentType)
    } else {
        setStatus(StatusCodes.NotFound)
    }
}