package com.shradhaabhishek.weddingtodos.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun WeddingLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "S", 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 24.sp
        )
        Text(
            text = " & ", 
            color = MaterialTheme.colorScheme.secondary, 
            fontWeight = FontWeight.Medium, 
            fontSize = 18.sp
        )
        Text(
            text = "A", 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 24.sp
        )
        Text(
            text = " Itinerary", 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontWeight = FontWeight.Light, 
            fontSize = 20.sp,
            letterSpacing = 1.sp
        )
    }
}
