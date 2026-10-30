package es.dudek.flizy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import es.dudek.flizy.data.TileStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.dudek.flizy.R

data class Accent(val id: String, val name: String, val color: Color)

/**
 * Motyw kolorystyczny. [lightInk]/[darkInk] to kolory ikon i napisów na kafelkach:
 * na jasnym kafelku ciemne, na ciemnym jasne. [alwaysLightInk] = zawsze jasne (klasyczny WP).
 */
data class FlizyTheme(
    val id: String,
    val name: String,
    val background: Color,
    val surface: Color,
    val text: Color,
    val subtle: Color,
    val lightInk: Color,
    val darkInk: Color,
    /** W kolejności używanej w trybie tęczy - sąsiednie kolory mają się wyraźnie różnić. */
    val accents: List<Accent>,
    val defaultAccent: String,
    val alwaysLightInk: Boolean = false,
) {
    fun accent(id: String): Accent = accents.firstOrNull { it.id == id } ?: accents.first { it.id == defaultAccent }
}

private fun a(id: String, hex: Long, name: String = id) = Accent(id, name, Color(hex))

val Themes = listOf(
    FlizyTheme(
        id = "metro", name = "Metro",
        background = Color.Black, surface = Color(0xFF1F1F1F),
        text = Color.White, subtle = Color(0xFF999999),
        lightInk = Color.White, darkInk = Color.Black, alwaysLightInk = true,
        // Kolory akcentu Windows Phone 8.1 (oryginalne wartości HEX).
        accents = listOf(
            a("lime", 0xFFA4C400, "limonkowy"), a("green", 0xFF60A917, "zielony"),
            a("emerald", 0xFF008A00, "szmaragdowy"), a("teal", 0xFF00ABA9, "morski"),
            a("cyan", 0xFF1BA1E2, "cyjan"), a("cobalt", 0xFF0050EF, "kobaltowy"),
            a("indigo", 0xFF6A00FF, "indygo"), a("violet", 0xFFAA00FF, "fioletowy"),
            a("pink", 0xFFF472D0, "różowy"), a("magenta", 0xFFD80073, "purpurowy"),
            a("crimson", 0xFFA20025, "karmazynowy"), a("red", 0xFFE51400, "czerwony"),
            a("orange", 0xFFFA6800, "pomarańczowy"), a("amber", 0xFFF0A30A, "bursztynowy"),
            a("yellow", 0xFFE3C800, "żółty"), a("brown", 0xFF825A2C, "brązowy"),
            a("olive", 0xFF6D8764, "oliwkowy"), a("steel", 0xFF647687, "stalowy"),
            a("mauve", 0xFF76608A, "liliowy"), a("taupe", 0xFF87794E, "taupe"),
        ),
        defaultAccent = "cobalt",
    ),
    FlizyTheme(
        id = "catppuccin", name = "Catppuccin Mocha",
        background = Color(0xFF1E1E2E), surface = Color(0xFF313244),
        text = Color(0xFFCDD6F4), subtle = Color(0xFFA6ADC8),
        lightInk = Color(0xFFCDD6F4), darkInk = Color(0xFF11111B),
        accents = listOf(
            a("mauve", 0xFFCBA6F7), a("blue", 0xFF89B4FA), a("peach", 0xFFFAB387), a("green", 0xFFA6E3A1),
            a("pink", 0xFFF5C2E7), a("teal", 0xFF94E2D5), a("yellow", 0xFFF9E2AF), a("lavender", 0xFFB4BEFE),
            a("red", 0xFFF38BA8), a("sapphire", 0xFF74C7EC), a("flamingo", 0xFFF2CDCD), a("sky", 0xFF89DCEB),
            a("maroon", 0xFFEBA0AC), a("rosewater", 0xFFF5E0DC),
        ),
        defaultAccent = "mauve",
    ),
    FlizyTheme(
        id = "solarized", name = "Solarized Dark",
        background = Color(0xFF002B36), surface = Color(0xFF073642),
        text = Color(0xFF93A1A1), subtle = Color(0xFF657B83),
        lightInk = Color(0xFFFDF6E3), darkInk = Color(0xFF002B36),
        accents = listOf(
            a("blue", 0xFF268BD2), a("orange", 0xFFCB4B16), a("cyan", 0xFF2AA198), a("magenta", 0xFFD33682),
            a("green", 0xFF859900), a("violet", 0xFF6C71C4), a("yellow", 0xFFB58900), a("red", 0xFFDC322F),
        ),
        defaultAccent = "blue",
    ),
    FlizyTheme(
        id = "rosepine", name = "Rosé Pine",
        background = Color(0xFF191724), surface = Color(0xFF26233A),
        text = Color(0xFFE0DEF4), subtle = Color(0xFF908CAA),
        lightInk = Color(0xFFE0DEF4), darkInk = Color(0xFF191724),
        accents = listOf(
            a("iris", 0xFFC4A7E7), a("foam", 0xFF9CCFD8), a("rose", 0xFFEBBCBA), a("pine", 0xFF31748F),
            a("gold", 0xFFF6C177), a("love", 0xFFEB6F92),
        ),
        defaultAccent = "iris",
    ),
    FlizyTheme(
        id = "nord", name = "Nord",
        background = Color(0xFF2E3440), surface = Color(0xFF3B4252),
        text = Color(0xFFECEFF4), subtle = Color(0xFFA0A8B7),
        lightInk = Color(0xFFECEFF4), darkInk = Color(0xFF2E3440),
        accents = listOf(
            a("frost", 0xFF88C0D0), a("aurora-orange", 0xFFD08770, "orange"), a("frost-blue", 0xFF5E81AC, "blue"),
            a("aurora-green", 0xFFA3BE8C, "green"), a("aurora-purple", 0xFFB48EAD, "purple"),
            a("frost-teal", 0xFF8FBCBB, "teal"), a("aurora-red", 0xFFBF616A, "red"),
            a("frost-light", 0xFF81A1C1, "light blue"), a("aurora-yellow", 0xFFEBCB8B, "yellow"),
        ),
        defaultAccent = "frost",
    ),
    FlizyTheme(
        id = "gruvbox", name = "Gruvbox Dark",
        background = Color(0xFF282828), surface = Color(0xFF3C3836),
        text = Color(0xFFEBDBB2), subtle = Color(0xFFA89984),
        lightInk = Color(0xFFFBF1C7), darkInk = Color(0xFF282828),
        accents = listOf(
            a("aqua", 0xFF689D6A), a("orange", 0xFFD65D0E), a("blue", 0xFF458588), a("yellow", 0xFFD79921),
            a("purple", 0xFFB16286), a("green", 0xFF98971A), a("red", 0xFFCC241D),
        ),
        defaultAccent = "aqua",
    ),
)

fun themeById(id: String): FlizyTheme = Themes.firstOrNull { it.id == id } ?: Themes.first()

/** Kolory w użyciu: motyw + wybrany akcent + przełączniki z ustawień. */
/** Kolory jednego kafelka. */
data class TileLook(val background: Color, val border: Color?, val icon: Color, val label: Color)

@Immutable
data class FlizyColors(
    val theme: FlizyTheme,
    val accent: Color,
    val rainbow: Boolean,
    val amoledBlack: Boolean,
    val tileStyle: TileStyle = TileStyle.FILLED,
) {
    val background: Color get() = if (amoledBlack) Color.Black else theme.background
    val surface: Color get() = theme.surface
    val text: Color get() = theme.text
    val subtle: Color get() = theme.subtle

    /** Kolor kafelka na pozycji [index]: akcent albo kolejny kolor motywu w trybie tęczy. */
    fun tileColor(index: Int): Color =
        if (rainbow) theme.accents[index % theme.accents.size].color else accent

    fun tileLook(color: Color): TileLook = when (tileStyle) {
        TileStyle.FILLED -> inkOn(color).let { TileLook(color, null, it, it) }
        TileStyle.OUTLINE -> TileLook(Color.Transparent, color, color, text)
        TileStyle.TINTED -> TileLook(color.copy(alpha = 0.2f).compositeOver(background), null, color, text)
    }

    /** Kolor ikony i napisu na tle [color]. */
    fun inkOn(color: Color): Color = when {
        theme.alwaysLightInk -> theme.lightInk
        color.luminance() > 0.3f -> theme.darkInk
        else -> theme.lightInk
    }

    companion object {
        val Default = Themes.first().let { FlizyColors(it, it.accent(it.defaultAccent).color, rainbow = false, amoledBlack = false) }
    }
}

val LocalColors = staticCompositionLocalOf { FlizyColors.Default }

val LocalAccent = staticCompositionLocalOf { FlizyColors.Default.accent }

/** Skróty do kolorów bieżącego motywu. */
object Metro {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalColors.current.background
    val Foreground: Color @Composable @ReadOnlyComposable get() = LocalColors.current.text
    val Subtle: Color @Composable @ReadOnlyComposable get() = LocalColors.current.subtle
    val MenuBackground: Color @Composable @ReadOnlyComposable get() = LocalColors.current.surface
    val Chrome: Color @Composable @ReadOnlyComposable get() = LocalColors.current.surface
}

val Selawik = FontFamily(
    Font(R.font.selawik_light, FontWeight.Light),
    Font(R.font.selawik_semilight, FontWeight.ExtraLight),
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_semibold, FontWeight.SemiBold),
)

private fun style(weight: FontWeight, size: Int, lineHeight: Int? = null) = TextStyle(
    fontFamily = Selawik,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
)

private val PageTitleBase = style(FontWeight.Light, 52)
private val AppTitleBase = style(FontWeight.SemiBold, 14)
private val ItemBase = style(FontWeight.Light, 22)
private val BodyBase = style(FontWeight.Normal, 17)
private val SubtleBase = style(FontWeight.Normal, 15)
private val SectionBase = style(FontWeight.SemiBold, 15)
private val ClockBase = style(FontWeight.Light, 88, lineHeight = 92)
private val DateBase = style(FontWeight.Normal, 20)
private val TileLabelBase = style(FontWeight.Normal, 14)
private val MenuBase = style(FontWeight.Normal, 21)

/** Style tekstu; kolor z bieżącego motywu. Napisy na kafelkach: kolor ustawia kafelek. */
object MetroText {
    /** Duży nagłówek strony ("aplikacje", "ustawienia"). */
    val PageTitle: TextStyle @Composable @ReadOnlyComposable get() = PageTitleBase.copy(color = Metro.Foreground)

    /** Mały nagłówek nad tytułem ("FLIZY"). */
    val AppTitle: TextStyle @Composable @ReadOnlyComposable get() = AppTitleBase.copy(color = Metro.Foreground)

    val Item: TextStyle @Composable @ReadOnlyComposable get() = ItemBase.copy(color = Metro.Foreground)
    val Body: TextStyle @Composable @ReadOnlyComposable get() = BodyBase.copy(color = Metro.Foreground)
    val Subtle: TextStyle @Composable @ReadOnlyComposable get() = SubtleBase.copy(color = Metro.Subtle)
    val Section: TextStyle @Composable @ReadOnlyComposable get() = SectionBase.copy(color = Metro.Subtle)
    val Clock: TextStyle @Composable @ReadOnlyComposable get() = ClockBase.copy(color = Metro.Foreground)
    val Date: TextStyle @Composable @ReadOnlyComposable get() = DateBase.copy(color = Metro.Foreground)
    val TileLabel: TextStyle get() = TileLabelBase
    val Menu: TextStyle @Composable @ReadOnlyComposable get() = MenuBase.copy(color = Metro.Foreground)
}
