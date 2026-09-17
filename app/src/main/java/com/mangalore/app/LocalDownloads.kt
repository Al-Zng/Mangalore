package com.mangalore.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class DownloadGroup(val key: String, val title: String, val cover: String, val total: Int, val done: Int, val mangaUrl: String = "", val chapterUrls: List<String> = emptyList())

object LocalDownloads {
    private const val PREFS = "mangalore_downloads"

    fun enqueue(context: Context, manga: MangaDetail, first: Int, last: Int) = enqueue(context, manga, (first..last).toList())
    fun enqueue(context: Context, manga: MangaDetail, indexes: List<Int>) {
        val chapters = indexes.distinct().sorted().mapNotNull { manga.chapters.getOrNull(it) }
        if (chapters.isEmpty()) return
        val key = manga.url.hashCode().toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(key, "$key|${manga.title}|${manga.chapters.size}|0|${manga.coverUrl}|${manga.url}|${chapters.joinToString("§§") { it.url }}").apply()
        val req = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(workDataOf(
                "manga" to manga.title,
                "mangaUrl" to manga.url,
                "cover" to manga.coverUrl,
                "key" to key,
                "urls" to chapters.map { it.url }.toTypedArray()
            )).build()
        WorkManager.getInstance(context).enqueue(req)
    }

    fun items(context: Context): List<String> = groups(context).map { "${it.title}|${it.total}|${it.done}|${it.cover}" }

    fun groups(context: Context): List<DownloadGroup> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        .filterKeys { !it.startsWith("chapter_") }
        .mapNotNull { (key, value) ->
            val p = (value as? String)?.split("|") ?: return@mapNotNull null
            if (p.size >= 7) DownloadGroup(key, p[1], p[4], p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0, p[5], p[6].split("§§").filter { it.isNotBlank() })
            else if (p.size >= 5) DownloadGroup(key, p[1], p[4], p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0)
            else if (p.size >= 4) DownloadGroup(key, p[0], p.getOrNull(3).orEmpty(), p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0)
            else null
        }

    fun localImages(context: Context, chapterUrl: String): List<String> {
        val dir = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("chapter_${chapterUrl.hashCode()}", null) ?: return emptyList()
        return File(dir).listFiles()?.sortedBy { it.name }?.map { it.absolutePath }.orEmpty()
    }
}

class ChapterDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urls = inputData.getStringArray("urls") ?: return@withContext Result.failure()
        val title = inputData.getString("manga") ?: "manga"
        val cover = inputData.getString("cover") ?: ""
        val key = inputData.getString("key") ?: title.hashCode().toString()
        val root = File(applicationContext.filesDir, "downloads/${title.hashCode()}").apply { mkdirs() }
        val client = OkHttpClient()
        var completed = 0
        val prefs = applicationContext.getSharedPreferences("mangalore_downloads", Context.MODE_PRIVATE)
        for ((index, url) in urls.withIndex()) {
            if (LocalDownloads.localImages(applicationContext, url).isNotEmpty()) {
                completed++
                continue
            }
            val (images, needsCf) = Scraper.fetchChapterImages(url)
            if (needsCf || images.isEmpty()) return@withContext Result.failure()
            val chapterDir = File(root, "chapter_${url.hashCode()}").apply { mkdirs() }
            for ((page, image) in images.withIndex()) {
                val file = File(chapterDir, "%04d.jpg".format(page))
                client.newCall(Request.Builder().url(image)
                    .header("Referer", "https://mangalik.net/")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.0.0")
                    .apply { if (CookieStore.has()) header("Cookie", CookieStore.cfCookies) }
                    .build()).execute().use { response ->
                        if (!response.isSuccessful) return@withContext Result.failure()
                        response.body?.bytes()?.let(file::writeBytes) ?: return@withContext Result.failure()
                    }
            }
            completed++
            prefs.edit()
                .putString("chapter_${url.hashCode()}", chapterDir.absolutePath)
                .putString(key, "$key|$title|${urls.size}|$completed|$cover|${inputData.getString("mangaUrl").orEmpty()}|${urls.joinToString("§§")}")
                .apply()
        }
        Result.success()
    }
}
