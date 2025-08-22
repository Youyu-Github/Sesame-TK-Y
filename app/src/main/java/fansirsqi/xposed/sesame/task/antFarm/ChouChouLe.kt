package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

class ChouChouLe {
    enum class TaskStatus {
        TODO, FINISHED, RECEIVED, DONATION
    }

    // 定义任务结构体
    private class TaskInfo {
        var taskStatus: String? = null
        var title: String? = null
        var taskId: String? = null
        var innerAction: String? = null
        var rightsTimes: Int = 0
        var rightsTimesLimit: Int = 0

        val remainingTimes: Int
            get() = max(0, rightsTimesLimit - rightsTimes)
    }

    fun chouchoule() {
        try {
            val response = AntFarmRpcCall.queryLoveCabin(UserMap.currentUid)
            val jo = JSONObject(response)
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }

            val drawMachineInfo = jo.optJSONObject("drawMachineInfo")
            if (drawMachineInfo == null) {
                Log.error(TAG, "抽抽乐🎁[获取抽抽乐活动信息失败]")
                return
            }

            if (drawMachineInfo.has("dailyDrawMachineActivityId")) {
                doChouchoule("dailyDraw")
            }
            if (drawMachineInfo.has("ipDrawMachineActivityId")) {
                doChouchoule("ipDraw")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("chouchoule err:", t)
        }
    }

    /**
     * 执行抽抽乐
     *
     * @param drawType "dailyDraw" or "ipDraw" 普通装扮或者IP装扮
     */
    private fun doChouchoule(drawType: String) {
        var doubleCheck: Boolean
        do {
            doubleCheck = false
            try {
                val jo = JSONObject(AntFarmRpcCall.chouchouleListFarmTask(drawType))
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.error(TAG, if (drawType == "ipDraw") "IP抽抽乐任务列表获取失败" else "抽抽乐任务列表获取失败")
                    continue
                }
                val farmTaskList = jo.getJSONArray("farmTaskList") //获取任务列表
                val tasks = parseTasks(farmTaskList)
                for (task in tasks) {
                    GlobalThreadPools.sleep(5 * 1000L)
                    if (TaskStatus.FINISHED.name == task.taskStatus) {
                        if (receiveTaskAward(drawType, task.taskId)) { //领取奖励
                            Thread.sleep(5000L)
                            doubleCheck = true
                        }
                    } else if (TaskStatus.TODO.name == task.taskStatus) {
                        if (task.remainingTimes > 0 && "DONATION" != task.innerAction) {
                            if (doChouTask(drawType, task)) {
                                doubleCheck = true
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace("doChouchoule err:", t)
            }
        } while (doubleCheck)

        if ("ipDraw" == drawType) {
            handleIpDraw()
        } else {
            handleDailyDraw()
        }
    }

    @Throws(Exception::class)
    private fun parseTasks(array: JSONArray): MutableList<TaskInfo> {
        val list: MutableList<TaskInfo> = ArrayList<TaskInfo>()
        for (i in 0..<array.length()) {
            val item = array.getJSONObject(i)
            val info = TaskInfo()
            info.taskStatus = item.getString("taskStatus")
            info.title = item.getString("title")
            info.taskId = item.getString("bizKey")
            info.innerAction = item.optString("innerAction")
            info.rightsTimes = item.optInt("rightsTimes", 0)
            info.rightsTimesLimit = item.optInt("rightsTimesLimit", 0)
            list.add(info)
        }
        return list
    }

    private fun doChouTask(drawType: String, task: TaskInfo): Boolean {
        try {
            val s = AntFarmRpcCall.chouchouleDoFarmTask(drawType, task.taskId)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm((if (drawType == "ipDraw") "IP抽抽乐" else "抽抽乐") + "🧾️[任务: " + task.title + "]")
                if(task.title.equals("消耗饲料换机会")) {
                    GlobalThreadPools.sleep(1 * 1000L);
                } else {
                    GlobalThreadPools.sleep(5 * 1000L);
                }
                return true
            }
            return false
        } catch (t: Throwable) {
            Log.printStackTrace("执行抽抽乐任务 err:", t)
            return false
        }
    }

    /**
     * 领取任务奖励
     *
     * @param drawType "dailyDraw" or "ipDraw" 普通装扮或者IP装扮
     * @param taskId   任务ID
     * @return 是否领取成功
     */
    private fun receiveTaskAward(drawType: String, taskId: String?): Boolean {
        try {
            val s = AntFarmRpcCall.chouchouleReceiveFarmTaskAward(drawType, taskId)
            val jo = JSONObject(s)
            return ResChecker.checkRes(TAG, jo)
        } catch (t: Throwable) {
            Log.printStackTrace("receiveFarmTaskAward err:", t)
        }
        return false
    }

    /**
     * 执行IP抽抽乐
     */
    private fun handleIpDraw() {
        try {
            val jo = JSONObject(AntFarmRpcCall.queryDrawMachineActivity())
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }

            val activity = jo.getJSONObject("drawMachineActivity")
            val endTime = activity.getLong("endTime")
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[" + activity.optString("activityId") + "]抽奖活动已结束")
                return
            }

            val drawTimes = jo.optInt("drawTimes", 0)
            for (i in 0..<drawTimes) {
                drawPrize("IP抽抽乐", AntFarmRpcCall.drawMachine())
                GlobalThreadPools.sleep(5 * 1000L)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("handleIpDraw err:", t)
        }
    }

    /**
     * 执行正常抽抽乐
     */
    private fun handleDailyDraw() {
        try {
            val jo = JSONObject(AntFarmRpcCall.enterDrawMachine())
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.record(TAG, "抽奖活动进入失败")
                return
            }

            val userInfo = jo.getJSONObject("userInfo")
            val drawActivityInfo = jo.getJSONObject("drawActivityInfo")
            val endTime = drawActivityInfo.getLong("endTime")
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[" + drawActivityInfo.optString("activityId") + "]抽奖活动已结束")
                return
            }

            val leftDrawTimes = userInfo.optInt("leftDrawTimes", 0)
            val activityId = drawActivityInfo.optString("activityId")

            for (i in 0..<leftDrawTimes) {
                val call = if (activityId == "null") AntFarmRpcCall.DrawPrize() else AntFarmRpcCall.DrawPrize(activityId)
                drawPrize("抽抽乐", call)
                GlobalThreadPools.sleep(5 * 1000L)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("handleDailyDraw err:", t)
        }
    }

    /**
     * 领取抽抽乐奖品
     *
     * @param prefix   抽奖类型
     * @param response 服务器返回的结果
     */
    private fun drawPrize(prefix: String?, response: String) {
        try {
            val jo = JSONObject(response)
            if (ResChecker.checkRes(TAG, jo)) {
                val title = jo.getString("title")
                val prizeNum = jo.optInt("prizeNum", 1)
                Log.farm(prefix + "🎁[领取: " + title + "*" + prizeNum + "]")
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        private val TAG: String = ChouChouLe::class.java.getSimpleName()
    }
}