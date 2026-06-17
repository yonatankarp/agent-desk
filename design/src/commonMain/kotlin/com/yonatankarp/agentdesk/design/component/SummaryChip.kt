@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun SummaryChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AgentDeskTheme.colors
    val spacing = AgentDeskTheme.spacing
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)) { append(value) }
        append(" ")
        withStyle(SpanStyle(color = colors.textSecondary)) { append(label) }
    }
    Text(
        text = text,
        modifier = modifier
            .background(colors.row, RoundedCornerShape(50))
            .border(spacing.lineWidth, colors.line, RoundedCornerShape(50))
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        style = AgentDeskTheme.typography.label,
    )
}
