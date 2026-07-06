package dev.codex.questhomeswitcher.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class RootShellRunner : ShellRunner {
    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        runDirect("id").success
    }

    override suspend fun requestPermissionIfNeeded(requestCode: Int) {
        // Running su triggers the Magisk prompt when approval is still missing.
        isReady()
    }

    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        runDirect(command)
    }

    private fun runDirect(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("su", "-c", command).start()
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exit = process.waitFor()
            ShellResult(exit, listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n"))
        } catch (error: Exception) {
            ShellResult(-1, "Root unavailable: ${error.message}")
        }
    }
}
