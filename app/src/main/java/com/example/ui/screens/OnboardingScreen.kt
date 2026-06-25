package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.MainViewModel
import com.example.ui.components.GlassBox
import com.example.ui.theme.*


@Composable
fun ExploreIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "explore")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = minOf(w, h)
        val centerX = w / 2
        val centerY = h / 2
        

        drawOval(
            color = Color.LightGray.copy(alpha = 0.35f),
            topLeft = Offset(centerX - scale * 0.3f, centerY + scale * 0.25f),
            size = Size(scale * 0.6f, scale * 0.1f)
        )
        


        drawRect(
            color = Color(0xFF1E1E1E),
            topLeft = Offset(centerX - scale * 0.2f, centerY - scale * 0.3f),
            size = Size(scale * 0.06f, scale * 0.55f)
        )

        drawRect(
            color = Color(0xFF1E1E1E),
            topLeft = Offset(centerX + scale * 0.14f, centerY - scale * 0.3f),
            size = Size(scale * 0.06f, scale * 0.55f)
        )

        drawArc(
            color = Color(0xFF1E1E1E),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - scale * 0.2f, centerY - scale * 0.36f),
            size = Size(scale * 0.4f, scale * 0.12f)
        )
        

        val portalPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.14f, centerY + scale * 0.25f)
            lineTo(centerX - scale * 0.14f, centerY - scale * 0.24f)
            quadraticBezierTo(centerX, centerY - scale * 0.3f, centerX + scale * 0.14f, centerY - scale * 0.24f)
            lineTo(centerX + scale * 0.14f, centerY + scale * 0.25f)
            close()
        }
        drawPath(
            path = portalPath,
            color = Color(0xFF5483B3)
        )
        

        drawOval(
            color = Color(0xFF0C0C0C),
            topLeft = Offset(centerX - scale * 0.08f, centerY - scale * 0.1f),
            size = Size(scale * 0.16f, scale * 0.3f)
        )
        

        drawCircle(
            color = Color(0xFF5483B3),
            radius = scale * 0.07f,
            center = Offset(centerX + scale * 0.28f, centerY - scale * 0.4f + (scale * floatOffset))
        )
        

        for (i in 0..2) {
            val stepY = centerY + scale * 0.25f + (i * scale * 0.04f)
            val stepW = scale * 0.26f - (i * scale * 0.03f)
            drawRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(centerX - (stepW / 2), stepY),
                size = Size(stepW, scale * 0.03f)
            )
        }
    }
}


@Composable
fun DiscoveryIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "discovery")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = minOf(w, h)
        val centerX = w / 2
        val centerY = h / 2


        drawOval(
            color = Color.LightGray.copy(alpha = 0.4f),
            topLeft = Offset(centerX - scale * 0.35f, centerY + scale * 0.28f),
            size = Size(scale * 0.7f, scale * 0.1f)
        )


        val platformPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.28f, centerY + scale * 0.2f)
            lineTo(centerX, centerY + scale * 0.1f)
            lineTo(centerX + scale * 0.28f, centerY + scale * 0.2f)
            lineTo(centerX, centerY + scale * 0.3f)
            close()
        }
        drawPath(platformPath, Color(0xFF1E1E1E))


        val platformSidePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.28f, centerY + scale * 0.2f)
            lineTo(centerX, centerY + scale * 0.3f)
            lineTo(centerX + scale * 0.28f, centerY + scale * 0.2f)
            lineTo(centerX + scale * 0.28f, centerY + scale * 0.23f)
            lineTo(centerX, centerY + scale * 0.33f)
            lineTo(centerX - scale * 0.28f, centerY + scale * 0.23f)
            close()
        }
        drawPath(platformSidePath, Color(0xFF0C0C0C))


        val headPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.14f, centerY + scale * 0.09f)
            lineTo(centerX - scale * 0.14f, centerY - scale * 0.09f)
            lineTo(centerX, centerY - scale * 0.16f)
            lineTo(centerX + scale * 0.14f, centerY - scale * 0.09f)
            lineTo(centerX + scale * 0.14f, centerY + scale * 0.09f)
            lineTo(centerX, centerY + scale * 0.16f)
            close()
        }
        drawPath(headPath, Color(0xFF5483B3))
        

        val openingPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.12f, centerY - scale * 0.07f)
            lineTo(centerX, centerY - scale * 0.14f)
            lineTo(centerX + scale * 0.12f, centerY - scale * 0.07f)
            lineTo(centerX, centerY - scale * 0.02f)
            close()
        }
        drawPath(openingPath, Color(0xFF1E1E1E))


        withTransform({
            translate(centerX, centerY - scale * 0.28f + (scale * floatOffset))
            rotate(rotation)
        }) {
            drawRect(
                color = Color(0xFF5483B3),
                topLeft = Offset(-scale * 0.05f, -scale * 0.05f),
                size = Size(scale * 0.1f, scale * 0.1f)
            )
            drawRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(-scale * 0.035f, -scale * 0.035f),
                size = Size(scale * 0.07f, scale * 0.07f)
            )
        }
    }
}


@Composable
fun CreateIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "create")
    val sand1Y by infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sand1"
    )
    val sand2Y by infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sand2"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = minOf(w, h)
        val centerX = w / 2
        val centerY = h / 2


        drawOval(
            color = Color.LightGray.copy(alpha = 0.4f),
            topLeft = Offset(centerX - scale * 0.28f, centerY + scale * 0.32f),
            size = Size(scale * 0.56f, scale * 0.1f)
        )


        val topLid = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.2f, centerY - scale * 0.26f)
            lineTo(centerX, centerY - scale * 0.33f)
            lineTo(centerX + scale * 0.2f, centerY - scale * 0.26f)
            lineTo(centerX, centerY - scale * 0.19f)
            close()
        }
        drawPath(topLid, Color(0xFF1E1E1E))


        val bottomLid = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - scale * 0.2f, centerY + scale * 0.26f)
            lineTo(centerX, centerY + scale * 0.19f)
            lineTo(centerX + scale * 0.2f, centerY + scale * 0.26f)
            lineTo(centerX, centerY + scale * 0.33f)
            close()
        }
        drawPath(bottomLid, Color(0xFF1E1E1E))


        val topGlass = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 0.17f * scale, centerY - 0.22f * scale)
            lineTo(centerX + 0.17f * scale, centerY - 0.22f * scale)
            lineTo(centerX, centerY)
            close()
        }
        drawPath(topGlass, Color.LightGray.copy(alpha = 0.25f))


        val bottomGlass = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 0.17f * scale, centerY + 0.22f * scale)
            lineTo(centerX + 0.17f * scale, centerY + 0.22f * scale)
            lineTo(centerX, centerY)
            close()
        }
        drawPath(bottomGlass, Color.LightGray.copy(alpha = 0.25f))


        val topSand = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 0.11f * scale, centerY - 0.16f * scale)
            lineTo(centerX + 0.11f * scale, centerY - 0.16f * scale)
            lineTo(centerX, centerY)
            close()
        }
        drawPath(topSand, Color(0xFF5483B3))


        val bottomSand = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 0.13f * scale, centerY + 0.18f * scale)
            lineTo(centerX + 0.13f * scale, centerY + 0.18f * scale)
            lineTo(centerX, centerY)
            close()
        }
        drawPath(bottomSand, Color(0xFF5483B3))


        drawCircle(
            color = Color(0xFF5483B3),
            radius = scale * 0.012f,
            center = Offset(centerX, centerY + (scale * sand1Y))
        )
        drawCircle(
            color = Color(0xFF5483B3),
            radius = scale * 0.01f,
            center = Offset(centerX, centerY + (scale * sand2Y))
        )
    }
}

@Composable
fun SocialIconBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), CircleShape)
            .clickable { /* decorative only */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onOnboardingComplete: () -> Unit) {
    var onboardingStep by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("6000") }
    var enableBiometrics by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF021024))
    ) {
        // Fullscreen wave background image
        Image(
            painter = painterResource(id = com.example.R.drawable.login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (onboardingStep < 4) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (Back / Skip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onboardingStep > 1) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .clickable { onboardingStep-- }
                                .padding(8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }

                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .clickable { onboardingStep = 4 }
                            .padding(8.dp)
                    )
                }

                // Illustration + Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (onboardingStep) {
                            1 -> ExploreIllustration(modifier = Modifier.fillMaxSize())
                            2 -> DiscoveryIllustration(modifier = Modifier.fillMaxSize())
                            3 -> CreateIllustration(modifier = Modifier.fillMaxSize())
                        }
                    }

                    val titleText = when (onboardingStep) {
                        1 -> "TRACK"
                        2 -> "MONITOR"
                        else -> "FORECAST"
                    }
                    val descriptionText = when (onboardingStep) {
                        1 -> "Log and map where your money goes. Effortlessly organize expenses into categories like Food, Bills, and Transport."
                        2 -> "Track your spending pace in real-time. Receive warning notifications when approaching or exceeding your budget limit."
                        else -> "Forecast your month-end savings based on current daily rates. Make smarter financial plans for the future."
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Footer (Dot Indicators + Action Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onboardingStep < 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..3) {
                                val isActive = onboardingStep == i
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isActive) 24.dp else 8.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "dotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = dotWidth, height = 8.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) Color.White else Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onboardingStep++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Next",
                                tint = Color(0xFF021024),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { onboardingStep = 4 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF021024)
                            )
                        ) {
                            Text(
                                text = "START TRACKING YOUR LIFE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // Step 4: Redesigned Welcome/Login Screen (matches Mockup)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .clickable { onboardingStep = 3 }
                            .padding(8.dp)
                    )
                }

                // Welcome Header (transparent part)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                    Text(
                        text = "Expense Tracker",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 36.sp,
                            letterSpacing = (-1).sp
                        )
                    )
                }

                // Bottom Login-style Profile Config Sheet Card
                Card(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF021024)
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("What is your name?") },
                            leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = "Name", tint = Color(0xFF052659)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5483B3),
                                unfocusedBorderColor = Color(0xFFC1E8FF),
                                focusedLabelColor = Color(0xFF5483B3),
                                unfocusedLabelColor = Color(0xFF7DA0CA),
                                cursorColor = Color(0xFF5483B3)
                            )
                        )

                        OutlinedTextField(
                            value = budget,
                            onValueChange = { budget = it },
                            label = { Text("Monthly Budget Limit (৳)") },
                            leadingIcon = { Icon(Icons.Rounded.Wallet, contentDescription = "Budget", tint = Color(0xFF052659)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_budget"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5483B3),
                                unfocusedBorderColor = Color(0xFFC1E8FF),
                                focusedLabelColor = Color(0xFF5483B3),
                                unfocusedLabelColor = Color(0xFF7DA0CA),
                                cursorColor = Color(0xFF5483B3)
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { enableBiometrics = !enableBiometrics }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = enableBiometrics,
                                onCheckedChange = { enableBiometrics = it ?: false },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5483B3))
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Enable Biometric Lock",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF021024)
                                    )
                                )
                                Text(
                                    text = "Protect your app access",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF7DA0CA)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val finalName = name.ifEmpty { "User" }
                                val finalBudget = budget.toDoubleOrNull() ?: 6000.0
                                viewModel.completeOnboarding(finalName, finalBudget, enableBiometrics)
                                onOnboardingComplete()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("onboarding_start_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5483B3),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
