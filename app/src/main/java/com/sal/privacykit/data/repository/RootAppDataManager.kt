package com.sal.privacykit.data.repository

import java.io.IOException
import android.util.Log
/**
 * Creates per-profile app-data directories and switches the package data path
 * using Android's root shell. These operations require a rooted device.
 */
class RootAppDataManager {
    fun provisionApp(packageName: String, profileIds: Collection<Long>) {
        requireValidPackage(packageName)
        if (profileIds.isEmpty()) return

        val base = "/data/user/0/$packageName"
        val commands = buildString {
            append("set -eu; ")
            append("base=").append(quote(base)).append("; ")
            append("echo \"[provision] base=\$base\"; ")
            append("echo \"[provision] mountns=$(readlink /proc/self/ns/mnt)\"; ")

            append("if [ ! -d \"\$base\" ]; then ")
            append("echo \"ERROR: app data directory not visible: \$base\" >&2; ")
            append("exit 1; ")
            append("fi; ")

            append("owner=$(stat -c '%u:%g' \"\$base\"); ")
            append("echo \"[provision] owner=\$owner\"; ")
            
            val packageRoot = "/data/adb/privacykit/$packageName"
            append("mkdir -p ").append(quote(packageRoot)).append("; ")
            append("chown 0:0 ").append(quote(packageRoot)).append("; ")
            append("chmod 700 ").append(quote(packageRoot)).append("; ")

            profileIds.sorted().forEach { id ->
                val destination = profileDataPath(packageName, id)
                    append("mkdir -p ").append(quote(destination)).append("; ")
                    append("chown -R \"\$owner\" ").append(quote(destination)).append("; ")
                    append("chmod 700 ").append(quote(destination)).append("; ")
            }
        }
        runAsRoot(commands)
    }


    /**
     * Removes the contents of each registered app's per-profile cache directory.
     * Only /data/adb/privacykit/<package>/<profile>/cache is touched; profile data
     * outside cache is preserved. Disabled apps are included when supplied by the caller.
     *
     * @return number of cache directories processed
     */
    fun clearProfileCaches(packageNames: Collection<String>): Int {
        packageNames.forEach(::requireValidPackage)
        if (packageNames.isEmpty()) return 0

        val commands = buildString {
            append("set -eu; cleared=0; ")
            packageNames.sorted().forEach { packageName ->
                val appRoot = "/data/adb/privacykit/$packageName"
                append("app_root=").append(quote(appRoot)).append("; ")
                append("if [ -d \"\\$app_root\" ] && [ ! -L \"\\$app_root\" ]; then ")
                append("for profile_dir in \"\\$app_root\"/*; do ")
                append("[ -d \"\\$profile_dir\" ] && [ ! -L \"\\$profile_dir\" ] || continue; ")
                append("cache_dir=\"\\$profile_dir/cache\"; ")
                append("if [ -d \"\\$cache_dir\" ] && [ ! -L \"\\$cache_dir\" ]; then ")
                append("find \"\\$cache_dir\" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +; ")
                append("cleared=\\$((cleared + 1)); ")
                append("fi; done; fi; ")
            }
            append("echo CACHE_DIRS_CLEARED=\\$cleared")
        }
        val output = runAsRoot(commands)
        return output.lineSequence()
            .mapNotNull { it.trim().removePrefix("CACHE_DIRS_CLEARED=").toIntOrNull() }
            .lastOrNull()
            ?: error("Could not read cache cleanup result: $output")
    }

    fun switchAppsToProfile(packageNames: Collection<String>, profileId: Long) {
        packageNames.forEach(::requireValidPackage)
        if (packageNames.isEmpty()) return
        
        val commands = buildString {
            append("set -e; ")
            packageNames.sorted().forEach { append("am force-stop ").append(quote(it)).append("; ") }
            packageNames.sorted().forEach { packageName ->
                val target = "/data/user/0/$packageName"
                val source = profileDataPath(packageName, profileId)
                append("umount ").append(quote(target)).append(" 2>/dev/null || true; ")
                append("mount --bind ").append(quote(source)).append(" ").append(quote(target)).append("; ")
                append("restorecon -R ").append(quote(target)).append("; ")
            }

        }
        runAsRoot(commands)
    }
    
    private fun profileDataPath(packageName: String, profileId: Long): String =
        "/data/adb/privacykit/$packageName/${profileId.toString().padStart(2, '0')}"

    private fun requireValidPackage(packageName: String) {
        require(packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
            "Invalid Android package name: $packageName"
        }
    }

    private fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun runAsRoot(script: String): String {
        val process = try {
            ProcessBuilder("su", "-mm", "-c", script).redirectErrorStream(true).start()
        } catch (error: IOException) {
            throw IllegalStateException("Unable to start root shell (su)", error)
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "Root app-data operation failed (exit $exitCode): $output"
        }
    }
}
