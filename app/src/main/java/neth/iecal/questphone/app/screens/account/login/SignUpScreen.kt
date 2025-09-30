package neth.iecal.questphone.app.screens.account.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import neth.iecal.questphone.BuildConfig
import neth.iecal.questphone.R

enum class SignUpStep {
    FORM,
    VERIFICATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(viewModel: LoginViewModel, onAnonymousSignInSuccess: () -> Unit) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isEmailValid = viewModel.isEmailValid()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authStep = viewModel.authStep

    val confirmPassword by viewModel.confirmPassword.collectAsState()

    val isConfirmPasswordVisible by viewModel.isConfirmPasswordVisible.collectAsState()
    val signUpStep by viewModel.signUpStep.collectAsState()

    val scope = rememberCoroutineScope()

    // Email and password validation
    val isPasswordValid = password.length >= 8
    val doPasswordsMatch = password == confirmPassword

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
    ) {
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
                text = "Quest Phone",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (signUpStep) {
                    SignUpStep.FORM -> "Create an account"
                    SignUpStep.VERIFICATION -> "Verify your email"
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            when (signUpStep) {
                SignUpStep.FORM -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = errorMessage != null && (email.isBlank() || !isEmailValid)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(
                                        if (isPasswordVisible) R.drawable.baseline_visibility_off_24
                                        else R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = errorMessage != null && (password.isBlank() || !isPasswordValid)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.toggleConfirmPasswordVisibility()
                            }) {
                                Icon(
                                    painter = painterResource(
                                        if (isConfirmPasswordVisible) R.drawable.baseline_visibility_off_24
                                        else R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = errorMessage != null && (confirmPassword.isBlank() || !doPasswordsMatch)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign up button
                    Button(
                        onClick = {
                           viewModel.signUp()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text("Sign Up")
                        }
                    }
                }

                SignUpStep.VERIFICATION -> {
                    Text(
                        text = "We've sent a verification email to",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "If you dont find the email in your inbox, please check the spam folder. (Please mark it as not spam in case so1)",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Resend email button
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.resendVerificationEmail()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading
                    ) {
                        Text("Resend email")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Back button
                    TextButton(
                        onClick = { viewModel.signUpStep.value = SignUpStep.FORM },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to sign up")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login option
            if (signUpStep == SignUpStep.FORM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
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

            if(BuildConfig.IS_FDROID){
                var isContinueWithoutLoginDialog = remember { mutableStateOf(false) }
                if (isContinueWithoutLoginDialog.value) {
                    AlertDialog(
                        onDismissRequest = { isContinueWithoutLoginDialog.value = false },
                        title = {
                            Text(text = "Warning")
                        },
                        text = {
                            Text("If you continue in this mode, you might never be able to access your data again after uninstalling this app.")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                isContinueWithoutLoginDialog.value = false
                                viewModel.signInAnonymously()
                                onAnonymousSignInSuccess()
                            }) {
                                Text("Continue Anyway")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isContinueWithoutLoginDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Continue without an account",
                    textAlign = TextAlign.Center,color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable{
                    isContinueWithoutLoginDialog.value = true
                })
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}