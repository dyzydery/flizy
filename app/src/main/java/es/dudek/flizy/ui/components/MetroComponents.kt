package es.dudek.flizy.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import es.dudek.flizy.R
import es.dudek.flizy.data.AppEntry
import es.dudek.flizy.data.IconLoader
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText

/**
 * Wciśnięcie w stylu WP: element lekko się zmniejsza i pochyla w stronę punktu dotyku.
 */
fun Modifier.metroPressable(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    val progress by animateFloatAsState(if (pressed) 1f else 0f, tween(if (pressed) 90 else 160), label = "press")
    val density = LocalDensity.current.density

    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            if (size.width > 0 && size.height > 0) {
                // -1..1 względem środka elementu
                val dx = (touch.x / size.width - 0.5f) * 2f
                val dy = (touch.y / size.height - 0.5f) * 2f
                // Mniejsze kąty dla szerokich kafelków, żeby krawędź nie "uciekała" za daleko.
                val maxY = 10f * (size.height.toFloat() / size.width).coerceAtMost(1f)
                rotationY = -dx * maxY * progress
                rotationX = dy * 10f * progress
                val scale = 1f - 0.04f * progress
                scaleX = scale
                scaleY = scale
                cameraDistance = 16f * density
            }
        }
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = { position ->
                    touch = position
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = onLongClick?.let { { _: Offset -> it() } },
                onTap = { onClick() },
            )
        }
}

/** Zwykłe kliknięcie bez efektu falowania Material. */
fun Modifier.plainClickable(role: Role? = Role.Button, onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = role, onClick = onClick)
}

@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier.statusBarsPadding().padding(start = 20.dp, top = 12.dp, end = 20.dp)) {
        BasicText(stringResource(R.string.app_title_caps), style = MetroText.AppTitle)
        BasicText(title, style = MetroText.PageTitle, modifier = Modifier.offset(x = (-3).dp))
    }
}

/** Przycisk WP: prostokąt z białą ramką. */
@Composable
fun MetroButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .metroPressable(onClick = onClick)
            .border(2.dp, Metro.Foreground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BasicText(text, style = MetroText.Body)
    }
}

/** Przełącznik WP: prostokątny tor, wypełniony kolorem akcentu po włączeniu. */
@Composable
fun MetroToggle(checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    val position by animateFloatAsState(if (checked) 1f else 0f, tween(120), label = "toggle")
    Box(
        modifier
            .size(width = 58.dp, height = 26.dp)
            .plainClickable(Role.Switch) { onCheckedChange(!checked) }
            .border(2.dp, Metro.Foreground)
            .padding(4.dp),
    ) {
        Box(Modifier.fillMaxSize().background(if (checked) accent else Color.Transparent))

        Box(
            Modifier
                .offset { IntOffset(((58 - 8 - 14).dp.toPx() * position).toInt(), 0) }
                .offset(y = (-6).dp)
                .size(width = 14.dp, height = 30.dp)
                .background(Metro.Foreground),
        )
    }
}

@Composable
fun AppIcon(app: AppEntry, icons: IconLoader, size: Dp, modifier: Modifier = Modifier) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState(icons.cachedIcon(app), app.key) { value = icons.icon(app, sizePx) }
    Box(modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
fun rememberMonoIcon(app: AppEntry, icons: IconLoader): ImageBitmap? {
    val bitmap by produceState(icons.cachedMono(app), app.key) { value = icons.monoIcon(app) }
    return bitmap
}

data class MenuItem(val label: String, val action: () -> Unit)

/**
 * Menu kontekstowe w stylu WP: ciemny pasek na całą szerokość pod przytrzymanym elementem,
 * reszta ekranu przyciemniona.
 */
@Composable
fun MetroContextMenu(anchorBottomPx: Float, items: List<MenuItem>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val density = LocalDensity.current
    var menuHeight by remember { mutableStateOf(0) }
    var screenHeight by remember { mutableStateOf(0) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = it.height }
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val y = if (anchorBottomPx + menuHeight > screenHeight - with(density) { 48.dp.toPx() }) {
            (screenHeight - menuHeight - with(density) { 48.dp.toPx() }).coerceAtLeast(0f)
        } else {
            anchorBottomPx
        }
        Column(
            Modifier
                .offset { IntOffset(0, y.toInt()) }
                .fillMaxWidth()
                .onSizeChanged { menuHeight = it.height }
                .background(Metro.MenuBackground)
                .pointerInput(Unit) { detectTapGestures { } } // nie zamykaj po stuknięciu w tło menu
                .padding(vertical = 12.dp),
        ) {
            items.forEach { item ->
                BasicText(
                    item.label,
                    style = MetroText.Menu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .plainClickable {
                            onDismiss()
                            item.action()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun SettingRow(title: String, modifier: Modifier = Modifier, description: String? = null, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = MetroText.Body)
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                BasicText(description, style = MetroText.Subtle)
            }
        }
        Spacer(Modifier.width(16.dp))
        trailing()
    }
}
