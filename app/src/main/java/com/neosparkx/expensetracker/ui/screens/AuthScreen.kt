package com.neosparkx.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.neosparkx.expensetracker.auth.BiometricHelper
import com.neosparkx.expensetracker.ui.components.GlassBox
import com.neosparkx.expensetracker.ui.theme.*

@Composable
fun AuthScreen(activity: FragmentActivity, onAuthenticated: () -> Unit) {
    var errorMsg by remember { mutableStateOf("") }
    var hardwareSupported by remember { mutableStateOf(true) }
    

    LaunchedEffect(Unit) {
        if (BiometricHelper.canAuthenticate(activity)) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = onAuthenticated,
                onError = { errorMsg = it }
            )
        } else {
            hardwareSupported = false
            errorMsg = "Biometric hardware not set up or not supported."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(85.dp)
        ) {
            drawCircle(Color(0xFF020203).copy(alpha = 0.08f), radius = size.width * 0.7f, center = Offset(size.width * 0.2f, size.height * 0.1f))
            drawCircle(Color(0xFFEA3B35).copy(alpha = 0.06f), radius = size.width * 0.8f, center = Offset(size.width * 0.8f, size.height * 0.8f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    GlassBox(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Text(
                        text = "Authentication Required",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (errorMsg.isNotEmpty()) errorMsg else "Please scan your fingerprint to enter the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMsg.isNotEmpty() && hardwareSupported) Color(0xFFEA3B35) else TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(36.dp))
                    
                    if (hardwareSupported) {
                        Button(
                            onClick = {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    onSuccess = onAuthenticated,
                                    onError = { errorMsg = it }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("unlock_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = "Fingerprint")
                                Text("Scan Fingerprint", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    } else {

                        Button(
                            onClick = onAuthenticated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("unlock_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Proceed to App", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}


