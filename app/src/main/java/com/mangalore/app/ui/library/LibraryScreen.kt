package com.mangalore.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.EmptyState
import com.mangalore.app.ui.common.MangaGridCard
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun LibraryScreen() {
    val store = LocalAppStore.current
    val nav = LocalNavCoordinator.current

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text(L("library.title"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (store.library.isEmpty()) {
            EmptyState(icon = Icons.Filled.LibraryBooks, message = L("library.empty"))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(store.library) { manga ->
                    MangaGridCard(manga = manga, onClick = { nav.go(Routes.detail(manga.slug)) })
                }
            }
        }
    }
}
