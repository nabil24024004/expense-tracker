package com.neosparkx.expensetracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoScaleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    val defaultFontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 14.sp
    var scaledFontSize by remember(text) { mutableStateOf(defaultFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        color = color,
        fontWeight = fontWeight,
        fontSize = scaledFontSize,
        maxLines = maxLines,
        textAlign = textAlign,
        overflow = TextOverflow.Clip,
        style = style,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                if (scaledFontSize.value > 8f) { // don't shrink below 8.sp
                    scaledFontSize = (scaledFontSize.value - 0.5f).sp
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}
