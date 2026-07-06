package dev.codex.questhomeswitcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.codex.questhomeswitcher.ui.HomeSwitcherApp
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var shizukuListenerRegistered = false
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        requestShizukuPermissionWhenReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermission()
        setContent {
            HomeSwitcherApp()
        }
        Thread {
            if (!isRootAvailable()) {
                runOnUiThread {
                    Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                    shizukuListenerRegistered = true
                    openShizukuIfServerIsOffline()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        if (shizukuListenerRegistered) {
            window.decorView.postDelayed({ requestShizukuPermissionWhenReady() }, 350L)
        }
    }

    override fun onDestroy() {
        if (shizukuListenerRegistered) {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
        }
        super.onDestroy()
    }

    private fun requestStoragePermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 7)
        }
    }

    private fun requestShizukuPermissionWhenReady() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(42)
        }
    }

    private fun isRootAvailable(): Boolean {
        return try {
            ProcessBuilder("su", "-c", "id").start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun openShizukuIfServerIsOffline() {
        window.decorView.postDelayed(
            {
                if (!Shizuku.pingBinder()) {
                    packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                } else {
                    requestShizukuPermissionWhenReady()
                }
            },
            700L,
        )
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
