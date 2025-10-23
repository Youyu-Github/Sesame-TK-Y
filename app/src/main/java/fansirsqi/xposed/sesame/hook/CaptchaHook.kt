package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.util.Log

/**
 * 支付宝滑块验证码Hook工具类（双Hook方案 - 参考x5.c实现）
 * 
 * 核心策略：
 * 1. Hook CaptchaDialog.show() - 阻止验证码对话框显示（UI层拦截）
 * 2. Hook RpcRdsUtilImpl.rdsCaptchaHandle() - 返回0跳过验证（RPC层拦截）
 * 
 * 双重防护：
 * - 第一层：用户看不到验证码
 * - 第二层：系统认为不需要处理验证码
 * 
 * 拦截级别：
 * - 普通验证（NORMAL_CAPTCHA）：只拦截普通验证码，放行滑块验证
 * - 滑块验证（SLIDE_CAPTCHA）：拦截所有验证码（推荐）
 * 
 * 参考：x5.c 的实现方式
 * 
 * 使用方式：
 * CaptchaHook.hookCaptcha(classLoader)
 * 
 * @author ghostxx
 * @since 2025-10-23
 */
object CaptchaHook {
    private const val TAG = "CaptchaHook"

    /**
     * 验证码对话框类名
     */
    private const val CLASS_CAPTCHA_DIALOG = "com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog"
    
    /**
     * RPC处理工具类名
     */
    private const val CLASS_RPC_RDS_UTIL = "com.alipay.edge.observer.rpc.RpcRdsUtilImpl"

    /**
     * 主入口：启动滑块验证码Hook
     * 
     * @param classLoader 目标应用的ClassLoader
     */
    fun hookCaptcha(classLoader: ClassLoader) {
        // 检查是否启用验证码拦截
        if (!BaseModel.enableCaptchaHook.value) {
            Log.runtime(TAG, "⚠️ 验证码拦截未启用，跳过Hook")
            return
        }
        
        val hookLevel = BaseModel.captchaHookLevel.value
        val levelName = when (hookLevel) {
            BaseModel.CaptchaHookLevel.NORMAL_CAPTCHA -> "普通验证(放行滑块)"
            BaseModel.CaptchaHookLevel.SLIDE_CAPTCHA -> "滑块验证(屏蔽所有)"
            else -> "未知"
        }
        
        Log.runtime(TAG, "开始Hook支付宝滑块验证码（双Hook方案）...")
        Log.runtime(TAG, "  拦截级别: $levelName")
        
        // 第一层：阻止验证码对话框显示
        hookCaptchaDialogShow(classLoader, hookLevel)
        
        // 第二层：返回0跳过RPC验证处理
        hookRpcRdsUtilHandle(classLoader, hookLevel)
        
        Log.runtime(TAG, "滑块验证码Hook设置完成 ✅")
    }

    /**
     * 第一层拦截：阻止验证码对话框显示
     * 
     * Hook点: CaptchaDialog.show()
     * 作用: 阻止对话框显示，用户看不到验证码
     * 
     * @param classLoader 类加载器
     * @param hookLevel 拦截级别
     */
    private fun hookCaptchaDialogShow(classLoader: ClassLoader, hookLevel: Int) {
        try {
            val captchaDialogClass = XposedHelpers.findClass(CLASS_CAPTCHA_DIALOG, classLoader)
            
            XposedHelpers.findAndHookMethod(
                captchaDialogClass,
                "show",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // 根据拦截级别判断是否拦截
                        when (hookLevel) {
                            BaseModel.CaptchaHookLevel.NORMAL_CAPTCHA -> {
                                // 普通验证模式：不拦截（放行滑块验证）
                                Log.runtime(TAG, "🔓 [UI层] 普通验证模式，放行滑块验证")
                            }
                            BaseModel.CaptchaHookLevel.SLIDE_CAPTCHA -> {
                                // 滑块验证模式：拦截所有
                                param.result = null
                                Log.runtime(TAG, "✅ [UI层拦截] 已阻止验证码对话框显示")
                                Log.runtime(TAG, "  对话框: ${param.thisObject.javaClass.simpleName}")
                            }
                        }
                    }
                }
            )
            
            Log.runtime(TAG, "✅ Hook CaptchaDialog.show() 成功")
        } catch (e: Throwable) {
            Log.error(TAG, "❌ Hook CaptchaDialog.show() 失败")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 第二层拦截：返回0跳过RPC验证处理
     * 
     * Hook点: RpcRdsUtilImpl.rdsCaptchaHandle(7个参数)
     * 作用: 返回0表示不需要处理验证码，系统跳过验证流程
     * 
     * @param classLoader 类加载器
     * @param hookLevel 拦截级别
     */
    private fun hookRpcRdsUtilHandle(classLoader: ClassLoader, hookLevel: Int) {
        try {
            val rpcRdsUtilClass = XposedHelpers.findClass(CLASS_RPC_RDS_UTIL, classLoader)
            
            // 方法签名：rdsCaptchaHandle(7个参数) -> int
            // 由于参数类型未知，我们Hook类的所有方法，找到名为 rdsCaptchaHandle 的
            val methods = rpcRdsUtilClass.declaredMethods
            val targetMethod = methods.find { it.name == "rdsCaptchaHandle" }
            
            if (targetMethod != null) {
                XposedHelpers.findAndHookMethod(
                    rpcRdsUtilClass,
                    "rdsCaptchaHandle",
                    *targetMethod.parameterTypes,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // 根据拦截级别判断是否拦截
                            when (hookLevel) {
                                BaseModel.CaptchaHookLevel.NORMAL_CAPTCHA -> {
                                    // 普通验证模式：不拦截，返回原始值
                                    Log.runtime(TAG, "🔓 [RPC层] 普通验证模式，执行原始逻辑")
                                }
                                BaseModel.CaptchaHookLevel.SLIDE_CAPTCHA -> {
                                    // 滑块验证模式：返回0跳过验证
                                    param.result = 0
                                    Log.runtime(TAG, "✅ [RPC层拦截] 已跳过验证处理")
                                    Log.runtime(TAG, "  返回值: 0 (不需要处理验证码)")
                                    Log.runtime(TAG, "  参数数量: ${param.args.size}")
                                }
                            }
                        }
                    }
                )
                
                Log.runtime(TAG, "✅ Hook RpcRdsUtilImpl.rdsCaptchaHandle() 成功")
                Log.runtime(TAG, "  方法参数数量: ${targetMethod.parameterTypes.size}")
            } else {
                Log.error(TAG, "❌ 未找到 rdsCaptchaHandle 方法")
                Log.error(TAG, "  可用方法列表:")
                methods.forEach {
                    Log.error(TAG, "    - ${it.name}(${it.parameterTypes.size}个参数)")
                }
            }
        } catch (e: Throwable) {
            Log.error(TAG, "❌ Hook RpcRdsUtilImpl.rdsCaptchaHandle() 失败")
            Log.printStackTrace(TAG, e)
        }
    }
}