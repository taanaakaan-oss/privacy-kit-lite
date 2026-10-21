package com.sal.privacykit.data.repository

import java.io.IOException

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
            append("set -e; ")
            append("id; ")
            append("ls -ld /data/user/0; ")
            append("echo PID=\\$\\$; ")
            append("readlink /proc/self/ns/mnt; ")
            append("grep -E ' /data(/| )' /proc/self/mountinfo; ")
            append("grep -F 'jp.co.barows.kenshocrane' /proc/self/mountinfo; ")
            append("ls -ld /data/data/jp.co.barows.kenshocrane 2>&1; ")
            append("ls -ld /data/user/0/jp.co.barows.kenshocrane 2>&1; ")
            append("ls -ld /data/com.sal.privacykit.lite 2>&1; ")
            append("ls -ld /data/data/com.sal.privacykit.lite 2>&1; ")
            append("echo === namespace ===; ")
            append("readlink /proc/self/ns/mnt; ")
            append("readlink /proc/1/ns/mnt; ")
            append("echo === /data/data target ===; ")
            append("readlink -f /data/data; ")
            append("echo === mountinfo ===; ")
            append("grep -E ' /data(/data|/user/0)( |$)' /proc/self/mountinfo; ")
            append("echo === target app dirs ===; ")
            append("ls -ld /data/data/jp.co.barows.kenshocrane /data/user/0/jp.co.barows.kenshocrane; ")
            append("ls -ld ").append(quote(base)).append(" 2>&1; ")
            append("stat -c '%u:%g' ").append(quote(base)).append(" 2>&1; ")
            append("owner=\$(stat -c '%u:%g' ").append(quote(base)).append("); ")
            profileIds.sorted().forEach { id ->
                val destination = profileDataPath(packageName, id)
                append("mkdir -p ").append(quote(destination)).append("; ")
                append("chown -R \"\$owner\" ").append(quote(destination)).append("; ")
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
        "/data/user/0/${packageName}_${profileId.toString().padStart(2, '0')}"

    private fun requireValidPackage(packageName: String) {
        require(packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
            "Invalid Android package name: $packageName"
        }
    }

    private fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun runAsRoot(script: String) {
        val process = try {
            ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
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
