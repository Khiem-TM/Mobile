package com.vitalai.ui.screens.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalRadius
import com.vitalai.ui.theme.VitalAITheme

@Composable
fun BlogCover(
    blog: BlogDto,
    modifier: Modifier,
    radius: Dp = VitalRadius.Lg,
    fallbackGradient: Boolean = false
) {
    Box(modifier = modifier.clip(RoundedCornerShape(radius))) {
        if (blog.thumbnailImage != null) {
            AsyncImage(
                model = blog.thumbnailImage,
                contentDescription = blog.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (fallbackGradient) {
                            Brush.linearGradient(listOf(Mint100, Mint500))
                        } else {
                            Brush.linearGradient(listOf(Mint100, Mint100))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = Mint600, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlogCoverPreview() {
    VitalAITheme {
        BlogCover(
            blog = previewBlogDto(),
            modifier = Modifier.size(width = 220.dp, height = 132.dp),
            fallbackGradient = true
        )
    }
}
