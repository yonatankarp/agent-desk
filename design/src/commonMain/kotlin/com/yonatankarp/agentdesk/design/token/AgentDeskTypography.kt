package com.yonatankarp.agentdesk.design.token

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    val display: TextStyle,
    val section: TextStyle,
    val rowTitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
)

@Composable
fun AgentDeskTypography(): AgentDeskTypography {
    val uiFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    val monoFamily = FontFamily(
        Font(Res.font.jetbrainsmono_regular, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_medium, FontWeight.Medium),
    )
    return AgentDeskTypography(
        uiFamily = uiFamily,
        monoFamily = monoFamily,
        display = TextStyle(fontFamily = uiFamily, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        section = TextStyle(fontFamily = uiFamily, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
        rowTitle = TextStyle(fontFamily = uiFamily, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
        body = TextStyle(fontFamily = uiFamily, fontSize = 12.sp, lineHeight = 17.sp),
        label = TextStyle(fontFamily = uiFamily, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
        caption = TextStyle(fontFamily = uiFamily, fontSize = 10.sp, lineHeight = 15.sp),
        mono = TextStyle(fontFamily = monoFamily, fontSize = 10.sp, lineHeight = 15.sp),
    )
}
