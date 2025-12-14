package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.extensions.JSONExtensions.toJSONArray
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.antFarm.AntFarm.AnimalFeedStatus
import fansirsqi.xposed.sesame.task.antFarm.AntFarm.AnimalInteractStatus
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Objects
import kotlin.math.abs


data object AntFarmFamily {
    private const val TAG = "小鸡家庭"

    /**
     * 家庭ID
     */
    private var groupId: String = ""

    /**
     * 家庭名称
     */
    private var groupName: String = ""

    /**
     * 家庭成员对象
     */
    private var familyAnimals: JSONArray = JSONArray()

    /**
     * 家庭成员列表
     */
    private var familyUserIds: MutableList<String> = mutableListOf()

    /**
     * 互动功能列表
     */
    private var familyInteractActions: JSONArray = JSONArray()

    /**
     * 美食配置对象
     */
    private var eatTogetherConfig: JSONObject = JSONObject()


    fun run(familyOptions: SelectModelField, notInviteList: SelectModelField) {
        try {
            enterFamily(familyOptions, notInviteList)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e.message, e)
        }
    }

    /**
     * 进入家庭
     */
    fun enterFamily(familyOptions: SelectModelField, notInviteList: SelectModelField) {
        try {
            val enterRes = JSONObject(AntFarmRpcCall.enterFamily())
            if (ResChecker.checkRes(TAG, enterRes)) {
                if (!enterRes.has("groupId")) {
                    Log.farm("请先开通小鸡家庭")
                    return
                }
                groupId = enterRes.getString("groupId")
                groupName = enterRes.getString("groupName")
                val familyAwardNum: Int = enterRes.optInt("familyAwardNum", 0)//奖励数量
                val familySignTips: Boolean = enterRes.optBoolean("familySignTips", false)//签到
                val assignFamilyMemberInfo: JSONObject? = enterRes.optJSONObject("assignFamilyMemberInfo")//分配成员信息-顶梁柱
                val feedFriendLimit: Boolean = enterRes.optBoolean("feedFriendLimit", false)//帮喂好友限制
                familyAnimals = enterRes.getJSONArray("animals")//家庭动物列表
                familyUserIds = (0..<familyAnimals.length())
                    .map { familyAnimals.getJSONObject(it).getString("userId") }
                    .toMutableList()
                familyInteractActions = enterRes.getJSONArray("familyInteractActions")//互动功能列表
                eatTogetherConfig = enterRes.getJSONObject("eatTogetherConfig")//美食配置对象

                if (familyOptions.value.contains("familySign") && familySignTips) {
                    familySign()
                }

                if (assignFamilyMemberInfo != null
                    && familyOptions.value.contains("assignRights")
                    && assignFamilyMemberInfo.getJSONObject("assignRights").getString("status") != "USED"
                ) {
                    if (assignFamilyMemberInfo.getJSONObject("assignRights").getString("assignRightsOwner") == UserMap.currentUid) {
                        assignFamilyMember(assignFamilyMemberInfo, familyUserIds)
                    } else {
                        Log.record("家庭任务🏡[使用顶梁柱特权] 不是家里的顶梁柱！")
                        familyOptions.value.remove("assignRights")
                    }
                }

                if (familyOptions.value.contains("familyClaimReward") && familyAwardNum > 0) {
                    familyClaimRewardList()
                }

                if (familyOptions.value.contains("feedFamilyAnimal") && !feedFriendLimit) {
                    familyFeedFriendAnimal(familyAnimals)
                }

                if (familyOptions.value.contains("eatTogetherConfig")) {
                    familyEatTogether(eatTogetherConfig, familyInteractActions, familyUserIds)
                }

                if (familyOptions.value.contains("deliverMsgSend")) {
                    deliverMsgSend(familyUserIds)
                }

                if (familyOptions.value.contains("shareToFriends")) {
                    familyShareToFriends(familyUserIds, notInviteList)
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e.message, e)
        }
    }


    /**
     * 家庭签到
     */
    fun familySign() {
        try {
            if (Status.hasFlagToday("farmfamily::dailySign")) return
            val res = JSONObject(AntFarmRpcCall.familyReceiveFarmTaskAward("FAMILY_SIGN_TASK"))
            if (ResChecker.checkRes(TAG, res)) {
                Log.farm("家庭任务🏡每日签到")
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e.message, e)
        }
    }

    /**
     * 领取家庭奖励
     */
    fun familyClaimRewardList() {
        try {
            var jo = JSONObject(AntFarmRpcCall.familyAwardList())
            if (ResChecker.checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("familyAwardRecordList")
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    if (jo.optBoolean("expired")
                        || jo.optBoolean("received", true)
                        || jo.has("linkUrl")
                        || (jo.has("operability") && !jo.getBoolean("operability"))
                    ) {
                        continue
                    }
                    val rightId = jo.getString("rightId")
                    val awardName = jo.getString("awardName")
                    val count = jo.optInt("count", 1)
                    val receveRes = JSONObject(AntFarmRpcCall.receiveFamilyAward(rightId))
                    if (ResChecker.checkRes(TAG, receveRes)) {
                        Log.farm("家庭奖励🏆: $awardName x $count")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "家庭领取奖励", t)
        }
    }

    /**
     * 顶梁柱
     */
    fun assignFamilyMember(jsonObject: JSONObject, userIds: MutableList<String>) {
        try {
            userIds.remove(UserMap.currentUid)
            //随机选一个家庭成员
            if (userIds.isEmpty()) {
                return
            }
            val beAssignUser = userIds[RandomUtil.nextInt(0, userIds.size - 1)]
            //随机获取一个任务类型
            val assignConfigList = jsonObject.getJSONArray("assignConfigList")
            val assignConfig = assignConfigList.getJSONObject(RandomUtil.nextInt(0, assignConfigList.length() - 1))
            val jo = JSONObject(AntFarmRpcCall.assignFamilyMember(assignConfig.getString("assignAction"), beAssignUser))
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("家庭任务🏡[使用顶梁柱特权] ${assignConfig.getString("assignDesc")}")
//                val sendRes = JSONObject(AntFarmRpcCall.sendChat(assignConfig.getString("chatCardType"), beAssignUser))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }


    /**
     * 帮好友喂小鸡
     * @param animals 家庭动物列表
     */
    fun familyFeedFriendAnimal(animals: JSONArray) {
        try {
            for (i in 0 until animals.length()) {
                val animal = animals.getJSONObject(i)
                val status = animal.getJSONObject("animalStatusVO")

                val interactStatus = status.getString("animalInteractStatus")
                val feedStatus = status.getString("animalFeedStatus")

                // 过滤非 HOME / HUNGRY 的
                if (interactStatus != AnimalInteractStatus.HOME.name ||
                    feedStatus != AnimalFeedStatus.HUNGRY.name) continue

                val groupId = animal.getString("groupId")
                val farmId = animal.getString("farmId")
                val userId = animal.getString("userId")

                // 非好友 → 跳过
                if (!UserMap.getUserIdSet().contains(userId)) {
                    Log.error(TAG, "$userId 不是你的好友！ 跳过家庭喂食")
                    continue
                }

                val flagKey = "farm::feedFriendLimit::$userId"

                // 如果该用户已经记录今日上限 → 跳过
                if (Status.hasFlagToday(flagKey)) {
                    Log.runtime("[$userId] 今日喂鸡次数已达上限（已记录）🥣，跳过")
                    continue
                }

                // 调用 RPC
                val jo = JSONObject(AntFarmRpcCall.feedFriendAnimal(farmId, groupId))

                // 统一错误码检查
                if (!jo.optBoolean("success", false)) {
                    val code = jo.optString("resultCode")

                    if (code == "391") {
                        // 记录该用户今日不能再喂
                        Status.setFlagToday(flagKey)
                        Log.runtime("[$userId] 今日帮喂次数已达上限🥣，已记录为当日限制")
                    } else {
                        Log.error(TAG, "喂食失败 user=$userId code=$code msg=${jo.optString("memo")}")
                    }
                    continue
                }
                // 正常成功
                val foodStock = jo.optInt("foodStock")
                val maskName = UserMap.getMaskName(userId)
                Log.farm("家庭任务🏠帮喂好友🥣[$maskName]的小鸡180g #剩余${foodStock}g")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "familyFeedFriendAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }


    /**
     * 请客吃美食
     * @param eatTogetherConfig 美食配置对象
     * @param familyInteractActions 互动功能列表
     * @param familyUserIds 家庭成员列表
     */
    private fun familyEatTogether(eatTogetherConfig: JSONObject, familyInteractActions: JSONArray, familyUserIds: MutableList<String>) {
        try {
            var isEat = false
            val periodItemList = eatTogetherConfig.optJSONArray("periodItemList")
            if (periodItemList == null || periodItemList.length() == 0) {
                Log.error(TAG, "美食不足,无法请客,请检查小鸡厨房")
                return
            }
            if (familyInteractActions.length() > 0) {
                for (i in 0..<familyInteractActions.length()) {
                    val familyInteractAction = familyInteractActions.getJSONObject(i)
                    if ("EatTogether" == familyInteractAction.optString("familyInteractType")) {
                        val endTime = familyInteractAction.optLong("interactEndTime", 0)
                        val gaptime = endTime - System.currentTimeMillis()
                        Log.record("正在吃..${formatDuration(gaptime)} 吃完")
                        return
                    }
                }
            }
            var periodName = ""
            val currentTime = Calendar.getInstance()
            for (i in 0..<periodItemList.length()) {
                val periodItem = periodItemList.getJSONObject(i)
                val startHour = periodItem.optInt("startHour")
                val startMinute = periodItem.optInt("startMinute")
                val endHour = periodItem.optInt("endHour")
                val endMinute = periodItem.optInt("endMinute")
                val startTime = Calendar.getInstance()
                startTime.set(Calendar.HOUR_OF_DAY, startHour)
                startTime.set(Calendar.MINUTE, startMinute)
                val endTime = Calendar.getInstance()
                endTime.set(Calendar.HOUR_OF_DAY, endHour)
                endTime.set(Calendar.MINUTE, endMinute)
                if (currentTime.after(startTime) && currentTime.before(endTime)) {
                    periodName = periodItem.optString("periodName")
                    isEat = true
                    break
                }
            }
            if (!isEat) {
                Log.record("家庭任务🏠请客吃美食#当前时间不在美食时间段")
                return
            }
            if (Objects.isNull(familyUserIds) || familyUserIds.isEmpty()) {
                Log.record("家庭成员列表为空,无法请客")
                return
            }
            val array: JSONArray? = queryRecentFarmFood(familyUserIds.size)
            if (array == null) {
                Log.record("查询最近的几份美食为空,无法请客")
                return
            }
            val jo = JSONObject(AntFarmRpcCall.familyEatTogether(groupId, familyUserIds.toJSONArray(), array))
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("家庭任务🏠请客" + periodName + "#消耗美食" + familyUserIds.size + "份")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "familyEatTogether err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 查询最近的几份美食
     * @param queryNum 查询数量
     */
    fun queryRecentFarmFood(queryNum: Int): JSONArray? {
        try {
            val jo = JSONObject(AntFarmRpcCall.queryRecentFarmFood(queryNum))
            if (!ResChecker.checkRes(TAG, jo)) {
                return null
            }
            val cuisines = jo.getJSONArray("cuisines")
            var count = 0
            for (i in 0..<cuisines.length()) {
                val cuisine = cuisines.getJSONObject(i)
                count += cuisine.optInt("count")
            }
            if (cuisines != null && queryNum <= count) {
                return cuisines
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryRecentFarmFood err:", t)
        }
        return null
    }


    /**
     * 发送道早安
     * @param familyUserIds 家庭成员列表
     */
    fun deliverMsgSend(familyUserIds: MutableList<String>) {
        try {
            // 1. 时间窗口控制：仅允许在「早安时间段」内自动发送（06:00 ~ 10:00）
            val now = Calendar.getInstance()
            val startTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (now.before(startTime) || now.after(endTime)) {
                Log.record(TAG, "家庭任务🏠道早安#当前时间不在 06:00-10:00，跳过")
                return
            }

            // groupId 是 enterFamily 返回的家庭 ID，如果为空说明当前账号未开通家庭
            if (groupId.isEmpty()) {
                Log.record(TAG, "家庭任务🏠道早安#未检测到家庭 groupId，可能尚未加入家庭，跳过")
                return
            }

            // 本地去重：一天只发送一次，避免重复打扰
            if (Status.hasFlagToday("antFarm::deliverMsgSend")) {
                Log.record(TAG, "家庭任务🏠道早安#今日已在本地发送过，跳过")
                return
            }

            // 2. 远端任务状态校验：确认「道早安」任务是否仍为 TODO
            try {
                val taskTipsRes = JSONObject(AntFarmRpcCall.familyTaskTips(familyAnimals))
                if (!ResChecker.checkRes(TAG, taskTipsRes)) {
                    Log.error(TAG, "家庭任务🏠道早安#familyTaskTips 调用失败，跳过")
                    return
                }

                val taskTips = taskTipsRes.optJSONArray("familyTaskTips")
                if (taskTips == null || taskTips.length() == 0) {
                    // familyTaskTips 为空：要么今天已经完成，要么当前无早安任务
                    Log.record(TAG, "家庭任务🏠道早安#远端无 GREETING 任务，可能今日已完成，跳过")
                    Status.setFlagToday("antFarm::deliverMsgSend")
                    return
                }

                var hasGreetingTodo = false
                for (i in 0 until taskTips.length()) {
                    val item = taskTips.getJSONObject(i)
                    val bizKey = item.optString("bizKey")
                    val taskStatus = item.optString("taskStatus")
                    if ("GREETING" == bizKey && "TODO" == taskStatus) {
                        hasGreetingTodo = true
                        break
                    }
                }

                if (!hasGreetingTodo) {
                    Log.record(TAG, "家庭任务🏠道早安#GREETING 任务非 TODO 状态，跳过")
                    Status.setFlagToday("antFarm::deliverMsgSend")
                    return
                }
            } catch (e: Throwable) {
                // safety：远端任务判断异常时，为了避免误刷，多数情况下选择跳过
                Log.printStackTrace(TAG, "familyTaskTips 解析失败，出于安全考虑跳过道早安：", e)
                return
            }

            // 3. 构建好友 userId 列表（去掉自己）
            // 先移除当前用户自己的 ID，否则 DeliverMsgSend 等接口会因为参数不合法而报错
            familyUserIds.remove(UserMap.currentUid)
            if (familyUserIds.isEmpty()) {
                Log.record(TAG, "家庭任务🏠道早安#家庭成员仅自己一人，跳过")
                return
            }

            val userIds = JSONArray().apply {
                for (userId in familyUserIds) {
                    put(userId)
                }
            }

            // 4. 确认 AI 隐私协议（OpenAIPrivatePolicy 抓包见看我.txt 中 deliverChickInfoVO.privatePolicyId）
            val resp0 = JSONObject(AntFarmRpcCall.OpenAIPrivatePolicy())
            if (!ResChecker.checkRes(TAG, resp0)) {
                Log.error(TAG, "家庭任务🏠道早安#OpenAIPrivatePolicy 调用失败")
                return
            }

            // 5. 请求推荐早安场景（deliverSubjectRecommend）以获取事件上下文
            val resp1 = JSONObject(AntFarmRpcCall.deliverSubjectRecommend(userIds))
            if (!ResChecker.checkRes(TAG, resp1)) {
                Log.error(TAG, "家庭任务🏠道早安#deliverSubjectRecommend 调用失败")
                return
            }

            // 提取后续调用所需的关键字段（均为动态值，绝不可写死）
            val ariverRpcTraceId = resp1.getString("ariverRpcTraceId")
            val eventId = resp1.getString("eventId")
            val eventName = resp1.getString("eventName")
            val memo = resp1.optString("memo")
            val resultCode = resp1.optString("resultCode")
            val sceneId = resp1.getString("sceneId")
            val sceneName = resp1.getString("sceneName")
            val success = resp1.optBoolean("success", true)

            // 6. 调用 DeliverContentExpand，实际向 AI 请求生成完整早安文案
            val resp2 = JSONObject(
                AntFarmRpcCall.deliverContentExpand(
                    ariverRpcTraceId,
                    eventId,
                    eventName,
                    memo,
                    resultCode,
                    sceneId,
                    sceneName,
                    success,
                    userIds
                )
            )
            if (!ResChecker.checkRes(TAG, resp2)) {
                Log.error(TAG, "家庭任务🏠道早安#DeliverContentExpand 调用失败")
                return
            }

            val deliverId = resp2.getString("deliverId")

            // 7. 使用 deliverId 再次确认扩展内容，得到最终的早安文案
            val resp3 = JSONObject(AntFarmRpcCall.QueryExpandContent(deliverId))
            if (!ResChecker.checkRes(TAG, resp3)) {
                Log.error(TAG, "家庭任务🏠道早安#QueryExpandContent 调用失败")
                return
            }

            val content = resp3.getString("content")

            // 8. 最终发送早安消息：DeliverMsgSend
            val resp4 = JSONObject(AntFarmRpcCall.deliverMsgSend(groupId, userIds, content, deliverId))
            if (ResChecker.checkRes(TAG, resp4)) {
                Log.farm("家庭任务🏠道早安: $content 🌈")
                Status.setFlagToday("antFarm::deliverMsgSend")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "deliverMsgSend err:", t)
        }
    }

    /**
     * 好友分享家庭
     * @param familyUserIds 好友列表
     * @param notInviteList 不邀请列表
     */
    private fun familyShareToFriends(familyUserIds: MutableList<String>, notInviteList: SelectModelField) {
        try {
            if (Status.hasFlagToday("antFarm::familyShareToFriends")) {
                return
            }

            val familyValue: MutableSet<String?> = notInviteList.value
            val allUser: List<AlipayUser> = AlipayUser.getList()

            if (allUser.isEmpty()) {
                Log.error(TAG, "allUser is empty")
                return
            }

            // 打乱顺序，实现随机选取
            val shuffledUsers = allUser.shuffled()

            val inviteList = JSONArray()
            for (u in shuffledUsers) {
                if (!familyUserIds.contains(u.id) && !familyValue.contains(u.id)) {
                    inviteList.put(u.id)
                    if (inviteList.length() >= 6) {
                        break
                    }
                }
            }

            if (inviteList.length() == 0) {
                Log.error(TAG, "没有符合分享条件的好友")
                return
            }

            Log.runtime(TAG, "inviteList: $inviteList")

            val jo = JSONObject(AntFarmRpcCall.inviteFriendVisitFamily(inviteList))
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("家庭任务🏠分享好友")
                Status.setFlagToday("antFarm::familyShareToFriends")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "familyShareToFriends err:", t)
        }
    }


    /**
     * 通用时间差格式化（自动区分过去/未来）
     * @param diffMillis 任意时间戳（毫秒）
     * @return 易读字符串，如 "刚刚", "5分钟后", "3天前"
     */
    fun formatDuration(diffMillis: Long): String {
        val absSeconds = abs(diffMillis) / 1000

        val (value, unit) = when {
            absSeconds < 60 -> Pair(absSeconds, "秒")
            absSeconds < 3600 -> Pair(absSeconds / 60, "分钟")
            absSeconds < 86400 -> Pair(absSeconds / 3600, "小时")
            absSeconds < 2592000 -> Pair(absSeconds / 86400, "天")
            absSeconds < 31536000 -> Pair(absSeconds / 2592000, "个月")
            else -> Pair(absSeconds / 31536000, "年")
        }

        return when {
            absSeconds < 1 -> "刚刚"
            diffMillis > 0 -> "$value$unit 后"
            else -> "$value$unit 前"
        }
    }


}
