package com.mangalore.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mangalore.app.R
import com.mangalore.app.localization.AppLanguage
import com.mangalore.app.localization.L
import com.mangalore.app.localization.LocalizationManager
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun LanguageSelectionScreen(onFinished: () -> Unit) {
    var selected by remember { mutableStateOf(LocalizationManager.language) }

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackground()
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.height(50.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(L("onboarding.chooseLanguage"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
            }

            AppLanguage.values().forEach { lang ->
                val isSelected = selected == lang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZTheme.card.copy(alpha = 0.85f))
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) ZTheme.accent else ZTheme.border, RoundedCornerShape(14.dp))
                        .clickable { selected = lang }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lang.displayName, color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) ZTheme.accent else ZTheme.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    LocalizationManager.updateLanguage(selected)
                    onFinished()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(L("onboarding.continue"), color = ZTheme.bg, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
