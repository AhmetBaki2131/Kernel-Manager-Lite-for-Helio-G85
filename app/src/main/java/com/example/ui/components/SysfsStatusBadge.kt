package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun SysfsStatusBadge(
    isSupported: Boolean,
    supportedText: String = "Supported",
    unsupportedText: String = "Unsupported Kernel Node"
) {
    val bgColor = if (isSupported) ActiveRootGreenBg else WarningTempBg
    val borderColor = if (isSupported) ActiveRootGreenBorder else Color(0xFF482320)
    val textColor = if (isSupported) ActiveRootGreenText else WarningTempText

    Row(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(100.dp))
            .border(BorderStroke(1.dp, borderColor), shape = RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(textColor, shape = CircleShape)
        )
        Text(
            text = if (isSupported) supportedText else unsupportedText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
