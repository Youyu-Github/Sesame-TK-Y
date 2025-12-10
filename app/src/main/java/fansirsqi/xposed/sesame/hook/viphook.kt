package fansirsqi.xposed.sesame.hook

import org.json.JSONObject

object VIPHook {

    private const val TAG = "VIPHook"

    /** 方法名 -> handler */
    private val rpcHandlerMap: MutableMap<String, (JSONObject) -> Unit> = mutableMapOf()

    /** 注册 RPC 回调处理器 */
    fun registerRpcHandler(methodName: String, handler: (JSONObject) -> Unit) {
        rpcHandlerMap[methodName] = handler
    }

    /** 调用 handler */
    fun handleRpc(method: String, paramsJson: JSONObject) {
        rpcHandlerMap[method]?.invoke(paramsJson)
    }

}