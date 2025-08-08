package neth.iecal.questphone.app.screens.account

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.app.screens.account.login.AuthStep
import neth.iecal.questphone.app.screens.account.login.LoginViewModel

enum class ForgotPasswordStep {
    FORM,
    VERIFICATION
}
@Composable
fun ForgotPasswordScreen(viewModel: LoginViewModel) {

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val email by viewModel.email.collectAsState()
    val isEmailValid = viewModel.isEmailValid()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val forgetPasswordStep by viewModel.forgetPasswordStep.collectAsState()
    val authStep = viewModel.authStep

    BackHandler {
        authStep.value = AuthStep.LOGIN
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)

    ) {
        // Back button
        IconButton(
            onClick = {
                authStep.value = AuthStep.LOGIN

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
                text = when (forgetPasswordStep) {
                    ForgotPasswordStep.FORM -> "Reset Password"
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

            when (forgetPasswordStep) {
                // Email step
                ForgotPasswordStep.FORM -> {
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChanged(it) },
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
                                viewModel.forgetPassword()
                            }
                        ),
                        isError = errorMessage != null && (email.isBlank() || !isEmailValid)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.forgetPassword() },
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
                        onClick = { viewModel.forgetPasswordStep.value = ForgotPasswordStep.FORM },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to email")
                    }
                }

            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login option
            if (forgetPasswordStep == ForgotPasswordStep.FORM) {
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
                        authStep.value = AuthStep.LOGIN

                    }) {
                        Text("Login")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}