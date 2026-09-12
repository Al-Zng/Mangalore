package com.mangalore.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.SBComment
import com.mangalore.app.data.SBManga
import com.mangalore.app.data.SupabaseSync
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

private enum class AdminTab { MANGA, COMMENTS }

@Composable
fun AdminScreen() {
    val auth = LocalSupabaseAuth.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(AdminTab.MANGA) }
    var mangaRows by remember { mutableStateOf<List<SBManga>>(emptyList()) }
    var commentRows by remember { mutableStateOf<List<SBComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        isLoading = true
        errorMessage = null
        try {
            when (tab) {
                AdminTab.MANGA -> mangaRows = SupabaseSync.fetchAllManga(auth)
                AdminTab.COMMENTS -> commentRows = SupabaseSync.fetchAllComments(auth)
            }
        } catch (e: Exception) { errorMessage = e.message }
        isLoading = false
    }

    LaunchedEffect(tab) { load() }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text("Admin Panel", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = ZTheme.surface,
            contentColor = ZTheme.accent
        ) {
            Tab(selected = tab == AdminTab.MANGA, onClick = { tab = AdminTab.MANGA }, text = { Text("Manga") })
            Tab(selected = tab == AdminTab.COMMENTS, onClick = { tab = AdminTab.COMMENTS }, text = { Text("Comments") })
        }

        when {
            isLoading -> LoadingSpinner()
            errorMessage != null -> Text(errorMessage ?: "", color = ZTheme.danger, modifier = Modifier.padding(20.dp))
            tab == AdminTab.MANGA -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mangaRows) { manga ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MangaCoverImage(url = manga.cover_url, modifier = Modifier.width(40.dp).height(56.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(manga.title, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text("${manga.chapters_count} ch. · id: ${manga.id.take(8)}…", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                try { SupabaseSync.deleteManga(manga.id, auth); mangaRows = mangaRows.filter { it.id != manga.id } } catch (e: Exception) { }
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ZTheme.danger) }
                    }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commentRows) { comment ->
                    Column(modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(comment.profiles?.username ?: "User", color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            comment.rating?.let { r ->
                                Row { repeat(r) { Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(10.dp)) } }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    try { SupabaseSync.deleteComment(comment.id, auth); commentRows = commentRows.filter { it.id != comment.id } } catch (e: Exception) { }
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ZTheme.danger, modifier = Modifier.size(16.dp)) }
                        }
                        Text(comment.content, color = ZTheme.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}
