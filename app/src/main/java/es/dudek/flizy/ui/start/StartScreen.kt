package es.dudek.flizy.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val TileGap = 4.dp
private val SidePadding = 8.dp

/** Rząd siatki: jeden szeroki kafelek albo do dwóch średnich. */
sealed interface TileRow {
    val key: String

    data class Wide(val tile: Tile) : TileRow {
        override val key get() = "w:${tile.app.key}"
    }

    data class Pair(val left: Tile, val right: Tile?) : TileRow {
        override val key get() = "p:${left.app.key}"
    }
}

/**
 * Gęste upakowanie jak w WP: samotny średni kafelek zostawia wolną połówkę rzędu,
 * którą zajmuje następny średni, nawet jeśli po drodze był szeroki.
 */
fun packTiles(tiles: List<Tile>): List<TileRow> {
    val rows = mutableListOf<TileRow>()
    var openRow = -1
    for (tile in tiles) {
        when {
            tile.config.size == TileSize.WIDE -> rows += TileRow.Wide(tile)
            openRow >= 0 -> {
                rows[openRow] = (rows[openRow] as TileRow.Pair).copy(right = tile)
                openRow = -1
            }
            else -> {
                rows += TileRow.Pair(tile, null)
                openRow = rows.lastIndex
            }
        }
    }
    return rows
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
) {
    val rows = remember(tiles) { packTiles(tiles) }
    BoxWithConstraints(modifier.fillMaxSize().background(Metro.Background)) {
        val medium: Dp = (maxWidth - SidePadding * 2 - TileGap) / 2
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // Przytrzymanie pustego miejsca (poza kafelkami) otwiera edycję.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onEditTiles() }) },
            contentPadding = PaddingValues(horizontal = SidePadding),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            item { Spacer(Modifier.statusBarsPadding().height(28.dp)) }

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
                    when (row) {
                        is TileRow.Wide -> TileView(row.tile, icons, Modifier.fillMaxSize(), onLaunch, onTileLongPress)
                        is TileRow.Pair -> {
                            TileView(row.left, icons, Modifier.width(medium).fillMaxHeight(), onLaunch, onTileLongPress)
                            row.right?.let { TileView(it, icons, Modifier.width(medium).fillMaxHeight(), onLaunch, onTileLongPress) }
                        }
                    }
                }
            }

            item { StartFooter(onEditTiles, onSettings, onAllApps) }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun TileView(
    tile: Tile,
    icons: IconLoader,
    modifier: Modifier,
    onLaunch: (Tile) -> Unit,
    onLongPress: (Tile, Float) -> Unit,
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
            style = MetroText.TileLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
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
