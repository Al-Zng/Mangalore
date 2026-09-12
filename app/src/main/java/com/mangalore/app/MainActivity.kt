@file:Suppress("UNUSED_EXPRESSION")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

// ─────────────────────────────────────────────────────────────
// DESIGN TOKENS
// ─────────────────────────────────────────────────────────────
private val Black    = Color(0xFF000000)
private val Surface1 = Color(0xFF0D0D0F)
private val Surface2 = Color(0xFF16161A)
private val Surface3 = Color(0xFF1E1E24)
private val Border   = Color(0xFF2A2A32)
private val TextPri  = Color(0xFFF0F0F3)
private val TextSec  = Color(0xFF9494A0)
private val TextDim  = Color(0xFF55555E)
private val Accent   = Color(0xFF5A7DB5)
private val AccentLt = Color(0xFF7FA2D4)
private val Gold     = Color(0xFFFFC44D)
private val Red      = Color(0xFFE05050)
private val Green    = Color(0xFF4CAF82)

private val ThemeColors = listOf(
    Color(0xFF5A7DB5), Color(0xFF8B5CB5), Color(0xFFB55C8B),
    Color(0xFFB55C5C), Color(0xFFB58B5C), Color(0xFF8BB55C),
    Color(0xFF5CB58B), Color(0xFF5C8BB5), Color(0xFF6B7FBF),
    Color(0xFFBF6B7F), Color(0xFF7FBF6B), Color(0xFF6BBFBF),
    Color(0xFFBF9F6B), Color(0xFF9F6BBF), Color(0xFFBF6B9F),
    Color(0xFF6BBF9F), Color(0xFF9FBF6B), Color(0xFFBF8B6B),
)

private val ReadexPro = FontFamily(
    Font(R.font.readex_pro_light,    FontWeight.Light),
    Font(R.font.readex_pro_regular,  FontWeight.Normal),
    Font(R.font.readex_pro_medium,   FontWeight.Medium),
    Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    Font(R.font.readex_pro_bold,     FontWeight.Bold),
)

// ─────────────────────────────────────────────────────────────
// NAVIGATION
// ─────────────────────────────────────────────────────────────
private sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Library : Screen()
    object History : Screen()
    object Profile : Screen()
    object Settings : Screen()
    data class Detail(val item: MangaItem) : Screen()
    data class DetailFull(val detail: MangaDetail) : Screen()
    data class Reader(val url: String, val title: String, val manga: MangaDetail) : Screen()
}

// ─────────────────────────────────────────────────────────────
// ENTRY
// ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MangaloreApp() }
    }
}

// ─────────────────────────────────────────────────────────────
// ROOT COMPOSABLE
// ─────────────────────────────────────────────────────────────
@Composable
private fun MangaloreApp() {
    var accent  by remember { mutableStateOf(Accent) }
    var amoled  by remember { mutableStateOf(false) }
    val bg      = if (amoled) Black else Surface1

    var stack   by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val screen  = stack.last()
    var drawer  by remember { mutableStateOf(false) }

    val library  = remember { mutableStateListOf<MangaItem>() }
    val history  = remember { mutableStateListOf<Pair<MangaItem, String>>() }

    var showCf  by remember { mutableStateOf(true) }
    var cfMsg   by remember { mutableStateOf("") }

    fun push(s: Screen) { stack = stack + s; drawer = false }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = stack.size > 1 || drawer) {
        when { drawer -> drawer = false; stack.size > 1 -> pop() }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(background = bg, surface = Surface2, primary = accent)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = ReadexPro)
        ) {
            Box(Modifier.fillMaxSize().background(bg)) {

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val fwd = stack.size > 1
                        (fadeIn(tween(240)) + slideInHorizontally(
                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                        ) { if (fwd) it / 8 else -it / 8 }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(
                            spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                        ) { if (fwd) -it / 8 else it / 8 })
                    },
                    label = "nav"
                ) { cur ->
                    when (cur) {
                        is Screen.Home -> HomeScreen(accent, { drawer = true }, { push(Screen.Search) }) { push(Screen.Detail(it)) }
                        is Screen.Search -> SearchScreen(accent, ::pop) { push(Screen.Detail(it)) }
                        is Screen.Library -> LibraryScreen(accent, library, ::pop, { push(Screen.Detail(it)) }) { library.remove(it) }
                        is Screen.History -> HistoryScreen(accent, history, ::pop, { push(Screen.Detail(it)) }) { history.clear() }
                        is Screen.Profile -> ProfileScreen(accent, ::pop)
                        is Screen.Settings -> SettingsScreen(accent, amoled, { amoled = it }, { accent = it }, ::pop)
                        is Screen.Detail -> DetailLoadScreen(cur.item, accent, library, ::pop) { push(Screen.DetailFull(it)) }
                        is Screen.DetailFull -> DetailFullScreen(
                            detail = cur.detail, accent = accent, library = library, onBack = ::pop
                        ) { ch ->
                            val asItem = MangaItem(cur.detail.slug, cur.detail.title,
                                cur.detail.slug, cur.detail.coverUrl, cur.detail.url)
                            history.removeAll { it.second == ch.title }
                            history.add(0, asItem to ch.title)
                            push(Screen.Reader(ch.url, ch.title, cur.detail))
                        }
                        is Screen.Reader -> ReaderScreen(
                            cur.url, cur.title, cur.manga.title, accent, ::pop
                        ) { showCf = true }
                    }
                }

                // Drawer
                AnimatedVisibility(
                    visible = drawer,
                    enter = fadeIn(tween(180)) + slideInHorizontally(
                        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                    ) { it },
                    exit  = fadeOut(tween(140)) + slideOutHorizontally(
                        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                    ) { it },
                    label = "drawer"
                ) {
                    NavDrawer(accent, screen, { drawer = false }) { dest ->
                        drawer = false
                        stack = when (dest) {
                            "home"     -> listOf(Screen.Home)
                            "library"  -> listOf(Screen.Home, Screen.Library)
                            "history"  -> listOf(Screen.Home, Screen.History)
                            "profile"  -> listOf(Screen.Home, Screen.Profile)
                            "settings" -> listOf(Screen.Home, Screen.Settings)
                            else       -> stack + Screen.Search
                        }
                    }
                }

                // CF popup
                if (showCf) {
                    CfBypassDialog(
                        onSolved = { c -> CookieStore.cfCookies = c; CookieStore.cfSolved = true
                            showCf = false; cfMsg = "✓ تم حل التحقق بنجاح" },
                        onDismiss = { showCf = false }
                    )
                }

                // Toast
                if (cfMsg.isNotEmpty()) {
                    LaunchedEffect(cfMsg) { kotlinx.coroutines.delay(3500L); cfMsg = "" }
                    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), Alignment.BottomCenter) {
                        Surface(color = Green, shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp) {
                            Text(cfMsg, color = Color.White,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CF BYPASS DIALOG
// ─────────────────────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CfBypassDialog(onSolved: (String) -> Unit, onDismiss: () -> Unit) {
    var pageLoading by remember { mutableStateOf(true) }
    val cfUrl = Scraper.cfChallengeUrl()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Surface1) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Surface(color = Surface2, shadowElevation = 4.dp) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, null, tint = Accent,
                                modifier = Modifier.padding(start = 8.dp).size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تحقق الأمان", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("أكمل التحقق للوصول إلى الفصول", color = TextSec, fontSize = 12.sp)
                            }
                            if (pageLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            TextButton(onDismiss) {
                                Text("تخطي", color = TextSec, fontSize = 13.sp)
                            }
                        }
                        // Info
                        if (pageLoading) {
                            Surface(color = Accent.copy(.1f), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Info, null, tint = AccentLt,
                                        modifier = Modifier.size(16.dp))
                                    Text(
                                        "انتظر قليلاً أو أكمل التحقق يدوياً إذا طُلب منك ذلك",
                                        color = AccentLt, fontSize = 12.sp, lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
                // WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled  = true
                                userAgentString  = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, url: String?, fav: android.graphics.Bitmap?) {
                                    pageLoading = true
                                }
                                override fun onPageFinished(v: WebView?, url: String?) {
                                    pageLoading = false
                                    val t = v?.title ?: ""
                                    val isCf = t.contains("Just a moment", true) ||
                                        t.contains("Attention Required", true) ||
                                        t.contains("Checking your browser", true)
                                    if (!isCf && url?.contains("mangalik.net") == true) {
                                        val c = CookieManager.getInstance().getCookie("https://mangalik.net") ?: ""
                                        if (c.isNotBlank()) onSolved(c)
                                    }
                                }
                                override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                                    val u = r?.url?.toString() ?: return false
                                    return if (u.contains("mangalik.net")) { v?.loadUrl(u); true } else false
                                }
                            }
                            loadUrl(cfUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(
    accent: Color, onMenu: () -> Unit, onSearch: () -> Unit, onPick: (MangaItem) -> Unit
) {
    var tab    by remember { mutableStateOf(0) }
    val scope  = rememberCoroutineScope()
    var latest  by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var popular by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf(false) }

    fun load() { loading = true; error = false
        scope.launch {
            val home = Scraper.fetchHome()
            if (home.isEmpty()) {
                val l = Scraper.fetchLatest(); val p = Scraper.fetchPopular()
                if (l.isEmpty() && p.isEmpty()) { error = true } else { latest = l; popular = p.ifEmpty { l } }
            } else { latest = home; popular = home.sortedByDescending { it.title }.take(30) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    val list = if (tab == 1) popular else latest

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onMenu) { Icon(Icons.Default.Menu, null, tint = TextPri) }
                Column(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    Text("Mangalore", color = TextPri, fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("مانجا · مانهوا · مانهوا", color = TextSec, fontSize = 11.sp)
                }
                IconButton(onSearch) { Icon(Icons.Default.Search, null, tint = TextPri) }
            }
        }

        if (loading) {
            item {
                ShimmerHero()
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(GridCells.Fixed(3),
                    Modifier.heightIn(max = 1600.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(14.dp)) {
                    items(12) { ShimmerCard(Modifier.fillMaxWidth().height(175.dp)) }
                }
            }
            return@LazyColumn
        }
        if (error) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, null, tint = TextDim, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("تعذّر الاتصال", color = TextSec, fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // Featured
        val feat = list.firstOrNull()
        if (feat != null) {
            item {
                Box(Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp)).clickable { onPick(feat) }) {
                    if (feat.coverUrl.isNotEmpty())
                        MImg(feat.coverUrl, Modifier.fillMaxSize())
                    else
                        Box(Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(accent.copy(.7f), accent.copy(.2f)))))
                    Box(Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.9f)))))
                    Surface(color = accent, shape = RoundedCornerShape(bottomEnd = 14.dp),
                        modifier = Modifier.align(Alignment.TopStart)) {
                        Text("⭐ مميز", color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(feat.title, color = TextPri, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (feat.latestChapter.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.MenuBook, null, tint = accent,
                                    modifier = Modifier.size(13.dp))
                                Text(feat.latestChapter, color = TextSec, fontSize = 12.sp)
                                if (feat.chapterDate.isNotEmpty()) {
                                    Text("·", color = TextDim)
                                    Text(feat.chapterDate, color = TextDim, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tabs
        item {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("آخر التحديثات", "الأكثر شعبية").forEachIndexed { i, lbl ->
                    val active = tab == i
                    val a by animateFloatAsState(if (active) 1f else .55f, label = "t")
                    Surface(
                        color  = if (active) accent else Surface3,
                        shape  = RoundedCornerShape(20.dp),
                        border = if (!active) BorderStroke(.5.dp, Border) else null,
                        modifier = Modifier.weight(1f).graphicsLayer { alpha = a }.clickable { tab = i }
                    ) {
                        Text(lbl, color = if (active) Color.White else TextSec,
                            fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp))
                    }
                }
            }
        }

        // Grid
        item {
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 5000.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(18.dp)) {
                items(list.drop(1), key = { it.id }) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp))) {
                                if (m.coverUrl.isNotEmpty()) MImg(m.coverUrl, Modifier.fillMaxSize())
                                else Box(Modifier.fillMaxSize().background(Surface3)) {
                                    Icon(Icons.Default.AutoStories, null, tint = TextDim,
                                        modifier = Modifier.align(Alignment.Center).size(32.dp))
                                }
                                if (m.latestChapter.isNotEmpty()) {
                                    Surface(color = Color.Black.copy(.75f),
                                        shape = RoundedCornerShape(topStart = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd)) {
                                        Text(m.latestChapter, color = Color.White, fontSize = 9.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, color = TextPri, fontSize = 12.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// SEARCH SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun SearchScreen(accent: Color, onBack: () -> Unit, onPick: (MangaItem) -> Unit) {
    var q       by remember { mutableStateOf("") }
    var sub     by remember { mutableStateOf("") }
    var res     by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()
    val cats    = listOf("أكشن","رومانسية","غموض","إثارة","كوميديا","دراما",
        "فنون قتالية","خيال","خيال علمي","تناسخ","سحر","مغامرة","رعب")
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
                    placeholder = { Text("ابحث عن مانجا...", color = TextDim, fontSize = 14.sp) },
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
                        focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                        cursorColor = accent,
                        unfocusedContainerColor = Surface2, focusedContainerColor = Surface2),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { cat ->
                        val sel = activeCat == cat
                        FilterChip(sel, { activeCat = if (sel) null else cat; search(if (sel) "" else cat) },
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
                items(12) { ShimmerCard(Modifier.fillMaxWidth().height(175.dp)) }
            }
        } else if (res.isNotEmpty()) {
            Text("${res.size} نتيجة", color = TextSec, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyVerticalGrid(GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(18.dp)) {
                items(res) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp))) {
                                if (m.coverUrl.isNotEmpty()) MImg(m.coverUrl, Modifier.fillMaxSize())
                                else Box(Modifier.fillMaxSize().background(Surface3))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, color = TextPri, fontSize = 12.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            }
        } else if (sub.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, tint = TextDim, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("لا نتائج لـ «$sub»", color = TextDim, fontSize = 14.sp)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, tint = TextDim.copy(.4f), modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("ابحث عن مانجا أو مانهوا", color = TextDim, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DETAIL LOAD (loading state while fetching MangaDetail)
// ─────────────────────────────────────────────────────────────
@Composable
private fun DetailLoadScreen(
    item: MangaItem, accent: Color, library: MutableList<MangaItem>,
    onBack: () -> Unit, onLoaded: (MangaDetail) -> Unit
) {
    val scope   = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf(false) }

    fun load() { loading = true; error = false
        scope.launch {
            val d = Scraper.fetchDetail(item.url)
            if (d != null) onLoaded(d) else { loading = false; error = true }
        }
    }
    LaunchedEffect(item.url) { load() }

    Box(Modifier.fillMaxSize(), Alignment.TopStart) {
        when {
            loading -> Column(Modifier.fillMaxSize()) {
                ShimmerBox(Modifier.fillMaxWidth().height(280.dp))
                Spacer(Modifier.height(16.dp))
                repeat(6) { ShimmerBox(Modifier.fillMaxWidth(.7f).height(14.dp)
                    .padding(horizontal = 16.dp, vertical = 5.dp)) }
            }
            error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("تعذّر التحميل", color = TextSec)
                    Spacer(Modifier.height(14.dp))
                    Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("إعادة المحاولة", color = Color.White)
                    }
                }
            }
        }
        IconButton(onBack, Modifier.statusBarsPadding().padding(4.dp)) {
            Surface(color = Color.Black.copy(.45f), shape = CircleShape) {
                Icon(Icons.Default.ArrowForward, null, tint = Color.White,
                    modifier = Modifier.padding(6.dp).size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DETAIL FULL
// ─────────────────────────────────────────────────────────────
@Composable
private fun DetailFullScreen(
    detail: MangaDetail, accent: Color, library: MutableList<MangaItem>,
    onBack: () -> Unit, onChapter: (ChapterItem) -> Unit
) {
    var tab       by remember { mutableStateOf(0) }
    val inLib     = library.any { it.url == detail.url }
    val asMItem   = MangaItem(detail.slug, detail.title, detail.slug, detail.coverUrl, detail.url)

    LazyColumn(Modifier.fillMaxSize()) {
        // Hero
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                if (detail.coverUrl.isNotEmpty())
                    MImg(detail.coverUrl, Modifier.fillMaxSize().blur(12.dp))
                Box(Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(.55f), Surface1))))
                IconButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)) {
                    Surface(color = Color.Black.copy(.4f), shape = CircleShape) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White,
                            modifier = Modifier.padding(6.dp).size(20.dp))
                    }
                }
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.width(108.dp).height(158.dp).clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White.copy(.18f), RoundedCornerShape(12.dp))) {
                        if (detail.coverUrl.isNotEmpty()) MImg(detail.coverUrl, Modifier.fillMaxSize())
                        else Box(Modifier.fillMaxSize().background(Surface3)) {
                            Icon(Icons.Default.AutoStories, null, tint = TextDim,
                                modifier = Modifier.align(Alignment.Center).size(36.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(detail.title, color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                        if (detail.author.isNotEmpty()) { Spacer(Modifier.height(4.dp))
                            Text(detail.author, color = TextSec, fontSize = 12.sp) }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            detail.genres.take(3).forEach { GChip(it, accent) }
                            if (detail.status.isNotEmpty()) StatusChip(detail.status, accent)
                        }
                    }
                }
            }
        }
        // Actions
        item {
            Surface(color = Surface2) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (detail.chapters.isNotEmpty()) {
                        Button({ onChapter(detail.chapters.last()) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ابدأ القراءة", fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(
                        { if (inLib) library.removeAll { it.url == detail.url } else library.add(0, asMItem) },
                        border = BorderStroke(1.dp, if (inLib) accent else Border),
                        modifier = Modifier.weight(1f)) {
                        Icon(if (inLib) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null,
                            tint = if (inLib) accent else TextSec, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (inLib) "في المكتبة" else "أضف للمكتبة",
                            color = if (inLib) accent else TextSec, fontSize = 13.sp)
                    }
                }
            }
        }
        // Stats
        if (detail.chapters.isNotEmpty() || detail.rating.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (detail.rating.isNotEmpty()) StatBox("${detail.rating}★", "التقييم", Icons.Default.Star, Modifier.weight(1f), Gold)
                    StatBox("${detail.chapters.size}", "فصل", Icons.Default.MenuBook, Modifier.weight(1f))
                    if (detail.views.isNotEmpty()) StatBox(detail.views, "مشاهدة", Icons.Default.Visibility, Modifier.weight(1f))
                }
            }
        }
        // Tabs
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                listOf("التفاصيل", "الفصول (${detail.chapters.size})").forEachIndexed { i, lbl ->
                    val active = tab == i
                    Column(Modifier.weight(1f).clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(lbl, color = if (active) accent else TextSec, fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(5.dp))
                        val w by animateFloatAsState(if (active) .5f else 0f, label = "tl")
                        Box(Modifier.fillMaxWidth(w).height(2.dp).background(accent, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
        // Tab body
        if (tab == 0) {
            if (detail.description.isNotEmpty()) {
                item { ExpandText(detail.description, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            }
            if (detail.author.isNotEmpty() || detail.status.isNotEmpty() || detail.releaseYear.isNotEmpty()) {
                item {
                    Surface(color = Surface2, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Column {
                            if (detail.author.isNotEmpty())      InfoRow2("المؤلف", detail.author)
                            if (detail.artist.isNotEmpty())      InfoRow2("الرسام", detail.artist)
                            if (detail.origin.isNotEmpty())      InfoRow2("النوع", detail.origin)
                            if (detail.releaseYear.isNotEmpty()) InfoRow2("السنة", detail.releaseYear)
                            if (detail.status.isNotEmpty())      InfoRow2("الحالة", detail.status)
                        }
                    }
                }
            }
            if (detail.genres.size > 3) {
                item {
                    FlowRow(Modifier.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)) {
                        detail.genres.forEach { GChip(it, accent) }
                    }
                }
            }
        } else {
            items(detail.chapters) { ch ->
                Row(Modifier.fillMaxWidth().clickable { onChapter(ch) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Surface3), Alignment.Center) {
                        Text(ch.number, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ch.title.ifEmpty { "الفصل ${ch.number}" }, color = TextPri, fontSize = 14.sp)
                        if (ch.date.isNotEmpty()) Text(ch.date, color = TextDim, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = TextDim, modifier = Modifier.size(18.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = .5.dp, color = Border)
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// READER
// ─────────────────────────────────────────────────────────────
@Composable
private fun ReaderScreen(
    chUrl: String, chTitle: String, mangaTitle: String,
    accent: Color, onBack: () -> Unit, onCfNeeded: () -> Unit
) {
    val scope   = rememberCoroutineScope()
    var imgs    by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf(false) }
    var cfNeed  by remember { mutableStateOf(false) }
    var bars    by remember { mutableStateOf(true) }

    fun load() { loading = true; error = false; cfNeed = false
        scope.launch {
            val (list, needsCf) = Scraper.fetchChapterImages(chUrl)
            when { needsCf -> cfNeed = true; list.isEmpty() -> error = true; else -> imgs = list }
            loading = false
        }
    }
    LaunchedEffect(chUrl) { load() }

    Box(Modifier.fillMaxSize().background(Black)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("جاري تحميل الفصل...", color = TextSec)
                }
            }
            cfNeed -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Shield, null, tint = accent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("مطلوب تحقق الأمان", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("حل تحدي الأمان للوصول إلى الفصل", color = TextSec, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button({ onCfNeeded() }, Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Icon(Icons.Default.Shield, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حل التحقق", color = Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onBack, Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Border)) {
                        Text("رجوع", color = TextSec)
                    }
                }
            }
            error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("تعذّر التحميل", color = TextSec)
                    Spacer(Modifier.height(14.dp))
                    Button(::load, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("إعادة المحاولة", color = Color.White)
                    }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { bars = !bars }
            ) {
                items(imgs) { url ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(url)
                            .addHeader("Referer", "https://mangalik.net/").crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        loading = {
                            Box(Modifier.fillMaxWidth().height(280.dp), Alignment.Center) {
                                CircularProgressIndicator(color = accent.copy(.5f),
                                    modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxWidth().height(100.dp).background(Surface2), Alignment.Center) {
                                Icon(Icons.Default.BrokenImage, null, tint = TextDim, modifier = Modifier.size(32.dp))
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Top bar
        AnimatedVisibility(
            visible = bars && !loading && !cfNeed && !error,
            enter = fadeIn() + slideInVertically { -it },
            exit  = fadeOut() + slideOutVertically { -it },
            label = "reader_top"
        ) {
            Surface(color = Color.Black.copy(.85f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(mangaTitle, color = Color.White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(chTitle, color = Color.White.copy(.7f), fontSize = 12.sp)
                    }
                    if (imgs.isNotEmpty())
                        Text("${imgs.size} ص", color = Color.White.copy(.6f), fontSize = 12.sp,
                            modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        if (loading || error || cfNeed) {
            IconButton(onBack, Modifier.statusBarsPadding().padding(4.dp)) {
                Surface(color = Color.Black.copy(.4f), shape = CircleShape) {
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LIBRARY
// ─────────────────────────────────────────────────────────────
@Composable
private fun LibraryScreen(
    accent: Color, items: List<MangaItem>, onBack: () -> Unit,
    onPick: (MangaItem) -> Unit, onRemove: (MangaItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar("مكتبتي", accent, onBack)
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryBooks, null, tint = TextDim.copy(.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("المكتبة فارغة", color = TextSec, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("أضف المانجا من صفحة التفاصيل", color = TextDim, fontSize = 13.sp)
                }
            }
        } else {
            Text("${items.size} عمل", color = TextSec, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(items, key = { it.id }) { m ->
                    SpringCard({ onPick(m) }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp))) {
                                if (m.coverUrl.isNotEmpty()) MImg(m.coverUrl, Modifier.fillMaxSize())
                                else Box(Modifier.fillMaxSize().background(Surface3))
                                IconButton({ onRemove(m) }, Modifier.align(Alignment.TopEnd).size(30.dp)) {
                                    Surface(color = Color.Black.copy(.6f), shape = CircleShape) {
                                        Icon(Icons.Default.Close, null, tint = Color.White,
                                            modifier = Modifier.padding(5.dp).size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, color = TextPri, fontSize = 12.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HISTORY
// ─────────────────────────────────────────────────────────────
@Composable
private fun HistoryScreen(
    accent: Color, history: List<Pair<MangaItem, String>>,
    onBack: () -> Unit, onPick: (MangaItem) -> Unit, onClear: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar("سجل القراءة", accent, onBack, action = {
            if (history.isNotEmpty()) TextButton(onClear) { Text("مسح الكل", color = Red, fontSize = 13.sp) }
        })
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = TextDim.copy(.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("لا يوجد سجل قراءة", color = TextSec, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("ستظهر أعمالك هنا بعد القراءة", color = TextDim, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn {
                itemsIndexed(history) { _, (m, ch) ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(m) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
                            if (m.coverUrl.isNotEmpty()) MImg(m.coverUrl, Modifier.fillMaxSize())
                            else Box(Modifier.fillMaxSize().background(Surface3))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(m.title, color = TextPri, fontSize = 14.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            Text(ch, color = accent, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronLeft, null, tint = TextDim, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = .5.dp, color = Border)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PROFILE
// ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileScreen(accent: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("الملف الشخصي", accent, onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape)
                    .background(Surface2).border(2.dp, Border, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("قراءة دون حساب", color = TextSec, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("يمكنك الاستمتاع بالقراءة دون تسجيل دخول", color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SETTINGS
// ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsScreen(
    accent: Color, amoled: Boolean, onAmoled: (Boolean) -> Unit,
    onAccent: (Color) -> Unit, onBack: () -> Unit
) {
    var picker  by remember { mutableStateOf(false) }
    var vScroll by remember { mutableStateOf(true) }
    var tapNav  by remember { mutableStateOf(true) }
    var fullscr by remember { mutableStateOf(true) }
    var pageNum by remember { mutableStateOf(true) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("الإعدادات", accent, onBack) }
        item { SectionLabel("المظهر", accent) }
        item {
            SCard {
                SToggle("وضع AMOLED", "خلفية سوداء حقيقية", amoled, onAmoled)
                D2()
                SAction("لون التطبيق", "18 لوناً للاختيار", Icons.Default.Palette, accent) { picker = true }
            }
        }
        item { SectionLabel("القراءة", accent) }
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
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent), Alignment.Center) {
                        Text("M", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Mangalore", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v2.0.0", color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }

    if (picker) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.75f)).clickable { picker = false }, Alignment.Center) {
            Surface(Modifier.padding(24.dp).clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() }) {}, color = Surface2,
                shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("لون التطبيق", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    ThemeColors.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { c ->
                                Box(Modifier.size(46.dp).clip(CircleShape).background(c)
                                    .border(if (c == accent) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onAccent(c); picker = false }, Alignment.Center) {
                                    if (c == accent) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
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

// ─────────────────────────────────────────────────────────────
// NAV DRAWER
// ─────────────────────────────────────────────────────────────
@Composable
private fun NavDrawer(
    accent: Color, current: Screen, onClose: () -> Unit, onNav: (String) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.65f))
        .pointerInput(Unit) { detectTapGestures { onClose() } }) {
        Surface(Modifier.fillMaxHeight().width(296.dp).align(Alignment.CenterStart)
            .pointerInput(Unit) { detectTapGestures { } },
            color = Surface1, shadowElevation = 24.dp) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(148.dp)
                    .background(Brush.verticalGradient(listOf(accent.copy(.2f), Surface1)))) {
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent), Alignment.Center) {
                            Text("M", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Mangalore", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)) {
                    item { DSection("التنقل", accent) }
                    item { DItem("الرئيسية",   Icons.Default.Home,          "home",     current is Screen.Home,     onNav) }
                    item { DItem("البحث",       Icons.Default.Search,        "search",   current is Screen.Search,   onNav) }
                    item { DItem("مكتبتي",      Icons.Default.LibraryBooks,  "library",  current is Screen.Library,  onNav) }
                    item { DItem("سجل القراءة", Icons.Default.History,       "history",  current is Screen.History,  onNav) }
                    item { DItem("الملف الشخصي",Icons.Default.Person,        "profile",  current is Screen.Profile,  onNav) }
                    item { Spacer(Modifier.height(8.dp)); HorizontalDivider(color = Border) }
                    item { DSection("الإعدادات", accent) }
                    item { DItem("الإعدادات",  Icons.Default.Settings,       "settings", current is Screen.Settings, onNav) }
                }
                HorizontalDivider(color = Border)
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("v2.0.0", color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable private fun TopBar(title: String, accent: Color, onBack: (() -> Unit)? = null, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowForward, null, tint = TextPri) }
        Text(title, color = TextPri, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(horizontal = if (onBack != null) 0.dp else 12.dp))
        action?.invoke(this)
    }
}

@Composable private fun MImg(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url)
            .addHeader("Referer","https://mangalik.net/").crossfade(300).build(),
        contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier
    )
}

@Composable private fun SpringCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.955f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "sc")
    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(src, null, onClick = onClick)) { content() }
}

@Composable private fun GChip(label: String, accent: Color) {
    Surface(color = accent.copy(.14f), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(.5.dp, accent.copy(.3f))) {
        Text(label, color = accent, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable private fun StatusChip(status: String, accent: Color) {
    val c = when {
        status.contains("مستمر",ignoreCase=true)||status.contains("Ongoing",ignoreCase=true) -> Green
        status.contains("مكتمل",ignoreCase=true)||status.contains("Completed",ignoreCase=true) -> accent
        else -> TextSec
    }
    Surface(color = c.copy(.14f), shape = RoundedCornerShape(20.dp), border = BorderStroke(.5.dp, c.copy(.3f))) {
        Text(status, color = c, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable private fun StatBox(value: String, label: String, icon: ImageVector, modifier: Modifier, tint: Color = TextSec) {
    Surface(color = Surface2, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSec, fontSize = 11.sp)
        }
    }
}

@Composable private fun InfoRow2(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.SpaceBetween) {
        Text(label, color = TextSec, fontSize = 13.sp)
        Text(value, color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp), textAlign = TextAlign.End)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = .5.dp, color = Border)
}

@Composable private fun ExpandText(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(text, color = TextSec, fontSize = 14.sp, lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis)
        if (text.length > 200)
            TextButton({ expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "أقل ↑" else "المزيد ↓", color = Accent, fontSize = 13.sp)
            }
    }
}

@Composable private fun SectionLabel(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = .5.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp))
}

@Composable private fun D2() { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = .5.dp, color = Border) }

@Composable private fun SCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface2, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) { Column(content = content) }
}

@Composable private fun SToggle(title: String, sub: String, checked: Boolean, on: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPri, fontSize = 15.sp)
            if (sub.isNotBlank()) Text(sub, color = TextSec, fontSize = 12.sp)
        }
        Switch(checked, on, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Accent))
    }
}

@Composable private fun SAction(title: String, sub: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = TextPri, fontSize = 15.sp)
            if (sub.isNotBlank()) Text(sub, color = TextSec, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint = TextDim)
    }
}

@Composable private fun DSection(label: String, accent: Color) {
    Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
}

@Composable private fun DItem(label: String, icon: ImageVector, dest: String, sel: Boolean, onNav: (String) -> Unit) {
    val bg by animateColorAsState(if (sel) Accent.copy(.14f) else Color.Transparent, label = "di")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).clickable { onNav(dest) }
        .padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (sel) Accent else TextPri, modifier = Modifier.size(20.dp))
        Text(label, color = if (sel) Accent else TextPri, fontSize = 15.sp,
            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// SHIMMER
// ─────────────────────────────────────────────────────────────
@Composable private fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "sh")
    val x by t.animateFloat(initialValue = -1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "sx")
    return Brush.linearGradient(
        listOf(Surface3.copy(.4f), Surface3.copy(.85f), Color(0xFF2C2C36), Surface3.copy(.85f), Surface3.copy(.4f)),
        start = Offset(x * 1000f, 0f), end = Offset((x + .5f) * 1200f, 200f)
    )
}

@Composable private fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(shimmerBrush()))
}

@Composable private fun ShimmerCard(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
}

@Composable private fun ShimmerHero() {
    Box(Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 14.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(20.dp)).background(shimmerBrush()))
}
