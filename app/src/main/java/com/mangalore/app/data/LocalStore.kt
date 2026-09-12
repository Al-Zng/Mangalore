package com.mangalore.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "mangalore_store")

/**
 * Local app state: reading history, library (favorites), downloads cache,
 * and a manga detail cache — equivalent of the iOS `AppStore` class. Also
 * opportunistically syncs to Supabase for signed-in (non-guest) users,
 * exactly like the iOS version does.
 */
class AppStore(private val context: Context) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    var history: List<ReadingProgress> by mutableStateOf(emptyList())
    var library: List<Manga> by mutableStateOf(emptyList())
    var cachedLatest: List<Manga>? by mutableStateOf(null)
    var cachedPopular: List<Manga>? by mutableStateOf(null)
    var mangaCache: MutableMap<String, Manga> = mutableMapOf()
    var newlyUnlockedAchievement: SBAchievement? by mutableStateOf(null)
    var reloadTrigger: Int by mutableStateOf(0)

    private val historyKey = stringPreferencesKey("history")
    private val libraryKey = stringPreferencesKey("library")
    private val mangaCacheKey = stringPreferencesKey("manga_cache")

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            prefs[historyKey]?.let {
                history = gson.fromJson(it, object : TypeToken<List<ReadingProgress>>() {}.type) ?: emptyList()
            }
            prefs[libraryKey]?.let {
                library = gson.fromJson(it, object : TypeToken<List<Manga>>() {}.type) ?: emptyList()
            }
            prefs[mangaCacheKey]?.let {
                val map: Map<String, Manga> = gson.fromJson(it, object : TypeToken<Map<String, Manga>>() {}.type) ?: emptyMap()
                mangaCache = map.toMutableMap()
            }
        }
    }

    fun cacheManga(manga: Manga) {
        mangaCache[manga.slug] = manga
        persistMangaCache()
    }

    fun saveCachedLatest(list: List<Manga>) { cachedLatest = list }
    fun saveCachedPopular(list: List<Manga>) { cachedPopular = list }

    // MARK: - History
    fun saveProgress(progress: ReadingProgress, auth: SupabaseAuth) {
        history = listOf(progress) + history.filter { it.mangaSlug != progress.mangaSlug }
        if (history.size > 200) history = history.take(200)
        persistHistory()
        syncProgressToSupabase(progress, auth)
    }

    fun clearHistory() {
        history = emptyList()
        persistHistory()
    }

    private fun persistHistory() {
        scope.launch {
            context.dataStore.edit { it[historyKey] = gson.toJson(history) }
        }
    }

    private fun syncProgressToSupabase(progress: ReadingProgress, auth: SupabaseAuth) {
        val manga = mangaCache[progress.mangaSlug] ?: return
        scope.launch {
            try {
                if (!auth.isAuthenticated) return@launch
                val row = SupabaseSync.upsertManga(manga, auth)
                SupabaseSync.saveProgress(row.id, progress.chapterSlug, progress.chapterNumber, progress.pageIndex, auth)
                checkForNewAchievements(auth)
            } catch (e: Exception) {
                // best-effort background sync; ignore failures
            }
        }
    }

    private suspend fun checkForNewAchievements(auth: SupabaseAuth) {
        val uid = auth.userId ?: return
        try {
            val unlockedNow = SupabaseSync.fetchUnlockedAchievements(uid, auth)
            val prefs = context.dataStore.data.first()
            val knownKey = stringPreferencesKey("known_achievements_$uid")
            val isFirstSync = prefs[knownKey] == null
            val known = prefs[knownKey]?.let {
                gson.fromJson<Set<String>>(it, object : TypeToken<Set<String>>() {}.type)
            } ?: emptySet()
            val newlyUnlocked = unlockedNow - known
            context.dataStore.edit { it[knownKey] = gson.toJson(unlockedNow) }

            if (isFirstSync) return
            val firstNewId = newlyUnlocked.firstOrNull() ?: return
            val all = SupabaseSync.fetchAllAchievements(auth)
            val achievement = all.firstOrNull { it.id == firstNewId } ?: return
            newlyUnlockedAchievement = achievement
        } catch (e: Exception) {
            // ignore
        }
    }

    // MARK: - Favorites
    fun addToLibrary(manga: Manga, auth: SupabaseAuth) {
        if (library.any { it.slug == manga.slug }) return
        library = listOf(manga) + library
        persistLibrary()
        if (!auth.isAuthenticated) return
        scope.launch {
            try {
                val row = SupabaseSync.upsertManga(manga, auth)
                SupabaseSync.toggleFavorite(row.id, "want_to_read", auth)
            } catch (e: Exception) { }
        }
    }

    fun removeFromLibrary(manga: Manga, auth: SupabaseAuth) {
        library = library.filter { it.slug != manga.slug }
        persistLibrary()
        if (!auth.isAuthenticated) return
        scope.launch {
            try {
                val row = SupabaseSync.upsertManga(manga, auth)
                SupabaseSync.removeFavorite(row.id, auth)
            } catch (e: Exception) { }
        }
    }

    fun isInLibrary(manga: Manga): Boolean = library.any { it.slug == manga.slug }

    private fun persistLibrary() {
        scope.launch {
            context.dataStore.edit { it[libraryKey] = gson.toJson(library) }
        }
    }

    private fun persistMangaCache() {
        scope.launch {
            context.dataStore.edit { it[mangaCacheKey] = gson.toJson(mangaCache) }
        }
    }

    companion object {
        @Volatile private var instance: AppStore? = null
        fun getInstance(context: Context): AppStore =
            instance ?: synchronized(this) {
                instance ?: AppStore(context.applicationContext).also { instance = it }
            }
    }
}

/** Local download manager — equivalent of the iOS `DownloadManager`. */
class DownloadManager(private val context: Context) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val downloadsKey = stringPreferencesKey("downloads")

    var downloads: Map<String, DownloadedChapter> by mutableStateOf(emptyMap())
    var activeDownloads: Map<String, Float> by mutableStateOf(emptyMap())

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            prefs[downloadsKey]?.let {
                downloads = gson.fromJson(it, object : TypeToken<Map<String, DownloadedChapter>>() {}.type) ?: emptyMap()
            }
        }
    }

    fun isDownloaded(mangaSlug: String, chapterSlug: String): Boolean =
        downloads.containsKey("$mangaSlug/$chapterSlug")

    fun isDownloading(mangaSlug: String, chapterSlug: String): Boolean =
        activeDownloads.containsKey("$mangaSlug/$chapterSlug")

    fun saveDownloadedChapter(chapter: DownloadedChapter) {
        downloads = downloads + ("${chapter.mangaSlug}/${chapter.chapterSlug}" to chapter)
        persist()
    }

    fun deleteChapter(mangaSlug: String, chapterSlug: String) {
        downloads = downloads - "$mangaSlug/$chapterSlug"
        persist()
    }

    fun groupedByManga(): List<DownloadedMangaGroup> =
        downloads.values.groupBy { it.mangaSlug }.map { (slug, chapters) ->
            DownloadedMangaGroup(slug, chapters.first().mangaTitle, chapters.first().mangaCover, chapters)
        }.sortedBy { it.mangaTitle }

    private fun persist() {
        scope.launch {
            context.dataStore.edit { it[downloadsKey] = gson.toJson(downloads) }
        }
    }

    companion object {
        @Volatile private var instance: DownloadManager? = null
        fun getInstance(context: Context): DownloadManager =
            instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
    }
}
