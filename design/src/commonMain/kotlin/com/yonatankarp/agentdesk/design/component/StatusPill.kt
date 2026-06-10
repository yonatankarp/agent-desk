@file:Suppress("FunctionName")

package com.yonatankarp.agentdesk.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.design.theme.AgentDeskTheme

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val role = AgentDeskTheme.statusRole(tone)
    Text(
        text = label,
        modifier = modifier
            .background(role.pillBg, RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 3.dp),
        color = role.text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}
