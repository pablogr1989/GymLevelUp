package com.gymlog.app.util

object InputValidator {
    fun isValidInt(value: String, range: IntRange? = null, maxLength: Int? = null): Boolean {
        if (value.isEmpty()) return true // Permitir vacío para borrado
        if (maxLength != null && value.length > maxLength) return false
        if (value.any { !it.isDigit() }) return false

        val intVal = value.toIntOrNull() ?: return false
        return range?.contains(intVal) ?: true
    }

    fun isValidFloat(value: String): Boolean {
        if (value.isEmpty()) return true
        // Regex para decimales positivos
        return value.matches(Regex("^\\d*\\.?\\d*$")) && value != "."
    }

    // Funciones de extensión para uso más fluido
    fun String.validateInt(range: IntRange? = null, maxLength: Int? = null): Boolean =
        isValidInt(this, range, maxLength)

    fun String.validateFloat(): Boolean = isValidFloat(this)
}