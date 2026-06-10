package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.yonatankarp.agentdesk.design.resources.Res
import com.yonatankarp.agentdesk.design.resources.inter_bold
import com.yonatankarp.agentdesk.design.resources.inter_medium
import com.yonatankarp.agentdesk.design.resources.inter_regular
import com.yonatankarp.agentdesk.design.resources.inter_semibold
import com.yonatankarp.agentdesk.design.resources.jetbrainsmono_medium
import com.yonatankarp.agentdesk.design.resources.jetbrainsmono_regular
import org.jetbrains.compose.resources.Font

class AgentDeskTypography(
    val uiFamily: FontFamily,
    val monoFamily: FontFamily,
)

@Composable
fun AgentDeskTypography(): AgentDeskTypography = AgentDeskTypography(
    uiFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    ),
    monoFamily = FontFamily(
        Font(Res.font.jetbrainsmono_regular, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_medium, FontWeight.Medium),
    ),
)
