package com.mangalore.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object LocalDownloads {
    private const val PREFS = "mangalore_downloads"

    fun enqueue(context: Context, manga: MangaDetail, first: Int, last: Int) = enqueue(context, manga, (first..last).toList())
    fun enqueue(context: Context, manga: MangaDetail, indexes: List<Int>) {
        val chapters = indexes.distinct().sorted().mapNotNull { manga.chapters.getOrNull(it) }
        if (chapters.isEmpty()) return
        val key = manga.url.hashCode().toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key, "${manga.title}|${chapters.size}|0|${manga.coverUrl}").apply()
        val req = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(workDataOf("manga" to manga.title, "key" to key, "urls" to chapters.map { it.url }.toTypedArray()))
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }

    fun items(context: Context): List<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        .filterKeys { !it.startsWith("chapter_") }
        .values.mapNotNull { it as? String }

    fun localImages(context: Context, chapterUrl: String): List<String> {
        val dir = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("chapter_${chapterUrl.hashCode()}", null) ?: return emptyList()
        return File(dir).listFiles()?.sortedBy { it.name }?.map { it.absolutePath }.orEmpty()
    }
}

class ChapterDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urls = inputData.getStringArray("urls") ?: return@withContext Result.failure()
        val title = inputData.getString("manga") ?: "manga"
        val key = inputData.getString("key") ?: title.hashCode().toString()
        val root = File(applicationContext.filesDir, "downloads/${title.hashCode()}").apply { mkdirs() }
        val client = OkHttpClient()
        var completed = 0
        for ((index, url) in urls.withIndex()) {
            val (images, needsCf) = Scraper.fetchChapterImages(url)
            if (needsCf || images.isEmpty()) return@withContext Result.failure()
            val chapterDir = File(root, "chapter_$index").apply { mkdirs() }
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
            applicationContext.getSharedPreferences("mangalore_downloads", Context.MODE_PRIVATE).edit()
                .putString("chapter_${urls[index].hashCode()}", chapterDir.absolutePath)
                .putString(key, "$title|${urls.size}|$completed|").apply()
        }
        Result.success()
    }
}
