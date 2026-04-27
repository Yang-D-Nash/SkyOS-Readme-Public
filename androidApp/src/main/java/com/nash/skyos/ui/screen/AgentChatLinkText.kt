package com.nash.skyos.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

private val agentHttpsUrlRegex = Regex("""https://[\w\-._~:/?#\[\]@!$&'()*+,;=%]+""")

internal fun buildAgentHttpsLinkAnnotatedString(
    text: String,
    baseColor: Color,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    val matches = agentHttpsUrlRegex.findAll(text).toList()
    if (matches.isEmpty()) {
        withStyle(SpanStyle(color = baseColor)) {
            append(text)
        }
        return@buildAnnotatedString
    }
    var index = 0
    for (match in matches) {
        if (match.range.first > index) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(index, match.range.first))
            }
        }
        val urlStr = match.value
        withLink(
            LinkAnnotation.Url(
                url = urlStr,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            ),
        ) {
            append(urlStr)
        }
        index = match.range.last + 1
    }
    if (index < text.length) {
        withStyle(SpanStyle(color = baseColor)) {
            append(text.substring(index))
        }
    }
}

@Composable
internal fun AgentHttpsClickableChatText(
    text: String,
    baseColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = baseColor)),
) {
    val context = LocalContext.current
    val uriHandler = remember(context) {
        object : UriHandler {
            override fun openUri(uri: String) {
                openExternalLink(context, uri)
            }
        }
    }
    val annotated = remember(text, baseColor, linkColor) {
        buildAgentHttpsLinkAnnotatedString(text, baseColor, linkColor)
    }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Text(
            text = annotated,
            style = style,
            modifier = modifier,
        )
    }
}
