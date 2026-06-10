@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun EvidenceItem(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AgentDeskTheme.colors
    val mono = AgentDeskTheme.typography.monoFamily
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(colors.accent))
        Text(text = label, color = colors.textSecondary, fontFamily = mono, fontSize = 12.sp)
    }
}
