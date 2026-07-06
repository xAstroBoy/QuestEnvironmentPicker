package dev.codex.questhomeswitcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import dev.codex.questhomeswitcher.domain.HomeEnvironment
import dev.codex.questhomeswitcher.domain.HomeEnvironmentType
import dev.codex.questhomeswitcher.domain.QuestHomeContract
import dev.codex.questhomeswitcher.shell.ShellRunner
import java.io.File
import java.text.DecimalFormat

class HomeRepository(context: Context) {
    private val packageManager = context.packageManager
    private val previewExtensions = listOf("png", "jpg", "jpeg", "webp")

    fun homesDirectory(): File {
        return File(Environment.getExternalStorageDirectory(), QuestHomeContract.HomesFolderName)
    }

    fun loadHomes(): List<HomeEnvironment> {
        return QuestHomeContract.SearchFolders
            .map { File(Environment.getExternalStorageDirectory(), it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().maxDepth(3).filter { it.isFile }.toList() }
            .filter {
                it.extension.equals("apk", ignoreCase = true) &&
                    (isTrustedHomeFolder(it.absolutePath) || isHomeApk(it.absolutePath))
            }
            .distinctBy { it.absolutePath }
            ?.sortedBy { it.nameWithoutExtension.lowercase() }
            ?.map { apk ->
                HomeEnvironment(
                    displayName = apk.nameWithoutExtension.cleanHomeName(),
                    apkPath = apk.absolutePath,
                    previewPath = findPreviewFor(apk)?.absolutePath,
                    sizeBytes = apk.length(),
                    lastModifiedMillis = apk.lastModified(),
                )
            }
            .orEmpty()
    }

    suspend fun loadHomesWithShell(shellRunner: ShellRunner): List<HomeEnvironment> {
        val dirs = QuestHomeContract.SearchFolders.joinToString(" ") { "/sdcard/${it.shellQuote()}" }
        val command = """
            for dir in $dirs; do
            [ -d "${'$'}dir" ] || continue
            find "${'$'}dir" -maxdepth 3 -type f \( -iname '*.apk' \) 2>/dev/null
            done | while IFS= read -r f; do
              [ -f "${'$'}f" ] || continue
              base="${'$'}{f%.*}"
              preview=""
              for ext in png jpg jpeg webp PNG JPG JPEG WEBP; do
                [ -f "${'$'}base.${'$'}ext" ] && preview="${'$'}base.${'$'}ext" && break
              done
              size=${'$'}(wc -c < "${'$'}f" | tr -d ' ')
              mod=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              home=0
              unzip -l "${'$'}f" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' && home=1
              printf '%s\t%s\t%s\t%s\t%s\n' "${'$'}f" "${'$'}size" "${'$'}mod" "${'$'}preview" "${'$'}home"
            done
        """.trimIndent()

        val result = shellRunner.run(command)
        if (!result.success) return emptyList()

        return result.output
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 3) return@mapNotNull null
                val apkPath = parts[0]
                val fileName = apkPath.substringAfterLast('/')
                HomeEnvironment(
                    displayName = fileName.substringBeforeLast('.').cleanHomeName(),
                    apkPath = apkPath,
                    previewPath = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                    sizeBytes = parts[1].toLongOrNull() ?: 0L,
                    lastModifiedMillis = (parts[2].toLongOrNull() ?: 0L) * 1000L,
                    verifiedHomeApk = parts.getOrNull(4) == "1",
                )
            }
            .filter { it.verifiedHomeApk || isTrustedHomeFolder(it.apkPath) || isHomeApk(it.apkPath) }
            .sortedBy { it.displayName.lowercase() }
            .toList()
    }

    suspend fun loadInstalledHomes(rootRunner: ShellRunner): List<HomeEnvironment> {
        val command = """
            for pkg in ${'$'}(pm list packages | sed 's/^package://'); do
              case "${'$'}pkg" in
                *environment*|*env.vista*|*env.footprint*) ;;
                *) continue ;;
              esac
              path=${'$'}(pm path "${'$'}pkg" 2>/dev/null | sed -n 's/^package://p' | head -n 1)
              [ -n "${'$'}path" ] || continue
              entry=${'$'}(unzip -Z1 "${'$'}path" 2>/dev/null | grep -Ei '(^|/)scene\.zip${'$'}' | head -n 1)
              [ -n "${'$'}entry" ] || entry=assets/scene.zip
              printf '%s\t%s\t%s\n' "${'$'}pkg" "${'$'}path" "${'$'}entry"
            done
        """.trimIndent()
        val result = rootRunner.run(command)
        if (!result.success) return emptyList()
        return result.output.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            val pkg = parts[0]
            HomeEnvironment(
                displayName = pkg.substringAfterLast('.').cleanHomeName(),
                apkPath = parts[1],
                previewPath = null,
                sizeBytes = 0L,
                lastModifiedMillis = 0L,
                packageName = pkg,
                sceneUri = "apk://$pkg/${parts[2]}",
                installed = true,
                type = when {
                    ".env.vista." in pkg -> HomeEnvironmentType.VISTA
                    ".env.footprint." in pkg -> HomeEnvironmentType.FOOTPRINT
                    else -> HomeEnvironmentType.ENVIRONMENT
                },
            )
        }.distinctBy { it.packageName }.sortedBy { it.displayName.lowercase() }.toList()
    }

    suspend fun findActiveHomeWithShell(
        shellRunner: ShellRunner,
        homes: List<HomeEnvironment>,
    ): HomeEnvironment? {
        if (homes.isEmpty()) return null

        val candidates = homes.filter { !it.installed }.joinToString(" ") { it.apkPath.shellQuote() }
        val command = """
            installed=${'$'}(pm path --user 0 ${QuestHomeContract.TargetPackage} 2>/dev/null | sed -n 's/^package://p' | head -n 1)
            [ -n "${'$'}installed" ] && [ -r "${'$'}installed" ] || exit 1
            installed_hash=${'$'}(sha256sum "${'$'}installed" | cut -d ' ' -f 1)
            for f in $candidates; do
              [ -f "${'$'}f" ] || continue
              candidate_hash=${'$'}(sha256sum "${'$'}f" | cut -d ' ' -f 1)
              if [ "${'$'}installed_hash" = "${'$'}candidate_hash" ]; then
                printf '%s\n' "${'$'}f"
                exit 0
              fi
            done
            exit 1
        """.trimIndent()

        val result = shellRunner.run(command)
        if (!result.success) return null
        val activePath = result.output.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null
        return homes.firstOrNull { it.apkPath == activePath }
    }

    suspend fun findActiveInstalledHome(
        rootRunner: ShellRunner,
        homes: List<HomeEnvironment>,
    ): HomeEnvironment? {
        val result = rootRunner.run(
            "oculuspreferences --getc environment_selected environment_vista_selected default_footprint",
        )
        if (!result.success) return null
        val uris = Regex("apk://[^\\\"\\s]+", RegexOption.IGNORE_CASE)
            .findAll(result.output)
            .map { it.value }
            .toSet()
        return homes.firstOrNull { it.type == HomeEnvironmentType.ENVIRONMENT && it.sceneUri in uris }
            ?: homes.firstOrNull { it.sceneUri in uris }
    }

    fun formatSize(bytes: Long): String {
        val mb = bytes / 1024.0 / 1024.0
        return "${DecimalFormat("0.0").format(mb)} MB"
    }

    private fun findPreviewFor(apk: File): File? {
        val base = File(apk.parentFile, apk.nameWithoutExtension)
        return previewExtensions
            .map { File(base.parentFile, "${base.name}.$it") }
            .firstOrNull { it.exists() && it.isFile }
    }

    @Suppress("DEPRECATION")
    private fun isHomeApk(path: String): Boolean {
        val info = packageManager.getPackageArchiveInfo(path, 0)
            ?: return false
        return isHomePackage(info.packageName)
    }

    private fun isTrustedHomeFolder(path: String): Boolean {
        val normalized = path.replace('\\', '/').lowercase()
        return normalized.contains("/quest homes/") ||
            normalized.contains("/questhomes/") ||
            normalized.contains("/homes/")
    }

    @Suppress("DEPRECATION")
    private fun isHomePackage(packageName: String): Boolean {
        val name = packageName.lowercase()
        return name == QuestHomeContract.TargetPackage ||
            ".environment." in name ||
            name.startsWith("com.environment.") ||
            name.startsWith("com.oculus.environment.") ||
            name.startsWith("com.meta.environment.") ||
            ".shell.env." in name ||
            ".env.vista." in name ||
            ".env.footprint." in name
    }

    private fun String.cleanHomeName(): String {
        return replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
    }

    private fun String.shellQuote(): String {
        return "'" + replace("'", "'\\''") + "'"
    }
}
