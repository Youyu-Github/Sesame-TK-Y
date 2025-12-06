package fansirsqi.xposed.sesame.data

import fansirsqi.xposed.sesame.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object ServiceManager {

    private const val TAG = "ServiceManager"

    /** 当前连接状态，线程安全 */
    private val _connectionState = AtomicReference<ConnectionState>(ConnectionState.Connecting)

    /** 外部获取当前状态 */
    val connectionState: ConnectionState
        get() = _connectionState.get()

    /** 已连接的服务（如果有） */
    val service: XposedService?
        get() = (_connectionState.get() as? ConnectionState.Connected)?.service

    /** 模块是否激活 */
    val isModuleActivated: Boolean
        get() = _connectionState.get() is ConnectionState.Connected

    /** 状态监听器列表 */
    private val listeners = CopyOnWriteArrayList<(ConnectionState) -> Unit>()

    /** 是否已初始化 - 使用 AtomicBoolean 替代 AtomicReference<Boolean> 以消除警告 */
    private val isInitialized = AtomicBoolean(false)

    /** 初始化 ServiceManager 并注册 XposedService 监听 */
    fun init() {
        // compareAndSet(expect, update) 用法与 AtomicReference 相同
        if (!isInitialized.compareAndSet(false, true)) return

        val listener = object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                if (isModuleActivated) {
                    Log.runtime(TAG, "Another Xposed service tried to connect: ${boundService.frameworkName}. Ignoring.")
                    return
                }
                Log.runtime(TAG, "LSPosed service connected: ${boundService.frameworkName} v${boundService.frameworkVersion}")
                updateState(ConnectionState.Connected(boundService))
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service == deadService) {
                    Log.runtime(TAG, "LSPosed service died.")
                    updateState(ConnectionState.Disconnected)
                }
            }
        }

        XposedServiceHelper.registerListener(listener)
        Log.runtime(TAG, "ServiceManager initialized and listener registered.")
    }

    /** 添加状态监听器，添加后立即触发一次当前状态 */
    fun addConnectionListener(listener: (ConnectionState) -> Unit) {
        listeners.add(listener)
        listener(connectionState)
    }

    /** 移除状态监听器 */
    fun removeConnectionListener(listener: (ConnectionState) -> Unit) {
        listeners.remove(listener)
    }

    /** 更新状态并通知监听器，线程安全 */
    private fun updateState(newState: ConnectionState) {
        _connectionState.set(newState)
        notifyListeners(newState)
    }

    /** 通知所有监听器状态变化 */
    private fun notifyListeners(state: ConnectionState) {
        for (listener in listeners) {
            listener(state)
        }
    }
}

sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data class Connected(val service: XposedService) : ConnectionState
    data object Disconnected : ConnectionState
}