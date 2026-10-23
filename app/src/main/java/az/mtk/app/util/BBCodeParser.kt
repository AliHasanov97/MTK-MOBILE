package az.mtk.app.util

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color

/**
 * Parses BBCode to AnnotatedString for Compose Text.
 * Supports: [b], [i], [u], [s], [color], [url], basic newlines.
 */
fun parseBBCode(input: String?): AnnotatedString {
    if (input == null) return AnnotatedString("")

    return buildAnnotatedString {
        var text = input

        // Replace common newline patterns
        text = text.replace("\r\n", "\n")
        text = text.replace("\r", "\n")

        var currentIndex = 0
        val length = text.length
        val styleStack = mutableListOf<BBCodeStyle>()

        while (currentIndex < length) {
            val remaining = text.substring(currentIndex)

            // Check for opening tags
            when {
                remaining.startsWith("[b]", ignoreCase = true) -> {
                    styleStack.add(BBCodeStyle.Bold)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    currentIndex += 3
                }
                remaining.startsWith("[/b]", ignoreCase = true) -> {
                    if (styleStack.removeLastOrNull() == BBCodeStyle.Bold) {
                        pop()
                    }
                    currentIndex += 4
                }
                remaining.startsWith("[i]", ignoreCase = true) -> {
                    styleStack.add(BBCodeStyle.Italic)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    currentIndex += 3
                }
                remaining.startsWith("[/i]", ignoreCase = true) -> {
                    if (styleStack.removeLastOrNull() == BBCodeStyle.Italic) {
                        pop()
                    }
                    currentIndex += 4
                }
                remaining.startsWith("[u]", ignoreCase = true) -> {
                    styleStack.add(BBCodeStyle.Underline)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    currentIndex += 3
                }
                remaining.startsWith("[/u]", ignoreCase = true) -> {
                    if (styleStack.removeLastOrNull() == BBCodeStyle.Underline) {
                        pop()
                    }
                    currentIndex += 4
                }
                remaining.startsWith("[s]", ignoreCase = true) -> {
                    styleStack.add(BBCodeStyle.Strikethrough)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    currentIndex += 3
                }
                remaining.startsWith("[/s]", ignoreCase = true) -> {
                    if (styleStack.removeLastOrNull() == BBCodeStyle.Strikethrough) {
                        pop()
                    }
                    currentIndex += 4
                }
                // [color=#RRGGBB] or [color=name]
                remaining.startsWith("[color=", ignoreCase = true) -> {
                    val endIndex = remaining.indexOf(']', 7)
                    if (endIndex > 7) {
                        val colorValue = remaining.substring(7, endIndex)
                        val color = parseColor(colorValue)
                        if (color != null) {
                            styleStack.add(BBCodeStyle.Color)
                            pushStyle(SpanStyle(color = color))
                        }
                        currentIndex += endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                remaining.startsWith("[/color]", ignoreCase = true) -> {
                    if (styleStack.removeLastOrNull() == BBCodeStyle.Color) {
                        pop()
                    }
                    currentIndex += 8
                }
                // [url=link]text[/url]
                remaining.startsWith("[url=", ignoreCase = true) -> {
                    val endIndex = remaining.indexOf(']', 5)
                    if (endIndex > 5) {
                        val url = remaining.substring(5, endIndex)
                        val closeTagIndex = remaining.indexOf("[/url]", ignoreCase = true)
                        if (closeTagIndex > endIndex) {
                            val linkText = remaining.substring(endIndex + 1, closeTagIndex)
                            pushStyle(SpanStyle(color = Color(0xFF2196F3), textDecoration = TextDecoration.Underline))
                            append(linkText)
                            pop()
                            currentIndex += closeTagIndex + 6
                        } else {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Simple [url]link[/url]
                remaining.startsWith("[url]", ignoreCase = true) -> {
                    val closeTagIndex = remaining.indexOf("[/url]", ignoreCase = true)
                    if (closeTagIndex > 5) {
                        val url = remaining.substring(5, closeTagIndex)
                        pushStyle(SpanStyle(color = Color(0xFF2196F3), textDecoration = TextDecoration.Underline))
                        append(url)
                        pop()
                        currentIndex += closeTagIndex + 6
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                else -> {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
        }
    }
}

private enum class BBCodeStyle {
    Bold, Italic, Underline, Strikethrough, Color
}

private fun parseColor(colorValue: String): Color? {
    return try {
        when {
            colorValue.startsWith("#") -> {
                val hex = colorValue.substring(1)
                when (hex.length) {
                    6 -> Color(android.graphics.Color.parseColor("#FF$hex"))
                    8 -> Color(android.graphics.Color.parseColor("#$hex"))
                    else -> null
                }
            }
            colorValue.equals("red", ignoreCase = true) -> Color.Red
            colorValue.equals("blue", ignoreCase = true) -> Color.Blue
            colorValue.equals("green", ignoreCase = true) -> Color.Green
            colorValue.equals("yellow", ignoreCase = true) -> Color.Yellow
            colorValue.equals("orange", ignoreCase = true) -> Color(0xFFFFA500)
            colorValue.equals("purple", ignoreCase = true) -> Color(0xFF800080)
            colorValue.equals("black", ignoreCase = true) -> Color.Black
            colorValue.equals("white", ignoreCase = true) -> Color.White
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
