@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val role = AgentDeskTheme.statusRole(tone)
    val spacing = AgentDeskTheme.spacing
    Row(
        modifier = modifier
            .background(role.pillBg, RoundedCornerShape(50))
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            Modifier
                .size(spacing.xs)
                .clip(RoundedCornerShape(50))
                .background(role.rail),
        )
        Text(
            text = label,
            color = role.text,
            style = AgentDeskTheme.typography.label,
            fontWeight = FontWeight.Medium,
        )
    }
}
