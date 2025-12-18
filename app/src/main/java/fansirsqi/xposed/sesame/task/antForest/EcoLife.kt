package fansirsqi.xposed.sesame.task.antForest

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.newutil.DataStore.put
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.StringUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

object EcoLife {
    val TAG: String = EcoLife::class.java.getSimpleName()

    /**
     * 执行绿色行动任务，包括查询任务开通状态、开通绿色任务、执行打卡任务等操作。
     * 1. 调用接口查询绿色行动的首页数据，检查是否成功。
     * 2. 如果绿色任务尚未开通，且用户未开通绿色任务，则记录日志并返回。
     * 3. 如果绿色任务尚未开通，且用户已开通绿色任务，则尝试开通绿色任务。
     * 4. 开通绿色任务成功后，再次查询任务状态，并更新数据。
     * 5. 获取任务的日期标识和任务列表，执行打卡任务。
     * 6. 如果绿色打卡设置为启用，执行 `ecoLifeTick` 方法提交打卡任务。
     * 7. 如果光盘打卡设置为启用，执行 `photoGuangPan` 方法上传光盘照片。
     * 8. 异常发生时，记录错误信息并打印堆栈。
     */
    fun ecoLife() {
        try {
            // 查询首页信息
            var jsonObject = JSONObject(AntForestRpcCall.ecolifeQueryHomePage())
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.ecoLife.queryHomePage", jsonObject.optString("resultDesc"))
                return
            }
            var data = jsonObject.getJSONObject("data")


            // 获取当天的积分和任务列表
            var dayPoint = data.optString("dayPoint", "0")
            if (dayPoint == "0") {
                Log.error(TAG, "不知道什么B原因自己去绿色行动找")
                return
            }

            if (AntForest.ecoLifeOption!!.value.contains("plate")) {
                // 光盘行动
                photoGuangPan(dayPoint)
            }

            val actionListVO = data.getJSONArray("actionListVO")
            // 绿色打卡
            if (AntForest.ecoLifeOption!!.value.contains("tick")) {
                if (!data.getBoolean("openStatus")) {
                    if (!openEcoLife() || !AntForest.ecoLifeOpen!!.value) {
                        return
                    }
                    jsonObject = JSONObject(AntForestRpcCall.ecolifeQueryHomePage())
                    data = jsonObject.getJSONObject("data")
                    dayPoint = data.getString("dayPoint")
                }
                ecoLifeTick(actionListVO, dayPoint)
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "ecoLife err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 封装绿色任务开通的逻辑
     *
     * @return 是否成功开通绿色任务
     */
    @Throws(JSONException::class)
    fun openEcoLife(): Boolean {
        GlobalThreadPools.sleep(300)
        val jsonObject = JSONObject(AntForestRpcCall.ecolifeOpenEcolife())
        if (!jsonObject.optBoolean("success")) {
            Log.runtime(TAG + ".ecoLife.openEcolife", jsonObject.optString("resultDesc"))
            return false
        }
        val opResult = JsonUtil.getValueByPath(jsonObject, "data.opResult")
        if ("true" != opResult) {
            return false
        }
        Log.forest("绿色任务🍀报告大人，开通成功(～￣▽￣)～可以愉快的玩耍了")
        GlobalThreadPools.sleep(300)
        return true
    }

    /**
     * 执行绿色行动打卡任务，遍历任务列表，依次提交每个未完成的任务。
     * 1. 遍历给定的任务列表（`actionListVO`），每个任务项包含多个子任务。
     * 2. 对于每个子任务，检查其是否已完成，如果未完成则提交打卡请求。
     * 3. 特别处理任务 ID 为 "photoguangpan" 的任务，跳过该任务的打卡。
     * 4. 如果任务打卡成功，记录成功日志；否则记录失败原因。
     * 5. 每次打卡请求后，等待 500 毫秒以避免请求过于频繁。
     * 6. 异常发生时，记录详细的错误信息。
     *
     * @param actionListVO 任务列表，每个任务包含多个子任务
     * @param dayPoint     任务的日期标识，用于标识任务的日期
     */
    fun ecoLifeTick(actionListVO: JSONArray, dayPoint: String?) {
        try {
            val source = "source"
            for (i in 0..<actionListVO.length()) {
                val actionVO = actionListVO.getJSONObject(i)
                val actionItemList = actionVO.getJSONArray("actionItemList")
                for (j in 0..<actionItemList.length()) {
                    val actionItem = actionItemList.getJSONObject(j)
                    if (!actionItem.has("actionId")) continue
                    if (actionItem.getBoolean("actionStatus")) continue
                    val actionId = actionItem.getString("actionId")
                    val actionName = actionItem.getString("actionName")
                    if ("photoguangpan" == actionId) continue
                    GlobalThreadPools.sleep(300)
                    val jo = JSONObject(AntForestRpcCall.ecolifeTick(actionId, dayPoint, source))
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.forest("绿色打卡🍀[" + actionName + "]") // 成功打卡日志
                    } else {
                        // 记录失败原因
                        Log.error(TAG + jo.getString("resultDesc"))
                        Log.error(TAG + jo)
                    }
                    GlobalThreadPools.sleep(300)
                }
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "ecoLifeTick err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 执行光盘行动任务，上传餐前餐后照片并提交任务。
     * 1. 查询当前任务的状态。
     * 2. 如果任务未完成，检查是否已有餐前餐后照片的URL，如果没有则从接口获取并保存。
     * 3. 上传餐前餐后照片，上传成功后提交任务，标记任务为完成。
     * 4. 如果任务已完成，则不做任何操作。
     * 5. 如果遇到任何错误，记录错误信息并停止执行。
     *
     * @param dayPoint 任务的日期标识，用于标识任务的日期
     */
    fun photoGuangPan(dayPoint: String?) {
        try {
            if (Status.hasFlagToday("EcoLife::photoGuangPan")) return

            val source = "renwuGD" // 任务来源标识

            val typeRef: TypeReference<MutableList<MutableMap<String?, String?>>> =
                object : TypeReference<MutableList<MutableMap<String?, String?>>>() {
                }
            val allPhotos: MutableList<MutableMap<String?, String?>> =
                DataStore.getOrCreate("plate", typeRef)
            Log.runtime("$TAG [DEBUG] guangPanPhoto 数据内容: $allPhotos")
            // 查询今日任务状态
            var str = AntForestRpcCall.ecolifeQueryDish(source, dayPoint)
            var jo = JSONObject(str)
            // 如果请求失败，则记录错误信息并返回
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.runtime("$TAG.photoGuangPan.ecolifeQueryDish", jo.optString("resultDesc"))
                return
            }
            var photo: MutableMap<String?, String?>? = HashMap<String?, String?>()
            val data = jo.optJSONObject("data")
            if (data != null) {
                val beforeMealsImageUrl = data.optString("beforeMealsImageUrl")
                val afterMealsImageUrl = data.optString("afterMealsImageUrl")
                // 如果餐前和餐后照片URL都存在，进行提取
                if (!StringUtil.isEmpty(beforeMealsImageUrl) && !StringUtil.isEmpty(
                        afterMealsImageUrl
                    )
                ) {
                    // 使用正则从URL中提取照片的路径部分
                    val pattern = Pattern.compile("img/(.*)/original")
                    val beforeMatcher = pattern.matcher(beforeMealsImageUrl)
                    if (beforeMatcher.find()) {
                        photo!!.put("before", beforeMatcher.group(1))
                    }
                    val afterMatcher = pattern.matcher(afterMealsImageUrl)
                    if (afterMatcher.find()) {
                        photo!!.put("after", afterMatcher.group(1))
                    }
                    // 避免重复添加相同的照片信息
                    var exists = false
                    for (p in allPhotos) {
                        if (p["before"] == photo!!["before"] && p["after"] == photo["after"]
                        ) {
                            exists = true
                            break
                        }
                    }
                    if (!exists) {
                        allPhotos.add(photo!!)
                        put("plate", allPhotos)
                    }
                }
            }
            if ("SUCCESS" == JsonUtil.getValueByPath(jo, "data.status")) {
                return
            }
            if (allPhotos.isEmpty()) {
                if (!Status.hasFlagToday("EcoLife::plateNotify0")) {
                    Log.forest("光盘行动🍛缓存中没有照片数据")
                    Status.setFlagToday("EcoLife::plateNotify0")
                }
                photo = null
            } else {
                photo = allPhotos[RandomUtil.nextInt(0, allPhotos.size)]
            }
            if (photo == null) {
                if (!Status.hasFlagToday("EcoLife::plateNotify1")) {
                    Log.forest("光盘行动🍛请先完成一次光盘打卡")
                    Status.setFlagToday("EcoLife::plateNotify1")
                }
                return
            }
            str = AntForestRpcCall.ecolifeUploadDishImage(
                "BEFORE_MEALS",
                photo["before"],
                0.16571736,
                0.07448776,
                0.7597949,
                dayPoint
            )
            jo = JSONObject(str)
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            GlobalThreadPools.sleep(3000)
            str = AntForestRpcCall.ecolifeUploadDishImage(
                "AFTER_MEALS",
                photo["after"],
                0.00040030346,
                0.99891376,
                0.0006858421,
                dayPoint
            )
            jo = JSONObject(str)
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            // 提交任务
            str = AntForestRpcCall.ecolifeTick("photoguangpan", dayPoint, source)
            jo = JSONObject(str)
            // 如果提交失败，记录错误信息并返回
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            // 任务完成，输出完成日志
            val toastMsg = "光盘行动🍛任务完成#" + jo.getJSONObject("data").getString("toastMsg")
            Status.setFlagToday("EcoLife::photoGuangPan")
            Log.forest(toastMsg)
            Toast.show(toastMsg)
        } catch (t: Throwable) {
            // 捕获异常，记录错误信息和堆栈追踪
            Log.runtime(TAG, "photoGuangPan err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
