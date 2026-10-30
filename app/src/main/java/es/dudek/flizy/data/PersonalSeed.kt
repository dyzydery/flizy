package es.dudek.flizy.data

/**
 * Konfiguracja startowa przeniesiona z Multilaunchera (ekran główny, ukryte aplikacje, aliasy).
 * Stosowana raz przy pierwszym uruchomieniu; zmiana zawartości wymaga podbicia [VERSION].
 *
 * userId to identyfikator profilu Androida (0 = główny, 11 = profil służbowy),
 * zamieniany w czasie działania na numer seryjny profilu.
 */
object PersonalSeed {
    const val VERSION = 5

    /** [className] null = pierwsza aktywność launchera z pakietu. */
    data class App(val packageName: String, val className: String?, val userId: Int)

    /** [since] = wersja konfiguracji, w której pozycja doszła (do uzupełniania istniejących instalacji). */
    data class SeedTile(val app: App, val size: TileSize, val since: Int = 1)

    data class SeedAlias(val packageName: String, val userId: Int, val label: String, val since: Int = 1)

    private const val MAIN = 0
    private const val WORK = 11

    /** MIUI "Podwójne aplikacje" (XSpace) - profil klonów. */
    private const val CLONE = 999

    /**
     * Układ Startu: 5 pełnych rzędów (każdy = 12 kolumn), które razem z zegarem wypełniają ekran.
     * Kolejność = kolejność wypełniania rzędów.
     */
    val tiles = listOf(
        // praca
        SeedTile(App("com.google.android.calendar", "com.android.calendar.AllInOneActivity", WORK), TileSize.HALF),
        SeedTile(App("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail", WORK), TileSize.QUARTER),
        SeedTile(App("com.anthropic.claude", "com.anthropic.claude.mainactivity.MainActivity", WORK), TileSize.QUARTER, since = 4),
        // rozmowy
        SeedTile(App("com.whatsapp", "com.whatsapp.Main", MAIN), TileSize.QUARTER),
        SeedTile(App("com.whatsapp", "com.whatsapp.Main", CLONE), TileSize.QUARTER, since = 2),
        SeedTile(App("org.fossify.messages", "org.fossify.messages.activities.SplashActivity.Green", MAIN), TileSize.QUARTER, since = 4),
        SeedTile(App("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail", MAIN), TileSize.QUARTER),
        // w drodze
        SeedTile(App("org.mozilla.firefox", "org.mozilla.firefox.App", MAIN), TileSize.THIRD, since = 4),
        SeedTile(App("com.google.android.apps.maps", "com.google.android.maps.MapsActivity", MAIN), TileSize.THIRD),
        SeedTile(App("com.spotify.music", "com.spotify.music.MainActivity", MAIN), TileSize.THIRD),
        // nauka i wiadomości
        SeedTile(App("com.duolingo", "com.duolingo.app.LoginActivity", MAIN), TileSize.THIRD),
        SeedTile(App("io.github.dyzydery.bahasakotak", "io.github.dyzydery.bahasakotak.MainActivity", MAIN), TileSize.THIRD, since = 4),
        SeedTile(App("pl.inko.infopigulaOne", "pl.inko.infopigulaOne.MainActivity", MAIN), TileSize.THIRD),
        // narzędzia
        SeedTile(App("com.android.camera", "com.android.camera.Camera", MAIN), TileSize.QUARTER, since = 4),
        SeedTile(App("org.fossify.gallery", "org.fossify.gallery.activities.SplashActivity.Green", MAIN), TileSize.QUARTER, since = 4),
        SeedTile(App("com.darkempire78.opencalculator", "com.darkempire78.opencalculator.activities.MainActivity", MAIN), TileSize.QUARTER),
        SeedTile(App("com.superproductivity.superproductivity", "com.superproductivity.superproductivity.FullscreenActivity", MAIN), TileSize.QUARTER, since = 4),
    )

    /** Od tej wersji układ kafelków jest ustawiany w całości (kolejność i rozmiary), a nie tylko uzupełniany. */
    const val LAYOUT_SINCE = 5

    /** Gest na ekranie Start (jak "swipe left" w Multilauncherze). */
    val swipeRightApp = App("org.mozilla.firefox", "org.mozilla.firefox.App", MAIN)
    const val SWIPE_RIGHT_SINCE = 3

    val hidden = listOf(
        App("com.android.deskclock", "com.android.deskclock.DeskClockTabActivity", MAIN),
        App("com.google.android.apps.subscriptions.red", "com.google.android.apps.subscriptions.red.LauncherActivity", MAIN),
        App("com.xiaomi.mipicks", "com.xiaomi.mipicks.MiPicksAlias", MAIN),
        App("com.miui.notes", "com.miui.notes.ui.NotesListActivity", MAIN),
        App("com.mi.android.globalFileexplorer", "com.android.fileexplorer.FileExplorerTabActivity", MAIN),
        App("com.miui.miservice", "com.miui.miservice.main.MainActivity", MAIN),
        App("com.miui.videoplayer", "com.miui.video.global.app.LauncherAlias", MAIN),
        App("com.gitlab.mudlej.MjPdfReader", "com.gitlab.mudlej.MjPdfReader.ui.main.MainActivity", MAIN),
        App("com.xiaomi.glgm", "com.mig.play.Icon0", MAIN),
        App("com.xiaomi.midrop", "com.xiaomi.midrop.SplashScreen", MAIN),
        App("com.miui.player", "com.miui.player.ui.MusicBrowserActivity", MAIN),
        App("com.miui.weather2", "com.miui.weather2.ActivityWeatherMain", MAIN),
        App("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList", WORK),
        App("com.miui.securitycenter", "com.miui.securityscan.MainActivity", MAIN),
        App("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList", MAIN),
        App("com.google.android.apps.messaging", "com.google.android.apps.messaging.ui.ConversationListActivity", MAIN),
        App("com.mi.globalminusscreen", "com.mi.globalminusscreen.LauncherAlias", WORK),
        App("com.android.thememanager", "com.android.thememanager.ThemeResourceTabActivity", MAIN),
        App("com.miui.compass", "com.miui.compass.CompassActivity", MAIN),
        App("com.mi.globalminusscreen", "com.mi.globalminusscreen.LauncherAlias", MAIN),
        App("com.google.android.contacts", "com.android.contacts.activities.PeopleActivity", WORK),
        App("com.google.android.contacts", "com.android.contacts.activities.PeopleActivity", MAIN),
        App("com.android.thememanager", "com.android.thememanager.ThemeResourceTabActivity", WORK),
        App("helium314.keyboard", "helium314.keyboard.settings.SettingsActivity", MAIN),
        App("com.android.stk", "com.android.stk.StkMain", MAIN),
        App("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity", MAIN),
        App("com.google.android.apps.safetyhub", "com.google.android.apps.safetyhub.LauncherActivity", MAIN),
    )

    /** Aplikacje z profilu służbowego dostają prefiks "AVS", żeby odróżnić je od prywatnych. */
    val aliases = listOf(
        SeedAlias("com.google.android.calendar", MAIN, "Calendar"),
        SeedAlias("com.google.android.calendar", WORK, "AVS Calendar"),
        SeedAlias("com.google.android.gm", MAIN, "Gmail"),
        SeedAlias("com.google.android.gm", WORK, "AVS Gmail"),
        SeedAlias("com.google.android.apps.docs", WORK, "AVS Drive"),
        SeedAlias("com.miui.gallery", MAIN, "Xiaomi Gallery"),
        SeedAlias("com.miui.calculator", MAIN, "Xiaomi Calculator"),
        SeedAlias("com.whatsapp", MAIN, "WhatsApp priv", since = 2),
        SeedAlias("com.whatsapp", CLONE, "WhatsApp praca", since = 2),
    )
}
