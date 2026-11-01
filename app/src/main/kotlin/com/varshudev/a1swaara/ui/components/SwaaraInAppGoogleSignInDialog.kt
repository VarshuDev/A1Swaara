package com.varshudev.a1swaara.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.cloud.SwaaraCloudAuth
import com.varshudev.a1swaara.cloud.SwaaraGoogleAuthManager
import com.varshudev.a1swaara.cloud.SwaaraSupabaseClient
import com.varshudev.a1swaara.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SwaaraInAppSignIn"
private const val CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SwaaraInAppGoogleSignInDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authUrl = remember {
        val base = SwaaraSupabaseClient.BASE_URL
        "$base/auth/v1/authorize?provider=google&redirect_to=swaara://auth-callback&prompt=select_account"
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true && !isAuthenticating) {
            webViewRef?.goBack()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isAuthenticating) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black,
                        spotColor = SwaaraColors.Magenta
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(SwaaraColors.Obsidian)
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                SwaaraColors.Magenta.copy(alpha = 0.8f),
                                SwaaraColors.Violet.copy(alpha = 0.8f),
                                SwaaraColors.Cyan.copy(alpha = 0.8f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SwaaraColors.DeepSurface)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SwaaraColors.Violet, SwaaraColors.Magenta)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = "Sign in with Google",
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A1 Swaara Cloud & Jam Sync",
                                    color = SwaaraColors.SecondaryText,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    hasError = false
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = "Reload",
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDismiss() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Close",
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Loading Progress Indicator
                    if (isLoading && !isAuthenticating) {
                        LinearProgressIndicator(
                            progress = { webProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = SwaaraColors.Cyan,
                            trackColor = SwaaraColors.DeepSurface
                        )
                    }

                    // Main WebView / Content Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0F0F1A))
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.parseColor("#0F0F1A"))
                                    isVerticalScrollBarEnabled = true
                                    isHorizontalScrollBarEnabled = false

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(false)
                                        builtInZoomControls = false
                                        displayZoomControls = false
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        userAgentString = CHROME_USER_AGENT
                                    }

                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    fun checkAndIntercept(url: String?): Boolean {
                                        val targetUrl = url ?: return false
                                        Log.d(TAG, "Inspecting URL: $targetUrl")

                                        if (targetUrl.contains("error=") &&
                                            (targetUrl.contains("localhost:3000") || targetUrl.startsWith("swaara://") || targetUrl.contains("supabase.co"))
                                        ) {
                                            stopLoading()
                                            coroutineScope.launch(Dispatchers.Main) {
                                                context.toast("Sign in was cancelled")
                                                onDismiss()
                                            }
                                            return true
                                        }

                                        if (targetUrl.contains("access_token=") ||
                                            targetUrl.startsWith("swaara://auth-callback") ||
                                            (targetUrl.contains("localhost:3000") && targetUrl.contains("access_token="))
                                        ) {
                                            stopLoading()
                                            if (!isAuthenticating) {
                                                isAuthenticating = true
                                                val tokens = SwaaraGoogleAuthManager.extractTokensFromUrl(targetUrl)
                                                if (tokens != null) {
                                                    coroutineScope.launch {
                                                        val ok = SwaaraGoogleAuthManager.processOAuthTokens(
                                                            context = context,
                                                            accessToken = tokens.first,
                                                            refreshToken = tokens.second,
                                                            expiresIn = tokens.third
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            if (ok) {
                                                                val name = SwaaraCloudAuth.getInstance(context).displayName
                                                                context.toast("Welcome, $name!")
                                                                onDismiss()
                                                            } else {
                                                                isAuthenticating = false
                                                                hasError = true
                                                                errorMessage = "Failed to establish authenticated session."
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    isAuthenticating = false
                                                    hasError = true
                                                    errorMessage = "Could not parse authentication tokens."
                                                }
                                            }
                                            return true
                                        }

                                        return false
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            webProgress = newProgress
                                            isLoading = newProgress < 100
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val currentUrl = request?.url?.toString()
                                            return checkAndIntercept(currentUrl)
                                        }

                                        @Deprecated("Deprecated in Java")
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            url: String?
                                        ): Boolean {
                                            return checkAndIntercept(url)
                                        }

                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            checkAndIntercept(url)
                                        }

                                        override fun onLoadResource(view: WebView?, url: String?) {
                                            super.onLoadResource(view, url)
                                            if (url?.contains("access_token=") == true) {
                                                checkAndIntercept(url)
                                            }
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            val failingUrl = request?.url?.toString() ?: ""
                                            if (failingUrl.contains("access_token=") || failingUrl.contains("localhost:3000")) {
                                                checkAndIntercept(failingUrl)
                                                return
                                            }
                                            super.onReceivedError(view, request, error)
                                        }
                                    }

                                    loadUrl(authUrl)
                                    webViewRef = this
                                }
                            },
                            update = {
                                webViewRef = it
                            }
                        )

                        if (isAuthenticating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SwaaraColors.Obsidian.copy(alpha = 0.95f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = SwaaraColors.Magenta,
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Connecting to A1 Swaara Cloud...",
                                        color = SwaaraColors.PrimaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Syncing your playlists, favorites, and Jam profile",
                                        color = SwaaraColors.SecondaryText,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        }

                        if (hasError && !isAuthenticating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SwaaraColors.Obsidian.copy(alpha = 0.96f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Sign-In Error",
                                        color = SwaaraColors.Magenta,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = errorMessage.ifEmpty { "Unable to connect to Google authentication service." },
                                        color = SwaaraColors.SecondaryText,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                hasError = false
                                                webViewRef?.loadUrl(authUrl)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SwaaraColors.Violet
                                            )
                                        ) {
                                            Text("Retry", color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { onDismiss() }
                                        ) {
                                            Text("Close", color = SwaaraColors.PrimaryText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
