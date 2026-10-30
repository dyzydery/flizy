package es.dudek.flizy.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.dudek.flizy.R
import es.dudek.flizy.ui.theme.LocalAccent
import es.dudek.flizy.ui.theme.Metro
import es.dudek.flizy.ui.theme.MetroText

/** Okno zmiany nazwy w stylu WP: pasek u góry ekranu z polem tekstowym i przyciskami. */
@Composable
fun RenameDialog(
    currentLabel: String,
    originalLabel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(currentLabel, TextRange(0, currentLabel.length))) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    fun save() {
        keyboard?.hide()
        onSave(value.text)
        onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Metro.MenuBackground)
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            BasicText(stringResource(R.string.rename_title), style = MetroText.Item.copy(fontSize = 28.sp))
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = MetroText.Body.copy(color = Metro.Background, fontSize = 19.sp),
                cursorBrush = SolidColor(LocalAccent.current),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().background(Metro.Foreground).padding(horizontal = 10.dp, vertical = 9.dp)) { inner() }
                },
            )
            Spacer(Modifier.height(8.dp))
            BasicText(stringResource(R.string.rename_hint, originalLabel), style = MetroText.Subtle)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetroButton(stringResource(R.string.rename_save), onClick = ::save)
                MetroButton(stringResource(R.string.rename_cancel)) {
                    keyboard?.hide()
                    onDismiss()
                }
            }
        }
    }
}
