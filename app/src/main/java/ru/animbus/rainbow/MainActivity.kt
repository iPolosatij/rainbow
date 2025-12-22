package com.example.colorscience

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        setContent {
            ColorScienceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomWebViewScreen()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
}

@Composable
fun CustomWebViewScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    val files = listOf("color_run.html", "light_color.html", "сlor_waves.html")
    val titles = listOf("ОПЫТ НЬЮТОНА", "ДЛИНА ВОЛНЫ", "ЦВЕТА СВЕТА")
    val colors = listOf(
        Color(0xFF8B00FF), // Фиолетовый для Ньютона
        Color(0xFF3498DB), // Синий для Волн
        Color(0xFFE74C3C)  // Красный для Цветов
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color(0xFF1A2A6C)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            println("🎯 Выбрана вкладка: $title")
                            selectedTab = index
                        }
                        .background(
                            if (selectedTab == index) colors[index]
                            else Color(0xFF2C3E50)
                        )
                        .padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        EnhancedWebView(
            htmlFile = files[selectedTab],
            modifier = Modifier.fillMaxSize()
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EnhancedWebView(htmlFile: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        adjustContentForFullView()
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                loadUrl("file:///android_asset/$htmlFile")

                setBackgroundColor(0xFF000000.toInt())
            }
        },
        update = { webView ->
            webView.loadUrl("file:///android_asset/$htmlFile")
        },
        modifier = modifier
    )
}

private fun WebView.adjustContentForFullView() {
    postDelayed({
        evaluateJavascript("""
            (function() {
                var body = document.body;
                var html = document.documentElement;
                
                // Получаем максимальную высоту
                var height = Math.max(
                    body.scrollHeight, 
                    body.offsetHeight,
                    html.clientHeight,
                    html.scrollHeight,
                    html.offsetHeight
                );
                
                // Получаем максимальную ширину
                var width = Math.max(
                    body.scrollWidth,
                    body.offsetWidth,
                    html.clientWidth,
                    html.scrollWidth,
                    html.offsetWidth
                );
                
                return JSON.stringify({height: height, width: width});
            })()
        """.trimIndent()) { result ->
            try {
                val json = result.removeSurrounding("\"")
                val data = if (json != "null") {
                    android.util.JsonReader(java.io.StringReader(json)).use { reader ->
                        reader.beginObject()
                        var height = 0
                        var width = 0
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "height" -> height = reader.nextInt()
                                "width" -> width = reader.nextInt()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        Pair(width, height)
                    }
                } else null

                data?.let { (contentWidth, contentHeight) ->
                    val viewWidth = this.width
                    val viewHeight = this.height

                    val scaleX = viewWidth.toFloat() / contentWidth.toFloat()
                    val scaleY = viewHeight.toFloat() / contentHeight.toFloat()
                    val scale = Math.min(scaleX, scaleY) * 100

                    this.setInitialScale(scale.toInt())

                    applyFullViewCSS()
                }
            } catch (e: Exception) {
                println("⚠️ Ошибка при расчете масштаба: ${e.message}")
            }
        }
    }, 500)
}

private fun WebView.applyFullViewCSS() {
    val css = """
        <style>
            html, body {
                width: 100% !important;
                height: 100% !important;
                margin: 0 !important;
                padding: 0 !important;
                overflow: hidden !important;
            }
            body > * {
                max-width: 100% !important;
                max-height: 100% !important;
                overflow: hidden !important;
            }
            .container {
                width: 100vw !important;
                height: 100vh !important;
                padding: 10px !important;
                box-sizing: border-box !important;
            }
        </style>
    """

    loadUrl("javascript:(function() {" +
            "var style = document.createElement('style');" +
            "style.innerHTML = `$css`;" +
            "document.head.appendChild(style);" +
            "document.body.style.transform = 'scale(1)';" +
            "document.body.style.transformOrigin = '0 0';" +
            "})()")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(htmlFile: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        println("✅ Страница загружена: $htmlFile")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        println("❌ Ошибка загрузки $htmlFile: $description")
                        // Показываем содержимое напрямую в случае ошибки
                        loadDataWithBaseURL(
                            null,
                            "<html><body><h1>Файл $htmlFile не найден</h1><p>Убедитесь что файл находится в папке assets</p></body></html>",
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }

                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true

                    allowFileAccess = true
                    allowContentAccess = true
                    allowUniversalAccessFromFileURLs = true
                    allowFileAccessFromFileURLs = true

                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)

                    loadsImagesAutomatically = true

                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true
                }

                setInitialScale(100)

                val url = "file:///android_asset/$htmlFile"
                println("🔄 Загружаем URL: $url")
                loadUrl(url)

                setBackgroundColor(0x00000000)

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        },
        update = { webView ->
            val url = "file:///android_asset/$htmlFile"
            println("🔄 Обновляем URL: $url")
            webView.loadUrl(url)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedPage by remember { mutableStateOf(0) }

    val pages = listOf(
        Triple("Ньютон", "color_run.html", Icons.Filled.PlayArrow),
        Triple("Волны", "light_color.html", Icons.Filled.Menu),
        Triple("Цвета", "сlor_waves.html", Icons.Filled.Star)
    )

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { index, (title, _, icon) ->
                Button(
                    onClick = {
                        println("🎯 Нажата кнопка: $title")
                        selectedPage = index
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPage == index)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            WebViewPage(
                htmlFile = pages[selectedPage].second,
                modifier = Modifier.fillMaxSize()
            )

            if (false) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Red.copy(alpha = 0.7f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Текущий файл:\n${pages[selectedPage].second}",
                        color = Color.White,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("🔮 Ньютон", "🌊 Волны", "🌈 Цвета")
    val files = listOf("color_run.html", "light_color.html", "сlor_waves.html")

    Column(modifier = Modifier.fillMaxSize()) {
        // Табы
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // WebView
        Box(modifier = Modifier.fillMaxSize()) {
            WebViewPage(
                htmlFile = files[selectedTab],
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFF9575CD),
    background = Color(0xFF000000), // Черный фон
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF2D2D2D),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun ColorScienceAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}