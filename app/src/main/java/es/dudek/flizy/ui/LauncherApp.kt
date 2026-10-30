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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
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
import es.dudek.flizy.ui.theme.FlizyColors
import es.dudek.flizy.ui.theme.LocalColors
import es.dudek.flizy.ui.theme.themeById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private class MenuRequest(val anchorBottomPx: Float, val items: List<MenuItem>)

/** Ekrany otwierane nad pagerem Start/Aplikacje. */
private enum class Screen { Settings, EditTiles, AddTile, HiddenApps, PickSwipeApp }

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
    val swipeRightApp by vm.swipeRightApp.collectAsStateWithLifecycle()

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
    val growLabel = stringResource(R.string.menu_resize_grow)
    val shrinkLabel = stringResource(R.string.menu_resize_shrink)
    val editLabel = stringResource(R.string.menu_edit_tiles)
    val settingsLabel = stringResource(R.string.menu_settings)
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
        val size = tile.config.size
        menu = MenuRequest(
            anchor,
            listOfNotNull(
                MenuItem(growLabel) { vm.resizeTile(tile.app.key, 1) }.takeIf { size != TileSize.FULL },
                MenuItem(shrinkLabel) { vm.resizeTile(tile.app.key, -1) }.takeIf { size != TileSize.QUARTER },
                MenuItem(renameLabel) { renaming = tile.app },
                MenuItem(unpinLabel) { vm.unpin(tile.app.key) },
                MenuItem(editLabel) { push(Screen.EditTiles) },
            ),
        )
    }

    fun showStartMenu(anchor: Float) {
        menu = MenuRequest(
            anchor,
            listOf(
                MenuItem(editLabel) { push(Screen.EditTiles) },
                MenuItem(settingsLabel) { push(Screen.Settings) },
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

    // Gesty na ekranie Start, rozpoznawane z przewijania, którego nikt nie zużył:
    // w prawo (pager jest na pierwszej stronie) i w dół (kafelki są na samej górze).
    val gestureThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val gestures = remember(gestureThreshold) {
        object : NestedScrollConnection {
            var dx = 0f
            var dy = 0f
            var fired = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || fired || pager.currentPage != PAGE_START || screens.isNotEmpty()) {
                    return Offset.Zero
                }
                if (available.x > 0f) dx += available.x
                if (available.y > 0f) dy += available.y
                when {
                    dx > gestureThreshold -> {
                        fired = true
                        swipeRightApp?.let { vm.launch(it) }
                    }
                    dy > gestureThreshold -> {
                        fired = true
                        scope.launch { pager.animateScrollToPage(PAGE_APPS) }
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                dx = 0f
                dy = 0f
                fired = false
                return Velocity.Zero
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

    val theme = themeById(cfg.theme)
    val colors = FlizyColors(theme, theme.accent(cfg.accent).color, cfg.rainbow, cfg.amoledBlack, cfg.tileStyle)
    CompositionLocalProvider(LocalColors provides colors, LocalAccent provides colors.accent) {
        Box(Modifier.fillMaxSize().background(Metro.Background).nestedScroll(gestures)) {
            HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    PAGE_START -> StartScreen(
                        tiles = tiles,
                        icons = vm.icons,
                        listState = startList,
                        onLaunch = { vm.launch(it.app) },
                        onTileLongPress = ::showTileMenu,
                        onEmptyLongPress = ::showStartMenu,
                        onClockClick = vm::openAlarms,
                        onDateClick = vm::openPrivateCalendar,
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
                        onTheme = vm::setTheme,
                        onRainbow = vm::setRainbow,
                        onAmoledBlack = vm::setAmoledBlack,
                        onTileStyle = vm::setTileStyle,
                        onAutoLaunch = vm::setAutoLaunch,
                        onSetDefault = onSetDefaultLauncher,
                        onEditTiles = { push(Screen.EditTiles) },
                        hiddenCount = hiddenApps.size,
                        onManageHidden = { push(Screen.HiddenApps) },
                        swipeRightApp = swipeRightApp?.label,
                        onPickSwipeApp = { push(Screen.PickSwipeApp) },
                        onClearSwipeApp = { vm.setSwipeRightApp(null) },
                    )
                    Screen.HiddenApps -> HiddenAppsScreen(hiddenApps, vm.icons, onUnhide = { vm.unhide(it.key) })
                    Screen.PickSwipeApp -> AppListScreen(
                        apps = apps,
                        icons = vm.icons,
                        active = resumed && screens.lastOrNull() == Screen.PickSwipeApp,
                        autoLaunch = false,
                        title = stringResource(R.string.picker_gesture_title),
                        onLaunch = { app ->
                            vm.setSwipeRightApp(app.key)
                            pop()
                        },
                    )
                    Screen.EditTiles -> EditTilesScreen(
                        tiles = tiles,
                        icons = vm.icons,
                        onMove = vm::moveTile,
                        onMoveTo = vm::moveTileTo,
                        onToggleSize = vm::cycleTileSize,
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
