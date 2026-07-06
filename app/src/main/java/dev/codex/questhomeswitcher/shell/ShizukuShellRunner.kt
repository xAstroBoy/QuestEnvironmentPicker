package dev.codex.questhomeswitcher.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuShellRunner : ShellRunner {
    override suspend fun isReady(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermissionIfNeeded(requestCode: Int) {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(requestCode)
        }
    }

    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext ShellResult(1, "Shizuku is not ready or permission is missing.")
        }

        val process = createShizukuProcess(command)
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
        val exit = process.waitFor()
        ShellResult(exit, listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n"))
    }

    private fun createShizukuProcess(command: String): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
    }
}
