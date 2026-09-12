package com.mangalore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF000000)
private val Panel = Color(0xFF0B0B0D)
private val Panel2 = Color(0xFF151519)
private val TextMain = Color(0xFFF2F2F4)
private val TextMuted = Color(0xFF8E8E96)
private val Accent = Color(0xFF9BB2D4)
private val Gold = Color(0xFFFFC44D)

private data class Manga(val title: String, val score: String, val chapters: String, val colors: List<Color>)
private val mangas = listOf(
    Manga("Back to Spring", "9.28", "82", listOf(Color(0xFF9ACDDD), Color(0xFF385B79))),
    Manga("I Became the Daughter of a Million-Dollar Actor", "8.82", "64", listOf(Color(0xFFE8A7B7), Color(0xFF55324D))),
    Manga("Bad Born Blood", "9.02", "99", listOf(Color(0xFF152F4A), Color(0xFFB34035))),
    Manga("Revenge of the Iron-Blooded Sword Hound", "9.11", "78", listOf(Color(0xFF5B1E29), Color(0xFFE8A04B))),
    Manga("Nano Machine", "9.40", "245", listOf(Color(0xFF152B38), Color(0xFF7BBDD0))),
    Manga("Magic Emperor", "9.16", "620", listOf(Color(0xFF372066), Color(0xFFAC4FDC))),
    Manga("Murim's Youngest Miracle", "9.43", "87", listOf(Color(0xFFF1B76E), Color(0xFF4A291E))),
    Manga("Becoming a Legendary Ace Employee", "9.12", "56", listOf(Color(0xFF244D92), Color(0xFFB2D3ED))),
    Manga("Solo Leveling", "9.55", "202", listOf(Color(0xFF161B3B), Color(0xFF6E54C8))),
    Manga("Logging 10,000 Years into the Future", "8.98", "135", listOf(Color(0xFF173A4A), Color(0xFF52A5B8))),
    Manga("Death Is the Only Ending for the Villainess", "9.31", "145", listOf(Color(0xFF8D3155), Color(0xFFEDB4BD))),
    Manga("I Became the Tyrant's Time-Limited Wife", "8.87", "71", listOf(Color(0xFF4D2948), Color(0xFFDB789B)))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MangaloreApp() }
    }
}

@Composable
private fun MangaloreApp() {
    var page by remember { mutableStateOf("الرئيسية") }
    var drawer by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Manga?>(null) }
    MaterialTheme(colorScheme = darkColorScheme(background = Black, surface = Panel, primary = Accent)) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(Black)) {
                when {
                    selected != null -> DetailScreen(selected!!, onBack = { selected = null })
                    page == "البحث" -> SearchScreen(onBack = { page = "الرئيسية" }, onPick = { selected = it })
                    page == "الإعدادات" -> SettingsScreen(onBack = { page = "الرئيسية" })
                    page == "الملف الشخصي" -> ProfileScreen(onBack = { page = "الرئيسية" })
                    page == "سجل المشاهدة" -> HistoryScreen(onBack = { page = "الرئيسية" })
                    page == "المكتبة" -> LibraryScreen(onBack = { page = "الرئيسية" }, onPick = { selected = it })
                    else -> HomeScreen(onMenu = { drawer = true }, onPick = { selected = it })
                }
                if (drawer) NavigationDrawer(onClose = { drawer = false }, onNavigate = { page = it; drawer = false })
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, "رجوع", tint = TextMain) }
        Text(title, color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(onMenu: () -> Unit, onPick: (Manga) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("الرئيسية", action = {
            Row { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "القائمة", tint = TextMain) }; IconButton(onClick = {}) { Icon(Icons.Default.Notifications, "الإشعارات", tint = TextMain) } }
        })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("آخر التحديثات", "الأكثر مشاهدة", "التصنيف").forEachIndexed { i, label ->
                Surface(color = if (i == 0) Accent else Panel2, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) { Text(label, color = if (i == 0) Black else TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items(mangas) { manga -> MangaCard(manga, onClick = { onPick(manga) }) }
        }
    }
}

@Composable
private fun MangaCard(manga: Manga, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(manga.colors))) {
            Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(alpha = .38f), modifier = Modifier.align(Alignment.Center).size(52.dp))
            Text("NEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).background(Color(0xFFE56B45), RoundedCornerShape(bottomEnd = 8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(6.dp)); Text(manga.title, color = TextMain, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${manga.score} ★", color = Gold, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(manga.chapters, color = TextMuted, fontSize = 11.sp); Icon(Icons.Default.MenuBook, null, tint = TextMuted, modifier = Modifier.size(13.dp)) }
    }
}

@Composable
private fun DetailScreen(manga: Manga, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("تفاصيل العمل", onBack) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.width(135.dp).height(195.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(manga.colors)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(.6f), modifier = Modifier.size(60.dp)) }
                Column(Modifier.weight(1f)) { Text(manga.title, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("الفصل ${manga.chapters} • مستمر", color = Accent, modifier = Modifier.padding(top = 10.dp)); Text("مانهوا كورية • +15", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp)); Text("أكشن  •  خيال  •  إثارة  •  انتقام", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item { Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.weight(1f)) { Text("ابدأ القراءة", color = Black) }; OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("أضف للمكتبة") } } }
        item { Text("تدور أحداث هذه القصة في عالم مليء بالأسرار والمواجهات. تابع أحدث الفصول واستمتع بتجربة قراءة مريحة ومخصصة بالكامل.", color = TextMuted, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
        item { Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("283K", "مشاهدات"); Stat(manga.chapters, "الفصول"); Stat(manga.score, "التقييم") } }
        item { Text("الفصول المتاحة", color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        items(8) { i -> ChapterRow(manga.chapters.toIntOrNull()?.minus(i) ?: 99) }
    }
}

@Composable private fun RowScope.Stat(value: String, label: String) { Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(label, color = TextMuted, fontSize = 11.sp) } } }
@Composable private fun ChapterRow(number: Int) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("الفصل $number", color = TextMain, fontSize = 16.sp); Text("منذ يومين", color = TextMuted, fontSize = 12.sp) }; Icon(Icons.Default.Download, "تنزيل", tint = Accent); Spacer(Modifier.width(18.dp)); Icon(Icons.Default.Comment, "تعليقات", tint = TextMuted) } }

@Composable private fun SearchScreen(onBack: () -> Unit, onPick: (Manga) -> Unit) { Column { TopBar("البحث", onBack); OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("اكتب هنا للبحث...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), singleLine = true); Text("أعمال مقترحة", color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp)); LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { items(mangas.take(9)) { MangaCard(it) { onPick(it) } } } } }
@Composable private fun LibraryScreen(onBack: () -> Unit, onPick: (Manga) -> Unit) { Column { TopBar("مكتبتي", onBack); Text("قوائمي", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp)); listOf("المفضلة", "أقرأها الآن", "للقراءة لاحقًا", "الأعمال المقروءة").forEach { Row(Modifier.fillMaxWidth().clickable {}.padding(horizontal = 22.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FavoriteBorder, null, tint = Accent); Text(it, color = TextMain, modifier = Modifier.padding(horizontal = 16.dp)); Spacer(Modifier.weight(1f)); Text("0", color = TextMuted) } }; Text("اقتراحات لك", color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp)); LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) { items(mangas.take(6)) { MangaCard(it) { onPick(it) } } } } }
@Composable private fun HistoryScreen(onBack: () -> Unit) { Column { TopBar("سجل المشاهدة", onBack); Text("6 أعمال", color = TextMuted, modifier = Modifier.padding(horizontal = 20.dp)); mangas.take(6).forEachIndexed { i, manga -> Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(manga.colors)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(.6f)) }; Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(manga.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("الفصل ${i + 1} • منذ ساعتين", color = TextMuted, fontSize = 12.sp) }; Icon(Icons.Default.DeleteOutline, null, tint = TextMuted) } } } }
@Composable private fun ProfileScreen(onBack: () -> Unit) { Column { TopBar("الملف الشخصي", onBack); Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFCB4A0D)), contentAlignment = Alignment.Center) { Text("M", color = Color.White, fontSize = 40.sp) }; Text("Mohammed Mohanad", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)) }; Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("23", "شوهدت"); Stat("0", "المفضلة"); Stat("6", "السجل") }; Text("آخر القراءات", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp)); mangas.take(4).forEach { Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) { Text(it.title, color = TextMain, modifier = Modifier.weight(1f)); Text("الفصل 1", color = TextMuted) } } } }

@Composable private fun SettingsScreen(onBack: () -> Unit) { var dark by remember { mutableStateOf(true) }; Column { TopBar("الإعدادات", onBack); Text("المظهر والعرض", color = Accent, modifier = Modifier.padding(20.dp)); SettingToggle("الوضع الليلي", "تفعيل الواجهة الداكنة", dark) { dark = it }; SettingToggle("وضع AMOLED الأسود", "توفير البطارية على شاشات AMOLED", true) {}; SettingRow("لون التطبيق", "اختر لون الواجهة", Icons.Default.Palette); SettingRow("نمط القوائم", "شبكة أو قائمة", Icons.Default.GridView); Text("تجربة القراءة", color = Accent, modifier = Modifier.padding(20.dp)); SettingRow("اتجاه القراءة", "طولي", Icons.Default.SwapVert); SettingToggle("ملء الشاشة", "إخفاء أشرطة النظام أثناء القراءة", true) {}; Text("التخزين والخصوصية", color = Accent, modifier = Modifier.padding(20.dp)); SettingRow("التخزين", "0.16 MB مستخدمة", Icons.Default.Storage); SettingRow("الخصوصية والأمان", "تحذيرات المحتوى والمستخدمون المحظورون", Icons.Default.Lock); Text("الحساب", color = Accent, modifier = Modifier.padding(20.dp)); SettingRow("الإشعارات", "إشعارات الردود", Icons.Default.Notifications); Text("Mangalore  •  الإصدار 1.0.0", color = TextMuted, modifier = Modifier.align(Alignment.CenterHorizontally).padding(28.dp)) } }
@Composable private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = TextMain, fontSize = 16.sp); Text(subtitle, color = TextMuted, fontSize = 12.sp) }; Switch(checked, onCheckedChange = onChecked) } }
@Composable private fun SettingRow(title: String, subtitle: String, icon: ImageVector) { Row(Modifier.fillMaxWidth().clickable {}.padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent); Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(title, color = TextMain, fontSize = 16.sp); Text(subtitle, color = TextMuted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = TextMuted) } }

@Composable private fun NavigationDrawer(onClose: () -> Unit, onNavigate: (String) -> Unit) { Box(Modifier.fillMaxSize().background(Color.Black.copy(.65f)).clickable { onClose() }) { Surface(Modifier.fillMaxHeight().width(330.dp).align(Alignment.CenterEnd), color = Panel) { Column(Modifier.padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFCB4A0D)), contentAlignment = Alignment.Center) { Text("M", color = Color.White, fontSize = 32.sp) }; Column(Modifier.padding(horizontal = 14.dp)) { Text("Mohammed Mohanad", color = TextMain, fontWeight = FontWeight.Bold); Text("الملف الشخصي", color = TextMuted, fontSize = 12.sp) } }; Spacer(Modifier.height(24.dp)); Text("القائمة الرئيسية", color = Accent, modifier = Modifier.padding(vertical = 10.dp)); DrawerItem("الرئيسية", Icons.Default.Home) { onNavigate("الرئيسية") }; DrawerItem("البحث", Icons.Default.Search) { onNavigate("البحث") }; DrawerItem("أعمال أضيفت مؤخرًا", Icons.Default.NewReleases) { onNavigate("الرئيسية") }; DrawerItem("المكتبة", Icons.Default.LibraryBooks) { onNavigate("المكتبة") }; DrawerItem("سجل المشاهدة", Icons.Default.History) { onNavigate("سجل المشاهدة") }; DrawerItem("الملف الشخصي", Icons.Default.Person) { onNavigate("الملف الشخصي") }; Spacer(Modifier.height(18.dp)); Text("الإعدادات", color = Accent, modifier = Modifier.padding(vertical = 10.dp)); DrawerItem("الإعدادات", Icons.Default.Settings) { onNavigate("الإعدادات") }; SettingToggle("الوضع الليلي", "", true) {}; Spacer(Modifier.weight(1f)); Text("Mangalore", color = TextMuted, modifier = Modifier.align(Alignment.CenterHorizontally)) } } } }
@Composable private fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = TextMain); Text(label, color = TextMain, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 16.sp) } }
