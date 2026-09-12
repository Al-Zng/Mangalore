package com.mangalore.app.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.SBManga
import com.mangalore.app.data.SBMessage
import com.mangalore.app.data.SupabaseSync
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(partnerId: String, partnerUsername: String) {
    val auth = LocalSupabaseAuth.current
    val nav = LocalNavCoordinator.current
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<SBMessage>>(emptyList()) }
    var sharedMangaLookup by remember { mutableStateOf<Map<String, SBManga>>(emptyMap()) }
    var draft by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    suspend fun load() {
        try {
            val loaded = SupabaseSync.fetchConversation(partnerId, auth)
            messages = loaded
            val mangaIds = loaded.mapNotNull { it.shared_manga_id }.toSet() - sharedMangaLookup.keys
            if (mangaIds.isNotEmpty()) {
                val rows = SupabaseSync.fetchMangaByIds(mangaIds.toList(), auth)
                sharedMangaLookup = sharedMangaLookup + rows.associateBy { it.id }
            }
            SupabaseSync.markConversationRead(partnerId, auth)
            nav.refreshUnreadCount(auth)
        } catch (e: Exception) { }
    }

    LaunchedEffect(partnerId) { load() }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ZTheme.textPrimary)
            }
            Text(partnerUsername, color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.sender_id == auth.userId,
                    sharedManga = message.shared_manga_id?.let { sharedMangaLookup[it] },
                    onOpenManga = { slug -> nav.go(Routes.detail(slug)) }
                )
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                placeholder = { Text(L("messages.typeMessage"), color = ZTheme.textTertiary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
                    focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) return@IconButton
                    draft = ""
                    isSending = true
                    scope.launch {
                        try {
                            SupabaseSync.sendMessage(partnerId, text, null, auth)
                            load()
                        } catch (e: Exception) { }
                        isSending = false
                    }
                },
                enabled = draft.isNotBlank() && !isSending,
                modifier = Modifier.background(ZTheme.goldGradient, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = ZTheme.bg)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SBMessage, isMine: Boolean, sharedManga: SBManga?, onOpenManga: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    if (isMine) ZTheme.goldGradient else androidx.compose.ui.graphics.Brush.linearGradient(listOf(ZTheme.card, ZTheme.card)),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            sharedManga?.let { manga ->
                Row(
                    modifier = Modifier
                        .background(ZTheme.bg.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { onOpenManga(manga.slug) }
                        .padding(8.dp)
                ) {
                    MangaCoverImage(url = manga.cover_url, modifier = Modifier.width(44.dp).height(62.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(manga.title, color = if (isMine) ZTheme.bg else ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(L("messages.open"), color = if (isMine) ZTheme.bg.copy(alpha = 0.8f) else ZTheme.accent, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (!message.content.isNullOrBlank()) {
                Text(message.content!!, color = if (isMine) ZTheme.bg else ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
