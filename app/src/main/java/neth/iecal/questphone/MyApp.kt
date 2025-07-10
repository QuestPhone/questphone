package neth.iecal.questphone

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.services.reloadServiceInfo
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.utils.isOnline
import neth.iecal.questphone.utils.triggerQuestSync
import neth.iecal.questphone.utils.triggerStatsSync

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