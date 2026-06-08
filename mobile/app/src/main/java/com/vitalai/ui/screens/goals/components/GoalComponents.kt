package com.vitalai.ui.screens.goals.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Ink500,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun GoalField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
fun MacroSliderField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxValue: Float
) {
    val floatValue = value.toFloatOrNull() ?: 0f
    
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Ink500, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("${floatValue.toInt()}g", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = floatValue,
            onValueChange = { onValueChange(it.toInt().toString()) },
            valueRange = 0f..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = Mint500,
                activeTrackColor = Mint500,
                inactiveTrackColor = Mint100
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}
