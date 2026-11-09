package app.speedbump.ui

import android.content.Context
import app.speedbump.core.FrictionMode
import app.speedbump.core.MetricKind
import app.speedbump.core.PolicyState
import app.speedbump.core.TargetApp
import org.json.JSONArray
import org.json.JSONObject

data class Settings(
    val mode: FrictionMode,
    val unlockMinutes: Int,
    val blocked: Set<TargetApp>,
    val hiddenMetrics: Set<MetricKind>,
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("speedbump", Context.MODE_PRIVATE)

    fun load() = Settings(
        mode = FrictionMode.valueOf(prefs.getString("mode", FrictionMode.WAIT_ONE_MINUTE.name)!!),
        unlockMinutes = prefs.getInt("unlockMinutes", 5),
        blocked = TargetApp.entries.filterTo(mutableSetOf()) { prefs.getBoolean("block_${it.name}", true) },
        hiddenMetrics = MetricKind.entries.filterTo(mutableSetOf()) { prefs.getBoolean("hide_${it.name}", false) },
    )

    fun setMode(mode: FrictionMode) = prefs.edit().putString("mode", mode.name).apply()
    fun setUnlockMinutes(minutes: Int) = prefs.edit().putInt("unlockMinutes", minutes).apply()
    fun setBlocked(app: TargetApp, on: Boolean) = prefs.edit().putBoolean("block_${app.name}", on).apply()
    fun setHidden(kind: MetricKind, on: Boolean) = prefs.edit().putBoolean("hide_${kind.name}", on).apply()

    fun isUnlocked(app: TargetApp, nowMs: Long) = nowMs < prefs.getLong("unlockedUntil_${app.name}", 0)

    fun unlock(app: TargetApp, nowMs: Long) =
        prefs.edit().putLong("unlockedUntil_${app.name}", nowMs + load().unlockMinutes * 60_000L).apply()

    fun savePolicyState(app: TargetApp, state: PolicyState) {
        val json = JSONObject()
            .put("came", state.cameFromDms)
            .put("watching", state.watchingSentItem)
            .put("travel", state.travelPx)
        state.baselineKey?.let { json.put("key", it) }
        state.baselineIndex?.let { json.put("index", it) }
        state.gaveUpAt?.let { json.put("gaveUp", it) }
        prefs.edit().putString("policy_${app.name}", json.toString()).apply()
    }

    fun loadPolicyState(app: TargetApp): PolicyState {
        val json = JSONObject(prefs.getString("policy_${app.name}", null) ?: return PolicyState())
        return PolicyState(
            cameFromDms = json.optBoolean("came"),
            watchingSentItem = json.optBoolean("watching"),
            baselineKey = if (json.has("key")) json.getString("key") else null,
            baselineIndex = if (json.has("index")) json.getInt("index") else null,
            travelPx = json.optLong("travel"),
            gaveUpAt = if (json.has("gaveUp")) json.getLong("gaveUp") else null,
        )
    }
}

data class AimeProblem(val id: Int, val topic: String, val text: String, val answer: Int)

fun loadProblems(context: Context): List<AimeProblem> {
    val json = context.assets.open("aime-problems.json").bufferedReader().use { it.readText() }
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        AimeProblem(item.getInt("id"), item.getString("topic"), item.getString("text"), item.getInt("answer"))
    }
}
