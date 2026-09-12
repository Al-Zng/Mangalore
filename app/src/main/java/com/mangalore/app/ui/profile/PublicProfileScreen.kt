package com.mangalore.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.*
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun PublicProfileScreen(userId: String) {
    val auth = LocalSupabaseAuth.current
    val nav = LocalNavCoordinator.current
    val scope = rememberCoroutineScope()
    val isMe = userId == auth.userId

    var profile by remember { mutableStateOf<SBProfile?>(null) }
    var achievements by remember { mutableStateOf<List<SBAchievement>>(emptyList()) }
    var unlockedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        try {
            profile = SupabaseSync.fetchPublicProfile(userId, auth)
            achievements = SupabaseSync.fetchAllAchievements(auth)
            unlockedIds = SupabaseSync.fetchUnlockedAchievements(userId, auth)
            followerCount = SupabaseSync.followerCount(userId, auth)
            followingCount = SupabaseSync.followingCount(userId, auth)
            if (!isMe && auth.isAuthenticated) isFollowing = SupabaseSync.isFollowing(userId, auth)
        } catch (e: Exception) { }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(profile?.username ?: "Profile", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (isLoading) {
            LoadingSpinner()
        } else if (profile == null) {
            Text("User not found", color = ZTheme.textSecondary, modifier = Modifier.padding(20.dp))
        } else {
            val p = profile!!
            Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    MangaCoverImage(url = p.avatar_url, modifier = Modifier.size(92.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(p.username, color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)
                    if (p.role == "admin") {
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCardMini(Icons.Filled.MenuBook, "${p.chapters_read_count}", L("profile.chaptersRead"))
                    StatCardMini(Icons.Filled.Whatshot, "${p.current_streak ?: 0}", "Day Streak")
                    StatCardMini(Icons.Filled.Star, "${p.longest_streak ?: 0}", "Best Streak")
                }

                if (!isMe) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isBusy = true
                                scope.launch {
                                    try {
                                        if (isFollowing) { SupabaseSync.unfollow(userId, auth); isFollowing = false; followerCount-- }
                                        else { SupabaseSync.follow(userId, auth); isFollowing = true; followerCount++ }
                                    } catch (e: Exception) { }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy && auth.isAuthenticated,
                            modifier = Modifier.weight(1f),
                            colors = if (isFollowing) ButtonDefaults.buttonColors(containerColor = ZTheme.card) else ButtonDefaults.buttonColors(containerColor = ZTheme.accent)
                        ) {
                            Icon(if (isFollowing) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null, tint = if (isFollowing) ZTheme.textPrimary else ZTheme.bg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFollowing) "Following" else "Follow", color = if (isFollowing) ZTheme.textPrimary else ZTheme.bg)
                        }
                        IconButton(
                            onClick = { nav.go(Routes.conversation(userId, p.username)) },
                            enabled = auth.isAuthenticated,
                            modifier = Modifier.size(48.dp).background(ZTheme.card, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, tint = ZTheme.textPrimary)
                        }
                    }
                }

                AchievementsSection(achievements, unlockedIds)
            }
        }
    }
}

@Composable
private fun RowScope.StatCardMini(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f).background(ZTheme.card, RoundedCornerShape(12.dp)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(15.dp))
        Text(value, color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        Text(label, color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
