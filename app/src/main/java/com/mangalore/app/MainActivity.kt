package com.mangalore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Palette ────────────────────────────────────────────────────────────────
private val BgBase     = Color(0xFF0A0A12)
private val BgSurface  = Color(0xFF13131C)
private val BgElevated = Color(0xFF1C1C28)
private val TextMain   = Color(0xFFF0F0F8)
private val TextMuted  = Color(0xFF7878A0)
private val Accent     = Color(0xFFFF6535)   // orange CTA
private val AccentSoft = Color(0xFF2A1810)   // orange tint bg
private val Purple     = Color(0xFF9B7DFF)
private val Gold       = Color(0xFFFFB020)
private val NavBg      = Color(0xFF0F0F18)

// ─── Data ───────────────────────────────────────────────────────────────────
private data class Manga(
    val title: String,
    val score: String,
    val chapters: String,
    val colors: List<Color>,
    val genres: List<String> = listOf("أكشن", "خيال"),
    val badge: String? = null   // "NEW" | "TOP" | null
)

private val mangas = listOf(
    Manga("Back to Spring",                                   "9.28", "82",  listOf(Color(0xFF1A4060), Color(0xFF2E8BC0)), listOf("رومانسي","دراما"),       "NEW"),
    Manga("I Became the Daughter of a Million-Dollar Actor",  "8.82", "64",  listOf(Color(0xFF4A1830), Color(0xFF8B3A5A)), listOf("دراما","شوجو"),          "TOP"),
    Manga("Bad Born Blood",                                   "9.02", "99",  listOf(Color(0xFF0E1E30), Color(0xFF8B2020)), listOf("أكشن","إثارة"),           "NEW"),
    Manga("Revenge of the Iron-Blooded Sword Hound",          "9.11", "78",  listOf(Color(0xFF3A1020), Color(0xFFC8703A)), listOf("أكشن","انتقام"),          "TOP"),
    Manga("Nano Machine",                                     "9.40", "245", listOf(Color(0xFF102030), Color(0xFF4A9FC0)), listOf("خيال علمي","أكشن"),       "NEW"),
    Manga("Magic Emperor",                                    "9.16", "620", listOf(Color(0xFF201040), Color(0xFF8A30C0)), listOf("خيال","سحر"),              "TOP"),
    Manga("Murim's Youngest Miracle",                         "9.43", "87",  listOf(Color(0xFF3A2010), Color(0xFFC09030)), listOf("كونغ فو","أكشن"),          "NEW"),
    Manga("Becoming a Legendary Ace Employee",                "9.12", "56",  listOf(Color(0xFF102040), Color(0xFF4070C0)), listOf("حياة يومية","كوميدي"),     null),
    Manga("Solo Leveling",                                    "9.55", "202", listOf(Color(0xFF0A0A20), Color(0xFF5040A0)), listOf("أكشن","خيال"),              "TOP"),
    Manga("Logging 10,000 Years into the Future",             "8.98", "135", listOf(Color(0xFF102030), Color(0xFF309080)), listOf("خيال علمي"),               null),
    Manga("Death Is the Only Ending for the Villainess",      "9.31", "145", listOf(Color(0xFF60203A), Color(0xFFC07080)), listOf("رومانسي","إيسكاي"),        "TOP"),
    Manga("I Became the Tyrant's Time-Limited Wife",          "8.87", "71",  listOf(Color(0xFF301830), Color(0xFFA04060)), listOf("رومانسي","دراما"),          "NEW")
)
private val featured = mangas[8] // Solo Leveling

// ─── Activity ───────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MangaloreApp() }
    }
}

// ─── Root ────────────────────────────────────────────────────────────────────
@Composable
private fun MangaloreApp() {
    var tab      by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Manga?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = BgBase, surface = BgSurface, primary = Accent
    )) {
        CompositionLocalProvider(
            LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Box(Modifier.fillMaxSize().background(BgBase)) {
                if (selected != null) {
                    DetailScreen(selected!!, onBack = { selected = null })
                } else {
                    Scaffold(
                        containerColor = BgBase,
                        bottomBar = { BottomBar(tab) { tab = it } }
                    ) { inner ->
                        Box(Modifier.padding(inner)) {
                            when (tab) {
                                0 -> HomeScreen(onPick = { selected = it })
                                1 -> ExploreScreen(onPick = { selected = it })
                                2 -> LibraryScreen(onPick = { selected = it })
                                3 -> HistoryScreen()
                                4 -> ProfileScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Bottom Navigation ───────────────────────────────────────────────────────
private val NAV_ITEMS = listOf(
    Pair("الرئيسية",  Icons.Default.Home),
    Pair("استكشاف",   Icons.Default.Explore),
    Pair("مكتبتي",    Icons.Default.LibraryBooks),
    Pair("السجل",     Icons.Default.History),
    Pair("ملفي",      Icons.Default.Person)
)

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = NavBg, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NAV_ITEMS.forEachIndexed { i, (label, icon) ->
                val active = i == selected
                Column(
                    Modifier.clickable { onSelect(i) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Accent.copy(alpha = 0.18f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null,
                            tint = if (active) Accent else TextMuted,
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(label, fontSize = 10.sp, color = if (active) Accent else TextMuted)
                }
            }
        }
    }
}

// ─── Home ────────────────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(onPick: (Manga) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {

        // ── App Bar ──────────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("مساء الخير 👋", color = TextMuted, fontSize = 13.sp)
                    Text("Mangamello", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                BadgedBox(badge = {
                    Badge(containerColor = Accent, modifier = Modifier.size(8.dp)) {}
                }) {
                    Icon(Icons.Default.Notifications, null, tint = TextMain, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── Search bar ───────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("ابحث عن مانغا، مانهوا...", color = TextMuted, fontSize = 14.sp)
            }
        }

        // ── Category chips ───────────────────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf("الكل" to true, "أكشن" to false, "رومانسي" to false,
                    "خيال" to false, "إثارة" to false, "مغامرات" to false)
                items(cats) { (label, active) ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) Accent else BgElevated)
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(label,
                            color = if (active) Color.White else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        // ── Featured Banner ──────────────────────────────────────────────────
        item { FeaturedBanner(featured, onClick = { onPick(featured) }) }

        // ── Latest Updates ───────────────────────────────────────────────────
        item { SectionHeader("آخر التحديثات", "عرض الكل") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mangas.take(7)) { m -> MangaCard(m) { onPick(m) } }
            }
        }

        // ── Top Rated ────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(20.dp)) }
        item { SectionHeader("الأعلى تقييمًا", "عرض الكل") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mangas.filter { it.badge == "TOP" }) { m -> MangaCard(m) { onPick(m) } }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FeaturedBanner(manga: Manga, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(manga.colors))
            .clickable(onClick = onClick)
    ) {
        // Scrim from end
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black.copy(0.75f))
                )
            )
        )
        // Book icon decoration
        Icon(
            Icons.Default.AutoStories, null,
            tint = Color.White.copy(0.12f),
            modifier = Modifier.size(150.dp).align(Alignment.CenterStart).offset(x = (-24).dp)
        )
        // Text content (on the right / end side in RTL = left visually)
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp, top = 20.dp, bottom = 20.dp)
                .fillMaxWidth(0.60f)
        ) {
            Surface(color = Accent, shape = RoundedCornerShape(6.dp)) {
                Text("مميز", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(manga.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(manga.score, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("  •  ${manga.chapters} فصل", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(33.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("اقرأ الآن", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(action, color = Accent, fontSize = 13.sp)
    }
}

// Horizontal scroll card (120dp wide)
@Composable
private fun MangaCard(manga: Manga, onClick: () -> Unit) {
    Column(Modifier.width(118.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.width(118.dp).height(168.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(manga.colors))
        ) {
            Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(0.25f),
                modifier = Modifier.align(Alignment.Center).size(48.dp))
            manga.badge?.let { badge ->
                val badgeColor = if (badge == "NEW") Accent else Purple
                Surface(color = badgeColor, shape = RoundedCornerShape(bottomEnd = 9.dp)) {
                    Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(manga.title, color = TextMain, fontSize = 11.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(manga.score, color = Gold, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("${manga.chapters}ف", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// ─── Explore ─────────────────────────────────────────────────────────────────
@Composable
private fun ExploreScreen(onPick: (Manga) -> Unit) {
    var query         by remember { mutableStateOf("") }
    var activeGenre   by remember { mutableStateOf("الكل") }
    val genres = listOf("الكل", "أكشن", "رومانسي", "خيال", "إثارة", "دراما", "مغامرات", "كوميدي")
    val visible = mangas.filter {
        (activeGenre == "الكل" || it.genres.contains(activeGenre)) &&
        (query.isEmpty() || it.title.contains(query, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        Text("استكشاف", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp))

        // Search field
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("ابحث عن عنوان...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = BgSurface,
                focusedContainerColor = BgSurface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Accent,
                cursorColor = Accent,
                unfocusedTextColor = TextMain,
                focusedTextColor = TextMain
            )
        )

        Spacer(Modifier.height(12.dp))

        // Genre chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { g ->
                val active = g == activeGenre
                Box(
                    Modifier.clickable { activeGenre = g }
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) Accent else BgElevated)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(g, color = if (active) Color.White else TextMuted, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(visible) { m -> GridCard(m) { onPick(m) } }
        }
    }
}

@Composable
private fun GridCard(manga: Manga, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(manga.colors))
        ) {
            Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(0.25f),
                modifier = Modifier.align(Alignment.Center).size(42.dp))
            manga.badge?.let { badge ->
                Surface(
                    color = if (badge == "NEW") Accent else Purple,
                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(manga.title, color = TextMain, fontSize = 11.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(manga.score, color = Gold, fontSize = 10.sp)
        }
    }
}

// ─── Detail ───────────────────────────────────────────────────────────────────
@Composable
private fun DetailScreen(manga: Manga, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(BgBase)) {

        // Hero
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                // Gradient cover
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.linearGradient(manga.colors + listOf(BgBase)))
                )
                // Bottom fade
                Box(
                    Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, BgBase)))
                )
                // Book icon
                Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(0.2f),
                    modifier = Modifier.size(110.dp).align(Alignment.Center))
                // Back button
                Box(
                    Modifier.statusBarsPadding().padding(12.dp).size(38.dp)
                        .clip(CircleShape).background(Color.Black.copy(0.5f))
                        .clickable(onClick = onBack)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowForward, "رجوع", tint = TextMain)
                }
            }
        }

        // Title & meta
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(manga.title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) {
                        Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(manga.score, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("  •  الفصل ${manga.chapters}  •  مستمر",
                        color = TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    manga.genres.forEach { g ->
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Purple.copy(0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(g, color = Purple, fontSize = 12.sp) }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(BgElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("مانهوا  •  +15", color = TextMuted, fontSize = 12.sp) }
                }
            }
        }

        // Action buttons
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ابدأ القراءة", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BgElevated)
                ) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text("أضف للمكتبة", color = TextMain)
                }
            }
        }

        // Description
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("القصة", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "تدور أحداث هذه القصة الملهمة في عالم مليء بالمغامرات والأسرار. يسعى البطل لاكتشاف حقيقة قوته الخفية بينما يواجه تحديات تتخطى حدود المنطق. تابع أحدث الفصول واستمتع بتجربة قراءة مثيرة.",
                    color = TextMuted, fontSize = 14.sp, lineHeight = 24.sp
                )
            }
        }

        // Stats
        item {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("283K", "مشاهدة")
                StatBox(manga.chapters, "فصل")
                StatBox(manga.score, "تقييم")
            }
        }

        // Chapters header
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("قائمة الفصول", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Default.FilterList, null, tint = TextMuted, modifier = Modifier.size(22.dp))
            }
        }

        // Chapters
        items(10) { i ->
            val num = manga.chapters.toIntOrNull()?.minus(i) ?: (99 - i)
            Row(
                Modifier.fillMaxWidth().clickable {}.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(BgSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$num", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("الفصل $num", color = TextMain, fontSize = 15.sp)
                    Text("منذ ${i + 1} ${if (i == 0) "يوم" else "أيام"}", color = TextMuted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Download, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Comment, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RowScope.StatBox(value: String, label: String) {
    Surface(color = BgSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}

// ─── Library ──────────────────────────────────────────────────────────────────
@Composable
private fun LibraryScreen(onPick: (Manga) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("مكتبتي", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp))
        }
        // Lists
        item {
            val lists = listOf(
                Triple("المفضلة",        Icons.Default.Favorite,    "0"),
                Triple("أقرأها الآن",    Icons.Default.MenuBook,    "3"),
                Triple("للقراءة لاحقًا", Icons.Default.Bookmark,    "7"),
                Triple("مقروءة",         Icons.Default.CheckCircle, "13")
            )
            lists.forEach { (label, icon, count) ->
                Row(
                    Modifier.fillMaxWidth().clickable {}
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(AccentSoft),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp)) }
                    Text(label, color = TextMain, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), fontSize = 15.sp)
                    Text(count, color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronLeft, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(BgElevated))
        }
        item { SectionHeader("مقترحات لك", "عرض الكل") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mangas.take(6)) { m -> MangaCard(m) { onPick(m) } }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── History ──────────────────────────────────────────────────────────────────
@Composable
private fun HistoryScreen() {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سجل القراءة", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Default.DeleteOutline, null, tint = TextMuted)
            }
        }
        Text("${mangas.size} عمل", color = TextMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn {
            items(mangas) { m ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(m.colors)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(0.6f)) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(m.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("الفصل 1  •  منذ ساعتين", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.35f },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = Accent, trackColor = BgElevated
                        )
                    }
                    Icon(Icons.Default.MoreVert, null, tint = TextMuted)
                }
            }
        }
    }
}

// ─── Profile ──────────────────────────────────────────────────────────────────
@Composable
private fun ProfileScreen() {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                Modifier.fillMaxWidth().statusBarsPadding().height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A0A30), BgBase)))
            ) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(82.dp).clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFFCB4A0D)),
                        contentAlignment = Alignment.Center
                    ) { Text("M", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))
                    Text("Mohammed Mohanad", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("عضو منذ 2024", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStat("23", "شوهدت")
                ProfileStat("0", "مفضلة")
                ProfileStat("6", "السجل")
            }
        }
        item {
            val items = listOf(
                Pair("الإشعارات",        Icons.Default.Notifications),
                Pair("الإعدادات",        Icons.Default.Settings),
                Pair("المظهر والعرض",    Icons.Default.Palette),
                Pair("الخصوصية والأمان", Icons.Default.Lock),
                Pair("التخزين",          Icons.Default.Storage)
            )
            Spacer(Modifier.height(8.dp))
            items.forEach { (label, icon) ->
                Row(
                    Modifier.fillMaxWidth().clickable {}
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AccentSoft),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp)) }
                    Text(label, color = TextMain, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), fontSize = 15.sp)
                    Icon(Icons.Default.ChevronLeft, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Text("Mangamello  •  الإصدار 1.0.0", color = TextMuted,
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RowScope.ProfileStat(value: String, label: String) {
    Surface(color = BgSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}
