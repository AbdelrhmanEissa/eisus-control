package com.eisus.control

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Luxury Liquid Glass Color Palette
private val GlassDarkBg = Color(0xFF090D16)
private val GlassSurface = Color(0xFF131B2B)
private val GlassSurfaceElevated = Color(0xFF1A2438)
private val GlassBorder = Color(0x33FFFFFF)
private val GlassBorderSubtle = Color(0x1AFFFFFF)
private val EisusOrange = Color(0xFFFF5722)
private val EisusOrangeGlow = Color(0xFFFF7043)
private val EisusGreen = Color(0xFF10B981)
private val EisusBlue = Color(0xFF0EA5E9)

class MainActivity : ComponentActivity() {
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("eisus_control_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = EisusOrange,
                        primaryContainer = Color(0x33FF5722),
                        secondary = EisusBlue,
                        background = GlassDarkBg,
                        surface = GlassSurface,
                        onSurface = Color.White
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = GlassDarkBg
                    ) {
                        ControlApp(prefs)
                    }
                }
            }
        }
    }
}

@Composable
fun ControlApp(prefs: SharedPreferences) {
    var savedHubId by remember { mutableStateOf(prefs.getString("hub_id", null)) }
    var savedToken by remember { mutableStateOf(prefs.getString("auth_token", null)) }
    var savedUser by remember { mutableStateOf(prefs.getString("user_name", null)) }

    if (savedToken.isNullOrBlank() || savedHubId.isNullOrBlank()) {
        ClientLoginScreen(
            onLoginSuccess = { hubId, token, userName ->
                prefs.edit()
                    .putString("hub_id", hubId)
                    .putString("auth_token", token)
                    .putString("user_name", userName)
                    .apply()
                savedHubId = hubId
                savedToken = token
                savedUser = userName
            }
        )
    } else {
        ClientDashboardScreen(
            hubId = savedHubId!!,
            token = savedToken!!,
            userName = savedUser ?: "العميل",
            onLogout = {
                prefs.edit().clear().apply()
                savedToken = null
                savedHubId = null
                savedUser = null
            }
        )
    }
}

@Composable
fun ClientLoginScreen(onLoginSuccess: (hubId: String, token: String, userName: String) -> Unit) {
    val scope = rememberCoroutineScope()
    var hubIdInput by remember { mutableStateOf("id-de6eff0f") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), GlassDarkBg),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Brand Icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Surface(
                shape = CircleShape,
                color = Color(0x22FF5722),
                border = BorderStroke(1.5.dp, EisusOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size((76 * pulseScale).dp)
                    .shadow(16.dp, CircleShape, spotColor = EisusOrange)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = EisusOrange
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "EISUS Control",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "بوابتك السحابية الذكية للتحكم بجميع أجهزتك والشاشة عن بعد",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(28.dp))

            // Liquid Glass Card
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = Color.Black),
                shape = RoundedCornerShape(26.dp),
                color = GlassSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hub ID Input
                    OutlinedTextField(
                        value = hubIdInput,
                        onValueChange = { hubIdInput = it.trim(); errorMessage = null },
                        label = { Text("معرّف الشاشة (Hub ID)", color = Color(0xFFCBD5E1)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Router, null, tint = EisusOrange) },
                        supportingText = { Text("مثال: id-de6eff0f", color = Color(0xFF64748B), fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EisusOrange,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE2E8F0),
                            focusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                            unfocusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Username Input
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.trim(); errorMessage = null },
                        label = { Text("اسم المستخدم أو البريد الإلكتروني", color = Color(0xFFCBD5E1)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = EisusOrange) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EisusOrange,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE2E8F0),
                            focusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                            unfocusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("كلمة المرور", color = Color(0xFFCBD5E1)) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = EisusOrange) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EisusOrange,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE2E8F0),
                            focusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                            unfocusedContainerColor = GlassSurfaceElevated.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Error Box
                    errorMessage?.let {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33EF4444),
                            border = BorderStroke(1.dp, Color(0x66EF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                                Text(
                                    text = it,
                                    color = Color(0xFFFCA5A5),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Login Button with Gradient
                    Button(
                        onClick = {
                            val cleanHub = hubIdInput.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
                            val cleanId = if (cleanHub.contains(".")) cleanHub.substringBefore(".") else cleanHub
                            if (cleanId.isBlank() || usernameInput.isBlank() || passwordInput.isBlank()) {
                                errorMessage = "جميع الحقول مطلوبة لتسجيل الدخول"
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val result = executeHubLogin(cleanId, usernameInput, passwordInput)
                                isLoading = false
                                if (result.isSuccess) {
                                    val (token, displayName) = result.getOrThrow()
                                    onLoginSuccess(cleanId, token, displayName)
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول"
                                }
                            }
                        },
                        enabled = !isLoading && hubIdInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = EisusOrange),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EisusOrange,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Bolt, null, Modifier.size(20.dp))
                                Text(
                                    "تسجيل الدخول والربط السحابي",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ClientDashboardScreen(
    hubId: String,
    token: String,
    userName: String,
    onLogout: () -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrlMode by remember { mutableStateOf("hub") } // "hub" or "ha"
    var isPageLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val hubUrl = "https://$hubId.eisus.online/?token=$token"
    val haUrl = "https://ha-$hubId.eisus.online"

    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Box(Modifier.fillMaxSize().background(GlassDarkBg)) {
        Column(Modifier.fillMaxSize()) {
            // Liquid Glass Floating Top Bar (animated visibility in fullscreen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color.Black),
                    shape = RoundedCornerShape(22.dp),
                    color = GlassSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // User Badge & Online Glow Dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.widthIn(max = 75.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EisusGreen, CircleShape)
                                    .shadow(6.dp, CircleShape, spotColor = EisusGreen)
                            )
                            Column {
                                Text(
                                    text = userName.split(" ").firstOrNull() ?: userName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp),
                                    maxLines = 1
                                )
                                Text(
                                    text = hubId,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color(0xFF94A3B8)),
                                    maxLines = 1
                                )
                            }
                        }

                        // Ultra-Modern Segmented Glass Switcher: [ EISUS Hub | Home Assistant ]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = GlassDarkBg.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, GlassBorderSubtle),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // EISUS Tab
                                val hubSelected = currentUrlMode == "hub"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(11.dp))
                                        .then(
                                            if (hubSelected) Modifier.background(Brush.horizontalGradient(listOf(EisusOrange, EisusOrangeGlow)))
                                            else Modifier
                                        )
                                        .clickable {
                                            if (currentUrlMode != "hub") {
                                                currentUrlMode = "hub"
                                                webViewInstance?.loadUrl(hubUrl)
                                            }
                                        }
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            Icons.Filled.Bolt,
                                            null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (hubSelected) Color.White else Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "EISUS",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (hubSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (hubSelected) Color.White else Color(0xFF94A3B8),
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }

                                // Home Assistant Tab
                                val haSelected = currentUrlMode == "ha"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(11.dp))
                                        .then(
                                            if (haSelected) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8))))
                                            else Modifier
                                        )
                                        .clickable {
                                            if (currentUrlMode != "ha") {
                                                currentUrlMode = "ha"
                                                webViewInstance?.loadUrl(haUrl)
                                            }
                                        }
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            Icons.Filled.Home,
                                            null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (haSelected) Color.White else Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "Home Assistant",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (haSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (haSelected) Color.White else Color(0xFF94A3B8),
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Action Icons: Refresh, Fullscreen, Logout
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            // Refresh
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isPageLoading) 360f else 0f,
                                animationSpec = tween(600, easing = LinearEasing),
                                label = "rotate"
                            )
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "تحديث",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp).rotate(rotationAngle)
                                )
                            }

                            // Fullscreen
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = "ملء الشاشة",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Logout
                            IconButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "خروج",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // High Precision Neon Loading Line
            if (isPageLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (currentUrlMode == "hub") EisusOrange else Color(0xFF0284C7),
                    trackColor = Color(0x22FFFFFF)
                )
            }

            // Main Web Frame
            if (hasError) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.padding(24.dp).widthIn(max = 380.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = GlassSurface,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(shape = CircleShape, color = Color(0x33EF4444), modifier = Modifier.size(64.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.WifiOff, null, Modifier.size(32.dp), tint = Color(0xFFF87171))
                                }
                            }
                            Text(
                                "تعذر الاتصال بالهدف",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text(
                                if (currentUrlMode == "hub") "تأكد من تشغيل شاشة الـ Hub وتوفر اتصال الإنترنت بالسيرفر."
                                else "تأكد من تفعيل استضافة Home Assistant من إعدادات الشاشة وأن الجهاز متاح على الراوتر.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    hasError = false
                                    webViewInstance?.reload()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EisusOrange)
                            ) {
                                Text("إعادة المحاولة الآن")
                            }
                        }
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = "$userAgentString EisusControlApp/2.0"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    // Inject token into localStorage so web interface authenticates seamlessly
                                    view?.evaluateJavascript(
                                        "try { localStorage.setItem('eisus_token', '$token'); } catch(e){}",
                                        null
                                    )
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        isPageLoading = false
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    if (newProgress >= 100) isPageLoading = false
                                }
                            }

                            webViewInstance = this
                            loadUrl(hubUrl)
                        }
                    }
                )
            }
        }

        // Floating Restore Button when in Fullscreen Mode
        if (isFullScreen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(40.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape,
                color = GlassSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                IconButton(onClick = { isFullScreen = false }) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = "استعادة البار",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("هل ترغب بالفعل في الخروج من جلسة التحكم وإلغاء الربط السحابي؟", color = Color(0xFFCBD5E1)) },
            containerColor = GlassSurface,
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، تسجيل الخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

suspend fun executeHubLogin(hubId: String, user: String, pass: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = "https://$hubId.eisus.online/api/v1/auth/login"
        val json = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (response.isSuccessful) {
            val resObj = JSONObject(responseBody)
            val token = resObj.optString("token")
            val userObj = resObj.optJSONObject("user")
            val displayName = userObj?.optString("displayName") ?: user
            if (token.isNotBlank()) {
                Result.success(Pair(token, displayName))
            } else {
                Result.failure(Exception("لم يتم استلام مفتاح توثيق صالح"))
            }
        } else {
            val errorMsg = try {
                JSONObject(responseBody).optString("error", "بيانات الدخول غير صحيحة")
            } catch (_: Exception) {
                "بيانات الدخول غير صحيحة (${response.code})"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(Exception("تعذر الاتصال بـ Hub: ${e.message}"))
    }
}
