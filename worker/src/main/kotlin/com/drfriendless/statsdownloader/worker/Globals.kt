package com.drfriendless.statsdownloader.worker

import mu.KLogging

/**
 * Created by john on 25/09/16.
 */
const val SECONDS_BETWEEN_POPULATES = 540
const val TOP_50 = "Top 50"
const val ES_TOP_100 = "Extended Stats Top 100"

val SERIES_NOT_FROM_METADATA = listOf(TOP_50, ES_TOP_100)

object logging: KLogging()

val logger = logging.logger