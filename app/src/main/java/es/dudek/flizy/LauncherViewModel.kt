package es.dudek.flizy

import android.app.Application
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.dudek.flizy.data.AppAlias
import es.dudek.flizy.data.AppEntry
import es.dudek.flizy.data.AppKey
import es.dudek.flizy.data.AppRepository
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.data.LauncherConfig
import es.dudek.flizy.data.PersonalSeed
import es.dudek.flizy.data.TileConfig
import es.dudek.flizy.data.TileSize
import es.dudek.flizy.data.launcherConfigStore
import es.dudek.flizy.data.sortedByLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Tile(val config: TileConfig, val app: AppEntry)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()
    private val store = context.launcherConfigStore
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val repository = AppRepository(context, viewModelScope)

    val icons = IconLoader(context)

    /** null do czasu wczytania z DataStore. */
    val config: StateFlow<LauncherConfig?> = store.data.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Wszystkie aplikacje z nałożonymi aliasami (także ukryte - mogą być przypięte). */
    private val labeledApps: StateFlow<List<AppEntry>?> = combine(repository.apps, store.data) { apps, config ->
        apps?.map { app ->
            config.aliasFor(app.key.packageName, app.key.userSerial)?.let(app::withLabel) ?: app
        }?.sortedByLabel()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Lista A-Z: bez ukrytych. */
    val apps: StateFlow<List<AppEntry>?> = combine(labeledApps, store.data) { apps, config ->
        val hidden = config.hiddenApps.toSet()
        apps?.filterNot { it.key in hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hiddenApps: StateFlow<List<AppEntry>> = combine(labeledApps, store.data) { apps, config ->
        val hidden = config.hiddenApps.toSet()
        apps.orEmpty().filter { it.key in hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tiles: StateFlow<List<Tile>> = combine(store.data, labeledApps) { config, apps ->
        val byKey = apps.orEmpty().associateBy { it.key }
        config.tiles.mapNotNull { t -> byKey[t.key]?.let { Tile(t, it) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedKeys: StateFlow<Set<AppKey>> = store.data
        .map { c -> c.tiles.mapTo(HashSet()) { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        repository.start()
        viewModelScope.launch {
            repository.packageEvents.collect { event ->
                icons.evict(event.packageName)
                if (event.removed) {
                    val serial = repository.userSerial(event.user)
                    update { c -> c.copy(tiles = c.tiles.filterNot { it.packageName == event.packageName && it.userSerial == serial }) }
                }
            }
        }
        viewModelScope.launch { applySeedOnce() }
    }

    override fun onCleared() {
        repository.stop()
    }

    private fun update(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch { store.updateData(transform) }
    }

    // --- Uruchamianie i akcje systemowe ---

    fun launch(app: AppEntry, sourceBounds: Rect? = null) {
        try {
            launcherApps.startMainActivity(app.component, app.user, sourceBounds, null)
        } catch (e: Exception) {
            Log.w(TAG, "Nie udało się uruchomić ${app.component}", e)
            Toast.makeText(context, context.getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
            .onFailure { Log.w(TAG, "Brak ekranu informacji o ${app.component}", it) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.key.packageName, null))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Nie udało się odinstalować ${app.key.packageName}", it) }
    }

    // --- Kafelki ---

    fun pin(app: AppEntry, size: TileSize = TileSize.MEDIUM) = update { c ->
        if (c.tiles.any { it.key == app.key }) c
        else c.copy(tiles = c.tiles + TileConfig(app.key.packageName, app.key.className, app.key.userSerial, size))
    }

    fun unpin(key: AppKey) = update { c -> c.copy(tiles = c.tiles.filterNot { it.key == key }) }

    fun setTileSize(key: AppKey, size: TileSize) = update { c ->
        c.copy(tiles = c.tiles.map { if (it.key == key) it.copy(size = size) else it })
    }

    fun toggleTileSize(key: AppKey) = update { c ->
        c.copy(tiles = c.tiles.map {
            if (it.key == key) it.copy(size = if (it.size == TileSize.MEDIUM) TileSize.WIDE else TileSize.MEDIUM) else it
        })
    }

    /** Przesuwa kafelek o [delta] pozycji (ujemne = w górę). */
    fun moveTile(key: AppKey, delta: Int) = update { c ->
        val from = c.tiles.indexOfFirst { it.key == key }
        val to = from + delta
        if (from < 0 || to !in c.tiles.indices) c
        else c.copy(tiles = c.tiles.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Przeniesienie na konkretną pozycję (przeciąganie na liście edycji). */
    fun moveTileTo(key: AppKey, toIndex: Int) = update { c ->
        val from = c.tiles.indexOfFirst { it.key == key }
        if (from < 0 || toIndex !in c.tiles.indices || from == toIndex) c
        else c.copy(tiles = c.tiles.toMutableList().apply { add(toIndex, removeAt(from)) })
    }

    // --- Ukrywanie i nazwy ---

    fun hide(key: AppKey) = update { c -> if (key in c.hiddenApps) c else c.copy(hiddenApps = c.hiddenApps + key) }

    fun unhide(key: AppKey) = update { c -> c.copy(hiddenApps = c.hiddenApps - key) }

    /** Pusta nazwa przywraca oryginalną. */
    fun rename(app: AppEntry, label: String) = update { c ->
        val others = c.aliases.filterNot { it.packageName == app.key.packageName && it.userSerial == app.key.userSerial }
        val trimmed = label.trim()
        c.copy(
            aliases = if (trimmed.isEmpty() || trimmed == app.originalLabel) others
            else others + AppAlias(app.key.packageName, app.key.userSerial, trimmed),
        )
    }

    // --- Ustawienia ---

    fun setAccent(id: String) = update { it.copy(accent = id) }

    fun setAutoLaunch(enabled: Boolean) = update { it.copy(autoLaunchSingleMatch = enabled) }

    fun setMoreTiles(enabled: Boolean) = update { it.copy(moreTiles = enabled) }

    // --- Pierwsze uruchomienie ---

    /**
     * Raz: kafelki, ukryte aplikacje i aliasy z [PersonalSeed]. Gdy żadnej aplikacji z kafelków
     * nie ma (inny telefon), kafelki z domyślnych aplikacji systemu.
     */
    private suspend fun applySeedOnce() {
        if (store.data.first().seedVersion >= PersonalSeed.VERSION) return
        val apps = repository.apps.filterNotNull().first()
        val serials = HashMap<Int, Long?>()
        fun serial(userId: Int) = serials.getOrPut(userId) { repository.serialForUserId(userId) }

        fun resolve(app: PersonalSeed.App): AppEntry? {
            val userSerial = serial(app.userId) ?: return null
            val candidates = apps.filter { it.key.packageName == app.packageName && it.key.userSerial == userSerial }
            // Nazwa aktywności mogła się zmienić po aktualizacji - wtedy pierwsza z pakietu.
            return candidates.firstOrNull { it.key.className == app.className } ?: candidates.firstOrNull()
        }

        val seededTiles = PersonalSeed.tiles.mapNotNull { seed ->
            resolve(seed.app)?.let { TileConfig(it.key.packageName, it.key.className, it.key.userSerial, seed.size) }
        }.distinctBy { it.key }
        val tiles = seededTiles.ifEmpty { defaultTiles(apps) }
        val hidden = PersonalSeed.hidden.mapNotNull { h ->
            resolve(h)?.key ?: serial(h.userId)?.let { AppKey(h.packageName, h.className ?: return@let null, it) }
        }
        val aliases = PersonalSeed.aliases.mapNotNull { a -> serial(a.userId)?.let { AppAlias(a.packageName, it, a.label) } }

        store.updateData { c ->
            if (c.seedVersion >= PersonalSeed.VERSION) c
            else c.copy(
                tiles = tiles,
                hiddenApps = (c.hiddenApps + hidden).distinct(),
                aliases = c.aliases + aliases.filter { a -> c.aliasFor(a.packageName, a.userSerial) == null },
                seedVersion = PersonalSeed.VERSION,
            )
        }
    }

    private fun defaultTiles(apps: List<AppEntry>): List<TileConfig> {
        val pm = context.packageManager
        val mainSerial = repository.userSerial(Process.myUserHandle())

        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.takeIf { it != "android" } // "android" = okno wyboru, brak domyślnej aplikacji

        val wanted = listOf(
            (context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
                ?: resolve(Intent(Intent.ACTION_DIAL))) to TileSize.MEDIUM,
            Telephony.Sms.getDefaultSmsPackage(context) to TileSize.MEDIUM,
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))) to TileSize.WIDE,
            resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) to TileSize.MEDIUM,
            resolve(Intent(Settings.ACTION_SETTINGS)) to TileSize.MEDIUM,
        )
        return wanted.mapNotNull { (pkg, size) ->
            apps.firstOrNull { it.key.packageName == pkg && it.key.userSerial == mainSerial }
                ?.let { TileConfig(it.key.packageName, it.key.className, it.key.userSerial, size) }
        }.distinctBy { it.key }
    }

    private companion object {
        const val TAG = "Flizy"
    }
}
