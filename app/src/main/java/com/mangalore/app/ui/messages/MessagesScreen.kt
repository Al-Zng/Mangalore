package com.mangalore.app.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.SBProfile
import com.mangalore.app.data.SupabaseSync
import com.mangalore.app.localization.L
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.EmptyState
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaCoverImage
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.LocalSupabaseAuth
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen() {
    val auth = LocalSupabaseAuth.current
    val nav = LocalNavCoordinator.current
    var partners by remember { mutableStateOf<List<SBProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.isAuthenticated) {
            try { partners = SupabaseSync.recentConversationPartners(auth) } catch (e: Exception) { }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text(L("nav.messages"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { showNewMessage = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ZTheme.accent)
            }
        }

        when {
            !auth.isAuthenticated -> EmptyState(icon = Icons.Filled.ChatBubbleOutline, message = L("auth.subtitle"))
            isLoading -> LoadingSpinner()
            partners.isEmpty() -> EmptyState(icon = Icons.Filled.ChatBubbleOutline, message = "No conversations yet")
            else -> LazyColumn {
                items(partners) { partner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.go(Routes.conversation(partner.id, partner.username)) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MangaCoverImage(url = partner.avatar_url, modifier = Modifier.size(40.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(partner.username, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showNewMessage) {
        NewMessageDialog(onDismiss = { showNewMessage = false })
    }
}

@Composable
private fun NewMessageDialog(onDismiss: () -> Unit) {
    val auth = LocalSupabaseAuth.current
    val nav = LocalNavCoordinator.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<SBProfile?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZTheme.surface,
        title = { Text(L("messages.newMessage"), color = ZTheme.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    placeholder = { Text(L("messages.searchUser")) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
                        focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary
                    )
                )
                if (isSearching) LoadingSpinner(modifier = Modifier.height(40.dp))
                errorMessage?.let { Text(it, color = ZTheme.danger, style = MaterialTheme.typography.labelSmall) }
                found?.let { user ->
                    Text(
                        user.username, color = ZTheme.accent, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp).clickable {
                            nav.go(Routes.conversation(user.id, user.username))
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                isSearching = true
                errorMessage = null
                found = null
                scope.launch {
                    try {
                        val user = SupabaseSync.findUser(username, auth)
                        if (user == null) errorMessage = "No user found with that username" else found = user
                    } catch (e: Exception) { errorMessage = e.message }
                    isSearching = false
                }
            }) { Text("Search", color = ZTheme.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = ZTheme.textSecondary) } }
    )
}
