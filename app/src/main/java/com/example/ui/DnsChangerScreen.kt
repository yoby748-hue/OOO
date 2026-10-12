package com.example.ui

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import coil.compose.AsyncImage
import com.example.R
import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dns.ConnectionStatus
import com.example.dns.CustomDnsItem
import com.example.dns.DnsPreset
import com.example.dns.DnsPresets
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DnsChangerScreen(
    uiState: DnsAppUiState,
    viewModel: DnsChangerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle back button when on non-home tab
    BackHandler(enabled = uiState.currentTab != 0) {
        viewModel.selectTab(0)
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect()
        }
    }

    // Unified single connect/disconnect handler
    val onSingleConnectToggle: () -> Unit = {
        if (uiState.connectionStatus == ConnectionStatus.CONNECTED ||
            uiState.connectionStatus == ConnectionStatus.CONNECTING
        ) {
            viewModel.disconnect()
        } else {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                vpnPermissionLauncher.launch(vpnIntent)
            } else {
                viewModel.connect()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(CyberNavyBg),
            containerColor = CyberNavyBg,
            bottomBar = {
                CyberBottomNavigationBar(
                    currentTab = uiState.currentTab,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                TopCyberHeader(
                    uiState = uiState,
                    onAccountClick = { viewModel.selectTab(3) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                ) {
                    when (uiState.currentTab) {
                        0 -> HomeConnectionTab(
                            uiState = uiState,
                            onToggle = onSingleConnectToggle,
                            onPingClick = { viewModel.testPing() },
                            onChangeServerClick = { viewModel.selectTab(1) }
                        )
                        1 -> PresetServersTab(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSelectPreset = {
                                viewModel.selectPreset(it)
                                viewModel.selectTab(0)
                            }
                        )
                        2 -> CustomDnsTab(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSelectCustom = {
                                viewModel.selectCustomServer(it)
                                viewModel.selectTab(0)
                            }
                        )
                        3 -> AccountProfileTab(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Custom DNS / VPN Add Dialog
        if (uiState.showAddCustomDialog) {
            AddCustomDialog(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

// -------------------------------------------------------------
// TOP CYBER HEADER
// -------------------------------------------------------------
@Composable
private fun TopCyberHeader(
    uiState: DnsAppUiState,
    onAccountClick: () -> Unit
) {
    val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
                    .border(
                        1.5.dp,
                        if (isConnected) EmeraldSuccess else CyanPrimary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "MESHKIDNS",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when (uiState.connectionStatus) {
                        ConnectionStatus.CONNECTED -> "ایمن و متصل"
                        ConnectionStatus.CONNECTING -> "در حال برقراری..."
                        ConnectionStatus.DISCONNECTED -> "آماده اتصال"
                        ConnectionStatus.ERROR -> "خطا در اتصال"
                    },
                    color = if (isConnected) EmeraldSuccess else if (uiState.connectionStatus == ConnectionStatus.ERROR) RoseError else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // App Status / Original Version Chip
        Surface(
            modifier = Modifier.clickable { onAccountClick() },
            shape = RoundedCornerShape(20.dp),
            color = CyberSurfaceVariant.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نسخه اصلی",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: HOME & SINGLE POWER ORB
// -------------------------------------------------------------
@Composable
private fun HomeConnectionTab(
    uiState: DnsAppUiState,
    onToggle: () -> Unit,
    onPingClick: () -> Unit,
    onChangeServerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Selected Server Indicator Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChangeServerClick() }
                .testTag("selected_server_card"),
            shape = RoundedCornerShape(16.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سرور انتخاب شده:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = uiState.selectedServerTitle,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (uiState.selectedServerCategory == "وی پی ان") Color(0xFFEC4899).copy(alpha = 0.18f)
                                else if (uiState.selectedServerCategory == "وایرگارد") Color(0xFFC084FC).copy(alpha = 0.15f)
                                else if (uiState.selectedServerCategory == "گیمینگ") EmeraldSuccess.copy(alpha = 0.15f)
                                else CyanPrimary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.selectedServerCategory,
                            color = if (uiState.selectedServerCategory == "وی پی ان") Color(0xFFEC4899)
                            else if (uiState.selectedServerCategory == "وایرگارد") Color(0xFFC084FC)
                            else if (uiState.selectedServerCategory == "گیمینگ") EmeraldSuccess
                            else CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تغییر",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // THE ONLY MAIN CONNECT BUTTON: Interactive Neon Power Orb
        SingleNeonPowerOrb(
            status = uiState.connectionStatus,
            isVpnMode = uiState.isVpnMode || uiState.selectedServerCategory == "وی پی ان",
            onToggle = onToggle
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Live Telemetry Dashboard Card
        TelemetryDashboardCard(
            uiState = uiState,
            onPingClick = onPingClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// SINGLE NEON POWER ORB COMPONENT
// -------------------------------------------------------------
@Composable
private fun SingleNeonPowerOrb(
    status: ConnectionStatus,
    isVpnMode: Boolean = false,
    onToggle: () -> Unit
) {
    val isConnected = status == ConnectionStatus.CONNECTED
    val isConnecting = status == ConnectionStatus.CONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    val activeGlowColor by animateColorAsState(
        targetValue = when {
            isConnected && isVpnMode -> Color(0xFFEC4899)
            isConnected -> EmeraldSuccess
            isConnecting -> CoralWarning
            else -> CyanPrimary
        },
        animationSpec = tween(500),
        label = "activeGlowColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onToggle() }
                .testTag("single_connect_power_orb"),
            contentAlignment = Alignment.Center
        ) {
            // Outer Halo Ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(if (isConnected || isConnecting) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                activeGlowColor.copy(alpha = if (isConnected || isConnecting) haloAlpha else 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer Orbit Border
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                activeGlowColor,
                                activeGlowColor.copy(alpha = 0.2f),
                                activeGlowColor
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Center Primary Touch Core
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                CyberSurfaceVariant,
                                CyberSurface
                            )
                        )
                    )
                    .border(1.5.dp, activeGlowColor.copy(alpha = 0.8f), CircleShape)
                    .shadow(
                        elevation = if (isConnected) 16.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = activeGlowColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = CoralWarning,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Connection",
                        tint = activeGlowColor,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Label
        Text(
            text = when (status) {
                 ConnectionStatus.CONNECTED -> if (isVpnMode) "متصل به وی‌پی‌ان - تونل کامل (لمس برای قطع)" else "متصل به دی‌ان‌اس (لمس برای قطع)"
                 ConnectionStatus.CONNECTING -> if (isVpnMode) "در حال اتصال به سرور وی‌پی‌ان..." else "در حال برقراری تونل امن دی‌ان‌اس..."
                 ConnectionStatus.DISCONNECTED -> "لمس کنید تا متصل شوید"
                 ConnectionStatus.ERROR -> "خطا در اتصال به سرور (لمس برای تلاش مجدد)"
            },
            color = if (isConnected) (if (isVpnMode) Color(0xFFEC4899) else EmeraldSuccess) else if (status == ConnectionStatus.ERROR) RoseError else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isConnected) {
                if (isVpnMode) "تمامی ترافیک گوشی از تونل امن V2Ray با سرعت بالا عبور می‌کند"
                else "کلیه درخواست‌های DNS با سرعت بالا از سرور منتخب عبور می‌کنند"
            } else {
                "یک لمس برای شروع اتصال پرسرعت و گذر از تحریم‌ها"
            },
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// -------------------------------------------------------------
// TELEMETRY DASHBOARD CARD
// -------------------------------------------------------------
@Composable
private fun TelemetryDashboardCard(
    uiState: DnsAppUiState,
    onPingClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isVpnMode) Icons.Default.Bolt else Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = if (uiState.isVpnMode) Color(0xFFEC4899) else CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isVpnMode) "وضعیت زنده وی‌پی‌ان و تاخیر Real Delay" else "وضعیت زنده شبکه و پینگ",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onPingClick,
                    enabled = !uiState.isTestingPing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (uiState.isTestingPing) {
                        CircularProgressIndicator(
                            color = if (uiState.isVpnMode) Color(0xFFEC4899) else CyanPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Ping",
                            tint = if (uiState.isVpnMode) Color(0xFFEC4899) else CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ping Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (uiState.isVpnMode) "تاخیر Real Delay" else "پینگ سرور",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                uiState.isTestingPing -> "در حال تست..."
                                uiState.pingMs != null -> "${uiState.pingMs} ms"
                                uiState.isPingReachable == false -> "خطا / تایم‌اوت"
                                else -> "—"
                            },
                            color = when {
                                uiState.pingMs == null -> if (uiState.isPingReachable == false) RoseError else TextSecondary
                                uiState.pingMs < 100 -> EmeraldSuccess
                                uiState.pingMs < 200 -> CoralWarning
                                else -> RoseError
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.pingDetail?.let { detail ->
                            Text(
                                text = detail,
                                color = TextMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Uptime Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = "مدت زمان اتصال", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val s = uiState.uptimeSeconds
                        val timeStr = String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
                        Text(
                            text = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) timeStr else "00:00:00",
                            color = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) EmeraldSuccess else TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) "فعال و متصل" else "غیرفعال",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Queries / Tunnel Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (uiState.isVpnMode) "نوع تونل" else "کوئری‌های موفق",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isVpnMode) "V2Ray" else "${uiState.resolvedQueries}",
                            color = if (uiState.isVpnMode) Color(0xFFEC4899) else CyanPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isVpnMode) "تونل کامل" else "پاسخ معتبر",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: PRESET SERVERS (Server 1, 2, ... NO NAMES OR IPS SHOWN!)
// -------------------------------------------------------------
@Composable
private fun PresetServersTab(
    uiState: DnsAppUiState,
    viewModel: DnsChangerViewModel,
    onSelectPreset: (DnsPreset) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("تحریم‌شکن") }
    val categories = listOf("تحریم‌شکن", "گیمینگ", "وایرگارد", "وی پی ان")

    val filteredPresets = remember(selectedCategory, uiState.serverPresets) {
        uiState.serverPresets.filter {
            it.category == selectedCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Tab Title & Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "انتخاب سرور اختصاصی",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "جهت اتصال به هر سرور روی آن ضربه بزنید",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Batch Real Delay Test Button
                Button(
                    onClick = { viewModel.testAllServersRealDelay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = Color(0xFFFACC15)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("test_all_delays_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Test All Delays",
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFFFACC15)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("تست تاخیر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Update / Sync Button from admin.php
                Button(
                    onClick = { viewModel.refreshServers(manual = true) },
                    enabled = !uiState.isUpdatingServers,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = CyanPrimary,
                        disabledContainerColor = CyberSurfaceVariant.copy(alpha = 0.5f),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("refresh_servers_button")
                ) {
                    if (uiState.isUpdatingServers) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("آپدیت...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(13.dp),
                            tint = CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("بروزرسانی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Server Update Toast/Banner
        AnimatedVisibility(visible = uiState.serverUpdateMessage != null) {
            uiState.serverUpdateMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { viewModel.dismissServerUpdateMessage() },
                    shape = RoundedCornerShape(10.dp),
                    color = CyberSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = msg, color = CyanPrimary, fontSize = 12.sp)
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips: ONLY "تحریم‌شکن" and "گیمینگ"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = CyberNavyBg,
                        containerColor = CyberSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CyberBorder,
                        selectedBorderColor = CyanPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Presets List - ONLY SHOWS SERVER NUMBER (e.g. "سرور ۱", "سرور ۲", etc.) WITHOUT NAMES OR IPS!
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(filteredPresets, key = { it.id }) { preset ->
                val isCurrent = uiState.selectedPresetId == preset.id

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPreset(preset) }
                        .testTag("preset_server_${preset.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) CyberSurfaceVariant else CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrent) CyanPrimary else CyberBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Server Number Circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) CyanPrimary.copy(alpha = 0.2f) else CyberSurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.serverNumber.replace("سرور ", "").trim(),
                                color = if (isCurrent) CyanPrimary else TextSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Middle Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = preset.serverNumber,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (preset.category == "وی پی ان") Color(0xFFEC4899).copy(alpha = 0.18f)
                                            else if (preset.category == "وایرگارد") Color(0xFFC084FC).copy(alpha = 0.15f)
                                            else if (preset.category == "گیمینگ") EmeraldSuccess.copy(alpha = 0.15f)
                                            else CyanPrimary.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = preset.tag,
                                        color = if (preset.category == "وی پی ان") Color(0xFFEC4899)
                                        else if (preset.category == "وایرگارد") Color(0xFFC084FC)
                                        else if (preset.category == "گیمینگ") EmeraldSuccess
                                        else CyanPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = when {
                                    preset.category == "وی پی ان" -> "تونل کامل V2Ray"
                                    preset.category == "وایرگارد" -> "شبکه اختصاصی وایرگارد"
                                    preset.category == "گیمینگ" -> "پینگ کم گیمینگ"
                                    else -> "سرور ضد تحریم"
                                },
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right Action Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val delayInfo = uiState.serverDelays[preset.id]

                            // Delay Pill
                            Surface(
                                onClick = { viewModel.testServerRealDelay(preset) },
                                shape = RoundedCornerShape(6.dp),
                                color = CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (delayInfo?.isTesting == true) {
                                        CircularProgressIndicator(
                                            color = CyanPrimary,
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    } else if (delayInfo?.latencyMs != null) {
                                        val ms = delayInfo.latencyMs
                                        val color = when {
                                            ms < 100 -> EmeraldSuccess
                                            ms < 200 -> CoralWarning
                                            else -> RoseError
                                        }
                                        Text(
                                            text = "$ms ms",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    } else if (delayInfo?.error != null) {
                                        Text(
                                            text = "خطا",
                                            fontSize = 9.sp,
                                            color = RoseError,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text("تست", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Button(
                                    onClick = { onSelectPreset(preset) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary.copy(alpha = 0.15f),
                                        contentColor = CyanPrimary
                                    ),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: CUSTOM DNS TAB (ADD, NAME, ENTER IPS, SAVE)
// -------------------------------------------------------------
@Composable
private fun CustomDnsTab(
    uiState: DnsAppUiState,
    viewModel: DnsChangerViewModel,
    onSelectCustom: (CustomDnsItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "دی‌ان‌اس اختصاصی (سفارشی)",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "افزودن و مدیریت سرورهای دی‌ان‌اس دلخواه",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Button(
                onClick = { viewModel.openAddCustomDialog() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = CyberNavyBg),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_custom_dns_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("افزودن سرور", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.customServers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ دی‌ان‌اس سفارشی ثبت نشده است",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "با دکمه «افزودن سرور» بالا می‌توانید نام و آی‌پی سرور دلخواه خود را ثبت کنید.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(uiState.customServers, key = { it.id }) { item ->
                    val isCurrent = uiState.selectedCustomId == item.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustom(item) }
                            .testTag("custom_server_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) CyberSurfaceVariant else CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isCurrent) CyanPrimary else CyberBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) CyanPrimary.copy(alpha = 0.2f) else CyberSurfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.serverType == "vpn") Icons.Default.Security else Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (isCurrent) CyanPrimary else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.serverType == "vpn") "کانفیگ اختصاصی وی‌پی‌ان" else "سرور دی‌ان‌اس اختصاصی",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Button(
                                        onClick = { onSelectCustom(item) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanPrimary.copy(alpha = 0.15f),
                                            contentColor = CyanPrimary
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { viewModel.deleteCustomServer(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = RoseError.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD CUSTOM DNS DIALOG
// -------------------------------------------------------------
@Composable
private fun AddCustomDialog(
    uiState: DnsAppUiState,
    viewModel: DnsChangerViewModel
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = { viewModel.closeAddCustomDialog() },
        containerColor = CyberSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("افزودن سرور / کانفیگ جدید", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "می‌توانید لینک کانفیگ وی‌پی‌ان (vless://, vmess://, trojan://, ss://) یا آی‌پی دی‌ان‌اس اختصاصی خود را وارد نمایید:",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Paste Button
                OutlinedButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let { clipText ->
                            val text = clipText.trim()
                            if (text.isNotBlank()) {
                                viewModel.onCustomPrimaryChanged(text)
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جایگذاری از کلیپ‌بورد (Paste Config/DNS)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Server Name
                OutlinedTextField(
                    value = uiState.newCustomNameInput,
                    onValueChange = { viewModel.onCustomNameChanged(it) },
                    label = { Text("نام سرور (اختیاری برای کانفیگ‌ها)", fontSize = 12.sp) },
                    placeholder = { Text("مثال: سرور توربو ترکیه", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Primary DNS IP or V2Ray Link
                OutlinedTextField(
                    value = uiState.newCustomPrimaryInput,
                    onValueChange = { viewModel.onCustomPrimaryChanged(it) },
                    label = { Text("لینک کانفیگ وی‌پی‌ان یا آی‌پی دی‌ان‌اس اول", fontSize = 12.sp) },
                    placeholder = { Text("vmess://... یا vless://... یا 1.1.1.1", fontSize = 12.sp, color = TextMuted) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_primary_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary DNS IP (optional)
                OutlinedTextField(
                    value = uiState.newCustomSecondaryInput,
                    onValueChange = { viewModel.onCustomSecondaryChanged(it) },
                    label = { Text("آی‌پی دی‌ان‌اس دوم (فقط برای حالت DNS)", fontSize = 12.sp) },
                    placeholder = { Text("مثال: 1.0.0.1", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_secondary_input")
                )

                // Error
                uiState.customDialogError?.let { err ->
                    Text(
                        text = err,
                        color = RoseError,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveCustomDns() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = CyberNavyBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_custom_dns_btn")
            ) {
                Text("ذخیره سرور", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeAddCustomDialog() }) {
                Text("انصراف", color = TextSecondary)
            }
        }
    )
}

// -------------------------------------------------------------
// TAB 3: ACCOUNT PROFILE TAB
// -------------------------------------------------------------
@Composable
private fun AccountProfileTab(
    uiState: DnsAppUiState,
    viewModel: DnsChangerViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingSync by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setProfileAvatar(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "حساب کاربری و اشتراک",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberSurfaceVariant)
                                .border(2.dp, CyanPrimary, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.profileAvatarPath != null) {
                                AsyncImage(
                                    model = uiState.profileAvatarPath,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Edit Camera Icon Overlay
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = CyberNavyBg,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = uiState.userSession?.username ?: "کاربر بدون نام",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "اشتراک فعال روی سرور",
                                color = EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.profileAvatarPath != null) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "حذف عکس",
                                    color = RoseError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.removeProfileAvatar() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CyberBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Remaining Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "اعتبار باقی‌مانده:", color = TextSecondary, fontSize = 13.sp)
                    }
                    Text(
                        text = uiState.userSession?.formattedRemainingTime ?: "—",
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days & Hours Breakdown Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("روزهای باقی‌مانده", color = TextMuted, fontSize = 11.sp)
                            Text("${uiState.userSession?.remainingDays ?: 0} روز", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("ساعت‌های باقی‌مانده", color = TextMuted, fontSize = 11.sp)
                            Text("${uiState.userSession?.remainingHours ?: 0} ساعت", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Single Device Policy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "محدودیت دستگاه:", color = TextSecondary, fontSize = 13.sp)
                    }
                    Text(
                        text = "۱ کاربر همزمان (قفل شده)",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Buy or Extend Subscription Button
        Button(
            onClick = { showSubscriptionDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("buy_or_extend_sub_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = CyberNavyBg
            )
        ) {
            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("خرید یا تمدید اشتراک دی‌ان‌اس", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Channels & Official Support
        Text(
            text = "کانال‌ها و پشتیبانی رسمی",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Telegram Channel Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openExternalUrl(context, "https://t.me/MESHKIYTIR") },
            shape = RoundedCornerShape(14.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("کانال تلگرام", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("@MESHKIYTIR", color = CyanPrimary, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // YouTube Channel Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openExternalUrl(context, "https://youtube.com/@meshki_yt_ir") },
            shape = RoundedCornerShape(14.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RoseError.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "YouTube", tint = RoseError, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("کانال یوتیوب ما", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("@meshki_yt_ir", color = RoseError, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Telegram Support Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openExternalUrl(context, "https://t.me/MESHKIYT") },
            shape = RoundedCornerShape(14.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SupportAgent, contentDescription = "Support", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("پشتیبانی آنلاین تلگرام", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("@MESHKIYT (پاسخگویی ۲۴ ساعته)", color = EmeraldSuccess, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Creator / Developer Attribution Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = CyberSurfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Developer",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سازنده: مهندس مهدی شیرازی",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "طراحی و توسعه اختصاصی شبکه MESHKIDNS",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Check Sync Button
        Button(
            onClick = {
                coroutineScope.launch {
                    isCheckingSync = true
                    viewModel.checkSessionValidity()
                    isCheckingSync = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberSurfaceVariant,
                contentColor = TextPrimary
            )
        ) {
            if (isCheckingSync) {
                CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("در حال هماهنگی با سرور...", fontSize = 13.sp)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("بروزرسانی وضعیت با سرور (Sync)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout Button
        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.6f))
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("خروج از حساب کاربری", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSubscriptionDialog) {
        SubscriptionPlansDialog(
            onDismissRequest = { showSubscriptionDialog = false }
        )
    }
}

// -------------------------------------------------------------
// CYBER BOTTOM NAVIGATION BAR
// -------------------------------------------------------------
@Composable
private fun CyberBottomNavigationBar(
    currentTab: Int,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(
        containerColor = CyberSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = CyberBorder,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyanPrimary,
            selectedTextColor = CyanPrimary,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted,
            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
        )

        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onSelectTab(0) },
            icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = "اتصال") },
            label = { Text("اتصال", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onSelectTab(1) },
            icon = { Icon(Icons.Default.Dns, contentDescription = "سرورها") },
            label = { Text("سرورها", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(Icons.Default.Tune, contentDescription = "کاستوم دی‌ان‌اس") },
            label = { Text("کاستوم دی‌ان‌اس", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onSelectTab(3) },
            icon = { Icon(Icons.Default.Info, contentDescription = "اطلاعات") },
            label = { Text("اطلاعات", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
            colors = itemColors
        )
    }
}
