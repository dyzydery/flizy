package es.dudek.flizy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.dudek.flizy.LauncherViewModel
import es.dudek.flizy.R
import es.dudek.flizy.Tile
import es.dudek.flizy.data.AppEntry
import es.dudek.flizy.data.TileSize
import es.dudek.flizy.ui.apps.AppListScreen
import es.dudek.flizy.ui.apps.HiddenAppsScreen
import es.dudek.flizy.ui.components.MenuItem
import es.dudek.flizy.ui.components.MetroContextMenu
import es.dudek.flizy.ui.components.RenameDialog
import es.dudek.flizy.ui.edit.EditTilesScreen
import es.dudek.flizy.ui.settings.SettingsScreen
import es.dudek.flizy.ui.start.StartScreen
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.accentById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private class MenuRequest(val anchorBottomPx: Float, val items: List<MenuItem>)

/** Ekrany otwierane nad pagerem Start/Aplikacje. */
private enum class Screen { Settings, EditTiles, AddTile, HiddenApps }

private const val PAGE_START = 0
private const val PAGE_APPS = 1

/**
 * @param homeEvents naciśnięcia Home; true = launcher był już na wierzchu.
 * @param resumed aktywność jest wznowiona (do fokusu wyszukiwarki).
 */
@Composable
fun LauncherApp(
    vm: LauncherViewModel,
    homeEvents: Flow<Boolean>,
    resumed: Boolean,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val pinned by vm.pinnedKeys.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val hiddenApps by vm.hiddenApps.collectAsStateWithLifecycle()

    val cfg = config ?: run {
        Box(Modifier.fillMaxSize().background(Metro.Background))
        return
    }

    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val pager = rememberPagerState { 2 }
    val startList = rememberLazyListState()
    var menu by remember { mutableStateOf<MenuRequest?>(null) }
    var screens by remember { mutableStateOf(emptyList<Screen>()) }
    var renaming by remember { mutableStateOf<AppEntry?>(null) }

    val pinLabel = stringResource(R.string.menu_pin)
    val unpinLabel = stringResource(R.string.menu_unpin)
    val infoLabel = stringResource(R.string.menu_app_info)
    val uninstallLabel = stringResource(R.string.menu_uninstall)
    val wideLabel = stringResource(R.string.menu_resize_wide)
    val mediumLabel = stringResource(R.string.menu_resize_medium)
    val editLabel = stringResource(R.string.menu_edit_tiles)
    val renameLabel = stringResource(R.string.menu_rename)
    val hideLabel = stringResource(R.string.menu_hide)

    fun push(screen: Screen) {
        keyboard?.hide()
        if (screens.lastOrNull() != screen) screens = screens + screen
    }

    fun pop() {
        screens = screens.dropLast(1)
    }

    fun showAppMenu(app: AppEntry, anchor: Float) {
        keyboard?.hide()
        val isPinned = app.key in pinned
        menu = MenuRequest(
            anchor,
            listOf(
                if (isPinned) MenuItem(unpinLabel) { vm.unpin(app.key) } else MenuItem(pinLabel) { vm.pin(app) },
                MenuItem(renameLabel) { renaming = app },
                MenuItem(hideLabel) { vm.hide(app.key) },
                MenuItem(infoLabel) { vm.openAppInfo(app) },
                MenuItem(uninstallLabel) { vm.uninstall(app) },
            ),
        )
    }

    fun showTileMenu(tile: Tile, anchor: Float) {
        val wide = tile.config.size == TileSize.WIDE
        menu = MenuRequest(
            anchor,
            listOf(
                MenuItem(if (wide) mediumLabel else wideLabel) { vm.toggleTileSize(tile.app.key) },
                MenuItem(renameLabel) { renaming = tile.app },
                MenuItem(unpinLabel) { vm.unpin(tile.app.key) },
                MenuItem(editLabel) { push(Screen.EditTiles) },
            ),
        )
    }

    LaunchedEffect(Unit) {
        homeEvents.collect { alreadyOnHome ->
            menu = null
            renaming = null
            when {
                !alreadyOnHome -> {
                    screens = emptyList()
                    pager.scrollToPage(PAGE_START)
                }
                screens.isNotEmpty() -> screens = emptyList()
                pager.currentPage != PAGE_START -> pager.animateScrollToPage(PAGE_START)
                else -> startList.animateScrollToItem(0)
            }
        }
    }

    // Launcher nie ma dokąd wrócić: Back zamyka ekran nakładkowy albo wraca z listy do kafelków.
    BackHandler {
        when {
            screens.isNotEmpty() -> pop()
            pager.currentPage != PAGE_START -> scope.launch { pager.animateScrollToPage(PAGE_START) }
        }
    }

    CompositionLocalProvider(LocalAccent provides accentById(cfg.accent).color) {
        Box(Modifier.fillMaxSize().background(Metro.Background)) {
            HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    PAGE_START -> StartScreen(
                        tiles = tiles,
                        icons = vm.icons,
                        listState = startList,
                        onLaunch = { vm.launch(it.app) },
                        onTileLongPress = ::showTileMenu,
                        onEditTiles = { push(Screen.EditTiles) },
                        onSettings = { push(Screen.Settings) },
                        onAllApps = { scope.launch { pager.animateScrollToPage(PAGE_APPS) } },
                    )
                    PAGE_APPS -> AppListScreen(
                        apps = apps,
                        icons = vm.icons,
                        active = resumed && screens.isEmpty() && pager.settledPage == PAGE_APPS,
                        autoLaunch = cfg.autoLaunchSingleMatch,
                        onLaunch = { vm.launch(it) },
                        onLongPress = ::showAppMenu,
                    )
                }
            }

            AnimatedContent(
                targetState = screens.lastOrNull(),
                transitionSpec = {
                    (fadeIn(tween(150)) + slideInVertically(tween(200)) { it / 12 }) togetherWith fadeOut(tween(120))
                },
                label = "screens",
            ) { screen ->
                if (screen != null) {
                    // Pochłania dotyk, żeby nie przechodził do pagera pod spodem.
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
                }
                when (screen) {
                    null -> Unit
                    Screen.Settings -> SettingsScreen(
                        config = cfg,
                        isDefaultLauncher = isDefaultLauncher,
                        onAccent = vm::setAccent,
                        onAutoLaunch = vm::setAutoLaunch,
                        onSetDefault = onSetDefaultLauncher,
                        onEditTiles = { push(Screen.EditTiles) },
                        hiddenCount = hiddenApps.size,
                        onManageHidden = { push(Screen.HiddenApps) },
                    )
                    Screen.HiddenApps -> HiddenAppsScreen(hiddenApps, vm.icons, onUnhide = { vm.unhide(it.key) })
                    Screen.EditTiles -> EditTilesScreen(
                        tiles = tiles,
                        icons = vm.icons,
                        onMove = vm::moveTile,
                        onMoveTo = vm::moveTileTo,
                        onToggleSize = vm::toggleTileSize,
                        onUnpin = vm::unpin,
                        onAdd = { push(Screen.AddTile) },
                    )
                    Screen.AddTile -> AppListScreen(
                        apps = apps?.filter { it.key !in pinned },
                        icons = vm.icons,
                        active = resumed && screens.lastOrNull() == Screen.AddTile,
                        autoLaunch = false,
                        title = stringResource(R.string.picker_title),
                        onLaunch = { app ->
                            vm.pin(app)
                            pop()
                        },
                    )
                }
            }

            menu?.let { m -> MetroContextMenu(m.anchorBottomPx, m.items, onDismiss = { menu = null }) }
            renaming?.let { app ->
                RenameDialog(
                    currentLabel = app.label,
                    originalLabel = app.originalLabel,
                    onSave = { vm.rename(app, it) },
                    onDismiss = { renaming = null },
                )
            }
        }
    }
}
