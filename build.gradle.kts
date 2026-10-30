plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 ma wbudowany Kotlin; ta linia tylko podbija wersję KGP na classpath.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
