package neth.iecal.questphone.app.screens.account

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.FileUploadResponse
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.core.Supabase
import nethical.questphone.data.UserInfo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class SetupProfileViewModel @Inject constructor(
    val userRepository: UserRepository
) : ViewModel() {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isProfileSetupDone = MutableStateFlow(false)
    val isProfileSetupDone: StateFlow<Boolean> = _isProfileSetupDone.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _profileUri = MutableStateFlow<Uri?>(null)
    val profileUri: StateFlow<Uri?> = _profileUri.asStateFlow()

    private val _profileUrl = MutableStateFlow<String?>(null)
    val profileUrl: StateFlow<String?> = _profileUrl.asStateFlow()

    suspend fun initializeProfile() {
        viewModelScope.launch {
            if (userRepository.userInfo.isAnonymous) {
                _isLoading.value = false
            }else {
                try {
                    Supabase.awaitSession()
                    val userId = Supabase.supabase.auth.currentUserOrNull()?.id.toString()

                    val profile = Supabase.supabase.from("profiles")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingleOrNull<UserInfo>()

                    if (profile != null) {
                        userRepository.userInfo = profile
                        if (profile.has_profile) {
                            _profileUrl.value =
                                "https://hplszhlnchhfwngbojnc.supabase.co/storage/v1/object/public/profile/$userId/profile"
                        }
                    } else {
                        userRepository.userInfo.username =
                            squashUserIdToUsername(userId).lowercase()
                        Supabase.supabase.postgrest["profiles"].upsert(userRepository.userInfo)
                    }
                    userRepository.saveUserInfo()

                    _name.value = userRepository.userInfo.full_name
                    _username.value = userRepository.userInfo.username
                    _isLoading.value = false
                }catch (e: Exception){
                    _errorMessage.value = e.message.toString()
                }
            }
        }
    }

    fun updateName(name: String) {
        _name.value = name
        _errorMessage.value = null
    }

    fun updateUsername(username: String) {
        val filtered = username.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
        if (filtered == username) {
            _username.value = filtered.lowercase()
            _errorMessage.value = null
        }
    }

    fun updateProfileImage(uri: Uri?) {
        _profileUri.value = uri
        if (uri != null) {
            _profileUrl.value = null
        }
    }

    fun updateProfile(context: Context) {
        if (_name.value.isBlank() || _username.value.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }

        _isLoading.value = true

        if (userRepository.userInfo.isAnonymous) {
            userRepository.userInfo = UserInfo(
                username = _username.value,
                full_name = _name.value,
                has_profile = _profileUri.value != null || _profileUrl.value != null
            )
            if (_profileUri.value != null) {
                copyFileFromUriToAppStorage(context, _profileUri.value!!)
            }
            userRepository.saveUserInfo()
            _isLoading.value = false
            _isProfileSetupDone.value = true
            return
        }

        viewModelScope.launch {
            try {
                val userId = Supabase.supabase.auth.currentUserOrNull()!!.id

                if (isUsernameTaken(_username.value, userId)) {
                    _errorMessage.value = "This username has already been taken"
                    _isLoading.value = false
                    return@launch
                }

                val avatarUrlResult: FileUploadResponse? = if (_profileUri.value != null) {
                    val avatarBytes = getBytesFromUri(context, _profileUri.value!!)
                    if (avatarBytes == null) {
                        _errorMessage.value = "Failed to read image"
                        _isLoading.value = false
                        return@launch
                    }

                    if (avatarBytes.size > 5 * 1024 * 1024) {
                        _errorMessage.value = "Avatar file is too large (max 5MB)"
                        _isLoading.value = false
                        return@launch
                    }

                    Supabase.supabase.storage
                        .from("profile")
                        .upload(
                            path = "$userId/profile",
                            data = avatarBytes,
                            options = {
                                upsert = true
                            })
                } else {
                    null
                }
                userRepository.userInfo = userRepository.userInfo.copy(
                    username = _username.value,
                    full_name = _name.value,
                    has_profile = _profileUri.value != null || _profileUrl.value != null
                )
                userRepository.saveUserInfo()

                Log.d("SetupProfile", userRepository.userInfo.toString())
                Supabase.supabase.postgrest["profiles"].upsert(userRepository.userInfo)

                _isLoading.value = false
                _isProfileSetupDone.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun SetupProfileScreen(
    isNextEnabledSetupProfile: MutableState<Boolean> = mutableStateOf(false),
    viewModel: SetupProfileViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val username by viewModel.username.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isProfileSetupDone by viewModel.isProfileSetupDone.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val profileUri by viewModel.profileUri.collectAsState()
    val profileUrl by viewModel.profileUrl.collectAsState()

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateProfileImage(uri)
    }

    LaunchedEffect(isNextEnabledSetupProfile.value) {
        if (isNextEnabledSetupProfile.value == false) {
            viewModel.initializeProfile()
            isNextEnabledSetupProfile.value = true
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            when {
                                profileUrl != null -> profileUrl
                                profileUri != null -> profileUri
                                else -> R.drawable.baseline_person_24
                            }
                        )
                        .crossfade(true)
                        .error(R.drawable.baseline_person_24)
                        .placeholder(R.drawable.baseline_person_24)
                        .build(),
                ),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable {
                        launcher.launch("image/*")
                    },
                colorFilter = if (profileUri == null && profileUrl == null)
                    ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                else
                    null,
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = viewModel::updateName,
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isProfileSetupDone) {
                Button(
                    onClick = { viewModel.updateProfile(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update Profile")
                    }
                }
            } else {
                Text("Profile setup successful!!")
            }

            Spacer(modifier = Modifier.height(12.dp))

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
        }
    }
}

// Utility functions
private suspend fun isUsernameTaken(username: String, id: String): Boolean {
    return try {
        val result = Supabase.supabase
            .from("profiles")
            .select(columns = Columns.list("id")) {
                filter {
                    eq("username", username)
                    neq("id", id)
                }
            }
            .decodeList<UserInfo>()

        result.isNotEmpty()
    } catch (e: Exception) {
        println("Error checking username: ${e.message}")
        true
    }
}

private fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        println("Error reading Uri: ${e.message}")
        null
    }
}

private fun squashUserIdToUsername(userId: String): String {
    val bytes = userId.toByteArray(Charsets.UTF_8)
    val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    return base64.take(5) + "_" +System.currentTimeMillis().toString().take(3)  // first 5 chars
}

private fun copyFileFromUriToAppStorage(
    context: Context,
    uri: Uri,
): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val destinationFile = File(context.filesDir, "profile")

        FileOutputStream(destinationFile).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }

        destinationFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}