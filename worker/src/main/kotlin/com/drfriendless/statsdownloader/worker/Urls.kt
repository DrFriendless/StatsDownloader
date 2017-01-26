package com.drfriendless.statsdownloader.worker

/**
 * Created by john on 2/10/16.
 */
const val COLLECTION_URL = "https://boardgamegeek.com/xmlapi2/collection?username={0}&brief=1&stats=1"
const val PLAYED_URL = "https://boardgamegeek.com/plays/bymonth/user/{0}/subtype/boardgame"
const val PROFILE_URL = "https://boardgamegeek.com/user/{0}"

const val GAME_URL = "https://boardgamegeek.com/xmlapi/boardgame/{0}&stats=1"

const val TOP50_URL = "https://www.boardgamegeek.com/browse/boardgame"
const val MOST_VOTERS_URL = "https://boardgamegeek.com/browse/boardgame?sort=numvoters&sortdir=desc"