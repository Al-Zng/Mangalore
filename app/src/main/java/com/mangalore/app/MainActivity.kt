package com.mangalore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

// ─────────────────────────────────────────────────────────────
// DESIGN TOKENS — Mangamello palette
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
private val GoldDim  = Color(0xFF7A5D23)
private val Silver   = Color(0xFFB0B8C8)
private val Bronze   = Color(0xFFC8895A)
private val Red      = Color(0xFFE05050)
private val Green    = Color(0xFF4CAF82)

private val ReadexPro = FontFamily(
    Font(R.font.readex_pro_light, FontWeight.Light),
    Font(R.font.readex_pro_regular, FontWeight.Normal),
    Font(R.font.readex_pro_medium, FontWeight.Medium),
    Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    Font(R.font.readex_pro_bold, FontWeight.Bold),
)

// 18 theme accent colors
private val ThemeColors = listOf(
    Color(0xFF5A7DB5), Color(0xFF8B5CB5), Color(0xFFB55C8B),
    Color(0xFFB55C5C), Color(0xFFB58B5C), Color(0xFF8BB55C),
    Color(0xFF5CB58B), Color(0xFF5C8BB5), Color(0xFF6B7FBF),
    Color(0xFFBF6B7F), Color(0xFF7FBF6B), Color(0xFF6BBFBF),
    Color(0xFFBF9F6B), Color(0xFF9F6BBF), Color(0xFFBF6B9F),
    Color(0xFF6BBF9F), Color(0xFF9FBF6B), Color(0xFFBF8B6B),
)

// ─────────────────────────────────────────────────────────────
// DATA
// ─────────────────────────────────────────────────────────────
private data class Manga(
    val title: String,
    val score: Float,
    val chapters: Int,
    val coverColors: List<Color>,
    val genre: List<String> = emptyList(),
    val status: String = "",
    val origin: String = "",
    val views: String = "",
    val description: String = ""
)

private val Mangas = mutableStateListOf<Manga>()

// ─────────────────────────────────────────────────────────────
// APP ENTRY
// ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MangaloreApp() }
    }
}

// ─────────────────────────────────────────────────────────────
// ROOT
// ─────────────────────────────────────────────────────────────
@Composable
private fun MangaloreApp() {
    var themeAccent by remember { mutableStateOf(Accent) }
    var amoled by remember { mutableStateOf(false) }
    val bg = if (amoled) Black else Surface1
    var screen by remember { mutableStateOf("home") }
    var drawer by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Manga?>(null) }
    val catalog = Mangas

    BackHandler(enabled = drawer || picked != null || screen != "home") {
        when {
            drawer -> drawer = false
            picked != null -> picked = null
            else -> screen = "home"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = Surface2, primary = themeAccent)) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = ReadexPro)
        ) {
            Box(Modifier.fillMaxSize().background(bg)) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(280)) + slideInHorizontally { -it / 12 },
                    label = "manga_screen_entrance"
                ) {
                    when {
                        picked != null -> DetailScreen(picked!!, themeAccent, onBack = { picked = null })
                        screen == "search" -> SearchScreen(themeAccent, onBack = { screen = "home" }, onPick = { picked = it })
                        screen == "library" -> LibraryScreen(themeAccent, onBack = { screen = "home" }, onPick = { picked = it })
                        screen == "history" -> HistoryScreen(themeAccent, onBack = { screen = "home" })
                        screen == "profile" -> ProfileScreen(themeAccent, onBack = { screen = "home" })
                        screen == "settings" -> SettingsScreen(themeAccent, amoled, { amoled = it }, { themeAccent = it }) { screen = "home" }
                        else -> HomeScreen(catalog, themeAccent, { drawer = true }, { screen = "search" }) { picked = it }
                    }
                }
                AnimatedVisibility(
                    visible = drawer,
                    enter = fadeIn(tween(180)) + slideInHorizontally { it },
                    exit = fadeOut(tween(140)) + slideOutHorizontally { it },
                    label = "navigation_drawer"
                ) {
                    NavigationDrawer(themeAccent, { drawer = false }) { dest ->
                        drawer = false
                        when (dest) {
                            "home" -> { screen = "home"; picked = null }
                            "search" -> screen = "search"
                            "library" -> screen = "library"
                            "history" -> screen = "history"
                            "profile" -> screen = "profile"
                            "settings" -> screen = "settings"
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────
@Composable
private fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onBack) {
                Icon(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack, "رجوع", tint = TextPri)
            }
        }
        Text(
            title, color = TextPri, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(horizontal = if (onBack != null) 0.dp else 12.dp)
        )
        action?.invoke(this)
    }
}

@Composable
private fun CoverBox(
    manga: Manga,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    showNew: Boolean = true
) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(manga.coverColors, start = Offset.Zero, end = Offset.Infinite))
    ) {
        // subtle texture overlay
        Box(
            Modifier.matchParentSize().background(
                Brush.radialGradient(
                    listOf(Color.White.copy(.04f), Color.Transparent),
                    radius = 300f
                )
            )
        )
        Icon(
            Icons.Default.AutoStories, null,
            tint = Color.White.copy(.28f),
            modifier = Modifier.align(Alignment.Center).size(iconSize)
        )
        if (showNew) {
            Text(
                "NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFFE05A30), RoundedCornerShape(bottomStart = 8.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun GenreChip(label: String, accent: Color) {
    Surface(
        color   = accent.copy(.15f),
        shape   = RoundedCornerShape(20.dp),
        border  = BorderStroke(.5.dp, accent.copy(.35f))
    ) {
        Text(label, color = accent, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(catalog: List<Manga>, accent: Color, onMenu: () -> Unit, onSearch: () -> Unit, onPick: (Manga) -> Unit) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("آخر التحديثات", "الأكثر مشاهدة", "التحميلات")
    val featured = catalog.firstOrNull()

    LazyColumn(Modifier.fillMaxSize()) {
        // ── Top bar
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onMenu) { Icon(Icons.Default.Menu, "القائمة", tint = TextPri) }
                Text("Mangalore", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onSearch) { Icon(Icons.Default.Search, "بحث", tint = TextPri) }
                IconButton({}) {
                    BadgedBox(badge = { Badge(containerColor = accent) { Text("3") } }) {
                        Icon(Icons.Default.Notifications, "الإشعارات", tint = TextPri)
                    }
                }
            }
        }

        // ── Featured hero banner
        item {
            if (featured == null) {
                Text("لا توجد مانجا مضافة", color = TextSec, modifier = Modifier.padding(24.dp))
            } else Box(
                Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(featured.coverColors))
                    .clickable { onPick(featured) }
            ) {
                // gradient overlay for text readability
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.88f)))
                    )
                )
                Icon(
                    Icons.Default.AutoStories, null,
                    tint = Color.White.copy(.18f),
                    modifier = Modifier.align(Alignment.Center).size(100.dp)
                )
                // badge
                Surface(
                    color   = accent,
                    shape   = RoundedCornerShape(bottomEnd = 14.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "مميز", color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                // info at bottom
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    Text(featured.title, color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${featured.score} ★", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("•", color = TextDim)
                        Text("${featured.chapters} فصل", color = TextSec, fontSize = 12.sp)
                        Text("•", color = TextDim)
                        Text(featured.status, color = Green, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Tab chips
        item {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { i, label ->
                    val active = activeTab == i
                    Surface(
                        color   = if (active) accent else Surface3,
                        shape   = RoundedCornerShape(20.dp),
                        border  = if (!active) BorderStroke(.5.dp, Border) else null,
                        modifier = Modifier.weight(1f).clickable { activeTab = i }
                    ) {
                        Text(
                            label, color = if (active) Color.White else TextSec,
                            fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ── Grid
        val displayList = when (activeTab) {
            1 -> catalog.sortedByDescending { it.score }
            2 -> catalog.filter { it.chapters > 100 }
            else -> catalog
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.heightIn(max = 2000.dp) // fix nested scroll
            ) {
                items(displayList) { manga ->
                    MangaCardSmall(manga, accent, onClick = { onPick(manga) })
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun MangaCardSmall(manga: Manga, accent: Color, onClick: () -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "card_press")
    Column(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .animateContentSize()
    ) {
        CoverBox(manga, Modifier.fillMaxWidth().height(180.dp))
        Spacer(Modifier.height(6.dp))
        Text(manga.title, color = TextPri, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${manga.score} ★", color = Gold, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("${manga.chapters}", color = TextSec, fontSize = 11.sp)
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Default.MenuBook, null, tint = TextDim, modifier = Modifier.size(11.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DETAIL SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun DetailScreen(manga: Manga, accent: Color, onBack: () -> Unit) {
    var activeTab by remember { mutableStateOf(0) }
    var inLibrary by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0) }

    LazyColumn(Modifier.fillMaxSize()) {
        // ── Hero cover with gradient overlay
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Box(
                    Modifier.matchParentSize()
                        .background(Brush.linearGradient(manga.coverColors))
                )
                // dim overlay
                Box(
                    Modifier.matchParentSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(.45f), Surface1)))
                )
                Icon(
                    Icons.Default.AutoStories, null,
                    tint = Color.White.copy(.16f),
                    modifier = Modifier.align(Alignment.Center).size(110.dp)
                )
                // Back button
                IconButton(onBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) {
                    Icon(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack, "رجوع", tint = Color.White)
                }

                // Cover card + info
                Row(
                    Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.width(120.dp).height(175.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(manga.coverColors))
                            .border(2.dp, Color.White.copy(.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(.5f), modifier = Modifier.size(56.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(manga.title, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("الفصل ${manga.chapters}  •  ${manga.status}", color = accent, fontSize = 13.sp)
                        Text(manga.origin, color = TextSec, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            manga.genre.take(3).forEach { GenreChip(it, accent) }
                        }
                    }
                }
            }
        }

        // ── Action bar: rating + library + comments
        item {
            Row(
                Modifier.fillMaxWidth().background(Surface2)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User star rating
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        Icon(
                            if (i < userRating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            null,
                            tint = if (i < userRating) Gold else TextDim,
                            modifier = Modifier.size(22.dp).clickable { userRating = i + 1 }
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton({ inLibrary = !inLibrary }) {
                    Icon(
                        if (inLibrary) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        "المكتبة",
                        tint = if (inLibrary) accent else TextSec
                    )
                }
            }
        }

        // ── Community stats
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox("${manga.views}", "مشاهدات", Icons.Default.Visibility, Modifier.weight(1f))
                StatBox("${manga.chapters}", "الفصول", Icons.Default.MenuBook, Modifier.weight(1f))
                StatBox("${manga.score}", "التقييم", Icons.Default.Star, Modifier.weight(1f), Gold)
            }
        }

        // ── Tab bar: التفاصيل / الفصول
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("التفاصيل", "الفصول").forEachIndexed { i, label ->
                    val active = activeTab == i
                    Column(
                        Modifier.weight(1f).clickable { activeTab = i },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            color = if (active) accent else TextSec,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth(.5f).height(2.dp).background(if (active) accent else Color.Transparent, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }

        if (activeTab == 0) {
            // ── Description
            item {
                Text(
                    manga.description,
                    color = TextSec, lineHeight = 24.sp, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // ── Chapters list
            items(minOf(manga.chapters, 20)) { i ->
                val chNum = manga.chapters - i
                ChapterRow(chNum, accent)
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun StatBox(value: String, label: String, icon: ImageVector, modifier: Modifier, iconTint: Color = TextSec) {
    Surface(color = Surface2, shape = RoundedCornerShape(12.dp), modifier = modifier.padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSec, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ChapterRow(number: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("الفصل $number", color = TextPri, fontSize = 15.sp)
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
}

// ─────────────────────────────────────────────────────────────
// READER SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun SearchScreen(accent: Color, onBack: () -> Unit, onPick: (Manga) -> Unit) {
    var query by remember { mutableStateOf("") }
    val categories = listOf("خيال", "أكشن", "رومانسية", "غموض", "إثارة", "كوميديا", "دراما", "فنون قتالية", "خيال علمي")
    var activeCategory by remember { mutableStateOf<String?>(null) }

    val filtered = if (query.isBlank() && activeCategory == null) Mangas
    else Mangas.filter {
        (query.isBlank() || it.title.contains(query, ignoreCase = true)) &&
        (activeCategory == null || it.genre.contains(activeCategory))
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("البحث", onBack)

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSec) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Default.Close, null, tint = TextSec) }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = Border,
                focusedTextColor = TextPri,
                unfocusedTextColor = TextPri,
                cursorColor = accent,
                unfocusedContainerColor = Surface2,
                focusedContainerColor = Surface2
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // Category chips
        Text("التصنيفات", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val active = activeCategory == cat
                Surface(
                    color   = if (active) accent else Surface2,
                    shape   = RoundedCornerShape(20.dp),
                    border  = if (!active) BorderStroke(.5.dp, Border) else null,
                    modifier = Modifier.clickable { activeCategory = if (active) null else cat }
                ) {
                    Text(
                        cat,
                        color  = if (active) Color.White else TextSec,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            if (query.isBlank() && activeCategory == null) "أعمال مقترحة" else "نتائج البحث (${filtered.size})",
            color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(filtered) { manga ->
                MangaCardSmall(manga, accent, onClick = { onPick(manga) })
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = TextDim, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("لا توجد نتائج لـ \"$query\"", color = TextDim, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LIBRARY SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun LibraryScreen(accent: Color, onBack: () -> Unit, onPick: (Manga) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("مكتبتي", onBack)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LibraryBooks, null, tint = TextDim, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text("المكتبة فارغة", color = TextSec, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HISTORY SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun HistoryScreen(accent: Color, onBack: () -> Unit) {
    val items = remember { Mangas.take(6).toMutableStateList() }
    Column(Modifier.fillMaxSize()) {
        TopBar("سجل المشاهدة", onBack, action = {
            TextButton(onClick = { items.clear() }) { Text("مسح الكل", color = Red, fontSize = 13.sp) }
        })
        Text("${items.size} أعمال", color = TextSec, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        HorizontalDivider(Modifier.padding(top = 6.dp), color = Border)
        LazyColumn {
            itemsIndexed(items) { i, manga ->
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CoverBox(manga, Modifier.size(64.dp), iconSize = 28.dp, showNew = false)
                    Column(Modifier.weight(1f)) {
                        Text(manga.title, color = TextPri, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("الفصل ${manga.chapters - i}  •  منذ ساعتين", color = TextSec, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (i + 1) / manga.chapters.toFloat().coerceAtMost(1f) + .35f },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = accent,
                            trackColor = Border
                        )
                    }
                    IconButton(onClick = { items.removeAt(i) }) {
                        Icon(Icons.Default.DeleteOutline, "حذف", tint = TextSec)
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = Border, thickness = .5.dp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileScreen(accent: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("الملف الشخصي", onBack)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("لا يوجد حساب مسجل", color = TextSec, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, icon: ImageVector, modifier: Modifier, iconTint: Color = TextSec) {
    Surface(color = Surface2, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            Text(value, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSec, fontSize = 10.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SETTINGS SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsScreen(
    accent: Color,
    amoled: Boolean,
    onAmoled: (Boolean) -> Unit,
    onAccentPick: (Color) -> Unit,
    onBack: () -> Unit
) {
    var darkMode        by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }
    var listStyle       by remember { mutableStateOf(false) } // false=grid true=list
    var classicDetail   by remember { mutableStateOf(false) }
    var verticalRead    by remember { mutableStateOf(true) }
    var tapNav          by remember { mutableStateOf(true) }
    var volButtons      by remember { mutableStateOf(false) }
    var fullscreen      by remember { mutableStateOf(true) }
    var showPageNum     by remember { mutableStateOf(true) }
    var incognitoComments by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("الإعدادات", onBack) }

        // ── 1. المظهر والعرض
        item { SectionLabel("المظهر والعرض", accent) }
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column {
                    SettingToggle("الوضع الداكن", "الواجهة الداكنة OLED", darkMode) { darkMode = it }
                    Divider2()
                    SettingToggle("وضع AMOLED الأسود", "توفير البطارية — خلفية سوداء حقيقية", amoled, onAmoled)
                    Divider2()
                    SettingAction("لون التطبيق", "اختر من 18 لوناً", Icons.Default.Palette, accent) { showColorPicker = true }
                    Divider2()
                    SettingToggle("نمط القوائم", if (listStyle) "قائمة" else "شبكة", listStyle) { listStyle = it }
                    Divider2()
                    SettingToggle("تصميم صفحة العمل", if (classicDetail) "كلاسيكي" else "حديث", classicDetail) { classicDetail = it }
                }
            }
        }

        // ── 2. تجربة القراءة
        item { SectionLabel("تجربة القراءة", accent) }
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column {
                    SettingToggle("اتجاه القراءة", if (verticalRead) "طولي (scroll)" else "عرضي (swipe)", verticalRead) { verticalRead = it }
                    Divider2()
                    SettingToggle("التنقل بالضغط", "اضغط على جوانب الشاشة للتنقل", tapNav) { tapNav = it }
                    Divider2()
                    SettingToggle("أزرار الصوت للتنقل", "استخدم أزرار الصوت للتصفح", volButtons) { volButtons = it }
                    Divider2()
                    SettingToggle("ملء الشاشة", "إخفاء أشرطة النظام أثناء القراءة", fullscreen) { fullscreen = it }
                    Divider2()
                    SettingToggle("رقم الصفحة", "عرض رقم الصفحة الحالية", showPageNum) { showPageNum = it }
                }
            }
        }

        // ── 3. الخصوصية

        item { SectionLabel("الخصوصية", accent) }
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column {
                    SettingToggle("وضع خفي للتعليقات", "لن يظهر اسمك في التعليقات", incognitoComments) { incognitoComments = it }
                    Divider2()
                    SettingAction("المستخدمون المحظورون", "0 مستخدمين محظورين", Icons.Default.Block, accent) {}
                    Divider2()
                    SettingAction("تصفية المحتوى", "تحذيرات المحتوى الحساس", Icons.Default.VisibilityOff, accent) {}
                }
            }
        }

        // Version info + logo
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent),
                    contentAlignment = Alignment.Center
                ) { Text("M", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) }
                Spacer(Modifier.height(8.dp))
                Text("الإصدار 1.0.0", color = TextDim, fontSize = 12.sp)
            }
        }
    }

    // Color picker overlay
    if (showColorPicker) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(.7f)).clickable { showColorPicker = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                Modifier.padding(24.dp),
                color = Surface2,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("اختر لون التطبيق", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    // 18 colors in 6×3 grid
                    ThemeColors.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { color ->
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape)
                                        .background(color)
                                        .border(if (color == accent) 3.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { onAccentPick(color); showColorPicker = false }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun SectionLabel(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp))
}

@Composable private fun Divider2() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
}

@Composable private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPri, fontSize = 15.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = TextSec, fontSize = 12.sp)
        }
        Switch(checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Accent))
    }
}

@Composable private fun SettingAction(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = TextPri, fontSize = 15.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = TextSec, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint = TextDim)
    }
}

// ─────────────────────────────────────────────────────────────
// NAVIGATION DRAWER
// ─────────────────────────────────────────────────────────────
@Composable
private fun NavigationDrawer(accent: Color, onClose: () -> Unit, onNavigate: (String) -> Unit) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(.65f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        Surface(
            Modifier.fillMaxHeight().width(320.dp).align(if (isRtl) Alignment.CenterStart else Alignment.CenterEnd)
                .pointerInput(Unit) { detectTapGestures { /* absorb taps */ } },
            color = Surface1,
            shadowElevation = 24.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(
                    Modifier.fillMaxWidth().height(180.dp)
                        .background(Brush.verticalGradient(listOf(accent.copy(.3f), Surface1)))
                ) {
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFCB4A0D), Color(0xFFE8803D)))),
                            contentAlignment = Alignment.Center
                        ) { Text("م", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
                        // online dot
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Green).offset(x = 50.dp, y = (-12).dp))
                        Text("الملف الشخصي", color = TextSec, fontSize = 12.sp)
                    }
                }

                LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    item { DrawerSection("القائمة الرئيسية", accent) }
                    item { DrawerItem("الرئيسية",       Icons.Default.Home,           "home",     onNavigate) }
                    item { DrawerItem("البحث",           Icons.Default.Search,         "search",   onNavigate) }
                    item { DrawerItem("أعمال أضيفت مؤخرًا", Icons.Default.NewReleases, "home",   onNavigate) }
                    item { DrawerItem("مكتبتي",         Icons.Default.LibraryBooks,    "library",  onNavigate) }
                    item { DrawerItem("سجل المشاهدة",   Icons.Default.History,         "history",  onNavigate) }
                    item { DrawerItem("الملف الشخصي",   Icons.Default.Person,          "profile",  onNavigate) }
                    item { Spacer(Modifier.height(8.dp)) }
                    item { HorizontalDivider(color = Border) }
                    item { DrawerSection("الإعدادات والمساعدة", accent) }
                    item { DrawerItem("الإعدادات",       Icons.Default.Settings,        "settings", onNavigate) }
                    item { DrawerItem("مركز المساعدة",   Icons.Default.HelpOutline,     "home",     onNavigate) }
                }

                // Footer
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(accent),
                        contentAlignment = Alignment.Center
                    ) { Text("M", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
                    Spacer(Modifier.width(8.dp))
                    Text("مانجاميلو", color = TextSec, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable private fun DrawerSection(label: String, accent: Color) {
    Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
}

@Composable private fun DrawerItem(label: String, icon: ImageVector, dest: String, onNavigate: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onNavigate(dest) }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextPri, modifier = Modifier.size(20.dp))
        Text(label, color = TextPri, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 14.dp))
    }
}
