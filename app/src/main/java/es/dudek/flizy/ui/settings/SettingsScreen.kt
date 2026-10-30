package es.dudek.flizy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.dudek.flizy.BuildConfig
import es.dudek.flizy.R
import es.dudek.flizy.data.LauncherConfig
import es.dudek.flizy.ui.components.MetroButton
import es.dudek.flizy.ui.components.MetroToggle
import es.dudek.flizy.ui.components.PageHeader
import es.dudek.flizy.ui.components.SettingRow
import es.dudek.flizy.ui.components.plainClickable
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText
import es.dudek.flizy.ui.theme.Accent
import es.dudek.flizy.ui.theme.FlizyTheme
import es.dudek.flizy.ui.theme.Themes
import es.dudek.flizy.ui.theme.themeById

@Composable
fun SettingsScreen(
    config: LauncherConfig,
    isDefaultLauncher: Boolean,
    onAccent: (String) -> Unit,
    onTheme: (String) -> Unit,
    onRainbow: (Boolean) -> Unit,
    onAmoledBlack: (Boolean) -> Unit,
    onAutoLaunch: (Boolean) -> Unit,
    onSetDefault: () -> Unit,
    onEditTiles: () -> Unit,
    hiddenCount: Int,
    onManageHidden: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Metro.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(stringResource(R.string.settings_title))
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            val theme = themeById(config.theme)
            val accent = theme.accent(config.accent)

            SectionTitle(stringResource(R.string.settings_theme))
            Themes.forEach { t ->
                ThemeRow(t, selected = t.id == theme.id) { onTheme(t.id) }
            }
            SettingRow(
                title = stringResource(R.string.settings_amoled),
                description = stringResource(R.string.settings_amoled_desc),
            ) {
                MetroToggle(config.amoledBlack, onCheckedChange = onAmoledBlack)
            }

            SectionTitle(stringResource(R.string.settings_accent))
            SettingRow(
                title = stringResource(R.string.settings_rainbow),
                description = stringResource(R.string.settings_rainbow_desc),
            ) {
                MetroToggle(config.rainbow, onCheckedChange = onRainbow)
            }
            BasicText(accent.name, style = MetroText.Body, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
            AccentGrid(theme.accents, selected = accent.id, onSelect = onAccent)

            SectionTitle(stringResource(R.string.settings_search))
            SettingRow(
                title = stringResource(R.string.settings_auto_launch),
                description = stringResource(R.string.settings_auto_launch_desc),
            ) {
                MetroToggle(config.autoLaunchSingleMatch, onCheckedChange = onAutoLaunch)
            }

            SectionTitle(stringResource(R.string.settings_launcher))
            BasicText(
                stringResource(if (isDefaultLauncher) R.string.settings_is_default else R.string.settings_not_default),
                style = MetroText.Body,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            MetroButton(stringResource(R.string.settings_set_default), onClick = onSetDefault)

            SectionTitle(stringResource(R.string.settings_hidden))
            BasicText(
                stringResource(R.string.settings_hidden_count, hiddenCount),
                style = MetroText.Body,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            MetroButton(stringResource(R.string.settings_hidden_manage), onClick = onManageHidden)

            SectionTitle(stringResource(R.string.settings_tiles))
            MetroButton(stringResource(R.string.settings_edit_tiles), onClick = onEditTiles)

            Spacer(Modifier.height(40.dp))
            BasicText(stringResource(R.string.settings_about, BuildConfig.VERSION_NAME), style = MetroText.Subtle)
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(text.uppercase(), style = MetroText.Section, modifier = Modifier.padding(top = 28.dp, bottom = 6.dp))
}

@Composable
private fun AccentGrid(accents: List<Accent>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accents.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { accent ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .semantics { contentDescription = accent.name }
                            .then(if (accent.id == selected) Modifier.border(3.dp, Metro.Foreground) else Modifier)
                            .padding(if (accent.id == selected) 5.dp else 0.dp)
                            .background(accent.color)
                            .plainClickable { onSelect(accent.id) },
                    )
                }
                // Niepełny ostatni rząd: puste miejsca, żeby próbki miały ten sam rozmiar.
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Motyw: nazwa i pasek próbek na jego własnym tle; wybrany obramowany. */
@Composable
private fun ThemeRow(theme: FlizyTheme, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (selected) Modifier.border(2.dp, Metro.Foreground) else Modifier)
            .background(theme.background)
            .plainClickable(onClick = onClick)
            .padding(10.dp),
    ) {
        BasicText(theme.name, style = MetroText.Body.copy(color = theme.text))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            theme.accents.take(10).forEach {
                Box(Modifier.weight(1f).height(14.dp).background(it.color))
            }
        }
    }
}
