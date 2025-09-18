package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.newui.WatermarkView.Companion.install
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.LanguageUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil
import fansirsqi.xposed.sesame.newui.WatermarkView
import java.io.File
import android.os.Looper
import org.json.JSONObject
import android.webkit.WebViewClient

class HtmlViewerActivity : BaseActivity() {
    val TAG = "HtmlViewerActivity"
    var mWebView: MyWebView? = null
    var progressBar: ProgressBar? = null
    private var uri: Uri? = null
    private var canClear: Boolean? = null
    var settings: WebSettings? = null
    private var refreshHandler: Handler? = null
    private var refreshRunnable: Runnable? = null
    private var isRefreshing: Boolean = false
    private var logFileName: String? = null
    private var logFileSize: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageUtil.setLocale(this)
        setContentView(R.layout.activity_html_viewer)
        WatermarkView.Companion.install(this)
        install(this)
        
        // 初始化 WebView 和进度条
        mWebView = findViewById(R.id.mwv_webview)
        progressBar = findViewById(R.id.pgb_webview)

        setupWebView()
        settings = mWebView!!.getSettings()

        // 安全设置 WebView
        try {
            if (mWebView != null) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    try {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings!!, true)
                    } catch (e: Exception) {
                        Log.error(TAG, "设置夜间模式失败: " + e.message)
                        Log.printStackTrace(TAG, e)
                    }
                }
                settings!!.javaScriptEnabled = false
                settings!!.domStorageEnabled = false
                progressBar!!.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.selection_color)))
                mWebView!!.setBackgroundColor(ContextCompat.getColor(this, R.color.background))
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG,"WebView初始化异常: ", e)
        }

        val contentView = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            mWebView!!.setPadding(
                mWebView!!.getPaddingLeft(),
                mWebView!!.paddingTop,
                mWebView!!.getPaddingRight(),
                systemBarsBottom
            )

            insets
        }
    }

    /**
     * 设置 WebView 的 WebChromeClient 和进度变化监听
     */
    private fun setupWebView() {
        mWebView!!.setWebChromeClient(
            object : WebChromeClient() {
                @SuppressLint("WrongConstant")
                override fun onProgressChanged(view: WebView?, progress: Int) {
                    progressBar!!.progress = progress
                    if (progress < 100) {
                        baseSubtitle = "Loading..."
                        progressBar!!.visibility = View.VISIBLE
                    } else {
                        // 设置副标题为"日志查看 >>> 文件名 (大小)"
                        val subtitle = "日志查看 >>> $logFileName (${formatFileSize(logFileSize)})"
                        baseSubtitle = subtitle
                        progressBar!!.visibility = View.GONE
                        
                        // 将文件信息传递给WebView
                        mWebView?.evaluateJavascript(
                            "setFileInfo('$logFileName', $logFileSize)", 
                            null
                        )
                    }
                }
            })
    }

    // 格式化文件大小
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    // 日志实时显示 开始
    private fun toJsString(s: String?): String {
        if (s == null) return "''"
        val sb = StringBuilder(s.length + 16)
        sb.append('\'')
        for (i in s.indices) {
            val c = s[i]
            when (c) {
                '\'' -> sb.append("\\'")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\u000C' -> sb.append("\\f") // \f
                '\b' -> sb.append("\\b")
                else -> {
                    if (c < 0x20.toChar()) {
                        sb.append(String.format("\\u%04x", c.toInt()))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append('\'')
        return sb.toString()
    }

    private fun readAllTextSafe(path: String): String {
        return try {
            val cs = java.nio.charset.StandardCharsets.UTF_8
            val data = java.nio.file.Files.readAllBytes(java.io.File(path).toPath())
            String(data, cs)
        } catch (t: Throwable) {
            ""
        }
    }
    // 日志实时显示 结束

    override fun onResume() {
        super.onResume()
        // 安全设置WebView
        try {
            val intent = intent // 获取传递过来的 Intent
            if (intent != null) {
                val webView = mWebView
                val webSettings = webView?.settings
                
                webSettings?.let { settings ->
                    settings.setSupportZoom(true) // 支持缩放
                    settings.builtInZoomControls = true // 启用内置缩放机制
                    settings.displayZoomControls = false // 不显示缩放控件
                    settings.useWideViewPort = true // 启用触摸缩放
                    settings.loadWithOverviewMode = true //概览模式加载
                    settings.textZoom = 85
                    
                    // 可选夜间模式设置
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        try {
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                        } catch (e: Exception) {
                            Log.error(TAG, "设置夜间模式失败: " + e.message)
                            Log.printStackTrace(TAG, e)
                        }
                    }
                    configureWebViewSettings(intent, settings)
                }
                val currentUri = intent.data
                uri = currentUri
                if (currentUri != null && webView != null) {
                    // 获取文件名和大小
                    if (currentUri.scheme?.equals("file", ignoreCase = true) == true) {
                        val path = currentUri.path
                        if (path != null && path.endsWith(".log")) {
                            val file = File(path)
                            logFileName = file.name
                            logFileSize = file.length()
                        }
                    }
                    
                    /// 日志实时显示 begin
                    webSettings?.javaScriptEnabled = true
                    webSettings?.domStorageEnabled = true // 可选

                    webView.loadUrl("file:///android_asset/log_viewer.html")
                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, progress: Int) {
                            progressBar?.progress = progress
                            if (progress < 100) {
                                // 替换 setBaseSubtitle
                                supportActionBar?.subtitle = "Loading..."
                                progressBar?.visibility = View.VISIBLE
                            } else {
                                // 设置副标题为"日志查看 >>> 文件名 (大小)"
                                val subtitle = "日志查看 >>> $logFileName (${formatFileSize(logFileSize)})"
                                supportActionBar?.subtitle = subtitle
                                progressBar?.visibility = View.GONE

                                // ★★ 页面已就绪：把现有文件一次性灌入 ★★
                                if (currentUri.scheme?.equals("file", ignoreCase = true) == true) {
                                    val path = currentUri.path
                                    if (path != null && path.endsWith(".log")) {
                                        val all = readAllTextSafe(path)
                                        val jsArg = toJsString(all)

                                        webView.evaluateJavascript("setFullText($jsArg)", null)

                                        // 然后启动增量监听（你在 MyWebView 里实现的）
                                        if (webView is MyWebView) {
                                            webView.startWatchingIncremental(path)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    /// 日志实时显示 end
                }
                
                canClear = intent.getBooleanExtra("canClear", false)
            }
        } catch (e: Exception) {
            Log.error(TAG, "WebView设置异常: " + e.message)
            Log.printStackTrace(TAG, e)
        }
    }

    // 添加缺失的方法实现
    private fun readFileContent(filePath: String): String {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            Log.error(TAG, "读取文件失败: ${e.message}")
            ""
        }
    }

    /**
     * 配置 WebView 的设置项
     *
     * @param intent   传递的 Intent
     * @param settings WebView 的设置
     */
    private fun configureWebViewSettings(intent: Intent, settings: WebSettings) {
        if (intent.getBooleanExtra("nextLine", true)) {
            settings.textZoom = 85
            settings.useWideViewPort = false
        } else {
            settings.textZoom = 85
            settings.useWideViewPort = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 创建菜单选项
        menu.add(0, 1, 1, getString(R.string.export_file))
        if (canClear == true) {
            menu.add(0, 2, 2, getString(R.string.clear_file))
        }
        menu.add(0, 3, 3, getString(R.string.open_with_other_browser))
        menu.add(0, 4, 4, getString(R.string.copy_the_url))
        menu.add(0, 5, 5, getString(R.string.scroll_to_top))
        menu.add(0, 6, 6, getString(R.string.scroll_to_bottom))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 ->                 // 导出文件
                exportFile()

            2 ->                 // 清空文件
                clearFile()

            3 ->                 // 使用其他浏览器打开
                openWithBrowser()

            4 ->                 // 复制 URL 到剪贴板
                copyUrlToClipboard()

            5 ->                 // 滚动到顶部
                mWebView!!.scrollTo(0, 0)

            6 ->                 // 滚动到底部
                mWebView!!.scrollToBottom()
        }
        invalidateOptionsMenu() // 刷新菜单以更新文本
        return true
    }

    /**
     * 导出当前文件
     */
    private fun exportFile() {
        try {
            if (uri != null) {
                val path = uri!!.path
                Log.runtime(TAG, "导出文件: $path")
                if (path != null) {
                    val exportFile = Files.exportFile(File(path), true)
                    if (exportFile != null && exportFile.exists()) {
                        ToastUtil.showToast(getString(R.string.file_exported) + exportFile.path)
                    } else {
                        Log.runtime(TAG, "导出失败，exportFile 对象为 null 或不存在！")
                    }
                } else {
                    Log.runtime(TAG, "路径为 null！")
                }
            } else {
                Log.runtime(TAG, "URI 为 null！")
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 清空当前文件
     */
    private fun clearFile() {
        try {
            if (uri != null) {
                val path = uri!!.path
                if (path != null) {
                    val file = File(path)
                    if (Files.clearFile(file)) {
                        ToastUtil.makeText(this, "文件已清空", Toast.LENGTH_SHORT).show()
                        // 更新文件大小
                        logFileSize = file.length()
                        // 更新副标题
                        val subtitle = "日志查看 >>> $logFileName (${formatFileSize(logFileSize)})"
                        supportActionBar?.subtitle = subtitle
                        mWebView!!.reload()
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 使用其他浏览器打开当前 URL
     */
    private fun openWithBrowser() {
        if (uri != null) {
            val scheme = uri!!.scheme
            if ("http".equals(scheme, ignoreCase = true) || "https".equals(scheme, ignoreCase = true)) {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else if ("file".equals(scheme, ignoreCase = true)) {
                ToastUtil.makeText(this, "该文件不支持用浏览器打开", Toast.LENGTH_SHORT).show()
            } else {
                ToastUtil.makeText(this, "不支持用浏览器打开", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 复制当前 WebView 的 URL 到剪贴板
     */
    private fun copyUrlToClipboard() {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mWebView!!.getUrl()))
            ToastUtil.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
    }

    // 日志实时显示
    override fun onPause() {
        super.onPause()
        if (mWebView is MyWebView) {
            (mWebView as MyWebView).stopWatchingIncremental()
        }
    }

    override fun onStop() {
        super.onStop()
        if (mWebView is MyWebView) {
            (mWebView as MyWebView).stopWatchingIncremental()
        }
    }

    override fun onDestroy() {
        // 先停止文件监听，再做 WebView 清理，最后再 super
        (mWebView as? MyWebView)?.stopWatchingIncremental()
        
        val webView = mWebView // 使用局部变量避免并发修改问题
        if (webView != null) {
            try {
                webView.loadUrl("about:blank")
                webView.stopLoading()
                
                // 使用安全的属性设置方式
                webView.webChromeClient = null
                
                webView.destroy()
            } catch (ignore: Throwable) {
                // 空捕获块
            }
        }
        super.onDestroy()
    }
}