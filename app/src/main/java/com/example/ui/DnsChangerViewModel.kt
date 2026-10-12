package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.LoginResult
import com.example.auth.UserSession
import com.example.auth.VerifyResult
import com.example.dns.ConnectionStatus
import com.example.dns.CustomDnsItem
import com.example.dns.CustomDnsRepository
import com.example.dns.DnsPacketUtils
import com.example.dns.DnsPingResult
import com.example.dns.DnsPreset
import com.example.dns.DnsPresets
import com.example.dns.DnsState
import com.example.dns.DnsTester
import com.example.dns.DnsVpnService
import com.example.dns.DnsVpnState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DnsAppUiState(
    // Auth state
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val userSession: UserSession? = null,
    val serverUrlInput: String = AuthManager.DEFAULT_SERVER_URL,
    val kickedOutReason: String? = null,
    val profileAvatarPath: String? = null,

    // Navigation Tab: 0 = Home/Connect, 1 = Servers, 2 = Custom DNS, 3 = Account
    val currentTab: Int = 0,

    // Dynamic Server Presets from admin.php
    val serverPresets: List<DnsPreset> = DnsPresets.presets,
    val isUpdatingServers: Boolean = false,
    val serverUpdateMessage: String? = null,

    // Selected Server details (Names shown only as "سرور ۱", "سرور ۲", no provider names or IPs)
    val selectedServerTitle: String = "سرور ۱",
    val selectedServerCategory: String = "تحریم‌شکن",
    val selectedPresetId: String? = "srv_1",
    val selectedCustomId: String? = null,
    val targetPrimaryDns: String = "178.22.122.100",
    val targetSecondaryDns: String = "185.51.200.2",
    val selectedV2RayLink: String = "",

    // Custom DNS list & Dialog state
    val customServers: List<CustomDnsItem> = emptyList(),
    val showAddCustomDialog: Boolean = false,
    val newCustomNameInput: String = "",
    val newCustomPrimaryInput: String = "",
    val newCustomSecondaryInput: String = "",
    val customDialogError: String? = null,

    // Connection & Telemetry
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isVpnMode: Boolean = false,
    val isTestingPing: Boolean = false,
    val pingMs: Long? = null,
    val pingDetail: String? = null,
    val isPingReachable: Boolean? = null,
    val uptimeSeconds: Long = 0L,
    val resolvedQueries: Int = 0,
    val statusMessage: String? = null,
    val serverDelays: Map<String, ServerDelayInfo> = emptyMap()
)

data class ServerDelayInfo(
    val latencyMs: Long? = null,
    val isTesting: Boolean = false,
    val error: String? = null
)

private data class AuthFlowState(
    val isLoggedIn: Boolean,
    val isLoggingIn: Boolean,
    val loginError: String?,
    val userSession: UserSession?,
    val kickedOutReason: String?,
    val profileAvatarPath: String?
)

private data class SelectedServerTuple(
    val title: String,
    val category: String,
    val presetId: String?,
    val customId: String?,
    val v2ray: String
)

private data class NavAndServerState(
    val currentTab: Int,
    val selectedServerTitle: String,
    val selectedServerCategory: String,
    val selectedPresetId: String?,
    val selectedCustomId: String?,
    val selectedV2RayLink: String
)

private data class ServerPresetsState(
    val serverPresets: List<DnsPreset>,
    val isUpdatingServers: Boolean,
    val serverUpdateMessage: String?
)

private data class TargetDnsState(
    val primary: String,
    val secondary: String
)

private data class CustomDnsDialogState(
    val customServers: List<CustomDnsItem>,
    val showDialog: Boolean,
    val nameInput: String,
    val primaryInput: String,
    val secondaryInput: String,
    val error: String?
)

private data class PingAndTelemetryState(
    val isTestingPing: Boolean,
    val pingMs: Long?,
    val pingDetail: String?,
    val isPingReachable: Boolean?,
    val uptimeSeconds: Long,
    val serverDelays: Map<String, ServerDelayInfo> = emptyMap()
)

class DnsChangerViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val customDnsRepo = CustomDnsRepository(application)
    private val avatarPrefs = application.getSharedPreferences("meshki_avatar_prefs", Context.MODE_PRIVATE)

    private val defaultPreset = DnsPresets.presets.first()

    // Auth StateFlows - Default true for direct access without login barrier
    private val _isLoggedIn = MutableStateFlow(true)
    private val _isLoggingIn = MutableStateFlow(false)
    private val _loginError = MutableStateFlow<String?>(null)
    private val _userSession = MutableStateFlow<UserSession?>(authManager.getCurrentSession())
    private val _serverUrlInput = MutableStateFlow(authManager.getServerUrl())
    private val _kickedOutReason = MutableStateFlow<String?>(null)
    private val _profileAvatarPath = MutableStateFlow<String?>(
        avatarPrefs.getString("avatar_path", null)?.let { path ->
            if (File(path).exists()) path else null
        }
    )

    // Navigation & Server Selection
    private val _currentTab = MutableStateFlow(0)
    private val _selectedServerTitle = MutableStateFlow(defaultPreset.serverNumber)
    private val _selectedServerCategory = MutableStateFlow(defaultPreset.category)
    private val _selectedPresetId = MutableStateFlow<String?>(defaultPreset.id)
    private val _selectedCustomId = MutableStateFlow<String?>(null)
    private val _selectedV2RayLink = MutableStateFlow(defaultPreset.v2rayLink)

    // Server Presets (Initial fallback from code, or cached from admin.php)
    private val _serverPresets = MutableStateFlow<List<DnsPreset>>(
        authManager.getCachedServers() ?: DnsPresets.presets
    )
    private val _isUpdatingServers = MutableStateFlow(false)
    private val _serverUpdateMessage = MutableStateFlow<String?>(null)

    // Active Target DNS addresses
    private val _targetPrimaryDns = MutableStateFlow(defaultPreset.primaryDns)
    private val _targetSecondaryDns = MutableStateFlow(defaultPreset.secondaryDns)

    // Custom DNS Management
    private val _customServers = MutableStateFlow<List<CustomDnsItem>>(customDnsRepo.getAllCustomServers())
    private val _showAddCustomDialog = MutableStateFlow(false)
    private val _newCustomNameInput = MutableStateFlow("")
    private val _newCustomPrimaryInput = MutableStateFlow("")
    private val _newCustomSecondaryInput = MutableStateFlow("")
    private val _customDialogError = MutableStateFlow<String?>(null)

    // Ping & Telemetry & Real Delay
    private val _isTestingPing = MutableStateFlow(false)
    private val _pingMs = MutableStateFlow<Long?>(null)
    private val _pingDetail = MutableStateFlow<String?>(null)
    private val _isPingReachable = MutableStateFlow<Boolean?>(null)
    private val _uptimeSeconds = MutableStateFlow(0L)
    private val _serverDelays = MutableStateFlow<Map<String, ServerDelayInfo>>(emptyMap())

    private var sessionMonitorJob: Job? = null
    private var uptimeJob: Job? = null

    init {
        if (_isLoggedIn.value) {
            startSessionMonitor()
            refreshServers(manual = false)
        }

        viewModelScope.launch {
            DnsVpnState.state.collect { state ->
                if (state.status == ConnectionStatus.CONNECTED) {
                    startUptimeTimer(state.connectedAtMillis)
                } else {
                    stopUptimeTimer()
                }
            }
        }
    }

    // Profile Avatar Management
    fun setProfileAvatar(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val file = File(context.filesDir, "meshki_profile_avatar.jpg")
                val outputStream = FileOutputStream(file)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val path = file.absolutePath
                avatarPrefs.edit().putString("avatar_path", path).apply()
                _profileAvatarPath.value = path
            } catch (_: Exception) {}
        }
    }

    fun removeProfileAvatar() {
        val context = getApplication<Application>()
        val file = File(context.filesDir, "meshki_profile_avatar.jpg")
        try { file.delete() } catch (_: Exception) {}
        avatarPrefs.edit().remove("avatar_path").apply()
        _profileAvatarPath.value = null
    }

    // Combine partial flows cleanly
    private val authFlow = combine(
        _isLoggedIn,
        _isLoggingIn,
        _loginError,
        _userSession,
        _kickedOutReason
    ) { loggedIn, loggingIn, error, session, kicked ->
        AuthFlowState(loggedIn, loggingIn, error, session, kicked, _profileAvatarPath.value)
    }

    private val serverSelectionFlow = combine(
        _selectedServerTitle,
        _selectedServerCategory,
        _selectedPresetId,
        _selectedCustomId,
        _selectedV2RayLink
    ) { title, cat, presetId, customId, v2ray ->
        SelectedServerTuple(title, cat, presetId, customId, v2ray)
    }

    private val navAndServerFlow = combine(
        _currentTab,
        serverSelectionFlow
    ) { tab, server ->
        NavAndServerState(tab, server.title, server.category, server.presetId, server.customId, server.v2ray)
    }

    private val serverPresetsFlow = combine(
        _serverPresets,
        _isUpdatingServers,
        _serverUpdateMessage
    ) { presets, isUpdating, msg ->
        ServerPresetsState(presets, isUpdating, msg)
    }

    private val targetDnsFlow = combine(
        _targetPrimaryDns,
        _targetSecondaryDns
    ) { prim, sec ->
        TargetDnsState(prim, sec)
    }

    private val customDialogFlow = combine(
        _customServers,
        _showAddCustomDialog,
        _newCustomNameInput,
        _newCustomPrimaryInput,
        _newCustomSecondaryInput
    ) { servers, show, name, prim, sec ->
        CustomDnsDialogState(servers, show, name, prim, sec, _customDialogError.value)
    }

    private data class PingCore(
        val isTesting: Boolean,
        val pingMs: Long?,
        val detail: String?
    )

    private val pingCoreFlow = combine(
        _isTestingPing,
        _pingMs,
        _pingDetail
    ) { isTesting, ping, detail ->
        PingCore(isTesting, ping, detail)
    }

    private val pingAndTelemetryFlow = combine(
        pingCoreFlow,
        _isPingReachable,
        _uptimeSeconds,
        _serverDelays
    ) { core, reachable, uptime, delays ->
        PingAndTelemetryState(
            isTestingPing = core.isTesting,
            pingMs = core.pingMs,
            pingDetail = core.detail,
            isPingReachable = reachable,
            uptimeSeconds = uptime,
            serverDelays = delays
        )
    }

    private val intermediateConfigFlow = combine(
        authFlow,
        navAndServerFlow,
        serverPresetsFlow,
        targetDnsFlow
    ) { auth, nav, srv, target ->
        IntermediateConfig(auth, nav, srv, target)
    }

    private val intermediateStateFlow = combine(
        customDialogFlow,
        pingAndTelemetryFlow,
        DnsVpnState.state,
        _profileAvatarPath
    ) { custom, ping, vpn, avatarPath ->
        IntermediateState(custom, ping, vpn, avatarPath)
    }

    private data class IntermediateConfig(
        val auth: AuthFlowState,
        val nav: NavAndServerState,
        val srv: ServerPresetsState,
        val target: TargetDnsState
    )

    private data class IntermediateState(
        val custom: CustomDnsDialogState,
        val ping: PingAndTelemetryState,
        val vpn: DnsState,
        val avatarPath: String?
    )

    val uiState: StateFlow<DnsAppUiState> = combine(
        intermediateConfigFlow,
        intermediateStateFlow
    ) { config, state ->
        DnsAppUiState(
            isLoggedIn = config.auth.isLoggedIn,
            isLoggingIn = config.auth.isLoggingIn,
            loginError = config.auth.loginError,
            userSession = config.auth.userSession,
            serverUrlInput = AuthManager.DEFAULT_SERVER_URL,
            kickedOutReason = config.auth.kickedOutReason,
            profileAvatarPath = state.avatarPath,
            currentTab = config.nav.currentTab,
            serverPresets = config.srv.serverPresets,
            isUpdatingServers = config.srv.isUpdatingServers,
            serverUpdateMessage = config.srv.serverUpdateMessage,
            selectedServerTitle = config.nav.selectedServerTitle,
            selectedServerCategory = config.nav.selectedServerCategory,
            selectedPresetId = config.nav.selectedPresetId,
            selectedCustomId = config.nav.selectedCustomId,
            targetPrimaryDns = config.target.primary,
            targetSecondaryDns = config.target.secondary,
            selectedV2RayLink = config.nav.selectedV2RayLink,
            customServers = state.custom.customServers,
            showAddCustomDialog = state.custom.showDialog,
            newCustomNameInput = state.custom.nameInput,
            newCustomPrimaryInput = state.custom.primaryInput,
            newCustomSecondaryInput = state.custom.secondaryInput,
            customDialogError = state.custom.error,
            connectionStatus = state.vpn.status,
            isVpnMode = state.vpn.isVpnMode,
            isTestingPing = state.ping.isTestingPing,
            pingMs = state.ping.pingMs,
            pingDetail = state.ping.pingDetail,
            isPingReachable = state.ping.isPingReachable,
            uptimeSeconds = state.ping.uptimeSeconds,
            resolvedQueries = state.vpn.resolvedQueriesCount,
            statusMessage = state.vpn.errorMessage,
            serverDelays = state.ping.serverDelays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DnsAppUiState()
    )

    // -------------------------------------------------------------
    // AUTHENTICATION & LOGIN LOGIC
    // -------------------------------------------------------------
    fun login(usernameInput: String, passwordInput: String = "") {
        val username = usernameInput.trim()
        val password = passwordInput.trim()

        if (username.isBlank()) {
            _loginError.value = "لطفاً نام کاربری را وارد کنید."
            return
        }
        if (password.isBlank()) {
            _loginError.value = "لطفاً رمز عبور را وارد کنید."
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            _kickedOutReason.value = null

            when (val result = authManager.login(username, password)) {
                is LoginResult.Success -> {
                    _isLoggedIn.value = true
                    _userSession.value = result.session
                    if (!result.servers.isNullOrEmpty()) {
                        _serverPresets.value = result.servers
                    }
                    _isLoggingIn.value = false
                    startSessionMonitor()
                }
                is LoginResult.Error -> {
                    _isLoggedIn.value = false
                    _userSession.value = null
                    _loginError.value = result.message
                    _isLoggingIn.value = false
                }
            }
        }
    }

    fun logout() {
        stopSessionMonitor()
        disconnect()
        authManager.logout()
        _isLoggedIn.value = false
        _userSession.value = null
        _loginError.value = null
        _kickedOutReason.value = null
    }

    fun dismissKickAlert() {
        _kickedOutReason.value = null
    }

    fun clearKickedReason() {
        _kickedOutReason.value = null
    }

    suspend fun checkSessionValidity(): Boolean = true

    private fun handleSessionExpired(reason: String) {
        // No-op in standalone mode
    }

    private fun startSessionMonitor() {
        // No-op in standalone mode
    }

    private fun stopSessionMonitor() {
        sessionMonitorJob?.cancel()
        sessionMonitorJob = null
    }

    fun refreshServers(manual: Boolean = false) {
        viewModelScope.launch {
            _isUpdatingServers.value = true
            val fetched = authManager.fetchServers()
            if (!fetched.isNullOrEmpty()) {
                _serverPresets.value = fetched
                if (manual) {
                    _serverUpdateMessage.value = "لیست سرورها با موفقیت بروزرسانی شد (${fetched.size} سرور)."
                }
            } else if (manual) {
                _serverUpdateMessage.value = "سرورهای جدیدی یافت نشد یا سرور پاسخ نداد."
            }
            _isUpdatingServers.value = false
        }
    }

    fun dismissServerUpdateMessage() {
        _serverUpdateMessage.value = null
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun selectPreset(preset: DnsPreset) {
        _selectedPresetId.value = preset.id
        _selectedCustomId.value = null
        _selectedServerTitle.value = preset.serverNumber
        _selectedServerCategory.value = preset.category
        _targetPrimaryDns.value = preset.primaryDns.ifBlank { "1.1.1.1" }
        _targetSecondaryDns.value = preset.secondaryDns.ifBlank { "8.8.8.8" }
        _selectedV2RayLink.value = preset.v2rayLink

        if (DnsVpnState.state.value.status == ConnectionStatus.CONNECTED) {
            connect()
        } else {
            testServerRealDelay(preset)
        }
    }

    fun openAddCustomDialog() {
        _newCustomNameInput.value = ""
        _newCustomPrimaryInput.value = ""
        _newCustomSecondaryInput.value = ""
        _customDialogError.value = null
        _showAddCustomDialog.value = true
    }

    fun closeAddCustomDialog() {
        _showAddCustomDialog.value = false
    }

    fun onCustomNameChanged(value: String) {
        _newCustomNameInput.value = value
        _customDialogError.value = null
    }

    fun onCustomPrimaryChanged(value: String) {
        _newCustomPrimaryInput.value = value
        _customDialogError.value = null
    }

    fun onCustomSecondaryChanged(value: String) {
        _newCustomSecondaryInput.value = value
        _customDialogError.value = null
    }

    fun saveCustomDns() {
        val name = _newCustomNameInput.value.trim()
        val input = _newCustomPrimaryInput.value.trim()
        val secondary = _newCustomSecondaryInput.value.trim()

        if (input.isBlank()) {
            _customDialogError.value = "لطفاً لینک کانفیگ وی‌پی‌ان یا آدرس آی‌پی دی‌ان‌اس را وارد کنید."
            return
        }

        // Check if input is a V2Ray link (vless://, vmess://, trojan://, ss://)
        val isV2RayLink = input.startsWith("vless://", ignoreCase = true) ||
                          input.startsWith("vmess://", ignoreCase = true) ||
                          input.startsWith("trojan://", ignoreCase = true) ||
                          input.startsWith("ss://", ignoreCase = true)

        if (isV2RayLink) {
            val config = com.example.vpn.v2ray.V2RayConfig.parse(input)
            if (config == null) {
                _customDialogError.value = "لینک کانفیگ وی‌پی‌ان نامعتبر است. لطفاً فرمت لینک را بررسی کنید."
                return
            }
            val finalName = if (name.isNotBlank()) name else (config.remark.ifBlank { "کانفیگ ${config.protocol.uppercase()}" })
            val item = customDnsRepo.addCustomServer(
                name = finalName,
                primaryDns = "1.1.1.1",
                secondaryDns = "8.8.8.8",
                serverType = "vpn",
                v2rayLink = input
            )
            _customServers.value = customDnsRepo.getAllCustomServers()
            _showAddCustomDialog.value = false
            selectCustomServer(item)
            return
        }

        // Otherwise treat as DNS IP
        if (name.isBlank()) {
            _customDialogError.value = "لطفاً برای سرور یک نام وارد کنید."
            return
        }

        if (!DnsPacketUtils.isValidIpv4(input)) {
            _customDialogError.value = "آدرس دی‌ان‌اس اول نامعتبر است یا لینک کانفیگ شناخته نشد."
            return
        }

        if (secondary.isNotBlank() && !DnsPacketUtils.isValidIpv4(secondary)) {
            _customDialogError.value = "آدرس دی‌ان‌اس دوم (Secondary) نامعتبر است."
            return
        }

        val item = customDnsRepo.addCustomServer(
            name = name,
            primaryDns = input,
            secondaryDns = secondary,
            serverType = "dns",
            v2rayLink = ""
        )

        _customServers.value = customDnsRepo.getAllCustomServers()
        _showAddCustomDialog.value = false

        selectCustomServer(item)
    }

    fun selectCustomServer(item: CustomDnsItem) {
        _selectedCustomId.value = item.id
        _selectedPresetId.value = null
        _selectedServerTitle.value = item.name
        _selectedServerCategory.value = if (item.serverType == "vpn") "وی پی ان" else "سرور دلخواه"
        _targetPrimaryDns.value = item.primaryDns.ifBlank { "1.1.1.1" }
        _targetSecondaryDns.value = item.secondaryDns.ifBlank { "8.8.8.8" }
        _selectedV2RayLink.value = item.v2rayLink

        if (DnsVpnState.state.value.status == ConnectionStatus.CONNECTED) {
            connect()
        } else {
            testPing(item.primaryDns)
        }
    }

    fun deleteCustomServer(id: String) {
        customDnsRepo.deleteCustomServer(id)
        _customServers.value = customDnsRepo.getAllCustomServers()
        if (_selectedCustomId.value == id) {
            val fallback = _serverPresets.value.firstOrNull() ?: defaultPreset
            selectPreset(fallback)
        }
    }

    fun toggleConnection() {
        val currentStatus = DnsVpnState.state.value.status
        if (currentStatus == ConnectionStatus.CONNECTED || currentStatus == ConnectionStatus.CONNECTING) {
            disconnect()
        } else {
            viewModelScope.launch {
                val isValid = checkSessionValidity()
                if (isValid) {
                    connect()
                }
            }
        }
    }

    fun connect() {
        val context = getApplication<Application>()
        val isVpn = _selectedServerCategory.value == "وی پی ان" || _selectedV2RayLink.value.isNotBlank()

        if (isVpn) {
            DnsVpnService.startVpn(
                context = context,
                v2rayLink = _selectedV2RayLink.value,
                serverTitle = _selectedServerTitle.value
            )
            return
        }

        val primary = _targetPrimaryDns.value.trim()
        val secondary = _targetSecondaryDns.value.trim()

        if (!DnsPacketUtils.isValidIpv4(primary)) {
            return
        }

        DnsVpnService.start(
            context = context,
            primaryDns = primary,
            secondaryDns = secondary,
            serverTitle = _selectedServerTitle.value
        )
    }

    fun disconnect() {
        val context = getApplication<Application>()
        DnsVpnState.updateDisconnected()
        DnsVpnService.stop(context)
    }

    fun testServerRealDelay(preset: DnsPreset) {
        viewModelScope.launch {
            _serverDelays.value = _serverDelays.value + (preset.id to ServerDelayInfo(isTesting = true))
            val result: DnsPingResult = if (preset.category == "وی پی ان" || preset.v2rayLink.isNotBlank()) {
                DnsTester.testV2RayDelay(preset.v2rayLink)
            } else {
                val ip = preset.primaryDns.ifBlank { "1.1.1.1" }
                DnsTester.pingDns(ip)
            }

            _serverDelays.value = _serverDelays.value + (preset.id to ServerDelayInfo(
                latencyMs = result.latencyMs,
                isTesting = false,
                error = if (!result.isReachable) result.detail else null
            ))

            if (_selectedPresetId.value == preset.id) {
                _pingMs.value = result.latencyMs
                _pingDetail.value = result.detail
                _isPingReachable.value = result.isReachable
                DnsVpnState.updatePing(result.latencyMs)
            }
        }
    }

    fun testAllServersRealDelay() {
        val currentPresets = _serverPresets.value
        currentPresets.forEach { preset ->
            testServerRealDelay(preset)
        }
    }

    fun testPing(targetIp: String? = null) {
        val isVpn = _selectedServerCategory.value == "وی پی ان" || _selectedV2RayLink.value.isNotBlank()

        viewModelScope.launch {
            _isTestingPing.value = true
            val result: DnsPingResult = if (isVpn) {
                DnsTester.testV2RayDelay(_selectedV2RayLink.value)
            } else {
                val ipToTest = (targetIp ?: _targetPrimaryDns.value).trim()
                if (DnsPacketUtils.isValidIpv4(ipToTest)) {
                    DnsTester.pingDns(ipToTest)
                } else {
                    DnsPingResult(null, false, "آدرس دی‌ان‌اس نامعتبر است")
                }
            }

            _pingMs.value = result.latencyMs
            _pingDetail.value = result.detail
            _isPingReachable.value = result.isReachable
            DnsVpnState.updatePing(result.latencyMs)

            _selectedPresetId.value?.let { currentId ->
                _serverDelays.value = _serverDelays.value + (currentId to ServerDelayInfo(
                    latencyMs = result.latencyMs,
                    isTesting = false,
                    error = if (!result.isReachable) result.detail else null
                ))
            }
            _isTestingPing.value = false
        }
    }

    private fun startUptimeTimer(connectedAt: Long) {
        uptimeJob?.cancel()
        uptimeJob = viewModelScope.launch {
            while (isActive) {
                val diff = (System.currentTimeMillis() - connectedAt) / 1000L
                _uptimeSeconds.value = diff.coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    private fun stopUptimeTimer() {
        uptimeJob?.cancel()
        uptimeJob = null
        _uptimeSeconds.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        stopSessionMonitor()
        stopUptimeTimer()
    }
}
