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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun SummaryChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AgentDeskTheme.colors
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)) { append(value) }
        append(" ")
        withStyle(SpanStyle(color = colors.textSecondary)) { append(label) }
    }
    Text(
        text = text,
        modifier = modifier
            .background(colors.row, RoundedCornerShape(50))
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        fontSize = 12.sp,
    )
}
