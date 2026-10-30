package es.dudek.flizy

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import es.dudek.flizy.ui.LauncherApp
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val homeEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private var resumed by mutableStateOf(false)
    private var isDefaultLauncher by mutableStateOf(false)

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshDefaultLauncher()
        // Niektóre nakładki (np. MIUI) ignorują prośbę o rolę - wtedy ręcznie w ustawieniach.
        if (!isDefaultLauncher) openHomeSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            LauncherApp(
                vm = viewModel(),
                homeEvents = homeEvents,
                resumed = resumed,
                isDefaultLauncher = isDefaultLauncher,
                onSetDefaultLauncher = ::requestDefaultLauncher,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // Ta sama heurystyka co w Launcher3: Home naciśnięty, gdy launcher już był na wierzchu.
            val alreadyOnHome = hasWindowFocus() &&
                (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            homeEvents.tryEmit(alreadyOnHome)
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        refreshDefaultLauncher()
    }

    override fun onPause() {
        super.onPause()
        resumed = false
    }

    private fun refreshDefaultLauncher() {
        isDefaultLauncher = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun requestDefaultLauncher() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            openHomeSettings()
        }
    }

    private fun openHomeSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }
}
