package com.mangalore.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// MODELS
// ─────────────────────────────────────────────────────────────
data class MangaItem(
    val id: String,
    val title: String,
    val slug: String,
    val coverUrl: String,          // thumbnail (for lists)
    val coverFull: String = "",    // full-resolution cover
    val url: String,
    val latestChapter: String = "",
    val prevChapter: String = "",
    val chapterDate: String = "",
    val score: String = "",
    val genres: List<String> = emptyList()
)

data class MangaDetail(
    val title: String,
    val slug: String,
    val coverUrl: String,
    val coverFull: String,
    val url: String,
    val genres: List<String>,
    val status: String,
    val author: String,
    val artist: String,
    val description: String,
    val rating: String,
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

// ─────────────────────────────────────────────────────────────
// COOKIE STORE
// ─────────────────────────────────────────────────────────────
object CookieStore {
    @Volatile var cfCookies: String = ""
    @Volatile var cfSolved:  Boolean = false
    fun has() = cfCookies.isNotBlank()
}

// ─────────────────────────────────────────────────────────────
// SCRAPER
// ─────────────────────────────────────────────────────────────
object Scraper {

    private fun cleanMeta(v: String): String {
        val bad = setOf("updating", "n/a", "unknown", "-", "?", "تحديث", "جاري التحديث")
        return if (v.trim().lowercase() in bad) "" else v.trim()
    }

    private const val BASE = "https://mangalik.net"
    private const val IO   = "https://io.mangalik.net"
    // A chapter page that reliably triggers CF for the bypass flow
    private const val CF_URL = "$BASE/manga/the-worlds-best-engineer/1/"

    private val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    fun cfChallengeUrl() = CF_URL
    fun searchUrl(q: String) = "$BASE/?s=${q.trim().replace(" ", "+")}&post_type=wp-manga"
    fun latestUrl(page: Int = 1) = "$BASE/manga-list/?status=&type=&order=latest&page=$page"
    fun allUrl(page: Int = 1) = "$BASE/manga-list/?page=$page"

    suspend fun fetchAll(page: Int = 1): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = get(allUrl(page)) ?: return@withContext emptyList()
        if (isCf(html)) return@withContext emptyList()
        parseMangaList(Jsoup.parse(html))
    }
    fun popularUrl(page: Int = 1) = "$BASE/manga-list/?status=&type=&order=trending&page=$page"

    // ── Upgrade thumbnail URL to full-size ────────────────────
    fun fullSizeUrl(url: String): String {
        if (url.isBlank()) return url
        // Remove WordPress size suffix  -175x238  -110x150  -193x278 etc.
        return url.replace(Regex("-\\d{2,4}x\\d{2,4}(?=\\.[a-zA-Z]+$)"), "")
    }

    // ── HTTP client ───────────────────────────────────────────
    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val r = chain.request().newBuilder()
                .header("User-Agent", UA)
                .header("Accept", "text/html,*/*;q=0.8")
                .header("Accept-Language", "ar,en;q=0.5")
                .header("Referer", BASE)
                .apply { if (CookieStore.has()) header("Cookie", CookieStore.cfCookies) }
                .build()
            chain.proceed(r)
        }.build()

    private fun get(url: String): String? = try {
        client().newCall(Request.Builder().url(url).build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            Log.d("MangaloreHttp", "GET $url code=${response.code} bytes=${body.length}")
            if (!response.isSuccessful) Log.e("MangaloreHttp", "HTTP ${response.code} for $url")
            body
        }
    } catch (t: Throwable) {
        Log.e("MangaloreHttp", "Request failed: $url", t)
        null
    }

    fun isCf(html: String) = html.contains("Just a moment") ||
            html.contains("cf-browser-verification") ||
            html.contains("Checking your browser") ||
            html.length < 1500

    // ── Home ──────────────────────────────────────────────────
    suspend fun fetchHome(): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = get(BASE) ?: return@withContext emptyList()
        if (isCf(html)) return@withContext emptyList()
        parseMangaList(Jsoup.parse(html))
    }

    // ── Latest updates ────────────────────────────────────────
    suspend fun fetchLatest(page: Int = 1): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = get(latestUrl(page)) ?: return@withContext emptyList()
        if (isCf(html)) return@withContext emptyList()
        parseMangaList(Jsoup.parse(html))
    }

    // ── Popular ───────────────────────────────────────────────
    suspend fun fetchPopular(page: Int = 1): List<MangaItem> = withContext(Dispatchers.IO) {
        val html = get(popularUrl(page)) ?: return@withContext emptyList()
        if (isCf(html)) return@withContext emptyList()
        parseMangaList(Jsoup.parse(html))
    }

    // ── Search ────────────────────────────────────────────────
    suspend fun search(query: String): List<MangaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val html = get(searchUrl(query)) ?: return@withContext emptyList()
        if (isCf(html)) return@withContext emptyList()
        val doc = Jsoup.parse(html)
        // Search results use c-tabs-item structure
        val searchItems = doc.select(".c-tabs-item .c-image-hover, .tab-thumb-wrap")
        if (searchItems.isNotEmpty()) {
            parseSearchResults(doc)
        } else {
            parseMangaList(doc)
        }
    }

    // ── Manga detail ──────────────────────────────────────────
    suspend fun fetchDetail(url: String): MangaDetail? = withContext(Dispatchers.IO) {
        val html = get(url) ?: return@withContext null
        if (isCf(html)) return@withContext null
        val doc = Jsoup.parse(html, url)

        // Title
        val title = doc.selectFirst("h1.post-title, .post-title h1")?.text()?.trim()
            ?: doc.selectFirst(".post-title")?.text()?.trim() ?: ""

        // Full-size cover: from og:image first (highest quality), then summary_image
        val ogImg  = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""
        val covImg = doc.selectFirst(".summary_image img")?.let {
            it.attr("src").ifEmpty { it.attr("data-src") }
        } ?: ""
        val coverFull = ogImg.ifEmpty { fullSizeUrl(covImg) }
        val coverThumb = covImg.ifEmpty { ogImg }

        // Genres
        val genres = doc.select(".genres-content a").map { it.text().trim() }

        // Meta fields - handle both Arabic and English labels
        fun metaVal(vararg labels: String): String {
            return doc.select(".post-content_item").firstOrNull { el ->
                val h = el.selectFirst(".summary-heading h5")?.text()?.trim() ?: ""
                labels.any { h.contains(it, ignoreCase = true) }
            }?.selectFirst(".summary-content")?.text()?.trim() ?: ""
        }

        val status = cleanMeta(metaVal("الحالة", "Status", "Durum"))
        val author = cleanMeta(metaVal("المؤلف", "Author", "Yazar"))
        val artist = cleanMeta(metaVal("الرسام", "Artist", "Çizer"))
        val year   = cleanMeta(metaVal("سنة", "Released", "Year"))
        val origin = cleanMeta(metaVal("النوع", "Type", "Tür"))

        // Description - clean of links/tags
        val desc = doc.selectFirst(".description-summary .summary__content, .description-summary p, .manga-excerpt p")
            ?.text()?.trim() ?: ""

        // Rating
        val rating = doc.selectFirst(".score.font-meta, .post-rating .score")?.text()?.trim() ?: ""

        // Slug
        val slug = url.trimEnd('/').substringAfterLast('/')

        // Chapters
        val chapters = doc.select("li.wp-manga-chapter").mapNotNull { li ->
            val a    = li.selectFirst("a") ?: return@mapNotNull null
            val chUrl   = a.attr("href").trim()
            val chTitle = a.text().trim()
            val date    = li.selectFirst(".chapter-release-date i, .chapter-release-date")
                ?.text()?.trim() ?: ""
            val num  = chTitle.replace(Regex("[^0-9.]"), "").trim()
            ChapterItem(
                number = num.ifEmpty { "?" },
                title  = chTitle,
                url    = chUrl,
                date   = date
            )
        }

        MangaDetail(
            title = title, slug = slug,
            coverUrl = coverThumb, coverFull = coverFull,
            url = url, genres = genres, status = status,
            author = author, artist = artist,
            description = desc, rating = rating,
            releaseYear = year, origin = origin,
            chapters = chapters
        )
    }

    // ── Chapter images ────────────────────────────────────────
    // Returns (images, needsCfBypass)
    suspend fun fetchChapterImages(url: String): Pair<List<String>, Boolean> =
        withContext(Dispatchers.IO) {
            if (!CookieStore.cfSolved) return@withContext Pair(emptyList(), true)
            Log.d("MangaloreChapter", "Fetching chapter: $url")
            val html = get(url) ?: return@withContext Pair(emptyList(), true)
            if (isCf(html)) {
                Log.w("MangaloreChapter", "Cloudflare/challenge response for: $url")
                return@withContext Pair(emptyList(), true)
            }
            val doc = Jsoup.parse(html)
            val imgs = doc.select("img.wp-manga-chapter-img, .reading-content img, .read-container img")
                .mapNotNull { el ->
                    listOf("data-src", "data-lazy-src", "data-original", "src")
                        .asSequence()
                        .map { el.attr(it).trim() }
                        .firstOrNull { it.startsWith("http") }
                }
                .distinct()
            Log.d("MangaloreChapter", "Chapter images found=${imgs.size} url=$url")
            if (imgs.isEmpty()) Log.e("MangaloreChapter", "No chapter images matched selectors for: $url")
            Pair(imgs, false)
        }

    // ── Parsers ───────────────────────────────────────────────
    private fun parseMangaList(doc: org.jsoup.nodes.Document): List<MangaItem> {
        val items = mutableListOf<MangaItem>()
        doc.select(".page-item-detail.manga, [class*=page-item-detail]").forEachIndexed { i, el ->
            val a     = el.selectFirst(".post-title a, h3 a, h5 a") ?: return@forEachIndexed
            val url   = a.attr("href").trim().ifEmpty { return@forEachIndexed }
            val title = a.text().trim().ifEmpty { return@forEachIndexed }
            val img   = el.selectFirst("img")
            val thumb = img?.let { it.attr("src").ifEmpty { it.attr("data-src") } } ?: ""
            // Try to get larger version from srcset
            val srcset = img?.attr("srcset") ?: ""
            val bigThumb = srcset.split(",").map { it.trim() }.maxByOrNull {
                it.split(" ").lastOrNull()?.replace("w","")?.toIntOrNull() ?: 0
            }?.split(" ")?.firstOrNull() ?: thumb
            val fullCover = fullSizeUrl(bigThumb.ifEmpty { thumb })

            // Chapters listed under item (latest 2)
            val chapterLinks = el.select(".list-chapter .chapter a, .chapter-item .chapter a")
            val latest = chapterLinks.firstOrNull()?.text()?.trim() ?: ""
            val prev   = chapterLinks.getOrNull(1)?.text()?.trim() ?: ""
            val date   = el.selectFirst(".post-on.font-meta, .chapter-release-date")?.text()?.trim() ?: ""

            // Score
            val score  = el.selectFirst(".score.font-meta, .total_votes")?.text()?.trim() ?: ""

            val slug = url.trimEnd('/').substringAfterLast('/')
            items += MangaItem(
                id = "$i-$slug", title = title, slug = slug,
                coverUrl = fullCover.ifEmpty { thumb },
                coverFull = fullCover,
                url = url,
                latestChapter = latest,
                prevChapter = prev,
                chapterDate = date,
                score = score
            )
        }
        return items.distinctBy { it.url }
    }

    private fun parseSearchResults(doc: org.jsoup.nodes.Document): List<MangaItem> {
        val items = mutableListOf<MangaItem>()
        // Madara search uses .c-tabs-item__content blocks
        doc.select(".c-tabs-item .tab-thumb-wrap, .c-tabs-item .c-image-hover").forEachIndexed { i, el ->
            val parent = el.parents().firstOrNull { it.hasClass("c-tabs-item") } ?: el.parent() ?: return@forEachIndexed
            val a     = parent.selectFirst("a") ?: return@forEachIndexed
            val url   = a.attr("href").trim().ifEmpty { return@forEachIndexed }
            val title = parent.selectFirst(".post-title")?.text()?.trim()
                ?: a.attr("title").ifEmpty { return@forEachIndexed }
            val img   = parent.selectFirst("img")
            val thumb = img?.let { it.attr("src").ifEmpty { it.attr("data-src") } } ?: ""
            val full  = fullSizeUrl(thumb)
            val slug  = url.trimEnd('/').substringAfterLast('/')
            items += MangaItem(
                id = "$i-$slug", title = title, slug = slug,
                coverUrl = full.ifEmpty { thumb }, coverFull = full, url = url
            )
        }
        // Fallback to general parsing
        return items.ifEmpty { parseMangaList(doc) }.distinctBy { it.url }
    }
}
