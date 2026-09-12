package com.mangalore.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

data class MangaItem(
    val id: String,
    val title: String,
    val slug: String,
    val coverUrl: String,
    val url: String,
    val latestChapter: String = "",
    val chapterDate: String = "",
    val genres: List<String> = emptyList()
)

data class MangaDetail(
    val title: String,
    val slug: String,
    val coverUrl: String,
    val url: String,
    val genres: List<String>,
    val status: String,
    val author: String,
    val artist: String,
    val description: String,
    val rating: String,
    val views: String,
    val releaseYear: String,
    val origin: String,
    val chapters: List<ChapterItem>
)

data class ChapterItem(
    val number: String,
    val title: String,
    val url: String,
    val date: String
)

data class SearchResult(
    val items: List<MangaItem>,
    val hasMore: Boolean
)

// ─────────────────────────────────────────────────────────────
// COOKIE STORE (shared between WebView and OkHttp)
// ─────────────────────────────────────────────────────────────

object CookieStore {
    @Volatile var cfCookies: String = ""
    @Volatile var cfSolved: Boolean = false

    fun hasCookies() = cfCookies.isNotBlank()
}

// ─────────────────────────────────────────────────────────────
// SCRAPER
// ─────────────────────────────────────────────────────────────

object Scraper {

    private const val BASE = "https://mangalik.net"
    private val CF_CHALLENGE_URL = "$BASE/manga/the-worlds-best-engineer/1/"

    private val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"

    fun cfChallengeUrl(): String = CF_CHALLENGE_URL

    fun searchUrl(query: String) =
        "$BASE/?s=${query.trim().replace(" ", "+")}&post_type=wp-manga"

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ar,en;q=0.5")
                .header("Referer", BASE)
                .apply {
                    if (CookieStore.hasCookies()) {
                        header("Cookie", CookieStore.cfCookies)
                    }
                }
                .build()
            chain.proceed(req)
        }
        .build()

    private fun fetchHtml(url: String): String? = try {
        val resp = client().newCall(Request.Builder().url(url).build()).execute()
        resp.body?.string()
    } catch (e: Exception) { null }

    fun isCfChallenge(html: String): Boolean =
        html.contains("cf-browser-verification") ||
        html.contains("Just a moment") ||
        html.contains("Checking your browser") ||
        html.contains("challenge-error-title") ||
        html.length < 2000   // CF challenge pages are very small

    // ── Home page ──────────────────────────────────────────
    suspend fun fetchHome(): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = fetchHtml(BASE) ?: return@withContext emptyList()
        if (isCfChallenge(html)) return@withContext emptyList()
        val doc = Jsoup.parse(html)
        parseMangaItems(doc)
    }

    // ── Search ─────────────────────────────────────────────
    suspend fun search(query: String): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = fetchHtml(searchUrl(query)) ?: return@withContext emptyList()
        if (isCfChallenge(html)) return@withContext emptyList()
        val doc = Jsoup.parse(html)
        parseMangaItems(doc)
    }

    // ── Latest / Category pages ─────────────────────────────
    suspend fun fetchLatest(page: Int = 1): List<MangaItem> = withContext(Dispatchers.IO) {
        val url = "$BASE/manga-list/?status=&type=&order=latest&page=$page"
        val html = fetchHtml(url) ?: return@withContext emptyList()
        if (isCfChallenge(html)) return@withContext emptyList()
        Jsoup.parse(html).let { parseMangaItems(it) }
    }

    suspend fun fetchPopular(page: Int = 1): List<MangaItem> = withContext(Dispatchers.IO) {
        val url = "$BASE/manga-list/?status=&type=&order=trending&page=$page"
        val html = fetchHtml(url) ?: return@withContext emptyList()
        if (isCfChallenge(html)) return@withContext emptyList()
        Jsoup.parse(html).let { parseMangaItems(it) }
    }

    // ── Manga detail ────────────────────────────────────────
    suspend fun fetchDetail(url: String): MangaDetail? = withContext(Dispatchers.IO) {
        val html = fetchHtml(url) ?: return@withContext null
        if (isCfChallenge(html)) return@withContext null
        val doc = Jsoup.parse(html, url)

        val title = doc.selectFirst(".post-title h1")?.text()?.trim()
            ?: doc.selectFirst(".post-title h3")?.text()?.trim() ?: ""

        val cover = doc.selectFirst(".summary_image img")
            ?.let { it.attr("src").ifEmpty { it.attr("data-src") } } ?: ""

        val genres = doc.select(".genres-content a").map { it.text().trim() }

        val status = doc.select(".post-content_item")
            .find { it.selectFirst(".summary-heading h5")?.text()?.contains("الحالة") == true
                 || it.selectFirst(".summary-heading h5")?.text()?.contains("Status") == true }
            ?.selectFirst(".summary-content")?.text()?.trim() ?: ""

        val author = doc.select(".post-content_item")
            .find { it.selectFirst(".summary-heading h5")?.text()?.contains("المؤلف") == true
                 || it.selectFirst(".summary-heading h5")?.text()?.contains("Author") == true }
            ?.selectFirst(".summary-content")?.text()?.trim() ?: ""

        val artist = doc.select(".post-content_item")
            .find { it.selectFirst(".summary-heading h5")?.text()?.contains("الرسام") == true
                 || it.selectFirst(".summary-heading h5")?.text()?.contains("Artist") == true }
            ?.selectFirst(".summary-content")?.text()?.trim() ?: ""

        val year = doc.select(".post-content_item")
            .find { it.selectFirst(".summary-heading h5")?.text()?.contains("سنة") == true
                 || it.selectFirst(".summary-heading h5")?.text()?.contains("Released") == true }
            ?.selectFirst(".summary-content")?.text()?.trim() ?: ""

        val origin = doc.select(".post-content_item")
            .find { it.selectFirst(".summary-heading h5")?.text()?.contains("النوع") == true
                 || it.selectFirst(".summary-heading h5")?.text()?.contains("Type") == true }
            ?.selectFirst(".summary-content")?.text()?.trim() ?: ""

        val desc = doc.select(".description-summary p, .manga-excerpt p")
            .firstOrNull { it.text().length > 10 }?.text()?.trim() ?: ""

        val rating = doc.selectFirst(".post-rating .score")?.text()?.trim()
            ?: doc.selectFirst(".manga-rating")?.attr("data-score")?.trim() ?: ""

        val views = doc.selectFirst(".views .font-meta")?.text()?.trim() ?: ""

        val slug = url.trimEnd('/').substringAfterLast('/')

        val chapters = doc.select("li.wp-manga-chapter").map { li ->
            val a = li.selectFirst("a")
            val chUrl = a?.attr("href") ?: ""
            val chTitle = a?.text()?.trim() ?: ""
            val date = li.selectFirst(".chapter-release-date i, .chapter-release-date")
                ?.text()?.trim() ?: ""
            val num = chTitle.replace(Regex("[^0-9.]"), "").trim()
            ChapterItem(
                number = num.ifEmpty { "?" },
                title = chTitle,
                url = chUrl,
                date = date
            )
        }

        MangaDetail(
            title = title, slug = slug, coverUrl = cover, url = url,
            genres = genres, status = status, author = author, artist = artist,
            description = desc, rating = rating, views = views,
            releaseYear = year, origin = origin, chapters = chapters
        )
    }

    // ── Chapter images ──────────────────────────────────────
    // Returns (imageUrls, needsCFBypass)
    suspend fun fetchChapterImages(url: String): Pair<List<String>, Boolean> =
        withContext(Dispatchers.IO) {
            if (!CookieStore.cfSolved) return@withContext Pair(emptyList(), true)
            val html = fetchHtml(url) ?: return@withContext Pair(emptyList(), true)
            if (isCfChallenge(html)) return@withContext Pair(emptyList(), true)
            val doc = Jsoup.parse(html)
            val imgs = doc.select("img.wp-manga-chapter-img, .reading-content img")
                .map {
                    it.attr("src").ifEmpty { it.attr("data-src") }
                        .ifEmpty { it.attr("data-lazy-src") }
                }
                .filter { it.startsWith("http") }
            Pair(imgs, false)
        }

    // ── Helpers ─────────────────────────────────────────────
    private fun parseMangaItems(doc: org.jsoup.nodes.Document): List<MangaItem> {
        val items = mutableListOf<MangaItem>()

        // Try different selectors that mangalik uses
        val elements = doc.select(".page-item-detail.manga, .c-image-hover, .manga-item")
            .ifEmpty { doc.select("[class*=page-item-detail]") }

        elements.forEachIndexed { i, el ->
            val titleEl = el.selectFirst(".post-title a, h3 a, h5 a") ?: return@forEachIndexed
            val url = titleEl.attr("href").ifEmpty { return@forEachIndexed }
            val title = titleEl.text().trim().ifEmpty { return@forEachIndexed }
            val img = el.selectFirst("img")
            val cover = img?.let {
                it.attr("src").ifEmpty { it.attr("data-src") }
                    .ifEmpty { it.attr("data-lazy-src") }
            } ?: ""
            val chapter = el.selectFirst(".chapter a, .chapter-item a")?.text()?.trim() ?: ""
            val date = el.selectFirst(".post-on, .chapter-release-date")?.text()?.trim() ?: ""
            val slug = url.trimEnd('/').substringAfterLast('/')

            items.add(MangaItem(
                id = "$i-$slug",
                title = title,
                slug = slug,
                coverUrl = cover,
                url = url,
                latestChapter = chapter,
                chapterDate = date
            ))
        }

        // If no items found via detail selector, try list selector
        if (items.isEmpty()) {
            doc.select("h3.h5 a, .post-title h3 a").forEachIndexed { i, a ->
                val url = a.attr("href")
                val title = a.text().trim()
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    val slug = url.trimEnd('/').substringAfterLast('/')
                    items.add(MangaItem(
                        id = "$i-$slug", title = title, slug = slug,
                        coverUrl = "", url = url
                    ))
                }
            }
        }
        return items.distinctBy { it.url }
    }
}
