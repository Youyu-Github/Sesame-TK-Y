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
 * 独立开关：
 * - enableCaptchaUIHook：UI层拦截开关（阻止对话框显示）
 * - enableCaptchaRPCHook：RPC层拦截开关（跳过验证处理）
 * 
 * 参考：x5.c 的实现方式
 * 
 * 使用方式：
 * CaptchaHook.setupHook(classLoader)
 * CaptchaHook.updateHooks(enableUI, enableRPC)  // 动态更新开关状态
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
     * UI层Hook卸载器（用于动态控制）
     */
    private var uiHookUnhook: XC_MethodHook.Unhook? = null
    
    /**
     * RPC层Hook卸载器（用于动态控制）
     */
    private var rpcHookUnhook: XC_MethodHook.Unhook? = null
    
    /**
     * 保存ClassLoader供后续使用
     */
    private var savedClassLoader: ClassLoader? = null

    /**
     * 初始化Hook系统
     * 
     * @param classLoader 目标应用的ClassLoader
     */
    fun setupHook(classLoader: ClassLoader) {
        savedClassLoader = classLoader
        Log.runtime(TAG, "验证码Hook系统初始化完成")
        Log.runtime(TAG, "⚠️ Hook配置将在配置文件加载后同步")
        
        // 注意：此时配置文件还未加载，不能立即应用Hook
        // 实际的Hook应用会在BaseModel.boot()中进行
    }
    
    /**
     * 动态更新Hook开关状态
     * 
     * @param enableUI 是否启用UI层拦截
     * @param enableRPC 是否启用RPC层拦截
     */
    fun updateHooks(enableUI: Boolean, enableRPC: Boolean) {
        val classLoader = savedClassLoader
        if (classLoader == null) {
            Log.error(TAG, "❌ ClassLoader未初始化，请先调用setupHook()")
            return
        }
        
        Log.runtime(TAG, "📝 更新验证码Hook状态:")
        Log.runtime(TAG, "  UI层拦截: ${if (enableUI) "✅ 开启" else "⛔ 关闭"}")
        Log.runtime(TAG, "  RPC层拦截: ${if (enableRPC) "✅ 开启" else "⛔ 关闭"}")
        
        // 先卸载所有现有Hook
        unhookAll()
        
        // 根据开关状态重新Hook
        if (enableUI) {
            Log.runtime(TAG, "  🔧 设置UI层拦截...")
            uiHookUnhook = hookCaptchaDialogShow(classLoader)
        }
        
        if (enableRPC) {
            Log.runtime(TAG, "  🔧 设置RPC层拦截...")
            rpcHookUnhook = hookRpcRdsUtilHandle(classLoader)
        }
        
        if (!enableUI && !enableRPC) {
            Log.runtime(TAG, "  ⚠️ 所有验证码拦截已关闭")
        }
        
        Log.runtime(TAG, "验证码Hook更新完成 ✅")
    }
    
    /**
     * 卸载所有Hook
     */
    private fun unhookAll() {
        uiHookUnhook?.unhook()
        uiHookUnhook = null
        
        rpcHookUnhook?.unhook()
        rpcHookUnhook = null
    }

    /**
     * 第一层拦截：阻止验证码对话框显示
     * 
     * Hook点: CaptchaDialog.show()
     * 作用: 阻止对话框显示，用户看不到验证码
     * 
     * @param classLoader 类加载器
     * @return Hook卸载器，失败时返回null
     */
    private fun hookCaptchaDialogShow(classLoader: ClassLoader): XC_MethodHook.Unhook? {
        return try {
            val captchaDialogClass = XposedHelpers.findClass(CLASS_CAPTCHA_DIALOG, classLoader)
            
            val unhook = XposedHelpers.findAndHookMethod(
                captchaDialogClass,
                "show",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // 阻止验证码对话框显示
                        param.result = null
                        Log.runtime(TAG, "✅ [UI层拦截] 已阻止验证码对话框显示")
                        Log.runtime(TAG, "  对话框: ${param.thisObject.javaClass.simpleName}")
                    }
                }
            )
            
            Log.runtime(TAG, "✅ Hook CaptchaDialog.show() 成功")
            unhook
        } catch (e: Throwable) {
            Log.error(TAG, "❌ Hook CaptchaDialog.show() 失败")
            Log.printStackTrace(TAG, e)
            null
        }
    }

    /**
     * 第二层拦截：返回0跳过RPC验证处理
     * 
     * Hook点: RpcRdsUtilImpl.rdsCaptchaHandle(7个参数)
     * 作用: 返回0表示不需要处理验证码，系统跳过验证流程
     * 
     * @param classLoader 类加载器
     * @return Hook卸载器，失败时返回null
     */
    private fun hookRpcRdsUtilHandle(classLoader: ClassLoader): XC_MethodHook.Unhook? {
        return try {
            val rpcRdsUtilClass = XposedHelpers.findClass(CLASS_RPC_RDS_UTIL, classLoader)
            
            // 方法签名：rdsCaptchaHandle(7个参数) -> int
            // 由于参数类型未知，我们Hook类的所有方法，找到名为 rdsCaptchaHandle 的
            val methods = rpcRdsUtilClass.declaredMethods
            val targetMethod = methods.find { it.name == "rdsCaptchaHandle" }
            
            if (targetMethod != null) {
                val unhook = XposedHelpers.findAndHookMethod(
                    rpcRdsUtilClass,
                    "rdsCaptchaHandle",
                    *targetMethod.parameterTypes,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // 返回0跳过验证处理
                            param.result = 0
                            Log.runtime(TAG, "✅ [RPC层拦截] 已跳过验证处理")
                            Log.runtime(TAG, "  返回值: 0 (不需要处理验证码)")
                            Log.runtime(TAG, "  参数数量: ${param.args.size}")
                        }
                    }
                )
                
                Log.runtime(TAG, "✅ Hook RpcRdsUtilImpl.rdsCaptchaHandle() 成功")
                Log.runtime(TAG, "  方法参数数量: ${targetMethod.parameterTypes.size}")
                unhook
            } else {
                Log.error(TAG, "❌ 未找到 rdsCaptchaHandle 方法")
                Log.error(TAG, "  可用方法列表:")
                methods.forEach {
                    Log.error(TAG, "    - ${it.name}(${it.parameterTypes.size}个参数)")
                }
                null
            }
        } catch (e: Throwable) {
            Log.error(TAG, "❌ Hook RpcRdsUtilImpl.rdsCaptchaHandle() 失败")
            Log.printStackTrace(TAG, e)
            null
        }
    }
}