package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.IpInfo
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AiState
import com.example.viewmodel.IpUiState
import com.example.viewmodel.IpViewModel
import kotlinx.coroutines.launch

// Beautiful "Sleek Interface" M3 Color Tokens
val SleekBg = Color(0xFFFEF7FF)
val SleekTextDark = Color(0xFF1D1B20)
val SleekTextMuted = Color(0xFF49454F)
val SleekPrimary = Color(0xFF6750A4)
val SleekPrimaryLight = Color(0xFFE8DEF8)
val SleekHeroCard = Color(0xFFEADDFF)
val SleekHeroText = Color(0xFF21005D)
val SleekCardNormal = Color(0xFFF3EDF7)
val SleekBorder = Color(0xFFCAC4D0).copy(alpha = 0.4f)
val M3Green = Color(0xFF22C55E)
val M3Red = Color(0xFFDC2626)
val M3White = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SleekBg
                ) {
                    IpLocatorDashboard()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpLocatorDashboard(viewModel: IpViewModel = viewModel()) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val localIpV6 by viewModel.localIpV6.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val carrierName by viewModel.carrierName.collectAsState()
    val pingResults by viewModel.pingResults.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    
    // Auto update information on system boot/startup
    LaunchedEffect(Unit) {
        viewModel.updateLocalNetworkInfo(context)
        viewModel.fetchIpDetails() // Loads current IP details by default
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SleekPrimaryLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Web logo",
                                tint = SleekPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "网络助手 · IP DETECTOR",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextDark,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SleekBg,
                    titleContentColor = SleekTextDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SleekCardNormal,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(BorderStroke(1.dp, SleekBorder.copy(alpha = 0.3f)))
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Overview",
                            tint = if (activeTab == 0) SleekPrimary else SleekTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "概览",
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (activeTab == 0) SleekPrimary else SleekTextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SleekPrimaryLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Tools",
                            tint = if (activeTab == 1) SleekPrimary else SleekTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "测速",
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (activeTab == 1) SleekPrimary else SleekTextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SleekPrimaryLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Ping",
                            tint = if (activeTab == 2) SleekPrimary else SleekTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "Ping诊断",
                            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (activeTab == 2) SleekPrimary else SleekTextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SleekPrimaryLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (activeTab == 3) SleekPrimary else SleekTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "设置",
                            fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (activeTab == 3) SleekPrimary else SleekTextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SleekPrimaryLight
                    )
                )
            }
        }
    ) { innerPadding ->
        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(SleekBg)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
            // Manual ip lookup box
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "手动查询自定义 IP (IP Enquiring)",
                        color = SleekTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("输入外部公网 IP 或直按右侧查询本机", color = SleekTextMuted, fontSize = 13.sp) },
                            textStyle = LocalTextStyle.current.copy(color = SleekTextDark, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.fetchIpDetails(searchQuery)
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder,
                                focusedContainerColor = SleekBg,
                                unfocusedContainerColor = SleekBg,
                                focusedLabelColor = SleekPrimary,
                                cursorColor = SleekPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("ip_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.fetchIpDetails(searchQuery)
                                keyboardController?.hide()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("ip_search_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Query",
                                tint = M3White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Central IP query status state machine
            when (val state = uiState) {
                is IpUiState.Idle -> {
                    // Handled gracefully on startup
                }
                is IpUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SleekPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在穿透物理网关并查询节点...\nResolving node configurations...",
                                color = SleekTextMuted,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                is IpUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, M3Red.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = M3Red,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "查询失败 / Troubleshooting Failed",
                                color = SleekTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                color = SleekTextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchIpDetails(searchQuery) },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("重试 (Retry)", color = M3White, fontSize = 12.sp)
                            }
                        }
                    }
                }
                is IpUiState.Success -> {
                    val ipData = state.ipInfo
                    
                    // Styled Highlight Hero IP details Card (Exactly matching HTML `#EADDFF` / `[28px] rounded`!)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekHeroCard),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (state.isCustomQuery) "您的自定义查询 IP" else "您的当前公网 IP",
                                color = SleekPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = ipData.ip,
                                        color = SleekHeroText,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp,
                                        modifier = Modifier.testTag("public_ip_display")
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { copyToClipboard(context, ipData.ip, "出口 IP") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy IP",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Network Status badge mirroring "网络连接正常" with green pulsing dot!
                            Box(
                                modifier = Modifier
                                    .background(SleekPrimary, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    var isGreenDotBright by remember { mutableStateOf(true) }
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            kotlinx.coroutines.delay(850)
                                            isGreenDotBright = !isGreenDotBright
                                        }
                                    }
                                    val dotAlpha by animateFloatAsState(
                                        targetValue = if (isGreenDotBright) 1f else 0.4f,
                                        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                                        label = "green_dot_pulse"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(M3Green.copy(alpha = dotAlpha), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "网络连接正常 (${ipData.ipVersion})",
                                        color = M3White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Geographical Details Card (Matching HTML format!)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(SleekPrimaryLight, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getCountryEmoji(ipData.countryCode),
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "地理位置 (Geographic Grid Location)",
                                    color = SleekTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${ipData.country} · ${ipData.region}省/州",
                                    color = SleekTextDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${ipData.city}市 · 邮编:${ipData.zip}",
                                    color = SleekTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // ISP / Telecom node operator card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(SleekPrimaryLight, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = "ISP Icon",
                                    tint = SleekTextDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "运营商属性 (Host Network Provider)",
                                    color = SleekTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ipData.isp,
                                    color = SleekTextDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "自治域: ${ipData.asn} · 时区: ${ipData.timezone}",
                                    color = SleekTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Sleekly themed GPS Concentric Radar Scanner
                    NetworkRadarScope(
                        lat = ipData.lat,
                        lon = ipData.lon,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Interactive OpenMaps locator launcher
                    Button(
                        onClick = { launchExternalMaps(context, ipData.lat, ipData.lon) },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Map Launcher",
                            tint = M3White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "在外部系统地图中精确定位这一 IP",
                            color = M3White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ISP / Operator and Carrier block
                    Text(
                        text = "物理网卡与本地区域网分层 (Network Hierarchy)",
                        color = SleekTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoMetricCard(
                            title = "真实卡移动基站商 (SIM Physical Carrier)",
                            value = carrierName,
                            icon = {
                                Icon(Icons.Default.SdCard, "SIM", tint = SleekTextDark, modifier = Modifier.size(20.dp))
                            }
                        )
                        InfoMetricCard(
                            title = "当前连接媒介 (Active Link Transport)",
                            value = networkType,
                            icon = {
                                Icon(Icons.Default.CellTower, "Type", tint = SleekTextDark, modifier = Modifier.size(20.dp))
                            }
                        )
                        InfoMetricCard(
                            title = "内网连接专用 IPv4 (Intranet Host IP v4)",
                            value = localIp,
                            enableCopy = true,
                            onCopy = { copyToClipboard(context, localIp, "内网 IP") },
                            icon = {
                                Icon(Icons.Default.Computer, "IPv4", tint = SleekTextDark, modifier = Modifier.size(20.dp))
                            }
                        )
                        InfoMetricCard(
                            title = "内网安全出口 IPv6 (Intranet Secure IP v6)",
                            value = localIpV6,
                            enableCopy = true,
                            onCopy = { copyToClipboard(context, localIpV6, "内网 IPv6") },
                            icon = {
                                Icon(Icons.Default.Fingerprint, "IPv6", tint = SleekTextDark, modifier = Modifier.size(20.dp))
                            }
                        )
                    }

                    // Latency Diagnostic Section (Sleek styled)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = "Metrics",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "全链路服务器延迟诊断 (RTT Latency)",
                                        color = SleekTextDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (isPinging) {
                                    CircularProgressIndicator(
                                        color = SleekPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    TextButton(
                                        onClick = { viewModel.runLatencyDiagnostics(ipData.ip) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("重测延迟", color = SleekPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val labels = listOf(
                                    "Cloudflare DNS (1.1.1.1)",
                                    "Google DNS (8.8.8.8)",
                                    "Baidu Public (180.76.76.76)",
                                    "当前目标 IP (${ipData.ip})"
                                )
                                for (label in labels) {
                                    val rtt = pingResults[label] ?: "检测中..."
                                    val rttNum = rtt.substringBefore(" ").toDoubleOrNull()
                                    val rttColor = when {
                                        rtt.contains("Timeout") || rtt.contains("Unreachable") -> M3Red
                                        rtt.contains("检测中") -> SleekTextMuted
                                        rttNum != null && rttNum < 50.0 -> M3Green
                                        rttNum != null && rttNum < 150.0 -> Color(0xFFD97706)
                                        else -> M3Red
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SleekBg, RoundedCornerShape(12.dp))
                                            .border(1.dp, SleekBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = SleekTextDark,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = rtt,
                                            color = rttColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gemini AI Diagnostic Consultant Dialog Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, SleekPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "AI Expert",
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI 拓扑诊断专家 (Gemini Intelligence)",
                                    color = SleekTextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "汇集当前公网运营商跃点、内网出口网关及主干网延迟信息，呼叫 Gemini-3.1-Pro-Preview 做高深度专家研判报告。",
                                color = SleekTextMuted,
                                fontSize = 12.sp
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            when (val ai = aiState) {
                                is AiState.Idle -> {
                                    // Stunning dark purple shadow-embossed button exactly mirroring HTML!
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SleekPrimary),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewModel.executeAiDiagnostic(ipData, carrierName, localIp) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "AI Action",
                                                    tint = M3White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "启动 AI 深度诊断研判",
                                                    color = M3White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = "Go",
                                                tint = M3White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                is AiState.Running -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = SleekPrimary, modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Gemini AI 专家诊断中 (Analytical Reasoning)... 所有跃点数据与物理路径均在分析中...",
                                                color = SleekTextMuted,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                is AiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SleekBg, RoundedCornerShape(12.dp))
                                            .border(1.dp, M3Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = "分析发生中断:",
                                            color = M3Red,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ai.message,
                                            color = SleekTextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { viewModel.executeAiDiagnostic(ipData, carrierName, localIp) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("重新载入", fontSize = 12.sp, color = M3White)
                                        }
                                    }
                                }
                                is AiState.Success -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SleekBg, RoundedCornerShape(20.dp))
                                            .border(1.dp, SleekPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DIAGNOSTIC ARCHITECTURE REPORT:",
                                                color = SleekPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(context, ai.analysis, "AI 报告") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy report",
                                                    tint = SleekTextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Dynamic text report
                                        Text(
                                            text = ai.analysis,
                                            color = SleekTextDark,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(SleekBg)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NetworkSpeedTestScreen(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(SleekBg)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PingDiagnosticScreen(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            3 -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(SleekBg)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsScreen(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// Map launcher helper
fun launchExternalMaps(context: Context, lat: Double, lon: Double) {
    try {
        val uri = "geo:$lat,$lon?q=$lat,$lon(IP+Locator)"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val browserUri = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(browserUri))
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "无法启动地图服务", Toast.LENGTH_SHORT).show()
        }
    }
}

// Clipboard copy helper
fun copyToClipboard(context: Context, text: String, label: String = "IP Address") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label 已成功复制到剪贴板", Toast.LENGTH_SHORT).show()
}

// Convert Country code to beautiful flag emoji
fun getCountryEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return "🌍"
    val code = countryCode.uppercase()
    return try {
        val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        "🌍"
    }
}

// Concentric Radar glowing scope matching Sleek design
@Composable
fun NetworkRadarScope(
    lat: Double,
    lon: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )
    val glowWidth by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(SleekCardNormal, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val maxRadius = minOf(size.width, size.height) / 2f
            
            // Outer frame circles
            drawCircle(color = SleekBorder, radius = maxRadius, center = center, style = Stroke(1.5f))
            drawCircle(color = SleekBorder.copy(alpha = 0.6f), radius = maxRadius * 0.66f, center = center, style = Stroke(1.2f))
            drawCircle(color = SleekBorder.copy(alpha = 0.4f), radius = maxRadius * 0.33f, center = center, style = Stroke(1f))
            
            // Radar Crosshairs
            drawLine(color = SleekBorder.copy(alpha = 0.6f), start = Offset(center.x - maxRadius, center.y), end = Offset(center.x + maxRadius, center.y), strokeWidth = 1f)
            drawLine(color = SleekBorder.copy(alpha = 0.6f), start = Offset(center.x, center.y - maxRadius), end = Offset(center.x, center.y + maxRadius), strokeWidth = 1f)
            
            // Sweep scan arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(SleekPrimary.copy(alpha = 0f), SleekPrimary.copy(alpha = 0.25f)),
                    center = center
                ),
                startAngle = rotation,
                sweepAngle = 70f,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2, maxRadius * 2)
            )
            
            // Active glowing target pin
            val pointRadius = 7f * glowWidth
            drawCircle(color = SleekPrimary, radius = pointRadius, center = center)
            drawCircle(color = SleekPrimary.copy(alpha = 0.35f), radius = pointRadius * 2.5f, center = center)
        }
        
        // Coordinates overlay
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "目标地球网格坐标 (GPS Location Grid)",
                color = SleekTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "纬度 LAT: %.4f • 经度 LON: %.4f".format(lat, lon),
                color = SleekPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Active visual badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(SleekPrimaryLight, RoundedCornerShape(8.dp))
                .border(1.dp, SleekPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "GPS RADAR LOCK",
                color = SleekHeroText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Reusable Metric info card list
@Composable
fun InfoMetricCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enableCopy: Boolean = false,
    onCopy: () -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SleekPrimaryLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SleekTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = SleekTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (value.any { it.isDigit() }) FontFamily.Monospace else FontFamily.Default
                )
            }
            if (enableCopy && value != "N/A" && value.isNotEmpty()) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("copy_btn_${title.take(8)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Content",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedometerGauge(
    speedMbps: Double,
    maxSpeedExpectation: Double = 100.0,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "speed_needle"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val radius = size.width / 2f
            val strokeWidth = 12.dp.toPx()

            // Draw speedometer background track
            drawArc(
                color = SleekBorder.copy(alpha = 0.5f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Draw active colored speed arc
            val percentage = (animatedSpeed / maxSpeedExpectation.toFloat()).coerceIn(0f, 1f)
            val sweepAngle = 270f * percentage
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(SleekPrimaryLight, SleekPrimary),
                    center = center
                ),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Draw ticks around the dial for extreme tactile premium feedback
            val totalTicks = 11
            for (i in 0 until totalTicks) {
                val angleDeg = 135f + (270f / (totalTicks - 1)) * i
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerRadius = radius - strokeWidth * 1.5f
                val outerRadius = radius - strokeWidth * 0.5f
                
                val startX = center.x + innerRadius * kotlin.math.cos(angleRad).toFloat()
                val startY = center.y + innerRadius * kotlin.math.sin(angleRad).toFloat()
                val endX = center.x + outerRadius * kotlin.math.cos(angleRad).toFloat()
                val endY = center.y + outerRadius * kotlin.math.sin(angleRad).toFloat()

                drawLine(
                    color = if (i / (totalTicks - 1f) <= percentage) SleekPrimary else SleekBorder.copy(alpha = 0.8f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i / (totalTicks - 1f) <= percentage) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Draw Needle pointer
            val needleAngleDeg = 135f + sweepAngle
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = radius - strokeWidth * 2f
            
            val needleEndX = center.x + needleLength * kotlin.math.cos(needleAngleRad).toFloat()
            val needleEndY = center.y + needleLength * kotlin.math.sin(needleAngleRad).toFloat()

            // Needle shaft
            drawLine(
                color = SleekPrimary,
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle center pin hub
            drawCircle(
                color = SleekBg,
                radius = 12.dp.toPx()
            )
            drawCircle(
                color = SleekPrimary,
                radius = 7.dp.toPx()
            )
        }

        // Speed text in the absolute center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = "%.1f".format(speedMbps),
                color = SleekTextDark,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Mbps",
                color = SleekTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NetworkSpeedTestScreen(viewModel: IpViewModel, modifier: Modifier = Modifier) {
    val speedTestState by viewModel.speedTestState.collectAsState()
    val currentSpeedMbps by viewModel.currentSpeedMbps.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val pingMs by viewModel.pingMs.collectAsState()
    val jitterMs by viewModel.jitterMs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        // Welcome Header
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SleekPrimaryLight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed Test Symbol",
                        tint = SleekPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "物理网带宽诊断测速",
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "通过 Cloudflare 核心边缘交换节点测量物理网卡的真实吞吐带宽与链路抖动",
                        color = SleekTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Core visual dial block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekCardNormal, RoundedCornerShape(28.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated dial
                SpeedometerGauge(
                    speedMbps = currentSpeedMbps,
                    maxSpeedExpectation = if (currentSpeedMbps > 100.0) 500.0 else 100.0
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress state caption
                val statusText = when (speedTestState) {
                    com.example.viewmodel.SpeedTestState.Idle -> "准备就绪 (Ready to Benchmark)"
                    com.example.viewmodel.SpeedTestState.TestingPing -> "建立边缘节点物理握手极速延迟测量中..."
                    com.example.viewmodel.SpeedTestState.TestingDownload -> "正在高速拉取临时物理数据块: ${(downloadProgress * 100).toInt()}%"
                    is com.example.viewmodel.SpeedTestState.Completed -> "测速完成 (Benchmark Complete)"
                    is com.example.viewmodel.SpeedTestState.Error -> "测速中断 (Benchmark Aborted)"
                }

                Text(
                    text = statusText,
                    color = SleekTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Horizontal Linear Progress indicator
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SleekPrimary,
                    trackColor = SleekBorder.copy(alpha = 0.3f)
                )
            }
        }

        // Metric summaries
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ping card
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ping Icon",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "链路延迟", color = SleekTextMuted, fontSize = 10.sp)
                    Text(
                        text = if (pingMs > 0.0) "%.1f ms".format(pingMs) else "--",
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Jitter card
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Jitter Icon",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "抖动 Jitter", color = SleekTextMuted, fontSize = 10.sp)
                    Text(
                        text = if (jitterMs > 0.0) "%.1f ms".format(jitterMs) else "--",
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Peak / final Average speed card
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Average Speed Icon",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "计算带宽", color = SleekTextMuted, fontSize = 10.sp)
                    Text(
                        text = if (speedTestState is com.example.viewmodel.SpeedTestState.Completed) "%.1f Mbps".format(currentSpeedMbps) else "--",
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action Buttons
        Spacer(modifier = Modifier.height(18.dp))
        when (speedTestState) {
            com.example.viewmodel.SpeedTestState.Idle, is com.example.viewmodel.SpeedTestState.Completed, is com.example.viewmodel.SpeedTestState.Error -> {
                Button(
                    onClick = { viewModel.startSpeedTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_speedtest_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Test",
                        tint = M3White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (speedTestState is com.example.viewmodel.SpeedTestState.Completed) "重新进行带宽测试" else "启动物理网络测速",
                        color = M3White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            com.example.viewmodel.SpeedTestState.TestingPing, com.example.viewmodel.SpeedTestState.TestingDownload -> {
                Button(
                    onClick = { viewModel.resetSpeedTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = M3Red),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("stop_speedtest_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Test",
                        tint = M3White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重置并强行中断",
                        color = M3White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Error message card
        if (speedTestState is com.example.viewmodel.SpeedTestState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, M3Red.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed Speed",
                        tint = M3Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = (speedTestState as com.example.viewmodel.SpeedTestState.Error).message,
                        color = SleekTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: IpViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val networkType by viewModel.networkType.collectAsState()
    val carrierName by viewModel.carrierName.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        // App Identity Header
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(SleekPrimaryLight, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "App logo",
                        tint = SleekPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "网络助手 · IP DETECTOR v1.5",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )
                Text(
                    text = "集成了多服公网 IP 智能检测、精确定位、Gemini 拓扑漏洞研判与物理吞吐带宽测速于一体的网络效能助理。",
                    color = SleekTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Text(
            text = "功能配置与调试 (App Diagnostics)",
            color = SleekTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                SettingsItem(
                    title = "清空缓存与诊断状态",
                    subtitle = "重洗当前的 IP 属性及物理延迟探测条目",
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.resetSpeedTest()
                        Toast.makeText(context, "缓存及测速结果清理成功", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = SleekBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp))
                SettingsItem(
                    title = "当前连接类型",
                    subtitle = networkType,
                    icon = Icons.Default.Wifi,
                    onClick = {}
                )
                HorizontalDivider(color = SleekBorder.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp))
                SettingsItem(
                    title = "物理 SIM 运营商",
                    subtitle = carrierName,
                    icon = Icons.Default.Build,
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Project about and copyright
        Text(
            text = "研发归属 & 协议 (Copyright Information)",
            color = SleekTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "• 本应用秉承 Edge-to-Edge 沉浸标准，精调 Material 3 设计系统，消除任何无关后台损耗，极速对齐网络分析场景。",
                    color = SleekTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 网络测速采用多网分块实时下载并拟合算法，最大限额使用 4MB 独立临时包，无持久存储消耗或个人隐私泄露风险。",
                    color = SleekTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SleekPrimaryLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SleekPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SleekTextDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = SleekTextMuted,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = SleekTextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CustomChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SleekPrimary else SleekBorder.copy(alpha = 0.2f))
            .border(1.dp, if (selected) SleekPrimary else SleekBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) M3White else SleekTextDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PingDiagnosticScreen(viewModel: IpViewModel, modifier: Modifier = Modifier) {
    val pingState by viewModel.pingDiagnosticState.collectAsState()
    val pingLogs by viewModel.pingDiagnosticLogs.collectAsState()

    var targetInput by remember { mutableStateOf("8.8.8.8") }
    var packetCountSelected by remember { mutableStateOf(4) }

    val presetHosts = listOf(
        Pair("百度 (Baidu)", "baidu.com"),
        Pair("Google DNS", "8.8.8.8"),
        Pair("Cloudflare", "1.1.1.1"),
        Pair("阿里公共DNS", "223.5.5.5"),
        Pair("本地网卡", "127.0.0.1")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        // 1. Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SleekPrimaryLight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Ping Tool",
                        tint = SleekPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ICMP 链路延迟探测诊断",
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "向目标 IP 或域名发送 ICMP 交互请求以精密分析链路传输往返时差",
                        color = SleekTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Control Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "测试目标配置 (Destination Config)",
                    color = SleekTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Input target row
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("目标主机或 IP 地址") },
                    textStyle = LocalTextStyle.current.copy(color = SleekTextDark, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekBg,
                        unfocusedContainerColor = SleekBg,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ping_host_input"),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("请输入域名如 baidu.com 或 IP 如 8.8.8.8", fontSize = 13.sp) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Presets title & chip row
                Text(
                    text = "快速预设 (Presets Selection)",
                    color = SleekTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Wrap Flow-style row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetHosts.forEach { (label, host) ->
                        CustomChip(
                            selected = targetInput == host,
                            label = label,
                            onClick = { targetInput = host }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Packets Count Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "请求数据包数量",
                            color = SleekTextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "要发送的 ICMP 请求包总数",
                            color = SleekTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(4, 8, 15).forEach { count ->
                            CustomChip(
                                selected = packetCountSelected == count,
                                label = "$count 次",
                                onClick = { packetCountSelected = count }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action execution Button
                when (pingState) {
                    com.example.viewmodel.PingDiagnosticState.Running -> {
                        Button(
                            onClick = { viewModel.stopPingDiagnostic() },
                            colors = ButtonDefaults.buttonColors(containerColor = M3Red),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ping_stop_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Ping",
                                tint = M3White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("停止诊断 (Abort Ping)", color = M3White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                if (targetInput.isNotBlank()) {
                                    viewModel.startPingDiagnostic(targetInput.trim(), packetCountSelected)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ping_start_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Ping",
                                tint = M3White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("启动链路 Ping 诊断", color = M3White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 3. Live terminal output
        if (pingState != com.example.viewmodel.PingDiagnosticState.Idle) {
            Text(
                text = "诊断终端输出日志 (Diagnostic Live logs)",
                color = SleekTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                    .padding(bottom = 12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val terminalScrollState = rememberScrollState()

                    // Auto-scroll logic as logs append
                    LaunchedEffect(pingLogs.size) {
                        terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(terminalScrollState)
                    ) {
                        pingLogs.forEach { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.startsWith("错误:") || logLine.contains("超时") || logLine.contains("失败")) Color(0xFFF44336) else if (logLine.contains("统计信息") || logLine.contains("往返") || logLine.contains("已发送")) Color(0xFF4CAF50) else Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Summaries & stats indicator panel
        if (pingState is com.example.viewmodel.PingDiagnosticState.Completed) {
            val stats = pingState as com.example.viewmodel.PingDiagnosticState.Completed

            Text(
                text = "可视化度量数据 (Analytical Visual Metrics)",
                color = SleekTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            // Dynamic grid list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Packet loss metric card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "传输丢包率", color = SleekTextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                            CircularProgressIndicator(
                                progress = { (stats.packetsReceived.toFloat() / stats.packetsSent.toFloat()).coerceIn(0f, 1f) },
                                color = if (stats.lossRate > 20.0) M3Red else SleekPrimary,
                                strokeWidth = 5.dp,
                                trackColor = SleekBorder.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "${stats.lossRate.toInt()}%",
                                color = SleekTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stats.packetsReceived}/${stats.packetsSent} 成功",
                            color = SleekTextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Average RTT Latency card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "平均行程时延", color = SleekTextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "%.1f ms".format(stats.avgRtt),
                            color = if (stats.avgRtt > 150.0) M3Red else SleekPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "物理抖动阻抗良好",
                            color = SleekTextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Extrema RTT latency card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekCardNormal),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "物理往返阶梯", color = SleekTextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "最短: %.1f ms".format(stats.minRtt), color = SleekTextDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(M3Red, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "最长: %.1f ms".format(stats.maxRtt), color = SleekTextDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "时差: %.1f ms".format(stats.maxRtt - stats.minRtt),
                            color = SleekTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

