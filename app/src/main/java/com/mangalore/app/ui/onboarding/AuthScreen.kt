package com.mangalore.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mangalore.app.R
import com.mangalore.app.data.SupabaseAuth
import com.mangalore.app.localization.L
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { SupabaseAuth.getInstance(context) }
    val scope = rememberCoroutineScope()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackground()
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            WelcomeMascot(modifier = Modifier.height(128.dp))
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.height(46.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(L("auth.welcome"), color = ZTheme.textPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                L("auth.subtitle"), color = ZTheme.textSecondary, style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZTheme.surface.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                    .border(1.dp, ZTheme.borderLight, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSignUp) {
                    AuthField(L("auth.username"), username, { username = it }, Icons.Filled.Person)
                }
                AuthField(L("auth.email"), email, { email = it }, Icons.Filled.Email, keyboardType = KeyboardType.Email)
                AuthField(L("auth.password"), password, { password = it }, Icons.Filled.Lock, isPassword = true)

                errorMessage?.let {
                    Text(it, color = ZTheme.danger, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        scope.launch {
                            try {
                                if (isSignUp) auth.signUp(email, password, username) else auth.signIn(email, password)
                                isLoading = false
                                onFinished()
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Something went wrong. Please try again."
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (!isSignUp || username.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZTheme.accent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = ZTheme.bg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isSignUp) L("auth.signup") else L("auth.login"), color = ZTheme.bg, style = MaterialTheme.typography.titleMedium)
                    }
                }

                TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isSignUp) L("auth.switchToLogin") else L("auth.switchToSignup"),
                        color = ZTheme.accent, style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { auth.continueAsGuest(); onFinished() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZTheme.textPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZTheme.borderLight.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(L("auth.guest"), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun AuthField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = ZTheme.textTertiary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = ZTheme.textTertiary) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ZTheme.card, unfocusedContainerColor = ZTheme.card,
            focusedBorderColor = ZTheme.border, unfocusedBorderColor = ZTheme.border,
            focusedTextColor = ZTheme.textPrimary, unfocusedTextColor = ZTheme.textPrimary,
            cursorColor = ZTheme.accent
        )
    )
}
