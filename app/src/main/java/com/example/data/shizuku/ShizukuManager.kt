package com.example.data.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.model.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShizukuExecResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int
)

class ShizukuManager(private val context: Context) {

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Checking)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _state.value = ShizukuState.Dead
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            checkStatus()
        } else {
            _state.value = ShizukuState.RunningUnauthorized
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Shizuku listeners", e)
        }
        checkStatus()
    }

    fun checkStatus() {
        try {
            if (!Shizuku.pingBinder()) {
                // Check if Shizuku app is installed on the system
                val isInstalled = isShizukuAppInstalled()
                _state.value = if (isInstalled) ShizukuState.Dead else ShizukuState.NotInstalled
                return
            }

            val isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                val version = try {
                    Shizuku.getVersion()
                } catch (_: Exception) {
                    13
                }
                _state.value = ShizukuState.Authorized(version)
            } else {
                _state.value = ShizukuState.RunningUnauthorized
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku status", e)
            _state.value = ShizukuState.NotInstalled
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    fun isAuthorized(): Boolean {
        return _state.value is ShizukuState.Authorized
    }

    private fun isShizukuAppInstalled(): Boolean {
        val pm = context.packageManager
        val shizukuPackages = listOf("moe.shizuku.privileged.api", "dev.rikka.shizuku")
        for (pkg in shizukuPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                // continue
            }
        }
        return false
    }

    /**
     * Executes `cmd package archive --user current <packageName>` via Shizuku privileged shell
     */
    suspend fun archivePackage(packageName: String): ShizukuExecResult = withContext(Dispatchers.IO) {
        val command = arrayOf("cmd", "package", "archive", "--user", "current", packageName)
        executeCommand(command)
    }

    /**
     * Executes native Android 15 unarchiving via Shizuku privileged execution.
     * 1. Grants REQUEST_INSTALL_PACKAGES to Archivist via Shizuku shell.
     * 2. Runs `cmd package install-existing --user current <pkg>` to restore the package if cached.
     * 3. Runs `am start --user current -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p <pkg>`
     *    which invokes Android 15's native OS-level Unarchive/Restore prompt.
     * 4. Attempts `PackageInstaller.requestUnarchive` as an additional system request.
     */
    suspend fun unarchivePackage(packageName: String): ShizukuExecResult = withContext(Dispatchers.IO) {
        // Step 1: Ensure REQUEST_INSTALL_PACKAGES permission is granted via Shizuku shell
        executeCommand(arrayOf("cmd", "appops", "set", context.packageName, "REQUEST_INSTALL_PACKAGES", "allow"))

        // Step 2: Attempt cmd package install-existing
        val installExistingResult = executeCommand(arrayOf("cmd", "package", "install-existing", "--user", "current", packageName))
        Log.i(TAG, "cmd package install-existing for $packageName: ${installExistingResult.output}")

        // Step 3: Trigger Android 15 system unarchiver via privileged launcher start
        val amResult = executeCommand(arrayOf("am", "start", "--user", "current", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "-p", packageName))
        Log.i(TAG, "am start launcher for $packageName: ${amResult.output}")

        // Step 4: PackageInstaller.requestUnarchive
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val statusIntent = Intent("com.example.archivist.UNARCHIVE_STATUS").apply {
                setPackage(context.packageName)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                statusIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )
            packageInstaller.requestUnarchive(packageName, pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller.requestUnarchive failed: ${e.message}", e)
        }

        val success = installExistingResult.success || amResult.success
        ShizukuExecResult(
            success = success,
            output = if (installExistingResult.success) installExistingResult.output else amResult.output,
            exitCode = if (success) 0 else -1
        )
    }

    private fun executeCommand(command: Array<String>): ShizukuExecResult {
        if (!isAuthorized()) {
            return ShizukuExecResult(
                success = false,
                output = "Shizuku service is not authorized or not running. Please start Shizuku and grant permission.",
                exitCode = -1
            )
        }

        return try {
            val newProcessMethod = try {
                Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
            } catch (e: Exception) {
                null
            }

            val process: Process = if (newProcessMethod != null) {
                newProcessMethod.invoke(null, command, null, null) as Process
            } else {
                Runtime.getRuntime().exec(command)
            }

            val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()

            val combined = (stdout + "\n" + stderr).trim()
            val isSuccess = exitCode == 0 ||
                    combined.contains("Success", ignoreCase = true) ||
                    (!combined.contains("Error", ignoreCase = true) && !combined.contains("Exception", ignoreCase = true) && exitCode == 0)

            ShizukuExecResult(
                success = isSuccess,
                output = if (combined.isBlank()) "Command executed successfully." else combined,
                exitCode = exitCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing Shizuku command: ${command.joinToString(" ")}", e)
            ShizukuExecResult(
                success = false,
                output = "Exception executing Shizuku privileged command: ${e.message}",
                exitCode = -2
            )
        }
    }

    fun cleanUp() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Shizuku listeners", e)
        }
    }

    companion object {
        private const val TAG = "ShizukuManager"
    }
}
