package fansirsqi.xposed.sesame.hook.lsp100

import de.robv.android.xposed.XposedBridge
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.XposedEnv
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
// import io.github.libxposed.service.XposedService
// import io.github.libxposed.service.XposedServiceHelper

class HookEntry(
    base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {
    val tag = "LsposedEntry"
    private val processName = param.processName
    var customHooker: ApplicationHook? = null

    /*object XposedInfo {
        var apiVersion: Int = 0
        var frameworkName: String = ""
        var frameworkVersion: String = ""
        var frameworkVersionCode: Long = 0
        var scope: MutableList<String?> = mutableListOf()
    }*/

    /*
    init {
        // 保留服务监听获取详细信息
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedInfo.apiVersion = service.apiVersion
                XposedInfo.frameworkName = service.frameworkName
                XposedInfo.frameworkVersion = service.frameworkVersion
                XposedInfo.frameworkVersionCode = service.frameworkVersionCode
                XposedInfo.scope = service.scope
                XposedBridge.log("$tag: Service bound - ${service.frameworkName} ${service.frameworkVersion}")
            }

            override fun onServiceDied(service: XposedService) {
                XposedBridge.log("$tag: Service died - ${service.frameworkName}")
            }
        })

        XposedBridge.log("$tag: Initialized for process $processName")
        
        // 同时输出两种方式获取的框架信息用于对比
        val baseFw = "${base.frameworkName} ${base.frameworkVersion}"
        val serviceFw = "${XposedInfo.frameworkName} ${XposedInfo.frameworkVersion}"
        XposedBridge.log("$tag: Framework from base: $baseFw")
        XposedBridge.log("$tag: Framework from service: $serviceFw")
        XposedBridge.log("$tag: API version: ${XposedInfo.apiVersion}")
    }
    */

    init {
        customHooker = ApplicationHook()
        customHooker?.xposedInterface = base
        // 将框架提供的 base 接口实例传递给逻辑核心，连接 Hook 进程与框架功能。
        XposedBridge.log("$tag: Initialized for process $processName")


        val baseFw = "${base.frameworkName} \n${base.frameworkVersion} \n${base.applicationInfo} \n${base.frameworkVersionCode}"
        XposedBridge.log("LspEntry: Framework from base: $baseFw ")
    }

    /**
     * 当模块作用域内的应用进程启动时，框架会回调此方法。
     */
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            if (General.PACKAGE_NAME != param.packageName) return
            XposedEnv.classLoader = param.classLoader
            XposedEnv.appInfo = param.applicationInfo
            XposedEnv.packageName = param.packageName
            XposedEnv.processName = processName
            customHooker?.loadPackage(param)
            XposedBridge.log("$tag: Hooking ${param.packageName} in process $processName")
        } catch (e: Throwable) {
            XposedBridge.log("$tag: Hook failed - ${e.message}")
            XposedBridge.log(e)
        }
    }
}