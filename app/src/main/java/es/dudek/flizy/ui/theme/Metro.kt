package es.dudek.flizy.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.dudek.flizy.R

data class Accent(val id: String, val name: String, val color: Color)

/** Kolory akcentu Windows Phone 8.1 (oryginalne wartości HEX). */
val WpAccents = listOf(
    Accent("lime", "limonkowy", Color(0xFFA4C400)),
    Accent("green", "zielony", Color(0xFF60A917)),
    Accent("emerald", "szmaragdowy", Color(0xFF008A00)),
    Accent("teal", "morski", Color(0xFF00ABA9)),
    Accent("cyan", "cyjan", Color(0xFF1BA1E2)),
    Accent("cobalt", "kobaltowy", Color(0xFF0050EF)),
    Accent("indigo", "indygo", Color(0xFF6A00FF)),
    Accent("violet", "fioletowy", Color(0xFFAA00FF)),
    Accent("pink", "różowy", Color(0xFFF472D0)),
    Accent("magenta", "purpurowy", Color(0xFFD80073)),
    Accent("crimson", "karmazynowy", Color(0xFFA20025)),
    Accent("red", "czerwony", Color(0xFFE51400)),
    Accent("orange", "pomarańczowy", Color(0xFFFA6800)),
    Accent("amber", "bursztynowy", Color(0xFFF0A30A)),
    Accent("yellow", "żółty", Color(0xFFE3C800)),
    Accent("brown", "brązowy", Color(0xFF825A2C)),
    Accent("olive", "oliwkowy", Color(0xFF6D8764)),
    Accent("steel", "stalowy", Color(0xFF647687)),
    Accent("mauve", "liliowy", Color(0xFF76608A)),
    Accent("taupe", "taupe", Color(0xFF87794E)),
)

fun accentById(id: String): Accent = WpAccents.firstOrNull { it.id == id } ?: WpAccents.first { it.id == "cobalt" }

val LocalAccent = staticCompositionLocalOf { accentById("cobalt").color }

object Metro {
    val Background = Color.Black
    val Foreground = Color.White
    val Subtle = Color(0xFF999999)
    val MenuBackground = Color(0xFF1F1F1F)
    val Chrome = Color(0xFF1F1F1F)
}

val Selawik = FontFamily(
    Font(R.font.selawik_light, FontWeight.Light),
    Font(R.font.selawik_semilight, FontWeight.ExtraLight),
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_semibold, FontWeight.SemiBold),
)

object MetroText {
    /** Duży nagłówek strony ("aplikacje", "ustawienia"). */
    val PageTitle = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Light, fontSize = 52.sp, color = Metro.Foreground)

    /** Mały nagłówek nad tytułem ("FLIZY"). */
    val AppTitle = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Metro.Foreground)

    val Item = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Light, fontSize = 22.sp, color = Metro.Foreground)
    val Body = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Normal, fontSize = 17.sp, color = Metro.Foreground)
    val Subtle = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Metro.Subtle)
    val Section = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Metro.Subtle)
    val TileLabel = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color.White)
    val Menu = TextStyle(fontFamily = Selawik, fontWeight = FontWeight.Normal, fontSize = 21.sp, color = Metro.Foreground)
}
