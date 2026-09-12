package com.mangalore.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.*
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalAppStore
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun MangaDetailScreen(slug: String) {
    val context = LocalContext.current
    val nav = LocalNavCoordinator.current
    val store = LocalAppStore.current
    val auth = LocalSupabaseAuth.current
    val service = remember { MangaService.getInstance(context) }

    var manga by remember { mutableStateOf(store.mangaCache[slug]) }
    var isLoading by remember { mutableStateOf(manga == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortAsc by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        try {
            val result = service.fetchDetail(slug)
            manga = result
            store.cacheManga(result)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load"
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        when {
            isLoading -> LoadingSpinner()
            errorMessage != null && manga == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ZTheme.danger, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMessage ?: "", color = ZTheme.textSecondary)
            }
            manga != null -> {
                val m = manga!!
                val sortedChapters = if (sortAsc) m.chapters.sortedBy { it.number.toIntOrNull() ?: 0 }
                                      else m.chapters.sortedByDescending { it.number.toIntOrNull() ?: 0 }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { HeroSection(m, store, auth, nav, onShareClick = { showShareSheet = true }) }
                    if (m.description.isNotBlank()) item { DescriptionSection(m.description) }
                    item { ChaptersHeader(m.chapters.size, sortAsc) { sortAsc = !sortAsc } }
                    items(sortedChapters) { chapter ->
                        ChapterRow(chapter, m, store) {
                            nav.go(Routes.reader(m.slug, chapter.slug))
                        }
                    }
                    item { CommentsSection(m, auth) }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }

    if (showShareSheet && manga != null) {
        ShareMangaDialog(manga = manga!!, auth = auth, onDismiss = { showShareSheet = false })
    }
}

@Composable
private fun HeroSection(
    manga: Manga, store: AppStore, auth: SupabaseAuth,
    nav: com.mangalore.app.ui.nav.NavCoordinator,
    onShareClick: () -> Unit
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            MangaCoverImage(url = manga.highQualityCoverURL, modifier = Modifier.fillMaxSize().blur(22.dp))
            Box(modifier = Modifier.fillMaxSize().background(ZTheme.bg.copy(alpha = 0.55f)))
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, ZTheme.bg))
                )
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                MangaCoverImage(
                    url = manga.highQualityCoverURL,
                    modifier = Modifier.width(110.dp).height(155.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    if (manga.rating.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(manga.rating, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        manga.title, color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                    if (manga.author.isNotBlank()) {
                        Text(manga.author, color = ZTheme.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            if (manga.genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    manga.genres.take(4).forEach { genre ->
                        Text(
                            genre, color = ZTheme.accent, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(ZTheme.accentDim, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val firstChapter = manga.chapters.minByOrNull { it.number.toIntOrNull() ?: 0 }
                if (firstChapter != null) {
                    Button(
                        onClick = { nav.go(Routes.reader(manga.slug, firstChapter.slug)) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ZTheme.bg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L("detail.startReading"), color = ZTheme.bg, style = MaterialTheme.typography.titleSmall)
                    }
                } else {
                    Text(L("detail.noChapters"), color = ZTheme.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
                }

                var inLibrary by remember { mutableStateOf(store.isInLibrary(manga)) }
                IconButton(
                    onClick = {
                        if (inLibrary) store.removeFromLibrary(manga, auth) else store.addToLibrary(manga, auth)
                        inLibrary = !inLibrary
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ZTheme.card, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        if (inLibrary) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null, tint = if (inLibrary) ZTheme.accent else ZTheme.textSecondary
                    )
                }

                if (auth.isAuthenticated) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(48.dp).background(ZTheme.card, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = ZTheme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .background(ZTheme.card, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(L("detail.synopsis").uppercase(), color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, color = ZTheme.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChaptersHeader(count: Int, sortAsc: Boolean, onToggleSort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$count ${L("detail.chapters").uppercase()}", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clickable(onClick = onToggleSort)
                .background(ZTheme.card, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (sortAsc) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null, tint = ZTheme.textSecondary, modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (sortAsc) "Oldest" else "Newest", color = ZTheme.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, manga: Manga, store: AppStore, onClick: () -> Unit) {
    val context = LocalContext.current
    val downloadManager = remember { DownloadManager.getInstance(context) }
    val isRead = store.history.any { it.mangaSlug == manga.slug && it.chapterSlug == chapter.slug }
    val isDownloaded = downloadManager.isDownloaded(manga.slug, chapter.slug)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .background(ZTheme.card, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(if (isRead) ZTheme.bg else ZTheme.accentDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(chapter.number, color = if (isRead) ZTheme.textTertiary else ZTheme.accent, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Chapter ${chapter.number}", color = if (isRead) ZTheme.textTertiary else ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
            if (chapter.date.isNotBlank()) {
                Text(chapter.date, color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (isDownloaded) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ZTheme.success, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = ZTheme.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CommentsSection(manga: Manga, auth: SupabaseAuth) {
    val scope = rememberCoroutineScope()
    var mangaRowId by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf<List<SBComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var myRating by remember { mutableStateOf(0) }
    var isPosting by remember { mutableStateOf(false) }

    LaunchedEffect(manga.slug) {
        try {
            val row = SupabaseSync.upsertManga(manga, auth)
            mangaRowId = row.id
            comments = SupabaseSync.fetchComments(row.id, auth)
        } catch (e: Exception) { }
        isLoading = false
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Filled.RateReview, contentDescription = null, tint = ZTheme.accent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(L("detail.ratingsAndComments").uppercase(), color = ZTheme.textSecondary, style = MaterialTheme.typography.labelSmall)
        }

        if (auth.isAuthenticated) {
            Column(
                modifier = Modifier.fillMaxWidth().background(ZTheme.card.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Row {
                    for (star in 1..5) {
                        Icon(
                            if (star <= myRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null, tint = ZTheme.accentBright,
                            modifier = Modifier.size(20.dp).clickable { myRating = star }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft, onValueChange = { draft = it },
                        placeholder = { Text(L("detail.writeComment"), color = ZTheme.textTertiary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
                            focusedBorderColor = ZTheme.border, unfocusedBorderColor = ZTheme.border,
                            focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rowId = mangaRowId ?: return@Button
                            if (draft.isBlank()) return@Button
                            isPosting = true
                            scope.launch {
                                try {
                                    SupabaseSync.postComment(rowId, if (myRating > 0) myRating else null, draft, auth)
                                    draft = ""; myRating = 0
                                    comments = SupabaseSync.fetchComments(rowId, auth)
                                } catch (e: Exception) { }
                                isPosting = false
                            }
                        },
                        enabled = !isPosting && draft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent)
                    ) {
                        Text(L("detail.post"), color = ZTheme.bg)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoading) {
            LoadingSpinner(modifier = Modifier.height(60.dp))
        } else if (comments.isEmpty()) {
            Text(L("detail.noComments"), color = ZTheme.textTertiary, style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                comments.forEach { comment ->
                    CommentRow(comment, canDelete = comment.user_id == auth.userId || auth.profile?.role == "admin") {
                        scope.launch {
                            try {
                                SupabaseSync.deleteComment(comment.id, auth)
                                comments = comments.filter { it.id != comment.id }
                            } catch (e: Exception) { }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: SBComment, canDelete: Boolean, onDelete: () -> Unit) {
    val nav = LocalNavCoordinator.current
    Row(
        modifier = Modifier.fillMaxWidth().background(ZTheme.card, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        MangaCoverImage(
            url = comment.profiles?.avatar_url,
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { nav.go(Routes.publicProfile(comment.user_id)) }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.profiles?.username ?: "User", color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { nav.go(Routes.publicProfile(comment.user_id)) }
                )
                comment.rating?.let { r ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Row { repeat(r) { Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(8.dp)) } }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (canDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = ZTheme.textTertiary, modifier = Modifier.size(14.dp).clickable(onClick = onDelete))
                }
            }
            Text(comment.content, color = ZTheme.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShareMangaDialog(manga: Manga, auth: SupabaseAuth, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZTheme.surface,
        title = { Text(L("detail.send"), color = ZTheme.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    placeholder = { Text(L("messages.searchUser")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
                        focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary
                    )
                )
                errorMessage?.let { Text(it, color = ZTheme.danger, style = MaterialTheme.typography.labelSmall) }
                if (sent) Text("Sent!", color = ZTheme.success)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                isSending = true
                scope.launch {
                    try {
                        val recipient = SupabaseSync.findUser(username, auth)
                        if (recipient == null) {
                            errorMessage = "No user found with that username"
                        } else {
                            val row = SupabaseSync.upsertManga(manga, auth)
                            SupabaseSync.sendMessage(recipient.id, null, row.id, auth)
                            sent = true
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                    isSending = false
                }
            }) { Text(L("detail.send"), color = ZTheme.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = ZTheme.textSecondary) } }
    )
}
