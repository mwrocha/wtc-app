package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// ── Máscara CPF: 111.111.111-11 ──────────────────────────────────────────────

fun applyCpfMask(cpf: String): String {
    val digits = cpf.filter { it.isDigit() }.take(11)
    return buildString {
        digits.forEachIndexed { i, c ->
            append(c)
            if (i == 2 || i == 5) append('.')
            if (i == 8) append('-')
        }
    }
}

class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(11)
        val masked = applyCpfMask(digits)
        val offsetMap = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformed = 0
                var original = 0
                while (original < offset && transformed < masked.length) {
                    if (masked[transformed].isDigit()) original++
                    transformed++
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                var original = 0
                for (i in 0 until offset.coerceAtMost(masked.length)) {
                    if (masked[i].isDigit()) original++
                }
                return original
            }
        }
        return TransformedText(AnnotatedString(masked), offsetMap)
    }
}

// ── Máscara Telefone: (11) 99999-9999 ────────────────────────────────────────

fun applyPhoneMask(phone: String): String {
    val digits = phone.filter { it.isDigit() }.take(11)
    return buildString {
        digits.forEachIndexed { i, c ->
            when (i) {
                0 -> append("($c")
                1 -> append("$c) ")
                6 -> append("$c-")
                else -> append(c)
            }
        }
    }
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(11)
        val masked = applyPhoneMask(digits)
        val offsetMap = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformed = 0
                var original = 0
                while (original < offset && transformed < masked.length) {
                    if (masked[transformed].isDigit()) original++
                    transformed++
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                var original = 0
                for (i in 0 until offset.coerceAtMost(masked.length)) {
                    if (masked[i].isDigit()) original++
                }
                return original
            }
        }
        return TransformedText(AnnotatedString(masked), offsetMap)
    }
}