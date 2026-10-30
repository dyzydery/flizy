package es.dudek.flizy.ui.start

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
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
import es.dudek.flizy.ui.components.tileSurface
import es.dudek.flizy.ui.theme.LocalColors
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TileGap = 4.dp
private val SidePadding = 8.dp

private const val GRID_COLUMNS = 12

/** Rząd siatki; wysokość rzędu = szerokość kafelka 1/3. */
data class TileRow(val tiles: List<Tile>) {
    val key: String get() = tiles.joinToString("|") { it.app.key.toString() }
}

/**
 * Gęste upakowanie jak w WP: kafelek trafia do pierwszego rzędu, w którym jest dla niego
 * miejsce, więc luki zostawione przez większe kafelki wypełniają kolejne mniejsze.
 */
fun packTiles(tiles: List<Tile>): List<TileRow> {
    val rows = mutableListOf<MutableList<Tile>>()
    val used = mutableListOf<Int>()
    for (tile in tiles) {
        val width = tile.config.size.columns
        val index = used.indexOfFirst { it + width <= GRID_COLUMNS }
        if (index >= 0) {
            rows[index] += tile
            used[index] += width
        } else {
            rows += mutableListOf(tile)
            used += width
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
    onEmptyLongPress: (anchorPx: Float) -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(tiles) { packTiles(tiles) }
    val positions = remember(tiles) { tiles.withIndex().associate { (i, t) -> t.app.key to i } }
    val colors = LocalColors.current
    BoxWithConstraints(modifier.fillMaxSize().background(Metro.Background)) {
        // Siatka 12 kolumn z odstępem między każdą: kafelek k kolumn = k*kolumna + (k-1)*odstęp,
        // dzięki czemu kafelki w różnych rzędach wyrównują się co do piksela.
        val column: Dp = (maxWidth - SidePadding * 2 - TileGap * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        fun widthOf(columns: Int): Dp = column * columns + TileGap * (columns - 1)
        // Rzędy rozciągają się, żeby razem z zegarem wypełnić ekran do paska nawigacji
        // (najwyżej 1,5× normalnej wysokości); gdy się nie mieszczą - zwykłe przewijanie.
        val density = LocalDensity.current
        var headerHeight by remember { mutableStateOf(0.dp) }
        val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val baseRow = widthOf(TileSize.THIRD.columns)
        val fill = if (rows.isEmpty()) baseRow else (maxHeight - headerHeight - navBar - TileGap * (rows.size + 1)) / rows.size
        val rowHeight = fill.coerceIn(baseRow, baseRow * 1.5f)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // Przytrzymanie pustego miejsca (poza kafelkami): menu z edycją i ustawieniami.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onEmptyLongPress(it.y) }) },
            contentPadding = PaddingValues(horizontal = SidePadding),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            item {
                ClockHeader(
                    onClockClick,
                    onDateClick,
                    Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
                )
            }

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
                Row(Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.tiles.forEach { tile ->
                        TileView(
                            tile = tile,
                            icons = icons,
                            color = colors.tileColor(positions[tile.app.key] ?: 0),
                            modifier = Modifier.width(widthOf(tile.config.size.columns)).fillMaxHeight(),
                            onLaunch = onLaunch,
                            onLongPress = onTileLongPress,
                        )
                    }
                }
            }

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
private fun ClockHeader(onClockClick: () -> Unit, onDateClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now = rememberNow()
    val locale = Locale.forLanguageTag("pl")
    val time = remember(now) {
        val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        SimpleDateFormat(pattern, locale).format(Date(now))
    }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", locale).format(Date(now)) }

    Column(
        modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            time,
            style = MetroText.Clock,
            modifier = Modifier.plainClickable(onClick = onClockClick),
        )
        BasicText(
            date,
            style = MetroText.Date,
            modifier = Modifier.plainClickable(onClick = onDateClick).padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun TileView(
    tile: Tile,
    icons: IconLoader,
    color: Color,
    modifier: Modifier,
    onLaunch: (Tile) -> Unit,
    onLongPress: (Tile, Float) -> Unit,
) {
    val narrow = tile.config.size == TileSize.QUARTER
    val mono = rememberMonoIcon(tile.app, icons)
    var bottom by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .onGloballyPositioned { bottom = it.boundsInRoot().bottom }
            .metroPressable(onLongClick = { onLongPress(tile, bottom) }, onClick = { onLaunch(tile) })
            .tileSurface(LocalColors.current.tileLook(color)),
    ) {
        val look = LocalColors.current.tileLook(color)
        mono?.let {
            Image(
                it,
                contentDescription = null,
                colorFilter = ColorFilter.tint(look.icon),
                modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.5f).aspectRatio(1f),
            )
        }
        BasicText(
            tile.app.label,
            style = MetroText.TileLabel.copy(color = look.label, fontSize = if (narrow) 11.sp else 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
        )
    }
}
