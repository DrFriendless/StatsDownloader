package com.drfriendless.statsdownloader.httpd

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonElement
import java.util.*

/**
 * Created by john on 8/10/16.
 */

// if I understood Kotson I might not have to do this.
fun toJson(obj: Any?): JsonElement {
    if (obj == null) throw RuntimeException("null")
    when (obj) {
        is String -> return obj.toJson()
        is Int -> return obj.toJson()
        is Long -> return obj.toJson()
        is Date -> return obj.time.toJson()
        is AccessPeriod -> return toJson(obj.toJson())
        is UserAccessRecord -> return return toJson(obj.toJson())
        is Collection<*> -> return jsonArray(obj.map(::toJson))
        is Map<*,*> -> return jsonObject(obj.entries.map { Pair(it.key.toString(), toJson(it.value)) })
        else -> throw RuntimeException("Can't convert $obj")
    }
}
