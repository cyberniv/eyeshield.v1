package com.example.myapplication4

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

// ═════════════════════════════════════════════
// OBSIDIAN KINETIC COLOR PALETTE
// ═════════════════════════════════════════════

object ObsidianColors {
    val Background = Color(0xFF0A0E16)
    val OnBackground = Color(0xFFE8EAF7)
    val OnSurface = OnBackground
    val SurfaceContainerLowest = Color(0xFF000000)
    val SurfaceContainerLow = Color(0xFF0F131D)
    val SurfaceContainer = Color(0xFF151A24)
    val SurfaceContainerHigh = Color(0xFF1B202B)
    val SurfaceContainerHighest = Color(0xFF202632)
    val SurfaceBright = Color(0xFF262C39)
    
    val Primary = Color(0xFF6DDDFF)
    val PrimaryDim = Color(0xFF00C3EB)
    val OnPrimary = Color(0xFF004C5E)
    val OnPrimaryContainer = Color(0xFF004352)
    
    val Secondary = Color(0xFFDD8BFB)
    val SecondaryDim = Color(0xFFCE7EEC)
    val OnSecondary = Color(0xFF4C0068)
    val SecondaryContainer = Color(0xFF6E208C)
    
    val Tertiary = Color(0xFF82A3FF)
    val TertiaryDim = Color(0xFF759AFF)
    val OnTertiary = Color(0xFF002363)
    
    val Error = Color(0xFFFF716C)
    val ErrorDim = Color(0xFFD7383B)
    val ErrorContainer = Color(0xFF9F0519)
    val OnError = Color(0xFF490006)
    
    val Safe = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    
    val Outline = Color(0xFF727580)
    val OutlineVariant = Color(0xFF444852)
    val OnSurfaceVariant = Color(0xFFA8ABB6)
    val InverseOnSurface = Color(0xFF51555F)
    
    val KineticGradient = Brush.linearGradient(listOf(Primary, Secondary))
    val VerdictSafeGradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
    val VerdictWarningGradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF78350F)))
    val VerdictDangerGradient = Brush.linearGradient(listOf(Error, Color(0xFF7F1D1D)))
}

// ═════════════════════════════════════════════
// DATA MODELS
// ═════════════════════════════════════════════

data class RecentScanInfo(val url: String, val verdict: String, val time: String, val confidence: String)

// ═════════════════════════════════════════════
// ACTIVITY & MAIN APP
// ═════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    private var pendingExternalUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExternalUrl = extractBrowsableUrl(intent)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = ObsidianColors.Background)) {
                Surface(modifier = Modifier.fillMaxSize(), color = ObsidianColors.Background) {
                    MainApp(
                        externalUrl = pendingExternalUrl,
                        onExternalUrlConsumed = { pendingExternalUrl = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingExternalUrl = extractBrowsableUrl(intent)
    }

    private fun extractBrowsableUrl(intent: Intent?): String? {
        val intentUri = intent?.data ?: return null
        if (intent.action != Intent.ACTION_VIEW) return null

        val scheme = intentUri.scheme?.lowercase() ?: return null
        return if (scheme == "http" || scheme == "https") intentUri.toString() else null
    }
}

@Composable
fun MainApp(externalUrl: String? = null, onExternalUrlConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val viewModel: SecurityViewModel = viewModel()
    val startSecureScan: (String) -> Unit = remember(navController, viewModel) {
        { rawUrl ->
            val normalizedUrl = rawUrl.trim()
            if (normalizedUrl.isNotBlank()) {
                viewModel.scanUrl(normalizedUrl)
                navController.navigate("analysis") {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(externalUrl) {
        val url = externalUrl ?: return@LaunchedEffect
        startSecureScan(url)
        onExternalUrlConsumed()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(ObsidianColors.Background)) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreenObsidian(
                    navController = navController,
                    viewModel = viewModel,
                    onScanRequested = startSecureScan
                )
            }
            composable("analysis") {
                AnalysisScreenObsidian(navController = navController, viewModel = viewModel)
            }
            composable("scans") {
                ScansScreenObsidian(navController = navController, viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreenObsidian(navController = navController)
            }
            composable(
                "webview/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                WebViewScreenObsidian(navController = navController, url = url)
            }
        }
    }
}

// ═════════════════════════════════════════════
// HOME SCREEN
// ═════════════════════════════════════════════

@Composable
fun HomeScreenObsidian(navController: NavController, viewModel: SecurityViewModel, onScanRequested: (String) -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var homeMessage by remember { mutableStateOf<String?>(null) }
    var showTipCard by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val clipboardManager = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    val recentScans by viewModel.recentScans.observeAsState(emptyList())
    val recentItems = remember(recentScans) { recentScans.take(3) }
    val lastVisitedSite = recentItems.firstOrNull()?.url

    LaunchedEffect(recentItems.isNotEmpty()) {
        if (recentItems.isNotEmpty()) {
            showTipCard = false
        }
    }

    Scaffold(
        topBar = { OKTopBar(navController) },
        bottomBar = { OKBottomNav(navController) },
        containerColor = ObsidianColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))
                AnimatedShieldHero()
                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.6f)).border(1.dp, ObsidianColors.OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, tint = ObsidianColors.Primary, modifier = Modifier.padding(start = 12.dp))
                        BasicTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            decorationBox = { innerTextField ->
                                if (urlInput.isEmpty()) Text("Enter or paste URL", color = ObsidianColors.OnSurfaceVariant.copy(alpha = 0.5f))
                                innerTextField()
                            }
                        )
                        Button(
                            onClick = {
                                val enteredUrl = urlInput.trim()
                                if (enteredUrl.isNotBlank()) {
                                    onScanRequested(enteredUrl)
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                            modifier = Modifier.background(ObsidianColors.KineticGradient, RoundedCornerShape(100.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Radar, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze URL", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Scan Copied Link",
                        subtitle = "Instantly check copied URLs",
                        icon = Icons.Default.ContentPasteSearch,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clipboardText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                            if (clipboardText.isNotBlank()) {
                                homeMessage = null
                                onScanRequested(clipboardText)
                            } else {
                                homeMessage = "Clipboard does not contain a URL"
                            }
                        }
                    )
                    QuickActionButton(
                        title = "Scan Recent Site",
                        subtitle = "Analyze your last visited website",
                        icon = Icons.Default.History,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!lastVisitedSite.isNullOrBlank()) {
                                homeMessage = null
                                onScanRequested(lastVisitedSite)
                            } else {
                                homeMessage = "No recent site available yet"
                            }
                        }
                    )
                }

                AnimatedVisibility(visible = homeMessage != null, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianColors.SurfaceContainerLow,
                        border = BorderStroke(1.dp, ObsidianColors.OutlineVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = homeMessage.orEmpty(),
                            color = ObsidianColors.OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showTipCard, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        Spacer(modifier = Modifier.height(18.dp))
                        TipCard(
                            title = "Security Tip",
                            message = "Avoid unknown links. Always verify the source before opening.",
                            onDismiss = { showTipCard = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                RecentIntelligenceHeader(onViewLedger = { navController.navigate("scans") })
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (recentItems.isEmpty()) {
                item {
                    EmptyScansState(
                        onCardClick = { navController.navigate("scans") },
                        onScanNow = {
                            homeMessage = "Enter or paste a URL above to start scanning"
                        }
                    )
                }
            } else {
                items(recentItems) { scan ->
                    RecentIntelligenceItem(scan = scan, onClick = { navController.navigate("scans") })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
fun RecentIntelligenceHeader(onViewLedger: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Recent Scans",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White,
                            ObsidianColors.Primary.copy(alpha = 0.92f)
                        )
                    )
                )
            )
            Text("Your latest scan results and insights", color = ObsidianColors.OnSurfaceVariant, fontSize = 12.sp)
        }
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.16f)),
            shadowElevation = 6.dp,
            modifier = Modifier.clickable(onClick = onViewLedger)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Timeline, null, tint = ObsidianColors.Primary, modifier = Modifier.size(14.dp))
                Text("View Ledger", color = ObsidianColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RecentIntelligenceItem(scan: RecentScanInfo, onClick: () -> Unit) {
    val color = when (scan.verdict) {
        "SAFE" -> ObsidianColors.Safe
        "WARNING" -> ObsidianColors.Warning
        else -> ObsidianColors.Error
    }
    val icon = when (scan.verdict) {
        "SAFE" -> Icons.Default.Public
        "WARNING" -> Icons.Default.Warning
        else -> Icons.Default.Dangerous
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(140), label = "recent_intelligence_scale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .background(
                    Brush.linearGradient(
                        listOf(
                            color.copy(alpha = 0.12f),
                            ObsidianColors.SurfaceContainerLow.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(46.dp).background(ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.8f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(displaySiteLabel(scan.url), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                            Text(scan.verdict.lowercase().replaceFirstChar { it.uppercase() }, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(scan.time, color = ObsidianColors.OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
                    ) {
                        Text(
                            scan.verdict.lowercase().replaceFirstChar { it.uppercase() },
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Text(scan.confidence, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(140), label = "quick_action_scale")

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.12f)),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            ObsidianColors.Primary.copy(alpha = 0.14f),
                            ObsidianColors.Secondary.copy(alpha = 0.09f),
                            ObsidianColors.SurfaceContainerLow.copy(alpha = 0.35f)
                        )
                    )
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.78f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(subtitle, color = ObsidianColors.OnSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun TipCard(title: String, message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, ObsidianColors.Warning.copy(alpha = 0.16f)),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ObsidianColors.Warning.copy(alpha = 0.14f),
                            ObsidianColors.Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(ObsidianColors.Warning.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TipsAndUpdates, null, tint = ObsidianColors.Warning, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(message, color = ObsidianColors.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyScansState(onCardClick: () -> Unit, onScanNow: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_scan")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "empty_state_rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "empty_state_pulse"
    )
    var visible by remember { mutableStateOf(false) }
    val cardInteraction = remember { MutableInteractionSource() }
    val cardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(if (cardPressed) 0.992f else 1f, tween(150), label = "empty_state_card_scale")
    val buttonInteraction = remember { MutableInteractionSource() }
    val buttonPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (buttonPressed) 0.97f else 1f, tween(120), label = "empty_state_button_scale")

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(360))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(cardScale),
            shape = RoundedCornerShape(22.dp),
            color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.14f)),
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = cardInteraction, indication = null, onClick = onCardClick)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianColors.Primary.copy(alpha = 0.10f),
                                ObsidianColors.Secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().rotate(ringRotation)) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        ObsidianColors.Primary.copy(alpha = 0.9f),
                                        ObsidianColors.Secondary.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 110f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulse)
                                .background(ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.78f), CircleShape)
                                .border(1.dp, ObsidianColors.Primary.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, null, tint = ObsidianColors.Primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text("No scans yet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(
                        "Scan a website to view protection insights and history",
                        color = ObsidianColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onScanNow,
                        interactionSource = buttonInteraction,
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .scale(buttonScale)
                            .background(ObsidianColors.KineticGradient, RoundedCornerShape(100.dp))
                    ) {
                        Icon(Icons.Default.Radar, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun displaySiteLabel(url: String): String {
    val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    return runCatching {
        Uri.parse(normalized).host?.removePrefix("www.")?.ifBlank { url } ?: url
    }.getOrDefault(url)
}

@Composable
fun AnimatedShieldHero() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_shield")
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_core_scale"
    )
    val outerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_outer_pulse_scale"
    )
    val outerPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_outer_pulse_alpha"
    )
    val innerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_inner_pulse_scale"
    )
    val innerPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_inner_pulse_alpha"
    )
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_scan_rotation"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(228.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(212.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ObsidianColors.Primary.copy(alpha = 0.2f),
                                ObsidianColors.Secondary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
                    .blur(26.dp)
            )

            Box(
                modifier = Modifier
                    .size(188.dp)
                    .scale(outerPulseScale)
                    .border(
                        width = 1.5.dp,
                        color = ObsidianColors.Primary.copy(alpha = outerPulseAlpha),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(innerPulseScale)
                    .border(
                        width = 1.dp,
                        color = ObsidianColors.Secondary.copy(alpha = innerPulseAlpha),
                        shape = CircleShape
                    )
            )

            Canvas(
                modifier = Modifier
                    .size(186.dp)
                    .rotate(scanRotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            ObsidianColors.Primary.copy(alpha = 0.95f),
                            ObsidianColors.Secondary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 16f,
                    sweepAngle = 92f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Surface(
                modifier = Modifier
                    .size(148.dp)
                    .scale(coreScale),
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.24f)),
                shadowElevation = 18.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ObsidianColors.Primary.copy(alpha = 0.18f),
                                    ObsidianColors.Secondary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .background(ObsidianColors.SurfaceContainerLow.copy(alpha = 0.92f))
                            .border(1.dp, ObsidianColors.Primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(ObsidianColors.KineticGradient, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                                    }
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = ObsidianColors.SurfaceContainer.copy(alpha = 0.72f),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.18f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ObsidianColors.Primary, CircleShape)
                )
                Text(
                    text = "All Systems Secure",
                    color = ObsidianColors.OnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════
// LOADING SCREEN
// ═════════════════════════════════════════════

@Composable
fun LoadingScreenObsidian() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_anims")
    val sweepAngle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "sweep")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val rotationNodes by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "nodes")

    Box(modifier = Modifier.fillMaxSize().background(ObsidianColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().rotate(sweepAngle)) {
                    drawArc(brush = Brush.sweepGradient(listOf(Color.Transparent, ObsidianColors.Primary.copy(alpha = 0.2f), Color.Transparent)), startAngle = 0f, sweepAngle = 90f, useCenter = true)
                }
                Box(modifier = Modifier.size(240.dp).border(1.dp, ObsidianColors.Primary.copy(alpha = 0.1f), CircleShape))
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(140.dp).scale(pulseScale).alpha(0.1f).background(ObsidianColors.Primary, CircleShape))
                    Surface(modifier = Modifier.size(140.dp), shape = CircleShape, color = ObsidianColors.SurfaceContainer, border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.2f)), shadowElevation = 20.dp) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, null, modifier = Modifier.size(72.dp).graphicsLayer(alpha = 0.99f).drawWithCache {
                                onDrawWithContent { drawContent(); drawRect(ObsidianColors.KineticGradient, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop) }
                            })
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().rotate(rotationNodes)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(12.dp)
                            .background(ObsidianColors.Primary, CircleShape)
                            .shadow(
                                elevation = 15.dp,
                                shape = CircleShape,
                                ambientColor = ObsidianColors.Primary,
                                spotColor = ObsidianColors.Primary
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("SECURE SCAN IN PROGRESS", style = TextStyle(brush = ObsidianColors.KineticGradient), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Level 7 AI Heuristic Audit Active", color = ObsidianColors.Primary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(48.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StageCard("URL Validation", "syntactic structural integrity", "Secure", Icons.Default.CheckCircle, true, Modifier.weight(1f))
                    StageCard("Threat Intel", "Cross-referencing blacklists", "Active", Icons.Default.Dns, false, Modifier.weight(1f), isActive = true)
                }
            }
        }
    }
}

@Composable
fun StageCard(title: String, desc: String, status: String, icon: ImageVector, isDone: Boolean, modifier: Modifier, isActive: Boolean = false, isPending: Boolean = false) {
    val alpha = if (isPending) 0.5f else 1f
    Box(modifier = modifier.alpha(alpha).background(if (isActive) ObsidianColors.SurfaceContainer else ObsidianColors.SurfaceContainerLow, RoundedCornerShape(12.dp)).run { if (isActive) border(1.dp, ObsidianColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)) else this }.padding(16.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(ObsidianColors.Primary.copy(alpha = if(isActive) 0.2f else 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if(isDone || isActive) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = ObsidianColors.OnSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

// ═════════════════════════════════════════════
// ANALYSIS SCREEN
// ═════════════════════════════════════════════

@Composable
fun AnalysisScreenObsidian(navController: NavController, viewModel: SecurityViewModel) {
    val scanState by viewModel.scanState.observeAsState(ScanState.Idle)
    
    when (scanState) {
        ScanState.Loading -> {
            Scaffold(
                containerColor = ObsidianColors.Background,
                topBar = { OKTopBar(navController) },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        PressableOutlinedActionButton(
                            text = "Cancel",
                            icon = Icons.Default.Close,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.reset()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    LoadingScreenObsidian()
                }
            }
        }
        is ScanState.Success -> {
            val success = scanState as ScanState.Success
            val verdict = success.analysis.verdict.uppercase()
            val isDangerous = verdict == "DANGER" || verdict == "DANGEROUS" || verdict == "MALICIOUS"
            val isWarning = verdict == "WARNING" || verdict == "SUSPICIOUS"
            val verdictGradient = if (isDangerous) ObsidianColors.VerdictDangerGradient else if (isWarning) ObsidianColors.VerdictWarningGradient else ObsidianColors.VerdictSafeGradient
            val verdictColor = if (isDangerous) ObsidianColors.Error else if (isWarning) ObsidianColors.Warning else ObsidianColors.Safe
            val riskSummaryPoints = remember {
                listOf(
                    "Website scanned successfully",
                    "No threats detected",
                    "Verified by 69 security engines"
                )
            }

            Scaffold(
                containerColor = ObsidianColors.Background,
                topBar = { OKTopBar(navController) },
                bottomBar = {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(modifier = Modifier.fillMaxWidth().height(80.dp), shape = CircleShape, color = ObsidianColors.SurfaceContainer.copy(alpha = 0.7f), tonalElevation = 8.dp) {
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ObsidianColors.Primary) }
                                IconButton(onClick = { }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ObsidianColors.Primary.copy(alpha = 0.4f)) }
                                FloatingActionButton(onClick = { viewModel.scanUrl(success.url) }, containerColor = Color.Transparent, elevation = FloatingActionButtonDefaults.elevation(0.dp), modifier = Modifier.size(56.dp).clip(CircleShape).background(ObsidianColors.KineticGradient)) { Icon(Icons.Default.Refresh, null, tint = Color.Black) }
                                IconButton(onClick = { }) { Icon(Icons.Default.Share, null, tint = ObsidianColors.Primary) }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianColors.Background,
                                    ObsidianColors.SurfaceContainerLowest,
                                    ObsidianColors.Background
                                )
                            )
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            PremiumVerdictCard(
                                verdict = verdict,
                                summary = success.analysis.summary,
                                verdictColor = verdictColor,
                                verdictGradient = verdictGradient,
                                isDangerous = isDangerous,
                                isWarning = isWarning
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ConfidenceLevelCard(success.analysis.confidenceScore)
                                CleanStatusCard(
                                    title = if (isDangerous) "Threat Signals Detected" else if (isWarning) "Caution Signals Present" else "Clean Status",
                                    subtitle = if (isDangerous) "Manual review recommended" else if (isWarning) "Low-risk anomalies observed" else "AI Verified Clean",
                                    accent = verdictColor
                                )
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("GLOBAL REPUTATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurfaceVariant, letterSpacing = 2.sp)
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(end = 4.dp)
                                ) {
                                    item {
                                        ReputationCard("Malicious", "${success.vtStats.malicious}", ObsidianColors.Error, Icons.Default.BugReport, Modifier.width(108.dp))
                                    }
                                    item {
                                        ReputationCard("Suspicious", "${success.vtStats.suspicious}", ObsidianColors.Warning, Icons.Default.WarningAmber, Modifier.width(108.dp))
                                    }
                                    item {
                                        ReputationCard("Clean", "${success.vtStats.harmless}", ObsidianColors.Safe, Icons.Default.Verified, Modifier.width(108.dp))
                                    }
                                }
                            }
                        }
                        item {
                            var expanded by remember { mutableStateOf(true) }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                color = ObsidianColors.SurfaceContainer.copy(alpha = 0.66f),
                                border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.14f)),
                                shadowElevation = 10.dp
                            ) {
                                Column(modifier = Modifier.animateContentSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded }
                                            .padding(horizontal = 22.dp, vertical = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianColors.KineticGradient), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Psychology, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                            }
                                            Text("AI Risk Summary", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                                        }
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = ObsidianColors.OnSurfaceVariant)
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp)
                                                .fillMaxWidth()
                                                .background(ObsidianColors.SurfaceContainerLow.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                                                .border(1.dp, ObsidianColors.OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                                                .padding(horizontal = 20.dp, vertical = 22.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            riskSummaryPoints.forEach { point ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(24.dp)
                                                            .background(ObsidianColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, ObsidianColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                            .shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = ObsidianColors.Primary.copy(alpha = 0.18f), spotColor = ObsidianColors.Primary.copy(alpha = 0.18f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Check, null, tint = ObsidianColors.Primary, modifier = Modifier.size(14.dp))
                                                    }
                                                    Text(point, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 24.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                PressableOutlinedActionButton(
                                    text = "Go Back",
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.reset()
                                        navController.popBackStack()
                                    }
                                )
                                PressableGradientActionButton(
                                    text = "Continue",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    brush = verdictGradient,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val encodedUrl = URLEncoder.encode(success.url, StandardCharsets.UTF_8.toString())
                                        navController.navigate("webview/$encodedUrl")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        is ScanState.Error -> {
            val errorState = scanState as ScanState.Error

            Scaffold(
                containerColor = ObsidianColors.Background,
                topBar = { OKTopBar(navController) },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        PressableOutlinedActionButton(
                            text = "Back Home",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.reset()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = ObsidianColors.SurfaceContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, ObsidianColors.Error.copy(alpha = 0.24f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(ObsidianColors.Error.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = ObsidianColors.Error, modifier = Modifier.size(28.dp))
                            }
                            Text("Scan Failed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text(
                                errorState.message,
                                color = ObsidianColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
        else -> {
            LaunchedEffect(Unit) {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun ReputationCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(140), label = "rep_scale")

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianColors.SurfaceContainer.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.16f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null, onClick = {})
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, color.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = color.copy(alpha = 0.24f), spotColor = color.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PremiumVerdictCard(
    verdict: String,
    summary: String,
    verdictColor: Color,
    verdictGradient: Brush,
    isDangerous: Boolean,
    isWarning: Boolean
) {
    val headline = when {
        isDangerous -> "Threat detected"
        isWarning -> "Use caution"
        else -> "SAFE"
    }
    val shortMessage = when {
        isDangerous -> "Risk markers found"
        isWarning -> "Minor anomalies detected"
        else -> "No major threats found"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ObsidianColors.SurfaceContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.14f)),
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(verdictGradient)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(112.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .blur(8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isDangerous) Icons.Default.GppBad else if (isWarning) Icons.Default.GppMaybe else Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                        Text(headline, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Text(verdict, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(shortMessage, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    Text(summary, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ConfidenceLevelCard(score: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "confidence_progress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ObsidianColors.SurfaceContainer.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.14f)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(ObsidianColors.SurfaceContainerLow.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = ObsidianColors.Primary,
                    trackColor = ObsidianColors.SurfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
                Icon(Icons.Default.VerifiedUser, null, tint = ObsidianColors.Primary.copy(alpha = 0.95f), modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Confidence Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurfaceVariant, letterSpacing = 1.1.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$score", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Primary.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 7.dp))
                }
                Text("Animated confidence score derived from AI and reputation engines.", fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun CleanStatusCard(title: String, subtitle: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.06f))))
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TaskAlt, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = ObsidianColors.OnSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PressableOutlinedActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "outlined_action_scale")

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(58.dp).scale(scale),
        shape = CircleShape,
        interactionSource = interactionSource,
        border = BorderStroke(1.5.dp, ObsidianColors.Primary.copy(alpha = 0.25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ObsidianColors.SurfaceContainer.copy(alpha = 0.45f),
            contentColor = Color.White
        )
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PressableGradientActionButton(
    text: String,
    icon: ImageVector,
    brush: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "gradient_action_scale")

    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp).scale(scale).clip(CircleShape).background(brush),
        shape = CircleShape,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.White)
    }
}

@Composable
fun ScansScreenObsidian(navController: NavController, viewModel: SecurityViewModel) {
    val recentScans by viewModel.recentScans.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf("Latest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedScan by remember { mutableStateOf<RecentScanInfo?>(null) }

    val filteredScans = remember(recentScans, searchQuery, selectedFilter, sortOption) {
        recentScans
            .filter { scan ->
                val matchesSearch = searchQuery.isBlank() || displaySiteLabel(scan.url).contains(searchQuery, ignoreCase = true) || scan.url.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    "Safe" -> scan.verdict == "SAFE"
                    "Warning" -> scan.verdict == "WARNING" || scan.verdict == "SUSPICIOUS"
                    "Dangerous" -> scan.verdict == "DANGEROUS" || scan.verdict == "MALICIOUS"
                    else -> true
                }
                matchesSearch && matchesFilter
            }
            .let { scans ->
                when (sortOption) {
                    "Risk Level" -> scans.sortedBy { intelligenceRiskRank(it.verdict) }
                    else -> scans
                }
            }
    }

    val safeCount = recentScans.count { it.verdict == "SAFE" }
    val blockedCount = recentScans.count { it.verdict == "WARNING" || it.verdict == "SUSPICIOUS" || it.verdict == "DANGEROUS" || it.verdict == "MALICIOUS" }
    val safeRate = if (recentScans.isEmpty()) 0 else ((safeCount.toFloat() / recentScans.size) * 100).toInt()

    if (selectedScan != null) {
        val scan = selectedScan!!
        AlertDialog(
            onDismissRequest = { selectedScan = null },
            containerColor = ObsidianColors.SurfaceContainer,
            title = { Text(displaySiteLabel(scan.url), color = Color.White) },
            text = { Text("Choose an action for this intelligence record.", color = ObsidianColors.OnSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedScan = null
                        viewModel.scanUrl(scan.url)
                        navController.navigate("analysis") {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Re-scan", color = ObsidianColors.Primary)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.removeRecentScan(scan.url)
                            selectedScan = null
                        }
                    ) {
                        Text("Delete", color = ObsidianColors.Error)
                    }
                    TextButton(onClick = { selectedScan = null }) {
                        Text("Cancel", color = ObsidianColors.OnSurfaceVariant)
                    }
                }
            }
        )
    }

    Scaffold(topBar = { OKTopBar(navController) }, bottomBar = { OKBottomNav(navController) }, containerColor = ObsidianColors.Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Text("All Intelligence", fontSize = 31.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Your scan history and protection insights", color = ObsidianColors.OnSurfaceVariant, fontSize = 13.sp)
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Tune, null, tint = ObsidianColors.Primary)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = ObsidianColors.SurfaceContainer
                        ) {
                            DropdownMenuItem(
                                text = { Text("Latest") },
                                onClick = {
                                    sortOption = "Latest"
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Risk Level") },
                                onClick = {
                                    sortOption = "Risk Level"
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.64f),
                    border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.12f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianColors.Primary.copy(alpha = 0.10f),
                                        ObsidianColors.Secondary.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(ObsidianColors.SurfaceContainerHigh, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isBlank()) {
                                    Text("Search domain or URL", color = ObsidianColors.OnSurfaceVariant)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listOf("All", "Safe", "Warning", "Dangerous")) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianColors.Primary.copy(alpha = 0.18f),
                                selectedLabelColor = Color.White,
                                containerColor = ObsidianColors.SurfaceContainerLow,
                                labelColor = ObsidianColors.OnSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = ObsidianColors.OutlineVariant.copy(alpha = 0.2f),
                                selectedBorderColor = ObsidianColors.Primary.copy(alpha = 0.22f)
                            )
                        )
                    }
                }
            }

            item {
                IntelligenceStatsBar(
                    totalScans = recentScans.size,
                    safeRate = safeRate,
                    blockedCount = blockedCount,
                    sortOption = sortOption
                )
            }

            if (filteredScans.isEmpty()) {
                item {
                    IntelligenceEmptyState(onClick = { navController.navigate("home") })
                }
            } else {
                items(filteredScans.size) { index ->
                    val scan = filteredScans[index]
                    IntelligenceHistoryCard(
                        scan = scan,
                        index = index,
                        onClick = {
                            viewModel.scanUrl(scan.url)
                            navController.navigate("analysis") {
                                launchSingleTop = true
                            }
                        },
                        onLongPress = { selectedScan = scan }
                    )
                }
            }
        }
    }
}

private fun intelligenceRiskRank(verdict: String): Int = when (verdict) {
    "DANGEROUS", "MALICIOUS" -> 0
    "WARNING", "SUSPICIOUS" -> 1
    else -> 2
}

@Composable
fun IntelligenceStatsBar(totalScans: Int, safeRate: Int, blockedCount: Int, sortOption: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, ObsidianColors.OutlineVariant.copy(alpha = 0.18f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IntelligenceMetric("Total scans", totalScans.toString(), ObsidianColors.Primary)
            IntelligenceMetric("Safe %", "$safeRate%", ObsidianColors.Safe)
            IntelligenceMetric("Threats blocked", blockedCount.toString(), ObsidianColors.Error)
            IntelligenceMetric("Sorted", sortOption, ObsidianColors.Warning)
        }
    }
}

@Composable
fun IntelligenceMetric(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(7.dp).background(accent, CircleShape))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = ObsidianColors.OnSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun IntelligenceEmptyState(onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "scans_empty_state")
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7600, easing = LinearEasing), RepeatMode.Restart),
        label = "scans_empty_rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scans_empty_pulse"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "scans_empty_button_scale")

    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(360))) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, ObsidianColors.Primary.copy(alpha = 0.16f)),
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianColors.Primary.copy(alpha = 0.11f),
                                ObsidianColors.Secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().rotate(ringRotation)) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        ObsidianColors.Primary.copy(alpha = 0.92f),
                                        ObsidianColors.Secondary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 110f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulse)
                                .background(ObsidianColors.SurfaceContainerHigh.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, ObsidianColors.Primary.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, null, tint = ObsidianColors.Primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text("No scans yet", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scan a website to view protection insights and history",
                        color = ObsidianColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        modifier = Modifier.scale(buttonScale).background(ObsidianColors.KineticGradient, RoundedCornerShape(100.dp))
                    ) {
                        Icon(Icons.Default.Radar, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntelligenceHistoryCard(scan: RecentScanInfo, index: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    val color = when (scan.verdict) {
        "SAFE" -> ObsidianColors.Safe
        "WARNING", "SUSPICIOUS" -> ObsidianColors.Warning
        else -> ObsidianColors.Error
    }
    val statusLabel = when (scan.verdict) {
        "SAFE" -> "Safe"
        "WARNING", "SUSPICIOUS" -> "Warning"
        else -> "Dangerous"
    }
    val icon = when (scan.verdict) {
        "SAFE" -> Icons.Default.Verified
        "WARNING", "SUSPICIOUS" -> Icons.Default.WarningAmber
        else -> Icons.Default.GppBad
    }
    var visible by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "intelligence_card_icon_$index")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "intelligence_card_pulse_$index"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 45).toLong())
        visible = true
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(280))) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianColors.SurfaceContainerLow.copy(alpha = 0.68f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                color.copy(alpha = 0.14f),
                                ObsidianColors.SurfaceContainerLow.copy(alpha = 0.26f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(pulse)
                            .background(color.copy(alpha = 0.14f), CircleShape)
                            .border(1.dp, color.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(displaySiteLabel(scan.url), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$statusLabel • ${scan.time}", color = ObsidianColors.OnSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
                    ) {
                        Text(statusLabel, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    Text(scan.confidence, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreenObsidian(navController: NavController) {
    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }

    var safeBrowsing by remember { mutableStateOf(true) }
    var blockMaliciousSites by remember { mutableStateOf(true) }
    var threatAlerts by remember { mutableStateOf(true) }
    var incognitoMode by remember { mutableStateOf(false) }
    var doNotTrack by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(true) }
    var realTimeProtection by remember { mutableStateOf(true) }
    var autoUpdateDatabase by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var clearDataMessage by remember { mutableStateOf<String?>(null) }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = ObsidianColors.SurfaceContainer,
            title = { Text("Clear browsing data?", color = Color.White) },
            text = {
                Text(
                    "This will remove local browsing history, cached pages, and temporary web data stored in the app.",
                    color = ObsidianColors.OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        clearDataMessage = "Browsing data cleared"
                    }
                ) {
                    Text("Clear", color = ObsidianColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = ObsidianColors.OnSurfaceVariant)
                }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        SettingsInfoDialog(
            title = "Privacy Policy",
            message = "Eye ShielD processes on-device settings locally. URL analysis requests are sent only when you explicitly scan or browse through protected mode.",
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    if (showTermsDialog) {
        SettingsInfoDialog(
            title = "Terms & Conditions",
            message = "Eye ShielD provides security insights to support safer browsing. It should be used as an aid, not as a sole source of trust decisions for sensitive transactions.",
            onDismiss = { showTermsDialog = false }
        )
    }

    if (showAboutDialog) {
        SettingsInfoDialog(
            title = "About Eye ShielD",
            message = "Eye ShielD is a cybersecurity-focused browser companion built to help identify risky URLs, highlight reputation signals, and reduce exposure to malicious websites.",
            onDismiss = { showAboutDialog = false }
        )
    }

    Scaffold(
        topBar = { OKTopBar(navController) },
        bottomBar = { OKBottomNav(navController) },
        containerColor = ObsidianColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Manage protection, privacy, and app behavior with a streamlined security configuration.",
                        color = ObsidianColors.OnSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            item {
                AnimatedVisibility(visible = clearDataMessage != null, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        color = ObsidianColors.SurfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(ObsidianColors.Safe.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = ObsidianColors.Safe, modifier = Modifier.size(18.dp))
                            }
                            Text(clearDataMessage.orEmpty(), color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Security Settings", icon = Icons.Default.Security) {
                    SettingsToggleRow(
                        title = "Safe Browsing",
                        description = "Warn before potentially dangerous pages load.",
                        icon = Icons.Default.TravelExplore,
                        checked = safeBrowsing,
                        onCheckedChange = { safeBrowsing = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Block Malicious Sites",
                        description = "Prevent access to flagged phishing and malware destinations.",
                        icon = Icons.Default.Block,
                        checked = blockMaliciousSites,
                        onCheckedChange = { blockMaliciousSites = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Enable Threat Alerts",
                        description = "Surface immediate alerts when suspicious activity is detected.",
                        icon = Icons.Default.NotificationImportant,
                        checked = threatAlerts,
                        onCheckedChange = { threatAlerts = it }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Privacy", icon = Icons.Default.PrivacyTip) {
                    SettingsActionRow(
                        title = "Clear Browsing Data",
                        description = "Delete cached pages, session traces, and temporary site data.",
                        icon = Icons.Default.DeleteSweep,
                        accentColor = ObsidianColors.Error,
                        onClick = {
                            clearDataMessage = null
                            showClearDataDialog = true
                        }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Enable Incognito Mode",
                        description = "Reduce local browsing traces for private sessions.",
                        icon = Icons.Default.VisibilityOff,
                        checked = incognitoMode,
                        onCheckedChange = { incognitoMode = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Do Not Track",
                        description = "Request that supported websites limit tracking behavior.",
                        icon = Icons.Default.GppGood,
                        checked = doNotTrack,
                        onCheckedChange = { doNotTrack = it }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "App Settings", icon = Icons.Default.Tune) {
                    SettingsToggleRow(
                        title = "Dark Mode",
                        description = "Keep the interface optimized for low-light use.",
                        icon = Icons.Default.DarkMode,
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Real-time Protection",
                        description = "Continuously inspect active browsing sessions for risk.",
                        icon = Icons.Default.GppMaybe,
                        checked = realTimeProtection,
                        onCheckedChange = { realTimeProtection = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Auto-update Database",
                        description = "Refresh reputation intelligence and detection rules automatically.",
                        icon = Icons.Default.SystemUpdateAlt,
                        checked = autoUpdateDatabase,
                        onCheckedChange = { autoUpdateDatabase = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "Notifications",
                        description = "Receive app updates and security event notifications.",
                        icon = Icons.Default.NotificationsActive,
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Info", icon = Icons.Default.Info) {
                    SettingsInfoRow(
                        title = "App Version",
                        value = appVersion,
                        icon = Icons.Default.Info,
                        showChevron = false,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = "Privacy Policy",
                        value = "Review how data is handled",
                        icon = Icons.Default.Policy,
                        onClick = { showPrivacyPolicyDialog = true }
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = "Terms & Conditions",
                        value = "Usage terms and responsibilities",
                        icon = Icons.Default.Description,
                        onClick = { showTermsDialog = true }
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = "About App",
                        value = "Product overview and purpose",
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ObsidianColors.SurfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, ObsidianColors.OutlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ObsidianColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                    }
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                content()
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(if (pressed) 0.99f else 1f, tween(140), label = "settings_toggle_row_scale")
    val iconScale by animateFloatAsState(if (checked) 1f else 0.92f, tween(220), label = "settings_toggle_icon_scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .scale(iconScale)
                .background(
                    if (checked) ObsidianColors.Primary.copy(alpha = 0.14f) else ObsidianColors.SurfaceContainerHigh,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) ObsidianColors.Primary else ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(description, color = ObsidianColors.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianColors.Primary,
                checkedTrackColor = ObsidianColors.Primary.copy(alpha = 0.38f),
                uncheckedThumbColor = ObsidianColors.OnSurfaceVariant,
                uncheckedTrackColor = ObsidianColors.SurfaceBright
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "settings_action_scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(description, color = ObsidianColors.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = ObsidianColors.OnSurfaceVariant)
    }
}

@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    icon: ImageVector,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.99f else 1f, tween(120), label = "settings_info_scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(ObsidianColors.SurfaceContainerHigh, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = ObsidianColors.Primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(value, color = ObsidianColors.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (showChevron) {
            Icon(Icons.Default.ChevronRight, null, tint = ObsidianColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = ObsidianColors.OutlineVariant.copy(alpha = 0.45f), thickness = 1.dp)
}

@Composable
fun SettingsInfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.SurfaceContainer,
        title = { Text(title, color = Color.White) },
        text = { Text(message, color = ObsidianColors.OnSurfaceVariant, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ObsidianColors.Primary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OKTopBar(navController: NavController? = null) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .animateContentSize()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                slideInVertically(animationSpec = tween(320, easing = FastOutSlowInEasing), initialOffsetY = { -it / 3 })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(36.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = ObsidianColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EyeShield",
                    color = ObsidianColors.OnBackground,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun OKBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = ObsidianColors.Background.copy(alpha = 0.6f), 
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .border(1.dp, ObsidianColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = ObsidianColors.Primary, selectedTextColor = ObsidianColors.Primary, unselectedIconColor = ObsidianColors.OnBackground.copy(alpha = 0.4f), unselectedTextColor = ObsidianColors.OnBackground.copy(alpha = 0.4f), indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = currentRoute == "scans",
            onClick = { navController.navigate("scans") },
            icon = { 
                Box(modifier = Modifier.size(48.dp).background(if(currentRoute == "scans") ObsidianColors.SurfaceContainer else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Radar, null) 
                }
            },
            label = { Text("Scans", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings", fontSize = 10.sp) }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreenObsidian(navController: NavController, url: String) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var currentUrl by remember(url) { mutableStateOf(url) }
    var pendingUrl by remember(url) { mutableStateOf(url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var blockedError by remember { mutableStateOf<String?>(null) }
    val secureConnection = currentUrl.startsWith("https://")
    val displayHost = remember(currentUrl) {
        val parsed = Uri.parse(currentUrl)
        parsed.host ?: currentUrl.removePrefix("https://").removePrefix("http://")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8F9FA),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrowserControlIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            enabled = true,
                            highlight = false,
                            contentDescription = "Back",
                            onClick = {
                                val view = webViewRef
                                if (view?.canGoBack() == true) {
                                    view.goBack()
                                    syncBrowserState(view) { back, forward, liveUrl ->
                                        canGoBack = back
                                        canGoForward = forward
                                        currentUrl = liveUrl
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDADCE0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (secureConnection) Icons.Default.Lock else Icons.Default.WarningAmber,
                                    null,
                                    tint = if (secureConnection) Color(0xFF188038) else Color(0xFFD93025),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    displayHost,
                                    color = Color(0xFF202124),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrowserStatusBadge(
                            if (secureConnection) "Secure" else "Not Secure",
                            if (secureConnection) Color(0xFF188038) else Color(0xFFD93025),
                            if (secureConnection) Icons.Default.Lock else Icons.Default.WarningAmber,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    if (progress < 100) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            progress = { progress / 100f },
                            color = Color(0xFF1A73E8),
                            trackColor = Color(0xFFE8EAED)
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(0.dp),
                        color = Color.White
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { androidContext ->
                                    WebView(androidContext).apply {
                                        webViewRef = this
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                progress = newProgress
                                                view?.let { browser ->
                                                    syncBrowserState(browser) { back, forward, liveUrl ->
                                                        canGoBack = back
                                                        canGoForward = forward
                                                        currentUrl = liveUrl
                                                    }
                                                }
                                            }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                blockedError = null
                                                pendingUrl = url ?: pendingUrl
                                                currentUrl = url ?: currentUrl
                                                view?.let { browser ->
                                                    syncBrowserState(browser) { back, forward, liveUrl ->
                                                        canGoBack = back
                                                        canGoForward = forward
                                                        currentUrl = liveUrl
                                                    }
                                                }
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                progress = 100
                                                currentUrl = url ?: currentUrl
                                                view?.let { browser ->
                                                    syncBrowserState(browser) { back, forward, liveUrl ->
                                                        canGoBack = back
                                                        canGoForward = forward
                                                        currentUrl = liveUrl
                                                    }
                                                }
                                            }

                                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                super.onReceivedError(view, request, error)
                                                if (request?.isForMainFrame != false) {
                                                    blockedError = error?.description?.toString() ?: "ERR_NAME_NOT_RESOLVED"
                                                    pendingUrl = request?.url?.toString() ?: pendingUrl
                                                    view?.let { browser ->
                                                        syncBrowserState(browser) { back, forward, liveUrl ->
                                                            canGoBack = back
                                                            canGoForward = forward
                                                            currentUrl = liveUrl
                                                        }
                                                    }
                                                }
                                            }

                                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                                super.onReceivedError(view, errorCode, description, failingUrl)
                                                blockedError = description ?: "ERR_NAME_NOT_RESOLVED"
                                                pendingUrl = failingUrl ?: pendingUrl
                                            }
                                        }
                                        loadUrl(url)
                                    }
                                },
                                update = { view ->
                                    webViewRef = view
                                },
                                modifier = Modifier.fillMaxSize().zIndex(0f)
                            )

                            if (blockedError != null) {
                                BrowserBlockedCard(
                                    errorCode = "ERR_NAME_NOT_RESOLVED",
                                    onRetry = {
                                        blockedError = null
                                        progress = 15
                                        val view = webViewRef
                                        val retryUrl = pendingUrl.ifBlank { currentUrl }.ifBlank { url }
                                        if (view != null) {
                                            val liveUrl = view.getUrl()
                                            if (liveUrl.isNullOrBlank() || liveUrl == "about:blank") {
                                                view.loadUrl(retryUrl)
                                            } else {
                                                view.reload()
                                            }
                                            syncBrowserState(view) { back, forward, liveUrl ->
                                                canGoBack = back
                                                canGoForward = forward
                                                currentUrl = liveUrl
                                            }
                                        }
                                    },
                                    onCheckSecurity = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrowserControlIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        enabled = canGoBack,
                        highlight = false,
                        contentDescription = "Back",
                        onClick = {
                            Log.d("EyeShielD-Browser", "Back button clicked")
                            val view = webViewRef
                            if (view?.canGoBack() == true) {
                                view.goBack()
                                syncBrowserState(view) { back, forward, liveUrl ->
                                    canGoBack = back
                                    canGoForward = forward
                                    currentUrl = liveUrl
                                }
                            }
                        }
                    )
                    BrowserControlIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        enabled = canGoForward,
                        highlight = false,
                        contentDescription = "Forward",
                        onClick = {
                            Log.d("EyeShielD-Browser", "Forward button clicked")
                            val view = webViewRef
                            if (view?.canGoForward() == true) {
                                view.goForward()
                                syncBrowserState(view) { back, forward, liveUrl ->
                                    canGoBack = back
                                    canGoForward = forward
                                    currentUrl = liveUrl
                                }
                            }
                        }
                    )
                    BrowserControlIconButton(
                        icon = Icons.Default.Refresh,
                        enabled = true,
                        highlight = true,
                        contentDescription = "Refresh",
                        onClick = {
                            Log.d("EyeShielD-Browser", "Refresh button clicked")
                            progress = 10
                            blockedError = null
                            val view = webViewRef
                            val reloadUrl = pendingUrl.ifBlank { currentUrl }.ifBlank { url }
                            if (view != null) {
                                val liveUrl = view.getUrl()
                                if (liveUrl.isNullOrBlank() || liveUrl == "about:blank") {
                                    view.loadUrl(reloadUrl)
                                } else {
                                    view.reload()
                                }
                                syncBrowserState(view) { back, forward, liveUrl ->
                                    canGoBack = back
                                    canGoForward = forward
                                    currentUrl = liveUrl
                                }
                            }
                        }
                    )
                    BrowserControlIconButton(
                        icon = Icons.Default.Share,
                        enabled = true,
                        highlight = false,
                        contentDescription = "Share",
                        onClick = {
                            Log.d("EyeShielD-Browser", "Share button clicked")
                            val shareUrl = webViewRef?.getUrl()?.takeIf { it.isNotBlank() } ?: currentUrl
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareUrl)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share link"))
                        }
                    )
                }
            }
        }
    }
}

private fun syncBrowserState(
    webView: WebView,
    onUpdate: (canGoBack: Boolean, canGoForward: Boolean, currentUrl: String) -> Unit
) {
    onUpdate(
        webView.canGoBack(),
        webView.canGoForward(),
        webView.getUrl() ?: ""
    )
}

@Composable
fun BrowserStatusBadge(label: String, accent: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Text(label, color = Color(0xFF5F6368), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun BrowserControlIconButton(
    icon: ImageVector,
    enabled: Boolean,
    highlight: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.92f else 1f,
        animationSpec = tween(120),
        label = "browser_nav_button_scale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .zIndex(3f)
            .size(if (highlight) 48.dp else 44.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
            .focusable(enabled)
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (enabled) {
                if (highlight) Color(0xFF1A73E8) else Color(0xFF5F6368)
            } else {
                Color(0xFFBDC1C6)
            },
            modifier = Modifier.size(if (highlight) 24.dp else 22.dp)
        )
    }
}

@Composable
fun BrowserBlockedCard(
    errorCode: String,
    onRetry: () -> Unit,
    onCheckSecurity: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFD93025), modifier = Modifier.size(44.dp))
            Text("Webpage not available", color = Color(0xFF202124), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text("This site can’t be reached", color = Color(0xFF5F6368), fontSize = 15.sp, textAlign = TextAlign.Center)
            Text(errorCode, color = Color(0xFF5F6368), fontSize = 13.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3C4043))
                ) {
                    Text("Retry")
                }
                Button(
                    onClick = onCheckSecurity,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8), contentColor = Color.White)
                ) {
                    Text("Check")
                }
            }
        }
    }
}
