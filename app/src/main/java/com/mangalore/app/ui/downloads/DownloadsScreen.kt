package com.mangalore.app.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.DownloadManager
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.EmptyState
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val nav = LocalNavCoordinator.current
    val downloadManager = remember { DownloadManager.getInstance(context) }
    val groups = downloadManager.groupedByManga()

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text(L("downloads.title"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (groups.isEmpty()) {
            EmptyState(icon = Icons.Filled.Download, message = L("downloads.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(groups) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.go(Routes.detail(group.mangaSlug)) }
                            .background(ZTheme.card, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MangaCoverImage(url = group.mangaCover, modifier = Modifier.width(56.dp).height(78.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(group.mangaTitle, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("${group.chapters.size} chapters downloaded", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
