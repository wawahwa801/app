package app.speedbump.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.speedbump.core.Friction
import app.speedbump.core.FrictionMode
import app.speedbump.core.TargetApp
import app.speedbump.service.SpeedbumpService
import kotlinx.coroutines.delay

/** The wall. Leaving it abandons the attempt, so the timer can't run in the background. */
class WallActivity : ComponentActivity() {
    private lateinit var app: TargetApp
    private lateinit var store: SettingsStore
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = TargetApp.valueOf(intent.getStringExtra(EXTRA_APP)!!)
        store = SettingsStore(this)
        val settings = store.load()
        isShowing = true
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = giveUp()
        })
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(app.sectionName, style = MaterialTheme.typography.headlineMedium)
                        when (settings.mode) {
                            FrictionMode.WAIT_ONE_MINUTE -> WaitWall(onDone = ::unlock)
                            FrictionMode.AIME_PROBLEM -> AimeWall(loadProblems(this@WallActivity), onDone = ::unlock)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = ::giveUp, modifier = Modifier.fillMaxWidth()) {
                            Text("Go back")
                        }
                        Text(
                            "Opens for ${settings.unlockMinutes} min once you get through.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isShowing = false
        if (!finished) finish()
    }

    override fun onDestroy() {
        isShowing = false
        super.onDestroy()
    }

    private fun unlock() {
        store.unlock(app, System.currentTimeMillis())
        SpeedbumpService.instance?.onWallUnlocked()
        finished = true
        finish()
    }

    private fun giveUp() {
        SpeedbumpService.instance?.onWallGaveUp(app)
        finished = true
        finish()
    }

    companion object {
        private const val EXTRA_APP = "app"

        @Volatile
        var isShowing = false
            private set

        fun intent(context: Context, app: TargetApp) = Intent(context, WallActivity::class.java)
            .putExtra(EXTRA_APP, app.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun WaitWall(onDone: () -> Unit) {
    var remaining by remember { mutableIntStateOf(Friction.WAIT_SECONDS) }
    LaunchedEffect(Unit) {
        val end = SystemClock.elapsedRealtime() + Friction.WAIT_SECONDS * 1000L
        while (remaining > 0) {
            delay(200)
            remaining = (((end - SystemClock.elapsedRealtime()) + 999) / 1000).toInt().coerceAtLeast(0)
        }
    }
    Text("Still want to go in? Stay on this screen for a minute.")
    Text("${remaining}s", style = MaterialTheme.typography.displayLarge)
    Button(onClick = onDone, enabled = remaining == 0, modifier = Modifier.fillMaxWidth()) {
        Text(if (remaining == 0) "Continue" else "Wait…")
    }
}

@Composable
private fun AimeWall(problems: List<AimeProblem>, onDone: () -> Unit) {
    var problem by remember { mutableStateOf(problems.random()) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var lockedFor by remember { mutableIntStateOf(0) }
    LaunchedEffect(lockedFor) {
        if (lockedFor > 0) {
            delay(1000)
            lockedFor -= 1
        }
    }
    Text("Solve this to go in. The answer is an integer from 000 to 999.")
    Text(problem.text, style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = input,
        onValueChange = { value -> input = value.filter(Char::isDigit).take(3) },
        label = { Text("Answer") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = {
            if (Friction.isCorrect(input, problem.answer)) {
                onDone()
            } else {
                input = ""
                message = "That's not it."
                lockedFor = Friction.WRONG_ANSWER_LOCK_SECONDS
            }
        },
        enabled = lockedFor == 0 && input.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (lockedFor > 0) "Try again in ${lockedFor}s" else "Submit")
    }
    TextButton(
        onClick = {
            problem = problems.filter { it.id != problem.id }.random()
            input = ""
            message = null
            lockedFor = Friction.WRONG_ANSWER_LOCK_SECONDS
        },
        enabled = lockedFor == 0,
    ) {
        Text("Different problem")
    }
}
