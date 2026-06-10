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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val mono = AgentDeskTheme.typography.monoFamily
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = type, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(text = occurredAt, color = colors.textMuted, fontFamily = mono, fontSize = 11.sp)
        }
        Text(text = detail, color = colors.textSecondary, fontSize = 12.sp)
        Text(text = source, color = colors.textMuted, fontFamily = mono, fontSize = 11.sp)
    }
    if (showDivider) HorizontalDivider(color = colors.rowLine)
}
