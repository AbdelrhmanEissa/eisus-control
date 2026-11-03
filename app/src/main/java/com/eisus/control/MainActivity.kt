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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

class MainActivity : ComponentActivity() {
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("eisus_control_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFFEA580C),
                        primaryContainer = Color(0xFFFFEDD5),
                        secondary = Color(0xFF0F172A),
                        background = Color(0xFFF8FAFC),
                        surface = Color.White
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "EISUS Control",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = "تحكم بمنزلك الذكي وأجهزتك عن بعد بأمان وسرعة فائقة",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = hubIdInput,
                    onValueChange = { hubIdInput = it.trim(); errorMessage = null },
                    label = { Text("معرّف الشاشة (Hub ID)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Router, null, tint = MaterialTheme.colorScheme.primary) },
                    supportingText = { Text("مثال: id-de6eff0f أو رابط السيرفر", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.trim(); errorMessage = null },
                    label = { Text("اسم المستخدم أو البريد الإلكتروني") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; errorMessage = null },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                errorMessage?.let {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("تسجيل الدخول والربط ⚡", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
    val ctx = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrlMode by remember { mutableStateOf("hub") } // "hub" or "ha"
    var isPageLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    val hubUrl = "https://$hubId.eisus.online/?token=$token"
    val haUrl = "https://ha-$hubId.eisus.online"

    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Sleek Client Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFF10B981).copy(alpha = 0.2f), modifier = Modifier.size(10.dp)) {
                        Box(Modifier.background(Color(0xFF10B981), CircleShape))
                    }
                    Text(
                        text = "متصل: $userName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Quick Switch between EISUS Hub and Home Assistant
                    FilterChip(
                        selected = currentUrlMode == "hub",
                        onClick = {
                            if (currentUrlMode != "hub") {
                                currentUrlMode = "hub"
                                webViewInstance?.loadUrl(hubUrl)
                            }
                        },
                        label = { Text("الرئيسية", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = currentUrlMode == "ha",
                        onClick = {
                            if (currentUrlMode != "ha") {
                                currentUrlMode = "ha"
                                webViewInstance?.loadUrl(haUrl)
                            }
                        },
                        label = { Text("Home Assistant", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "تسجيل خروج", tint = Color(0xFFDC2626))
                    }
                }
            }
        }

        if (isPageLoading) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }

        if (hasError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.padding(24.dp).widthIn(max = 380.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.WifiOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("تعذر الاتصال بـ Hub", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("تأكد من اتصال الشاشة بالإنترنت ومن استقرار الشبكة ثم أعد المحاولة.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Button(
                            onClick = {
                                hasError = false
                                webViewInstance?.reload()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إعادة المحاولة")
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
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "$userAgentString EisusControlApp/1.0"
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
