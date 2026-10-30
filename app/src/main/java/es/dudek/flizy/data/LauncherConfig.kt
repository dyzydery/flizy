package es.dudek.flizy.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
enum class TileSize { MEDIUM, WIDE }

/**
 * Przypięty kafelek. Nowe pola (np. żywe kafelki, duży rozmiar) dodawać z wartościami
 * domyślnymi - stare pliki konfiguracji nadal się wczytają.
 */
@Serializable
data class TileConfig(
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val size: TileSize = TileSize.MEDIUM,
) {
    val key: AppKey get() = AppKey(packageName, className, userSerial)
}

/** Własna nazwa aplikacji, wspólna dla listy A-Z i kafelka. */
@Serializable
data class AppAlias(
    val packageName: String,
    val userSerial: Long,
    val label: String,
)

@Serializable
data class LauncherConfig(
    val tiles: List<TileConfig> = emptyList(),
    val accent: String = DEFAULT_ACCENT,
    val autoLaunchSingleMatch: Boolean = true,
    /** Siatka 6 kolumn (3 średnie kafelki w rzędzie) zamiast 4, jak "więcej kafelków" w WP 8.1. */
    val moreTiles: Boolean = true,
    val hiddenApps: List<AppKey> = emptyList(),
    val aliases: List<AppAlias> = emptyList(),
    /** Wersja zastosowanej konfiguracji startowej ([PersonalSeed]); 0 = jeszcze nie zastosowana. */
    val seedVersion: Int = 0,
) {
    fun aliasFor(packageName: String, userSerial: Long): String? =
        aliases.firstOrNull { it.packageName == packageName && it.userSerial == userSerial }?.label

    companion object {
        const val DEFAULT_ACCENT = "cobalt"
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object LauncherConfigSerializer : Serializer<LauncherConfig> {
    override val defaultValue = LauncherConfig()

    override suspend fun readFrom(input: InputStream): LauncherConfig =
        try {
            json.decodeFromString(LauncherConfig.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Nie można odczytać konfiguracji", e)
        }

    override suspend fun writeTo(t: LauncherConfig, output: OutputStream) {
        output.write(json.encodeToString(LauncherConfig.serializer(), t).encodeToByteArray())
    }
}

val Context.launcherConfigStore by dataStore("launcher.json", LauncherConfigSerializer)
