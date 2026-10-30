package es.dudek.flizy.util

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

/** Małe litery, bez polskich znaków i innych diakrytyków ("Łódź" -> "lodz"). */
fun String.normalizeForSearch(): String {
    val lower = lowercase(Locale.ROOT).replace('ł', 'l')
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
}

fun String.searchWords(): List<String> = split(WORD_SEPARATORS).filter { it.isNotEmpty() }

/**
 * Ocena dopasowania (mniej = lepiej): 0 początek nazwy, 1 początek słowa, 2 zawieranie.
 * null = brak dopasowania. [query] musi być już znormalizowane.
 */
fun matchScore(name: String, words: List<String>, query: String): Int? = when {
    name.startsWith(query) -> 0
    words.any { it.startsWith(query) } -> 1
    name.contains(query) -> 2
    else -> null
}
