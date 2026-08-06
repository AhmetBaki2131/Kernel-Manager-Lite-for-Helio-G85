package com.example.root

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CommandResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) {
    val isSuccess: Boolean get() = exitCode == 0
    val outputText: String get() = (stdout + stderr).joinToString("\n").ifBlank { "No output" }
}

object RootShell {
    private const val TAG = "RootShell"

    init {
        // Configure Shell settings if needed
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check root status", e)
            false
        }
    }

    suspend fun runCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd(command).exec()
            CommandResult(
                exitCode = result.code,
                stdout = result.out ?: emptyList(),
                stderr = result.err ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing shell command: $command", e)
            // Fallback shell execution if libsu shell throws an exception
            runFallbackCommand(command)
        }
    }

    suspend fun runCommands(commands: List<String>): CommandResult = withContext(Dispatchers.IO) {
        if (commands.isEmpty()) return@withContext CommandResult(0, emptyList(), emptyList())
        try {
            val result = Shell.cmd(*commands.toTypedArray()).exec()
            CommandResult(
                exitCode = result.code,
                stdout = result.out ?: emptyList(),
                stderr = result.err ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing multiple commands", e)
            runFallbackCommand(commands.joinToString("; "))
        }
    }

    private fun runFallbackCommand(command: String): CommandResult {
        return try {
            val process = ProcessBuilder("su", "-c", command).start()
            val stdout = process.inputStream.bufferedReader().readLines()
            val stderr = process.errorStream.bufferedReader().readLines()
            val exitCode = process.waitFor()
            CommandResult(exitCode, stdout, stderr)
        } catch (e1: Exception) {
            try {
                val process = ProcessBuilder("sh", "-c", command).start()
                val stdout = process.inputStream.bufferedReader().readLines()
                val stderr = process.errorStream.bufferedReader().readLines()
                val exitCode = process.waitFor()
                CommandResult(exitCode, stdout, stderr)
            } catch (e2: Exception) {
                CommandResult(-1, emptyList(), listOf("Failed to execute command: ${e2.localizedMessage}"))
            }
        }
    }

    suspend fun readSysfsNode(path: String): String = withContext(Dispatchers.IO) {
        val res = runCommand("cat $path")
        if (res.isSuccess) {
            res.stdout.joinToString("\n").trim()
        } else {
            ""
        }
    }

    suspend fun writeSysfsNode(path: String, value: String): CommandResult = withContext(Dispatchers.IO) {
        val cmd = "chmod 644 $path 2>/dev/null; echo \"$value\" > $path"
        runCommand(cmd)
    }
}
