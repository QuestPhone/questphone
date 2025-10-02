package neth.iecal.questphone.app.screens.account.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.screens.account.ForgotPasswordStep
import neth.iecal.questphone.core.Supabase
import neth.iecal.questphone.backed.repositories.UserRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")

    val isPasswordVisible = MutableStateFlow(false)
    val isConfirmPasswordVisible = MutableStateFlow(false)

    val isLoading = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    val signUpStep = MutableStateFlow(SignUpStep.FORM)
    val forgetPasswordStep = MutableStateFlow(ForgotPasswordStep.FORM)
    val authStep =  mutableStateOf(AuthStep.SIGNUP)

    fun onEmailChanged(value: String) {
        email.value = value
        errorMessage.value = null
    }

    fun onPasswordChanged(value: String) {
        password.value = value
        errorMessage.value = null
    }

    fun onConfirmPasswordChanged(value: String) {
        confirmPassword.value = value
        errorMessage.value = null
    }

    fun togglePasswordVisibility() {
        isPasswordVisible.value = !isPasswordVisible.value
    }

    fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible.value = !isConfirmPasswordVisible.value
    }

    fun signIn(onSuccess: () -> Unit) {
        val e = email.value
        val p = password.value

        if (e.isBlank() || p.isBlank()) {
            errorMessage.value = "Please fill in all fields"
            return
        }

        if (!e.contains("@") || !e.contains(".")) {
            errorMessage.value = "Please enter a valid email"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Supabase.supabase.auth.signInWith(Email) {
                    this.email = e
                    this.password = p
                }
                onSuccess()
            } catch (e: AuthRestException) {
                errorMessage.value = e.errorDescription + "\n error code: ${e.errorCode}" + "\n ${e.error}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signUp(onVerificationSent: () -> Unit = {}) {
        val e = email.value
        val p = password.value
        val c = confirmPassword.value

        when {
            e.isBlank() || p.isBlank() || c.isBlank() -> {
                errorMessage.value = "Please fill in all fields"
            }
            !e.contains("@") || !e.contains(".") -> {
                errorMessage.value = "Please enter a valid email"
            }
            p.length < 8 -> {
                errorMessage.value = "Password must be at least 8 characters"
            }
            p != c -> {
                errorMessage.value = "Passwords don't match"
            }
            else -> {
                isLoading.value = true
                errorMessage.value = null

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        Supabase.supabase.auth.signUpWith(Email) {
                            this.email = e
                            this.password = p
                        }
                        signUpStep.value = SignUpStep.VERIFICATION
                        onVerificationSent()
                    } catch (e: AuthRestException) {
                        errorMessage.value = e.message ?: "Sign-up failed"
                    } finally {
                        isLoading.value = false
                    }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        val e = email.value
        viewModelScope.launch {
            try {
                isLoading.value = true
                Supabase.supabase.auth.resendEmail(
                    email = e,
                    type = OtpType.Email.SIGNUP
                )
            } catch (e: Exception) {
                errorMessage.value = "Failed to resend email: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun forgetPassword(){

        if (email.value.isBlank()) {
            errorMessage.value = "Please enter your email"
        } else if (!isEmailValid()) {
            errorMessage.value = "Please enter a valid email"
        } else {
            errorMessage.value = null
            isLoading.value = true

            viewModelScope.launch {
                Supabase.supabase.auth.resetPasswordForEmail(email.value)
            }

            forgetPasswordStep.value = ForgotPasswordStep.VERIFICATION
            isLoading.value = false
        }
    }

    fun resetSignUpForm() {
        signUpStep.value = SignUpStep.FORM
    }

    fun isEmailValid(): Boolean {
        return email.value.contains("@") && email.value.contains(".")
    }

    fun signInAnonymously() {
        authStep.value = AuthStep.COMPLETE
        userRepository.userInfo.isAnonymous = true
        userRepository.saveUserInfo()
    }

}

