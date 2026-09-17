package com.mangalore.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.os.Bundle
import java.io.File
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import org.json.JSONArray

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
private val Accent   = Color(0xFFB88A5A)
private val AccentLt = Color(0xFFD1AA78)
private val Gold     = Color(0xFFFFC44D)
private val Red      = Color(0xFFE05050)
private val Green    = Color(0xFF4CAF82)

private val Palette = listOf(
    Color(0xFFB88A5A), Color(0xFF9B7653), Color(0xFF8E6B58), Color(0xFF7A6655),
    Color(0xFF6E7B73), Color(0xFF708090), Color(0xFF7B7065), Color(0xFFA58B72),
    Color(0xFF8B7765), Color(0xFF6F7C78), Color(0xFF927B65), Color(0xFF7D8580),
    Color(0xFFB09A7A), Color(0xFF8A8178), Color(0xFF9A8670), Color(0xFF65736E),
    Color(0xFFA48C76), Color(0xFF7F6D5D),
)

// Standard manga cover sizes (2:3 ratio)
private val CoverAspect = 2f / 3f
private val CoverHeightXs = 124.dp
private val CoverWidthXs = 88.dp
private val CoverHeightSm = 155.dp
private val CoverHeightMd = 158.dp
private val CoverWidthMd = 108.dp
private val CoverHeightHero = 148.dp
private val CoverWidthHero = 102.dp
private val CoverHeightCh = 68.dp
private val CoverWidthCh = 46.dp

private val Font = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.readex_pro_light,    FontWeight.Light),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_regular,  FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_medium,   FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.readex_pro_bold,     FontWeight.Bold),
)
private val InputShape = RoundedCornerShape(24.dp)
private val ButtonShape = RoundedCornerShape(20.dp)
private val AppTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Font), displayMedium = t.displayMedium.copy(fontFamily = Font), displaySmall = t.displaySmall.copy(fontFamily = Font),
        headlineLarge = t.headlineLarge.copy(fontFamily = Font), headlineMedium = t.headlineMedium.copy(fontFamily = Font), headlineSmall = t.headlineSmall.copy(fontFamily = Font),
        titleLarge = t.titleLarge.copy(fontFamily = Font), titleMedium = t.titleMedium.copy(fontFamily = Font), titleSmall = t.titleSmall.copy(fontFamily = Font),
        bodyLarge = t.bodyLarge.copy(fontFamily = Font), bodyMedium = t.bodyMedium.copy(fontFamily = Font), bodySmall = t.bodySmall.copy(fontFamily = Font),
        labelLarge = t.labelLarge.copy(fontFamily = Font), labelMedium = t.labelMedium.copy(fontFamily = Font), labelSmall = t.labelSmall.copy(fontFamily = Font)
    )
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "شعار مانجالور",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

private fun commentsKey(url: String) = "comments_${url.hashCode().toUInt().toString(16)}"

private fun formatRelativeDate(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val months = mapOf(
        "يناير" to "January", "فبراير" to "February", "مارس" to "March", "أبريل" to "April",
        "مايو" to "May", "يونيو" to "June", "يوليو" to "July", "أغسطس" to "August",
        "سبتمبر" to "September", "أكتوبر" to "October", "نوفمبر" to "November", "ديسمبر" to "December"
    )
    val normalized = months.entries.fold(value) { text, (ar, en) -> text.replace(ar, en) }
    val epoch = normalized.toLongOrNull()?.let { if (it < 10_000_000_000L) it * 1000L else it }
    val parsed = epoch ?: runCatching {
        val formats = listOf("MMMM d, yyyy", "MMM d, yyyy", "yyyy-MM-dd", "dd/MM/yyyy")
        formats.firstNotNullOfOrNull { pattern ->
            runCatching { java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).parse(normalized)?.time }.getOrNull()
        }
    }.getOrNull() ?: return value
    val diff = System.currentTimeMillis() - parsed
    if (diff < 0) return value
    val minutes = diff / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 60 -> "قبل ساعة"
        hours == 1L -> "قبل ساعة"
        hours == 2L -> "قبل ساعتين"
        hours in 3..10 -> "قبل $hours ساعات"
        days == 1L -> "قبل يوم"
        days == 2L -> "قبل يومين"
        days in 3..7 -> "قبل $days أيام"
        else -> java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ar")).format(java.util.Date(parsed))
    }
}

private fun readSavedComments(context: Context, url: String): List<String> {
    val prefs = context.getSharedPreferences("mangalore_comments", Context.MODE_PRIVATE)
    val encoded = prefs.getString(commentsKey(url), null)
    if (!encoded.isNullOrBlank()) runCatching {
        val array = JSONArray(encoded)
        return (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }
    return prefs.getString(url, "").orEmpty().split("\n").filter { it.isNotBlank() }
}

private fun saveComments(context: Context, url: String, comments: List<String>) {
    val array = JSONArray()
    comments.forEach { array.put(it) }
    context.getSharedPreferences("mangalore_comments", Context.MODE_PRIVATE)
        .edit().putString(commentsKey(url), array.toString()).remove(url).apply()
}

// ══════════════════════════════════════════════════════════════
// NAV
// ══════════════════════════════════════════════════════════════
private sealed class Dest {
    object Home : Dest()
    object Search : Dest()
    object AllManga : Dest()
    object LatestManga : Dest()
    object PopularManga : Dest()
    object Library : Dest()
    object History : Dest()
    object Downloads : Dest()
    data class DownloadedManga(val group: DownloadGroup) : Dest()
    object CustomLists : Dest()
    object Profile : Dest()
    object Settings : Dest()
    data class Detail(val item: MangaItem) : Dest()
    data class DetailFull(val d: MangaDetail) : Dest()
    data class Reader(val url: String, val chTitle: String, val manga: MangaDetail, val chapterIndex: Int, val page: Int = 1) : Dest()
}

data class ReadingProgress(val item: MangaItem, val manga: MangaDetail, val chapterIndex: Int, val page: Int = 1, val totalPages: Int = 0, val completed: Boolean = false)

// ══════════════════════════════════════════════════════════════
// ENTRY
// ══════════════════════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    private var oauthTick by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)
        setContent { App(oauthTick) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }
    private fun handleOAuthIntent(intent: Intent?) {
        if (intent?.data?.scheme == "mangalore") oauthTick++
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Bg), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(Modifier.size(84.dp))
            Spacer(Modifier.height(16.dp))
            Text("Mangalore", color = TextPri, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                fontFamily = Font, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AuthScreen(onSignedIn: () -> Unit, onGoogle: () -> Unit, onGuest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            BrandMark(Modifier.size(92.dp))
            Spacer(Modifier.height(14.dp))
            Text("Mangalore", color = TextPri, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                fontFamily = Font, letterSpacing = (-0.5).sp)
            Text("احفظ مكتبتك وتابع قراءتك", color = TextSec, fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

            // Main card
            Surface(
                color = Surface2,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp)) {

                    // Tab switcher (دخول / تسجيل)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Surface3).padding(4.dp)) {
                        listOf("دخول", "تسجيل").forEachIndexed { i, lbl ->
                            val sel = (i == 1) == register
                            Surface(
                                color = if (sel) Accent else Color.Transparent,
                                shape = ButtonShape,
                                modifier = Modifier.weight(1f).clickable { register = i == 1; error = "" }
                            ) {
                                Text(lbl, color = if (sel) Color.White else TextSec,
                                    fontSize = 14.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (register) {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("الاسم") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent, unfocusedBorderColor = Border,
                                focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                                focusedLabelColor = Accent, unfocusedLabelColor = TextSec,
                                cursorColor = Accent,
                                unfocusedContainerColor = Surface3, focusedContainerColor = Surface3
                            ),
                            shape = InputShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = TextDim, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent, unfocusedBorderColor = Border,
                            focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                            focusedLabelColor = Accent, unfocusedLabelColor = TextSec,
                            cursorColor = Accent,
                            unfocusedContainerColor = Surface3, focusedContainerColor = Surface3
                        ),
                        shape = InputShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextDim, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton({ showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextDim, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPass) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent, unfocusedBorderColor = Border,
                            focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                            focusedLabelColor = Accent, unfocusedLabelColor = TextSec,
                            cursorColor = Accent,
                            unfocusedContainerColor = Surface3, focusedContainerColor = Surface3
                        ),
                        shape = InputShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = error.isNotBlank()) {
                        Surface(
                            color = Red.copy(.12f),
                            shape = InputShape,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(16.dp))
                                Text(error, color = Red, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        enabled = !busy && email.isNotBlank() && password.length >= 6 && (!register || name.isNotBlank()),
                        onClick = {
                            busy = true; error = ""
                            scope.launch {
                                val result = if (register)
                                    AuthStore.signUp(context, email.trim(), password, name.trim())
                                else
                                    AuthStore.signIn(context, email.trim(), password)
                                busy = false
                                result.onSuccess { onSignedIn() }
                                    .onFailure { error = it.message ?: "تعذر إكمال العملية" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = InputShape
                    ) {
                        if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(
                            if (register) "إنشاء الحساب" else "دخول",
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = Border)
                        Text("  أو  ", color = TextDim, fontSize = 12.sp)
                        HorizontalDivider(Modifier.weight(1f), color = Border)
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onGoogle,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = InputShape,
                        border = BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface3)
                    ) {
                        Image(painterResource(R.drawable.google_g), "Google", Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("المتابعة مع Google", color = TextPri, fontWeight = FontWeight.Medium, fontSize = 14.sp, fontFamily = Font)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
                        Text("الدخول كضيف", color = AccentLt, fontFamily = Font, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("باستخدام التطبيق توافق على شروط الاستخدام", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// APP ROOT
// ══════════════════════════════════════════════════════════════
@Composable
private fun App(oauthTick: Int = 0) {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()
    var authReady by remember { mutableStateOf(false) }
    var signedIn by remember { mutableStateOf(false) }
    var guestMode by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var authNotice by remember { mutableStateOf("") }
    LaunchedEffect(oauthTick) {
        val uri = (context as? MainActivity)?.intent?.data
        if (uri?.scheme == "mangalore") {
            AuthStore.completeAuthCallback(context, uri).onSuccess {
                signedIn = true
                guestMode = false
                if (uri.fragment?.contains("type=signup") == true || uri.getQueryParameter("type") == "signup") {
                    authNotice = "تم التحقق من البريد الإلكتروني، وتم تسجيل الدخول تلقائياً"
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        signedIn = AuthStore.restore(context)
        authReady = true
    }
    if (!authReady) {
        SplashScreen()
        return
    }
    if (!signedIn && !guestMode) {
        MaterialTheme(
            colorScheme = darkColorScheme(background = Bg, surface = Surface2, primary = Accent),
            typography = AppTypography,
            shapes = Shapes(extraSmall = InputShape, small = InputShape, medium = InputShape, large = InputShape, extraLarge = InputShape)
        ) {
            AuthScreen(onSignedIn = { signedIn = true; guestMode = false }, onGoogle = { showGoogleDialog = true }, onGuest = { guestMode = true })
            if (showGoogleDialog) {
                GoogleAuthDialog(
                    onSigned = { uri ->
                        showGoogleDialog = false
                        appScope.launch { AuthStore.completeGoogle(context, uri).onSuccess { signedIn = true; guestMode = false } }
                    },
                    onDismiss = { showGoogleDialog = false }
                )
            }
        }
        return
    }
    val settingsPrefs = remember { context.getSharedPreferences("mangalore_settings", Context.MODE_PRIVATE) }
    var accent  by remember { mutableStateOf(Color(settingsPrefs.getInt("accent", Accent.toArgb()))) }
    var amoled  by remember { mutableStateOf(settingsPrefs.getBoolean("amoled", false)) }
    val appBg   = if (amoled) Black else Bg

    var stack   by remember { mutableStateOf(listOf<Dest>(Dest.Home)) }
    val cur     = stack.last()
    var drawer  by remember { mutableStateOf(false) }

    val lib     = remember { mutableStateListOf<MangaItem>() }
    var hist by remember { mutableStateOf(listOf<ReadingProgress>()) }
    LaunchedEffect(signedIn) {
        if (signedIn) {
            runCatching { CloudStore.fetchLibrary() }.onSuccess { remote ->
                lib.clear()
                lib.addAll(remote)
            }
            runCatching { CloudStore.fetchProgress() }.onSuccess { remote ->
                hist = remote.distinctBy { it.manga.url }
            }
        }
    }

    var showCf  by remember { mutableStateOf(false) }
    var readerRefresh by remember { mutableIntStateOf(0) }
    var toast   by remember { mutableStateOf("") }
    LaunchedEffect(authNotice) {
        if (authNotice.isNotBlank()) {
            toast = authNotice
            authNotice = ""
        }
    }

    fun push(d: Dest) { stack = stack + d; drawer = false }
    fun replaceTop(d: Dest) { stack = if (stack.size > 1) stack.dropLast(1) + d else listOf(d) }
    fun pop()  { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home() { stack = listOf(Dest.Home); drawer = false }

    BackHandler(stack.size > 1 || drawer) { if (drawer) drawer = false else pop() }

    MaterialTheme(
        colorScheme = darkColorScheme(background = appBg, surface = Surface2, primary = accent),
        typography = AppTypography,
        shapes = Shapes(
            extraSmall = InputShape,
            small = InputShape,
            medium = InputShape,
            large = InputShape,
            extraLarge = InputShape
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Font)
        ) {
            Box(Modifier.fillMaxSize().background(appBg)) {

                // ── Screens ───────────────────────────────────
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = cur,
                    transitionSpec = {
                        if (initialState is Dest.Detail && targetState is Dest.DetailFull) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val forward = stack.size > 1
                            (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { if (forward) it else -it }) togetherWith
                                (fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { if (forward) -it else it })
                        }
                    },
                    label = "rtl-screen-transition"
                ) { d ->
                    when (d) {
                        is Dest.Home -> HomeScreen(accent, { drawer = true }, { push(Dest.Search) }) { push(Dest.Detail(it)) }
                        is Dest.Search -> SearchScreen(accent, ::pop) { push(Dest.Detail(it)) }
                        is Dest.AllManga -> MangaListScreen("كل المانجا", accent, ::pop, { Scraper.fetchAll(it) }) { push(Dest.Detail(it)) }
                        is Dest.LatestManga -> MangaListScreen("أحدث المانجا", accent, ::pop, { Scraper.fetchLatest(it) }) { push(Dest.Detail(it)) }
                        is Dest.PopularManga -> MangaListScreen("أشهر المانجا", accent, ::pop, { Scraper.fetchPopular(it) }) { push(Dest.Detail(it)) }
                        is Dest.Library -> LibraryScreen(accent, lib, ::pop, { push(Dest.Detail(it)) }) {
                            lib.remove(it)
                            appScope.launch { runCatching { CloudStore.removeLibrary(it.url) } }
                        }
                        is Dest.History -> HistoryScreen(accent, hist, ::pop, { p -> push(Dest.Reader(p.manga.chapters[p.chapterIndex].url, "الفصل ${p.manga.chapters[p.chapterIndex].number}", p.manga, p.chapterIndex, p.page)) }) { hist = emptyList() }
                        is Dest.Downloads -> DownloadsScreen(accent, ::pop) { push(Dest.DownloadedManga(it)) }
                        is Dest.DownloadedManga -> DownloadedMangaScreen(d.group, accent, ::pop) { chapter, manga -> push(Dest.Reader(chapter.url, "الفصل ${chapter.number}", manga, d.group.chapterUrls.indexOf(chapter.url), 1)) }
                        is Dest.CustomLists -> CustomListsScreen(accent, ::pop) { push(Dest.Detail(it)) }
                        is Dest.Profile -> ProfileScreen(accent, ::pop)
                        is Dest.Settings -> SettingsScreen(
                            accent, amoled,
                            { amoled = it; settingsPrefs.edit().putBoolean("amoled", it).apply() },
                            { accent = it; settingsPrefs.edit().putInt("accent", it.toArgb()).apply() },
                            ::pop,
                            { AuthStore.signOut(context); signedIn = false; guestMode = false },
                            { signedIn = false; guestMode = false }
                        )
                        is Dest.Detail -> DetailLoadingScreen(d.item, accent, lib, ::pop) { replaceTop(Dest.DetailFull(it)) }
                        is Dest.DetailFull -> DetailScreen(
                            d.d, accent, lib, hist, ::pop,
                            onChapter = { ch, index ->
                                val asItem = MangaItem(d.d.slug, d.d.title, d.d.slug, d.d.coverUrl, d.d.coverFull, d.d.url)
                                hist = listOf(ReadingProgress(asItem, d.d, index)) + hist.filterNot { it.manga.url == d.d.url }
                                push(Dest.Reader(ch.url, ch.title.ifEmpty { "الفصل ${ch.number}" }, d.d, index, 1))
                            },
                            onContinue = { saved ->
                                push(Dest.Reader(saved.manga.chapters[saved.chapterIndex].url, "الفصل ${saved.manga.chapters[saved.chapterIndex].number}", saved.manga, saved.chapterIndex, saved.page))
                            }
                        )
                        is Dest.Reader -> key("${d.url}-${d.chapterIndex}-$readerRefresh") { ReaderScreen(
                            d.url, d.chTitle, d.manga, d.chapterIndex, accent, ::pop, d.page,
                            onProgress = { page, total, completed ->
                                hist = hist.map { p -> if (p.manga.url == d.manga.url) p.copy(manga = d.manga, item = MangaItem(d.manga.slug, d.manga.title, d.manga.slug, d.manga.coverUrl, d.manga.coverFull, d.manga.url), chapterIndex = d.chapterIndex, page = page, totalPages = total, completed = completed) else p }
                                val ch = d.manga.chapters.getOrNull(d.chapterIndex)
                                if (ch != null) appScope.launch { runCatching { CloudStore.saveProgress(MangaItem(d.manga.slug, d.manga.title, d.manga.slug, d.manga.coverUrl, d.manga.coverFull, d.manga.url), ch.url, ch.number, d.chapterIndex, page, total, completed) } }
                            },
                            onCfNeeded = { showCf = true }
                        ) }
                    }
                }

                if (cur is Dest.Reader && !CookieStore.cfSolved && !showCf) {
                    CfProbe(
                        onChallenge = { showCf = true },
                        onSolved = { cookies ->
                            CookieStore.cfCookies = cookies
                            CookieStore.cfSolved = true
                            readerRefresh++
                        }
                    )
                }

                // ── Drawer ────────────────────────────────────
                if (drawer) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Color.Black.copy(.65f))
                            .pointerInput(Unit) { detectTapGestures { drawer = false } }
                    )
                }
                AnimatedVisibility(
                    visible = drawer,
                    enter = fadeIn(tween(120)) + slideInHorizontally(tween(220)) { it },
                    exit = fadeOut(tween(100)) + slideOutHorizontally(tween(180)) { it },
                    label = "rtl-drawer"
                ) {
                    Drawer(accent, cur, { drawer = false }) { dest ->
                        when (dest) {
                            "home"     -> home()
                            "search"   -> push(Dest.Search)
                            "all"      -> push(Dest.AllManga)
                            "latest"   -> push(Dest.LatestManga)
                            "popular"  -> push(Dest.PopularManga)
                            "library"  -> { if (cur !is Dest.Library)  push(Dest.Library) else drawer = false }
                            "history"  -> { if (cur !is Dest.History)  push(Dest.History) else drawer = false }
                            "downloads" -> { if (cur !is Dest.Downloads) push(Dest.Downloads) else drawer = false }
                            "custom" -> { if (cur !is Dest.CustomLists) push(Dest.CustomLists) else drawer = false }
                            "profile"  -> { if (cur !is Dest.Profile)  push(Dest.Profile) else drawer = false }
                            "settings" -> { if (cur !is Dest.Settings) push(Dest.Settings) else drawer = false }
                        }
                    }
                }

                // ── CF Popup ──────────────────────────────────
                if (showCf) {
                    CfDialog(
                        onSolved = { c -> CookieStore.cfCookies = c; CookieStore.cfSolved = true
                            showCf = false; readerRefresh++; toast = "✓ تم التحقق بنجاح" },
                        onSkip   = { showCf = false }
                    )
                }

                // ── Toast ─────────────────────────────────────
                if (toast.isNotEmpty()) {
                    LaunchedEffect(toast) { kotlinx.coroutines.delay(3000L); toast = "" }
                    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), Alignment.BottomCenter) {
                        Surface(color = Green, shape = InputShape, shadowElevation = 8.dp) {
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
                                Text("أكمل التحقق للوصول إلى الفصول", color = TextSec, fontFamily = Font, fontSize = 12.sp)
                            }
                            if (loading) CircularProgressIndicator(
                                Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onSkip) { Text("تخطي", color = TextSec, fontFamily = Font, fontSize = 13.sp) }
                        }

                        if (loading) {
                            Row(Modifier.fillMaxWidth().background(Accent.copy(.08f))
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Info, null, tint = AccentLt, modifier = Modifier.size(15.dp))
                                Text("انتظر حتى يكتمل التحقق تلقائياً، وسيظهر التحدي فقط عند الحاجة",
                                    color = AccentLt, fontFamily = Font, fontSize = 12.sp, lineHeight = 17.sp)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CfProbe(onChallenge: () -> Unit, onSolved: (String) -> Unit) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView?, url: String?) {
                        val title = v?.title.orEmpty()
                        val challengeText = listOf(
                            "Just a moment", "Attention Required", "Checking your browser",
                            "لحظة واحدة", "جارٍ التحقق", "تحقق من أنك لست روبوتًا", "تحقق من الأمان"
                        )
                        fun handle(text: String) {
                            if (challengeText.any { text.contains(it, ignoreCase = true) }) {
                                // Give the hidden WebView time to finish the automatic check first.
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (!CookieStore.cfSolved) onChallenge()
                                }, 4500L)
                            } else if (url?.contains("mangalik.net") == true) {
                                CookieManager.getInstance().getCookie("https://mangalik.net")
                                    ?.takeIf { it.isNotBlank() }?.let(onSolved)
                            }
                        }
                        if (challengeText.any { title.contains(it, ignoreCase = true) }) handle(title)
                        else v?.evaluateJavascript("document.body ? document.body.innerText : ''") { body ->
                            handle(body.trim('"').replace("\\n", "\n"))
                        }
                    }
                }
                loadUrl(Scraper.cfChallengeUrl())
            }
        },
        modifier = Modifier.size(1.dp).alpha(0f)
    )
}

// ══════════════════════════════════════════════════════════════
// GOOGLE AUTH IN-APP DIALOG
// ══════════════════════════════════════════════════════════════
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GoogleAuthDialog(onSigned: (Uri) -> Unit, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    androidx.compose.ui.window.Dialog(
        onDismiss,
        androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Surface(color = Surface2, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.google_g), "Google", Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("تسجيل الدخول بـ Google", color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("أكمل تسجيل الدخول في النافذة أدناه", color = TextSec, fontSize = 12.sp)
                        }
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onDismiss) { Text("إلغاء", color = TextSec, fontSize = 13.sp) }
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, u: String?, f: android.graphics.Bitmap?) { loading = true }
                                override fun onPageFinished(v: WebView?, url: String?) { loading = false }
                                override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                                    val uri = r?.url ?: return false
                                    if (uri.scheme == "mangalore") { onSigned(uri); return true }
                                    return false
                                }
                            }
                            loadUrl(AuthStore.googleAuthUrl())
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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(top = 72.dp)) {

        // ── Shimmer or Error ──────────────────────────────────
        if (loading) {
            item {
                // Hero shimmer
                Box(Modifier.fillMaxWidth().height(252.dp).padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp)).background(staticPlaceholder()))
                Spacer(Modifier.height(14.dp).fillMaxWidth())
                // Tab shimmer
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(20.dp)).background(staticPlaceholder()))
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(20.dp)).background(staticPlaceholder()))
                }
                Spacer(Modifier.height(14.dp).fillMaxWidth())
                // Grid shimmer
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 1600.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(14.dp)) {
                    items(12) {
                        Column {
                            Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp)).background(staticPlaceholder()))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth(.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(staticPlaceholder()))
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth(.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(staticPlaceholder()))
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
                                .width(CoverWidthHero).height(CoverHeightHero)
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
                                Text("ابدأ القراءة", fontSize = 13.sp, color = Color.White,
                                    fontFamily = Font)
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
                    Surface(
                        color  = if (sel) accent else Surface3,
                        shape  = RoundedCornerShape(20.dp),
                        border = if (!sel) BorderStroke(.5.dp, Border) else null,
                        modifier = Modifier.weight(1f).clickable { tab = i }
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
                                Column(Modifier.width(CoverWidthXs)) {
                                    Box(Modifier.size(CoverWidthXs, CoverHeightXs).clip(RoundedCornerShape(12.dp))) {
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
                            Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp))) {
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
                                Text(formatRelativeDate(m.chapterDate), color = TextDim, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
            item { Spacer(Modifier.height(48.dp)) }
        }
        Surface(color = Surface2, shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onMenu) { Icon(Icons.Default.Menu, null, tint = TextPri) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Mangalore",
                        color = TextPri,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Font,
                        letterSpacing = (-0.5).sp
                    )
                }
                IconButton(onSearch) { Icon(Icons.Default.Search, null, tint = TextPri) }
            }
        }
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
                    textStyle = LocalTextStyle.current.copy(fontFamily = Font),
                    placeholder = { Text("ابحث عن مانجا أو مانهوا...", color = TextDim, fontSize = 13.sp,
                        fontFamily = Font) },
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
                    shape = InputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(cats) { cat ->
                        val sel = activeCat == cat
                        FilterChip(sel,
                            { activeCat = if (sel) null else cat
                              if (!sel) search(cat) else { res = emptyList(); sub = "" } },
                            label = { Text(cat, fontSize = 12.sp, fontFamily = Font) },
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
                        Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp)).background(staticPlaceholder()))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(.8f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(staticPlaceholder()))
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
                            Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp))) {
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

@Composable
private fun MangaListScreen(title: String, accent: Color, onBack: () -> Unit, loadPage: suspend (Int) -> List<MangaItem>, onPick: (MangaItem) -> Unit) {
    var items by remember { mutableStateOf<List<MangaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    fun load() { loading = true; scope.launch {
        val first = loadPage(1)
        items = first.distinctBy { it.url }
        page = 1
        hasMore = first.size >= 10
        loading = false
    } }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(gridState) {
        snapshotFlow {
            listOf(
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                gridState.layoutInfo.totalItemsCount,
                if (loading) 1 else 0,
                if (loadingMore) 1 else 0,
                if (hasMore) 1 else 0
            )
        }.collect { trigger ->
                val lastVisible = trigger[0]
                val total = trigger[1]
                if (trigger[2] == 0 && trigger[3] == 0 && trigger[4] == 1 && total > 0 && lastVisible >= total - 3) {
                    loadingMore = true
                    val next = loadPage(page + 1).filterNot { candidate -> items.any { it.url == candidate.url } }
                    if (next.isNotEmpty()) {
                        items = items + next
                        page += 1
                        hasMore = next.size >= 10
                    } else {
                        hasMore = false
                    }
                    loadingMore = false
                }
            }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(title, accent, onBack)
        if (loading) {
            LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(9) { Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp)).background(staticPlaceholder())) }
            }
        } else if (items.isEmpty()) {
            EmptyState(Icons.Default.WifiOff, "لا توجد أعمال", "تعذّر تحميل القائمة")
        } else {
            LazyVerticalGrid(state = gridState, columns = GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(items, key = { it.id }) { m -> SpringCard({ onPick(m) }) { Column { Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp))) { Img(m.coverUrl, Modifier.fillMaxSize()) }; Spacer(Modifier.height(6.dp)); Text(m.title, color = TextPri, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontFamily = Font) } } }
                if (loadingMore) item(span = { GridItemSpan(3) }) {
                    Box(Modifier.fillMaxWidth().padding(18.dp), Alignment.Center) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                }
                item(span = { GridItemSpan(3) }) { Text("${items.size} عمل", color = TextDim, fontSize = 11.sp, fontFamily = Font, modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center) }
            }
        }
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
                // Static placeholder; the only motion is the single screen transition.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(280.dp).background(Surface2))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(.7f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(Surface3))
                    Box(Modifier.fillMaxWidth(.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Surface3))
                    Spacer(Modifier.height(6.dp))
                    repeat(5) { Box(Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(4.dp)).background(Surface3)) }
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
    history: List<ReadingProgress>, onBack: () -> Unit,
    onChapter: (ChapterItem, Int) -> Unit, onContinue: (ReadingProgress) -> Unit
) {
    val context = LocalContext.current
    val cloudScope = rememberCoroutineScope()
    var tab   by remember { mutableStateOf(0) }
    var newestFirst by remember { mutableStateOf(true) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedChapters by remember { mutableStateOf(setOf<Int>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(d.url) {
        comments = readSavedComments(context, d.url)
        runCatching { AuthStore.loadComments(d.url) }.onSuccess { remote ->
            if (remote.isNotEmpty()) {
                comments = remote
                saveComments(context, d.url, remote)
            }
        }
    }
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
                Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)) {
                    IconButton({ showMoreMenu = true }) { Icon(Icons.Default.MoreVert, "المزيد", tint = Color.White) }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(text = { Text(if (inLib) "إزالة من المكتبة" else "حفظ في المكتبة") }, onClick = {
                            if (inLib) { lib.removeAll { it.url == d.url }; cloudScope.launch { runCatching { CloudStore.removeLibrary(d.url) } } }
                            else { lib.add(0, asItem); cloudScope.launch { runCatching { CloudStore.addLibrary(asItem) } } }
                            showMoreMenu = false
                        }, leadingIcon = { Icon(if (inLib) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd, null) })
                        DropdownMenuItem(text = { Text("تحديد فصول للتنزيل") }, onClick = { selectionMode = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.Checklist, null) })
                        DropdownMenuItem(text = { Text("تنزيل فصول") }, onClick = { showDownloadDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.Download, null) })
                        DropdownMenuItem(text = { Text("إضافة إلى قائمة") }, onClick = { showListPicker = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) })
                    }
                }

                // Cover + info at bottom
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Cover (right in RTL = first)
                    Box(Modifier.width(CoverWidthMd).height(CoverHeightMd).clip(RoundedCornerShape(12.dp))
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
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                    if (d.chapters.isNotEmpty()) {
                        val saved = history.filter { it.manga.url == d.url && !it.completed }
                            .maxByOrNull { it.chapterIndex }
                        Button({
                            if (saved != null && saved.chapterIndex in d.chapters.indices) onContinue(saved)
                            else onChapter(d.chapters.last(), d.chapters.lastIndex)
                        }, Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = TextPri),
                            shape = ButtonShape) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (saved != null) "أكمل الفصل ${d.chapters[saved.chapterIndex].number} · ص${saved.page}" else "ابدأ القراءة", fontSize = 13.sp, fontFamily = Font)
                        }
                        if (d.chapters.size > 1) {
                                Button({ onChapter(d.chapters.first(), 0) }, Modifier.defaultMinSize(minWidth = 112.dp, minHeight = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Surface3),
                                shape = ButtonShape) {
                                Icon(Icons.Default.LastPage, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("آخر فصل", fontSize = 13.sp, color = TextPri, fontFamily = Font)
                            }
                        }
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
                listOf("التفاصيل","الفصول (${d.chapters.size})","التعليقات (${comments.size})").forEachIndexed { i, lbl ->
                    val active = tab == i
                    Column(Modifier.weight(1f).clickable { tab = i },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(lbl, color = if (active) accent else TextSec, fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(6.dp))
                        val w = if (active) .5f else 0f
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
                Surface(color = Surface2, shape = InputShape,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Column {
                        val na = "غير متوفر"
                        IRow("المؤلف", d.author.ifEmpty { na }, dimValue = d.author.isEmpty())
                        IRow("الرسام", d.artist.ifEmpty { na }, dimValue = d.artist.isEmpty())
                        IRow("نوع العمل", d.origin.ifEmpty { na }, dimValue = d.origin.isEmpty())
                        IRow("السنة", d.releaseYear.ifEmpty { na }, dimValue = d.releaseYear.isEmpty())
                        IRow("الحالة", d.status.ifEmpty { na }, last = true, dimValue = d.status.isEmpty())
                    }
                }
            }
            if (d.genres.isNotEmpty()) {
                item {
                    FlowRow(Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)) {
                        d.genres.forEach { GChip(it, accent) }
                    }
                }
            }
        } else if (tab == 2) {
            // Comment input
            item {
                Surface(color = Surface2, shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // User avatar
                            Box(Modifier.size(36.dp).clip(CircleShape)
                                .background(accent.copy(.2f)).border(1.dp, accent.copy(.35f), CircleShape),
                                Alignment.Center) {
                                Text(AuthStore.displayName.firstOrNull()?.uppercase() ?: "أ",
                                    color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(AuthStore.displayName.ifBlank { "قارئ مانجالور" },
                                color = TextSec, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedTextField(
                            value = commentText, onValueChange = { commentText = it },
                            placeholder = { Text("شارك رأيك بهذا العمل...", fontFamily = Font, color = TextDim) },
                            modifier = Modifier.fillMaxWidth(), maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent, unfocusedBorderColor = Border,
                                focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                                unfocusedContainerColor = Surface3, focusedContainerColor = Surface3
                            ),
                            shape = InputShape
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                enabled = commentText.isNotBlank(),
                                onClick = {
                                    val value = commentText.trim()
                                    if (value.isNotEmpty()) {
                                        comments = comments + value
                                        commentText = ""
                                        saveComments(context, d.url, comments)
                                        cloudScope.launch { runCatching { AuthStore.syncComments(d.url, comments) } }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                shape = ButtonShape,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("نشر", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Empty state
            if (comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).clip(CircleShape).background(Surface3), Alignment.Center) {
                                Icon(Icons.Default.ModeComment, null, tint = TextDim.copy(.5f),
                                    modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("لا توجد تعليقات بعد", color = TextSec, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text("كن أول من يشارك رأيه", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Comments list
            itemsIndexed(comments.asReversed()) { idx, comment ->
                val commentAccent = Palette[(idx * 3 + 7) % Palette.size]
                val initial = AuthStore.displayName.firstOrNull()?.uppercase() ?: "أ"
                Surface(
                    color = Surface2, shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(.5.dp, Border),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Comment header
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(34.dp).clip(CircleShape)
                                .background(commentAccent.copy(.2f))
                                .border(.5.dp, commentAccent.copy(.4f), CircleShape),
                                Alignment.Center) {
                                Text(initial, color = commentAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(AuthStore.displayName.ifBlank { "قارئ مانجالور" },
                                    color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("قارئ مانجالور", color = TextDim, fontSize = 10.sp)
                            }
                            IconButton(
                                onClick = {
                                    comments = comments.filterNot { it == comment }
                                    saveComments(context, d.url, comments)
                                    cloudScope.launch { runCatching { AuthStore.syncComments(d.url, comments) } }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = TextDim.copy(.6f),
                                    modifier = Modifier.size(14.dp))
                            }
                        }
                        // Comment body
                        Text(comment, color = TextPri, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = newestFirst, onClick = { newestFirst = true },
                        label = { Text("الأحدث", fontFamily = Font, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(.18f), selectedLabelColor = accent,
                            selectedLeadingIconColor = accent)
                    )
                    FilterChip(
                        selected = !newestFirst, onClick = { newestFirst = false },
                        label = { Text("الأقدم", fontFamily = Font, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(.18f), selectedLabelColor = accent,
                            selectedLeadingIconColor = accent)
                    )
                    Spacer(Modifier.weight(1f))
                    if (selectionMode) {
                        if (selectedChapters.isNotEmpty()) {
                            TextButton(onClick = {
                                LocalDownloads.enqueue(context, d, selectedChapters.sorted())
                                selectedChapters = emptySet(); selectionMode = false
                            }) {
                                Icon(Icons.Default.Download, null, tint = accent, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تنزيل (${selectedChapters.size})", color = accent, fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = {
                            selectedChapters = if (selectedChapters.size == d.chapters.size) emptySet() else d.chapters.indices.toSet()
                        }) {
                            Text(if (selectedChapters.size == d.chapters.size) "إلغاء الكل" else "تحديد الكل", color = accent, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectionMode = false; selectedChapters = emptySet() },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextSec, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            val orderedChapters = if (newestFirst) d.chapters else d.chapters.asReversed()
            itemsIndexed(orderedChapters) { displayIndex, ch ->
                val originalIndex = if (newestFirst) displayIndex else d.chapters.lastIndex - displayIndex
                val progress = history.firstOrNull { it.manga.url == d.url && it.chapterIndex == originalIndex }
                val progressLabel = when {
                    progress?.completed == true -> "تمت المشاهدة"
                    progress != null -> "أكمل من الصفحة ${progress.page}"
                    ch.date.isNotEmpty() -> formatRelativeDate(ch.date).ifBlank { "التاريخ غير متوفر" }
                    else -> "التاريخ غير متوفر"
                }
                val progressColor = when {
                    progress?.completed == true -> Green
                    progress != null -> accent
                    else -> TextDim
                }
                val isSelected = selectedChapters.contains(originalIndex)
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .pointerInput(selectionMode) {
                            detectTapGestures(
                                onLongPress = {
                                    selectionMode = true
                                    selectedChapters = selectedChapters + originalIndex
                                },
                                onTap = {
                                    if (selectionMode) {
                                        selectedChapters = if (isSelected)
                                            selectedChapters - originalIndex
                                        else
                                            selectedChapters + originalIndex
                                        if (selectedChapters.isEmpty()) selectionMode = false
                                    } else {
                                        onChapter(ch, originalIndex)
                                    }
                                }
                            )
                        },
                    color = if (isSelected && selectionMode) accent.copy(.10f) else Surface2,
                    shape = InputShape,
                    border = BorderStroke(1.dp, if (isSelected && selectionMode) accent.copy(.5f) else Border)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.width(CoverWidthCh).height(CoverHeightCh).clip(RoundedCornerShape(8.dp))) {
                            Img(d.coverUrl, Modifier.fillMaxSize())
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.82f))))
                                .padding(bottom = 3.dp, top = 8.dp), Alignment.Center) {
                                Text(ch.number, color = Color.White, fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(ch.title.ifEmpty { "الفصل ${ch.number}" }, color = TextPri, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(progressLabel, color = progressColor, fontSize = 11.sp,
                                fontWeight = if (progress != null) FontWeight.Medium else FontWeight.Normal)
                        }
                        if (selectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedChapters = if (checked) selectedChapters + originalIndex else selectedChapters - originalIndex
                                    if (selectedChapters.isEmpty()) selectionMode = false
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = TextDim)
                            )
                        } else {
                            IconButton(
                                onClick = { LocalDownloads.enqueue(context, d, listOf(originalIndex)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Download, null, tint = TextDim, modifier = Modifier.size(18.dp))
                            }
                        }
                        Icon(
                            if (progress?.completed == true) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            null, tint = progressColor, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
    if (showListPicker) {
        val names = CustomListsStore.names(context)
        AlertDialog(onDismissRequest = { showListPicker = false }, containerColor = Surface2,
            title = { Text("إضافة إلى قائمة", color = TextPri, fontFamily = Font, fontWeight = FontWeight.Bold) },
            text = {
                if (names.isEmpty()) Text("لا توجد قوائم مخصصة. أنشئ قائمة من قسم قوائمي أولاً.", color = TextSec, fontFamily = Font)
                else Column { names.forEach { name -> TextButton({
                    CustomListsStore.add(context, name, asItem)
                    if (AuthStore.hasSession()) cloudScope.launch { runCatching { CloudStore.addCustomList(name, asItem) } }
                    showListPicker = false
                }, modifier = Modifier.fillMaxWidth()) { Text(name, color = TextPri, fontFamily = Font) } } }
            },
            confirmButton = { TextButton({ showListPicker = false }) { Text("إغلاق", color = TextSec, fontFamily = Font) } })
    }
    if (showDownloadDialog) {
        var from by remember { mutableStateOf("1") }
        var to by remember { mutableStateOf(d.chapters.size.toString()) }
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            containerColor = Surface2,
            titleContentColor = TextPri,
            textContentColor = TextSec,
            title = { Text("تنزيل الفصول", fontFamily = Font, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (selectedChapters.isEmpty()) "حدد فصلًا واحدًا أو نطاقًا للتنزيل على الجهاز" else "تم تحديد ${selectedChapters.size} فصل — يمكنك تعديل النطاق أو تنزيل المحدد", color = TextSec, fontSize = 12.sp, fontFamily = Font)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(from, { from = it }, label = { Text("من") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(to, { to = it }, label = { Text("إلى") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val rangeStart = ((from.toIntOrNull() ?: 1) - 1).coerceIn(0, d.chapters.lastIndex)
                    val rangeEnd = ((to.toIntOrNull() ?: from.toIntOrNull() ?: 1) - 1).coerceIn(rangeStart, d.chapters.lastIndex)
                    val indexes = if (selectedChapters.isNotEmpty()) selectedChapters.sorted() else (rangeStart..rangeEnd).toList()
                    if (d.chapters.isNotEmpty()) LocalDownloads.enqueue(context, d, indexes)
                    selectedChapters = emptySet()
                    showDownloadDialog = false
                }) { Text("تنزيل", color = accent) }
            },
            dismissButton = { TextButton(onClick = { showDownloadDialog = false }) { Text("إلغاء") } }
        )
    }
}

// ══════════════════════════════════════════════════════════════
// READER
// ══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    chUrl: String, chTitle: String, manga: MangaDetail, chapterIndex: Int,
    accent: Color, onBack: () -> Unit, initialPage: Int = 1,
    onProgress: (Int, Int, Boolean) -> Unit, onCfNeeded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    var blocks by remember { mutableStateOf<List<Pair<Int, List<String>>>>(emptyList()) }
    var loadedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var state by remember { mutableStateOf(0) }
    var bars by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var readingMode by remember { mutableStateOf("طولي") }
    var horizontalDirection by remember { mutableStateOf("يمين لليسار") }
    val activeChapterBlock by remember { derivedStateOf {
        if (blocks.isEmpty()) 0 else {
            val visible = listState.firstVisibleItemIndex
            var cursor = 0
            var found = 0
            blocks.forEachIndexed { index, block ->
                val count = if (readingMode == "عرضي") block.second.size else block.second.size + 1
                if (visible >= cursor) found = index
                cursor += count
            }
            found.coerceIn(0, blocks.lastIndex)
        }
    } }
    val currentPage by remember { derivedStateOf {
        if (blocks.isEmpty()) 1 else {
            val visible = listState.firstVisibleItemIndex
            var cursor = 0
            blocks.forEachIndexed { index, block ->
                val offset = if (readingMode == "عرضي") 0 else 1
                val end = cursor + offset + block.second.size
                if (visible < end || index == blocks.lastIndex) return@derivedStateOf (visible - cursor - offset + 1).coerceIn(1, block.second.size)
                cursor += offset + block.second.size
            }
            1
        }
    } }
    val totalPages by remember { derivedStateOf { blocks.getOrNull(activeChapterBlock)?.second?.size ?: 0 } }
    var showChapterList by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var zoomImages by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }
    var showPageNumber by remember { mutableStateOf(true) }
    var autoScrollEnabled by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableStateOf(3) }
    var showSpeedPicker by remember { mutableStateOf(false) }
    var failedImageUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var retryingAll by remember { mutableStateOf(false) }
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }
    val displayedChapterNum = remember(blocks, activeChapterBlock) { blocks.getOrNull(activeChapterBlock)?.first?.let { manga.chapters.getOrNull(it)?.number } ?: "" }
    val displayedChapterTitle = remember(blocks, activeChapterBlock) { blocks.getOrNull(activeChapterBlock)?.first?.let { i -> manga.chapters.getOrNull(i)?.let { c -> if (c.title.isNotEmpty() && c.title != "الفصل ${c.number}") c.title else "" } } ?: chTitle }

    fun loadChapter(index: Int, reset: Boolean = false) {
        if (index !in manga.chapters.indices || (!reset && index in loadedIndices)) return
        if (reset) { state = 0; retryingAll = true }
        scope.launch {
            val local = LocalDownloads.localImages(context, manga.chapters[index].url)
            val (remoteImages, cf) = if (local.isNotEmpty()) Pair(local, false) else Scraper.fetchChapterImages(manga.chapters[index].url)
            val images = remoteImages
            when {
                cf -> { state = 2; retryingAll = false }
                images.isEmpty() -> { if (reset) state = 3; retryingAll = false }
                else -> {
                    blocks = if (reset) listOf(index to images) else blocks + (index to images)
                    loadedIndices = if (reset) setOf(index) else loadedIndices + index
                    state = 1; retryingAll = false; failedImageUrls = emptySet()
                }
            }
        }
    }
    fun retry() { blocks = emptyList(); loadedIndices = emptySet(); failedImageUrls = emptySet(); loadChapter(chapterIndex, true) }
    LaunchedEffect(chUrl, chapterIndex) { retry() }
    LaunchedEffect(state, initialPage) { if (state == 1 && initialPage > 1) listState.scrollToItem(initialPage.coerceAtMost((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))) }
    LaunchedEffect(currentPage, totalPages, state) { if (state == 1 && totalPages > 0) onProgress(currentPage, totalPages, currentPage >= totalPages) }
    LaunchedEffect(autoScrollEnabled, autoScrollSpeed) {
        if (!autoScrollEnabled) return@LaunchedEffect
        val delayMs = when (autoScrollSpeed) { 1 -> 120L; 2 -> 80L; 3 -> 50L; 4 -> 25L; else -> 12L }
        while (autoScrollEnabled) { delay(delayMs); if (!listState.canScrollForward) autoScrollEnabled = false else listState.scrollBy(1f) }
    }

    Box(Modifier.fillMaxSize().background(Black)) {
        when (state) {
            0 -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = accent); Spacer(Modifier.height(14.dp)); Text("جاري تحميل الفصل...", color = TextSec) } }
            // Keep the reader surface quiet while the hidden Cloudflare probe runs.
            // If a manual challenge is actually required, CfProbe opens CfDialog automatically.
            2 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(58.dp), strokeWidth = 5.dp)
            }
            3 -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.CloudOff, null, tint = Red, modifier = Modifier.size(52.dp)); Text("تعذّر تحميل الفصل", color = TextSec); Button(::retry, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("إعادة المحاولة", color = Color.White) } } }
            else -> {
                val readerContent: LazyListScope.() -> Unit = {
                blocks.forEachIndexed { position, (index, images) ->
                    item(key = "chapter-header-$index") { Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent.copy(.22f), accent.copy(.06f), Color.Transparent))).padding(horizontal = 16.dp, vertical = 14.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.width(CoverWidthCh).height(CoverHeightCh).clip(RoundedCornerShape(8.dp))) { Img(manga.coverUrl, Modifier.fillMaxSize()); Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.82f)))).padding(bottom = 3.dp, top = 8.dp), Alignment.Center) { Text(manga.chapters[index].number, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) } }; Column { Text("الفصل ${manga.chapters[index].number}", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold); if (displayedChapterTitle.isNotEmpty()) Text(displayedChapterTitle, color = TextSec, fontSize = 12.sp) } } } }
                    itemsIndexed(images, key = { i, url -> "$index-$i-$url" }) { _, url ->
                        SubcomposeAsyncImage(model = ImageRequest.Builder(LocalContext.current).data(if (url.startsWith("/")) File(url) else url).addHeader("Referer", "https://mangalik.net/").addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.0.0").apply { if (CookieStore.has()) addHeader("Cookie", CookieStore.cfCookies) }
                            .listener(onError = { _, result -> Log.e("MangaloreImages", "Image failed url=$url", result.throwable) })
                            .crossfade(false).build(), contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().graphicsLayer {
                                val zoom = if (zoomImages) 1.15f else 1f
                                scaleX = zoom; scaleY = zoom
                            }, loading = { Box(Modifier.fillMaxWidth().height(270.dp), Alignment.Center) { CircularProgressIndicator(color = accent.copy(.5f), modifier = Modifier.size(30.dp), strokeWidth = 2.dp) } }, error = { failedImageUrls = failedImageUrls + url; Box(Modifier.fillMaxWidth().height(90.dp).background(Surface2), Alignment.Center) { Icon(Icons.Default.BrokenImage, null, tint = TextDim, modifier = Modifier.size(30.dp)) } })
                    }
                    if (position == blocks.lastIndex) item(key = "chapter-loader-$index") {
                        LaunchedEffect(index, blocks.size) { if (readingMode == "طولي") loadChapter(index - 1) }
                        if (index > 0) LinearProgressIndicator(Modifier.fillMaxWidth().padding(18.dp), color = accent)
                        else Text("انتهت الفصول", color = TextDim, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
                }
                val horizontal = readingMode == "عرضي"
                if (horizontal) {
                    val horizontalImages = blocks.flatMap { it.second }
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        userScrollEnabled = false,
                        reverseLayout = false
                    ) {
                        itemsIndexed(horizontalImages, key = { i, url -> "horizontal-$i-$url" }) { page, url ->
                            Box(
                                Modifier.fillParentMaxWidth().fillMaxHeight().pointerInput(page, horizontalImages.size, horizontalDirection) {
                                    detectTapGestures { tap ->
                                        val edge = size.width * 0.28f
                                        val onLeft = tap.x < edge
                                        val onRight = tap.x > size.width - edge
                                        if (!onLeft && !onRight) {
                                            bars = !bars
                                        } else {
                                            val forward = if (horizontalDirection == "يمين لليسار") onLeft else onRight
                                            val target = (page + if (forward) 1 else -1).coerceIn(0, horizontalImages.lastIndex)
                                            scope.launch { listState.animateScrollToItem(target) }
                                        }
                                    }
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(url)
                                        .addHeader("Referer", "https://mangalik.net/")
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.0.0")
                                        .apply { if (CookieStore.has()) addHeader("Cookie", CookieStore.cfCookies) }
                                        .crossfade(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(.92f),
                                    loading = { CircularProgressIndicator(color = accent.copy(.6f)) },
                                    error = { failedImageUrls = failedImageUrls + url; Icon(Icons.Default.BrokenImage, null, tint = TextDim, modifier = Modifier.size(42.dp)) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { bars = !bars },
                        state = listState,
                        content = readerContent
                    )
                }
            }
        }
        AnimatedVisibility(bars && state == 1, enter = fadeIn(tween(140)) + slideInVertically(tween(160)) { -it }, exit = fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it }, label = "reader-top-bar") {
            Surface(color = Color.Black.copy(.92f)) { Column { Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }; Column(Modifier.weight(1f).padding(horizontal = 4.dp)) { Text(manga.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (displayedChapterNum.isNotEmpty()) "الفصل $displayedChapterNum${if (displayedChapterTitle.isNotEmpty()) " • $displayedChapterTitle" else ""}" else chTitle, color = Color.White.copy(.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; if (showPageNumber && totalPages > 0) Text("$currentPage / $totalPages", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp)); IconButton(onClick = { showReaderSettings = true }) { Icon(Icons.Default.Settings, "إعدادات القارئ", tint = Color.White) }; IconButton(onClick = { showChapterList = true }) { Icon(Icons.Default.List, "قائمة الفصول", tint = Color.White) } } } }
        }
        AnimatedVisibility(visible = bars && state == 1, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn(tween(140)) + slideInVertically(tween(160)) { it }, exit = fadeOut(tween(100)) + slideOutVertically(tween(120)) { it }, label = "reader-bottom-bar") { Surface(color = Color.Black.copy(.92f)) { Row(Modifier.navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("تمرير تلقائي", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f)); IconButton(onClick = { autoScrollEnabled = !autoScrollEnabled }) { Icon(if (autoScrollEnabled) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = accent) }; IconButton(onClick = { showSpeedPicker = !showSpeedPicker }) { Icon(Icons.Default.Timer, null, tint = Color.White) }; if (failedImageUrls.isNotEmpty()) IconButton(onClick = { retry() }) { Icon(Icons.Default.Sync, "إعادة الجلب", tint = Red) } } } }
        if (showSpeedPicker) Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp), color = Surface3, shape = InputShape) { Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..5).forEach { speed -> FilterChip(autoScrollSpeed == speed, { autoScrollSpeed = speed; showSpeedPicker = false }, label = { Text("$speed") }) } } }
        if (showReaderSettings) {
            ModalBottomSheet(onDismissRequest = { showReaderSettings = false }, containerColor = Color.Black, contentColor = Color.White, dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }) {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text("إعدادات القارئ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    Text("التنقل", color = accent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Text("اتجاه القراءة", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                    Text("طولي أو عرضي", color = TextSec, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(Color.Black, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("طولي", "عرضي").forEach { option ->
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (readingMode == option) accent else Color.Transparent).clickable { readingMode = option }.padding(vertical = 12.dp), Alignment.Center) { Text(option, color = Color.White, fontSize = 14.sp) }
                        }
                    }
                    if (readingMode == "عرضي") {
                        D2()
                        Text("الاتجاه التقليدي", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        Text("للقراءة العرضية", color = TextSec, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(Color.Black, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("يمين لليسار", "يسار لليمين").forEach { option ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (horizontalDirection == option) accent else Color.Transparent).clickable { horizontalDirection = option }.padding(vertical = 12.dp), Alignment.Center) { Text(option, color = Color.White, fontSize = 13.sp) }
                            }
                        }
                    }
                    D2()
                    Text("أثناء القراءة", color = accent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    SToggle("التكبير", "تفعيل تكبير الصور", zoomImages) { zoomImages = it }
                    D2()
                    SToggle("إبقاء الشاشة مضاءة", "منع إطفاء الشاشة أثناء القراءة", keepScreenOn) { keepScreenOn = it }
                    D2()
                    SToggle("رقم الصفحة", "إظهار رقم الصفحة الحالية أسفل الشاشة", showPageNumber) { showPageNumber = it }
                }
            }
        }
        if (showChapterList) {
            var newestFirst by remember { mutableStateOf(true) }
            ModalBottomSheet(onDismissRequest = { showChapterList = false }, containerColor = Color.Black, contentColor = Color.White, dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }) {
                Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text("قائمة الفصول", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Font, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(newestFirst, { newestFirst = true }, label = { Text("الأحدث", fontFamily = Font) }, leadingIcon = { Icon(Icons.Default.ArrowDownward, null) })
                        FilterChip(!newestFirst, { newestFirst = false }, label = { Text("الأقدم", fontFamily = Font) }, leadingIcon = { Icon(Icons.Default.ArrowUpward, null) })
                    }
                    LazyColumn(Modifier.heightIn(max = 520.dp)) {
                        val chapters = if (newestFirst) manga.chapters.asReversed() else manga.chapters
                        itemsIndexed(chapters) { _, ch ->
                            val i = manga.chapters.indexOf(ch)
                            Row(Modifier.fillMaxWidth().clickable { showChapterList = false; loadChapter(i, true) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("الفصل ${ch.number}", color = Color.White, fontSize = 14.sp, fontFamily = Font); if (ch.title.isNotEmpty()) Text(ch.title, color = Color.White.copy(.62f), fontSize = 11.sp, fontFamily = Font) }
                                if (i in loadedIndices) Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                            }
                            HorizontalDivider(color = Color.White.copy(.10f))
                        }
                    }
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
                            Box(Modifier.fillMaxWidth().aspectRatio(CoverAspect).clip(RoundedCornerShape(12.dp))) {
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
private fun HistoryScreen(accent:Color, hist:List<ReadingProgress>,
    onBack:()->Unit, onPick:(ReadingProgress)->Unit, onClear:()->Unit) {
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
                        Box(Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(10.dp))) {
                            Img(h.item.coverUrl, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(h.item.title, color=TextPri, fontSize=14.sp, maxLines=1,
                                overflow=TextOverflow.Ellipsis, fontWeight=FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            val chapter = h.manga.chapters.getOrNull(h.chapterIndex)
                            Text("الفصل ${chapter?.number ?: "?"} · الصفحة ${h.page}", color=accent, fontSize=12.sp, fontFamily = Font)
                            if (h.completed) Text("تمت المشاهدة", color = Green, fontSize = 10.sp, fontFamily = Font)
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
private fun DownloadsScreen(accent: Color, onBack: () -> Unit, onOpen: (DownloadGroup) -> Unit) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf(LocalDownloads.groups(context)) }
    LaunchedEffect(Unit) { while (true) { rows = LocalDownloads.groups(context); delay(1000) } }
    Column(Modifier.fillMaxSize()) {
        TopBar("التنزيلات", accent, onBack)
        if (rows.isEmpty()) {
            EmptyState(Icons.Default.Download, "لا توجد تنزيلات", "حدد فصولًا من صفحة المانجا لتنزيلها على جهازك")
        } else {
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.key }) { group ->
                    Surface(color = Surface2, shape = InputShape, border = BorderStroke(1.dp, Border), modifier = Modifier.clickable { onOpen(group) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(54.dp, 78.dp).clip(RoundedCornerShape(10.dp))) { Img(group.cover, Modifier.fillMaxSize()) }
                            Column(Modifier.weight(1f)) {
                                Text(group.title, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${group.done} فصل محمّل", color = TextSec, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                                LinearProgressIndicator(progress = { if (group.total > 0) group.done.toFloat() / group.total else 0f }, color = accent, trackColor = Surface3, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(accent: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(Triple(0, 0, 0)) }
    var editNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var savingName by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(AuthStore.displayName.ifBlank { "قارئ مانجالور" }) }
    var avatarUrl by remember { mutableStateOf(AuthStore.avatarUrl) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.toString()?.let {
            avatarUrl = it
            AuthStore.updateAvatar(context, it)
            scope.launch { runCatching { AuthStore.syncAvatar(it) } }
        }
    }

    LaunchedEffect(Unit) { runCatching { CloudStore.stats() }.onSuccess { stats = it } }

    Column(Modifier.fillMaxSize()) {
        TopBar("الملف الشخصي", accent, onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Avatar + name row
            item {
                Surface(color = Surface2, shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Border)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(70.dp).clip(RoundedCornerShape(18.dp))
                                .background(Surface3).border(1.dp, Border, RoundedCornerShape(18.dp))
                                .clickable { avatarPicker.launch("image/*") }, Alignment.Center
                        ) {
                            if (avatarUrl.isNotBlank()) AsyncImage(
                                model = avatarUrl, contentDescription = "صورة المستخدم",
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            ) else Text(
                                displayName.firstOrNull()?.uppercase() ?: "م",
                                color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold
                            )
                            Box(Modifier.align(Alignment.BottomEnd).size(22.dp).background(Color.Black.copy(.55f), CircleShape), Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(displayName, color = TextPri, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("عضو في مانجالور", color = TextSec, fontSize = 12.sp)
                        }
                        // Edit name button
                        IconButton(onClick = { nameInput = AuthStore.displayName; editNameDialog = true },
                            modifier = Modifier.size(38.dp)) {
                            Surface(color = accent.copy(.15f), shape = ButtonShape) {
                                Icon(Icons.Default.Edit, null, tint = accent,
                                    modifier = Modifier.padding(7.dp).size(16.dp))
                            }
                        }
                    }
                }
            }

            // Stats section
            item {
                Text("إحصائيات القراءة", color = TextSec, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
            }
            item {
                Surface(color = Surface2, shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Border)) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("الفصول المكتملة", color = TextSec, fontSize = 13.sp)
                                Text("${stats.first}", color = TextPri, fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                                .background(accent.copy(.15f)), Alignment.Center) {
                                Icon(Icons.Default.BarChart, null, tint = accent,
                                    modifier = Modifier.size(28.dp))
                            }
                        }
                        HorizontalDivider(color = Border)
                        Row(Modifier.fillMaxWidth()) {
                            ProfileStat("جاري القراءة", stats.second.toString(), Modifier.weight(1f))
                            VerticalDivider(Modifier.height(64.dp), color = Border)
                            ProfileStat("المكتبة", stats.third.toString(), Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Border)
                        Row(Modifier.fillMaxWidth()) {
                            ProfileStat("قراءة لاحقًا", "0", Modifier.weight(1f))
                            VerticalDivider(Modifier.height(64.dp), color = Border)
                            ProfileStat("المانجا", stats.third.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Text("آخر القراءات", color = TextSec, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
            }
            item {
                Surface(color = Surface2, shape = InputShape,
                    border = BorderStroke(1.dp, Border)) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, null, tint = TextDim.copy(.5f),
                                modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("ستظهر آخر قراءاتك هنا", color = TextDim, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Edit name dialog
    if (editNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!savingName) editNameDialog = false },
            containerColor = Surface2,
            titleContentColor = TextPri,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Edit, null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("تعديل الاسم", fontFamily = Font, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                        focusedLabelColor = accent, unfocusedLabelColor = TextSec,
                        unfocusedContainerColor = Surface3, focusedContainerColor = Surface3
                    ),
                    shape = InputShape,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = !savingName && nameInput.isNotBlank(),
                    onClick = {
                        savingName = true
                        scope.launch {
                            runCatching { AuthStore.updateDisplayName(context, nameInput.trim()) }
                                .onSuccess {
                                    displayName = nameInput.trim()
                                    editNameDialog = false
                                }
                            savingName = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = ButtonShape
                ) {
                    if (savingName) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton({ if (!savingName) editNameDialog = false }) { Text("إلغاء", color = TextSec) }
            }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPri, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSec, fontSize = 11.sp)
    }
}

// ══════════════════════════════════════════════════════════════
// SETTINGS
// ══════════════════════════════════════════════════════════════
@Composable
private fun SettingsScreen(accent:Color, amoled:Boolean, onAmoled:(Boolean)->Unit, onAccent:(Color)->Unit, onBack:()->Unit, onSignedOut:()->Unit, onDeleted:()->Unit) {
    var picker   by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var finalDeleteDialog by remember { mutableStateOf(false) }
    var deleteName by remember { mutableStateOf("") }
    var deleteChecked by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

        item { SecLabel("الحساب", accent) }
        item {
            SCard {
                SAction("تسجيل الخروج", "الخروج من الحساب على هذا الجهاز", Icons.Default.Logout, accent) { confirmLogout = true }
                D2()
                SAction("حذف الحساب", "حذف الحساب وبياناته نهائياً", Icons.Default.DeleteForever, Red) {
                    deleteName = ""; deleteChecked = false; deleteError = ""; deleteDialog = true
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark(Modifier.size(72.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Mangalore", color = TextSec, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Font, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("v2.0.0", color=TextDim, fontSize=12.sp)
                }
            }
        }
    }

    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false }, containerColor = Surface2,
        title = { Text("تسجيل الخروج", color = TextPri, fontFamily = Font, fontWeight = FontWeight.Bold) },
        text = { Text("هل تريد تسجيل الخروج من هذا الجهاز؟", color = TextSec, fontFamily = Font) },
        confirmButton = { Button({ confirmLogout = false; AuthStore.signOut(context); onSignedOut() }, shape = ButtonShape) { Text("تسجيل الخروج", fontFamily = Font) } },
        dismissButton = { TextButton({ confirmLogout = false }) { Text("إلغاء", color = TextSec, fontFamily = Font) } }
    )

    if (deleteDialog) AlertDialog(
        onDismissRequest = { if (!deleting) deleteDialog = false }, containerColor = Surface2,
        title = { Text("حذف الحساب", color = Red, fontFamily = Font, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("هذا الإجراء نهائي. اكتب اسمك كما يظهر في الملف الشخصي:", color = TextSec, fontFamily = Font)
                OutlinedTextField(deleteName, { deleteName = it }, label = { Text("اسم الحساب") }, singleLine = true, shape = InputShape, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(deleteChecked, { deleteChecked = it })
                    Text("أفهم أن حذف الحساب نهائي ولا يمكن التراجع عنه", color = TextSec, fontFamily = Font, fontSize = 12.sp)
                }
                if (deleteError.isNotBlank()) Text(deleteError, color = Red, fontFamily = Font, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(enabled = !deleting && deleteName.trim() == AuthStore.displayName.trim() && deleteChecked,
                onClick = { deleteDialog = false; finalDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Red), shape = ButtonShape) { Text("متابعة", fontFamily = Font) }
        },
        dismissButton = { TextButton({ if (!deleting) deleteDialog = false }) { Text("إلغاء", color = TextSec, fontFamily = Font) } }
    )

    if (finalDeleteDialog) AlertDialog(
        onDismissRequest = { if (!deleting) finalDeleteDialog = false }, containerColor = Surface2,
        title = { Text("تأكيد حذف الحساب", color = Red, fontFamily = Font, fontWeight = FontWeight.Bold) },
        text = { Text("سيتم حذف حسابك نهائياً الآن. هل تريد المتابعة؟", color = TextSec, fontFamily = Font) },
        confirmButton = {
            Button(enabled = !deleting, onClick = {
                deleting = true
                scope.launch {
                    runCatching { AuthStore.deleteAccount(context) }
                        .onSuccess { finalDeleteDialog = false; onDeleted() }
                        .onFailure { deleteError = it.message ?: "تعذر حذف الحساب"; finalDeleteDialog = false; deleteDialog = true }
                    deleting = false
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = ButtonShape) {
                Text(if (deleting) "جارٍ الحذف..." else "حذف نهائياً", fontFamily = Font)
            }
        },
        dismissButton = { TextButton({ if (!deleting) finalDeleteDialog = false }) { Text("إلغاء", color = TextSec, fontFamily = Font) } }
    )

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
private fun Drawer(accent:Color, cur:Dest, onClose:()->Unit, onNav:(String)->Unit) {
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxHeight().width(296.dp).align(Alignment.CenterStart).pointerInput(Unit){detectTapGestures{}},
            color=Bg, shadowElevation=24.dp) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(Modifier.fillMaxWidth().height(150.dp)
                    .background(Brush.verticalGradient(listOf(accent.copy(.22f), Bg)))) {
                    Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onNav("profile") }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Surface3), Alignment.Center) {
                                if (AuthStore.avatarUrl.isNotBlank()) AsyncImage(
                                    model = AuthStore.avatarUrl, contentDescription = "صورة المستخدم",
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                ) else Icon(Icons.Default.Person, null, tint = TextSec, modifier = Modifier.size(28.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(AuthStore.displayName.ifBlank { "قارئ مانجالور" }, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("الملف الشخصي", color = TextSec, fontSize = 11.sp)
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal=8.dp, vertical=8.dp)) {
                    item { DSec("التنقل", accent) }
                    item { DItem("الرئيسية",    Icons.Default.Home,          "home",     cur is Dest.Home,     onNav) }
                    item { DItem("كل المانجا",    Icons.Default.GridView,       "all",      cur is Dest.AllManga, onNav) }
                    item { DItem("أحدث المانجا",  Icons.Default.NewReleases,    "latest",   cur is Dest.LatestManga, onNav) }
                    item { DItem("أشهر المانجا",  Icons.Default.TrendingUp,     "popular",  cur is Dest.PopularManga, onNav) }
                    item { DItem("البحث",        Icons.Default.Search,        "search",   cur is Dest.Search,   onNav) }
                    item { DItem("مكتبتي",       Icons.Default.LibraryBooks,  "library",  cur is Dest.Library,  onNav) }
                    item { DItem("سجل القراءة",  Icons.Default.History,       "history",  cur is Dest.History,  onNav) }
                    item { DItem("التنزيلات",    Icons.Default.Download,      "downloads", cur is Dest.Downloads, onNav) }
                    item { DItem("قوائمي",        Icons.Default.PlaylistPlay,   "custom",    cur is Dest.CustomLists, onNav) }
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
            .crossfade(0).build(),
        contentDescription = null, contentScale = scale, modifier = modifier
    )
}

@Composable
private fun SpringCard(onClick:()->Unit, content:@Composable ()->Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(100), label = "card-press")
    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(source, null, onClick = onClick)) { content() }
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
    Surface(color=Surface2, shape=InputShape, modifier=modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon, null, tint=tint, modifier=Modifier.size(17.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color=TextPri, fontSize=15.sp, fontWeight=FontWeight.Bold)
            Text(label, color=TextSec, fontSize=11.sp)
        }
    }
}

@Composable private fun IRow(label:String, value:String, last:Boolean=false, dimValue:Boolean=false) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=11.dp), Arrangement.SpaceBetween) {
        Text(label, color=TextSec, fontSize=13.sp)
        Text(value, color=if (dimValue) TextDim else TextPri, fontSize=13.sp, fontWeight=if (dimValue) FontWeight.Normal else FontWeight.Medium,
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
    Surface(color=Surface2, shape=InputShape,
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
    val bg = if(sel) Accent.copy(.13f) else Color.Transparent
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
@Composable private fun staticPlaceholder(): Brush {
    val transition = rememberInfiniteTransition(label = "placeholder-shimmer")
    val x by transition.animateFloat(-1f, 1.5f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "placeholder-offset")
    return Brush.linearGradient(listOf(Surface3.copy(.45f), Color(0xFF252530), Surface3.copy(.8f)), Offset(x * 900f, 0f), Offset((x + .5f) * 1100f, 180f))
}

@Composable
private fun CustomListsScreen(accent: Color, onBack: () -> Unit, onPick: (MangaItem) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var names by remember { mutableStateOf(CustomListsStore.names(context)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(emptyList<MangaItem>()) }
    var cloudLists by remember { mutableStateOf<Map<String, List<MangaItem>>>(emptyMap()) }
    var creating by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    fun refresh() { names = if (AuthStore.hasSession()) cloudLists.keys.sorted() else CustomListsStore.names(context); selected?.let { items = cloudLists[it] ?: CustomListsStore.get(context, it) } }
    LaunchedEffect(Unit) { if (AuthStore.hasSession()) runCatching { CloudStore.fetchCustomLists() }.onSuccess { cloudLists = it; names = it.keys.sorted() } }
    Column(Modifier.fillMaxSize()) {
        TopBar("قوائمي", accent, onBack, action = { IconButton({ creating = true; nameInput = "" }) { Icon(Icons.Default.Add, "إنشاء قائمة", tint = accent) } })
        if (names.isEmpty()) EmptyState(Icons.Default.PlaylistPlay, "لا توجد قوائم", "أنشئ قائمة من زر الإضافة")
        else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(names) { name ->
                val count = (cloudLists[name] ?: CustomListsStore.get(context, name)).size
                Surface(color = if (selected == name) accent.copy(.15f) else Surface2, shape = InputShape, border = BorderStroke(1.dp, if (selected == name) accent else Border)) {
                    Row(Modifier.fillMaxWidth().clickable { selected = name; items = cloudLists[name] ?: CustomListsStore.get(context, name) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlaylistPlay, null, tint = accent, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, color = TextPri, fontWeight = FontWeight.Bold); Text("$count مانجا", color = TextSec, fontSize = 12.sp) }
                        IconButton({ if (AuthStore.hasSession()) scope.launch { runCatching { CloudStore.deleteCustomList(name) }; cloudLists = cloudLists - name; refresh() } else { CustomListsStore.delete(context, name); if (selected == name) { selected = null; items = emptyList() }; refresh() } }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Red) }
                    }
                }
            }
            if (selected != null) {
                item { Text("محتويات القائمة", color = TextSec, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
                items(items, key = { it.url }) { manga ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(manga) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp, 64.dp).clip(RoundedCornerShape(8.dp))) { Img(manga.coverUrl, Modifier.fillMaxSize()) }
                        Text(manga.title, color = TextPri, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        IconButton({ if (AuthStore.hasSession()) scope.launch { runCatching { CloudStore.removeCustomListItem(selected!!, manga.url) }; cloudLists = cloudLists.mapValues { if (it.key == selected) it.value.filterNot { m -> m.url == manga.url } else it.value }; refresh() } else { CustomListsStore.remove(context, selected!!, manga.url); refresh() } }) { Icon(Icons.Default.Close, "إزالة", tint = TextDim) }
                    }
                }
            }
        }
    }
    if (creating) AlertDialog(onDismissRequest = { creating = false }, containerColor = Surface2,
        title = { Text("قائمة جديدة", color = TextPri, fontFamily = Font) },
        text = { OutlinedTextField(nameInput, { nameInput = it }, label = { Text("اسم القائمة") }, singleLine = true, shape = InputShape) },
        confirmButton = { TextButton({ if (nameInput.trim().isNotBlank() && !names.contains(nameInput.trim())) { CustomListsStore.create(context, nameInput); names = names + nameInput.trim(); creating = false; refresh() } }) { Text("إنشاء", color = accent) } },
        dismissButton = { TextButton({ creating = false }) { Text("إلغاء", color = TextSec) } })
}

@Composable
private fun DownloadedMangaScreen(group: DownloadGroup, accent: Color, onBack: () -> Unit, onRead: (ChapterItem, MangaDetail) -> Unit) {
    val context = LocalContext.current
    val chapters = group.chapterUrls.mapIndexed { index, url -> ChapterItem((index + 1).toString(), "", url, "") }
    val manga = MangaDetail(group.title, group.key, group.cover, group.cover, group.mangaUrl, emptyList(), "", "", "", "", "", "", "", chapters)
    Column(Modifier.fillMaxSize()) {
        TopBar(group.title, accent, onBack)
        if (chapters.isEmpty()) EmptyState(Icons.Default.Download, "لا توجد فصول مكتملة", "أعد تنزيل الفصول من صفحة المانجا")
        else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("الفصول المحفوظة على الجهاز", color = TextSec, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp)) }
            items(chapters, key = { it.url }) { chapter ->
                val pages = LocalDownloads.localImages(context, chapter.url).size
                Row(Modifier.fillMaxWidth().clip(InputShape).background(Surface2).clickable { onRead(chapter, manga) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = accent, modifier = Modifier.size(22.dp))
                    Text("الفصل ${chapter.number}", color = TextPri, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Text("$pages صفحة", color = TextSec, fontSize = 12.sp)
                    Icon(Icons.Default.ChevronLeft, null, tint = TextDim)
                }
            }
        }
    }
}
