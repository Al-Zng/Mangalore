package com.mangalore.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomList(val name: String, val items: List<MangaItem>)

object CustomListsStore {
    private const val PREFS = "mangalore_custom_lists"

    // Never share custom lists between accounts on the same device.
    private fun prefs(context: Context) = context.getSharedPreferences(
        "${PREFS}_${AuthStore.userId.ifBlank { "guest" }}", Context.MODE_PRIVATE
    )

    fun names(context: Context): List<String> = prefs(context).all.keys.sorted()

    fun get(context: Context, name: String): List<MangaItem> {
        val raw = prefs(context).getString(name, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MangaItem(o.optString("id"), o.optString("title"), o.optString("slug"), o.optString("cover"), o.optString("full"), o.optString("url"))
        }
    }

    fun create(context: Context, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || names(context).contains(clean)) return false
        prefs(context).edit().putString(clean, "[]").apply()
        return true
    }

    fun delete(context: Context, name: String) { prefs(context).edit().remove(name).apply() }

    fun add(context: Context, name: String, item: MangaItem) {
        val items = get(context, name).filterNot { it.url == item.url } + item
        val arr = JSONArray()
        items.forEach { m -> arr.put(JSONObject().put("id", m.id).put("title", m.title).put("slug", m.slug).put("cover", m.coverUrl).put("full", m.coverFull).put("url", m.url)) }
        prefs(context).edit().putString(name, arr.toString()).apply()
    }

    fun remove(context: Context, name: String, url: String) {
        if (get(context, name).none { it.url == url }) return
        val arr = JSONArray()
        get(context, name).filterNot { it.url == url }.forEach { m -> arr.put(JSONObject().put("id", m.id).put("title", m.title).put("slug", m.slug).put("cover", m.coverUrl).put("full", m.coverFull).put("url", m.url)) }
        prefs(context).edit().putString(name, arr.toString()).apply()
    }
}
