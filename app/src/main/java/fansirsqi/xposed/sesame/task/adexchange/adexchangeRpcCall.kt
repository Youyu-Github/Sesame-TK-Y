package fansirsqi.xposed.sesame.task.adexchange

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONObject
import java.net.URLDecoder

object XLightRpcCall {

    // 固定 SDK 信息
    private const val AD_COMPONENT_TYPE = "FEEDS"
    private const val AD_COMPONENT_VERSION = "4.28.66"
    private const val ENABLE_FUSION = true
    private const val NETWORK_TYPE = "WIFI"
    private const val PAGE_NO = 1
    private const val UNION_APP_ID = "2060090000304921"
    private const val XLIGHT_RUNTIME_SDK_VERSION = "4.28.66"
    private const val XLIGHT_SDK_TYPE = "h5"
    private const val XLIGHT_SDK_VERSION = "4.28.66"

    /**
     * 调用 xlightPlugin
     * @param referToken referToken 字符串
     * @param pageUrl 当前页面 url
     * @param pageFrom 页面来源
     */
    fun xlightPlugin(referToken: String, pageUrl: String, pageFrom: String,session: String): String {
        return try {
            // 构建 positionRequest
            val positionRequest = JSONObject().apply {
                put("extMap", JSONObject())
                put("referInfo", JSONObject().apply {
                    put("referToken", referToken)
                })
                put("searchInfo", JSONObject())
                put("spaceCode", "BABA_FARM_TASK_task_70000") // 可以根据需要改成参数
            }

            // 构建 sdkPageInfo
            val sdkPageInfo = JSONObject().apply {
                put("adComponentType", AD_COMPONENT_TYPE)
                put("adComponentVersion", AD_COMPONENT_VERSION)
                put("enableFusion", ENABLE_FUSION)
                put("networkType", NETWORK_TYPE)
                put("pageFrom", pageFrom)
                put("pageNo", PAGE_NO)
                put("pageUrl", pageUrl)
                put("session", session)
                put("unionAppId", UNION_APP_ID)
                put("usePlayLink", "true")
                put("xlightRuntimeSDKversion", XLIGHT_RUNTIME_SDK_VERSION)
                put("xlightSDKType", XLIGHT_SDK_TYPE)
                put("xlightSDKVersion", XLIGHT_SDK_VERSION)
            }

            // 构建最终请求参数
            val args = JSONObject().apply {
                put("positionRequest", positionRequest)
                put("sdkPageInfo", sdkPageInfo)
            }

            RequestManager.requestString(
                "com.alipay.adexchange.ad.facade.xlightPlugin",
                args.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 完成广告任务
     * @param playBizId 广告或任务的业务 ID
     * @param playEventInfo playEventInfo JSON 对象，直接传入完整事件信息
     * @return 接口返回的字符串
     */
    fun finishTask(playBizId: String, playEventInfo: JSONObject): String {
        return try {
            val args = JSONObject().apply {
                put("extendInfo", JSONObject())   // 固定空对象
                put("playBizId", playBizId)
                put("playEventInfo", playEventInfo)
                put("source", "adx")              // 固定来源
            }

            RequestManager.requestString(
                "com.alipay.adtask.biz.mobilegw.service.interaction.finish",
                args.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

object UrlUtil {

    /**
     * 从 URL 中提取指定参数
     * @param url 原始 URL 字符串
     * @param key 要提取的参数名，例如 "spaceCodeFeeds"
     * @return 参数值，如果不存在返回 null
     */
    fun getParam(url: String, key: String): String? {
        return try {
            // 找到 URL 中的查询部分
            val query = url.substringAfter("?", "")
            if (query.isEmpty()) return null

            // query 可能经过多次 URL 编码，先解码一次
            val decodedQuery = URLDecoder.decode(query, "UTF-8")

            // 按 & 分割每个参数
            val params = decodedQuery.split("&")
            for (param in params) {
                val pair = param.split("=")
                if (pair.size == 2 && pair[0] == key) {
                    return URLDecoder.decode(pair[1], "UTF-8")
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}