package com.mangalore.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.SBProfile
import com.mangalore.app.data.SupabaseSync
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun LeaderboardScreen() {
    val auth = LocalSupabaseAuth.current
    val nav = LocalNavCoordinator.current
    var entries by remember { mutableStateOf<List<SBProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { entries = SupabaseSync.fetchLeaderboard(auth) } catch (e: Exception) { }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text("Leaderboard", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (isLoading) {
            LoadingSpinner()
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(entries) { index, profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.go(Routes.publicProfile(profile.id)) }
                            .background(ZTheme.card, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "#${index + 1}", color = rankColor(index + 1), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(36.dp)
                        )
                        MangaCoverImage(url = profile.avatar_url, modifier = Modifier.size(38.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.username, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            val streak = profile.current_streak ?: 0
                            if (streak > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Whatshot, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("$streak-day streak", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Text("${profile.chapters_read_count}", color = ZTheme.accent, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ch.", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun rankColor(rank: Int): Color = when (rank) {
    1 -> ZTheme.accentBright
    2 -> Color(0xFFC0C0C0)
    3 -> Color(0xFFCD7F32)
    else -> ZTheme.border
}
