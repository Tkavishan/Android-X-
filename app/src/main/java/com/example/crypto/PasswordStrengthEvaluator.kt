package com.example.crypto

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultGoldLight
import com.example.ui.theme.VaultSuccess
import com.example.ui.theme.VaultWarning

enum class PasswordStrengthLevel(val label: String, val color: Color, val fraction: Float) {
    EMPTY("Empty", Color.Gray, 0f),
    WEAK("Weak", VaultError, 0.25f),
    FAIR("Fair", VaultWarning, 0.5f),
    GOOD("Good", VaultGold, 0.75f),
    STRONG("Strong", VaultGoldLight, 0.9f),
    EXCELLENT("Military Grade", VaultSuccess, 1f)
}

data class PasswordEvaluation(
    val level: PasswordStrengthLevel,
    val score: Int, // 0..4
    val hints: List<String>,
    val hasMinLength: Boolean,
    val hasUpper: Boolean,
    val hasLower: Boolean,
    val hasDigit: Boolean,
    val hasSymbol: Boolean
)

object PasswordStrengthEvaluator {
    fun evaluate(password: CharSequence): PasswordEvaluation {
        if (password.isEmpty()) {
            return PasswordEvaluation(
                level = PasswordStrengthLevel.EMPTY,
                score = 0,
                hints = listOf("Enter a strong master password"),
                hasMinLength = false,
                hasUpper = false,
                hasLower = false,
                hasDigit = false,
                hasSymbol = false
            )
        }

        val length = password.length
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (i in 0 until length) {
            val c = password[i]
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        val hints = mutableListOf<String>()
        if (length < 10) hints.add("Use at least 10 characters")
        if (!hasUpper) hints.add("Add uppercase letters (A-Z)")
        if (!hasLower) hints.add("Add lowercase letters (a-z)")
        if (!hasDigit) hints.add("Add numeric digits (0-9)")
        if (!hasSymbol) hints.add("Add special symbols (!@#$%...)")

        var score = 0
        if (length >= 8) score++
        if (length >= 12) score++
        if (length >= 16) score++
        val varietyCount = listOf(hasUpper, hasLower, hasDigit, hasSymbol).count { it }
        if (varietyCount >= 3) score++
        if (varietyCount == 4) score++

        val level = when {
            length < 6 -> PasswordStrengthLevel.WEAK
            score <= 1 -> PasswordStrengthLevel.WEAK
            score == 2 -> PasswordStrengthLevel.FAIR
            score == 3 -> PasswordStrengthLevel.GOOD
            score == 4 -> PasswordStrengthLevel.STRONG
            else -> PasswordStrengthLevel.EXCELLENT
        }

        return PasswordEvaluation(
            level = level,
            score = (score.coerceIn(0, 4)),
            hints = hints,
            hasMinLength = length >= 8,
            hasUpper = hasUpper,
            hasLower = hasLower,
            hasDigit = hasDigit,
            hasSymbol = hasSymbol
        )
    }
}
