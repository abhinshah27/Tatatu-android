package rs.highlande.app.tatatu

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.comscore.Analytics
import com.comscore.PublisherConfiguration
import com.comscore.UsagePropertiesAutoUpdateMode
//import com.comscore.Analytics
//import com.comscore.PublisherConfiguration
//import com.comscore.UsagePropertiesAutoUpdateMode
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import io.branch.referral.Branch
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import rs.highlande.app.tatatu.connection.webSocket.WebSocketConnection
import rs.highlande.app.tatatu.core.di.*
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager


class TTUApp : Application() {

    companion object {
        val TAG = TTUApp::class.java.simpleName

        var deviceConnected = false
        private val networkRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()


        var socketConnecting = false
        var subscribedToSocket = false
        var socketConnectingChat = false
        var subscribedToSocketChat = false

        var canPlaySounds = false
        var canVibrate = false

        var hasValidCredentials = false
            set(value) {
                LogUtils.d(TAG, "(1) SET hasValidCredentials = $value")
                field = value
            }

        // INFO: 2020-01-09    Lottie is commented
//        lateinit var siriComposition: LottieComposition
//        fun initLottieAnimation(context: Context, listener: LottieListener<LottieComposition>?) {
//            val task = LottieCompositionFactory.fromRawRes(context, R.raw.lottie_wave)
//            task.addListener(listener ?: LottieListener { result -> siriComposition = result })
//        }

    }



    private val authManager: AuthManager by inject()
    internal val foregroundManager: ForegroundManager by inject()
    private val socketConnection: WebSocketConnection by inject()
    private var ringerReceiver:RingerChangedReceiver ?= null
    private val preferenceHelper: PreferenceHelper by inject()

    enum class Connection { ON, OFF, LOSING }

    private var connectionStatus: Connection = Connection.OFF


    private val connectionObservers = mutableSetOf<DeviceConnectionListener?>()

    override fun onCreate() {
        super.onCreate()

        // start Koin!
        startKoin {

            // declare used Android context
            androidContext(this@TTUApp)

            // declare the level for logging
            androidLogger()
            printLogger()

            // declare modules
            modules(
                listOf(
                    appModule,
                    homeModule,
                    createPostModule,
                    postModule,
                    suggestedModule,
                    profileModule,
                    loginSignupModule,
                    connectionModule,
                    multimediaModule,
                    settingsModule,
                    inviteModule,
                    walletModule,
                    notificationModule,
                    callModule,
                    chatModule,
                    cacheModule
                )
            )
        }

        //Realm.IO
        RealmUtils.init(this)


        // Handle unhandled error
        RxJavaPlugins.setErrorHandler { throwable ->
            LogUtils.e(TAG, throwable.javaClass.name)
            LogUtils.e(TAG, throwable.message)
            throwable.printStackTrace()
        }


        // INFO: 2020-01-09    Lottie is commented
//        initLottieAnimation(this, null)


//        authManager.checkAndFetchCredentials()


        // INFO: 2019-09-04    currently NO TIMBER
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//        }


        with(foregroundManager) {
            registerActivityLifecycleCallbacks(this)
            registerListener(object : ForegroundManager.ForegroundListener {
                override fun onBecameForeground() {
                    LogUtils.i(TAG, "Became Foreground")

                    authManager.checkAndFetchCredentials()
                    LogUtils.i(TAG, "CONNECTION: hasValidCredentials = $hasValidCredentials")

                    startNetworkListener()

                    registerReceivers()
                }

                override fun onBecameBackground() {
                    LogUtils.i(TAG, "Became Background")

                    stopNetworkListener()

                    unregisterReceivers()

                    // reset value so that every bg/fg cycle starts from scratch
                    hasValidCredentials = false
                    socketConnecting = false
                    socketConnectingChat = false

                    subscribedToSocket = false
                    subscribedToSocketChat = false

                    if (isSocketConnected()) closeSocketConnection()

                    deviceConnected = false
                }
            })
        }


        /* EMOJI SUPPORT */
        val emojiRequest = FontRequest(getString(R.string.font_provider_authority), getString(R.string.font_provider_package), getString(R.string.font_query_emoji), R.array.com_google_android_gms_fonts_certs)
        val config = FontRequestEmojiCompatConfig(this, emojiRequest).setReplaceAll(true).registerInitCallback(object : EmojiCompat.InitCallback() {
            override fun onInitialized() {
                super.onInitialized()
                LogUtils.d(TAG, "Emoji Font initialization SUCCESS")
            }

            override fun onFailed(throwable: Throwable?) {
                super.onFailed(throwable)
                LogUtils.e(TAG, "Emoji Font initialization FAILED", throwable)
            }
        })
        EmojiCompat.init(config)


        /* Night mode */
        val nightMode =
            if (hasQ()) (getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.nightMode ?: -1
            else preferenceHelper.getTheme()

        when (nightMode) {
            UiModeManager.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }


        /* Fabric Crashlytics */
        // Set up Crashlytics, disabled for debug builds
        val crashlyticsKit = if (BuildConfig.USE_CRASHLYTICS) Crashlytics()
        else {
            Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build()
        }

        // Initialize Fabric with the debug-disabled Crashlytics.
        Fabric.with(this, crashlyticsKit)

        ringerReceiver = RingerChangedReceiver()

        // Initialize Branch.io
        Branch.getAutoInstance(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(TTULifecycleObserver())

        // Initialize comScore
        PublisherConfiguration.Builder()
            .publisherId(getString(R.string.comscore_publisher_id)) // Publisher ID
            .build().let {
                Analytics.getConfiguration().apply {

                    addClient(it)

                    if (BuildConfig.DEBUG)
                        enableImplementationValidationMode()

                    setApplicationName("TTUa")

                    // TODO: 2019-10-30    find out about these
                    setUsagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED)    // or FOREGROUND_ONLY?
                    setUsagePropertiesAutoUpdateInterval(60) // default

                }
                Analytics.start(this@TTUApp)
            }

    }


    private fun startNetworkListener() {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun stopNetworkListener() {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(
            networkCallback
        )

    }


    var connectedCount = 0
    private var connectionWasLost = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onUnavailable() {
            super.onUnavailable()
            LogUtils.e(TAG, "CONNECTION: UNAVAILABLE")
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            LogUtils.i(TAG, "CONNECTION: OK")

            connectedCount++

            if (connectionWasLost) {
                connectionWasLost = false
                notifyObservers(ConnectionStatus.CONNECTED)
            }

            connectionStatus = Connection.ON
            deviceConnected = true

            LogUtils.i(TAG, "CONNECTION: hasValidCredentials = $hasValidCredentials")
            if (hasValidCredentials)
                reconnect()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            LogUtils.i(TAG, "CONNECTION: LOST")

            if (connectedCount > 0)
                connectedCount--

            if (connectedCount == 0) {
                LogUtils.i(TAG, "CONNECTION: REALLY LOST")

                connectionStatus = Connection.OFF
                deviceConnected = false
                connectionWasLost = true

                notifyObservers(ConnectionStatus.LOST)
            }
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            LogUtils.i(TAG, "CONNECTION: LOSING")

            connectionStatus = Connection.LOSING
        }
    }


    fun attachObserver(activity: DeviceConnectionListener?) = connectionObservers.add(activity)
    fun removeObserver(activity: DeviceConnectionListener?) = connectionObservers.remove(activity)


    enum class ConnectionStatus { CONNECTED, LOST }
    private fun notifyObservers(status: ConnectionStatus) {
        connectionObservers.forEach {
            it?.let {
                if (status == ConnectionStatus.CONNECTED) it.notifyConnectionAcquired()
                else it.notifyConnectionLost()
            } ?: connectionObservers.remove(it)
        }
    }


    /**
     * Takes care of the registration of all receivers linked directly to [Application].
     */
    private fun registerReceivers() {
        registerReceiver(ringerReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
    }

    /**
     * Takes care of the un-registration of all receivers linked directly to [Application].
     */
    private fun unregisterReceivers() {
        unregisterReceiver(ringerReceiver)
    }

    /**
     * Closes socket connection on becoming background.
     */
    private fun closeSocketConnection() {
        LogUtils.d(TAG, "Closing socket connection from TTUApp")
        socketConnection.closeConnection()
    }

    /**
     * Checks whether at least one socket is connected.
     */
    private fun isSocketConnected(): Boolean {
        return socketConnection.isConnected(false) || socketConnection.isConnectedChat(false)
    }

    /**
     * Takes care of the reconnection of the socket.
     */
    private fun reconnect() {
        if (!socketConnecting && !authManager.onPauseForSocial) {
            LogUtils.d(TAG, "Connecting socket from TTUApp.ConnectionListener")
            socketConnecting = true
            socketConnection.openConnection(false)
        }

        if (!socketConnectingChat && !authManager.onPauseForSocial) {
            LogUtils.d(TAG, "Connecting socket CHAT from TTUApp.ConnectionListener")
            socketConnectingChat = true
            socketConnection.openConnection(true)
        }
    }



    fun isForeground() = foregroundManager.isForeground



    interface DeviceConnectionListener {

        fun notifyConnectionLost()
        fun notifyConnectionAcquired()

    }

    // INFO: 2019-10-23    Consider this solution to replace ForegroundManager: TO BE IMPLEMENTED and TESTED
    inner class TTULifecycleObserver: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onMoveToForeground() {
            val notificationManager = NotificationManagerCompat.from(this@TTUApp)
            notificationManager.cancelAll()
        }
    }

}