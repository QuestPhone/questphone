package neth.iecal.questphone.app.screens.launcher.dialogs

import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import neth.iecal.questphone.app.navigation.LauncherDialogRoutes
import neth.iecal.questphone.app.theme.LocalCustomTheme


@Composable
fun LowCoinsDialog(
    coins: Int,
    pkgName: String,
    navController: NavController
) {
    val context = LocalContext.current
    val appIconDrawable = context.packageManager.getApplicationIcon(pkgName)
    val bitmap = remember (appIconDrawable) {
        val bitmap =
            createBitmap(appIconDrawable.intrinsicWidth, appIconDrawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        appIconDrawable.setBounds(0, 0, canvas.width, canvas.height)
        appIconDrawable.draw(canvas)
        bitmap.asImageBitmap()
    }
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            bitmap = bitmap,
            contentDescription = "Instagram Icon",
            Modifier
                .size(100.dp)
                .padding(16.dp)
        )
        Text(
            text = "Balance: $coins coins",
            color = textColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val appName = try {
            context.packageManager.getApplicationInfo(pkgName, 0)
                .loadLabel(context.packageManager).toString()
        } catch (_: Exception) {
            pkgName
        }
        Text(
            text = "You're too broke to use $appName right now. ",
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.size(12.dp))

        OutlinedButton(
            onClick = {
                navController.navigate(LauncherDialogRoutes.ShowAllQuest.route)
            },
        ) {
            Text("Start A Quest")
        }
        Spacer(Modifier.size(8.dp))

        Text(
            text = "Just let me in for a while",
            color = textColor,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = {
                    navController.navigate(LauncherDialogRoutes.MakeAChoice.route)
                })
        )

    }
}