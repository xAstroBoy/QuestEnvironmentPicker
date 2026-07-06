package dev.codex.questhomeswitcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.codex.questhomeswitcher.data.HomeRepository
import dev.codex.questhomeswitcher.domain.ActivateHomeUseCase
import dev.codex.questhomeswitcher.domain.HomeEnvironment
import dev.codex.questhomeswitcher.domain.QuestHomeContract
import dev.codex.questhomeswitcher.shell.ShizukuShellRunner
import dev.codex.questhomeswitcher.shell.RootShellRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeSwitcherUiState(
    val homes: List<HomeEnvironment> = emptyList(),
    val selected: HomeEnvironment? = null,
    val activeHome: HomeEnvironment? = null,
    val isBusy: Boolean = false,
    val shizukuReady: Boolean = false,
    val rootReady: Boolean = false,
    val showRestartAction: Boolean = false,
    val message: String = "",
    val log: String = "",
)

class HomeSwitcherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val shellRunner = ShizukuShellRunner()
    private val rootRunner = RootShellRunner()
    private val activateHome = ActivateHomeUseCase(shellRunner, rootRunner)

    private val _uiState = MutableStateFlow(HomeSwitcherUiState())
    val uiState: StateFlow<HomeSwitcherUiState> = _uiState

    init {
        preparePrivilegesOnLaunch()
    }

    private fun preparePrivilegesOnLaunch() {
        viewModelScope.launch {
            if (!rootRunner.isReady()) {
                shellRunner.requestPermissionIfNeeded(42)
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val ready = shellRunner.isReady()
            val rootReady = rootRunner.isReady()
            val fileHomes = if (ready) repository.loadHomesWithShell(shellRunner) else repository.loadHomes()
            val homes = if (rootReady) repository.loadInstalledHomes(rootRunner) + fileHomes else fileHomes
            val activeHome = when {
                rootReady -> repository.findActiveInstalledHome(rootRunner, homes)
                ready -> repository.findActiveHomeWithShell(shellRunner, homes)
                else -> null
            }
            _uiState.update {
                it.copy(
                    homes = homes,
                    selected = it.selected?.takeIf { selected -> homes.any { home -> home.apkPath == selected.apkPath } }
                        ?: activeHome
                        ?: homes.firstOrNull(),
                    activeHome = activeHome,
                    shizukuReady = ready,
                    rootReady = rootReady,
                    message = if (homes.isEmpty()) {
                        "Download a home APK or install an environment package"
                    } else if (activeHome != null) {
                        "Active: ${activeHome.displayName}"
                    } else {
                        "${homes.size} home(s) found · ${if (rootReady) "Root direct mode" else "Shizuku fallback"}"
                    },
                )
            }
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            shellRunner.requestPermissionIfNeeded(42)
            _uiState.update {
                it.copy(message = "Approve Shizuku permission, then press refresh.")
            }
        }
    }

    fun select(home: HomeEnvironment) {
        _uiState.update { it.copy(selected = home) }
    }

    fun activateSelected() {
        val home = _uiState.value.selected ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    showRestartAction = false,
                    message = "Installing ${home.displayName}...",
                    log = "",
                )
            }
            val result = activateHome(home)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeHome = if (result.success) home else it.activeHome,
                    showRestartAction = result.success && result.needsReboot,
                    message = when {
                        !result.success -> "Install failed. Open the log."
                        else -> "Active: ${home.displayName}"
                    },
                    log = if (result.success) "" else result.log,
                )
            }
        }
    }

    fun restartQuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Requesting Quest restart...") }
            val result = shellRunner.run("reboot")
            _uiState.update {
                it.copy(
                    message = if (result.success) "Restart requested." else "Restart failed. Use the Quest power menu.",
                    log = if (result.output.isBlank()) it.log else it.log + "\n== Restart ==\n" + result.output,
                )
            }
        }
    }

    fun formatSize(bytes: Long): String = repository.formatSize(bytes)
}
