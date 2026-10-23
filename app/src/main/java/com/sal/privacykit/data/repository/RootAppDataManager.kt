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

    private fun runAsRoot(script: String) {
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
