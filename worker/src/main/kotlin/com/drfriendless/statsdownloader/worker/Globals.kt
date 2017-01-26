package com.drfriendless.statsdownloader.worker

import mu.KotlinLogging

/**
 * Created by john on 25/09/16.
 */
const val SECONDS_BETWEEN_POPULATES = 540
const val SERIES_TOP_50 = "Top 50"
const val SERIES_ES_TOP_100 = "Extended Stats Top 100"
const val SERIES_MOST_VOTERS = "Most Voters"

val SERIES_NOT_FROM_METADATA = listOf(SERIES_TOP_50, SERIES_ES_TOP_100, SERIES_MOST_VOTERS)

val logger = KotlinLogging.logger {}

val TILL_NEXT_UPDATE = mapOf(
        Pair(PROCESS_COLLECTION, THREE_DAYS),
        Pair(PROCESS_MARKET, THREE_DAYS),
        Pair(PROCESS_PLAYED, THREE_DAYS),
        Pair(PROCESS_GAME, LONGEST_TIME),
        Pair(PROCESS_TOP50, THREE_DAYS),
        Pair(PROCESS_MOST_VOTERS, THREE_DAYS),
        Pair(PROCESS_ES_TOP100, ONE_DAY),
        Pair(PROCESS_FRONT_PAGE, ONE_DAY)
)