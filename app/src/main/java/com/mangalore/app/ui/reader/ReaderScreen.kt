package com.mangalore.app.ui.reader

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mangalore.app.data.Chapter
import com.mangalore.app.data.MangaService
import com.mangalore.app.data.ReadingProgress
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val android.content.Context.readerDataStore by preferencesDataStore(name = "reader_settings")

private object ReaderPrefs {
    val TAP_TO_SCROLL = booleanPreferencesKey("tap_to_scroll")
    val ZOOM_ENABLED = booleanPreferencesKey("zoom_enabled")
    val AUTO_LOAD_NEXT = booleanPreferencesKey("auto_load_next")
    val PRELOAD_NEXT = booleanPreferencesKey("preload_next")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val BRIGHTNESS = doublePreferencesKey("brightness")
    val ZOOM_LEVEL = doublePreferencesKey("zoom_level")
}

@Composable
fun ReaderScreen(mangaSlug: String, initialChapterSlug: String) {
    val context = LocalContext.current
    val store = LocalAppStore.current
    val auth = LocalSupabaseAuth.current
    val service = remember { MangaService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val dataStore = context.readerDataStore

    var manga by remember { mutableStateOf(store.mangaCache[mangaSlug]) }
    var currentChapterSlug by remember { mutableStateOf(initialChapterSlug) }
    var allPages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loadedChapters by remember { mutableStateOf(setOf(initialChapterSlug)) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var totalPages by remember { mutableStateOf(0) }
    var showUI by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var loadingNextChapter by remember { mutableStateOf(false) }

    var tapToScrollEnabled by remember { mutableStateOf(false) }
    var zoomEnabled by remember { mutableStateOf(true) }
    var autoLoadNextChapter by remember { mutableStateOf(true) }
    var preloadNextChapter by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(-1.0) }
    var zoomLevel by remember { mutableStateOf(1.0) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val prefs = dataStore.data.first()
        tapToScrollEnabled = prefs[ReaderPrefs.TAP_TO_SCROLL] ?: false
        zoomEnabled = prefs[ReaderPrefs.ZOOM_ENABLED] ?: true
        autoLoadNextChapter = prefs[ReaderPrefs.AUTO_LOAD_NEXT] ?: true
        preloadNextChapter = prefs[ReaderPrefs.PRELOAD_NEXT] ?: false
        keepScreenOn = prefs[ReaderPrefs.KEEP_SCREEN_ON] ?: false
        brightness = prefs[ReaderPrefs.BRIGHTNESS] ?: -1.0
        zoomLevel = prefs[ReaderPrefs.ZOOM_LEVEL] ?: 1.0
    }

    val activity = context as? Activity
    DisposableEffect(keepScreenOn, brightness) {
        val window = activity?.window
        val original = window?.attributes?.screenBrightness
        if (keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (brightness >= 0) {
            val attrs = window?.attributes
            attrs?.screenBrightness = brightness.toFloat()
            window?.attributes = attrs
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val attrs = window?.attributes
            attrs?.screenBrightness = original ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = attrs
        }
    }

    suspend fun persist() {
        dataStore.edit { p ->
            p[ReaderPrefs.TAP_TO_SCROLL] = tapToScrollEnabled
            p[ReaderPrefs.ZOOM_ENABLED] = zoomEnabled
            p[ReaderPrefs.AUTO_LOAD_NEXT] = autoLoadNextChapter
            p[ReaderPrefs.PRELOAD_NEXT] = preloadNextChapter
            p[ReaderPrefs.KEEP_SCREEN_ON] = keepScreenOn
            p[ReaderPrefs.BRIGHTNESS] = brightness
            p[ReaderPrefs.ZOOM_LEVEL] = zoomLevel
        }
    }

    LaunchedEffect(mangaSlug) {
        if (manga == null) {
            try {
                val result = service.fetchDetail(mangaSlug)
                manga = result
                store.cacheManga(result)
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(currentChapterSlug) {
        isLoading = true
        loadingProgress = 0f
        try {
            val pages = service.fetchChapterPages(mangaSlug, currentChapterSlug)
            totalPages = pages.size
            val steps = minOf(pages.size, 24).coerceAtLeast(1)
            for (i in 0 until steps) {
                loadingProgress = (i + 1).toFloat() / steps
                kotlinx.coroutines.delay(25)
            }
            allPages = pages.map { currentChapterSlug to it }
        } catch (e: Exception) { }
        isLoading = false
    }

    fun sortedChapters() = manga?.chapters?.sortedByDescending { it.number.toIntOrNull() ?: 0 } ?: emptyList()

    fun nextChapter(): Chapter? {
        val sorted = sortedChapters()
        val curIdx = sorted.indexOfFirst { it.slug == currentChapterSlug }
        if (curIdx < 0) return null
        return sorted.getOrNull(curIdx + 1)
    }

    suspend fun loadNextChapterPages() {
        val next = nextChapter() ?: return
        if (loadedChapters.contains(next.slug) || loadingNextChapter) return
        loadingNextChapter = true
        try {
            val pages = service.fetchChapterPages(mangaSlug, next.slug)
            allPages = allPages + pages.map { next.slug to it }
            loadedChapters = loadedChapters + next.slug
        } catch (e: Exception) { }
        loadingNextChapter = false
    }

    fun saveProgress(pageIndex: Int, chapterSlug: String) {
        val m = manga ?: return
        val chapter = m.chapters.firstOrNull { it.slug == chapterSlug } ?: return
        store.saveProgress(
            ReadingProgress(
                mangaSlug = mangaSlug, mangaTitle = m.title, mangaCover = m.coverURL,
                chapterSlug = chapterSlug, chapterNumber = chapter.number, pageIndex = pageIndex
            ),
            auth
        )
    }

    LaunchedEffect(listState, allPages) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val page = allPages.getOrNull(index) ?: return@collect
            saveProgress(index, page.first)
            val triggerDistance = if (preloadNextChapter) 10 else 3
            if (autoLoadNextChapter && index >= allPages.size - triggerDistance && !loadingNextChapter) {
                loadNextChapterPages()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        if (isLoading && allPages.isEmpty()) {
            LoadingOverlay(loadingProgress, totalPages)
        } else {
            var scale by remember { mutableStateOf(1f) }
            val transformState = rememberTransformableState { zoomChange, _, _ ->
                if (zoomEnabled) scale = (scale * zoomChange).coerceIn(1f, 4f)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showUI = !showUI })
                    }
            ) {
                items(allPages) { pageEntry ->
                    val url = pageEntry.second
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .addHeader("Referer", "https://lekmanga.site")
                                .build()
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale * zoomLevel.toFloat()
                                scaleY = scale * zoomLevel.toFloat()
                            }
                            .transformable(transformState, enabled = zoomEnabled)
                    )
                }
                if (loadingNextChapter) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ZTheme.accent)
                        }
                    }
                }
                if (!autoLoadNextChapter && nextChapter() != null) {
                    item {
                        val next = nextChapter()!!
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ZTheme.success, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("End of Chapter", color = androidx.compose.ui.graphics.Color.White)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { scope.launch { loadNextChapterPages() } },
                                colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Next: Chapter ${next.number}", color = ZTheme.bg)
                            }
                        }
                    }
                }
            }
        }

        if (showUI) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    manga?.title ?: "", color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clip(CircleShape)
                        .clickable { showSettings = true }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Reader settings", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            tapToScrollEnabled = tapToScrollEnabled, onTapToScrollChange = { tapToScrollEnabled = it; scope.launch { persist() } },
            zoomEnabled = zoomEnabled, onZoomEnabledChange = { zoomEnabled = it; scope.launch { persist() } },
            autoLoadNextChapter = autoLoadNextChapter, onAutoLoadChange = { autoLoadNextChapter = it; scope.launch { persist() } },
            preloadNextChapter = preloadNextChapter, onPreloadChange = { preloadNextChapter = it; scope.launch { persist() } },
            keepScreenOn = keepScreenOn, onKeepScreenOnChange = { keepScreenOn = it; scope.launch { persist() } },
            brightness = brightness, onBrightnessChange = { brightness = it; scope.launch { persist() } },
            zoomLevel = zoomLevel, onZoomLevelChange = { zoomLevel = it; scope.launch { persist() } },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun LoadingOverlay(progress: Float, totalPages: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(16.dp))
        if (totalPages > 0) {
            Text("${(progress * 100).toInt()}%", color = ZTheme.textPrimary, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = progress, color = ZTheme.accent, trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                modifier = Modifier.width(220.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Preparing chapter...", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelMedium)
        } else {
            CircularProgressIndicator(color = ZTheme.accent)
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    tapToScrollEnabled: Boolean, onTapToScrollChange: (Boolean) -> Unit,
    zoomEnabled: Boolean, onZoomEnabledChange: (Boolean) -> Unit,
    autoLoadNextChapter: Boolean, onAutoLoadChange: (Boolean) -> Unit,
    preloadNextChapter: Boolean, onPreloadChange: (Boolean) -> Unit,
    keepScreenOn: Boolean, onKeepScreenOnChange: (Boolean) -> Unit,
    brightness: Double, onBrightnessChange: (Double) -> Unit,
    zoomLevel: Double, onZoomLevelChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ZTheme.bg) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reader Settings", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Brightness4, contentDescription = null, tint = ZTheme.textTertiary, modifier = Modifier.size(16.dp))
                    Slider(
                        value = (if (brightness < 0) 0.5f else brightness.toFloat()),
                        onValueChange = { onBrightnessChange(it.toDouble()) },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = ZTheme.accent, activeTrackColor = ZTheme.accent)
                    )
                    Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = ZTheme.textPrimary, modifier = Modifier.size(18.dp))
                }
                if (brightness >= 0) {
                    TextButton(onClick = { onBrightnessChange(-1.0) }) {
                        Text("Reset to System Brightness", color = ZTheme.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(14.dp))
                    Text("Image Size", color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Text("${(zoomLevel * 100).toInt()}%", color = ZTheme.accentBright, style = MaterialTheme.typography.titleSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("50%", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = zoomLevel.toFloat(), onValueChange = { onZoomLevelChange(it.toDouble()) },
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = ZTheme.accent, activeTrackColor = ZTheme.accent)
                    )
                    Text("250%", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                }
                if (zoomLevel != 1.0) {
                    TextButton(onClick = { onZoomLevelChange(1.0) }) {
                        Text("Reset to 100%", color = ZTheme.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            SettingsToggleRow("Tap to Scroll", "Tap the page to advance instead of toggling the UI", Icons.Filled.TouchApp, tapToScrollEnabled, onTapToScrollChange)
            SettingsToggleRow("Auto-load Next Chapter", "Continue seamlessly into the next chapter", Icons.Filled.SkipNext, autoLoadNextChapter, onAutoLoadChange)
            SettingsToggleRow("Preload Earlier", "Start fetching the next chapter sooner", Icons.Filled.Bolt, preloadNextChapter, onPreloadChange)
            SettingsToggleRow("Pinch to Zoom", "Pinch any page to zoom in further", Icons.Filled.ZoomIn, zoomEnabled, onZoomEnabledChange)
            SettingsToggleRow("Keep Screen On", "Prevent the screen from sleeping while reading", Icons.Filled.BrightnessHigh, keepScreenOn, onKeepScreenOnChange)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(14.dp)).padding(14.dp),
        content = content
    )
}

@Composable
private fun SettingsToggleRow(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(description, color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = ZTheme.accent, checkedTrackColor = ZTheme.accent.copy(alpha = 0.5f)))
    }
}
