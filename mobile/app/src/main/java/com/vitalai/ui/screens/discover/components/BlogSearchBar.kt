package com.vitalai.ui.screens.discover.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.VitalAITheme
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.VitalRadius

@Composable
fun BlogSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        placeholder = { Text("Search...", fontSize = 13.sp, color = Ink400) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = Ink400, modifier = Modifier.size(20.dp))
        },
        singleLine = true,
        shape = RoundedCornerShape(VitalRadius.Pill),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppSurface2,
            unfocusedContainerColor = AppSurface2,
            disabledContainerColor = AppSurface2,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            disabledTextColor = Ink700,
            disabledPlaceholderColor = Ink400,
            disabledLeadingIconColor = Ink400
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun BlogSearchBarPreview() {
    VitalAITheme {
        BlogSearchBar(value = "", onValueChange = {})
    }
}
