package app.speedbump.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.speedbump.core.FrictionMode
import app.speedbump.core.MetricKind
import app.speedbump.core.TargetApp
import app.speedbump.service.SpeedbumpService

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

class MainActivity : ComponentActivity() {
    private lateinit var store: SettingsStore
    private val serviceEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = SettingsStore(this)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    SettingsScreen(store, serviceEnabled.value) {
                        startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = getSystemService(AccessibilityManager::class.java)
        serviceEnabled.value = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.name == SpeedbumpService::class.java.name && it.resolveInfo.serviceInfo.packageName == packageName }
    }
}

@Composable
private fun SettingsScreen(store: SettingsStore, serviceEnabled: Boolean, openAccessibility: () -> Unit) {
    var settings by remember { mutableStateOf(store.load()) }
    fun refresh() { settings = store.load() }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Speedbump", style = MaterialTheme.typography.headlineMedium)

        if (!serviceEnabled) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Turn on Speedbump in Accessibility", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Speedbump uses Android's Accessibility Service to see when you're in Reels, Shorts or " +
                            "Channels and to cover Instagram counts. It reads only Instagram, YouTube and WeChat " +
                            "screens, stores none of that content, and has no internet permission.",
                    )
                    Text(
                        "If Android says the setting is restricted: open this app's App info, tap ⋮, choose " +
                            "\"Allow restricted settings\", then try again.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = openAccessibility) { Text("Open Accessibility settings") }
                }
            }
        }

        Section("Sections to block") {
            TargetApp.entries.forEach { app ->
                ToggleRow(app.sectionName, app in settings.blocked) {
                    store.setBlocked(app, it)
                    refresh()
                }
            }
        }

        Section("To get past a block") {
            ModeRow("Wait one minute", settings.mode == FrictionMode.WAIT_ONE_MINUTE) {
                store.setMode(FrictionMode.WAIT_ONE_MINUTE)
                refresh()
            }
            ModeRow("Solve an AIME-style problem", settings.mode == FrictionMode.AIME_PROBLEM) {
                store.setMode(FrictionMode.AIME_PROBLEM)
                refresh()
            }
            Text("Stay open for", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 15).forEach { minutes ->
                    FilterChip(
                        selected = settings.unlockMinutes == minutes,
                        onClick = {
                            store.setUnlockMinutes(minutes)
                            refresh()
                        },
                        label = { Text("$minutes min") },
                    )
                }
            }
        }

        Section("Hide on Instagram") {
            val labels = mapOf(
                MetricKind.LIKES to "Like counts",
                MetricKind.VIEWS to "View counts",
                MetricKind.FOLLOWERS to "Follower counts",
            )
            MetricKind.entries.forEach { kind ->
                ToggleRow(labels.getValue(kind), kind in settings.hiddenMetrics) {
                    store.setHidden(kind, it)
                    refresh()
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}
