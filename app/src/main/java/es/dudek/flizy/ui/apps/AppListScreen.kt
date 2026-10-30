package es.dudek.flizy.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.dudek.flizy.R
import es.dudek.flizy.data.AppEntry
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.ui.components.AppIcon
import es.dudek.flizy.ui.components.PageHeader
import es.dudek.flizy.ui.components.metroPressable
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.LocalColors
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText
import es.dudek.flizy.util.matchScore
import es.dudek.flizy.util.normalizeForSearch

private sealed interface ListRow {
    val key: String

    data class Header(val letter: Char) : ListRow {
        override val key get() = "header:$letter"
    }

    data class App(val app: AppEntry) : ListRow {
        override val key get() = app.key.toString()
    }
}

fun filterApps(apps: List<AppEntry>, rawQuery: String): List<AppEntry> {
    val query = rawQuery.trim().normalizeForSearch()
    if (query.isEmpty()) return apps
    // sortedBy jest stabilne, więc w obrębie tej samej oceny zostaje kolejność alfabetyczna
    return apps
        .mapNotNull { app -> matchScore(app.searchName, app.searchWords, query)?.let { app to it } }
        .sortedBy { it.second }
        .map { it.first }
}

/**
 * Lista A-Z z wyszukiwarką. [active] = strona jest widoczna i aktywność wznowiona:
 * wtedy pole wyszukiwania dostaje fokus i pokazuje się klawiatura; po utracie - czyścimy.
 */
@Composable
fun AppListScreen(
    apps: List<AppEntry>?,
    icons: IconLoader,
    active: Boolean,
    autoLaunch: Boolean,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.apps_title),
    onLongPress: ((AppEntry, anchorBottomPx: Float) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(active) {
        if (active) {
            withFrameNanos { } // poczekaj, aż pole będzie podpięte do okna
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            query = ""
            focusManager.clearFocus()
            keyboard?.hide()
            listState.scrollToItem(0)
        }
    }

    // Przewijanie listy palcem chowa klawiaturę, jak w WP.
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) {
        if (dragged) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    val allApps = apps.orEmpty()
    val results = remember(allApps, query) { filterApps(allApps, query) }
    val rows = remember(results, query) {
        if (query.isBlank()) {
            buildList<ListRow> {
                var last: Char? = null
                for (app in results) {
                    if (app.section != last) {
                        add(ListRow.Header(app.section))
                        last = app.section
                    }
                    add(ListRow.App(app))
                }
            }
        } else {
            results.map<AppEntry, ListRow> { ListRow.App(it) }
        }
    }

    fun launch(app: AppEntry) {
        query = ""
        onLaunch(app)
    }

    Column(modifier.fillMaxSize().background(Metro.Background).imePadding()) {
        PageHeader(title)
        SearchField(
            value = query,
            onValueChange = { new ->
                val grew = new.trim().length > query.trim().length
                query = new
                if (autoLaunch && grew) {
                    filterApps(allApps, new).singleOrNull()?.let { launch(it) }
                }
            },
            onGo = { results.firstOrNull()?.let { launch(it) } },
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
        )

        if (apps == null) {
            BasicText(stringResource(R.string.apps_loading), style = MetroText.Subtle, modifier = Modifier.padding(20.dp))
        } else if (query.isNotBlank() && results.isEmpty()) {
            BasicText(
                stringResource(R.string.search_no_results, query.trim()),
                style = MetroText.Subtle,
                modifier = Modifier.padding(20.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            items(rows, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    is ListRow.Header -> LetterHeader(row.letter)
                    is ListRow.App -> AppRow(row.app, icons, onClick = { launch(row.app) }, onLongPress = onLongPress)
                }
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onGo: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val accent = LocalAccent.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MetroText.Body.copy(color = Metro.Background, fontSize = 19.sp),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(if (focused) Metro.Foreground else Metro.Subtle)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                if (value.isEmpty()) {
                    BasicText(
                        stringResource(R.string.search_hint),
                        style = MetroText.Body.copy(color = Metro.Background.copy(alpha = 0.55f), fontSize = 19.sp),
                    )
                }
                inner()
            }
        },
    )
}

/** Nagłówek litery jak w WP: mała litera w kwadracie w kolorze akcentu. */
@Composable
private fun LetterHeader(letter: Char) {
    val accent = LocalAccent.current
    Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(46.dp).background(accent)) {
        BasicText(
            letter.toString(),
            style = MetroText.Item.copy(fontSize = 26.sp, color = LocalColors.current.inkOn(accent)),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 5.dp, bottom = 1.dp),
        )
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    icons: IconLoader,
    onClick: () -> Unit,
    onLongPress: ((AppEntry, Float) -> Unit)?,
) {
    var bottom by remember { mutableFloatStateOf(0f) }
    Row(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bottom = it.boundsInRoot().bottom }
            .metroPressable(onLongClick = onLongPress?.let { { it(app, bottom) } }, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, icons, 40.dp)
        Spacer(Modifier.width(14.dp))
        BasicText(app.label, style = MetroText.Item, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
