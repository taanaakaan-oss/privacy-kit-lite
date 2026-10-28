package com.sal.privacykit.xposed

import android.util.Log
import com.sal.privacykit.data.model.DeterministicStepCount
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.json.JSONObject
import java.lang.reflect.Method
import java.time.LocalDate

private const val HEALTH_TAG = "PrivacyKitHealthHook"
private const val HOST_PACKAGE = "com.sal.privacykit.lite"
private const val HEALTH_CLIENT = "androidx.health.connect.client.HealthConnectClient"
private const val AGGREGATION_RESULT = "androidx.health.connect.client.aggregate.AggregationResult"
private const val STEPS_RECORD = "androidx.health.connect.client.records.StepsRecord"

/**
 * Hooks Health Connect aggregation reads in the scoped app process.
 *
 * The switch is deliberately process-local: aggregate() enables it only for requests that
 * include StepsRecord.COUNT_TOTAL; a subsequent aggregate request without that metric disables it.
 */
class HealthConnectStepHook : XposedModule() {
    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName == HOST_PACKAGE) return

        val classLoader = param.classLoader
        val config = readRootConfig()?.optJSONObject(param.packageName)
        val profileId = config?.optLong("profileId", 0L)?.coerceAtLeast(0L) ?: 0L
        val global = readRootConfig()
            ?.optJSONObject(XposedConfigExporter.KEY_GLOBAL_SETTINGS)

        val minSteps = global?.optLong("stepCountMin", DEFAULT_MIN_STEPS)
            ?.coerceAtLeast(0L) ?: DEFAULT_MIN_STEPS
        val maxSteps = (global?.optLong("stepCountMax", DEFAULT_MAX_STEPS)
            ?: DEFAULT_MAX_STEPS).coerceAtLeast(minSteps)

        hookAggregate(classLoader, profileId, minSteps, maxSteps)
        hookAggregationGet(classLoader, profileId, minSteps, maxSteps)
    }

    private fun hookAggregate(
        classLoader: ClassLoader,
        profileId: Long,
        minSteps: Long,
        maxSteps: Long,
    ) {
        runCatching {
            val clientClass = Class.forName(HEALTH_CLIENT, false, classLoader)
            val aggregate = clientClass.methods.firstOrNull { method ->
                method.name == "aggregate" &&
                    method.parameterTypes.firstOrNull()?.name ==
                    "androidx.health.connect.client.request.AggregateRequest"
            } ?: error("HealthConnectClient.aggregate(AggregateRequest) was not found")

            hook(aggregate)
                .setId("privacykitlite.health.aggregate.steps")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(XposedInterface.Hooker { chain ->
                    val request = chain.getArg(0)
                    val requestedSteps = request != null &&
                        requestContainsStepsMetric(request, classLoader)
                    synchronized(stateLock) {
                        spoofStepsEnabled = requestedSteps
                        activeProfileId = profileId
                        activeMinSteps = minSteps
                        activeMaxSteps = maxSteps
                    }
                    chain.proceed()
                })
            log("hooked HealthConnectClient.aggregate; profile=$profileId")
        }.onFailure { log("failed to hook HealthConnectClient.aggregate", it) }
    }

    private fun hookAggregationGet(
        classLoader: ClassLoader,
        profileId: Long,
        minSteps: Long,
        maxSteps: Long,
    ) {
        runCatching {
            val resultClass = Class.forName(AGGREGATION_RESULT, false, classLoader)
            val metricClass = Class.forName(
                "androidx.health.connect.client.aggregate.AggregateMetric",
                false,
                classLoader,
            )
            val getMethod: Method = resultClass.methods.firstOrNull { method ->
                method.name == "get" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].name == metricClass.name
            } ?: error("AggregationResult.get(AggregateMetric) was not found")

            hook(getMethod)
                .setId("privacykitlite.health.aggregation_result.get")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(XposedInterface.Hooker { chain ->
                    val metric = chain.getArg(0)
                    val enabled: Boolean
                    val currentProfileId: Long
                    val currentMin: Long
                    val currentMax: Long
                    synchronized(stateLock) {
                        enabled = spoofStepsEnabled
                        currentProfileId = activeProfileId
                        currentMin = activeMinSteps
                        currentMax = activeMaxSteps
                    }

                    if (enabled && metric != null && isStepsTotalMetric(metric, classLoader)) {
                        DeterministicStepCount.forProfileDate(
                            profileId = currentProfileId,
                            date = LocalDate.now(),
                            min = currentMin,
                            max = currentMax,
                        )
                    } else {
                        chain.proceed()
                    }
                })
            log("hooked AggregationResult.get")
        }.onFailure { log("failed to hook AggregationResult.get", it) }
    }

    private fun requestContainsStepsMetric(request: Any, classLoader: ClassLoader): Boolean {
        val metrics = runCatching {
            request.javaClass.methods.firstOrNull {
                it.name == "getMetrics" && it.parameterCount == 0
            }?.invoke(request) as? Iterable<*>
        }.getOrNull() ?: return false

        return metrics.any { metric -> metric != null && isStepsTotalMetric(metric, classLoader) }
    }

    private fun isStepsTotalMetric(metric: Any, classLoader: ClassLoader): Boolean {
        return runCatching {
            val stepsClass = Class.forName(STEPS_RECORD, false, classLoader)
            val countTotal = stepsClass.getField("COUNT_TOTAL").get(null)
            metric == countTotal
        }.getOrDefault(false)
    }

    private fun readRootConfig(): JSONObject? {
        val prefs = getRemotePreferences(XposedConfigExporter.PREFS_GROUP)
        val raw = prefs.getString(XposedConfigExporter.KEY_CONFIG, null) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun log(message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.i(HEALTH_TAG, message)
        else Log.w(HEALTH_TAG, message, throwable)
    }

    companion object {
        private const val DEFAULT_MIN_STEPS = 12000L
        private const val DEFAULT_MAX_STEPS = 15000L

        private val stateLock = Any()

        @Volatile
        private var spoofStepsEnabled = false

        @Volatile
        private var activeProfileId = 0L

        @Volatile
        private var activeMinSteps = DEFAULT_MIN_STEPS

        @Volatile
        private var activeMaxSteps = DEFAULT_MAX_STEPS
    }
}
