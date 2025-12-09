package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.max
import kotlin.math.abs
import kotlin.random.Random


class ChouChouLe {
    enum class TaskStatus {
        TODO, FINISHED, RECEIVED
    }

    private class TaskInfo {
        var taskStatus: String? = null
        var title: String? = null
        var taskId: String? = null
        var innerAction: String? = null
        var rightsTimes: Int = 0
        var rightsTimesLimit: Int = 0
        // FIX: Add missing properties directly to the TaskInfo class.
        var awardType: String? = null
        var awardCount: Int = 0
        var targetUrl: String? = null

        val remainingTimes: Int
            get() = max(0, rightsTimesLimit - rightsTimes)
    }

    fun chouchoule() {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            if (currentHour < 8) {
                return
            }

            val response = AntFarmRpcCall.queryLoveCabin(UserMap.currentUid)
            val jo = JSONObject(response)
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }

            val drawMachineInfo = jo.optJSONObject("drawMachineInfo") ?: run {
                Log.error(TAG, "抽抽乐🎁[获取抽抽乐活动信息失败]")
                return
            }

            if (drawMachineInfo.has("dailyDrawMachineActivityId")) {
                // Log.farm("开始执行[普通抽抽乐]任务...")
                doChouchoule("dailyDraw")
            }
            if (drawMachineInfo.has("ipDrawMachineActivityId")) {
                // Log.farm("开始执行[IP抽抽乐]任务...")
                doChouchoule("ipDraw")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.chouchoule err:", t)
        }
    }

    private fun doChouchoule(drawType: String) {
        val drawTypeName = if (drawType == "ipDraw") "IP抽抽乐" else "普通抽抽乐"
        var doubleCheck: Boolean
        do {
            doubleCheck = false
            try {
                val jo = JSONObject(AntFarmRpcCall.chouchouleListFarmTask(drawType))
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.error(TAG, "$drawTypeName 任务列表获取失败")
                    continue
                }

                val farmTaskList = jo.getJSONArray("farmTaskList")
                val tasks = parseTasks(farmTaskList)

                for (task in tasks) {
                    when (TaskStatus.valueOf(task.taskStatus!!)) {
                        TaskStatus.FINISHED -> {
                            Log.farm("$drawTypeName 🧾️[领取任务奖励: ${task.title}]")
                            // Check if taskId is not null before calling the function.
                            if (task.taskId != null && receiveTaskAward(drawType, task.taskId!!)) {
                                GlobalThreadPools.sleep(2 * 1000L)
                                doubleCheck = true // 领取奖励后需要重新检查任务列表和抽奖次数
                            }
                        }
                        TaskStatus.TODO -> {
                            if (task.remainingTimes > 0 && "DONATION" != task.innerAction) {
                                Log.farm("$drawTypeName 🧾️[执行任务: ${task.title}]")
                                if (doChouTask(drawType, task)) {
                                    GlobalThreadPools.sleep(if (task.title == "消耗饲料换机会") 2 * 1000L else 5 * 1000L)
                                    doubleCheck = true // 完成任务后需要重新检查
                                }
                            }
                        }
                        TaskStatus.RECEIVED -> {
                            // Already received, do nothing
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace("$TAG.doChouchoule err for $drawType:", t)
            }
        } while (doubleCheck)

        // All tasks are done, now perform the draws
        if ("ipDraw" == drawType) {
            handleIpDraw()
        } else {
            handleDailyDraw()
        }
    }

    @Throws(Exception::class)
    private fun parseTasks(array: JSONArray): List<TaskInfo> {
        return List(array.length()) { i ->
            val item = array.getJSONObject(i)
            TaskInfo().apply {
                taskStatus = item.getString("taskStatus")
                title = item.getString("title")
                taskId = item.getString("bizKey")
                innerAction = item.optString("innerAction")
                rightsTimes = item.optInt("rightsTimes", 0)
                rightsTimesLimit = item.optInt("rightsTimesLimit", 0)
                // FIX: Remove the unresolved 'info' reference.
                awardType = item.optString("awardType");
                awardCount = item.optInt("awardCount", 0);
                targetUrl = item.optString("targetUrl", "");
            }
        }
    }
    
    // Add the missing 'receiveTaskAward' function.
    private fun receiveTaskAward(drawType: String, taskId: String): Boolean {
        try {
            // FIX: Corrected the RPC method name to match AntFarmRpcCall.kt
            val s = AntFarmRpcCall.chouchouleReceiveFarmTaskAward(drawType, taskId)
            val jo = JSONObject(s)
            return ResChecker.checkRes(TAG, jo)
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.receiveTaskAward err:", t)
            return false
        }
    }

    private fun doChouTask(drawType: String, task: TaskInfo): Boolean {
        try {
            // Use non-null assertion for taskId as the function requires a non-null String.
            val s = AntFarmRpcCall.chouchouleDoFarmTask(drawType, task.taskId!!)
            val jo = JSONObject(s)
            return ResChecker.checkRes(TAG, jo)
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.doChouTask err:", t)
            return false
        }
    }

    /**
    * 处理广告任务
    */
    private fun handleAdTask(drawType: String, task: TaskInfo): Boolean {
        try {
            val referToken = AntFarm.loadAntFarmReferToken()
            val taskSceneCode = if (drawType == "ipDraw") "ANTFARM_IP_DRAW_TASK" else "ANTFARM_DAILY_DRAW_TASK"

            // 如果有referToken，尝试执行广告任务
            if (!referToken.isNullOrEmpty()) {
                val response = AntFarmRpcCall.xlightPlugin(referToken, "HDWFCJGXNZW_CUSTOM_20250826173111")
                val jo = JSONObject(response)

                if (jo.optString("retCode") == "0") {
                    val resData = jo.getJSONObject("resData")
                    val adList = resData.optJSONArray("adList")

                    if (adList != null && adList.length() > 0) {
                        // 检查是否有猜一猜任务
                        val playingResult = resData.optJSONObject("playingResult")
                        if (playingResult != null &&
                            playingResult.optString("playingStyleType") == "XLIGHT_GUESS_PRICE_FEEDS"
                        ) {
                            return handleGuessTask(drawType, task, adList, playingResult)
                        }
                    }
                }
                Log.record(TAG, "浏览广告任务[没有可用广告或不支持，使用普通完成方式]")
            } else {
                Log.record(TAG, "浏览广告任务[没有可用Token，请手动看一起广告]")
            }

            // 没有token或广告任务失败，使用普通完成方式
            val outBizNo = "${task.taskId}_${System.currentTimeMillis()}_${Integer.toHexString(Random.nextInt(0xFFFFFF))}"
            // Use non-null assertion for taskId.
            val response = AntFarmRpcCall.finishTask(task.taskId!!, taskSceneCode, outBizNo)
            val jo = JSONObject(response)

            if (jo.optBoolean("success", false)) {
                val taskName = if (drawType == "ipDraw") "IP抽抽乐" else "抽抽乐"
                Log.farm("$taskName🧾️[任务: ${task.title}]")
                GlobalThreadPools.sleep(3 * 1000L)
                return true
            }
            return false
        } catch (t: Throwable) {
            Log.printStackTrace("处理广告任务 err:", t)
            return false
        }
    }

    /**
    * 处理猜一猜任务
    */
    private fun handleGuessTask(
        drawType: String,
        task: TaskInfo,
        adList: JSONArray,
        playingResult: JSONObject
    ): Boolean {
        try {
            // 找到最接近目标价格的广告
            var correctPrice = -1
            var targetAdId = ""
            val targetPrice = 11888 // 目标价格

            for (i in 0 until adList.length()) {
                val ad = adList.getJSONObject(i)
                val schemaJson = ad.optString("schemaJson", "")
                if (schemaJson.isNotEmpty()) {
                    val schema = JSONObject(schemaJson)
                    val price = schema.optInt("price", -1)
                    if (price > 0) {
                        if (correctPrice == -1 || abs(price - targetPrice) < abs(correctPrice - targetPrice)) {
                            correctPrice = price
                            targetAdId = ad.optString("adId", "")
                        }
                    }
                }
            }

            if (correctPrice > 0 && targetAdId.isNotEmpty()) {
                // 提交猜价格结果
                val playBizId = playingResult.optString("playingBizId", "")
                val eventRewardDetail = playingResult.optJSONObject("eventRewardDetail")
                eventRewardDetail?.optJSONArray("eventRewardInfoList")?.let { eventRewardInfoList ->
                    if (eventRewardInfoList.length() > 0) {
                        val playEventInfo = eventRewardInfoList.getJSONObject(0)
                        val taskSceneCode = if (drawType == "ipDraw") "ANTFARM_IP_DRAW_TASK" else "ANTFARM_DAILY_DRAW_TASK"

                        // Use non-null assertion for taskId.
                        val response = AntFarmRpcCall.finishAdTask(
                            playBizId, playEventInfo, task.taskId!!, taskSceneCode
                        )
                        val jo = JSONObject(response)

                        if (jo.optJSONObject("resData")?.optBoolean("success", false) == true) {
                            val taskName = if (drawType == "ipDraw") "IP抽抽乐" else "抽抽乐"
                            Log.farm("$taskName🧾️[猜价格任务完成: ${task.title}, 猜中价格: $correctPrice]")
                            GlobalThreadPools.sleep(3 * 1000L)
                            return true
                        }
                    }
                }
            }

            Log.record(TAG, "猜价格任务[未找到合适价格，使用普通完成方式]")
            return false
        } catch (t: Throwable) {
            Log.printStackTrace("处理猜价格任务 err:", t)
            return false
        }
    }

    private fun handleIpDraw() {
        val scene = "ipDrawMachine"
        try {
            val jo = JSONObject(AntFarmRpcCall.queryDrawMachineActivity(scene))
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }

            val activity = jo.getJSONObject("drawMachineActivity")
            if (System.currentTimeMillis() > activity.getLong("endTime")) {
                Log.record(TAG, "IP抽抽乐[${activity.optString("activityId")}]活动已结束")
                return
            }

            val drawTimes = jo.optInt("drawTimes", 0)
            if (drawTimes > 0) {
                Log.farm("IP抽抽乐✨[剩余抽奖次数: $drawTimes]")
                for (i in 0 until drawTimes) {
                    drawPrize("IP抽抽乐", AntFarmRpcCall.drawMachine(scene))
                    GlobalThreadPools.sleep(5 * 1000L)
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.handleIpDraw err:", t)
        }
    }

    private fun handleDailyDraw() {
        val scene = "dailyDrawMachine"
        try {
            val jo = JSONObject(AntFarmRpcCall.queryDrawMachineActivity(scene))
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            val activity = jo.getJSONObject("drawMachineActivity")
            if (System.currentTimeMillis() > activity.getLong("endTime")) {
                Log.record(TAG, "普通抽抽乐[${activity.optString("activityId")}]活动已结束")
                return
            }

            val drawTimes = jo.optInt("drawTimes", 0)
            if (drawTimes > 0) {
                Log.farm("普通抽抽乐✨[剩余抽奖次数: $drawTimes]")
                for (i in 0 until drawTimes) {
                    drawPrize("普通抽抽乐", AntFarmRpcCall.drawMachine(scene))
                    GlobalThreadPools.sleep(5 * 1000L)
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.handleDailyDraw err:", t)
        }
    }

    private fun drawPrize(prefix: String, response: String) {
        try {
            val jo = JSONObject(response)
            if (ResChecker.checkRes(TAG, jo)) {
                val prize = jo.getJSONObject("drawMachinePrize")
                val title = prize.getString("title")
                Log.farm("$prefix🎁[抽中: $title]")
            } else {
                Log.farm("$prefix🎁[抽奖失败: ${jo.optString("memo")}]")
            }
        } catch (e: Exception) {
            Log.printStackTrace("$TAG.drawPrize err:", e)
        }
    }

    companion object {
        private val TAG: String = ChouChouLe::class.java.simpleName
    }
}