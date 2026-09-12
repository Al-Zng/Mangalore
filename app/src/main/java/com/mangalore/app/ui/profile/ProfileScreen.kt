package com.mangalore.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.*
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.nav.iconForName
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val auth = LocalSupabaseAuth.current
    val store = LocalAppStore.current
    val scope = rememberCoroutineScope()

    var achievements by remember { mutableStateOf<List<SBAchievement>>(emptyList()) }
    var unlockedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        auth.refreshProfile()
        val uid = auth.userId ?: return@LaunchedEffect
        try {
            achievements = SupabaseSync.fetchAllAchievements(auth)
            unlockedIds = SupabaseSync.fetchUnlockedAchievements(uid, auth)
            followerCount = SupabaseSync.followerCount(uid, auth)
            followingCount = SupabaseSync.followingCount(uid, auth)
        } catch (e: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text(L("nav.profile"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (auth.isGuest) {
            GuestState(auth)
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProfileHeader(auth, followerCount, followingCount)
                StatsRow(auth, store)
                StreakCard(auth)
                AchievementsSection(achievements, unlockedIds)
                MemberSinceRow(auth)
                LeaderboardLinkRow()
                SignOutButton(auth)
            }
        }
    }
}

@Composable
private fun GuestState(auth: SupabaseAuth) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = ZTheme.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(L("auth.welcome"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(L("auth.subtitle"), color = ZTheme.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { auth.signOut() }, colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent)) {
            Text(L("auth.login"), color = ZTheme.bg)
        }
    }
}

@Composable
private fun ProfileHeader(auth: SupabaseAuth, followerCount: Int, followingCount: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MangaCoverImage(url = auth.profile?.avatar_url, modifier = Modifier.size(92.dp).clip(CircleShape))
        Spacer(modifier = Modifier.height(10.dp))
        Text(auth.profile?.username ?: "", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)
        if (auth.profile?.role == "admin") {
            Text(
                L("profile.admin"), color = ZTheme.bg, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(ZTheme.goldGradient, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$followerCount", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
                Text("Followers", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$followingCount", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
                Text("Following", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun StatsRow(auth: SupabaseAuth, store: AppStore) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(icon = Icons.Filled.MenuBook, value = "${auth.profile?.chapters_read_count ?: 0}", label = L("profile.chaptersRead"), modifier = Modifier.weight(1f))
        StatCard(icon = Icons.Filled.Favorite, value = "${store.library.size}", label = L("profile.favorites"), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(ZTheme.card, RoundedCornerShape(14.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)
        Text(label, color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StreakCard(auth: SupabaseAuth) {
    Row(modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(14.dp)).padding(vertical = 14.dp)) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(22.dp))
            Text("${auth.profile?.current_streak ?: 0}", color = ZTheme.textPrimary, style = MaterialTheme.typography.headlineSmall)
            Text("Day Streak", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Box(modifier = Modifier.width(1.dp).height(44.dp).background(ZTheme.border).align(Alignment.CenterVertically))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(22.dp))
            Text("${auth.profile?.longest_streak ?: 0}", color = ZTheme.textPrimary, style = MaterialTheme.typography.headlineSmall)
            Text("Best Streak", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AchievementsSection(achievements: List<SBAchievement>, unlockedIds: Set<String>) {
    Column(modifier = Modifier.fillMaxWidth().background(ZTheme.card.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ACHIEVEMENTS (${unlockedIds.size}/${achievements.size})", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements) { a ->
                val unlocked = unlockedIds.contains(a.id)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (unlocked) 1f else 0.45f)) {
                    Box(
                        modifier = Modifier.size(48.dp).background(if (unlocked) ZTheme.goldGradient else androidx.compose.ui.graphics.Brush.linearGradient(listOf(ZTheme.card, ZTheme.card)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconForName(a.icon), contentDescription = null, tint = if (unlocked) ZTheme.bg else ZTheme.textTertiary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(a.title, color = if (unlocked) ZTheme.textPrimary else ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun MemberSinceRow(auth: SupabaseAuth) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = ZTheme.textTertiary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(L("profile.memberSince"), color = ZTheme.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatDate(auth.profile?.created_at), color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LeaderboardLinkRow() {
    val nav = LocalNavCoordinator.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { nav.go(Routes.LEADERBOARD) }
            .background(ZTheme.card, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ZTheme.accent)
        Spacer(modifier = Modifier.width(10.dp))
        Text("View Leaderboard", color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ZTheme.textTertiary)
    }
}

@Composable
private fun SignOutButton(auth: SupabaseAuth) {
    OutlinedButton(
        onClick = { auth.signOut() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZTheme.danger),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZTheme.danger.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(L("nav.signOut"), style = MaterialTheme.typography.titleSmall)
    }
}

private fun formatDate(iso: String?): String {
    if (iso == null) return "—"
    return try {
        val instant = java.time.Instant.parse(iso)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) { "—" }
}
