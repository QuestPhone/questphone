package neth.iecal.questphone

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import dagger.hilt.android.HiltAndroidApp
import nethical.questphone.backend.isOnline
import nethical.questphone.backend.triggerQuestSync
import nethical.questphone.backend.triggerStatsSync
import nethical.questphone.core.core.services.reloadServiceInfo
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.User


@HiltAndroidApp(Application::class)
class MyApp : Application() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate() {
        super.onCreate()

        User.init(this)
        VibrationHelper.init(this)
        reloadServiceInfo(this)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger sync when network becomes available
                triggerQuestSync(applicationContext)
                triggerStatsSync(applicationContext)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        if (isOnline()) {
            triggerQuestSync(applicationContext)
        }
    }
}