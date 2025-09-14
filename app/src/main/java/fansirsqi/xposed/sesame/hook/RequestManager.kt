package fansirsqi.xposed.sesame.hook

import android.Manifest
import androidx.annotation.RequiresPermission
import fansirsqi.xposed.sesame.entity.RpcEntity

// import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.NetworkUtils

/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
object RequestManager {
    private fun checkResult(result: String, method: String?): String {
        check(!(result.trim { it <= ' ' }.isEmpty())) { "Empty response from RPC method: $method" }
        return result
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getRpcBridge(): RpcBridge? {
        if (!NetworkUtils.isNetworkAvailable()) {
            Log.record("网络未连接，等待5秒")
            try {
                Thread.sleep(5000)
            } catch (ignored: InterruptedException) {
            }
            if (!NetworkUtils.isNetworkAvailable()) {
                val networkType = NetworkUtils.getNetworkType()
                Log.record("网络仍未连接，当前网络类型: $networkType，放弃本次请求...")
                return null
            }
        }
        var rpcBridge = ApplicationHook.rpcBridge
        if (rpcBridge == null) {
            Log.record("ApplicationHook.rpcBridge 为空，等待5秒")
            try {
                Thread.sleep(5000)
            } catch (ignored: InterruptedException) {
            }
            rpcBridge = ApplicationHook.rpcBridge
        }
        return rpcBridge
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity): String {
        // val result = ApplicationHook.rpcBridge.requestString(rpcEntity, 3, -1)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, 3, -1)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        // val result = ApplicationHook.rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?): String {
        // val result = ApplicationHook.rpcBridge.requestString(method, data)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, relation: String?): String {
        // val result = ApplicationHook.rpcBridge.requestString(method, data, relation)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, appName: String?, methodName: String?, facadeName: String?): String {
        // val result = ApplicationHook.rpcBridge.requestString(method, data, appName, methodName, facadeName)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, appName, methodName, facadeName)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String {
        // val result = ApplicationHook.rpcBridge.requestString(method, data, tryCount, retryInterval)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, tryCount, retryInterval)
        return checkResult(result, method)
    }

    fun requestString(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): String {
        // val result = ApplicationHook.rpcBridge.requestString(method, data, relation, tryCount, retryInterval)
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation, tryCount, retryInterval)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestObject(rpcEntity: RpcEntity?, tryCount: Int, retryInterval: Int) {
        // ApplicationHook.rpcBridge.requestObject(rpcEntity, tryCount, retryInterval)
        val rpcBridge = getRpcBridge() ?: return
        rpcBridge.requestObject(rpcEntity, tryCount, retryInterval)
    }
}
