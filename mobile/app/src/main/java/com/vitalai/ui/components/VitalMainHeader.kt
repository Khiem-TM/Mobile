package com.vitalai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.VitalDisplayFontFamily

@Composable
fun VitalMainHeader(
    title: String,
    modifier: Modifier = Modifier,
    textAlphaProvider: () -> Float = { 1f },
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(com.vitalai.ui.theme.AppSurface)
    ) {
        Spacer(Modifier.statusBarsPadding())
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 21.dp, end = 21.dp, top = 12.dp, bottom = 16.dp)
                .graphicsLayer { alpha = textAlphaProvider() },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Ink900,
                fontSize = 28.sp,
                fontFamily = VitalDisplayFontFamily,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}
