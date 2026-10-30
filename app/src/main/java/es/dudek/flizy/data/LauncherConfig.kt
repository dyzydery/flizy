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

@Serializable
data class LauncherConfig(
    val tiles: List<TileConfig> = emptyList(),
    val accent: String = DEFAULT_ACCENT,
    val autoLaunchSingleMatch: Boolean = true,
    val defaultTilesApplied: Boolean = false,
) {
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
