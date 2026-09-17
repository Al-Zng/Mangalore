package com.mangalore.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class DownloadGroup(
    val key: String, val title: String, val cover: String, val total: Int, val done: Int,
    val mangaUrl: String = "", val chapterUrls: List<String> = emptyList(), val chapterNames: List<String> = emptyList(),
    val genres: List<String> = emptyList(), val status: String = "", val author: String = "",
    val artist: String = "", val description: String = "", val rating: String = "",
    val releaseYear: String = "", val origin: String = ""
)

object LocalDownloads {
    private const val PREFS = "mangalore_downloads"

    fun enqueue(context: Context, manga: MangaDetail, first: Int, last: Int) = enqueue(context, manga, (first..last).toList())
    fun enqueue(context: Context, manga: MangaDetail, indexes: List<Int>) {
        val chapters = indexes.distinct().sorted().mapNotNull { manga.chapters.getOrNull(it) }
        if (chapters.isEmpty()) return
        val key = manga.url.hashCode().toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = JSONObject().put("key", key).put("title", manga.title).put("cover", manga.coverUrl)
            .put("total", chapters.size).put("done", 0).put("mangaUrl", manga.url)
            .put("chapterUrls", JSONArray(chapters.map { it.url })).put("chapterNames", JSONArray(chapters.map { it.title })) .put("genres", JSONArray(manga.genres))
            .put("status", manga.status).put("author", manga.author).put("artist", manga.artist)
            .put("description", manga.description).put("rating", manga.rating).put("releaseYear", manga.releaseYear).put("origin", manga.origin)
        prefs.edit().putString(key, saved.toString()).apply()
        val req = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(workDataOf(
                "manga" to manga.title,
                "mangaUrl" to manga.url,
                "cover" to manga.coverUrl,
                "key" to key,
                "urls" to chapters.map { it.url }.toTypedArray()
            )).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("download_$key", ExistingWorkPolicy.REPLACE, req)
    }

    fun items(context: Context): List<String> = groups(context).map { "${it.title}|${it.total}|${it.done}|${it.cover}" }

    fun groups(context: Context): List<DownloadGroup> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        .filterKeys { !it.startsWith("chapter_") }
        .mapNotNull { (key, value) ->
            val raw = value as? String ?: return@mapNotNull null
            if (raw.trimStart().startsWith("{")) {
                val j = runCatching { JSONObject(raw) }.getOrNull() ?: return@mapNotNull null
                run {
                    val urls = j.optJSONArray("chapterUrls") ?: JSONArray()
                    val names = j.optJSONArray("chapterNames") ?: JSONArray()
                    val genres = j.optJSONArray("genres") ?: JSONArray()
                    DownloadGroup(j.optString("key", key), j.optString("title"), j.optString("cover"), j.optInt("total"), j.optInt("done"), j.optString("mangaUrl"),
                        (0 until urls.length()).map { urls.optString(it) }.filter { it.isNotBlank() },
                        (0 until names.length()).map { names.optString(it) },
                        (0 until genres.length()).map { genres.optString(it) }, j.optString("status"), j.optString("author"), j.optString("artist"), j.optString("description"), j.optString("rating"), j.optString("releaseYear"), j.optString("origin"))
                }
            } else {
            val p = raw.split("|")
            if (p.size >= 7) DownloadGroup(key, p[1], p[4], p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0, p[5], p[6].split("§§").filter { it.isNotBlank() })
            else if (p.size >= 5) DownloadGroup(key, p[1], p[4], p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0)
            else if (p.size >= 4) DownloadGroup(key, p[0], p.getOrNull(3).orEmpty(), p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0)
            else null }
        }

    fun localImages(context: Context, chapterUrl: String): List<String> {
        val dir = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("chapter_${chapterUrl.hashCode()}", null) ?: return emptyList()
        return File(dir).listFiles()?.sortedBy { it.name }?.map { it.absolutePath }.orEmpty()
    }

    fun delete(context: Context, group: DownloadGroup) {
        WorkManager.getInstance(context).cancelUniqueWork("download_${group.key}")
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(group.key)
        group.chapterUrls.forEach { edit.remove("chapter_${it.hashCode()}") }
        edit.apply()
        File(context.filesDir, "downloads/${group.title.hashCode()}").deleteRecursively()
    }

    fun storageBytes(context: Context): Long = File(context.filesDir, "downloads").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun clearAll(context: Context) { groups(context).forEach { delete(context, it) }; File(context.filesDir, "downloads").deleteRecursively() }
}

class ChapterDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urls = inputData.getStringArray("urls") ?: return@withContext Result.failure()
        val title = inputData.getString("manga") ?: "manga"
        val cover = inputData.getString("cover") ?: ""
        val key = inputData.getString("key") ?: title.hashCode().toString()
        val root = File(applicationContext.filesDir, "downloads/${title.hashCode()}").apply { mkdirs() }
        val client = OkHttpClient()
        CookieStore.load(applicationContext)
        val coverBitmap: Bitmap? = runCatching {
            if (cover.startsWith("http")) client.newCall(Request.Builder().url(cover).build()).execute().use { response -> response.body?.byteStream()?.use { BitmapFactory.decodeStream(it) } } else null
        }.getOrNull()
        var completed = 0
        val prefs = applicationContext.getSharedPreferences("mangalore_downloads", Context.MODE_PRIVATE)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mangalore_downloads"
        if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(channelId, "تنزيلات مانجالور", NotificationManager.IMPORTANCE_LOW))
        fun notify(done: Int, text: String, ongoing: Boolean = true) {
            nm.notify(key.hashCode(), NotificationCompat.Builder(applicationContext, channelId).setSmallIcon(com.mangalore.app.R.drawable.app).setLargeIcon(coverBitmap).setContentTitle("تنزيل $title").setContentText(text).setOnlyAlertOnce(true).setOngoing(ongoing).setProgress(urls.size, done, false).build())
        }
        notify(0, "بدء التنزيل…")
        for ((index, url) in urls.withIndex()) {
            if (LocalDownloads.localImages(applicationContext, url).isNotEmpty()) {
                completed++
                notify(completed, "$completed من ${urls.size} فصول")
                continue
            }
            val (images, needsCf) = Scraper.fetchChapterImages(url)
            if (needsCf || images.isEmpty()) { notify(completed, "سيُستأنف التنزيل تلقائياً عند توفر الاتصال", false); return@withContext Result.retry() }
            val chapterDir = File(root, "chapter_${url.hashCode()}").apply { mkdirs() }
            for ((page, image) in images.withIndex()) {
                val file = File(chapterDir, "%04d.jpg".format(page))
                client.newCall(Request.Builder().url(image)
                    .header("Referer", "https://mangalik.net/")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.0.0")
                    .apply { if (CookieStore.has()) header("Cookie", CookieStore.cfCookies) }
                    .build()).execute().use { response ->
                        if (!response.isSuccessful) return@withContext Result.retry()
                        response.body?.bytes()?.let(file::writeBytes) ?: return@withContext Result.retry()
                    }
            }
            completed++
            val old = prefs.getString(key, "{}").orEmpty()
            val updated = if (old.trimStart().startsWith("{")) JSONObject(old).put("done", completed).toString()
            else "$key|$title|${urls.size}|$completed|$cover|${inputData.getString("mangaUrl").orEmpty()}|${urls.joinToString("§§")}"
            prefs.edit().putString("chapter_${url.hashCode()}", chapterDir.absolutePath).putString(key, updated).apply()
            notify(completed, "$completed من ${urls.size} فصول")
        }
        nm.cancel(key.hashCode())
        Result.success()
    }
}
