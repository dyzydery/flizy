package es.dudek.flizy.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.dudek.flizy.R
import es.dudek.flizy.data.AppEntry
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.ui.components.AppIcon
import es.dudek.flizy.ui.components.PageHeader
import es.dudek.flizy.ui.components.metroPressable
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText

@Composable
fun HiddenAppsScreen(hidden: List<AppEntry>, icons: IconLoader, onUnhide: (AppEntry) -> Unit) {
    Column(Modifier.fillMaxSize().background(Metro.Background)) {
        PageHeader(stringResource(R.string.hidden_title))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                BasicText(
                    stringResource(if (hidden.isEmpty()) R.string.hidden_empty else R.string.hidden_hint),
                    style = MetroText.Subtle,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            items(hidden, key = { it.key.toString() }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .metroPressable { onUnhide(app) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app, icons, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    BasicText(
                        app.label,
                        style = MetroText.Item,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(stringResource(R.string.hidden_show), style = MetroText.Subtle.copy(color = LocalAccent.current))
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
    }
}
