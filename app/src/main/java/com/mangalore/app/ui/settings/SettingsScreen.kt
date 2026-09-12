package com.mangalore.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mangalore.app.R
import com.mangalore.app.localization.AppLanguage
import com.mangalore.app.localization.LocalizationManager
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun SettingsScreen() {
    var reduceMotion by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(LocalizationManager.language) }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text("Settings", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection(title = "Language / اللغة") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val selected = language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { language = lang; LocalizationManager.updateLanguage(lang) },
                            label = { Text(lang.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZTheme.accent, selectedLabelColor = ZTheme.bg,
                                containerColor = ZTheme.card, labelColor = ZTheme.textPrimary
                            )
                        )
                    }
                }
            }

            SettingsSection(title = null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduce Motion", color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = reduceMotion, onCheckedChange = { reduceMotion = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ZTheme.accent, checkedTrackColor = ZTheme.accent.copy(alpha = 0.5f))
                    )
                }
                Text(
                    "All reading & zoom settings are now inside the reader itself — tap the ⚙️ icon while reading a chapter.",
                    color = ZTheme.textTertiary, style = MaterialTheme.typography.labelSmall
                )
            }

            SettingsSection(title = "About") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.height(22.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Version 1.0", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column {
        title?.let {
            Text(it, color = ZTheme.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZTheme.card, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
