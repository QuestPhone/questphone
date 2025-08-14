package neth.iecal.questphone.app.screens.account.login

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.R


@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel(), onLoginSucess: ()->Unit) {

    var isPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isEmailValid = viewModel.isEmailValid()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authStep = viewModel.authStep

    val context = LocalContext.current
    BackHandler {
        authStep.value = AuthStep.SIGNUP
    }

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
                text = "Sign in to continue",
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = errorMessage != null && email.isBlank() || (email.isNotBlank() && !isEmailValid)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
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
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            painter = if (isPasswordVisible)
                                painterResource(id = R.drawable.baseline_visibility_off_24)
                            else
                                painterResource(id = R.drawable.baseline_visibility_24),
                            contentDescription = if (isPasswordVisible)
                                "Hide password"
                            else
                                "Show password"
                        )
                    }
                },
                isError = errorMessage != null && password.isBlank()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot password
            TextButton(
                onClick = {
                    authStep.value = AuthStep.FORGOT_PASSWORD
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.signIn(
                        onSuccess = onLoginSucess,
                    )
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
                    Text("Continue")
                }
            }


            Spacer(modifier = Modifier.height(32.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = {
                    authStep.value = AuthStep.SIGNUP
                }) {
                    Text("Sign up")
                }
            }


            Spacer(modifier = Modifier.weight(1f))
        }
    }
}