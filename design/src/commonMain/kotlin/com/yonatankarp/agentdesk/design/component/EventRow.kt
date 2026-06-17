@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun EventRow(
    type: String,
    occurredAt: String,
    detail: String,
    source: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = AgentDeskTheme.colors
    val spacing = AgentDeskTheme.spacing
    Column(modifier = modifier.fillMaxWidth().padding(vertical = spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = type, color = colors.textPrimary, style = AgentDeskTheme.typography.rowTitle, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(text = occurredAt, color = colors.textMuted, style = AgentDeskTheme.typography.mono)
        }
        Text(text = detail, color = colors.textSecondary, style = AgentDeskTheme.typography.body)
        Text(text = source, color = colors.textMuted, style = AgentDeskTheme.typography.caption)
    }
    if (showDivider) HorizontalDivider(color = colors.rowLine)
}
