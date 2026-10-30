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
import es.dudek.flizy.ui.theme.WpAccents
import es.dudek.flizy.ui.theme.accentById

@Composable
fun SettingsScreen(
    config: LauncherConfig,
    isDefaultLauncher: Boolean,
    onAccent: (String) -> Unit,
    onAutoLaunch: (Boolean) -> Unit,
    onMoreTiles: (Boolean) -> Unit,
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
            SectionTitle(stringResource(R.string.settings_accent))
            BasicText(accentById(config.accent).name, style = MetroText.Body, modifier = Modifier.padding(bottom = 10.dp))
            AccentGrid(selected = config.accent, onSelect = onAccent)

            SectionTitle(stringResource(R.string.settings_start))
            SettingRow(
                title = stringResource(R.string.settings_more_tiles),
                description = stringResource(R.string.settings_more_tiles_desc),
            ) {
                MetroToggle(config.moreTiles, onCheckedChange = onMoreTiles)
            }

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
private fun AccentGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WpAccents.chunked(5).forEach { row ->
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
            }
        }
    }
}
