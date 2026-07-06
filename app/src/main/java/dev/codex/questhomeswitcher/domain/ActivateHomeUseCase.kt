package dev.codex.questhomeswitcher.domain

import dev.codex.questhomeswitcher.shell.ShellRunner

class ActivateHomeUseCase(
    private val shellRunner: ShellRunner,
    private val rootRunner: ShellRunner,
) {
    suspend operator fun invoke(home: HomeEnvironment): ActivationResult {
        if (home.installed && home.sceneUri != null && rootRunner.isReady()) {
            return activateWithRoot(home)
        }
        val packageRunner = when {
            rootRunner.isReady() -> rootRunner
            shellRunner.isReady() -> shellRunner
            else -> return ActivationResult(false, false, "Neither root nor Shizuku is ready.")
        }
        val log = StringBuilder()

        fun appendStep(title: String, output: String) {
            log.appendLine("== $title ==")
            if (output.isBlank()) {
                log.appendLine("(no output)")
            } else {
                log.appendLine(output.trim())
            }
            log.appendLine()
        }

        val escapedApkPath = home.apkPath.shellQuote()
        val uninstall = packageRunner.run("pm uninstall --user 0 ${QuestHomeContract.TargetPackage}")
        appendStep("Uninstall old ${QuestHomeContract.TargetPackage}", uninstall.output)

        val install = packageRunner.run("cat $escapedApkPath | pm install -S ${home.sizeBytes} -r -d -g --user 0")
        appendStep("Install ${home.displayName}", install.output)
        if (!install.success) {
            return ActivationResult(
                success = false,
                needsReboot = false,
                log = log.appendLine("Install failed.").toString(),
            )
        }

        val reload = tryReloadHorizon(packageRunner)
        appendStep("Try Horizon OS reload", reload.output)

        return ActivationResult(
            success = true,
            needsReboot = true,
            log = log.toString(),
        )
    }

    private suspend fun activateWithRoot(home: HomeEnvironment): ActivationResult {
        val uri = requireNotNull(home.sceneUri).shellQuote()
        val preferenceCommands = when (home.type) {
            HomeEnvironmentType.ENVIRONMENT -> """
                oculuspreferences --setc environment_selected $uri || exit 10
                oculuspreferences --setc environment_default $uri || exit 11
                oculuspreferences --setc resolved_environment $uri || exit 12
            """.trimIndent()
            HomeEnvironmentType.VISTA -> """
                oculuspreferences --setc default_vista $uri || exit 20
                oculuspreferences --setc resolved_vista $uri || exit 21
                oculuspreferences --setc environment_vista_selected $uri || exit 22
            """.trimIndent()
            HomeEnvironmentType.FOOTPRINT ->
                "oculuspreferences --setc default_footprint $uri || exit 30"
        }
        val command = """
            command -v oculuspreferences >/dev/null 2>&1 || exit 127
            $preferenceCommands
            am force-stop com.oculus.vrshell
        """.trimIndent()
        val result = rootRunner.run(command)
        return ActivationResult(
            success = result.success,
            needsReboot = false,
            log = if (result.success) "Root mode: preferences updated and VR Shell reloaded." else result.output,
        )
    }

    private suspend fun tryReloadHorizon(runner: ShellRunner): dev.codex.questhomeswitcher.shell.ShellResult {
        val command = """
            am force-stop com.oculus.vrshell >/dev/null 2>&1
            am force-stop com.oculus.shellenv >/dev/null 2>&1
            cmd activity broadcast -a android.intent.action.PACKAGE_CHANGED -d package:${QuestHomeContract.TargetPackage} >/dev/null 2>&1
        """.trimIndent()

        return runner.run(command)
    }

    private fun String.shellQuote(): String {
        return "'" + replace("'", "'\\''") + "'"
    }
}
