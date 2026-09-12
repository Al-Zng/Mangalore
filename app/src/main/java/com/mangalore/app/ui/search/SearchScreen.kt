package com.mangalore.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangalore.app.data.Manga
import com.mangalore.app.data.MangaService
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.EmptyState
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaGridCard
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

private val genres = listOf("دراما", "رومانسى", "فانتازا", "أكشن", "كوميدى", "رعب", "خيال علمى", "مغامرات", "رياضة")
private val genreGradients = listOf(
    listOf(Color(0xFFFF6B9D), Color(0xFFC44569)),
    listOf(Color(0xFFFF9FF3), Color(0xFFF368E0)),
    listOf(Color(0xFF5F27CD), Color(0xFF341F97)),
    listOf(Color(0xFFEE5A24), Color(0xFFC0392B)),
    listOf(Color(0xFFFFC048), Color(0xFFF0932B)),
    listOf(Color(0xFF2C2C54), Color(0xFF0C0C1E)),
    listOf(Color(0xFF00D2D3), Color(0xFF01A3A4)),
    listOf(Color(0xFF10AC84), Color(0xFF0B8457)),
    listOf(Color(0xFF1E90FF), Color(0xFF1565C0))
)

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val nav = LocalNavCoordinator.current
    val service = remember { MangaService.getInstance(context) }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var activeLabel by remember { mutableStateOf("") }

    fun runSearch(q: String) {
        if (q.isBlank()) { results = emptyList(); hasSearched = false; return }
        isLoading = true
        hasSearched = true
        activeLabel = q
        scope.launch {
            try { results = service.search(q) } catch (e: Exception) { results = emptyList() }
            isLoading = false
        }
    }

    fun runGenreSearch(genre: String) {
        isLoading = true
        hasSearched = true
        activeLabel = genre
        scope.launch {
            try { results = service.fetchByGenre(genre) } catch (e: Exception) { results = emptyList() }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; runSearch(it) },
                placeholder = { Text(L("search.placeholder"), color = ZTheme.textTertiary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ZTheme.textTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
                    focusedBorderColor = ZTheme.border, unfocusedBorderColor = ZTheme.border,
                    focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary
                )
            )
        }

        when {
            isLoading -> LoadingSpinner()
            hasSearched && results.isEmpty() -> EmptyState(icon = Icons.Filled.Search, message = "${L("search.noResults")} \"$activeLabel\"")
            hasSearched -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(results) { manga -> MangaGridCard(manga = manga, onClick = { nav.go(Routes.detail(manga.slug)) }) }
                }
            }
            else -> GenreBrowseGrid(onGenreClick = { runGenreSearch(it) })
        }
    }
}

@Composable
private fun GenreBrowseGrid(onGenreClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("BROWSE BY GENRE", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genres.size) { index ->
                val gradient = genreGradients[index % genreGradients.size]
                Box(
                    modifier = Modifier
                        .height(74.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(gradient))
                        .clickable { onGenreClick(genres[index]) },
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        genres[index], color = Color.White, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
