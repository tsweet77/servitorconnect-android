package com.anthroteacher.servitorconnect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.anthroteacher.servitorconnect.MainActivity
import com.anthroteacher.servitorconnect.R
import com.anthroteacher.servitorconnect.data.Frequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.Serializable
import kotlin.math.min

class ServitorService : android.app.Service() {

    companion object {
        private const val CHANNEL_ID = "servitor_channel"
        private const val NOTIF_ID = 1001

        data class Status(
            val running: Boolean = false,
            val statusText: String = "Ready",
            val iterations: Long = 0L,
            val elapsedSec: Int = 0,
            val itersPerSec: Long = 0L
        )

        private val _status = MutableStateFlow(Status())
        val status = _status.asStateFlow()

        fun buildIntent(ctx: Context, start: Boolean, cfg: Config? = null) =
            Intent(ctx, ServitorService::class.java).apply {
                action = if (start) ACTION_START else ACTION_STOP
                if (start && cfg != null) putExtra(EXTRA_CONFIG, cfg)
            }

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_CONFIG = "config"
    }

    data class Config(
        val intention: String,
        val burstCount: Int,
        val frequency: Frequency,
        val durationSec: Int
    ) : Serializable

    private var job: Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val cfg: Config? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getSerializableExtra(EXTRA_CONFIG, Config::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_CONFIG) as? Config
                }
                if (cfg == null) return START_NOT_STICKY
                startForegroundInternal()
                startBroadcast(cfg)
            }
            ACTION_STOP -> stopBroadcast()
        }
        return START_STICKY
    }
    private fun startForegroundInternal() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notif_channel_desc) }
            nm.createNotificationChannel(ch)
        }
        startForeground(NOTIF_ID, buildNotification("Ready"))
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun startBroadcast(cfg: Config) {
        if (job?.isActive == true) return
        job = CoroutineScope(Dispatchers.Default).launch {
            var iterations = 0L
            val startMs = System.currentTimeMillis()
            var lastSecMark = startMs
            var itersAtLastSec = 0L

            _status.value = _status.value.copy(running = true, statusText = "Broadcasting…")
            updateNotification("Broadcasting…")

            val endMs = startMs + cfg.durationSec * 1000L
            val chunk = 100_000

            while (isActive && System.currentTimeMillis() < endMs) {
                // one burst (chunked)
                var remaining = cfg.burstCount
                while (remaining > 0 && isActive) {
                    val step = min(chunk, remaining)
                    repeat(step) {
                        @Suppress("UNUSED_VARIABLE")
                        val placeholder = cfg.intention
                        iterations++
                    }
                    remaining -= step
                    yield()
                }

                // frequency
                when (cfg.frequency) {
                    Frequency.Max -> {
                        // cooperative; “fast enough” under throttling
                        yield()
                        // or delay(1)
                    }
                    Frequency.Hz3 -> delay(333L)
                    Frequency.Hz8 -> delay(125L)
                    Frequency.Min5 -> {
                        // Break 5-minute delay into 1-second updates
                        val targetDelayMs = 300_000L
                        val startDelayTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startDelayTime < targetDelayMs && isActive) {
                            delay(1000L)  // Wait 1 second
                            
                            // Update metrics each second
                            val now = System.currentTimeMillis()
                            val elapsedSec = ((now - startMs) / 1000L).toInt()
                            _status.value = _status.value.copy(
                                iterations = iterations,
                                elapsedSec = elapsedSec,
                                itersPerSec = 0L
                            )
                            updateNotification("Broadcasting…")
                        }
                    }
                }

                // secondly metrics
                val now = System.currentTimeMillis()
                if (now - lastSecMark >= 1000L) {
                    val elapsedSec = ((now - startMs) / 1000L).toInt()
                    val ips = iterations - itersAtLastSec
                    itersAtLastSec = iterations
                    lastSecMark = now
                    _status.value = _status.value.copy(
                        iterations = iterations,
                        elapsedSec = elapsedSec,
                        itersPerSec = if (cfg.frequency == Frequency.Max) ips else 0L
                    )
                    updateNotification("Broadcasting…")
                }
            }

            _status.value = _status.value.copy(running = false, statusText = "Ready")
            updateNotification("Ready")
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopBroadcast() {
        job?.cancel()
        job = null
        _status.value = _status.value.copy(running = false, statusText = "Ready")
        updateNotification("Ready")
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
