package com.mangalore.app.data


data class Chapter(
    val slug: String,
    val number: String,
    val date: String = ""
)

data class Manga(
    val slug: String,
    var title: String,
    var coverURL: String,
    var genres: List<String> = emptyList(),
    var status: String = "",
    var rating: String = "",
    var description: String = "",
    var chapters: List<Chapter> = emptyList(),
    var author: String = "",
    var artist: String = "",
    var latestChapterNumber: String? = null,
    var lastUpdated: String? = null
) {
    val id: String get() = slug

    /** Some cover URLs on the source site are low-res thumbnails; this
     * upgrades common WordPress thumbnail suffixes to the full-size image. */
    val highQualityCoverURL: String
        get() = coverURL
            .replace(Regex("-\\d+x\\d+(?=\\.(jpg|jpeg|png|webp))"), "")
}

data class ReadingProgress(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mangaSlug: String,
    val mangaTitle: String,
    val mangaCover: String,
    val chapterSlug: String,
    val chapterNumber: String,
    val pageIndex: Int,
    val lastRead: Long = System.currentTimeMillis()
)

data class DownloadedChapter(
    val mangaSlug: String,
    val mangaTitle: String,
    val mangaCover: String,
    val chapterSlug: String,
    val chapterNumber: String,
    val localPaths: List<String>
)

data class DownloadedMangaGroup(
    val mangaSlug: String,
    val mangaTitle: String,
    val mangaCover: String,
    val chapters: List<DownloadedChapter>
)

class MangaloreException(message: String) : Exception(message)
