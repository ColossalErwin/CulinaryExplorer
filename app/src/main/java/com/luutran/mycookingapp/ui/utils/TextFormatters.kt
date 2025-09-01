package com.luutran.mycookingapp.ui.utils

import android.text.style.URLSpan
import android.util.Log
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

@Composable
fun FormattedClickableHtmlText(htmlContent: String, modifier: Modifier = Modifier) {
    val spanned = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
    val annotatedString = buildAnnotatedString {

        append(spanned.toString())

        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span) {
                is android.text.style.StyleSpan -> {
                    if (span.style == android.graphics.Typeface.BOLD) {
                        addStyle(style = SpanStyle(fontWeight = FontWeight.Bold), start = start, end = end)
                    }
                    if (span.style == android.graphics.Typeface.ITALIC) {
                        addStyle(style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start = start, end = end)
                    }
                }
                is URLSpan -> {
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = start,
                        end = end
                    )
                    addStringAnnotation(
                        tag = "URL",
                        annotation = span.url,
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        Log.w("FormattedHtmlText", "Could not open URI: ${annotation.item}", e)
                    }
                }
        },
        modifier = modifier
    )
}