package com.mangalore.app.ui.nav

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mangalore.app.data.AppStore
import com.mangalore.app.data.SupabaseAuth
import com.mangalore.app.data.SupabaseSync
import com.mangalore.app.localization.L
import com.mangalore.app.ui.admin.AdminScreen
import com.mangalore.app.ui.common.CloudflareChallengeHost
import com.mangalore.app.ui.detail.MangaDetailScreen
import com.mangalore.app.ui.downloads.DownloadsScreen
import com.mangalore.app.ui.history.HistoryScreen
import com.mangalore.app.ui.home.HomeScreen
import com.mangalore.app.ui.leaderboard.LeaderboardScreen
import com.mangalore.app.ui.library.LibraryScreen
import com.mangalore.app.ui.messages.ConversationScreen
import com.mangalore.app.ui.messages.MessagesScreen
import com.mangalore.app.ui.newreleases.NewReleasesScreen
import com.mangalore.app.ui.onboarding.AuthScreen
import com.mangalore.app.ui.onboarding.LanguageSelectionScreen
import com.mangalore.app.ui.onboarding.LoadingScreen
import com.mangalore.app.ui.profile.ProfileScreen
import com.mangalore.app.ui.profile.PublicProfileScreen
import com.mangalore.app.ui.reader.ReaderScreen
import com.mangalore.app.ui.search.SearchScreen
import com.mangalore.app.ui.settings.SettingsScreen
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val NEW_RELEASES = "new_releases"
    const val PROFILE = "profile"
    const val MESSAGES = "messages"
    const val ADMIN = "admin"
    const val LEADERBOARD = "leaderboard"
    const val DETAIL = "detail/{slug}"
    const val READER = "reader/{mangaSlug}/{chapterSlug}"
    const val PUBLIC_PROFILE = "public_profile/{userId}"
    const val CONVERSATION = "conversation/{userId}/{username}"

    fun detail(slug: String) = "detail/${URLEncoder.encode(slug, "UTF-8")}"
    fun reader(mangaSlug: String, chapterSlug: String) =
        "reader/${URLEncoder.encode(mangaSlug, "UTF-8")}/${URLEncoder.encode(chapterSlug, "UTF-8")}"
    fun publicProfile(userId: String) = "public_profile/$userId"
    fun conversation(userId: String, username: String) =
        "conversation/$userId/${URLEncoder.encode(username, "UTF-8")}"

    fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}

/** Central place any screen can call to open the drawer or navigate — the
 * Android equivalent of the iOS `NavCoordinator`. */
class NavCoordinator(val navController: NavHostController, val drawerState: DrawerState, val scope: kotlinx.coroutines.CoroutineScope) {
    var unreadMessageCount by mutableStateOf(0)

    fun openDrawer() { scope.launch { drawerState.open() } }
    fun closeDrawer() { scope.launch { drawerState.close() } }

    fun go(route: String) {
        closeDrawer()
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun refreshUnreadCount(auth: SupabaseAuth) {
        scope.launch {
            try {
                if (auth.isAuthenticated) unreadMessageCount = SupabaseSync.unreadMessageCount(auth)
            } catch (e: Exception) { }
        }
    }
}

val LocalNavCoordinator = staticCompositionLocalOf<NavCoordinator> { error("No NavCoordinator provided") }
val LocalAppStore = staticCompositionLocalOf<AppStore> { error("No AppStore provided") }
val LocalSupabaseAuth = staticCompositionLocalOf<SupabaseAuth> { error("No SupabaseAuth provided") }

@Composable
fun MangaloreApp() {
    val context = LocalContext.current
    var showLoading by remember { mutableStateOf(true) }
    var hasChosenLanguage by remember { mutableStateOf(hasChosenLanguagePref(context)) }
    val auth = remember { SupabaseAuth.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        when {
            showLoading -> LoadingScreen(onFinished = { showLoading = false })
            !hasChosenLanguage -> LanguageSelectionScreen(onFinished = {
                setChosenLanguagePref(context)
                hasChosenLanguage = true
            })
            auth.isAuthenticated || auth.isGuest -> MainShell(auth)
            else -> AuthScreen(onFinished = { })
        }
    }

    // Mounted once at the app root so it's available no matter which screen
    // triggers a background fetch that runs into a Cloudflare interstitial.
    CloudflareChallengeHost()
}

private fun hasChosenLanguagePref(context: Context): Boolean =
    context.getSharedPreferences("mangalore_locale", Context.MODE_PRIVATE).getBoolean("has_chosen_language", false)

private fun setChosenLanguagePref(context: Context) {
    context.getSharedPreferences("mangalore_locale", Context.MODE_PRIVATE).edit().putBoolean("has_chosen_language", true).apply()
}

@Composable
fun MainShell(auth: SupabaseAuth) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val nav = remember { NavCoordinator(navController, drawerState, scope) }
    val appStore = remember { AppStore.getInstance(context) }

    LaunchedEffect(Unit) {
        nav.refreshUnreadCount(auth)
    }

    CompositionLocalProvider(
        LocalNavCoordinator provides nav,
        LocalAppStore provides appStore,
        LocalSupabaseAuth provides auth
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { DrawerContent(auth, nav) }
        ) {
            Box(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen() }
                    composable(Routes.SEARCH) { SearchScreen() }
                    composable(Routes.LIBRARY) { LibraryScreen() }
                    composable(Routes.DOWNLOADS) { DownloadsScreen() }
                    composable(Routes.HISTORY) { HistoryScreen() }
                    composable(Routes.SETTINGS) { SettingsScreen() }
                    composable(Routes.NEW_RELEASES) { NewReleasesScreen() }
                    composable(Routes.PROFILE) { ProfileScreen() }
                    composable(Routes.MESSAGES) { MessagesScreen() }
                    composable(Routes.ADMIN) { AdminScreen() }
                    composable(Routes.LEADERBOARD) { LeaderboardScreen() }
                    composable(Routes.DETAIL) { backStackEntry ->
                        val slug = Routes.decode(backStackEntry.arguments?.getString("slug") ?: "")
                        MangaDetailScreen(slug = slug)
                    }
                    composable(Routes.READER) { backStackEntry ->
                        val mangaSlug = Routes.decode(backStackEntry.arguments?.getString("mangaSlug") ?: "")
                        val chapterSlug = Routes.decode(backStackEntry.arguments?.getString("chapterSlug") ?: "")
                        ReaderScreen(mangaSlug = mangaSlug, initialChapterSlug = chapterSlug)
                    }
                    composable(Routes.PUBLIC_PROFILE) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        PublicProfileScreen(userId = userId)
                    }
                    composable(Routes.CONVERSATION) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        val username = Routes.decode(backStackEntry.arguments?.getString("username") ?: "")
                        ConversationScreen(partnerId = userId, partnerUsername = username)
                    }
                }

                appStore.newlyUnlockedAchievement?.let { achievement ->
                    AchievementToast(achievement = achievement, onDismiss = { appStore.newlyUnlockedAchievement = null })
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AchievementToast(achievement: com.mangalore.app.data.SBAchievement, onDismiss: () -> Unit) {
    LaunchedEffect(achievement) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }
    Card(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZTheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ZTheme.accent.copy(alpha = 0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(ZTheme.goldGradient, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconForName(achievement.icon), contentDescription = null, tint = ZTheme.bg)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Achievement Unlocked!", color = ZTheme.accentBright, style = MaterialTheme.typography.labelSmall)
                Text(achievement.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(achievement.description, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

fun iconForName(name: String) = when (name) {
    "book.fill" -> Icons.Filled.MenuBook
    "books.vertical.fill" -> Icons.Filled.LibraryBooks
    "flame.fill" -> Icons.Filled.Whatshot
    "crown.fill" -> Icons.Filled.EmojiEvents
    "bolt.fill" -> Icons.Filled.Bolt
    "star.circle.fill" -> Icons.Filled.Star
    "heart.fill" -> Icons.Filled.Favorite
    "bubble.left.fill" -> Icons.Filled.ChatBubble
    else -> Icons.Filled.EmojiEvents
}

@Composable
private fun DrawerContent(auth: SupabaseAuth, nav: NavCoordinator) {
    ModalDrawerSheet(drawerContainerColor = ZTheme.surface) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier.fillMaxWidth().background(ZTheme.card).padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.mangalore.app.R.drawable.logo),
                    contentDescription = "Mangalore",
                    modifier = Modifier.height(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your Manga Universe", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                DrawerItem(Icons.Filled.Home, L("nav.home")) { nav.go(Routes.HOME) }
                DrawerItem(Icons.Filled.Search, L("nav.search")) { nav.go(Routes.SEARCH) }
                DrawerItem(Icons.Filled.AutoAwesome, L("nav.newReleases"), badge = "NEW") { nav.go(Routes.NEW_RELEASES) }
                DrawerItem(Icons.Filled.AccountCircle, L("nav.profile")) { nav.go(Routes.PROFILE) }
                DrawerItem(
                    Icons.Filled.ChatBubble, L("nav.messages"),
                    badge = if (nav.unreadMessageCount > 0) "${nav.unreadMessageCount}" else null,
                    isNotification = true
                ) { nav.go(Routes.MESSAGES) }
                DrawerItem(Icons.Filled.EmojiEvents, "Leaderboard") { nav.go(Routes.LEADERBOARD) }
                DrawerItem(Icons.Filled.LibraryBooks, L("nav.library")) { nav.go(Routes.LIBRARY) }
                DrawerItem(Icons.Filled.Download, L("nav.downloads")) { nav.go(Routes.DOWNLOADS) }
                DrawerItem(Icons.Filled.History, L("nav.history")) { nav.go(Routes.HISTORY) }

                Divider(color = ZTheme.border, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                DrawerItem(Icons.Filled.Settings, L("nav.settings")) { nav.go(Routes.SETTINGS) }

                if (auth.profile?.role == "admin") {
                    Divider(color = ZTheme.border, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    DrawerItem(Icons.Filled.Shield, "Admin Panel", badge = "ADMIN") { nav.go(Routes.ADMIN) }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mangalore", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelMedium)
                Text("Version 1.0", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, badge: String? = null, isNotification: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                modifier = Modifier
                    .background(if (isNotification) ZTheme.danger else ZTheme.accent, androidx.compose.foundation.shape.CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, color = if (isNotification) Color.White else ZTheme.bg, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
