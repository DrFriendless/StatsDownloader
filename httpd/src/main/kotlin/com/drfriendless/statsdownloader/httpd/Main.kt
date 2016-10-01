package com.drfriendless.statsdownloader.httpd

import org.slf4j.LoggerFactory
import org.wasabifx.wasabi.app.AppConfiguration
import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.routing.RouteHandler

/**
 * Created by john on 2/10/16.
 */
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
    val server = AppServer(httpdConfig)
    val logger = LoggerFactory.getLogger("main")
    server.getLogError("/", {
//        response.contentType = "text/html"
//        response.send(renderPuzzle(puzzle))
    })
    server.getLogError("/images/:icon", {
//        val icon = request.routeParams["icon"]
//        response.setFileResponseHeaders(serveFile("/icons/svg/$icon"), "image/svg+xml")
    })
    logger.info("Starting Downloader httpd")
    server.start()
}