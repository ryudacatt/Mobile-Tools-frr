package com.skids.idamobile.disassembly

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Applies deterministic color spans to assembly lines.
 */
object AssemblySyntaxHighlighter {
    fun highlight(line: InstructionLine): AnnotatedString {
        val addressText = "%08x".format(line.address)
        val operands = line.operands
        val commentStart = operands.indexOf(';')
        val opPart = if (commentStart >= 0) operands.substring(0, commentStart) else operands
        val commentPart = if (commentStart >= 0) operands.substring(commentStart) else ""

        return buildAnnotatedString {
            withStyle(SpanStyle(color = AddressColor)) {
                append(addressText)
            }
            append("  ")
            withStyle(SpanStyle(color = MnemonicColor)) {
                append(line.mnemonic)
            }

            if (opPart.isNotBlank()) {
                append(" ")
                appendWithPatterns(this, opPart)
            }
            if (commentPart.isNotBlank()) {
                append(" ")
                withStyle(SpanStyle(color = CommentColor)) {
                    append(commentPart)
                }
            }
        }
    }

    private fun appendWithPatterns(builder: AnnotatedString.Builder, text: String) {
        var index = 0
        while (index < text.length) {
            val registerMatch = RegisterRegex.find(text, index)
            val hexMatch = HexRegex.find(text, index)
            val decMatch = DecimalRegex.find(text, index)

            val next = listOfNotNull(
                registerMatch?.let { StyledTokenMatch(it, TokenType.REGISTER) },
                hexMatch?.let { StyledTokenMatch(it, TokenType.LITERAL) },
                decMatch?.let { StyledTokenMatch(it, TokenType.LITERAL) }
            ).minByOrNull { it.match.range.first }

            if (next == null) {
                builder.append(text.substring(index))
                break
            }

            if (next.match.range.first > index) {
                builder.append(text.substring(index, next.match.range.first))
            }

            val style = when (next.type) {
                TokenType.REGISTER -> SpanStyle(color = RegisterColor)
                TokenType.LITERAL -> SpanStyle(color = LiteralColor)
            }

            builder.withStyle(style) {
                append(next.match.value)
            }
            index = next.match.range.last + 1
        }
    }

    private val RegisterRegex = Regex("""\b[vp]\d+\b""")
    private val HexRegex = Regex("""(?<![A-Za-z0-9_])0x[0-9a-fA-F]+\b""")
    private val DecimalRegex = Regex("""(?<![A-Za-z0-9_])-?\d+\b""")

    private data class StyledTokenMatch(
        val match: MatchResult,
        val type: TokenType
    )

    private enum class TokenType {
        REGISTER,
        LITERAL
    }

    private val AddressColor = Color(0xFF7F8C8D)
    private val MnemonicColor = Color(0xFF4FC3F7)
    private val RegisterColor = Color(0xFFFDD835)
    private val LiteralColor = Color(0xFFFFA726)
    private val CommentColor = Color(0xFF81C784)
}
