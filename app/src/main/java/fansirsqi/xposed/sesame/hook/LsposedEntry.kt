package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XposedBridge
import fansirsqi.xposed.sesame.data.General
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class LsposedEntry(
    base: XposedInterface,
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {
    
    private val processName = param.processName
    private val tag = "LsposedEntry"
    
    // 提前初始化 ApplicationHook，但不设置不存在的属性
    val customHooker: ApplicationHook = ApplicationHook()

    object XposedInfo {
        var apiVersion: Int = 0
        var frameworkName: String = ""
        var frameworkVersion: String = ""
        var frameworkVersionCode: Long = 0
        var scope: MutableList<String?> = mutableListOf()
    }

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

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        // 直接使用 init 中初始化的 customHooker
        when (processName) {
            General.PACKAGE_NAME -> {
                XposedBridge.log("$tag: Loading target package hook")
                customHooker.loadPackage(param)
            }
            General.MODULE_PACKAGE_NAME -> {
                XposedBridge.log("$tag: Loading module self hook")
                customHooker.loadModelPackage(param)
            }
            else -> {
                XposedBridge.log("$tag: Unknown process $processName, skipping hook")
            }
        }
    }
}