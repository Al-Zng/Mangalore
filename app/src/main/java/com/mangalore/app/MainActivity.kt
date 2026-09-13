package com.mangalore.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════
// TOKENS
// ══════════════════════════════════════════════════════════════
private val Black    = Color(0xFF000000)
private val Bg       = Color(0xFF0D0D0F)
private val Surface2 = Color(0xFF16161A)
private val Surface3 = Color(0xFF1E1E24)
private val Border   = Color(0xFF2A2A32)
private val TextPri  = Color(0xFFF0F0F3)
private val TextSec  = Color(0xFF9494A0)
private val TextDim  = Color(0xFF4E4E58)
private val Accent   = Color(0xFF5A7DB5)
private val AccentLt = Color(0xFF7FA2D4)
private val Gold     = Color(0xFFFFC44D)
private val Red      = Color(0xFFE05050)
private val Green    = Color(0xFF4CAF82)

private val Palette = listOf(
    Color(0xFF5A7DB5), Color(0xFF8B5CB5), Color(0xFFB55C8B), Color(0xFFB55C5C),
    Color(0xFFB58B5C), Color(0xFF8BB55C), Color(0xFF5CB58B), Color(0xFF5C8BB5),
    Color(0xFF6B7FBF), Color(0xFFBF6B7F), Color(0xFF7FBF6B), Color(0xFF6BBFBF),
    Color(0xFFBF9F6B), Color(0xFF9F6BBF), Color(0xFFBF6B9F), Color(0xFF6BBF9F),
    Color(0xFF9FBF6B), Color(0xFFBF8B6B),
)

private val Font = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.readex_pro_light,    FontWeight.Light),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_regular,  FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_medium,   FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_bold,     FontWeight.Bold),
)

// ══════════════════════════════════════════════════════════════
// NAV
// ══════════════════════════════════════════════════════════════
private sealed class Dest {
    object Home : Dest()
    object Search : Dest()
    object Library : Dest()
    object History : Dest()
    object Profile : Dest()
    object Settings : Dest()
    data class Detail(val item: MangaItem) : Dest()
    data class DetailFull(val d: MangaDetail) : Dest()
    data class Reader(val url: String, val chTitle: String, val manga: MangaDetail) : Dest()
}

// ══════════════════════════════════════════════════════════════
// ENTRY
// ══════════════════════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

// ══════════════════════════════════════════════════════════════
// APP ROOT
// ══════════════════════════════════════════════════════════════
@Composable
private fun App() {
    var accent  by remember { mutableStateOf(Accent) }
    var amoled  by remember { mutableStateOf(false) }
    val appBg   = if (amoled) Black else Bg

    var stack   by remember { mutableStateOf(listOf<Dest>(Dest.Home)) }
    val cur     = stack.last()
    var drawer  by remember { mutableStateOf(false) }

    val lib     = remember { mutableStateListOf<MangaItem>() }
    val hist    = remember { mutableStateListOf<Triple<MangaItem, String, String>>() } // item, chNum, chTitle

    var showCf  by remember { mutableStateOf(true) }
    var toast   by remember { mutableStateOf("") }

    fun push(d: Dest) { stack = stack + d; drawer = false }
    fun root(d: Dest) { stack = listOf(d); drawer = false }
    fun replaceTop(d: Dest) { stack = if (stack.size > 1) stack.dropLast(1) + d else listOf(d) }
    fun pop()  { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home() { stack = listOf(Dest.Home); drawer = false }

    BackHandler(stack.size > 1 || drawer) { if (drawer) drawer = false else pop() }

    MaterialTheme(colorScheme = darkColorScheme(background = appBg, surface = Surface2, primary = accent)) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Font)
        ) {
            Box(Modifier.fillMaxSize().background(appBg)) {

                // ── Screens ───────────────────────────────────
                AnimatedContent(
                    modifier = Modifier.fillMaxSize().padding(
                        bottom = if (cur !is Dest.Reader && cur !is Dest.Detail && cur !is Dest.DetailFull) 80.dp else 0.dp
                    ),
                    targetState = cur,
                    transitionSpec = {
                        val fwd = stack.size > 1
                        (fadeIn(tween(220)) + slideInHorizontally(
                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                        ) { if (fwd) it / 10 else -it / 10 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(
                            spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                        ) { if (fwd) -it / 10 else it / 10 })
                    },
                    label = "screen"
                ) { d ->
                    when (d) {
                        is Dest.Home -> HomeScreen(accent, { drawer = true }, { push(Dest.Search) }) { push(Dest.Detail(it)) }
                        is Dest.Search -> SearchScreen(accent, ::pop) { push(Dest.Detail(it)) }
                        is Dest.Library -> LibraryScreen(accent, lib, ::pop, { push(Dest.Detail(it)) }) { lib.remove(it) }
                        is Dest.History -> HistoryScreen(accent, hist, ::pop, { push(Dest.Detail(it.first)) }) { hist.clear() }
                        is Dest.Profile -> ProfileScreen(accent, ::pop)
                        is Dest.Settings -> SettingsScreen(accent, amoled, { amoled = it }, { accent = it }, ::pop)
                        is Dest.Detail -> DetailLoadingScreen(d.item, accent, lib, ::pop) { replaceTop(Dest.DetailFull(it)) }
                        is Dest.DetailFull -> DetailScreen(
                            d.d, accent, lib, ::pop,
                            onChapter = { ch ->
                                val asItem = MangaItem(d.d.slug, d.d.title, d.d.slug, d.d.coverUrl, d.d.coverFull, d.d.url)
                                hist.removeAll { it.third == ch.url }
                                hist.add(0, Triple(asItem, ch.number, ch.url))
                                push(Dest.Reader(ch.url, ch.title.ifEmpty { "الفصل ${ch.number}" }, d.d))
                            }
                        )
                        is Dest.Reader -> ReaderScreen(d.url, d.chTitle, d.manga.title, accent, ::pop) { showCf = true }
                    }
                }

                // ── Drawer ────────────────────────────────────
                AnimatedVisibility(drawer,
                    enter = fadeIn(tween(180)) + slideInHorizontally(
                        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) { it },
                    exit  = fadeOut(tween(140)) + slideOutHorizontally(
                        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { it },
                    label = "drawer"
                ) {
                    Drawer(accent, cur, { drawer = false }) { dest ->
                        when (dest) {
                            "home"     -> home()
                            "search"   -> push(Dest.Search)
                            "library"  -> { if (cur !is Dest.Library)  push(Dest.Library) else drawer = false }
                            "history"  -> { if (cur !is Dest.History)  push(Dest.History) else drawer = false }
                            "profile"  -> { if (cur !is Dest.Profile)  push(Dest.Profile) else drawer = false }
                            "settings" -> { if (cur !is Dest.Settings) push(Dest.Settings) else drawer = false }
                        }
                    }
                }

                // ── Fixed bottom navigation ───────────────────
                if (cur !is Dest.Reader && cur !is Dest.Detail && cur !is Dest.DetailFull) {
                    Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                        BottomNav(cur, accent, ::root)
                    }
                }

                // ── CF Popup ──────────────────────────────────
                if (showCf) {
                    CfDialog(
                        onSolved = { c -> CookieStore.cfCookies = c; CookieStore.cfSolved = true
                            showCf = false; toast = "✓ تم التحقق بنجاح" },
                        onSkip   = { showCf = false }
                    )
                }

                // ── Toast ─────────────────────────────────────
                if (toast.isNotEmpty()) {
                    LaunchedEffect(toast) { kotlinx.coroutines.delay(3000L); toast = "" }
                    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), Alignment.BottomCenter) {
                        Surface(color = Green, shape = RoundedCornerShape(14.dp), shadowElevation = 8.dp) {
                            Text(toast, color = Color.White, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// CF BYPASS DIALOG
// ══════════════════════════════════════════════════════════════
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CfDialog(onSolved: (String) -> Unit, onSkip: () -> Unit) {
    var loading by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(onSkip,
        androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {

                // Header
                Surface(color = Surface2, shadowElevation = 4.dp) {
                    Column {
                        Row(Modifier.fillMaxWidth().statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {

                            Box(Modifier.size(40.dp).clip(CircleShape).background(Accent.copy(.15f)),
                                Alignment.Center) {
                                Icon(Icons.Default.Security, null, tint = Accent, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("التحقق من الأمان", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("أكمل التحقق للوصول إلى الفصول", color = TextSec, fontSize = 12.sp)
                            }
                            if (loading) CircularProgressIndicator(
                                Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onSkip) { Text("تخطي", color = TextSec, fontSize = 13.sp) }
                        }

                        AnimatedVisibility(loading) {
                            Row(Modifier.fillMaxWidth().background(Accent.copy(.08f))
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Info, null, tint = AccentLt, modifier = Modifier.size(15.dp))
                                Text("انتظر قليلاً أو حلّ التحقق يدوياً إذا ظهر لك",
                                    color = AccentLt, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }

                // WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true; domStorageEnabled = true
                                databaseEnabled = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                setSupportZoom(true); builtInZoomControls = true; displayZoomControls = false
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, u: String?, f: android.graphics.Bitmap?) { loading = true }
                                override fun onPageFinished(v: WebView?, url: String?) {
                                    loading = false
                                    val t = v?.title ?: ""
                                    val isCfPage = listOf("Just a moment", "Attention Required", "Checking your browser")
                                        .any { t.contains(it, true) }
                                    if (!isCfPage && url?.contains("mangalik.net") == true) {
                                        val c = CookieManager.getInstance().getCookie("https://mangalik.net") ?: ""
                                        if (c.isNotBlank()) onSolved(c)
                                    }
                                }
                                override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                                    val u = r?.url?.toString() ?: return false
                                    return if (u.contains("mangalik.net")) { v?.loadUrl(u); true } else false
                                }
                            }
                            loadUrl(Scraper.cfChallengeUrl())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HOME SCREEN
// ══════════════════════════════════════════════════════════════
@Composable
private fun HomeScreen(accent: Color, onMenu: () -> Unit, onSearch: () -> Unit, onPick: (MangaItem) -> Unit) {
    var tab     by remember { mutableStateOf(0) }
    val scope   = rememberCoroutineScope()
    var latest  by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var popular by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf(false) }

    fun load() { loading = true; error = false
        scope.launch {
            val home = Scraper.fetchHome()
            if (home.isNotEmpty()) {
                latest = home
                popular = home.sortedByDescending { it.score }
            } else {
                val l = Scraper.fetchLatest(); val p = Scraper.fetchPopular()
                if (l.isEmpty() && p.isEmpty()) error = true
                else { latest = l; popular = p.ifEmpty { l } }
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    val list    = if (tab == 1) popular else latest
    val feature = list.firstOrNull()

    LazyColumn(Modifier.fillMaxSize()) {

        // ── App bar ───────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onMenu) { Icon(Icons.Default.Menu, null, tint = TextPri) }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text("Mangalore", color = TextPri, fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("مانجا · مانهوا · كوميك", color = TextSec, fontSize = 10.sp)
                }
                IconButton(onSearch) { Icon(Icons.Default.Search, null, tint = TextPri) }
            }
        }

        // ── Shimmer or Error ──────────────────────────────────
        if (loading) {
            item {
                // Hero shimmer
                Box(Modifier.fillMaxWidth().height(252.dp).padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp)).background(shimmer()))
                Spacer(Modifier.height(14.dp).fillMaxWidth())
                // Tab shimmer
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(20.dp)).background(shimmer()))
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(20.dp)).background(shimmer()))
                }
                Spacer(Modifier.height(14.dp).fillMaxWidth())
                // Grid shimmer
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 1600.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(14.dp)) {
                    items(12) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp)).background(shimmer()))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth(.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmer()))
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth(.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer()))
                        }
                    }
                }
            }
            return@LazyColumn
        }

        if (error) {
            item {
                Box(Modifier.fillMaxWidth().height(400.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, null, tint = TextDim, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("لا يوجد اتصال بالإنترنت", color = TextSec, fontSize = 15.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // ══ HERO BANNER ═══════════════════════════════════════
        if (feature != null) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(252.dp)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onPick(feature) }
                ) {
                    // Full-bleed background image (blurred)
                    if (feature.coverUrl.isNotEmpty()) {
                        Img(
                            feature.coverFull.ifEmpty { feature.coverUrl },
                            Modifier.fillMaxSize().blur(18.dp),
                            ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(accent.copy(.5f), Surface3))))
                    }

                    // Bottom-to-top fade: transparent → app background
                    Box(Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0f   to Color.Black.copy(.15f),
                            .35f to Color.Black.copy(.40f),
                            1f   to Bg
                        )
                    ))

                    // ──── Inner content row ────────────────────
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // RIGHT side (RTL first = right) — Cover thumbnail card
                        Box(
                            Modifier
                                .width(102.dp).height(148.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shadow(12.dp, RoundedCornerShape(14.dp))
                                .border(1.5.dp, Color.White.copy(.15f), RoundedCornerShape(14.dp))
                        ) {
                            Img(
                                feature.coverFull.ifEmpty { feature.coverUrl },
                                Modifier.fillMaxSize()
                            )
                        }

                        // LEFT side — title + genres + button
                        Column(
                            Modifier.weight(1f).padding(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Genre chips
                            if (feature.genres.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    feature.genres.take(2).forEach { GChip(it, accent) }
                                }
                            }

                            // Title
                            Text(
                                feature.title,
                                color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(Color.Black.copy(.8f), Offset(0f, 2f), blurRadius = 4f)
                                )
                            )

                            // Latest chapter tag
                            if (feature.latestChapter.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.MenuBook, null, tint = accent,
                                        modifier = Modifier.size(12.dp))
                                    Text(feature.latestChapter, color = TextSec, fontSize = 11.sp)
                                }
                            }

                            // Read button
                            Button(
                                onClick = { onPick(feature) },
                                colors  = ButtonDefaults.buttonColors(containerColor = accent),
                                shape   = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("ابدأ القراءة", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }

                    // "Featured" badge top-right (RTL = top start = physically top right)
                    Surface(
                        color   = Color.Black.copy(.55f),
                        shape   = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("⭐ مميز", color = Color.White, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }

        // ── Tabs ──────────────────────────────────────────────
        item {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("آخر التحديثات", "الأكثر شعبية").forEachIndexed { i, lbl ->
                    val sel = tab == i
                    val a by animateFloatAsState(if (sel) 1f else .5f, label = "ta")
                    Surface(
                        color  = if (sel) accent else Surface3,
                        shape  = RoundedCornerShape(20.dp),
                        border = if (!sel) BorderStroke(.5.dp, Border) else null,
                        modifier = Modifier.weight(1f).graphicsLayer { alpha = a }.clickable { tab = i }
                    ) {
                        Text(lbl, color = if (sel) Color.White else TextSec,
                            fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 9.dp))
                    }
                }
            }
        }

        // ── Latest chapter quick row (horizontal) ─────────────
        if (list.size > 1) {
            item {
                Column {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("تحديثات سريعة", color = TextSec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list.take(10).drop(1), key = { "qs-${it.id}" }) { m ->
                            SpringCard({ onPick(m) }) {
                                Column(Modifier.width(90.dp)) {
                                    Box(Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))) {
                                        Img(m.coverUrl, Modifier.fillMaxSize())
                                        if (m.latestChapter.isNotEmpty()) {
                                            Surface(color = Color.Black.copy(.7f), shape = RoundedCornerShape(topStart=7.dp),
                                                modifier = Modifier.align(Alignment.BottomEnd)) {
                                                Text(m.latestChapter, color=Color.White, fontSize=8.sp,
                                                    maxLines=1, modifier=Modifier.padding(horizontal=5.dp, vertical=2.dp))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(m.title, color=TextSec, fontSize=10.sp, maxLines=2,
                                        overflow=TextOverflow.Ellipsis, lineHeight=13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Section label
        item {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("جميع الأعمال", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${list.size} عمل", color = TextDim, fontSize = 11.sp)
            }
        }

        // ── Main Grid ─────────────────────────────────────────
        item {
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 9000.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp)) {
                items(list.drop(1), key = { it.id }) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))) {
                                Img(m.coverUrl, Modifier.fillMaxSize())
                                // Chapter badge
                                if (m.latestChapter.isNotEmpty()) {
                                    Surface(color = Color.Black.copy(.72f), shape = RoundedCornerShape(topStart = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd)) {
                                        Text(m.latestChapter, color = Color.White, fontSize = 9.sp, maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                                // Score badge
                                if (m.score.isNotEmpty() && m.score != "0") {
                                    Surface(color = Color.Black.copy(.65f), shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)) {
                                        Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(9.dp))
                                            Text(m.score, color = Gold, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, color = TextPri, fontSize = 11.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                            if (m.chapterDate.isNotEmpty()) {
                                Text(m.chapterDate, color = TextDim, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════
// SEARCH
// ══════════════════════════════════════════════════════════════
@Composable
private fun SearchScreen(accent: Color, onBack: () -> Unit, onPick: (MangaItem) -> Unit) {
    var q       by remember { mutableStateOf("") }
    var sub     by remember { mutableStateOf("") }
    var res     by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()
    val cats    = listOf("أكشن","رومانسية","غموض","إثارة","كوميديا","دراما","فنون قتالية",
        "خيال","خيال علمي","تناسخ","سحر","مغامرة","رعب","ترقي","نظام")
    var activeCat by remember { mutableStateOf<String?>(null) }

    fun search(query: String) {
        if (query.isBlank()) return
        sub = query; loading = true
        scope.launch { res = Scraper.search(query); loading = false }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = Surface2, shadowElevation = 2.dp) {
            Column {
                TopBar("البحث", accent, onBack)
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    placeholder = { Text("ابحث عن مانجا أو مانهوا...", color = TextDim, fontSize = 13.sp) },
                    leadingIcon = {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, null, tint = TextSec)
                    },
                    trailingIcon = {
                        if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Default.Close, null, tint = TextSec) }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { search(q) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPri, unfocusedTextColor = TextPri, cursorColor = accent,
                        unfocusedContainerColor = Surface2, focusedContainerColor = Surface2),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(cats) { cat ->
                        val sel = activeCat == cat
                        FilterChip(sel,
                            { activeCat = if (sel) null else cat
                              if (!sel) search(cat) else { res = emptyList(); sub = "" } },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent, selectedLabelColor = Color.White,
                                containerColor = Surface3, labelColor = TextSec),
                            border = FilterChipDefaults.filterChipBorder(true, sel, Border, Color.Transparent))
                    }
                }
            }
        }

        if (loading) {
            LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(14.dp)) {
                items(12) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(shimmer()))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(.8f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(shimmer()))
                    }
                }
            }
        } else if (res.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("نتائج البحث", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${res.size} عمل", color = TextDim, fontSize = 12.sp)
            }
            LazyVerticalGrid(GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp)) {
                items(res, key = { it.id }) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))) {
                                Img(m.coverUrl, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, color = TextPri, fontSize = 11.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                        }
                    }
                }
            }
        } else if (sub.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, tint = TextDim, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("لا نتائج لـ «$sub»", color = TextDim, fontSize = 14.sp)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, tint = TextDim.copy(.4f), modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("ابحث عن مانجا أو مانهوا", color = TextDim, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// DETAIL – LOADING STATE
// ══════════════════════════════════════════════════════════════
@Composable
private fun DetailLoadingScreen(
    item: MangaItem, accent: Color, lib: MutableList<MangaItem>,
    onBack: () -> Unit, onLoaded: (MangaDetail) -> Unit
) {
    val scope = rememberCoroutineScope()
    var err   by remember { mutableStateOf(false) }

    fun load() { err = false
        scope.launch {
            val d = Scraper.fetchDetail(item.url)
            if (d != null) onLoaded(d) else err = true
        }
    }
    LaunchedEffect(item.url) { load() }

    Box(Modifier.fillMaxSize()) {
        if (!err) {
            // Shimmer placeholder that mimics detail layout
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(280.dp).background(shimmer()))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(.7f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(shimmer()))
                    Box(Modifier.fillMaxWidth(.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer()))
                    Spacer(Modifier.height(6.dp))
                    repeat(5) { Box(Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(4.dp)).background(shimmer())) }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("تعذّر التحميل", color = TextSec, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("إعادة المحاولة", color = Color.White)
                    }
                }
            }
        }
        // Back always visible
        BackBtn(onBack, Modifier.statusBarsPadding().padding(4.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// DETAIL FULL
// ══════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailScreen(
    d: MangaDetail, accent: Color, lib: MutableList<MangaItem>,
    onBack: () -> Unit, onChapter: (ChapterItem) -> Unit
) {
    var tab   by remember { mutableStateOf(0) }
    val inLib = lib.any { it.url == d.url }
    val asItem = MangaItem(d.slug, d.title, d.slug, d.coverUrl, d.coverFull, d.url)

    LazyColumn(Modifier.fillMaxSize()) {

        // ── Hero: blurred background + cover + info ───────────
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                // Blurred bg
                if (d.coverFull.isNotEmpty() || d.coverUrl.isNotEmpty()) {
                    Img(d.coverFull.ifEmpty { d.coverUrl }, Modifier.fillMaxSize().blur(16.dp),
                        ContentScale.Crop)
                }
                // Gradient overlay
                Box(Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(.5f), .6f to Color.Black.copy(.7f), 1f to Bg)))

                // Back button
                BackBtn(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp))

                // Cover + info at bottom
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Cover (right in RTL = first)
                    Box(Modifier.width(108.dp).height(158.dp).clip(RoundedCornerShape(12.dp))
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color.White.copy(.15f), RoundedCornerShape(12.dp))) {
                        Img(d.coverFull.ifEmpty { d.coverUrl }, Modifier.fillMaxSize())
                    }

                    // Info (left in RTL = second)
                    Column(Modifier.weight(1f).padding(bottom = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.title, color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(Color.Black, Offset(0f,1f), blurRadius = 4f)))
                        if (d.author.isNotEmpty())
                            Text(d.author, color = TextSec, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            d.genres.take(3).forEach { GChip(it, accent) }
                            if (d.status.isNotEmpty()) StatChip(d.status, accent)
                        }
                    }
                }
            }
        }

        // ── Actions bar ───────────────────────────────────────
        item {
            Surface(color = Surface2) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (d.chapters.isNotEmpty()) {
                        Button({ onChapter(d.chapters.last()) }, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ابدأ القراءة", fontSize = 13.sp)
                        }
                        if (d.chapters.size > 1) {
                            Button({ onChapter(d.chapters.first()) }, Modifier.wrapContentWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Surface3),
                                shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.LastPage, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("آخر فصل", fontSize = 13.sp, color = TextPri)
                            }
                        }
                    }
                    OutlinedButton(
                        { if (inLib) lib.removeAll { it.url == d.url } else lib.add(0, asItem) },
                        border = BorderStroke(1.dp, if (inLib) accent else Border),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.wrapContentWidth()) {
                        Icon(if (inLib) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            null, tint = if (inLib) accent else TextSec, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Stats ─────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (d.rating.isNotEmpty() && d.rating != "0")
                    StatBox("${d.rating}★","التقييم", Icons.Default.Star, Modifier.weight(1f), Gold)
                StatBox("${d.chapters.size}","فصل", Icons.Default.MenuBook, Modifier.weight(1f))
                if (d.releaseYear.isNotEmpty())
                    StatBox(d.releaseYear,"السنة", Icons.Default.CalendarMonth, Modifier.weight(1f))
            }
        }

        // ── Tabs ──────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                listOf("التفاصيل","الفصول (${d.chapters.size})").forEachIndexed { i, lbl ->
                    val active = tab == i
                    Column(Modifier.weight(1f).clickable { tab = i },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(lbl, color = if (active) accent else TextSec, fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(6.dp))
                        val w by animateFloatAsState(if (active) .5f else 0f, label = "tl")
                        Box(Modifier.fillMaxWidth(w).height(2.dp).background(accent, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }

        // ── Tab body ──────────────────────────────────────────
        if (tab == 0) {
            if (d.description.isNotEmpty()) {
                item { ExpandText(d.description, Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
            }
            item {
                Surface(color = Surface2, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Column {
                        if (d.author.isNotEmpty())      IRow("المؤلف", d.author)
                        if (d.artist.isNotEmpty())      IRow("الرسام", d.artist)
                        if (d.origin.isNotEmpty())      IRow("النوع", d.origin)
                        if (d.releaseYear.isNotEmpty()) IRow("السنة", d.releaseYear)
                        if (d.status.isNotEmpty())      IRow("الحالة", d.status, last = true)
                    }
                }
            }
            if (d.genres.size > 3) {
                item {
                    FlowRow(Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)) {
                        d.genres.forEach { GChip(it, accent) }
                    }
                }
            }
        } else {
            items(d.chapters) { ch ->
                Row(Modifier.fillMaxWidth().clickable { onChapter(ch) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Surface3),
                        Alignment.Center) {
                        Text(ch.number, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ch.title.ifEmpty { "الفصل ${ch.number}" }, color = TextPri, fontSize = 14.sp)
                        if (ch.date.isNotEmpty()) Text(ch.date, color = TextDim, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = TextDim, modifier = Modifier.size(17.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Border, thickness = .5.dp)
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════
// READER
// ══════════════════════════════════════════════════════════════
@Composable
private fun ReaderScreen(
    chUrl: String, chTitle: String, mangaTitle: String,
    accent: Color, onBack: () -> Unit, onCfNeeded: () -> Unit
) {
    val scope  = rememberCoroutineScope()
    var imgs   by remember { mutableStateOf<List<String>>(emptyList()) }
    var state  by remember { mutableStateOf(0) } // 0=loading,1=ok,2=cf,3=err
    var bars   by remember { mutableStateOf(true) }

    fun load() { state = 0; scope.launch {
        val (list, cf) = Scraper.fetchChapterImages(chUrl)
        state = when { cf -> 2; list.isEmpty() -> 3; else -> 1 }
        imgs = list
    }}
    LaunchedEffect(chUrl) { load() }

    Box(Modifier.fillMaxSize().background(Black)) {
        when (state) {
            0 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("جاري تحميل الفصل...", color = TextSec)
                }
            }
            2 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Security, null, tint = accent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("مطلوب تحقق الأمان", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("حل تحدي الأمان للوصول إلى الفصل", color = TextSec, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button({ onCfNeeded() }, Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Security, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حل التحقق", color = Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onBack, Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Border), shape = RoundedCornerShape(12.dp)) {
                        Text("رجوع", color = TextSec)
                    }
                }
            }
            3 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("تعذّر تحميل الفصل", color = TextSec)
                    Spacer(Modifier.height(16.dp))
                    Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("إعادة المحاولة", color = Color.White)
                    }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().clickable(indication=null,
                    interactionSource=remember{MutableInteractionSource()}) { bars=!bars }
            ) {
                items(imgs) { url ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(url)
                            .addHeader("Referer","https://mangalik.net/").crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        loading = {
                            Box(Modifier.fillMaxWidth().height(270.dp), Alignment.Center) {
                                CircularProgressIndicator(color = accent.copy(.5f), modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxWidth().height(90.dp).background(Surface2), Alignment.Center) {
                                Icon(Icons.Default.BrokenImage, null, tint=TextDim, modifier=Modifier.size(30.dp))
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Top bar
        AnimatedVisibility(bars && state==1,
            enter=fadeIn()+slideInVertically{-it}, exit=fadeOut()+slideOutVertically{-it}, label="rb") {
            Surface(color = Color.Black.copy(.88f), modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.statusBarsPadding().padding(horizontal=4.dp, vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onBack) { Icon(Icons.Default.ArrowForward, null, tint=Color.White) }
                    Column(Modifier.weight(1f).padding(horizontal=4.dp)) {
                        Text(mangaTitle, color=Color.White, fontSize=14.sp, fontWeight=FontWeight.Bold,
                            maxLines=1, overflow=TextOverflow.Ellipsis)
                        Text(chTitle, color=Color.White.copy(.7f), fontSize=12.sp)
                    }
                    if (imgs.isNotEmpty())
                        Text("${imgs.size} ص", color=Color.White.copy(.6f), fontSize=12.sp,
                            modifier=Modifier.padding(end=12.dp))
                }
            }
        }
        if (state != 1) BackBtn(onBack, Modifier.statusBarsPadding().padding(4.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// LIBRARY
// ══════════════════════════════════════════════════════════════
@Composable
private fun LibraryScreen(accent:Color, items:List<MangaItem>, onBack:()->Unit,
    onPick:(MangaItem)->Unit, onRemove:(MangaItem)->Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("مكتبتي", accent, onBack)
        if (items.isEmpty()) {
            EmptyState(Icons.Default.LibraryBooks, "المكتبة فارغة", "أضف المانجا من صفحة التفاصيل")
        } else {
            Text("${items.size} عمل", color=TextSec, fontSize=12.sp,
                modifier=Modifier.padding(horizontal=14.dp, vertical=8.dp))
            LazyVerticalGrid(GridCells.Fixed(3),
                contentPadding=PaddingValues(horizontal=14.dp),
                horizontalArrangement=Arrangement.spacedBy(10.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)) {
                items(items, key={it.id}) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))) {
                                Img(m.coverUrl, Modifier.fillMaxSize())
                                IconButton({ onRemove(m) }, Modifier.align(Alignment.TopEnd).size(28.dp)) {
                                    Surface(color = Color.Black.copy(.6f), shape = CircleShape) {
                                        Icon(Icons.Default.Close, null, tint=Color.White,
                                            modifier=Modifier.padding(5.dp).size(12.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(m.title, color=TextPri, fontSize=11.sp, maxLines=2,
                                overflow=TextOverflow.Ellipsis, lineHeight=15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HISTORY
// ══════════════════════════════════════════════════════════════
@Composable
private fun HistoryScreen(accent:Color, hist:List<Triple<MangaItem,String,String>>,
    onBack:()->Unit, onPick:(Triple<MangaItem,String,String>)->Unit, onClear:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("سجل القراءة", accent, onBack, action = {
            if (hist.isNotEmpty()) TextButton(onClear) { Text("مسح الكل", color=Red, fontSize=13.sp) }
        })
        if (hist.isEmpty()) {
            EmptyState(Icons.Default.History, "لا يوجد سجل قراءة", "ستظهر أعمالك هنا بعد القراءة")
        } else {
            LazyColumn {
                itemsIndexed(hist) { _, h ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(h) }.padding(14.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(62.dp).clip(RoundedCornerShape(10.dp))) {
                            Img(h.first.coverUrl, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(h.first.title, color=TextPri, fontSize=14.sp, maxLines=1,
                                overflow=TextOverflow.Ellipsis, fontWeight=FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            Text("الفصل ${h.second}", color=accent, fontSize=12.sp)
                        }
                        Icon(Icons.Default.ChevronLeft, null, tint=TextDim, modifier=Modifier.size(17.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal=14.dp), color=Border, thickness = .5.dp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// PROFILE
// ══════════════════════════════════════════════════════════════
@Composable
private fun ProfileScreen(accent: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("الملف الشخصي", accent, onBack)
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Surface2).border(2.dp,Border,CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint=TextDim, modifier=Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("قراءة دون حساب", color=TextSec, fontSize=15.sp, fontWeight=FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("يمكنك الاستمتاع بالقراءة دون تسجيل دخول", color=TextDim, fontSize=13.sp, textAlign=TextAlign.Center)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SETTINGS
// ══════════════════════════════════════════════════════════════
@Composable
private fun SettingsScreen(accent:Color, amoled:Boolean, onAmoled:(Boolean)->Unit, onAccent:(Color)->Unit, onBack:()->Unit) {
    var picker   by remember { mutableStateOf(false) }
    var vScroll  by remember { mutableStateOf(true) }
    var tapNav   by remember { mutableStateOf(true) }
    var fullscr  by remember { mutableStateOf(true) }
    var pageNum  by remember { mutableStateOf(true) }
    var imgQual  by remember { mutableStateOf(true) } // full-res images

    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("الإعدادات", accent, onBack) }

        item { SecLabel("المظهر", accent) }
        item {
            SCard {
                SToggle("وضع AMOLED", "خلفية سوداء لتوفير الطاقة", amoled, onAmoled)
                D2()
                SAction("لون التطبيق", "18 لوناً للاختيار", Icons.Default.Palette, accent) { picker = true }
            }
        }

        item { SecLabel("جودة الصور", accent) }
        item {
            SCard {
                SToggle("صور عالية الجودة", "تحميل الصور بأعلى دقة متاحة", imgQual) { imgQual = it }
                D2()
                SAction("مسح ذاكرة التخزين المؤقت", "تحرير مساحة التخزين", Icons.Default.DeleteSweep, accent) {}
            }
        }

        item { SecLabel("القراءة", accent) }
        item {
            SCard {
                SToggle("تمرير عمودي", "من أعلى لأسفل", vScroll) { vScroll = it }
                D2()
                SToggle("التنقل بالضغط", "اضغط جوانب الشاشة للتنقل", tapNav) { tapNav = it }
                D2()
                SToggle("ملء الشاشة", "إخفاء أشرطة النظام", fullscr) { fullscr = it }
                D2()
                SToggle("رقم الصفحة", "عرض رقم الصفحة الحالية", pageNum) { pageNum = it }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent), Alignment.Center) {
                        Text("M", color=Color.White, fontWeight=FontWeight.ExtraBold, fontSize=28.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Mangalore", color=TextPri, fontSize=16.sp, fontWeight=FontWeight.Bold)
                    Text("v2.0.0", color=TextDim, fontSize=12.sp)
                }
            }
        }
    }

    if (picker) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.78f)).clickable { picker=false }, Alignment.Center) {
            Surface(modifier = Modifier.padding(24.dp), color = Surface2, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("لون التطبيق", color=TextPri, fontSize=18.sp, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Palette.chunked(6).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            row.forEach { c ->
                                Box(Modifier.size(46.dp).clip(CircleShape).background(c)
                                    .border(if(c==accent) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onAccent(c); picker=false }, Alignment.Center) {
                                    if(c==accent) Icon(Icons.Default.Check, null, tint=Color.White, modifier=Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// DRAWER
// ══════════════════════════════════════════════════════════════
@Composable
private fun BottomNav(cur: Dest, accent: Color, onNavigate: (Dest) -> Unit) {
    val selected = when (cur) {
        is Dest.Search -> 1
        is Dest.Library -> 2
        is Dest.History -> 3
        else -> 0
    }
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Surface2,
        tonalElevation = 12.dp
    ) {
        val items = listOf(
            Triple("الرئيسية", Icons.Default.Home, Dest.Home),
            Triple("البحث", Icons.Default.Search, Dest.Search),
            Triple("مكتبتي", Icons.Default.LibraryBooks, Dest.Library),
            Triple("السجل", Icons.Default.History, Dest.History)
        )
        items.forEachIndexed { index, (label, icon, destination) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { if (selected != index) onNavigate(destination) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = accent.copy(alpha = .14f),
                    unselectedIconColor = TextSec,
                    unselectedTextColor = TextSec
                )
            )
        }
    }
}

@Composable
private fun Drawer(accent:Color, cur:Dest, onClose:()->Unit, onNav:(String)->Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.65f)).pointerInput(Unit){detectTapGestures{onClose()}}) {
        Surface(Modifier.fillMaxHeight().width(296.dp).align(Alignment.CenterStart).pointerInput(Unit){detectTapGestures{}},
            color=Bg, shadowElevation=24.dp) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(Modifier.fillMaxWidth().height(150.dp)
                    .background(Brush.verticalGradient(listOf(accent.copy(.22f), Bg)))) {
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(accent), Alignment.Center) {
                            Text("M", color=Color.White, fontSize=26.sp, fontWeight=FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Mangalore", color=TextPri, fontSize=16.sp, fontWeight=FontWeight.Bold)
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal=8.dp, vertical=8.dp)) {
                    item { DSec("التنقل", accent) }
                    item { DItem("الرئيسية",    Icons.Default.Home,          "home",     cur is Dest.Home,     onNav) }
                    item { DItem("البحث",        Icons.Default.Search,        "search",   cur is Dest.Search,   onNav) }
                    item { DItem("مكتبتي",       Icons.Default.LibraryBooks,  "library",  cur is Dest.Library,  onNav) }
                    item { DItem("سجل القراءة",  Icons.Default.History,       "history",  cur is Dest.History,  onNav) }
                    item { DItem("الملف الشخصي", Icons.Default.Person,        "profile",  cur is Dest.Profile,  onNav) }
                    item { Spacer(Modifier.height(8.dp)); HorizontalDivider(color=Border) }
                    item { DSec("أخرى", accent) }
                    item { DItem("الإعدادات",    Icons.Default.Settings,      "settings", cur is Dest.Settings, onNav) }
                }
                HorizontalDivider(color=Border)
                Row(Modifier.padding(horizontal=20.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint=TextDim, modifier=Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("v2.0.0", color=TextDim, fontSize=12.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ══════════════════════════════════════════════════════════════

@Composable
private fun TopBar(title:String, accent:Color, onBack:(()->Unit)?=null, action:(@Composable RowScope.()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=8.dp, vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
        if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowForward, null, tint=TextPri) }
        Text(title, color=TextPri, fontSize=22.sp, fontWeight=FontWeight.Bold,
            modifier=Modifier.weight(1f).padding(horizontal=if(onBack!=null)0.dp else 12.dp))
        action?.invoke(this)
    }
}

@Composable
private fun BackBtn(onClick:()->Unit, modifier:Modifier=Modifier) {
    IconButton(onClick, modifier) {
        Surface(color=Color.Black.copy(.45f), shape=CircleShape) {
            Icon(Icons.Default.ArrowForward, null, tint=Color.White,
                modifier=Modifier.padding(6.dp).size(20.dp))
        }
    }
}

@Composable
private fun Img(url:String, modifier:Modifier=Modifier, scale:ContentScale=ContentScale.Crop) {
    if (url.isEmpty()) {
        Box(modifier.background(Surface3))
        return
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url)
            .addHeader("Referer","https://mangalik.net/")
            .addHeader("User-Agent","Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0")
            .crossfade(300).build(),
        contentDescription = null, contentScale = scale, modifier = modifier
    )
}

@Composable
private fun SpringCard(onClick:()->Unit, content:@Composable ()->Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) 0.956f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label="sc")
    Box(Modifier.graphicsLayer{scaleX=scale;scaleY=scale}.clickable(src,null,onClick=onClick)) { content() }
}

@Composable private fun GChip(label:String, accent:Color) {
    Surface(color=accent.copy(.14f), shape=RoundedCornerShape(20.dp), border=BorderStroke(.5.dp,accent.copy(.3f))) {
        Text(label, color=accent, fontSize=11.sp, modifier=Modifier.padding(horizontal=10.dp, vertical=4.dp))
    }
}

@Composable private fun StatChip(status:String, accent:Color) {
    val c = when {
        status.contains("مستمر",true)||status.contains("Ongoing",true) -> Green
        status.contains("مكتمل",true)||status.contains("Completed",true) -> accent
        else -> TextSec
    }
    Surface(color=c.copy(.13f), shape=RoundedCornerShape(20.dp), border=BorderStroke(.5.dp,c.copy(.28f))) {
        Text(status, color=c, fontSize=11.sp, modifier=Modifier.padding(horizontal=10.dp, vertical=4.dp))
    }
}

@Composable private fun StatBox(value:String, label:String, icon:ImageVector, modifier:Modifier, tint:Color=TextSec) {
    Surface(color=Surface2, shape=RoundedCornerShape(12.dp), modifier=modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon, null, tint=tint, modifier=Modifier.size(17.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color=TextPri, fontSize=15.sp, fontWeight=FontWeight.Bold)
            Text(label, color=TextSec, fontSize=11.sp)
        }
    }
}

@Composable private fun IRow(label:String, value:String, last:Boolean=false) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=11.dp), Arrangement.SpaceBetween) {
        Text(label, color=TextSec, fontSize=13.sp)
        Text(value, color=TextPri, fontSize=13.sp, fontWeight=FontWeight.Medium,
            modifier=Modifier.padding(start=12.dp), textAlign=TextAlign.End)
    }
    if (!last) HorizontalDivider(modifier = Modifier.padding(horizontal=16.dp), color=Border, thickness = .5.dp)
}

@Composable private fun ExpandText(text:String, modifier:Modifier=Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(text, color=TextSec, fontSize=14.sp, lineHeight=22.sp,
            maxLines=if(expanded) Int.MAX_VALUE else 4,
            overflow=if(expanded) TextOverflow.Clip else TextOverflow.Ellipsis)
        if (text.length > 200) TextButton({expanded=!expanded}, contentPadding=PaddingValues(0.dp)) {
            Text(if(expanded) "أقل ↑" else "المزيد ↓", color=Accent, fontSize=13.sp)
        }
    }
}

@Composable private fun EmptyState(icon:ImageVector, title:String, sub:String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon, null, tint=TextDim.copy(.35f), modifier=Modifier.size(64.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color=TextSec, fontSize=16.sp, fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color=TextDim, fontSize=13.sp, textAlign=TextAlign.Center)
        }
    }
}

@Composable private fun SecLabel(text:String, accent:Color) {
    Text(text, color=accent, fontSize=12.sp, fontWeight=FontWeight.SemiBold, letterSpacing=.5.sp,
        modifier=Modifier.padding(start=20.dp, top=20.dp, bottom=4.dp))
}

@Composable private fun D2() { HorizontalDivider(modifier = Modifier.padding(horizontal=16.dp), color=Border, thickness = .5.dp) }

@Composable private fun SCard(content:@Composable ColumnScope.()->Unit) {
    Surface(color=Surface2, shape=RoundedCornerShape(14.dp),
        modifier=Modifier.padding(horizontal=14.dp, vertical=4.dp)) { Column(content=content) }
}

@Composable private fun SToggle(title:String, sub:String, checked:Boolean, on:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=13.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color=TextPri, fontSize=15.sp)
            if (sub.isNotBlank()) Text(sub, color=TextSec, fontSize=12.sp)
        }
        Switch(checked, on, colors=SwitchDefaults.colors(checkedThumbColor=Color.White, checkedTrackColor=Accent))
    }
}

@Composable private fun SAction(title:String, sub:String, icon:ImageVector, accent:Color, onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp, vertical=13.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(icon, null, tint=accent, modifier=Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
            Text(title, color=TextPri, fontSize=15.sp)
            if (sub.isNotBlank()) Text(sub, color=TextSec, fontSize=12.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint=TextDim)
    }
}

@Composable private fun DSec(label:String, accent:Color) {
    Text(label, color=accent, fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=1.sp,
        modifier=Modifier.padding(horizontal=12.dp, vertical=10.dp))
}

@Composable private fun DItem(label:String, icon:ImageVector, dest:String, sel:Boolean, onNav:(String)->Unit) {
    val bg by animateColorAsState(if(sel) Accent.copy(.13f) else Color.Transparent, label="di")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).clickable{onNav(dest)}
        .padding(horizontal=12.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(icon, null, tint=if(sel) Accent else TextPri, modifier=Modifier.size(20.dp))
        Text(label, color=if(sel) Accent else TextPri, fontSize=15.sp,
            fontWeight=if(sel) FontWeight.SemiBold else FontWeight.Normal,
            modifier=Modifier.padding(horizontal=14.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// SHIMMER
// ══════════════════════════════════════════════════════════════
@Composable private fun shimmer(): Brush {
    val t = rememberInfiniteTransition(label="sh")
    val x by t.animateFloat(-1f, 1.6f, infiniteRepeatable(tween(1100, easing=LinearEasing)), label="sx")
    return Brush.linearGradient(
        listOf(Surface3.copy(.4f), Color(0xFF252530), Surface3.copy(.85f), Color(0xFF252530), Surface3.copy(.4f)),
        Offset(x*1100f, 0f), Offset((x+.55f)*1300f, 220f))
}
