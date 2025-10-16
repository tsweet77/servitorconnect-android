package com.anthroteacher.servitorconnect

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anthroteacher.servitorconnect.data.Frequency
import com.anthroteacher.servitorconnect.data.SavedSettings
import com.anthroteacher.servitorconnect.data.readSettings
import com.anthroteacher.servitorconnect.data.saveSettings
import com.anthroteacher.servitorconnect.service.ServitorService
import com.anthroteacher.servitorconnect.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var loaded by remember { mutableStateOf(false) }
            var intention by rememberSaveable { mutableStateOf("") }
            var burstCountText by rememberSaveable { mutableStateOf("888888") }
            var durationText by rememberSaveable { mutableStateOf("86400") }
            var frequency by rememberSaveable { mutableStateOf(Frequency.Min5) }
            var forceDark by rememberSaveable { mutableStateOf(false) }

            val status by ServitorService.status.collectAsState(ServitorService.Companion.Status())

            LaunchedEffect(Unit) {
                readSettings(context).collectLatest { s ->
                    if (!loaded) {
                        intention = s.intention
                        burstCountText = s.burstCount.coerceAtLeast(1).toString()
                        durationText = s.durationSec.coerceIn(1, 86_400).toString()
                        frequency = s.frequency
                        forceDark = s.forceDark
                        loaded = true
                    }
                }
            }

            val notifPermLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val openDocLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val hash = contentSha512(contentResolver.openInputStream(uri))
                        if (hash.isNotBlank()) {
                            intention = if (intention.isBlank()) hash else "$intention\n$hash"
                        }
                    }
                }
            }

            MyApplicationTheme(forceDark = forceDark) {
                Surface(Modifier.fillMaxSize()) {
                    val scroll = rememberScrollState()

                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("ServitorConnect", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (status.running) "Broadcasting…" else "Ready",
                            color = if (status.running) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = forceDark,
                                onCheckedChange = { forceDark = it },
                                enabled = !status.running
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Dark Mode")
                        }

                        OutlinedTextField(
                            value = intention,
                            onValueChange = { intention = it },
                            label = { Text("Intention") },
                            minLines = 5,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !status.running
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = burstCountText,
                                onValueChange = { burstCountText = it.filter(Char::isDigit).take(9) },
                                label = { Text("Burst Count") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f),
                                enabled = !status.running,
                                supportingText = { Text("positive only", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FrequencySelect(
                                value = frequency,
                                onChange = { frequency = it },
                                enabled = !status.running,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it.filter(Char::isDigit).take(6) },
                            label = { Text("Duration (seconds)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !status.running,
                            supportingText = { Text("1 to 86400") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                                enabled = !status.running
                            ) { Text("Load File") }

                            Spacer(Modifier.weight(1f))

                            Metrics(status.iterations, status.elapsedSec, status.itersPerSec, frequency)
                        }

                        val valid = validateInputs(burstCountText, durationText)
                        Button(
                            // inside Button(onClick = { ... })
                            onClick = {
                                // 1) On Android 13+ make sure we have POST_NOTIFICATIONS before starting the FGS
                                if (!status.running && Build.VERSION.SDK_INT >= 33) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        // Ask first, then return without starting
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Button
                                    }
                                }

                                // 2) Start/Stop as before, but catch the FGS edge-case just in case
                                if (!status.running) {
                                    val settings = SavedSettings(
                                        intention = intention,
                                        burstCount = sanitizedBurst(burstCountText),
                                        frequency = frequency,
                                        durationSec = sanitizedDuration(durationText),
                                        forceDark = forceDark
                                    )
                                    val cfg = ServitorService.Config(
                                        intention = settings.intention,
                                        burstCount = settings.burstCount,
                                        frequency = settings.frequency,
                                        durationSec = settings.durationSec
                                    )
                                    val i = ServitorService.buildIntent(context, start = true, cfg = cfg)
                                    try {
                                        ContextCompat.startForegroundService(context, i)
                                        scope.launch { saveSettings(context, settings) }
                                    } catch (t: Throwable) {
                                        // optional: show a lightweight error instead of crashing
                                        // Snackbar, Toast, etc.
                                    }
                                } else {
                                    val i = ServitorService.buildIntent(context, start = false)
                                    try {
                                        ContextCompat.startForegroundService(context, i)
                                    } catch (_: Throwable) { }
                                }
                            },
                            enabled = valid,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (status.running) "Stop" else "Start") }

                        if (!valid) {
                            Text(
                                "Enter Burst Count ≥ 1 and Duration between 1 and 86400.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencySelect(
    value: Frequency,
    onChange: (Frequency) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (value) {
        Frequency.Max -> "Max"
        Frequency.Hz3 -> "3 Hz"
        Frequency.Hz8 -> "8 Hz"
        Frequency.Min5 -> "5 Min"
    }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Frequency: $label") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Max") }, onClick = { onChange(Frequency.Max); expanded = false })
            DropdownMenuItem(text = { Text("3 Hz") }, onClick = { onChange(Frequency.Hz3); expanded = false })
            DropdownMenuItem(text = { Text("8 Hz") }, onClick = { onChange(Frequency.Hz8); expanded = false })
            DropdownMenuItem(text = { Text("5 Min") }, onClick = { onChange(Frequency.Min5); expanded = false })
        }
    }
}

@Composable
private fun Metrics(iterations: Long, elapsedSec: Int, itersPerSec: Long, freq: Frequency) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text("Iterations: $iterations")
        Text("Timer: ${formatHms(elapsedSec)}")
        if (freq == Frequency.Max) Text("Iter/s: $itersPerSec")
    }
}

private fun validateInputs(burstText: String, durationText: String): Boolean {
    val b = runCatching { burstText.toLong() }.getOrNull() ?: return false
    val d = runCatching { durationText.toInt() }.getOrNull() ?: return false
    return b >= 1 && d in 1..86_400
}

private fun sanitizedBurst(burstText: String): Int {
    val v = runCatching { burstText.toLong() }.getOrDefault(888_888L)
    return v.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
}

private fun sanitizedDuration(durationText: String): Int {
    val v = runCatching { durationText.toInt() }.getOrDefault(86_400)
    return v.coerceIn(1, 86_400)
}

private fun formatHms(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}

private suspend fun contentSha512(input: InputStream?): String {
    input ?: return ""
    return withContext(Dispatchers.IO) {
        input.use { ins ->
            val md = MessageDigest.getInstance("SHA-512")
            val buf = ByteArray(8192)
            var read: Int
            while (ins.read(buf).also { read = it } != -1) {
                md.update(buf, 0, read)
            }
            val bytes = md.digest()
            val bi = BigInteger(1, bytes)
            "SHA512:" + bi.toString(16).padStart(128, '0')
        }
    }
}
