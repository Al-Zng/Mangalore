package com.mangalore.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomList(val name: String, val items: List<MangaItem>)

object CustomListsStore {
    private const val PREFS = "mangalore_custom_lists"

    fun names(context: Context): List<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.keys.sorted()

    fun get(context: Context, name: String): List<MangaItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(name, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MangaItem(o.optString("id"), o.optString("title"), o.optString("slug"), o.optString("cover"), o.optString("full"), o.optString("url"))
        }
    }

    fun create(context: Context, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || names(context).contains(clean)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(clean, "[]").apply()
        return true
    }

    fun delete(context: Context, name: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(name).apply() }

    fun add(context: Context, name: String, item: MangaItem) {
        val items = get(context, name).filterNot { it.url == item.url } + item
        val arr = JSONArray()
        items.forEach { m -> arr.put(JSONObject().put("id", m.id).put("title", m.title).put("slug", m.slug).put("cover", m.coverUrl).put("full", m.coverFull).put("url", m.url)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(name, arr.toString()).apply()
    }

    fun remove(context: Context, name: String, url: String) {
        val item = get(context, name).firstOrNull { it.url == url } ?: return
        val arr = JSONArray()
        get(context, name).filterNot { it.url == item.url }.forEach { m -> arr.put(JSONObject().put("id", m.id).put("title", m.title).put("slug", m.slug).put("cover", m.coverUrl).put("full", m.coverFull).put("url", m.url)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(name, arr.toString()).apply()
    }
}
