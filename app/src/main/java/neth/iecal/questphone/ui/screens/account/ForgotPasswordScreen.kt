package neth.iecal.questphone.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import neth.iecal.questphone.core.utils.managers.Supabase

enum class ForgotPasswordStep {
    EMAIL,
    VERIFICATION
}
@Composable
fun ForgotPasswordScreen(loginStep: MutableState<LoginStep>) {
    // States
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var forgotPasswordStep by remember { mutableStateOf(ForgotPasswordStep.EMAIL) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()
    // Email validation
    val isEmailValid = email.contains("@") && email.contains(".")

    // Function to handle email submission
    val handleEmailSubmit = {
        if (email.isBlank()) {
            errorMessage = "Please enter your email"
        } else if (!isEmailValid) {
            errorMessage = "Please enter a valid email"
        } else {
            errorMessage = null
            isLoading = true

            coroutineScope.launch {
                Supabase.supabase.auth.resetPasswordForEmail(email)
            }

            forgotPasswordStep = ForgotPasswordStep.VERIFICATION
            isLoading = false
        }
    }


    BackHandler {
        loginStep.value = LoginStep.LOGIN
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)

    ) {
        // Back button
        IconButton(
            onClick = {
                loginStep.value = LoginStep.LOGIN

            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back to login"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo or app name
            Text(
                text = "QuestPhone",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (forgotPasswordStep) {
                    ForgotPasswordStep.EMAIL -> "Reset Password"
                    ForgotPasswordStep.VERIFICATION -> "Check your email"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error message
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            when (forgotPasswordStep) {
                // Email step
                ForgotPasswordStep.EMAIL -> {
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                handleEmailSubmit()
                            }
                        ),
                        isError = errorMessage != null && (email.isBlank() || !isEmailValid)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit button
                    Button(
                        onClick = handleEmailSubmit,
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Password Reset Link")
                        }
                    }
                }

                // Verification step
                ForgotPasswordStep.VERIFICATION -> {
                    Text(
                        text = "We've sent a verification link to",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Please make sure to check the spam folder in case you don't find any emails from us.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Back button
                    TextButton(
                        onClick = { forgotPasswordStep = ForgotPasswordStep.EMAIL },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to email")
                    }
                }

            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login option
            if (forgotPasswordStep == ForgotPasswordStep.EMAIL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remember your password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = {
                        loginStep.value = LoginStep.LOGIN

                    }) {
                        Text("Login")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}