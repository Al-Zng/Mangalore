package com.mangalore.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mangalore.app.R
import com.mangalore.app.data.DownloadManager
import com.mangalore.app.data.Manga
import com.mangalore.app.data.MangaService
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.*
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

enum class HomeTab(val labelKey: String) {
    DOWNLOADS("home.downloads"), POPULAR("home.mostViewed"), LATEST("home.latestUpdates")
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val nav = LocalNavCoordinator.current
    val store = LocalAppStore.current
    val service = remember { MangaService.getInstance(context) }
    val downloadManager = remember { DownloadManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(HomeTab.POPULAR) }

    var latest by remember { mutableStateOf(store.cachedLatest ?: emptyList()) }
    var popular by remember { mutableStateOf(store.cachedPopular ?: emptyList()) }
    var latestPage by remember { mutableStateOf(1) }
    var popularPage by remember { mutableStateOf(1) }
    var isLoadingLatest by remember { mutableStateOf(latest.isEmpty()) }
    var isLoadingPopular by remember { mutableStateOf(popular.isEmpty()) }
    var loadingMoreLatest by remember { mutableStateOf(false) }
    var loadingMorePopular by remember { mutableStateOf(false) }
    var latestEnded by remember { mutableStateOf(false) }
    var popularEnded by remember { mutableStateOf(false) }

    fun mergeUnique(existing: List<Manga>, incoming: List<Manga>): List<Manga> {
        val seen = existing.map { it.slug }.toMutableSet()
        val merged = existing.toMutableList()
        for (m in incoming) if (seen.add(m.slug)) merged.add(m)
        return merged
    }

    suspend fun loadLatest(reset: Boolean) {
        if (reset) { latestPage = 1; latestEnded = false }
        isLoadingLatest = latest.isEmpty() || reset
        try {
            val items = service.fetchLatest(latestPage)
            latest = if (reset || latest.isEmpty()) items else mergeUnique(latest, items)
            if (items.isEmpty()) latestEnded = true
            store.saveCachedLatest(latest)
        } catch (e: Exception) { }
        isLoadingLatest = false
    }

    suspend fun loadMoreLatest() {
        val next = latestPage + 1
        try {
            val items = service.fetchLatest(next)
            latestPage = next
            val before = latest.size
            latest = mergeUnique(latest, items)
            if (items.isEmpty() || latest.size == before) latestEnded = true
            store.saveCachedLatest(latest)
        } catch (e: Exception) { }
        loadingMoreLatest = false
    }

    suspend fun loadPopular(reset: Boolean) {
        if (reset) { popularPage = 1; popularEnded = false }
        isLoadingPopular = popular.isEmpty() || reset
        try {
            val items = service.fetchPopular(popularPage)
            popular = if (reset || popular.isEmpty()) items else mergeUnique(popular, items)
            if (items.isEmpty()) popularEnded = true
            store.saveCachedPopular(popular)
        } catch (e: Exception) { }
        isLoadingPopular = false
    }

    suspend fun loadMorePopular() {
        val next = popularPage + 1
        try {
            val items = service.fetchPopular(next)
            popularPage = next
            val before = popular.size
            popular = mergeUnique(popular, items)
            if (items.isEmpty() || popular.size == before) popularEnded = true
            store.saveCachedPopular(popular)
        } catch (e: Exception) { }
        loadingMorePopular = false
    }

    LaunchedEffect(Unit) {
        if (latest.isEmpty()) loadLatest(false)
        if (popular.isEmpty()) loadPopular(false)
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) { Icon(Icons.Filled.Notifications, contentDescription = null, tint = ZTheme.textSecondary) }
            IconButton(onClick = { nav.go(Routes.SEARCH) }) { Icon(Icons.Filled.Search, contentDescription = null, tint = ZTheme.textSecondary) }
        }

        Row(modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(top = 8.dp)) {
            HomeTab.values().forEach { t ->
                Column(
                    modifier = Modifier.weight(1f).clickable { tab = t }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        L(t.labelKey), color = if (tab == t) ZTheme.textPrimary else ZTheme.textTertiary,
                        style = if (tab == t) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(3.dp).width(40.dp).clip(RoundedCornerShape(2.dp)).background(if (tab == t) ZTheme.accent else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                HomeTab.DOWNLOADS -> DownloadsGrid(downloadManager)
                HomeTab.POPULAR -> InfiniteMangaGrid(
                    items = popular, isLoading = isLoadingPopular, loadingMore = loadingMorePopular,
                    onNearEnd = { if (!loadingMorePopular && !popularEnded) { loadingMorePopular = true; scope.launch { loadMorePopular() } } },
                    header = { ContinueReadingRow() }
                )
                HomeTab.LATEST -> InfiniteMangaGrid(
                    items = latest, isLoading = isLoadingLatest, loadingMore = loadingMoreLatest,
                    onNearEnd = { if (!loadingMoreLatest && !latestEnded) { loadingMoreLatest = true; scope.launch { loadMoreLatest() } } }
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingRow() {
    val store = LocalAppStore.current
    val nav = LocalNavCoordinator.current
    if (store.history.isEmpty()) return

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CONTINUE READING", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            lazyRowItems(store.history.take(10)) { progress ->
                Column(
                    modifier = Modifier.width(116.dp).clickable { nav.go(Routes.detail(progress.mangaSlug)) }
                ) {
                    MangaCoverImage(url = progress.mangaCover, modifier = Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(progress.mangaTitle, color = ZTheme.textPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text("Ch.${progress.chapterNumber}", color = ZTheme.accentBright, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun InfiniteMangaGrid(
    items: List<Manga>,
    isLoading: Boolean,
    loadingMore: Boolean,
    onNearEnd: () -> Unit,
    header: (@Composable () -> Unit)? = null
) {
    val nav = LocalNavCoordinator.current
    if (isLoading && items.isEmpty()) {
        LoadingSpinner()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (header != null) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { header() }
        }
        items.forEachIndexed { index, manga ->
            item(key = manga.slug) {
                MangaGridCard(manga = manga, onClick = { nav.go(Routes.detail(manga.slug)) })
                if (index == items.size - 4) {
                    LaunchedEffect(manga.slug) { onNearEnd() }
                }
            }
        }
        if (loadingMore) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZTheme.accent, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadsGrid(downloadManager: DownloadManager) {
    val nav = LocalNavCoordinator.current
    val groups = downloadManager.groupedByManga()
    if (groups.isEmpty()) {
        EmptyState(icon = Icons.Filled.Download, message = L("home.noDownloads"))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(groups) { group ->
            Column(modifier = Modifier.clickable { nav.go(Routes.detail(group.mangaSlug)) }) {
                MangaCoverImage(url = group.mangaCover, modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4.2f).clip(RoundedCornerShape(12.dp)))
                Spacer(modifier = Modifier.height(6.dp))
                Text(group.mangaTitle, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${group.chapters.size} ch.", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
