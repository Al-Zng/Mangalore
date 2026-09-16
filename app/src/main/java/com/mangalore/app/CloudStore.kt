package com.mangalore.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object CloudStore {
    private const val BASE = "https://ifczsjsqazlnogmyomsk.supabase.co"
    private const val KEY = "sb_publishable_HA8TWsQQG8IjQHko1JVwJA_IHFgUAk-"
    private val http = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    private suspend fun call(path: String, method: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(BASE + path).header("apikey", KEY).header("Authorization", "Bearer ${AuthStore.accessToken}").header("Accept", "application/json")
        if (method == "POST") b.header("Prefer", "resolution=merge-duplicates,return=minimal")
        if (body != null) b.method(method, body.toRequestBody(json)) else b.method(method, null)
        http.newCall(b.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Supabase request failed: ${r.code}")
            text
        }
    }

    suspend fun fetchLibrary(): List<MangaItem> {
        if (!AuthStore.hasSession()) return emptyList()
        val arr = JSONArray(call("/rest/v1/library_items?user_id=eq.${AuthStore.userId}&select=manga_url,manga_slug,manga_title,cover_url&order=created_at.desc", "GET"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MangaItem(o.optString("manga_slug"), o.optString("manga_title"), o.optString("manga_slug"), o.optString("cover_url"), o.optString("cover_url"), o.optString("manga_url"))
        }
    }

    suspend fun addLibrary(item: MangaItem) {
        if (!AuthStore.hasSession()) return
        val o = JSONObject().put("user_id", AuthStore.userId).put("manga_url", item.url).put("manga_slug", item.slug).put("manga_title", item.title).put("cover_url", item.coverUrl)
        call("/rest/v1/library_items?on_conflict=user_id,manga_url", "POST", o.toString())
    }

    suspend fun removeLibrary(url: String) {
        if (AuthStore.hasSession()) call("/rest/v1/library_items?user_id=eq.${AuthStore.userId}&manga_url=eq.${java.net.URLEncoder.encode(url, "UTF-8")}", "DELETE")
    }

    suspend fun saveProgress(item: MangaItem, chapterUrl: String, chapterNumber: String, chapterIndex: Int, page: Int, total: Int, completed: Boolean) {
        if (!AuthStore.hasSession()) return
        val o = JSONObject().put("user_id", AuthStore.userId).put("manga_url", item.url).put("manga_slug", item.slug).put("manga_title", item.title).put("cover_url", item.coverUrl).put("chapter_url", chapterUrl).put("chapter_number", chapterNumber).put("chapter_index", chapterIndex).put("page", page).put("total_pages", total).put("completed", completed)
        call("/rest/v1/reading_progress?on_conflict=user_id,manga_url,chapter_url", "POST", o.toString())
    }

    suspend fun fetchProgress(): List<ReadingProgress> {
        if (!AuthStore.hasSession()) return emptyList()
        val arr = JSONArray(call("/rest/v1/reading_progress?user_id=eq.${AuthStore.userId}&select=manga_url,manga_slug,manga_title,cover_url,chapter_url,chapter_number,page,total_pages,completed&order=updated_at.desc", "GET"))
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val item = MangaItem(o.optString("manga_slug"), o.optString("manga_title"), o.optString("manga_slug"), o.optString("cover_url"), o.optString("cover_url"), o.optString("manga_url"))
            val chapter = ChapterItem(o.optString("chapter_number"), "", o.optString("chapter_url"), "")
            val manga = MangaDetail(item.title, item.slug, item.coverUrl, item.coverFull, item.url, emptyList(), "", "", "", "", "", "", "", listOf(chapter))
            ReadingProgress(item, manga, 0, o.optInt("page", 1), o.optInt("total_pages", 0), o.optBoolean("completed"))
        }
    }

    suspend fun stats(): Triple<Int, Int, Int> {
        if (!AuthStore.hasSession()) return Triple(0, 0, 0)
        val library = call("/rest/v1/library_items?user_id=eq.${AuthStore.userId}&select=id", "GET").let { JSONArray(it).length() }
        val progress = call("/rest/v1/reading_progress?user_id=eq.${AuthStore.userId}&select=chapter_url,completed", "GET").let { JSONArray(it) }
        var completed = 0
        var inProgress = 0
        for (i in 0 until progress.length()) {
            val o = progress.getJSONObject(i)
            if (o.optBoolean("completed")) completed++
            else inProgress++
        }
        return Triple(completed, inProgress, library)
    }
}
