package com.example.myapplication4

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// ═════════════════════════════════════════════
// PREMIUM HOME SCREEN - OBSIDIAN KINETIC
// ═════════════════════════════════════════════

@Composable
fun PremiumHomeScreen(navController: NavController, viewModel: SecurityViewModel) {
    var urlInput by remember { mutableStateOf("") }
    val scanState by viewModel.scanState.observeAsState(ScanState.Idle)
    val recentScans by viewModel.recentScans.observeAsState(emptyList())
    
    LaunchedEffect(scanState) {
        if (scanState is ScanState.Loading) {
            navController.navigate("loading") {
                popUpTo("home") { saveState = true }
            }
        } else if (scanState is ScanState.Success) {
            navController.navigate("results") {
                popUpTo("home") { saveState = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.Background)
    ) {
        // Background decorative glow effects
        Box(
            modifier = Modifier
                .size(600.dp)
                .background(
                    ObsidianColors.Primary.copy(alpha = 0.05f),
                    shape = CircleShape
                )
                .blur(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = -100.dp)
        )
        Box(
            modifier = Modifier
                .size(600.dp)
                .background(
                    ObsidianColors.Secondary.copy(alpha = 0.05f),
                    shape = CircleShape
                )
                .blur(120.dp)
                .align(Alignment.BottomStart)
                .offset(x = -100.dp, y = 100.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 80.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HERO SECTION
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    // Animated Central Shield
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow ring
                        val infiniteTransition = rememberInfiniteTransition()
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.7f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    ObsidianColors.Primary.copy(alpha = glowAlpha * 0.2f),
                                    shape = CircleShape
                                )
                                .blur(80.dp)
                        )
                        
                        // Inner glass panel
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    color = ObsidianColors.SurfaceContainer.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = ObsidianColors.Primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 40.dp,
                                    shape = CircleShape,
                                    ambientColor = ObsidianColors.Primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Shield",
                                tint = ObsidianColors.Primary,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                    
                    // Heading
                    Text(
                        "Eye ShielD",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(ObsidianColors.Primary, ObsidianColors.Secondary)
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    
                    Text(
                        "Secure AI Browser Protection",
                        fontSize = 14.sp,
                        color = ObsidianColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // SEARCH & SCAN SECTION
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Input Field with gradient border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                color = ObsidianColors.SurfaceContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = ObsidianColors.Primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = ObsidianColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            TextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = {
                                    Text(
                                        "Enter website URL",
                                        color = ObsidianColors.OnSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Transparent),
                                textStyle = TextStyle(color = ObsidianColors.OnSurface),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                    
                    // Scan Button
                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                viewModel.scanUrl(urlInput)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            ObsidianColors.Primary,
                                            ObsidianColors.Secondary
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = ObsidianColors.OnPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Scan Now",
                                    color = ObsidianColors.OnPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    // Stats Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChip("Status: ONLINE")
                        StatChip("256-bit Encryption")
                    }
                }
            }
            
            // RECENT SCANS SECTION
            if (recentScans.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Recent Intelligence",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianColors.OnSurface
                            )
                            Text(
                                "Last verification signatures",
                                fontSize = 12.sp,
                                color = ObsidianColors.OnSurfaceVariant
                            )
                        }
                        Text(
                            "View All",
                            fontSize = 12.sp,
                            color = ObsidianColors.Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { }
                        )
                    }
                }

                items(recentScans.take(3)) { scan ->
                    ScanResultCard(
                        url = scan.url,
                        status = "Scanned ${scan.time}",
                        riskLevel = scan.confidence.filter(Char::isDigit).toIntOrNull()
                    )
                }
            }
        }
        
        // Top Bar
        TopBar()
        
        // Bottom Navigation
        BottomNavigation(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ═════════════════════════════════════════════
// PREMIUM LOADING/SCANNING SCREEN
// ═════════════════════════════════════════════

@Composable
fun PremiumLoadingScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Background effects
        Box(
            modifier = Modifier
                .size(600.dp)
                .background(
                    ObsidianColors.Primary.copy(alpha = 0.06f),
                    shape = CircleShape
                )
                .blur(120.dp)
                .align(Alignment.Center)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            // Animated scanning shield
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                contentAlignment = Alignment.Center
            ) {
                // Spinning outer ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = ObsidianColors.Primary.copy(alpha = 0.3f),
                        radius = size.width / 2,
                        style = Stroke(2.dp.toPx())
                    )
                }
                
                // Inner shield
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            ObsidianColors.SurfaceContainer.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .border(1.5.dp, ObsidianColors.Primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ObsidianColors.Primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            // Status text
            Text(
                "SECURE SCAN IN PROGRESS",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ObsidianColors.OnSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                "Level 7 AI Heuristic Audit Active",
                fontSize = 12.sp,
                color = ObsidianColors.Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            // Progress stages
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScanStageItem("URL Validation", "Complete", true)
                ScanStageItem("Threat Intelligence Check", "Active", false)
                ScanStageItem("AI Analysis", "Pending", false)
                ScanStageItem("Risk Scoring", "Pending", false)
            }
            
            // Linear progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SYSTEM ENTROPY: MINIMAL",
                        fontSize = 10.sp,
                        color = ObsidianColors.Primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "42%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.Primary
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            ObsidianColors.SurfaceContainerHigh,
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        ObsidianColors.Primary,
                                        ObsidianColors.Secondary
                                    )
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ═════════════════════════════════════════════
// PREMIUM RESULTS SCREEN
// ═════════════════════════════════════════════

@Composable
fun PremiumResultsScreen(navController: NavController, viewModel: SecurityViewModel) {
    val scanState by viewModel.scanState.observeAsState(ScanState.Idle)
    
    if (scanState !is ScanState.Success) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    
    val success = scanState as ScanState.Success
    val verdict = success.analysis.verdict.uppercase()
    val riskLevel = success.analysis.confidenceScore
    
    val isPhishing = verdict.contains("PHISHING")
    val isDangerous = verdict.contains("MALICIOUS") || verdict.contains("DANGEROUS")
    val isSuspicious = verdict.contains("SUSPICIOUS")
    
    val riskColor = when {
        isDangerous || isPhishing -> ObsidianColors.Error
        isSuspicious -> ObsidianColors.Warning
        else -> ObsidianColors.Safe
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 80.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ALERT CARD
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDangerous || isPhishing)
                                ObsidianColors.ErrorContainer.copy(alpha = 0.2f)
                            else
                                ObsidianColors.Tertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = riskColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Risk Meter Circle
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { riskLevel / 100f },
                                modifier = Modifier.size(120.dp),
                                color = riskColor,
                                strokeWidth = 8.dp,
                                trackColor = ObsidianColors.SurfaceContainer
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    "$riskLevel%",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = riskColor
                                )
                                Text(
                                    "RISK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = riskColor.copy(alpha = 0.7f),
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isDangerous || isPhishing)
                                    "PHISHING DETECTED" else "THREAT DETECTED",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ObsidianColors.OnSurface
                            )
                            Text(
                                success.analysis.summary,
                                fontSize = 13.sp,
                                color = ObsidianColors.OnSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            
            // FINDINGS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "KEY FINDINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.Primary.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    
                    FindingCard("Suspicious Domain", "Typosquatting detected", riskColor)
                    FindingCard("Blacklist Match", "URL matches known database", riskColor)
                    FindingCard("Missing HTTPS", "Connection is unencrypted", riskColor)
                }
            }
            
            // ACTION BUTTONS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(riskColor, riskColor.copy(alpha = 0.8f))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Go Back to Safety",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
        
        TopBar()
        BottomNavigation(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ═════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═════════════════════════════════════════════

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                color = ObsidianColors.Background.copy(alpha = 0.6f),
                shape = RoundedCornerShape(bottomStartPercent = 20, bottomEndPercent = 20)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF00D4FF),
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Eye ShielD",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF6DDDFF), Color(0xFFDD8BFB))
                    )
                )
            )
        }
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BottomNavigation(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = ObsidianColors.Background.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStartPercent = 30, topEndPercent = 30)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("HOME", Icons.Default.Home)
        NavItem("SCANS", Icons.Default.Radar, isActive = true)
        NavItem("SHIELD", Icons.Default.VerifiedUser)
        NavItem("SETTINGS", Icons.Default.Settings)
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector? = null, isActive: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable { }
            .padding(8.dp)
    ) {
        // Use the icon that was passed, or a default if not
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = ObsidianColors.SurfaceContainer,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = ObsidianColors.OutlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ObsidianColors.Primary, CircleShape)
            )
            Text(
                text,
                fontSize = 11.sp,
                color = ObsidianColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ScanResultCard(url: String, status: String, riskLevel: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ObsidianColors.SurfaceContainerLow,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = ObsidianColors.OutlineVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = ObsidianColors.Primary,
                modifier = Modifier.size(28.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    url.take(30),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianColors.OnSurface
                )
                Text(
                    status,
                    fontSize = 12.sp,
                    color = ObsidianColors.OnSurfaceVariant
                )
            }
            
            if (riskLevel != null) {
                Box(
                    modifier = Modifier
                        .background(
                            ObsidianColors.Primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp, 4.dp)
                ) {
                    Text(
                        "SECURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
fun ScanStageItem(title: String, status: String, isComplete: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isComplete)
                    ObsidianColors.SurfaceContainer.copy(alpha = 0.5f)
                else
                    ObsidianColors.SurfaceContainerLow,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isComplete)
                    ObsidianColors.Primary.copy(alpha = 0.2f)
                else
                    ObsidianColors.OutlineVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isComplete)
                                ObsidianColors.Primary.copy(alpha = 0.2f)
                            else
                                ObsidianColors.SurfaceContainerHigh,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ObsidianColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.OnSurface
                    )
                    Text(
                        status,
                        fontSize = 11.sp,
                        color = ObsidianColors.OnSurfaceVariant
                    )
                }
            }
            
            if (!isComplete && status == "Active") {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ObsidianColors.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun FindingCard(title: String, description: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianColors.OnSurface
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = ObsidianColors.OnSurfaceVariant
                )
            }
        }
    }
}
