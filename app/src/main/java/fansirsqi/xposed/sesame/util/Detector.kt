package fansirsqi.xposed.sesame.util

import android.content.Context
import android.content.pm.PackageManager
import fansirsqi.xposed.sesame.BuildConfig
import java.io.File

object Detector {
    private const val TAG = "Detector"

    // --- Native 方法声明 (添加了 @JvmStatic 以供 Java 静态调用) ---
    private external fun init(context: Context)

    @JvmStatic
    external fun tips(context: Context, message: String?)

    @JvmStatic
    external fun isEmbeddedNative(context: Context): Boolean

    @JvmStatic
    external fun dangerous(context: Context)

    @JvmStatic
    external fun genWua(): String

    @JvmStatic
    external fun loadLibraryWithContextNative(context: Context, libraryName: String): Boolean

    @JvmStatic
    external fun getApiUrlWithKey(key: Int): String

    @JvmStatic
    external fun getRandomApi(key: Int): String

    @JvmStatic
    external fun getRandomEncryptData(key: Int): String

    @JvmStatic
    fun loadLibrary(libraryName: String): Boolean {
        try {
            System.loadLibrary(libraryName)
            Log.runtime(TAG, "loadLibrary $libraryName success")
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.error(TAG, "loadLibrary${e.message}")
            return false
        }
    }

    @JvmStatic
    fun getApi(key: Int): String {
        return getRandomApi(key)
    }

    /**
     * 检测是否通过LSPatch运行
     */
    private fun isRunningInLSPatch(context: Context): Boolean {
        try {
            // 检查应用元数据中是否有LSPatch标记
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.containsKey("lspatch") == true
            return appInfo.metaData?.containsKey("lspatch") == true
        } catch (e: Exception) {
            Log.error(TAG, "检查LSPatch运行环境时出错: ${e.message}")
            return false
        }
    }

    /**
     * 检测模块是否在合法环境中运行
     */
    @JvmStatic
    fun isLegitimateEnvironment(context: Context): Boolean {
        val isRunningInLSPatch = isRunningInLSPatch(context)
        if (!isRunningInLSPatch) {
            return false
        }
        val isEmbedded = isEmbeddedNative(context)
        Log.runtime(TAG, "isEmbedded: $isEmbedded")
        return isEmbedded
    }

    @JvmStatic
    fun initDetector(context: Context) {
        try {
            init(context)
        } catch (e: Exception) {
            Log.error(TAG, "initDetector ${e.message}")
        }
    }

    private fun getApkPath(context: Context, packageName: String): String? {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            Log.runtime(TAG, "appInfo.sourceDir: " + appInfo.sourceDir)
            return appInfo.sourceDir
        } catch (_: PackageManager.NameNotFoundException) {
            Log.runtime(TAG, "Package not found: $packageName")
            return null
        }
    }
}