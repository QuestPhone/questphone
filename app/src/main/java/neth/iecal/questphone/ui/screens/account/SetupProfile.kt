package neth.iecal.questphone.ui.screens.account

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.FileUploadResponse
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.UserInfo
import neth.iecal.questphone.data.game.saveUserInfo
import neth.iecal.questphone.utils.Supabase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Base64

@Composable
fun SetupProfileScreen(isNextEnabledSetupProfile: MutableState<Boolean> = mutableStateOf(false)) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isProfileSetupDone by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    var profileUri by remember { mutableStateOf<Uri?>(null) }
    var profileUrl by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileUri = uri
            profileUrl = null
        }
    }
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        if(User.userInfo.isAnonymous) {
            isNextEnabledSetupProfile.value = true
            isLoading = false
            return@LaunchedEffect
        }
        val userId = Supabase.supabase.auth.currentUserOrNull()!!.id

        val profile = Supabase.supabase.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<UserInfo>()
        if (profile != null) {
            User.userInfo = profile
            if (profile.has_profile) {
                profileUrl =
                    "https://hplszhlnchhfwngbojnc.supabase.co/storage/v1/object/public/profile/$userId/profile"
            }
        } else {
            User.userInfo.username = squashUserIdToUsername(userId)
            Supabase.supabase.postgrest["profiles"].upsert(
                User.userInfo
            )
        }
        User.saveUserInfo()
        name = User.userInfo.full_name
        username = User.userInfo.username
        isLoading = false
        isNextEnabledSetupProfile.value = true
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
                            if (profileUrl != null)
                                profileUrl
                            else if (profileUri != null) profileUri
                            else R.drawable.baseline_person_24
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
                onValueChange = { name = it; errorMessage = null },
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
                onValueChange = {
                    val filtered = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' }
                    if (filtered == it) {
                        username = filtered
                        errorMessage = null
                    }
                },
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
                    onClick = {
                        if (name.isBlank() || username.isBlank()) {
                            errorMessage = "Please fill in all fields."
                            return@Button
                        }
                        isLoading = true
                        if(User.userInfo.isAnonymous){
                            User.userInfo = UserInfo(
                                username = username,
                                full_name = name,
                                has_profile = profileUri != null || profileUrl != null
                            )
                            if(profileUri!=null){
                                copyFileFromUriToAppStorage(context,profileUri!!)
                            }
                            User.saveUserInfo()
                            isLoading = false
                            isProfileSetupDone = true
                            return@Button
                        }
                        coroutineScope.launch {
                            isNextEnabledSetupProfile.value = true
                            val userId = Supabase.supabase.auth.currentUserOrNull()!!.id
                            if (isUsernameTaken(username, userId)) {
                                errorMessage = "This username has already been taken"
                                isLoading = false
                                return@launch
                            }
                            val avatarUrlResult: FileUploadResponse? = if (profileUri != null) {
                                val avatarBytes = getBytesFromUri(context, profileUri!!)
                                if (avatarBytes == null) {
                                    errorMessage = "Failed to read image"
                                    isLoading = false
                                    return@launch
                                }

                                if (avatarBytes.size > 5 * 1024 * 1024) {
                                    errorMessage = "Avatar file is too large (max 5MB)"
                                    isLoading = false
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

                            User.userInfo = UserInfo(
                                username = username,
                                full_name = name,
                                has_profile = profileUri != null || profileUrl != null
                            )
                            User.saveUserInfo()

                            Log.d("SetupProfile", User.userInfo.toString())
                            Supabase.supabase.postgrest["profiles"].upsert(
                                User.userInfo
                            )
                            isLoading = false
                            isNextEnabledSetupProfile.value = true

                            isProfileSetupDone = true
                        }
                    },
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
suspend fun isUsernameTaken(username: String,id:String): Boolean {
    return try {
        val result = Supabase.supabase
            .from("profiles")
            .select(columns = Columns.list("id")) {
                filter {
                    eq("username", username)
                    neq("id",id)
                }
            }
            .decodeList<UserInfo>()

        return result.isNotEmpty()
        //check if the record is the users record itself
    } catch (e: Exception) {
        println("Error checking username: ${e.message}")
        true
    }
}
fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        println("Error reading Uri: ${e.message}")
        null
    }
}
fun squashUserIdToUsername(userId: String): String {
    val bytes = userId.toByteArray(Charsets.UTF_8)
    val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    return base64.take(5)  // first 5 chars
}
fun copyFileFromUriToAppStorage(
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
