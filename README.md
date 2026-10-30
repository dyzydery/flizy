# Flizy

Launcher Androida w stylu Windows Phone 8.1 (Metro). Kotlin + Jetpack Compose, minSdk 31, target/compileSdk 37.

## Budowanie (CLI, bez Android Studio)

Wymagane: JDK 17, Android SDK z `platforms;android-37.0`, `build-tools;36.0.0`, `platform-tools`.

```sh
./gradlew assembleDebug     # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # instalacja przez adb (USB)
```

Na MIUI w Opcjach programisty włącz **Debugowanie USB** oraz **Instaluj przez USB**.

## Struktura

```
app/src/main/java/es/dudek/flizy/
  MainActivity.kt          aktywność HOME, obsługa przycisku Home, RoleManager
  LauncherViewModel.kt     stan: aplikacje, kafelki, ustawienia, akcje
  data/
    AppRepository.kt       LauncherApps + LauncherApps.Callback
    IconLoader.kt          ikony kolorowe i białe (monochromatyczne) z oceną czytelności
    LauncherConfig.kt      model konfiguracji, DataStore + kotlinx.serialization (launcher.json)
  ui/
    LauncherApp.kt         pager Start/Aplikacje, ekrany nakładkowe, menu kontekstowe, Back/Home
    start/StartScreen.kt   siatka kafelków 4 kolumny (średni 2x2, szeroki 4x2), gęste upakowanie
    apps/AppListScreen.kt  lista A-Z z wyszukiwarką i auto-uruchamianiem
    edit/EditTilesScreen.kt  kolejność (przeciąganie/strzałki), rozmiar, odpinanie, dodawanie
    settings/SettingsScreen.kt  akcent, auto-uruchamianie, domyślny launcher
    components/            pochylenie przy wciśnięciu, przyciski, przełącznik, menu
    theme/Metro.kt         paleta akcentów WP 8.1, Selawik
```

Font Selawik © Microsoft, licencja SIL OFL 1.1 (`licenses/Selawik-OFL.txt`).
