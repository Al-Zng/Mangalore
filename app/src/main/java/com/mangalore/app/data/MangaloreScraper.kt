package com.mangalore.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Parsed content models used by the UI and reader. */
data class MangaSummary(
    val title: String,
    val url: String,
    val coverUrl: String?,
    val score: Float?,
    val genres: List<String>,
    val status: String?,
    val latestChapter: String?
)

data class MangaDetails(
    val summary: MangaSummary,
    val description: String,
    val chapters: List<Chapter>
)

data class Chapter(val title: String, val url: String, val number: String?)
data class ReaderPage(val index: Int, val imageUrl: String)
data class ReaderContent(val title: String, val pages: List<ReaderPage>)

data class ChallengeRequest(val url: String, val userAgent: String, val reason: String)
data class ChallengeResolution(val cookieHeader: String?, val userAgent: String?)

/** The Activity can present an embedded WebView and call [resume] after the user completes verification. */
interface ChallengeHandler {
    suspend fun requestChallenge(request: ChallengeRequest): ChallengeResolution?
}

class NoopChallengeHandler : ChallengeHandler {
    override suspend fun requestChallenge(request: ChallengeRequest): ChallengeResolution? = null
}

private class PersistentCookieJar(context: Context) : CookieJar {
    private val file = File(context.cacheDir, "mangalore-cookies.txt")
    private val cookies = mutableMapOf<String, MutableList<Cookie>>()

    init {
        if (file.exists()) file.readLines().forEach { line ->
            runCatching { Cookie.parse("https://mangalik.net/".toHttpUrl(), line) }
                .getOrNull()?.let { cookies.getOrPut(it.domain) { mutableListOf() }.add(it) }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies[url.host] = cookies.toMutableList()
        file.parentFile?.mkdirs()
        file.writeText(this.cookies.values.flatten().joinToString("\n") { it.toString() })
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookies.values.flatten().filter { it.matches(url) }

    fun importHeader(url: HttpUrl, header: String) {
        header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }.forEach { cookie ->
            cookies.getOrPut(url.host) { mutableListOf() }.removeAll { it.name == cookie.name }
            cookies.getOrPut(url.host) { mutableListOf() }.add(cookie)
        }
        file.parentFile?.mkdirs()
        file.writeText(cookies.values.flatten().joinToString("\n") { it.toString() })
    }
}

class MangaloreScraper(
    context: Context,
    private val baseUrl: String = "https://mangalik.net",
    private val challengeHandler: ChallengeHandler = NoopChallengeHandler(),
    private val cacheTtlMs: Long = 5 * 60 * 1000L
) {
    private val cookieJar = PersistentCookieJar(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
    private val cacheDir = File(context.cacheDir, "mangalore-html")
    @Volatile private var currentUserAgent: String = USER_AGENT

    suspend fun search(query: String, page: Int = 1): List<MangaSummary> =
        fetchHtml("/?s=${query.trim().replace(" ", "+")}&post_type=wp-manga", page)
            .let(::parseMangaList)

    suspend fun catalog(page: Int = 1): List<MangaSummary> =
        fetchHtml("/manga/page/$page/", page).let(::parseMangaList)

    suspend fun details(url: String): MangaDetails =
        fetchHtml(normalizePath(url), 1).let(::parseDetails)

    suspend fun reader(url: String): ReaderContent =
        fetchHtml(normalizePath(url), 1).let(::parseReader)

    private suspend fun fetchHtml(path: String, page: Int): String = withContext(Dispatchers.IO) {
        val key = path.replace(Regex("[^A-Za-z0-9._-]"), "_") + "_$page.html"
        val cached = File(cacheDir, key)
        if (cached.exists() && System.currentTimeMillis() - cached.lastModified() < cacheTtlMs) return@withContext cached.readText()
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val request = Request.Builder().url(baseUrl.trimEnd('/') + path)
                    .header("User-Agent", currentUserAgent).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.code == 403 || looksLikeChallenge(body)) {
                        val solved = challengeHandler.requestChallenge(ChallengeRequest(request.url.toString(), currentUserAgent, "Protected page"))
                        if (solved != null) {
                            solved.cookieHeader?.let { cookieJar.importHeader(request.url, it) }
                            solved.userAgent?.takeIf { it.isNotBlank() }?.let { currentUserAgent = it }
                            throw RetryRequest()
                        }
                        throw ChallengeRequiredException(request.url.toString())
                    }
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    cacheDir.mkdirs(); cached.writeText(body); return@withContext body
                }
            } catch (e: RetryRequest) {
                lastError = e
            } catch (e: ChallengeRequiredException) {
                throw e
            } catch (e: IOException) {
                lastError = e
            }
            kotlinx.coroutines.delay(350L * (1L shl attempt))
        }
        throw lastError ?: IOException("Request failed")
    }

    private fun parseMangaList(html: String): List<MangaSummary> = Jsoup.parse(html)
        .select(".c-tabs-item__content, .page-item-detail.manga, .row.c-tabs-item__content")
        .mapNotNull { item ->
            val link = item.selectFirst(".tab-thumb a, .row-title a, .post-title a, a[href*='/manga/']") ?: return@mapNotNull null
            MangaSummary(
                title = link.text().trim(), url = link.absUrl("href"),
                coverUrl = item.selectFirst("img")?.absUrl("src")?.ifBlank { null },
                score = item.selectFirst(".post-total-rating .score, .post-total-rating")?.text()?.toFloatOrNull(),
                genres = item.select(".post-meta .post-meta-item, .genres-content a").map { it.text().trim() },
                status = item.selectFirst(".post-meta .post-meta-item:last-child")?.text()?.trim(),
                latestChapter = item.selectFirst(".chapter a")?.text()?.trim()
            )
        }.distinctBy { it.url }

    private fun parseDetails(html: String): MangaDetails {
        val doc = Jsoup.parse(html)
        val titleNode = doc.selectFirst(".post-title h1, .post-title")
        val summary = MangaSummary(
            titleNode?.text()?.trim().orEmpty(), titleNode?.parent()?.absUrl("href").orEmpty(),
            doc.selectFirst(".summary_image img, .summary_image a img")?.absUrl("src"),
            doc.selectFirst(".post-total-rating .score, .post-total-rating")?.text()?.toFloatOrNull(),
            doc.select(".genres-content a").map { it.text().trim() },
            doc.selectFirst(".post-content_item:contains(Status) .summary-content")?.text()?.trim(),
            null
        )
        val chapters = doc.select(".listing-chapter_wrap .wp-manga-chapter a, .chapter-list .wp-manga-chapter a")
            .map { Chapter(it.text().trim(), it.absUrl("href"), Regex("[0-9]+(?:\\.[0-9]+)?").find(it.text())?.value) }
        return MangaDetails(summary, doc.selectFirst(".summary__content, .description-summary")?.text()?.trim().orEmpty(), chapters)
    }

    private fun parseReader(html: String): ReaderContent {
        val doc = Jsoup.parse(html)
        return ReaderContent(doc.selectFirst(".c-breadcrumb-wrapper .breadcrumb li:last-child")?.text()?.trim().orEmpty(),
            doc.select("img.wp-manga-chapter-img").mapIndexed { i, image -> ReaderPage(i, image.absUrl("src")) })
    }

    private fun normalizePath(value: String): String = value.removePrefix(baseUrl).ifBlank { "/" }
    private fun looksLikeChallenge(body: String): Boolean = Regex("""(?i)(cf-chl-|challenge-platform|turnstile|just a moment\.\.\.)""").containsMatchIn(body)

    private class RetryRequest : IOException()
    class ChallengeRequiredException(val challengeUrl: String) : IOException("Verification required")

    companion object { const val USER_AGENT = "Mangalore/1.0 (Android)" }
}
