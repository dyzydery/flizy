package es.dudek.flizy.ui.start

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import es.dudek.flizy.R
import es.dudek.flizy.Tile
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.data.TileSize
import es.dudek.flizy.ui.components.metroPressable
import es.dudek.flizy.ui.components.plainClickable
import es.dudek.flizy.ui.components.rememberMonoIcon
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TileGap = 4.dp
private val SidePadding = 8.dp

/** Rząd siatki o wysokości średniego kafelka; średni zajmuje 1 miejsce, szeroki 2. */
data class TileRow(val tiles: List<Tile>) {
    val key: String get() = tiles.joinToString("|") { it.app.key.toString() }
}

private val Tile.slots: Int get() = if (config.size == TileSize.WIDE) 2 else 1

/**
 * Gęste upakowanie jak w WP: kafelek trafia do pierwszego rzędu, w którym jest dla niego
 * miejsce, więc luki po samotnych średnich kafelkach wypełniają kolejne.
 * [slotsPerRow] = 2 (siatka 4 kolumn) albo 3 (6 kolumn, "więcej kafelków").
 */
fun packTiles(tiles: List<Tile>, slotsPerRow: Int): List<TileRow> {
    val rows = mutableListOf<MutableList<Tile>>()
    val used = mutableListOf<Int>()
    for (tile in tiles) {
        val index = used.indexOfFirst { it + tile.slots <= slotsPerRow }
        if (index >= 0) {
            rows[index] += tile
            used[index] += tile.slots
        } else {
            rows += mutableListOf(tile)
            used += tile.slots
        }
    }
    return rows.map { TileRow(it) }
}

@Composable
fun StartScreen(
    tiles: List<Tile>,
    icons: IconLoader,
    listState: LazyListState,
    onLaunch: (Tile) -> Unit,
    onTileLongPress: (Tile, anchorBottomPx: Float) -> Unit,
    onEditTiles: () -> Unit,
    onSettings: () -> Unit,
    onAllApps: () -> Unit,
    modifier: Modifier = Modifier,
    moreTiles: Boolean = true,
    onClockClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
) {
    val slotsPerRow = if (moreTiles) 3 else 2
    val rows = remember(tiles, slotsPerRow) { packTiles(tiles, slotsPerRow) }
    BoxWithConstraints(modifier.fillMaxSize().background(Metro.Background)) {
        val medium: Dp = (maxWidth - SidePadding * 2 - TileGap * (slotsPerRow - 1)) / slotsPerRow
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // Przytrzymanie pustego miejsca (poza kafelkami) otwiera edycję.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onEditTiles() }) },
            contentPadding = PaddingValues(horizontal = SidePadding),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            item { ClockHeader(onClockClick, onDateClick) }

            if (tiles.isEmpty()) {
                item {
                    BasicText(
                        stringResource(R.string.start_empty),
                        style = MetroText.Subtle,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                    )
                }
            }

            items(rows, key = { it.key }) { row ->
                Row(Modifier.fillMaxWidth().height(medium), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.tiles.forEach { tile ->
                        val width = medium * tile.slots + TileGap * (tile.slots - 1)
                        TileView(tile, icons, Modifier.width(width).fillMaxHeight(), onLaunch, onTileLongPress, compact = moreTiles)
                    }
                }
            }

            item { StartFooter(onEditTiles, onSettings, onAllApps) }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

/** Aktualny czas, odświeżany na początku każdej minuty. */
@Composable
private fun rememberNow(): Long {
    // delay() nie liczy czasu uśpienia telefonu, więc po każdym powrocie na ekran liczymy od nowa.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(System.currentTimeMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(60_000 - value % 60_000)
            }
        }
    }
    return now
}

/** Duży zegar (stuknięcie: alarmy) i data (stuknięcie: kalendarz). */
@Composable
private fun ClockHeader(onClockClick: () -> Unit, onDateClick: () -> Unit) {
    val context = LocalContext.current
    val now = rememberNow()
    val locale = Locale.forLanguageTag("pl")
    val time = remember(now) {
        val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        SimpleDateFormat(pattern, locale).format(Date(now))
    }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", locale).format(Date(now)) }

    Column(Modifier.statusBarsPadding().padding(start = 4.dp, top = 16.dp, bottom = 16.dp)) {
        BasicText(
            time,
            style = MetroText.Clock,
            modifier = Modifier.plainClickable(onClick = onClockClick),
        )
        BasicText(
            date,
            style = MetroText.Date,
            modifier = Modifier.plainClickable(onClick = onDateClick).padding(start = 4.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun TileView(
    tile: Tile,
    icons: IconLoader,
    modifier: Modifier,
    onLaunch: (Tile) -> Unit,
    onLongPress: (Tile, Float) -> Unit,
    compact: Boolean,
) {
    val mono = rememberMonoIcon(tile.app, icons)
    var bottom by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .onGloballyPositioned { bottom = it.boundsInRoot().bottom }
            .metroPressable(onLongClick = { onLongPress(tile, bottom) }, onClick = { onLaunch(tile) })
            .background(LocalAccent.current),
    ) {
        mono?.let {
            Image(
                it,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.4f).aspectRatio(1f),
            )
        }
        BasicText(
            tile.app.label,
            style = if (compact) MetroText.TileLabel.copy(fontSize = 12.sp) else MetroText.TileLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = if (compact) 4.dp else 6.dp),
        )
    }
}

@Composable
private fun StartFooter(onEditTiles: () -> Unit, onSettings: () -> Unit, onAllApps: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FooterLink(stringResource(R.string.start_edit), onEditTiles)
        Spacer(Modifier.width(20.dp))
        FooterLink(stringResource(R.string.start_settings), onSettings)
        Spacer(Modifier.weight(1f))
        // Okrągły przycisk ze strzałką do listy aplikacji, jak w WP 8.
        val allApps = stringResource(R.string.start_all_apps)
        Box(
            Modifier
                .size(44.dp)
                .semantics { contentDescription = allApps }
                .border(2.dp, Metro.Foreground, CircleShape)
                .plainClickable(onClick = onAllApps),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("→", style = MetroText.Body.copy(fontSize = 22.sp))
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    BasicText(text, style = MetroText.Subtle, modifier = Modifier.plainClickable(onClick = onClick).padding(vertical = 10.dp))
}
