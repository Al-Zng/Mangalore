package com.mangalore.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.EmptyState
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun HistoryScreen() {
    val store = LocalAppStore.current
    val nav = LocalNavCoordinator.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text(L("history.title"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (store.history.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = ZTheme.danger)
                }
            }
        }

        if (store.history.isEmpty()) {
            EmptyState(icon = Icons.Filled.History, message = L("history.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(store.history) { progress ->
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clickable { nav.go(Routes.detail(progress.mangaSlug)) }
                            .background(ZTheme.card, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MangaCoverImage(url = progress.mangaCover, modifier = Modifier.width(56.dp).height(78.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(progress.mangaTitle, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Ch. ${progress.chapterNumber} · Page ${progress.pageIndex + 1}", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?") },
            text = { Text("This will remove all reading history.") },
            confirmButton = {
                TextButton(onClick = { store.clearHistory(); showClearConfirm = false }) {
                    Text("Clear", color = ZTheme.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
            containerColor = ZTheme.surface
        )
    }
}
