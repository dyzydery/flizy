package es.dudek.flizy.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import es.dudek.flizy.R
import es.dudek.flizy.Tile
import es.dudek.flizy.data.AppKey
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.data.TileSize
import es.dudek.flizy.ui.components.MetroButton
import es.dudek.flizy.ui.components.PageHeader
import es.dudek.flizy.ui.components.plainClickable
import es.dudek.flizy.ui.components.rememberMonoIcon
import es.dudek.flizy.ui.components.tileSurface
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.LocalColors
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText

private val RowHeight = 72.dp

@Composable
fun EditTilesScreen(
    tiles: List<Tile>,
    icons: IconLoader,
    onMove: (AppKey, delta: Int) -> Unit,
    onMoveTo: (AppKey, index: Int) -> Unit,
    onToggleSize: (AppKey) -> Unit,
    onUnpin: (AppKey) -> Unit,
    onAdd: () -> Unit,
) {
    // Podczas przeciągania kolejność trzymamy lokalnie, zapis do DataStore dopiero po puszczeniu.
    var order by remember(tiles) { mutableStateOf(tiles) }
    var draggedKey by remember { mutableStateOf<AppKey?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }

    fun dragBy(key: AppKey, dy: Float) {
        dragOffset += dy
        val index = order.indexOfFirst { it.app.key == key }
        val target = when {
            dragOffset > rowHeightPx / 2 && index < order.lastIndex -> index + 1
            dragOffset < -rowHeightPx / 2 && index > 0 -> index - 1
            else -> return
        }
        order = order.toMutableList().apply { add(target, removeAt(index)) }
        dragOffset -= (target - index) * rowHeightPx
    }

    fun endDrag() {
        val key = draggedKey ?: return
        val index = order.indexOfFirst { it.app.key == key }
        draggedKey = null
        dragOffset = 0f
        if (index >= 0) onMoveTo(key, index)
    }

    Column(Modifier.fillMaxSize().background(Metro.Background)) {
        PageHeader(stringResource(R.string.edit_title))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                BasicText(
                    stringResource(if (tiles.isEmpty()) R.string.edit_empty else R.string.edit_hint),
                    style = MetroText.Subtle,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            items(order, key = { it.app.key.toString() }) { tile ->
                val key = tile.app.key
                val dragged = key == draggedKey
                EditRow(
                    tile = tile,
                    index = tiles.indexOfFirst { it.app.key == key }.coerceAtLeast(0),
                    icons = icons,
                    canMoveUp = order.firstOrNull() != tile,
                    canMoveDown = order.lastOrNull() != tile,
                    onMove = { onMove(key, it) },
                    onToggleSize = { onToggleSize(key) },
                    onUnpin = { onUnpin(key) },
                    handleModifier = Modifier.pointerInput(key, tiles) {
                        detectDragGestures(
                            onDragStart = { draggedKey = key; dragOffset = 0f },
                            onDragEnd = ::endDrag,
                            onDragCancel = ::endDrag,
                            onDrag = { change, amount ->
                                change.consume()
                                dragBy(key, amount.y)
                            },
                        )
                    },
                    modifier = if (dragged) {
                        Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset }.background(Metro.Chrome)
                    } else {
                        Modifier.animateItem()
                    },
                )
            }
            item {
                MetroButton(stringResource(R.string.edit_add), Modifier.padding(top = 20.dp), onClick = onAdd)
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
    }
}

@Composable
private fun EditRow(
    tile: Tile,
    index: Int,
    icons: IconLoader,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onToggleSize: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val colors = LocalColors.current
    val tileColor = colors.tileColor(index)
    val mono = rememberMonoIcon(tile.app, icons)
    Row(modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        Box(handleModifier.size(width = 36.dp, height = RowHeight), contentAlignment = Alignment.CenterStart) {
            BasicText("≡", style = MetroText.Item.copy(fontSize = 28.sp, color = Metro.Subtle))
        }
        val look = colors.tileLook(tileColor)
        Box(Modifier.size(52.dp).tileSurface(look), contentAlignment = Alignment.Center) {
            mono?.let {
                Image(it, contentDescription = null, colorFilter = ColorFilter.tint(look.icon), modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(tile.app.label, style = MetroText.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(
                stringResource(
                    when (tile.config.size) {
                        TileSize.QUARTER -> R.string.edit_size_quarter
                        TileSize.THIRD -> R.string.edit_size_third
                        TileSize.HALF -> R.string.edit_size_half
                        TileSize.TWO_THIRDS -> R.string.edit_size_two_thirds
                        TileSize.FULL -> R.string.edit_size_full
                    },
                ),
                style = MetroText.Subtle.copy(color = accent),
                modifier = Modifier.plainClickable(onClick = onToggleSize).padding(vertical = 4.dp, horizontal = 0.dp),
            )
        }
        IconText("↑", stringResource(R.string.edit_move_up), enabled = canMoveUp) { onMove(-1) }
        IconText("↓", stringResource(R.string.edit_move_down), enabled = canMoveDown) { onMove(1) }
        IconText("✕", stringResource(R.string.edit_unpin), enabled = true, onClick = onUnpin)
    }
}

@Composable
private fun IconText(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Metro.Foreground else Metro.Chrome
    Box(
        Modifier
            .size(40.dp)
            .semantics { contentDescription = description }
            .then(if (enabled) Modifier.plainClickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(symbol, style = MetroText.Body.copy(fontSize = 20.sp, color = color))
    }
}
