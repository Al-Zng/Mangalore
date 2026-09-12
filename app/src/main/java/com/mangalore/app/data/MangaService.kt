package com.mangalore.app.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

private const val BASE_URL = "https://lekmanga.site"
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"

class MangaService private constructor(private val appContext: Context) {

    companion object {
        @Volatile private var instance: MangaService? = null
        fun getInstance(context: Context): MangaService =
            instance ?: synchronized(this) {
                instance ?: MangaService(context.applicationContext).also { instance = it }
            }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Referer", BASE_URL)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ar,en;q=0.9")
                .build()
            chain.proceed(request)
        })
        .build()

    // MARK: - HTML fetching (OkHttp first, WebView fallback for Cloudflare)
    private suspend fun fetchHTML(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""

        val isCloudflare = response.code == 403 ||
            body.contains("Just a moment") ||
            body.contains("cf-browser-verification") ||
            body.contains("Checking your browser") ||
            body.contains("Attention Required")

        if (isCloudflare) {
            return fetchHTMLViaWebView(url)
        }
        return body
    }

    // Cloudflare won't clear a challenge for a WebView with no window attached, so
    // this hands off to a Compose dialog (CloudflareChallengeHost) that shows the
    // page to the user in a real, visible WebView and reports the resulting HTML
    // back once the interstitial clears.
    private suspend fun fetchHTMLViaWebView(url: String): String =
        CloudflareChallengeManager.solve(url)

    // MARK: - Public API
    suspend fun fetchLatest(page: Int = 1): List<Manga> {
        val html = fetchHTML("$BASE_URL/manga/?m_orderby=latest&page=$page")
        return parseMangaList(html, extractChapterInfo = true)
    }

    suspend fun fetchPopular(page: Int = 1): List<Manga> {
        val html = fetchHTML("$BASE_URL/manga/?m_orderby=views&page=$page")
        return parseMangaList(html, extractChapterInfo = false)
    }

    suspend fun fetchNewReleases(page: Int = 1): List<Manga> {
        val html = fetchHTML("$BASE_URL/manga/?m_orderby=new-manga&page=$page")
        return parseMangaList(html, extractChapterInfo = true)
    }

    suspend fun search(query: String, page: Int = 1): List<Manga> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = fetchHTML("$BASE_URL/?s=$encoded&post_type=wp-manga&page=$page")
        return parseMangaList(html, extractChapterInfo = false)
            .filter { !it.slug.contains("feed") && it.slug.isNotEmpty() && it.coverURL.isNotEmpty() }
    }

    suspend fun fetchByGenre(genre: String, page: Int = 1): List<Manga> {
        val html = fetchHTML("$BASE_URL/manga-genre/$genre/?page=$page")
        return parseMangaList(html, extractChapterInfo = false)
    }

    suspend fun fetchDetail(slug: String): Manga {
        val html = fetchHTML("$BASE_URL/manga/$slug/")
        return parseMangaDetail(html, slug)
    }

    suspend fun fetchChapterPages(mangaSlug: String, chapterSlug: String): List<String> {
        val url = "$BASE_URL/manga/$mangaSlug/$chapterSlug/"
        val html = fetchHTML(url)

        val chapterIdMatch = Regex("(?:wp-manga-current-chap[^>]+data-id|data-id)=\"(\\d+)\"").find(html)
        if (chapterIdMatch != null) {
            val ajaxPages = fetchChapterImagesViaAjax(chapterIdMatch.groupValues[1])
            if (ajaxPages.isNotEmpty()) return ajaxPages
        }

        val directPages = parseChapterPages(html)
        if (directPages.isNotEmpty()) return directPages

        val finalHtml = fetchChapterHtmlViaWebViewWithWait(url)
        return parseChapterPages(finalHtml)
    }

    // Same hand-off as fetchHTMLViaWebView, but the extraction script first waits
    // for the reader's lazy-loaded page images to populate before grabbing the HTML.
    private suspend fun fetchChapterHtmlViaWebViewWithWait(url: String): String {
        val waitThenExtractJs = """
            (function() {
                return new Promise((resolve) => {
                    let tries = 0;
                    const check = () => {
                        tries++;
                        const imgs = document.querySelectorAll('.reading-content img');
                        const ok = Array.from(imgs).some(img => {
                            const s = img.dataset.lazySrc || img.dataset.src || img.src || '';
                            return s.startsWith('http') && !s.includes('data:image');
                        });
                        if (ok || tries >= 15) resolve(document.documentElement.outerHTML);
                        else setTimeout(check, 200);
                    };
                    setTimeout(check, 500);
                });
            })()
        """.trimIndent()
        return CloudflareChallengeManager.solve(url, waitThenExtractJs)
    }

    private fun fetchChapterImagesViaAjax(chapterId: String): List<String> {
        return try {
            val body = "action=manga_get_chapter_img_list&chapter_id=$chapterId"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/wp-admin/admin-ajax.php")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val text = response.body?.string() ?: return emptyList()

            val gson = com.google.gson.Gson()
            try {
                val asArray = gson.fromJson(text, object : com.google.gson.reflect.TypeToken<Array<Map<String, Any>>>() {}.type) as Array<Map<String, Any>>
                return asArray.mapNotNull { it["url"] as? String }.filter { it.startsWith("http") }
            } catch (_: Exception) { }
            try {
                @Suppress("UNCHECKED_CAST")
                val asObj = gson.fromJson(text, Map::class.java) as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val images = asObj["data"] as? List<Map<String, Any>>
                if (images != null) return images.mapNotNull { it["url"] as? String }.filter { it.startsWith("http") }
            } catch (_: Exception) { }
            if (text.contains("<img")) return parseChapterPages(text)
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - Parsing (regex-based, ported from the iOS parser)

    private fun isLogoOnly(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("lekmanga.png") || lower.contains("-512.png") || lower.contains("/favicon")
    }

    private fun htmlDecode(str: String): String =
        str.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#039;", "'").replace("&nbsp;", " ").trim()

    private fun stripHtml(html: String): String = html.replace(Regex("<[^>]+>"), "").trim()

    private fun extractImageUrl(tags: List<String>): String? {
        for (tag in tags) {
            Regex("data-lazy-src\\s*=\\s*\"([^\"]+)\"").find(tag)?.groupValues?.get(1)?.let { if (it.startsWith("http")) return it }
            Regex("data-src\\s*=\\s*\"([^\"]+)\"").find(tag)?.groupValues?.get(1)?.let { if (it.startsWith("http")) return it }
            Regex("\\bsrc\\s*=\\s*\"([^\"]+)\"").find(tag)?.groupValues?.get(1)?.let { if (it.startsWith("http") && !isLogoOnly(it)) return it }
        }
        return null
    }

    private fun extractTags(tagName: String, html: String): List<String> =
        Regex("<$tagName\\s[^>]*>", RegexOption.DOT_MATCHES_ALL).findAll(html).map { it.value }.toList()

    private fun parseMangaList(html: String, extractChapterInfo: Boolean): List<Manga> {
        val results = mutableListOf<Manga>()
        val cardRegex = Regex(
            "<div class=\"page-item-detail[^\"]*manga[^\"]*\">(.*?)</div>\\s*</div>\\s*</div>",
            RegexOption.DOT_MATCHES_ALL
        )
        for (match in cardRegex.findAll(html).take(30)) {
            val block = match.value
            val manga = parseMangaCard(block) ?: continue
            if (manga.coverURL.isEmpty() || isLogoOnly(manga.coverURL)) continue
            if (extractChapterInfo) {
                val (chapter, time) = parseLatestChapterInfo(block)
                manga.latestChapterNumber = chapter
                manga.lastUpdated = time
            }
            results.add(manga)
        }
        if (results.isEmpty()) return parseMangaSimple(html, extractChapterInfo)
        return results
    }

    private fun parseMangaCard(block: String): Manga? {
        val slug = Regex("href=\"https?://[^/]+/manga/([^/\"]+)/\"").find(block)?.groupValues?.get(1) ?: return null
        if (slug.isEmpty() || slug == "feed") return null
        val title = Regex("<h3[^>]*>\\s*<a[^>]*>([^<]+)</a>").find(block)?.groupValues?.get(1)
            ?: Regex("<h5[^>]*>\\s*<a[^>]*>([^<]+)</a>").find(block)?.groupValues?.get(1)
            ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }
        val cover = extractImageUrl(extractTags("img", block)) ?: ""
        if (isLogoOnly(cover)) return null
        return Manga(slug = slug, title = htmlDecode(title), coverURL = cover)
    }

    private fun parseMangaSimple(html: String, extractChapterInfo: Boolean): List<Manga> {
        val results = mutableListOf<Manga>()
        val pattern = Regex("href=\"(https?://[^/]+/manga/([^/\"]+)/)\"[^>]*>\\s*(?:<[^>]+>\\s*)*([^<]{3,})")
        for (match in pattern.findAll(html)) {
            val slug = match.groupValues[2]
            val rawTitle = match.groupValues[3].trim()
            if (slug.isEmpty() || rawTitle.isEmpty() || rawTitle.length >= 200 || slug == "feed" || slug.contains("cdn-cgi")) continue
            if (results.any { it.slug == slug }) continue
            val start = match.range.first
            val searchWindow = html.substring(start, minOf(html.length, start + 2000))
            val cover = extractImageUrl(extractTags("img", searchWindow)) ?: ""
            val manga = Manga(slug = slug, title = htmlDecode(rawTitle), coverURL = if (isLogoOnly(cover)) "" else cover)
            if (extractChapterInfo) {
                val (chapter, time) = parseLatestChapterInfo(searchWindow)
                manga.latestChapterNumber = chapter
                manga.lastUpdated = time
            }
            results.add(manga)
        }
        return results
    }

    private fun parseLatestChapterInfo(block: String): Pair<String?, String?> {
        val chapter = Regex("<a[^>]+href=\"[^\"]*chapter[^\"]*\"[^>]*>Chapter\\s*([^<]+)</a>").find(block)?.groupValues?.get(1)?.trim()
        val time = Regex("<span[^>]+class=\"[^\"]*font-meta[^\"]*\"[^>]*>([^<]+)</span>").find(block)?.groupValues?.get(1)?.trim()
        return chapter to time
    }

    private fun parseMangaDetail(html: String, slug: String): Manga {
        val title = Regex("<div class=\"post-title\"[^>]*>\\s*<h1[^>]*>\\s*([^<]+)", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
        val summaryBlock = Regex("(<div class=\"summary_image[^\"]*\">.*?</div>)", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: html
        val cover = extractImageUrl(extractTags("img", summaryBlock)) ?: ""

        val description = Regex("<div class=\"summary__content[^\"]*\">(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.let { stripHtml(it).trim() } ?: ""
        val rating = Regex("id=\"averagerate\"[^>]*>([^<]+)<").find(html)?.groupValues?.get(1) ?: ""
        val status = Regex("<div class=\"summary-content\">\\s*(مستمرة|مكتملة|Ongoing|Completed)\\s*</div>")
            .find(html)?.groupValues?.get(1) ?: ""
        val author = Regex("class=\"author-content\">(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.let { stripHtml(it) } ?: ""

        val genres = Regex("/manga-genre/[^/]+/\">([^<]+)</a>").findAll(html).map { it.groupValues[1] }.toList()

        val chapters = mutableListOf<Chapter>()
        val chapterBlockRegex = Regex("<li class=\"wp-manga-chapter[^\"]*\">(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
        for (match in chapterBlockRegex.findAll(html)) {
            val block = match.groupValues[1]
            val fullLink = Regex("href=\"(https?://[^/]+/manga/[^/]+/([^/]+)/)\"").find(block)?.groupValues?.get(1) ?: continue
            val slugPart = fullLink.trimEnd('/').substringAfterLast('/')
            val numberPart = Regex(">(\\d+)</a>").find(block)?.groupValues?.get(1) ?: slugPart
            val date = Regex("class=\"chapter-release-date\"[^>]*>\\s*(?:<[^>]+>)?([^<]+)<").find(block)?.groupValues?.get(1)?.trim() ?: ""
            if (slugPart.isNotEmpty() && chapters.none { it.slug == slugPart }) {
                chapters.add(Chapter(slug = slugPart, number = numberPart, date = date))
            }
        }
        if (chapters.isEmpty()) {
            val altRegex = Regex("href=\"https?://[^/]+/manga/[^/]+/([\\d]+(?:-[\\d]+)?)/\"[^>]*>\\s*(?:<[^>]*>\\s*)*(\\d+)")
            for (match in altRegex.findAll(html)) {
                val s = match.groupValues[1]
                val n = match.groupValues[2]
                if (chapters.none { it.slug == s }) chapters.add(Chapter(slug = s, number = n))
            }
        }
        val sortedChapters = chapters.sortedByDescending { it.number.toIntOrNull() ?: 0 }

        return Manga(
            slug = slug,
            title = htmlDecode(title ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }),
            coverURL = cover, genres = genres, status = status, rating = rating,
            description = description, chapters = sortedChapters, author = author
        )
    }

    private fun extractReadingContent(html: String): String {
        val startMatch = Regex("<div[^>]+class=\"[^\"]*reading-content[^\"]*\"[^>]*>", RegexOption.IGNORE_CASE).find(html)
            ?: return html
        val remaining = html.substring(startMatch.range.last + 1)
        var depth = 1
        var currentIndex = 0
        val divRegex = Regex("</?div", RegexOption.IGNORE_CASE)
        while (currentIndex < remaining.length && depth > 0) {
            val next = divRegex.find(remaining, currentIndex) ?: break
            depth += if (next.value.startsWith("</")) -1 else 1
            currentIndex = next.range.last + 1
        }
        return if (depth == 0) remaining.substring(0, maxOf(0, currentIndex - "</div>".length)) else remaining
    }

    private fun parseChapterPages(html: String): List<String> {
        val content = extractReadingContent(html)
        val seen = mutableSetOf<String>()
        val pages = mutableListOf<String>()
        val imgRegex = Regex("<img\\s[^>]*>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        for (match in imgRegex.findAll(content)) {
            val tag = match.value
            if (!tag.contains("wp-manga-chapter-img") && !tag.contains("data-src") && !tag.contains("data-lazy-src")) continue
            val url = Regex("data-lazy-src=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                ?: Regex("data-src=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                ?: Regex("\\bsrc=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
            val trimmed = url?.trim()
            if (trimmed != null && trimmed.startsWith("http") && !trimmed.contains("data:image") &&
                !isLogoOnly(trimmed) && !seen.contains(trimmed)
            ) {
                seen.add(trimmed)
                pages.add(trimmed)
            }
        }
        return pages
    }
}
