package com.mangalore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
    val genre: List<String> = listOf("أكشن", "إثارة"),
    val status: String = "مستمر",
    val origin: String = "مانهوا كورية",
    val views: String = "283K",
    val description: String = "تدور أحداث هذه القصة في عالم مليء بالأسرار والمواجهات المصيرية. تابع أحدث الفصول واستمتع بتجربة قراءة مريحة ومخصصة بالكامل."
)

private val Mangas = listOf(
    Manga("Back to Spring", 9.28f, 82,  listOf(Color(0xFF9ACDDD), Color(0xFF18405A)), listOf("رومانسية", "خيال")),
    Manga("I Became the Daughter of a Million-Dollar Actor", 8.82f, 64, listOf(Color(0xFFE8A7B7), Color(0xFF3A1C30)), listOf("دراما", "رومانسية")),
    Manga("Bad Born Blood", 9.02f, 99, listOf(Color(0xFF152F4A), Color(0xFFB34035)), listOf("أكشن", "غموض")),
    Manga("Revenge of the Iron-Blooded Sword Hound", 9.11f, 78, listOf(Color(0xFF5B1E29), Color(0xFFE8A04B)), listOf("أكشن", "خيال")),
    Manga("Nano Machine", 9.40f, 245, listOf(Color(0xFF152B38), Color(0xFF7BBDD0)), listOf("أكشن", "خيال علمي")),
    Manga("Magic Emperor", 9.16f, 620, listOf(Color(0xFF372066), Color(0xFFAC4FDC)), listOf("خيال", "سحر")),
    Manga("Murim's Youngest Miracle", 9.43f, 87, listOf(Color(0xFF4A291E), Color(0xFFF1B76E)), listOf("فنون قتالية")),
    Manga("Becoming a Legendary Ace Employee", 9.12f, 56, listOf(Color(0xFF244D92), Color(0xFFB2D3ED)), listOf("كوميديا", "دراما")),
    Manga("Solo Leveling", 9.55f, 202, listOf(Color(0xFF161B3B), Color(0xFF6E54C8)), listOf("أكشن", "خيال")),
    Manga("Logging 10,000 Years into the Future", 8.98f, 135, listOf(Color(0xFF173A4A), Color(0xFF52A5B8)), listOf("خيال علمي")),
    Manga("Death Is the Only Ending for the Villainess", 9.31f, 145, listOf(Color(0xFF8D3155), Color(0xFFEDB4BD)), listOf("رومانسية", "دراما")),
    Manga("I Became the Tyrant's Time-Limited Wife", 8.87f, 71, listOf(Color(0xFF4D2948), Color(0xFFDB789B)), listOf("رومانسية", "إثارة")),
)

private data class Comment(
    val user: String, val text: String, val likes: Int,
    val rank: Int // 1=ذهبي 2=فضي 3=برونزي 0=عادي
)
private val SampleComments = listOf(
    Comment("النجم السريع", "قصة رائعة لا مثيل لها! أنتظر كل فصل جديد بشغف.", 1204, 1),
    Comment("قارئ_مانجا99", "الرسومات والحبكة في أعلى مستوياتها، يستحق التقييم الكامل.", 887, 2),
    Comment("أبو_المانهوا", "من أفضل الأعمال التي قرأتها. الفصل الأخير كان مذهلاً!", 412, 3),
    Comment("مانجا_نايت", "أتمنى لو يُترجم بسرعة أكبر لكن الجودة تستحق الانتظار.", 203, 0),
    Comment("قمر_المانجا", "الشخصية الرئيسية تطورت كثيراً منذ البداية، رائع!", 178, 0),
)

// ─────────────────────────────────────────────────────────────
// APP ENTRY
// ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MangamelloApp() }
    }
}

// ─────────────────────────────────────────────────────────────
// ROOT
// ─────────────────────────────────────────────────────────────
@Composable
private fun MangamelloApp() {
    var themeAccent by remember { mutableStateOf(Accent) }
    var amoled      by remember { mutableStateOf(false) }
    val bg          = if (amoled) Black else Surface1

    var screen  by remember { mutableStateOf("home") }
    var drawer  by remember { mutableStateOf(false) }
    var picked  by remember { mutableStateOf<Manga?>(null) }
    var reading by remember { mutableStateOf<Manga?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(background = bg, surface = Surface2, primary = themeAccent)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(bg)) {

                when {
                    reading != null ->
                        ReaderScreen(reading!!, onBack = { reading = null })

                    picked != null ->
                        DetailScreen(
                            picked!!,
                            accent    = themeAccent,
                            onBack    = { picked = null },
                            onRead    = { reading = picked }
                        )

                    screen == "search"  -> SearchScreen(
                        accent  = themeAccent,
                        onBack  = { screen = "home" },
                        onPick  = { picked = it }
                    )
                    screen == "library" -> LibraryScreen(
                        accent  = themeAccent,
                        onBack  = { screen = "home" },
                        onPick  = { picked = it }
                    )
                    screen == "history" -> HistoryScreen(
                        accent  = themeAccent,
                        onBack  = { screen = "home" }
                    )
                    screen == "profile" -> ProfileScreen(
                        accent  = themeAccent,
                        onBack  = { screen = "home" }
                    )
                    screen == "settings" -> SettingsScreen(
                        accent       = themeAccent,
                        amoled       = amoled,
                        onAmoled     = { amoled = it },
                        onAccentPick = { themeAccent = it },
                        onBack       = { screen = "home" }
                    )
                    else -> HomeScreen(
                        accent   = themeAccent,
                        onMenu   = { drawer = true },
                        onSearch = { screen = "search" },
                        onPick   = { picked = it }
                    )
                }

                // Drawer overlay
                if (drawer) {
                    NavigationDrawer(
                        accent     = themeAccent,
                        onClose    = { drawer = false },
                        onNavigate = { dest ->
                            drawer = false
                            when (dest) {
                                "home"     -> { screen = "home"; picked = null; reading = null }
                                "search"   -> screen = "search"
                                "library"  -> screen = "library"
                                "history"  -> screen = "history"
                                "profile"  -> screen = "profile"
                                "settings" -> screen = "settings"
                            }
                        }
                    )
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
                Icon(Icons.Default.ArrowForward, "رجوع", tint = TextPri)
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
private fun HomeScreen(accent: Color, onMenu: () -> Unit, onSearch: () -> Unit, onPick: (Manga) -> Unit) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("آخر التحديثات", "الأكثر مشاهدة", "التحميلات")
    val featured = Mangas[8] // Solo Leveling as featured

    LazyColumn(Modifier.fillMaxSize()) {
        // ── Top bar
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onMenu) { Icon(Icons.Default.Menu, "القائمة", tint = TextPri) }
                // Logo M
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(accent),
                    contentAlignment = Alignment.Center
                ) { Text("M", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
                Spacer(Modifier.width(8.dp))
                Text("مانجاميلو", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            Box(
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
            1 -> Mangas.sortedByDescending { it.score }
            2 -> Mangas.filter { it.chapters > 100 }
            else -> Mangas
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
    Column(Modifier.clickable(onClick = onClick)) {
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
private fun DetailScreen(manga: Manga, accent: Color, onBack: () -> Unit, onRead: () -> Unit) {
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
                    Icon(Icons.Default.ArrowForward, "رجوع", tint = Color.White)
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
                IconButton({}) { Icon(Icons.Default.Comment, "تعليقات", tint = TextSec) }
                IconButton({ inLibrary = !inLibrary }) {
                    Icon(
                        if (inLibrary) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        "المكتبة",
                        tint = if (inLibrary) accent else TextSec
                    )
                }
            }
        }

        // ── Big action buttons
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRead,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ابدأ القراءة", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { inLibrary = !inLibrary },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (inLibrary) accent else TextSec),
                    border = BorderStroke(1.dp, if (inLibrary) accent else Border),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(if (inLibrary) "في المكتبة ✓" else "أضف للمكتبة")
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
            // ── Comments section
            item {
                Text(
                    "التعليقات",
                    color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
                )
            }
            items(SampleComments) { comment ->
                CommentRow(comment)
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
private fun CommentRow(comment: Comment) {
    val medalColor = when (comment.rank) {
        1 -> Gold; 2 -> Silver; 3 -> Bronze; else -> null
    }
    val medalLabel = when (comment.rank) {
        1 -> "ذهبي"; 2 -> "فضي"; 3 -> "برونزي"; else -> null
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Accent, AccentLt))),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.user.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(comment.user, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (medalColor != null && medalLabel != null) {
                    Surface(color = medalColor.copy(.2f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            medalLabel, color = medalColor, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.text, color = TextSec, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.ThumbUp, null, tint = TextDim, modifier = Modifier.size(14.dp))
                Text("${comment.likes}", color = TextDim, fontSize = 12.sp)
            }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
}

@Composable
private fun ChapterRow(number: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("الفصل $number", color = TextPri, fontSize = 15.sp)
            Text("منذ يومين", color = TextSec, fontSize = 12.sp)
        }
        IconButton({}) { Icon(Icons.Outlined.FileDownload, "تنزيل", tint = accent) }
        IconButton({}) { Icon(Icons.Default.Comment, "تعليقات", tint = TextDim) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
}

// ─────────────────────────────────────────────────────────────
// READER SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun ReaderScreen(manga: Manga, onBack: () -> Unit) {
    var uiVisible     by remember { mutableStateOf(true) }
    var brightness    by remember { mutableStateOf(.8f) }
    var showBrightness by remember { mutableStateOf(false) }
    var showChapters  by remember { mutableStateOf(false) }
    var currentChapter by remember { mutableStateOf(manga.chapters) }

    Box(
        Modifier.fillMaxSize().background(Black)
            .pointerInput(Unit) { detectTapGestures { uiVisible = !uiVisible } }
    ) {
        // Simulated manga pages
        LazyColumn(Modifier.fillMaxSize()) {
            items(8) { i ->
                Box(
                    Modifier.fillMaxWidth().height(320.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(manga.coverColors[0].copy(.3f), manga.coverColors[1].copy(.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("صفحة ${i + 1}", color = Color.White.copy(.3f), fontSize = 20.sp)
                }
                if (i < 7) Spacer(Modifier.height(2.dp))
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        // Top toolbar
        AnimatedVisibility(uiVisible, Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
            Surface(
                color = Black.copy(.88f),
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onBack) { Icon(Icons.Default.ArrowForward, "رجوع", tint = TextPri) }
                    Column(Modifier.weight(1f)) {
                        Text(manga.title, color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("الفصل $currentChapter", color = TextSec, fontSize = 12.sp)
                    }
                    // Next chapter
                    IconButton({ if (currentChapter < manga.chapters) currentChapter++ }) {
                        Icon(Icons.Default.NavigateBefore, "التالي", tint = TextPri)
                    }
                }
            }
        }

        // Bottom toolbar
        AnimatedVisibility(uiVisible, Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
            Surface(color = Black.copy(.92f), modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Brightness slider
                    AnimatedVisibility(showBrightness) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BrightnessLow, null, tint = TextSec, modifier = Modifier.size(18.dp))
                            Slider(value = brightness, onValueChange = { brightness = it }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
                            Icon(Icons.Default.BrightnessHigh, null, tint = TextPri, modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReaderIconBtn(Icons.Default.ScreenRotation, "تدوير") {}
                        ReaderIconBtn(Icons.Default.PlayCircle, "تشغيل") {}
                        ReaderIconBtn(Icons.Default.Brightness6, "سطوع") { showBrightness = !showBrightness }
                        ReaderIconBtn(Icons.Default.List, "الفصول") { showChapters = true }
                        ReaderIconBtn(Icons.Default.BrokenImage, "إصلاح") {}
                    }
                }
            }
        }

        // Chapter list bottom sheet (simplified)
        if (showChapters) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(.6f)).clickable { showChapters = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    Modifier.fillMaxWidth().fillMaxHeight(.55f).clickable {},
                    color = Surface2,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الفصول", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton({ showChapters = false }) { Icon(Icons.Default.Close, "إغلاق", tint = TextSec) }
                        }
                        HorizontalDivider(color = Border)
                        LazyColumn {
                            items(minOf(manga.chapters, 30)) { i ->
                                val chNum = manga.chapters - i
                                Row(
                                    Modifier.fillMaxWidth().clickable { currentChapter = chNum; showChapters = false }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الفصل $chNum",
                                        color = if (chNum == currentChapter) Accent else TextPri,
                                        fontWeight = if (chNum == currentChapter) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (chNum == currentChapter) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.PlayArrow, null, tint = Accent, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Border, thickness = .5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderIconBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick) { Icon(icon, label, tint = TextPri, modifier = Modifier.size(22.dp)) }
        Text(label, color = TextDim, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// SEARCH SCREEN
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
            placeholder = { Text("اكتب هنا للبحث...", color = TextDim) },
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
    val lists = listOf(
        Triple("المفضلة", Icons.Default.Favorite, 3),
        Triple("أقرأها الآن", Icons.Default.PlayCircle, 5),
        Triple("للقراءة لاحقًا", Icons.Default.BookmarkAdd, 8),
        Triple("الأعمال المكتملة", Icons.Default.CheckCircle, 2),
    )
    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("مكتبتي", onBack) }
        item {
            Text("قوائمي", color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        items(lists) { (label, icon, count) ->
            Row(
                Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = accent.copy(.15f), shape = CircleShape) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(40.dp).padding(10.dp))
                }
                Text(label, color = TextPri, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                Text("$count", color = TextSec, fontSize = 13.sp)
                Icon(Icons.Default.ChevronLeft, null, tint = TextDim)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
        }
        item {
            Text("اقتراحات لك", color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp))
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 1200.dp)
            ) {
                items(Mangas.take(6)) { manga ->
                    MangaCardSmall(manga, accent, onClick = { onPick(manga) })
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
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
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TopBar("الملف الشخصي", onBack, action = {
                IconButton({}) { Icon(Icons.Default.Edit, "تعديل", tint = TextSec) }
            })
        }
        // Avatar + name
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFCB4A0D), Color(0xFFE8803D)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("م", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                // online indicator
                Box(Modifier.size(14.dp).clip(CircleShape).background(Green).align(Alignment.Top))
                Column(Modifier.padding(horizontal = 14.dp)) {
                    Text("Mohammed Mohanad", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("@mangalover_99", color = TextSec, fontSize = 13.sp)
                }
            }
        }
        // Stats row
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("23", "شوهدت", Icons.Default.Visibility, Modifier.weight(1f))
                ProfileStat("5", "جاري", Icons.Default.PlayCircle, Modifier.weight(1f), Green)
                ProfileStat("3", "مفضلة", Icons.Default.Favorite, Modifier.weight(1f), Red)
                ProfileStat("8", "مكتملة", Icons.Default.CheckCircle, Modifier.weight(1f), accent)
            }
        }
        // Mini reading bar chart (simulated with boxes)
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("نشاط القراءة (هذا الشهر)", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    val bars = listOf(4, 7, 3, 9, 5, 12, 8, 6, 10, 4, 7, 11, 3, 8)
                    val maxVal = bars.max().toFloat()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        bars.forEach { v ->
                            Box(
                                Modifier.width(16.dp).height((v / maxVal * 60).dp + 4.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Brush.verticalGradient(listOf(accent, accent.copy(.4f))))
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("إجمالي 97 فصلاً هذا الشهر", color = TextSec, fontSize = 12.sp)
                }
            }
        }
        // Recent reads
        item {
            Text("آخر القراءات", color = TextPri, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp))
        }
        items(Mangas.take(5)) { manga ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverBox(manga, Modifier.size(52.dp), iconSize = 22.dp, showNew = false)
                Column(Modifier.weight(1f)) {
                    Text(manga.title, color = TextPri, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Text("الفصل ${manga.chapters}  •  منذ يوم", color = TextSec, fontSize = 12.sp)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border, thickness = .5.dp)
        }
        item { Spacer(Modifier.height(40.dp)) }
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

        // ── 3. التخزين
        item { SectionLabel("التخزين", accent) }
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("مساحة التخزين", color = TextPri, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text("0.16 / 2 GB", color = TextSec, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { .08f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = Surface3
                    )
                    Spacer(Modifier.height(12.dp))
                    Divider2()
                    SettingAction("تنظيف التخزين", "حذف الكاش والفصول القديمة", Icons.Default.CleaningServices, accent) {}
                    Divider2()
                    SettingAction("تنظيف تلقائي", "كل 30 يوم", Icons.Default.Autorenew, accent) {}
                }
            }
        }

        // ── 4. الخصوصية
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

        // ── 5. الحساب والإشعارات
        item { SectionLabel("الحساب والإشعارات", accent) }
        item {
            Surface(color = Surface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column {
                    SettingAction("الإشعارات", "إشعارات الفصول الجديدة والردود", Icons.Default.Notifications, accent) {}
                    Divider2()
                    SettingAction("إدارة الاشتراك", "الخطة المجانية", Icons.Default.Star, Gold) {}
                }
            }
        }

        // ── Danger zone
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Surface(
                color = Red.copy(.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(.5.dp, Red.copy(.25f)),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = Red, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text("حذف الحساب", color = Red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("لا يمكن التراجع عن هذا الإجراء", color = Red.copy(.6f), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = Red.copy(.6f))
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
                Text("مانجاميلو  •  الإصدار 1.0.0", color = TextDim, fontSize = 12.sp)
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
                Modifier.padding(24.dp).clickable {},
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
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(.65f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        Surface(
            Modifier.fillMaxHeight().width(320.dp).align(Alignment.CenterEnd)
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
                        Text("Mohammed Mohanad", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
