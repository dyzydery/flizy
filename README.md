# Flizy

Launcher Androida w stylu Windows Phone 8.1 (Metro). Kotlin + Jetpack Compose, minSdk 31, target/compileSdk 35.

## Budowanie (CLI, bez Android Studio)

Środowisko jak w `bahasa-kotak`: JDK 21 z `~/.local/share/jdks/jdk-21` (ustawiony w `gradle.properties`),
Android SDK w `~/Android/Sdk` z `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`.
Gradle 8.14.3, AGP 8.11.1, Kotlin 2.2.0.

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
