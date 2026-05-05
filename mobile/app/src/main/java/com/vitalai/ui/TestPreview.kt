package com.vitalai.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SimpleText() {
    Text(text = "Hello VitalAI")
}

@Preview(showBackground = true)
@Composable
fun SimpleTextPreview() {
    SimpleText()
}
