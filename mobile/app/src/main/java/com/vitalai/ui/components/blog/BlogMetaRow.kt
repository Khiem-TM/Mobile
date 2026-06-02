package com.vitalai.ui.components.blog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.VitalAITheme

@Composable
fun BlogMetaRow(blog: BlogDto, modifier: Modifier = Modifier, inverse: Boolean = false) {
    Text(
        text = "${blog.displayAuthor} · ${blog.createdAt.take(10)}",
        fontSize = 12.sp,
        color = if (inverse) Color.White.copy(alpha = 0.78f) else Ink500,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun BlogMetaRowPreview() {
    VitalAITheme {
        BlogMetaRow(blog = previewBlogDto())
    }
}
