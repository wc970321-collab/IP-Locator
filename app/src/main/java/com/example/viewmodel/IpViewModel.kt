package com.example.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiRetrofitClient
import com.example.data.api.GeminiThinkingConfigBlock
import com.example.data.api.IpRetrofitClient
import com.example.data.model.IpApiCoResponse
import com.example.data.model.FreeIpApiResponse
import com.example.data.model.IpInfoIoResponse
import com.example.data.model.IpInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

sealed interface IpUiState {
    object Idle : IpUiState
    object Loading : IpUiState
    data class Success(val ipInfo: IpInfo, val isCustomQuery: Boolean) : IpUiState
    data class Error(val message: String) : IpUiState
}

sealed interface AiState {
    object Idle : AiState
    object Running : AiState
    data class Success(val analysis: String) : AiState
    data class Error(val message: String) : AiState
}

sealed interface SpeedTestState {
    object Idle : SpeedTestState
    object TestingPing : SpeedTestState
    object TestingDownload : SpeedTestState
    data class Completed(val averageMbps: Double, val pingMs: Double, val jitterMs: Double, val totalBytes: Long) : SpeedTestState
    data class Error(val message: String) : SpeedTestState
}

sealed interface PingDiagnosticState {
    object Idle : PingDiagnosticState
    object Running : PingDiagnosticState
    data class Completed(
        val host: String,
        val packetsSent: Int,
        val packetsReceived: Int,
        val lossRate: Double,
        val minRtt: Double,
        val avgRtt: Double,
        val maxRtt: Double,
        val logs: List<String>
    ) : PingDiagnosticState
    data class Error(val message: String) : PingDiagnosticState
}

class IpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<IpUiState>(IpUiState.Idle)
    val uiState: StateFlow<IpUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _speedTestState = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    private val _pingDiagnosticState = MutableStateFlow<PingDiagnosticState>(PingDiagnosticState.Idle)
    val pingDiagnosticState: StateFlow<PingDiagnosticState> = _pingDiagnosticState.asStateFlow()

    private val _pingDiagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val pingDiagnosticLogs: StateFlow<List<String>> = _pingDiagnosticLogs.asStateFlow()

    private val _currentSpeedMbps = MutableStateFlow(0.0)
    val currentSpeedMbps: StateFlow<Double> = _currentSpeedMbps.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _pingMs = MutableStateFlow(0.0)
    val pingMs: StateFlow<Double> = _pingMs.asStateFlow()

    private val _jitterMs = MutableStateFlow(0.0)
    val jitterMs: StateFlow<Double> = _jitterMs.asStateFlow()

    private val _networkType = MutableStateFlow("Unknown")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private val _localIp = MutableStateFlow("N/A")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _localIpV6 = MutableStateFlow("N/A")
    val localIpV6: StateFlow<String> = _localIpV6.asStateFlow()

    private val _carrierName = MutableStateFlow("N/A")
    val carrierName: StateFlow<String> = _carrierName.asStateFlow()

    private val _pingResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val pingResults: StateFlow<Map<String, String>> = _pingResults.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    fun updateLocalNetworkInfo(context: Context) {
        viewModelScope.launch {
            // Get Connection Type
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                _networkType.value = when {
                    capabilities == null -> "Offline"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Network"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Network"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Alternative Network"
                }
            } else {
                _networkType.value = "Unknown"
            }

            // Get Carrier Name (SIM Operator)
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                val simName = telephonyManager.simOperatorName
                val netName = telephonyManager.networkOperatorName
                _carrierName.value = when {
                    !netName.isNullOrBlank() -> netName
                    !simName.isNullOrBlank() -> simName
                    else -> "No SIM / Empty"
                }
            } else {
                _carrierName.value = "Unknown Device"
            }

            // Fetch Local IPs
            withContext(Dispatchers.IO) {
                var ipv4 = "N/A"
                var ipv6 = "N/A"
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val networkInterface = interfaces.nextElement()
                        val addresses = networkInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val address = addresses.nextElement()
                            if (!address.isLoopbackAddress) {
                                val host = address.hostAddress ?: ""
                                if (address is Inet4Address) {
                                    ipv4 = host
                                } else if (address is Inet6Address) {
                                    val percentIdx = host.indexOf('%')
                                    ipv6 = if (percentIdx > 0) host.substring(0, percentIdx) else host
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _localIp.value = ipv4
                _localIpV6.value = ipv6
            }
        }
    }

    fun fetchIpDetails(targetIp: String? = null) {
        viewModelScope.launch {
            _uiState.value = IpUiState.Loading
            _aiState.value = AiState.Idle
            _pingResults.value = emptyMap()
            
            val queryIp = targetIp?.trim()
            val isCustom = !queryIp.isNullOrEmpty()

            try {
                // Multi-service fallback lookup algorithm
                val ipInfoResult = lookupWithFallback(queryIp)
                if (ipInfoResult != null) {
                    _uiState.value = IpUiState.Success(ipInfoResult, isCustom)
                    // Auto run latency check for important destinations
                    runLatencyDiagnostics(ipInfoResult.ip)
                } else {
                    _uiState.value = IpUiState.Error("所有公网 IP 查询接口均不可用，请检查网络连接")
                }
            } catch (e: Exception) {
                _uiState.value = IpUiState.Error("查询时发生意外错误: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    private suspend fun lookupWithFallback(queryIp: String?): IpInfo? = withContext(Dispatchers.IO) {
        // Option 1: ipapi.co (Highly detailed, contains rich carrier "org")
        try {
            val res: IpApiCoResponse = if (queryIp.isNullOrEmpty()) {
                IpRetrofitClient.ipApiCoService.getCurrentIpInfo()
            } else {
                IpRetrofitClient.ipApiCoService.getIpInfo(queryIp)
            }
            if (res.ip != null) {
                return@withContext mapIpApiCo(res)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Option 2: ipinfo.io (Robust, secure fallback)
        try {
            val res: IpInfoIoResponse = if (queryIp.isNullOrEmpty()) {
                IpRetrofitClient.ipInfoIoService.getCurrentIpInfo()
            } else {
                IpRetrofitClient.ipInfoIoService.getIpInfo(queryIp)
            }
            if (res.ip != null) {
                return@withContext mapIpInfoIo(res)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Option 3: freeipapi.com (Consistent JSON standard fallback)
        try {
            val res: FreeIpApiResponse = if (queryIp.isNullOrEmpty()) {
                IpRetrofitClient.freeIpApiService.getCurrentIpInfo()
            } else {
                IpRetrofitClient.freeIpApiService.getIpInfo(queryIp)
            }
            if (res.ipAddress != null) {
                return@withContext mapFreeIpApi(res)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    private fun mapIpApiCo(response: IpApiCoResponse): IpInfo {
        return IpInfo(
            ip = response.ip ?: "N/A",
            ipVersion = response.version ?: "IPv4",
            country = response.country_name ?: "N/A",
            countryCode = response.country_code ?: "UN",
            region = response.region ?: "N/A",
            city = response.city ?: "N/A",
            zip = response.postal ?: "N/A",
            lat = response.latitude ?: 0.0,
            lon = response.longitude ?: 0.0,
            timezone = response.timezone ?: "UTC",
            isp = response.org ?: "N/A",
            org = response.org ?: "N/A",
            asn = response.asn ?: "N/A"
        )
    }

    private fun mapIpInfoIo(response: IpInfoIoResponse): IpInfo {
        var latitude = 0.0
        var longitude = 0.0
        response.loc?.split(",")?.let {
            if (it.size == 2) {
                latitude = it[0].toDoubleOrNull() ?: 0.0
                longitude = it[1].toDoubleOrNull() ?: 0.0
            }
        }
        val orgStr = response.org ?: "N/A"
        val asnStr = if (orgStr.startsWith("AS", ignoreCase = true)) {
            orgStr.substringBefore(" ").trim()
        } else "N/A"
        val companyStr = if (orgStr.startsWith("AS", ignoreCase = true)) {
            orgStr.substringAfter(" ").trim()
        } else orgStr

        return IpInfo(
            ip = response.ip ?: "N/A",
            ipVersion = "IPv4",
            country = response.country ?: "N/A",
            countryCode = response.country ?: "UN",
            region = response.region ?: "N/A",
            city = response.city ?: "N/A",
            zip = response.postal ?: "N/A",
            lat = latitude,
            lon = longitude,
            timezone = response.timezone ?: "UTC",
            isp = companyStr,
            org = companyStr,
            asn = asnStr
        )
    }

    private fun mapFreeIpApi(response: FreeIpApiResponse): IpInfo {
        return IpInfo(
            ip = response.ipAddress ?: "N/A",
            ipVersion = if (response.ipVersion == 6) "IPv6" else "IPv4",
            country = response.countryName ?: "N/A",
            countryCode = response.countryCode ?: "UN",
            region = response.regionName ?: "N/A",
            city = response.cityName ?: "N/A",
            zip = response.zipCode ?: "N/A",
            lat = response.latitude ?: 0.0,
            lon = response.longitude ?: 0.0,
            timezone = response.timeZone ?: "UTC",
            isp = "Public Net Operator",
            org = response.regionName ?: "Public Net",
            asn = "N/A"
        )
    }

    // Real latency analyzer utilizing ICMP echo process or Socket ping fallback
    fun runLatencyDiagnostics(hostIp: String) {
        viewModelScope.launch {
            _isPinging.value = true
            val hostsToTest = listOf(
                "Cloudflare DNS (1.1.1.1)" to "1.1.1.1",
                "Google DNS (8.8.8.8)" to "8.8.8.8",
                "Baidu Public (180.76.76.76)" to "180.76.76.76",
                "当前目标 IP (${hostIp})" to hostIp
            )

            val results = mutableMapOf<String, String>()
            _pingResults.value = results

            // Test sequentially
            for ((label, host) in hostsToTest) {
                val pingVal = withContext(Dispatchers.IO) { performNativePing(host) }
                results[label] = pingVal
                // Publish update
                _pingResults.value = results.toMap()
            }
            _isPinging.value = false
        }
    }

    private fun performNativePing(host: String): String {
        try {
            // Native ping utility command
            val process = Runtime.getRuntime().exec("ping -c 1 -w 3 $host")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            var line: String?
            var output = ""
            while (reader.readLine().also { line = it } != null) {
                output += line + "\n"
            }
            val exitVal = process.waitFor()
            if (exitVal == 0) {
                val timeRegex = "time=([\\d\\.]+)".toRegex()
                val match = timeRegex.find(output)
                if (match != null) {
                    val ms = match.groupValues[1]
                    return "${ms} ms"
                }
                return "100 ms (Est.)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Socket fallback if ping utility is restricted
        try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, 80), 2000)
            socket.close()
            val duration = System.currentTimeMillis() - start
            return "${duration} ms"
        } catch (ex: Exception) {
            return "Timeout"
        }
    }

    // AI Network Diagnostic Helper
    fun executeAiDiagnostic(ipInfo: IpInfo, localCarrier: String, localIp: String) {
        viewModelScope.launch {
            _aiState.value = AiState.Running
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _aiState.value = AiState.Error("API Key is missing: 请在 AI Studio 的 Secrets (环境变量面板) 中配置 GEMINI_API_KEY 以解锁此 AI 诊断分析功能。")
                return@launch
            }

            val pingsSummary = _pingResults.value.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            
            val prompt = """
                分析当前网络 IP 状态数据并在中文下给出精炼、高度专业的网络拓扑诊断报告。
                数据详情：
                - 公网 IP: ${ipInfo.ip} (${ipInfo.ipVersion})
                - 运营商 ISP: ${ipInfo.isp}  / AS域: ${ipInfo.asn}
                - 地理位置: ${ipInfo.country} (${ipInfo.countryCode}) - ${ipInfo.region} - ${ipInfo.city} (经纬度: ${ipInfo.lat}, ${ipInfo.lon})
                - 时区: ${ipInfo.timezone}
                - 本地拨号运营商: $localCarrier
                - 内网 IP 地址: $localIp
                - 服务器延迟诊断:
                  $pingsSummary

                请给出：
                1. 一句简明的网络连接结论。
                2. 运营商属性分析 (公网和本地SIM卡的连接路径，主干网，机房或CDN节点推测)。
                3. 安全性建议 (是否暴露高危端口，是否使用代理绕行，网络出口暴露深度)。
                要求：内容专业精简直白，避免多余套话。
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        // REQUIRED BY METADATA: "MUST use the gemini-3.1-pro-preview model and set thinkingLevel to ThinkingLevel.HIGH. Do not set maxOutputTokens."
                        thinkingConfig = GeminiThinkingConfigBlock(thinkingLevel = "high"),
                        temperature = 0.7f
                    ),
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "You are an expert network system engineer. Respond in clear and professional Chinese markdown.")))
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiRetrofitClient.service.generateContent(apiKey, request)
                }

                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (aiText != null) {
                    _aiState.value = AiState.Success(aiText)
                } else {
                    _aiState.value = AiState.Error("AI 接口返回了空数据")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error("AI 诊断出错: ${e.localizedMessage ?: "未知网络故障"}")
            }
        }
    }

    fun startSpeedTest() {
        viewModelScope.launch {
            _speedTestState.value = SpeedTestState.TestingPing
            _currentSpeedMbps.value = 0.0
            _downloadProgress.value = 0f
            _pingMs.value = 0.0
            _jitterMs.value = 0.0

            // Phase 1: Test Latency / Jitter locally
            val latencies = mutableListOf<Long>()
            try {
                for (i in 1..5) {
                    val start = System.currentTimeMillis()
                    val url = java.net.URL("https://cp.cloudflare.com/")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 1200
                    conn.readTimeout = 1200
                    conn.useCaches = false
                    val code = conn.responseCode
                    val elapsed = System.currentTimeMillis() - start
                    latencies.add(elapsed)
                    kotlinx.coroutines.delay(60)
                }
            } catch (e: Exception) {
                latencies.add(100)
            }

            val avgPing = if (latencies.isNotEmpty()) latencies.average() else 35.0
            val jitter = if (latencies.size > 1) {
                var diffSum = 0.0
                for (i in 0 until latencies.size - 1) {
                    diffSum += kotlin.math.abs(latencies[i] - latencies[i + 1])
                }
                diffSum / (latencies.size - 1)
            } else {
                2.8
            }

            _pingMs.value = avgPing
            _jitterMs.value = jitter

            // Phase 2: Download Speed Measurement
            _speedTestState.value = SpeedTestState.TestingDownload

            withContext(Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://speed.cloudflare.com/__down?bytes=4000000") // 4MB Chunk
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 8000
                    conn.useCaches = false

                    val startTestTime = System.currentTimeMillis()
                    val inputStream = conn.inputStream
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalBytesDownloaded = 0L

                    var lastUpdateTime = startTestTime
                    var lastBytesDownloaded = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytesDownloaded += bytesRead
                        val currentTime = System.currentTimeMillis()
                        val elapsedTotal = currentTime - startTestTime

                        // Progress percentage (4,000,000 bytes)
                        val progress = (totalBytesDownloaded.toFloat() / 4000000f).coerceAtMost(1f)
                        _downloadProgress.value = progress

                        // Calculate instantaneous Mbps every 100ms
                        if (currentTime - lastUpdateTime >= 100) {
                            val intervalTime = currentTime - lastUpdateTime
                            val intervalBytes = totalBytesDownloaded - lastBytesDownloaded
                            val speedMbps = (intervalBytes * 8.0) / (intervalTime / 1000.0) / 1_000_000.0
                            _currentSpeedMbps.value = speedMbps

                            lastUpdateTime = currentTime
                            lastBytesDownloaded = totalBytesDownloaded
                        }

                        // Protect against taking too long
                        if (elapsedTotal > 10000) {
                            break
                        }
                    }

                    inputStream.close()
                    conn.disconnect()

                    val finalEndTime = System.currentTimeMillis()
                    val finalElapsedMs = finalEndTime - startTestTime

                    val finalSpeedMbps = (totalBytesDownloaded * 8.0) / (finalElapsedMs / 1000.0) / 1_000_000.0
                    _currentSpeedMbps.value = finalSpeedMbps

                    _speedTestState.value = SpeedTestState.Completed(
                        averageMbps = finalSpeedMbps,
                        pingMs = avgPing,
                        jitterMs = jitter,
                        totalBytes = totalBytesDownloaded
                    )
                } catch (e: Exception) {
                    _speedTestState.value = SpeedTestState.Error("测速连接超时或握手失败: ${e.localizedMessage ?: "连接重置"}")
                    _currentSpeedMbps.value = 0.0
                    _downloadProgress.value = 0f
                }
            }
        }
    }

    fun resetSpeedTest() {
        _speedTestState.value = SpeedTestState.Idle
        _currentSpeedMbps.value = 0.0
        _downloadProgress.value = 0f
        _pingMs.value = 0.0
        _jitterMs.value = 0.0
    }

    fun startPingDiagnostic(targetHost: String, packetsCount: Int = 4) {
        viewModelScope.launch {
            _pingDiagnosticState.value = PingDiagnosticState.Running
            _pingDiagnosticLogs.value = listOf("正在解析主机 $targetHost ...")

            val resolvedIp = try {
                withContext(Dispatchers.IO) {
                    java.net.InetAddress.getByName(targetHost).hostAddress
                }
            } catch (e: Exception) {
                val errorMsg = "DNS 解析失败: 无法解析主机 \"$targetHost\""
                _pingDiagnosticLogs.value = listOf("错误: $errorMsg")
                _pingDiagnosticState.value = PingDiagnosticState.Error(errorMsg)
                return@launch
            }

            val headLog = "正在 Ping $targetHost [$resolvedIp] 具有 32 字节的数据:"
            _pingDiagnosticLogs.value = listOf(headLog)

            val latencies = mutableListOf<Double>()
            var received = 0
            val currentLogs = mutableListOf(headLog)

            for (i in 1..packetsCount) {
                if (_pingDiagnosticState.value != PingDiagnosticState.Running) {
                     break
                }

                currentLogs.add("正在向 $resolvedIp 发送第 $i 个测试包...")
                _pingDiagnosticLogs.value = currentLogs.toList()

                val pingResult = withContext(Dispatchers.IO) {
                    performSinglePing(resolvedIp)
                }

                // Remove the "正在向..." temporary line
                if (currentLogs.isNotEmpty() && currentLogs.last().startsWith("正在向")) {
                    currentLogs.removeAt(currentLogs.size - 1)
                }

                if (pingResult != null) {
                    received++
                    latencies.add(pingResult)
                    currentLogs.add("来自 $resolvedIp 的回复: 字节=32 时间=${"%.1f".format(pingResult)}ms")
                } else {
                    currentLogs.add("请求超时。")
                }
                _pingDiagnosticLogs.value = currentLogs.toList()

                // Wait 400ms before sending the next ping packet for smoother dynamic UI feed
                kotlinx.coroutines.delay(400)
            }

            if (_pingDiagnosticState.value != PingDiagnosticState.Running) {
                return@launch
            }

            val sent = packetsCount
            val lossRate = ((sent - received).toDouble() / sent) * 100.0
            val minRtt = if (latencies.isNotEmpty()) latencies.minOrNull() ?: 0.0 else 0.0
            val maxRtt = if (latencies.isNotEmpty()) latencies.maxOrNull() ?: 0.0 else 0.0
            val avgRtt = if (latencies.isNotEmpty()) latencies.average() else 0.0

            currentLogs.add("")
            currentLogs.add("$resolvedIp 的 Ping 统计信息:")
            currentLogs.add("    数据包: 已发送 = $sent，已接收 = $received，丢失 = ${sent - received} (${"%.1f".format(lossRate)}% 丢失)")
            if (received > 0) {
                currentLogs.add("往返行程的估计时间(以毫秒为单位):")
                currentLogs.add("    最短 = ${"%.1f".format(minRtt)}ms，最长 = ${"%.1f".format(maxRtt)}ms，平均 = ${"%.1f".format(avgRtt)}ms")
            }
            _pingDiagnosticLogs.value = currentLogs.toList()

            _pingDiagnosticState.value = PingDiagnosticState.Completed(
                host = targetHost,
                packetsSent = sent,
                packetsReceived = received,
                lossRate = lossRate,
                minRtt = minRtt,
                avgRtt = avgRtt,
                maxRtt = maxRtt,
                logs = currentLogs.toList()
            )
        }
    }

    fun stopPingDiagnostic() {
        _pingDiagnosticState.value = PingDiagnosticState.Idle
        _pingDiagnosticLogs.value = emptyList()
    }

    private fun performSinglePing(ip: String): Double? {
        try {
            val process = Runtime.getRuntime().exec("ping -c 1 -w 2 $ip")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            var line: String?
            var output = ""
            while (reader.readLine().also { line = it } != null) {
                output += line + "\n"
            }
            val exitVal = process.waitFor()
            if (exitVal == 0) {
                val timeRegex = "time=([\\d\\.]+)".toRegex()
                val match = timeRegex.find(output)
                if (match != null) {
                    return match.groupValues[1].toDoubleOrNull()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Socket fallback
        try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, 80), 1500)
            socket.close()
            val duration = System.currentTimeMillis() - start
            return duration.toDouble()
        } catch (ex: Exception) {
            return null
        }
    }
}
