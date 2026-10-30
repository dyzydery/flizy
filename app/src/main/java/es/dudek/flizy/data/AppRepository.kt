package es.dudek.flizy.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import es.dudek.flizy.util.normalizeForSearch
import es.dudek.flizy.util.searchWords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** Identyfikator aplikacji niezależny od procesu: komponent + numer seryjny profilu użytkownika. */
data class AppKey(val packageName: String, val className: String, val userSerial: Long)

class AppEntry(
    val key: AppKey,
    val label: String,
    val user: UserHandle,
    val info: LauncherActivityInfo,
) {
    val component: ComponentName get() = info.componentName
    val searchName: String = label.normalizeForSearch()
    val searchWords: List<String> = searchName.searchWords()

    /** Litera sekcji listy A-Z; wszystko poza a-z trafia do "#". */
    val section: Char = searchName.firstOrNull()?.takeIf { it in 'a'..'z' } ?: '#'
}

data class PackageEvent(val packageName: String, val user: UserHandle, val removed: Boolean)

/** Lista aplikacji z LauncherApps, odświeżana przez LauncherApps.Callback. */
class AppRepository(context: Context, private val scope: CoroutineScope) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val collator = Collator.getInstance(Locale.forLanguageTag("pl")).apply {
        strength = Collator.SECONDARY
    }

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)

    /** null do czasu pierwszego wczytania. */
    val apps: StateFlow<List<AppEntry>?> = _apps

    private val _packageEvents = MutableSharedFlow<PackageEvent>(extraBufferCapacity = 64)

    /** Zmiany pojedynczych pakietów (do unieważniania ikon i odpinania usuniętych aplikacji). */
    val packageEvents: SharedFlow<PackageEvent> = _packageEvents

    private var reloadJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(user, true, packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(user, false, packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(user, false, packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(user, false, *packageNames)

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(user, false, *packageNames)

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) =
            changed(user, false, *packageNames)

        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) =
            changed(user, false, *packageNames)

        private fun changed(user: UserHandle, removed: Boolean, vararg packageNames: String) {
            packageNames.forEach { _packageEvents.tryEmit(PackageEvent(it, user, removed)) }
            requestReload()
        }
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        requestReload(debounce = false)
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
    }

    fun userSerial(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    fun userForSerial(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    /** Instalacja często generuje kilka zdarzeń pod rząd - zbieramy je w jedno przeładowanie. */
    private fun requestReload(debounce: Boolean = true) {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            if (debounce) delay(300)
            _apps.value = withContext(Dispatchers.Default) { loadApps() }
        }
    }

    private fun loadApps(): List<AppEntry> =
        launcherApps.profiles
            .flatMap { user ->
                val serial = userSerial(user)
                launcherApps.getActivityList(null, user).map { info ->
                    AppEntry(
                        key = AppKey(info.componentName.packageName, info.componentName.className, serial),
                        label = info.label.toString().trim(),
                        user = user,
                        info = info,
                    )
                }
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
}
