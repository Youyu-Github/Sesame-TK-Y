package fansirsqi.xposed.sesame.hook;

import android.content.pm.PackageInfo;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.entity.AlipayVersion;
import fansirsqi.xposed.sesame.util.Log;

/**
 * 版本号 Hook 工具类
 * 用于在应用启动早期拦截并获取支付宝版本信息
 */
public class VersionHook {
    private static final String TAG = "VersionHook";

    // 缓存捕获的版本信息
    private static volatile AlipayVersion capturedVersion = null;
    private static volatile boolean hookInstalled = false;

    /**
     * 在 loadPackage 阶段尽早安装 Hook
     *
     * @param classLoader 类加载器
     */
    public static void installHook(ClassLoader classLoader) {
        // 防止重复安装
        if (hookInstalled) {
            Log.runtime(TAG, "⚠️ Hook 已安装,跳过");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.ApplicationPackageManager",
                    classLoader,
                    "getPackageInfo",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("deprecation") // 1. 添加注解忽略过时警告
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                PackageInfo packageInfo = (PackageInfo) param.getResult();

                                // 只处理支付宝的包信息
                                if (packageInfo != null &&
                                        General.PACKAGE_NAME.equals(packageInfo.packageName)) {

                                    String versionName = packageInfo.versionName;
                                    long longVersionCode;

                                    // 2. 优化逻辑：根据 Android 版本选择正确的 API
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        // Android 9.0+ 使用 getLongVersionCode
                                        longVersionCode = packageInfo.getLongVersionCode();
                                    } else {
                                        // 低版本使用旧字段 (因为加了注解，这里不会报错了)
                                        longVersionCode = packageInfo.versionCode;
                                    }

                                    // 为了保持原有日志和逻辑兼容，强转回 int 用于显示
                                    int versionCode = (int) longVersionCode;

                                    // 只在第一次捕获时记录日志
                                    if (capturedVersion == null && versionName != null) {
                                        capturedVersion = new AlipayVersion(versionName);
                                        Log.runtime(TAG, "✅ 捕获支付宝版本: " + versionName +
                                                " (code: " + versionCode +
                                                ", longCode: " + longVersionCode + ")");
                                    }
                                }
                            } catch (Throwable t) {
                                // 静默处理异常,避免影响应用正常运行
                                Log.printStackTrace(TAG, t);
                            }
                        }
                    }
            );

            hookInstalled = true;
            Log.runtime(TAG, "✅ 版本号 Hook 安装成功");

        } catch (Throwable t) {
            Log.runtime(TAG, "❌ 安装版本号 Hook 失败");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 获取已捕获的版本信息
     *
     * @return AlipayVersion 对象,如果未捕获则返回 null
     */
    public static AlipayVersion getCapturedVersion() {
        return capturedVersion;
    }

    /**
     * 检查是否已成功捕获版本号
     *
     * @return true: 已捕获, false: 未捕获
     */
    public static boolean hasVersion() {
        return capturedVersion != null;
    }

    /**
     * 重置捕获状态 (用于测试或重新初始化)
     */
    public static void reset() {
        capturedVersion = null;
        hookInstalled = false;
        Log.runtime(TAG, "🔄 版本号 Hook 状态已重置");
    }
}