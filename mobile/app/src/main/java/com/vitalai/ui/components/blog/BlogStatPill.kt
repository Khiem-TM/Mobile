package com.vitalai.ui.components.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius
import com.vitalai.ui.theme.VitalAITheme

@Composable
fun BlogStatPill(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
    inverse: Boolean = false,
    color: Color = Mint500
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(if (inverse) Color.Black.copy(alpha = 0.34f) else AppSurface2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (inverse) Color.White else color, modifier = Modifier.size(13.dp))
        Text(value, color = if (inverse) Color.White else Ink700, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun BlogStatPillPreview() {
    VitalAITheme {
        BlogStatPill(icon = Icons.Default.Visibility, value = "128")
    }
}
