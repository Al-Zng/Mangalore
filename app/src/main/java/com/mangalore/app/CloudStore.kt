package com.mangalore.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AdminUser(val id: String, val email: String, val name: String, val avatar: String, val createdAt: String, val banned: Boolean)
data class CommentRecord(val id: String, val userId: String, val name: String, val content: String, val parentId: String?, val spoiler: Boolean, val createdAt: String)

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

    suspend fun adminUsers(): List<AdminUser> {
        if (!AuthStore.isOwner) return emptyList()
        val arr = JSONArray(call("/rest/v1/rpc/admin_list_users", "POST", "{}"))
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { o -> AdminUser(o.optString("user_id"), o.optString("email"), o.optString("display_name"), o.optString("avatar_url"), o.optString("created_at"), o.optBoolean("is_banned")) } }
    }

    suspend fun adminSetBanned(userId: String, banned: Boolean) {
        if (!AuthStore.isOwner) return
        call("/rest/v1/rpc/admin_set_user_banned", "POST", JSONObject().put("target_user", userId).put("should_ban", banned).put("ban_reason", "إشراف مانجالور").toString())
    }

    suspend fun adminDeleteUser(userId: String) {
        if (!AuthStore.isOwner) return
        call("/rest/v1/rpc/admin_delete_user", "POST", JSONObject().put("target_user", userId).toString())
    }

    suspend fun fetchComments(mangaUrl: String): List<CommentRecord> {
        val encoded = java.net.URLEncoder.encode(mangaUrl, "UTF-8")
        val arr = JSONArray(call("/rest/v1/comments?manga_url=eq.$encoded&select=id,user_id,content,parent_id,is_spoiler,created_at&order=created_at.asc", "GET"))
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { o -> CommentRecord(o.optString("id"), o.optString("user_id"), "", o.optString("content"), o.optString("parent_id").ifBlank { null }, o.optBoolean("is_spoiler"), o.optString("created_at")) } }
    }

    suspend fun addComment(mangaUrl: String, mangaSlug: String, content: String, spoiler: Boolean, parentId: String? = null) {
        if (!AuthStore.hasSession()) return
        val body = JSONObject().put("user_id", AuthStore.userId).put("manga_url", mangaUrl).put("manga_slug", mangaSlug).put("content", content.trim()).put("is_spoiler", spoiler).apply { if (parentId != null) put("parent_id", parentId) }
        call("/rest/v1/comments", "POST", body.toString())
    }

    suspend fun deleteComment(id: String) {
        if (!AuthStore.hasSession()) return
        call("/rest/v1/comments?id=eq.${java.net.URLEncoder.encode(id, "UTF-8")}", "DELETE")
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

    suspend fun fetchCustomLists(): Map<String, List<MangaItem>> {
        if (!AuthStore.hasSession()) return emptyMap()
        val encoded = java.net.URLEncoder.encode(AuthStore.userId, "UTF-8")
        val arr = JSONArray(call("/rest/v1/custom_manga_lists?user_id=eq.$encoded&select=list_name,manga_url,manga_slug,manga_title,cover_url,cover_full&order=created_at.asc", "GET"))
        val out = linkedMapOf<String, MutableList<MangaItem>>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.getOrPut(o.optString("list_name")) { mutableListOf() }.add(MangaItem(o.optString("manga_slug"), o.optString("manga_title"), o.optString("manga_slug"), o.optString("cover_url"), o.optString("cover_full"), o.optString("manga_url")))
        }
        return out
    }

    suspend fun addCustomList(name: String, item: MangaItem) {
        if (!AuthStore.hasSession()) return
        val o = JSONObject().put("user_id", AuthStore.userId).put("list_name", name).put("manga_url", item.url).put("manga_slug", item.slug).put("manga_title", item.title).put("cover_url", item.coverUrl).put("cover_full", item.coverFull)
        call("/rest/v1/custom_manga_lists", "POST", o.toString())
    }

    suspend fun removeCustomListItem(name: String, url: String) {
        if (AuthStore.hasSession()) call("/rest/v1/custom_manga_lists?user_id=eq.${AuthStore.userId}&list_name=eq.${java.net.URLEncoder.encode(name, "UTF-8")}&manga_url=eq.${java.net.URLEncoder.encode(url, "UTF-8")}", "DELETE")
    }

    suspend fun deleteCustomList(name: String) {
        if (AuthStore.hasSession()) call("/rest/v1/custom_manga_lists?user_id=eq.${AuthStore.userId}&list_name=eq.${java.net.URLEncoder.encode(name, "UTF-8")}", "DELETE")
    }

    suspend fun saveProgress(item: MangaItem, chapterUrl: String, chapterNumber: String, chapterIndex: Int, page: Int, total: Int, completed: Boolean) {
        if (!AuthStore.hasSession()) return
        val o = JSONObject().put("user_id", AuthStore.userId).put("manga_url", item.url).put("manga_slug", item.slug).put("manga_title", item.title).put("cover_url", item.coverUrl).put("chapter_url", chapterUrl).put("chapter_number", chapterNumber).put("chapter_index", chapterIndex).put("page", page).put("total_pages", total).put("completed", completed)
        call("/rest/v1/reading_progress?on_conflict=user_id,manga_url,chapter_url", "POST", o.toString())
    }

    suspend fun fetchProgress(): List<ReadingProgress> {
        if (!AuthStore.hasSession()) return emptyList()
        val arr = JSONArray(call("/rest/v1/reading_progress?user_id=eq.${AuthStore.userId}&select=manga_url,manga_slug,manga_title,cover_url,chapter_url,chapter_number,page,total_pages,completed&order=updated_at.desc", "GET"))
        val seenManga = mutableSetOf<String>()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (!seenManga.add(o.optString("manga_url"))) return@mapNotNull null
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
