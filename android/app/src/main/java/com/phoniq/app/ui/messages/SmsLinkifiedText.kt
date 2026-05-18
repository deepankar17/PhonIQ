package com.phoniq.app.ui.messages

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
internal fun SmsLinkifiedText(
    text: String,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
    onLinkOpen: (String) -> Unit,
    onLinkLongPress: (String) -> Unit,
) {
    val annotated =
        remember(text, linkColor) {
            buildSmsUrlAnnotatedString(text, linkColor)
        }
    if (annotated == null) {
        Text(text, style = style.copy(color = color), modifier = modifier)
        return
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        style = style.copy(color = color),
        onTextLayout = { layoutResult = it },
        modifier =
            modifier.pointerInput(annotated, layoutResult) {
                detectTapGestures(
                    onLongPress = { pos ->
                        layoutResult
                            ?.getOffsetForPosition(pos)
                            ?.let { offset ->
                                annotated.getStringAnnotations(SMS_URL_ANNOTATION_TAG, offset, offset).firstOrNull()?.item
                            }
                            ?.let(onLinkLongPress)
                    },
                    onTap = { pos ->
                        layoutResult
                            ?.getOffsetForPosition(pos)
                            ?.let { offset ->
                                annotated.getStringAnnotations(SMS_URL_ANNOTATION_TAG, offset, offset).firstOrNull()?.item
                            }
                            ?.let(onLinkOpen)
                    },
                )
            },
    )
}

private fun buildSmsUrlAnnotatedString(text: String, linkColor: Color): AnnotatedString? {
    val spans = smsUrlSpans(text)
    if (spans.isEmpty()) return null
    return buildAnnotatedString {
        var i = 0
        for ((start, end, url) in spans) {
            if (start < i) continue
            if (start > i) {
                append(text.substring(i, start))
            }
            pushStringAnnotation(tag = SMS_URL_ANNOTATION_TAG, annotation = url)
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                append(text.substring(start, end))
            }
            pop()
            i = end
        }
        if (i < text.length) {
            append(text.substring(i))
        }
    }
}
