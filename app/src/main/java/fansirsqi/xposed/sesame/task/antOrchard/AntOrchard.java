package fansirsqi.xposed.sesame.task.antOrchard;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.data.StatusFlags;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antOrchard.UrlUtil;
import fansirsqi.xposed.sesame.task.antOrchard.XLightRpcCall;
import fansirsqi.xposed.sesame.newutil.TaskBlacklist;
import fansirsqi.xposed.sesame.util.Detector;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;

public class AntOrchard extends ModelTask {
    private static final String TAG = AntOrchard.class.getSimpleName();

    private String userId = UserMap.currentUid;
    private String treeLevel;
    private String[] wuaList;
    private Integer executeIntervalInt;
    
    private IntegerModelField executeInterval;
    private BooleanModelField receiveOrchardTaskAward;
    private BooleanModelField orchardSpreadManure; // 是否开启施肥
    private IntegerModelField orchardSpreadManureCount;
    private BooleanModelField batchHireAnimal;
    private SelectModelField dontHireList;
    private SelectModelField dontWeedingList;
    private SelectModelField assistFriendList;

    @Override
    public String getName() {
        return "芭芭农场";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.ORCHARD;
    }

    @Override
    public String getIcon() {
        return "AntOrchard.png";
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(executeInterval = new IntegerModelField("executeInterval", "执行间隔(毫秒)", 500));
        modelFields.addField(receiveOrchardTaskAward = new BooleanModelField("receiveOrchardTaskAward", "收取芭芭农场任务奖励", false));
        modelFields.addField(orchardSpreadManure = new BooleanModelField("orchardSpreadManure", "果树施肥", false));
        modelFields.addField(orchardSpreadManureCount = new IntegerModelField("orchardSpreadManureCount", "芭芭农场每日施肥次数", 0));
        modelFields.addField(assistFriendList = new SelectModelField("assistFriendList", "助力好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        // 保留Java版原有高级功能
        modelFields.addField(batchHireAnimal = new BooleanModelField("batchHireAnimal", "一键捉鸡除草", false));
        modelFields.addField(dontHireList = new SelectModelField("dontHireList", "除草 | 不雇佣好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(dontWeedingList = new SelectModelField("dontWeedingList", "除草 | 不除草好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        return modelFields;
    }

    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        try {
            Log.record(TAG, "执行开始-" + getName());
            executeIntervalInt = Math.max(executeInterval.getValue(), 500);

            String s = AntOrchardRpcCall.orchardIndex();
            JSONObject jo = new JSONObject(s);

            if (!"100".equals(jo.optString("resultCode"))) {
                Log.runtime(TAG, jo.optString("resultDesc", "orchardIndex 调用失败"));
                return;
            }

            if (jo.optBoolean("userOpenOrchard", false)) {
                JSONObject taobaoData = new JSONObject(jo.getString("taobaoData"));
                treeLevel = Integer.toString(taobaoData.getJSONObject("gameInfo")
                        .getJSONObject("plantInfo").getJSONObject("seedStage").getInt("stageLevel"));

                // 获取 userId: 优先从 teamMembers 获取（同步Kotlin逻辑），获取不到则使用 System Uid
                /*
                userId = null;
                if (jo.has("teamMembers")) {
                    JSONArray teamMembers = jo.getJSONArray("teamMembers");
                    for (int i = 0; i < teamMembers.length(); i++) {
                        JSONObject member = teamMembers.getJSONObject(i);
                        if (member.optBoolean("self", false)) {
                            userId = member.getString("alipayId");
                            break;
                        }
                    }
                }
                */
                if (userId == null) {
                    userId = UserMap.currentUid;
                }
                if (userId == null) {
                    Log.error(TAG, "无法获取 userId");
                    return;
                }

                // 七日礼包
                if (jo.has("lotteryPlusInfo")) {
                    drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"));
                }

                // 每日肥料
                extraInfoGet();

                // 如果有🥚 则进行砸🥚
                JSONObject goldenEggInfo = jo.optJSONObject("goldenEggInfo");
                if (goldenEggInfo != null) {
                    int unsmashedGoldenEggs = goldenEggInfo.optInt("unsmashedGoldenEggs");
                    if (unsmashedGoldenEggs > 0) {
                        smashedGoldenEgg(unsmashedGoldenEggs);
                    }
                }

                // 芭芭农场任务
                if (receiveOrchardTaskAward.getValue()) {
                    doOrchardDailyTask(userId);
                    triggerTbTask();
                }

                /**
                 * 返访奖励
                 * Visit Reward
                 */
                if (!Status.hasFlagToday(StatusFlags.FLAG_ANTORCHARD_WIDGET_DAILY_AWARD)) {
                    receiveOrchardVisitAward();
                }

                limitedTimeChallenge();

                // 施肥逻辑
                int manureCountValue = orchardSpreadManureCount.getValue();
                if (manureCountValue > 0 && orchardSpreadManure.getValue() && Status.canSpreadManureToday(userId)) {
                    GlobalThreadPools.sleep(200);
                    orchardSpreadManureLogic();
                }

                // 子任务活动
                if (manureCountValue >= 3 && manureCountValue < 10) {
                    querySubplotsActivity(3);
                } else if (manureCountValue >= 10) {
                    querySubplotsActivity(10);
                }

                // 助力好友
                orchardassistFriend();

                // 捉鸡除草 (Java版保留功能)
                if (batchHireAnimal.getValue()) {
                    try {
                        JSONObject joo = new JSONObject(AntOrchardRpcCall.friendList());
                        if (ResChecker.checkRes(TAG, joo)) {
                            if (!joo.optBoolean("hireCountOnceLimit", true) && !joo.optBoolean("hireCountOneDayLimit", true)) {
                                batchHireAnimalRecommend();
                            }
                        }
                    } catch (Exception e) {
                        Log.record(TAG, "获取除草信息异常: " + e.getMessage());
                    }
                }

            } else {
                getEnableField().setValue(0);
                Log.farm("请先开通芭芭农场！");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "芭芭农场主流程执行异常！");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    private boolean canSpreadManureContinue(int stageBefore, int stageAfter) {
        if (stageAfter - stageBefore > 1) {
            return true;
        } else {
            Log.record(TAG, "施肥只加0.01%进度今日停止施肥！");
            return false;
        }
    }

    private void gotHarvest(JSONObject orchardIndexData) {
        try {
            if (orchardIndexData.has("spreadManureActivity")) {
                JSONObject spreadManureStage = orchardIndexData.getJSONObject("spreadManureActivity").getJSONObject("spreadManureStage");
                if ("FINISHED".equals(spreadManureStage.getString("status"))) {
                    String sceneCode = spreadManureStage.getString("sceneCode");
                    String taskType = spreadManureStage.getString("taskType");
                    int awardCount = spreadManureStage.getInt("awardCount");
                    JSONObject joo = new JSONObject(AntOrchardRpcCall.receiveTaskAward(sceneCode, taskType));
                    if (joo.optBoolean("success")) {
                        Log.farm("芭芭农场丰收礼包🎁[返肥料奖励*" + awardCount + "]g");
                    } else {
                        Log.record(TAG, "芭芭农场 丰收礼包 错误：" + joo.optString("desc"));
                    }
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "gotHarvest error");
            Log.printStackTrace(TAG, e);
        }
    }

    private JSONObject checkCanExchange(JSONObject orchardIndexTaobaoData) throws Exception {
        JSONObject plantInfo = orchardIndexTaobaoData.getJSONObject("gameInfo").getJSONObject("plantInfo");
        boolean canExchange = plantInfo.getBoolean("canExchange");
        if (canExchange) {
            Log.farm("🎉 芭芭农场果树似乎可以兑换了！");
        }
        return plantInfo;
    }

    private void orchardSpreadManureLogic() {
        try {
            List<String> sourceList = List.of("DNHZ_NC_zhimajingnangSF", "widget_shoufei", "ch_appcenter__chsub_9patch");
            int count = 0;
            do {
                try {
                    String indexStr = AntOrchardRpcCall.orchardIndex();
                    JSONObject orchardIndexData = new JSONObject(indexStr);
                    if (!"100".equals(orchardIndexData.getString("resultCode"))) {
                        Log.runtime(TAG, orchardIndexData.getString("resultDesc"));
                        return;
                    }

                    gotHarvest(orchardIndexData);

                    JSONObject taobaoData = new JSONObject(orchardIndexData.getString("taobaoData"));
                    JSONObject plantInfo = checkCanExchange(taobaoData);

                    if (plantInfo.getBoolean("canExchange")) return;

                    JSONObject seedStage = plantInfo.getJSONObject("seedStage");
                    treeLevel = Integer.toString(seedStage.getInt("stageLevel"));

                    JSONObject accountInfo = taobaoData.getJSONObject("gameInfo").getJSONObject("accountInfo");
                    int happyPoint = Integer.parseInt(accountInfo.getString("happyPoint"));
                    int wateringCost = accountInfo.getInt("wateringCost");
                    int wateringLeftTimes = accountInfo.getInt("wateringLeftTimes");

                    if (count > 20) {
                        Log.runtime(TAG, "一次浇水不超过 " + count + " 次避免任务时间过长");
                        return;
                    }
                    if (happyPoint < wateringCost) {
                        Log.runtime(TAG, "芭芭农场肥料不足以施肥 " + wateringCost);
                        return;
                    }
                    if (wateringLeftTimes == 0) {
                        Log.runtime(TAG, "剩余施肥次数为 0");
                        return;
                    }

                    if ((200 - wateringLeftTimes) < orchardSpreadManureCount.getValue()) {

                        String randomSource = sourceList.get(new Random().nextInt(sourceList.size()));

                        JSONObject spreadManureData = new JSONObject(AntOrchardRpcCall.orchardSpreadManure(null, randomSource));

                        if (!"100".equals(spreadManureData.getString("resultCode"))) {
                            Log.record(TAG, "芭芭农场 orchardSpreadManure 错误：" + spreadManureData.getString("resultDesc"));
                            return;
                        }

                        JSONObject spreadTaobaoData = new JSONObject(spreadManureData.getString("taobaoData"));
                        String stageText = spreadTaobaoData.getJSONObject("currentStage").getString("stageText");
                        int dailyAppWateringCount = spreadTaobaoData.getJSONObject("statistics").getInt("dailyAppWateringCount");

                        Log.farm("今日芭芭农场已施肥💩 " + dailyAppWateringCount + " 次 [" + stageText + "]");
                        count++;

                        if (!canSpreadManureContinue(seedStage.getInt("totalValue"), spreadTaobaoData.getJSONObject("currentStage").getInt("totalValue"))) {
                            Status.spreadManureToday(userId);
                            return;
                        }
                        continue;
                    }
                } finally {
                    GlobalThreadPools.sleep(executeIntervalInt);
                }
                break;
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "芭芭农场施肥异常！");
            Log.printStackTrace(TAG, t);
        }
    }

    private void extraInfoGet() {
        try {
            JSONObject jo = new JSONObject(AntOrchardRpcCall.extraInfoGet());
            if ("100".equals(jo.getString("resultCode"))) {
                JSONObject fertilizerPacket = jo.getJSONObject("data").getJSONObject("extraData").getJSONObject("fertilizerPacket");

                if ("todayFertilizerWaitTake".equals(fertilizerPacket.getString("status"))) {
                    int num = fertilizerPacket.getInt("todayFertilizerNum");
                    JSONObject joSet = new JSONObject(AntOrchardRpcCall.extraInfoSet());
                    if ("100".equals(joSet.getString("resultCode"))) {
                        Log.farm("每日肥料💩[" + num + "g]");
                    } else {
                        Log.runtime(TAG, joSet.toString());
                    }
                }
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "extraInfoGet err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void drawLotteryPlus(JSONObject lotteryPlusInfo) {
        try {
            if (!lotteryPlusInfo.has("userSevenDaysGiftsItem")) return;
            String itemId = lotteryPlusInfo.getString("itemId");
            JSONObject jo = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem");
            JSONArray ja = jo.getJSONArray("userEverydayGiftItems");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo2 = ja.getJSONObject(i);
                if (jo2.getString("itemId").equals(itemId)) {
                    if (!jo2.getBoolean("received")) {
                        JSONObject jo3 = new JSONObject(AntOrchardRpcCall.drawLottery());
                        if ("100".equals(jo3.getString("resultCode"))) {
                            JSONArray userEverydayGiftItems = jo3.getJSONObject("lotteryPlusInfo").getJSONObject("userSevenDaysGiftsItem").getJSONArray("userEverydayGiftItems");
                            for (int j = 0; j < userEverydayGiftItems.length(); j++) {
                                JSONObject jo4 = userEverydayGiftItems.getJSONObject(j);
                                if (jo4.getString("itemId").equals(itemId)) {
                                    int awardCount = jo4.optInt("awardCount", 1);
                                    Log.farm("七日礼包🎁[获得肥料]#" + awardCount + "g");
                                    break;
                                }
                            }
                        } else {
                            Log.runtime(TAG, jo3.toString());
                        }
                    } else {
                        Log.record(TAG, "七日礼包已领取");
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "drawLotteryPlus err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void doOrchardDailyTask(String userId) {
        try {
            String s = AntOrchardRpcCall.orchardListTask();
            JSONObject jo = new JSONObject(s);
            if (!"100".equals(jo.optString("resultCode"))) {
                Log.record("doOrchardDailyTask响应异常", s);
                return;
            }

            boolean inTeam = jo.optBoolean("inTeam", false);
            Log.record(TAG, inTeam ? "当前为芭芭农场 team 模式（合种/帮帮种已开启）" : "当前为普通单人农场模式");

            if (jo.has("signTaskInfo")) {
                orchardSign(jo.getJSONObject("signTaskInfo"));
            }

            JSONArray jaTaskList = jo.getJSONArray("taskList");
            for (int i = 0; i < jaTaskList.length(); i++) {
                JSONObject task = jaTaskList.getJSONObject(i);

                if (!"TODO".equals(task.optString("taskStatus"))) continue;

                String actionType = task.optString("actionType");
                String sceneCode = task.optString("sceneCode");
                String taskId = task.optString("taskId");
                String groupId = task.optString("groupId");

                JSONObject displayConfig = task.optJSONObject("taskDisplayConfig");
                String title = (displayConfig != null) ? displayConfig.optString("title", "未知任务") : "未知任务";

                if (TaskBlacklist.INSTANCE.isTaskInBlacklist(groupId)) {
                    Log.record(TAG, "跳过黑名单任务[" + title + "] groupId=" + groupId);
                    continue;
                }

                if ("VISIT".equals(actionType) || "XLIGHT".equals(actionType)) {
                    int rightsTimes = task.optInt("rightsTimes", 0);
                    int rightsTimesLimit = task.optInt("rightsTimesLimit", 0);

                    JSONObject extend = task.optJSONObject("extend");
                    if (extend != null && rightsTimesLimit <= 0) {
                        String limitStr = extend.optString("rightsTimesLimit", "");
                        if (!limitStr.isEmpty()) {
                            try {
                                rightsTimesLimit = Integer.parseInt(limitStr);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    int timesToDo = (rightsTimesLimit > 0) ? (rightsTimesLimit - rightsTimes) : 1;
                    if (timesToDo <= 0) continue;

                    for (int cnt = 0; cnt < timesToDo; cnt++) {
                        JSONObject finishResponse = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
                        if (finishResponse.optBoolean("success")) {
                            Log.farm("芭芭农场广告任务📺[" + title + "] 第" + (rightsTimes + cnt + 1) + "次");
                        } else {
                            Log.record(TAG, "失败：芭芭农场广告任务📺[" + title + "] " + finishResponse.optString("desc"));
                            // 自动添加到黑名单
                            String errorCode = finishResponse.optString("code", "");
                            if (!errorCode.isEmpty()) {
                                TaskBlacklist.INSTANCE.autoAddToBlacklist(groupId, title, errorCode);
                            }
                            break;
                        }
                        GlobalThreadPools.sleep(executeIntervalInt);
                    }
                    continue;
                }

                if ("TRIGGER".equals(actionType) || "ADD_HOME".equals(actionType) || "PUSH_SUBSCRIBE".equals(actionType)) {
                    JSONObject finishResponse = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
                    if (finishResponse.optBoolean("success")) {
                        Log.farm("芭芭农场任务🧾[" + title + "]");
                    } else {
                        Log.record(TAG, "芭芭农场任务🧾[" + title + "]" + finishResponse.optString("desc"));
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doOrchardDailyTask 错误:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void orchardSign(JSONObject signTaskInfo) {
        try {
            JSONObject currentSignItem = signTaskInfo.getJSONObject("currentSignItem");
            if (!currentSignItem.getBoolean("signed")) {
                JSONObject joSign = new JSONObject(AntOrchardRpcCall.orchardSign());
                if ("100".equals(joSign.getString("resultCode"))) {
                    int awardCount = joSign.getJSONObject("signTaskInfo").getJSONObject("currentSignItem").getInt("awardCount");
                    Log.farm("芭芭农场签到📅[获得肥料]#" + awardCount + "g");
                } else {
                    Log.runtime(TAG, joSign.toString());
                }
            } else {
                Log.record(TAG, "芭芭农场今日已签到");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "orchardSign err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 执行砸金蛋操作。
     * 注意：此方法包含一个阻塞性网络调用，必须在后台线程中执行。
     *
     * @param count 要砸的金蛋数量
     */
    private void smashedGoldenEgg(int count) {
        try {
            String response = AntOrchardRpcCall.smashedGoldenEgg(count);
            JSONObject jo = new JSONObject(response);

            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray batchSmashedList = jo.getJSONArray("batchSmashedList");
                for (int i = 0; i < batchSmashedList.length(); i++) {
                    JSONObject smashedItem = batchSmashedList.getJSONObject(i);
                    int manureCount = smashedItem.optInt("manureCount", 0);
                    boolean jackpot = smashedItem.optBoolean("jackpot", false);
                    String jackpotMessage = jackpot ? "（触发大奖）" : "";
                    Log.forest(TAG, "砸出肥料 🎖️: " + manureCount + " g" + jackpotMessage);
                }
                /*
                 // 可选：输出 goldenEggInfoVO 状态
                 JSONObject goldenEggInfo = jo.optJSONObject("goldenEggInfoVO");
                 if (goldenEggInfo != null) {
                     int smashedGoldenEggs = goldenEggInfo.optInt("smashedGoldenEggs", 0);
                     int unsmashedGoldenEggs = goldenEggInfo.optInt("unsmashedGoldenEggs", 0);
                     Log.forest(TAG, "已砸蛋: " + smashedGoldenEggs + ", 剩余可砸蛋: " + unsmashedGoldenEggs);
                 }
                 */
            } else {
                Log.record(TAG, jo.optString("resultDesc", "未知错误"));
                Log.runtime(TAG, response);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "smashedGoldenEgg err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 领取已完成的每日任务奖励
     */
    private void triggerTbTask() {
        try {
            String response = AntOrchardRpcCall.orchardListTask();
            JSONObject jo = new JSONObject(response);

            if ("100".equals(jo.getString("resultCode"))) {
                JSONArray jaTaskList = jo.getJSONArray("taskList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    JSONObject jo2 = jaTaskList.getJSONObject(i);
                    if (!"FINISHED".equals(jo2.getString("taskStatus"))) {
                        continue;
                    }

                    String title = jo2.getJSONObject("taskDisplayConfig").getString("title");
                    int awardCount = jo2.optInt("awardCount", 0);
                    String taskId = jo2.getString("taskId");
                    String taskPlantType = jo2.getString("taskPlantType");

                    String triggerResponse = AntOrchardRpcCall.triggerTbTask(taskId, taskPlantType);
                    JSONObject jo3 = new JSONObject(triggerResponse);

                    if ("100".equals(jo3.getString("resultCode"))) {
                        Log.forest(TAG, "领取奖励🎖️[" + title + "]#" + awardCount + "g肥料");
                    } else {
                        Log.record(TAG, jo3.toString());
                        Log.runtime(TAG, jo3.toString());
                    }
                }
            } else {
                Log.record(TAG, jo.getString("resultDesc"));
                Log.runtime(TAG, response);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "triggerTbTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 领取小组件回访奖励
     */
    private void receiveOrchardVisitAward() {
        try {
            String response = AntOrchardRpcCall.receiveOrchardVisitAward();
            JSONObject jo = new JSONObject(response);

            if (!jo.optBoolean("success", false)) {
                Log.error(TAG, "领取回访奖励失败: " + response);
                return;
            }

            JSONArray awardList = jo.optJSONArray("orchardVisitAwardList");
            if (awardList == null || awardList.length() == 0) {
                Log.record(TAG, "领取回访奖励失败: 无奖励，可能已领取过");
                // 修复点 2：使用正确的 Status 调用
                Status.setFlagToday(StatusFlags.FLAG_ANTORCHARD_WIDGET_DAILY_AWARD);
                return;
            }

            for (int i = 0; i < awardList.length(); i++) {
                JSONObject awardObj = awardList.optJSONObject(i);
                if (awardObj == null) continue;

                int awardCount = awardObj.optInt("awardCount", 0);
                String awardDesc = awardObj.optString("awardDesc", "");

                Log.forest(TAG, "回访奖励[" + awardDesc + "] " + awardCount + " g肥料");
            }
            // 修复点 2：使用正确的 Status 调用
            Status.setFlagToday(StatusFlags.FLAG_ANTORCHARD_WIDGET_DAILY_AWARD);
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveOrchardVisitAward err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void limitedTimeChallenge() {
        try {
            String response = AntOrchardRpcCall.orchardSyncIndex("");
            JSONObject root = new JSONObject(response);

            if (!ResChecker.checkRes(TAG, root)) {
                Log.record(TAG, "orchardSyncIndex 查询失败: " + response);
                return;
            }

            JSONObject challenge = root.optJSONObject("limitedTimeChallenge");
            if (challenge == null) {
                Log.record(TAG, "错误：limitedTimeChallenge 字段不存在或为 null");
                return;
            }

            int currentRound = challenge.optInt("currentRound", 0);
            if (currentRound <= 0) {
                Log.record(TAG, "错误：currentRound 无效：" + currentRound);
                return;
            }

            JSONArray taskArray = challenge.optJSONArray("limitedTimeChallengeTasks");
            if (taskArray == null) {
                Log.record(TAG, "错误：limitedTimeChallengeTasks 字段不存在或不是数组");
                return;
            }

            int targetIdx = currentRound - 1;
            if (targetIdx < 0 || targetIdx >= taskArray.length()) {
                Log.record(TAG, "错误：当前轮数 " + currentRound + " 对应下标 " + targetIdx + " 超出数组长度: " + taskArray.length());
                return;
            }

            JSONObject roundTask = taskArray.optJSONObject(targetIdx);
            if (roundTask == null) {
                Log.record(TAG, "错误：第 " + currentRound + " 轮任务不存在");
                return;
            }

            boolean ongoing = roundTask.optBoolean("ongoing", false);
            String MtaskStatus = roundTask.optString("taskStatus");
            String MtaskId = roundTask.optString("taskId");
            int MawardCount = roundTask.optInt("awardCount", 0);

            if ("FINISHED".equals(MtaskStatus) && ongoing) {
                Log.record(TAG, "第 " + currentRound + " 轮 奖励未领取，尝试领取");
                String awardResp = AntOrchardRpcCall.receiveTaskAward("ORCHARD_LIMITED_TIME_CHALLENGE", MtaskId);
                JSONObject joo = new JSONObject(awardResp);
                if (joo.optBoolean("success")) {
                    Log.forest(TAG, "第 " + currentRound + " 轮 限时任务🎁[肥料 * " + MawardCount + "]");
                } else {
                    String desc = joo.optString("desc", "未知错误");
                    Log.record(TAG, "芭芭农场 限时任务 错误：" + desc);
                    Log.runtime(TAG, "芭芭农场 限时任务 错误：" + joo.toString());
                }
                return;
            }

            if (!"TODO".equals(roundTask.optString("taskStatus"))) {
                Log.record(TAG, "警告：第 " + currentRound + " 轮任务非 TODO，状态=" + roundTask.optString("taskStatus"));
                return;
            }

            JSONArray childTasks = roundTask.optJSONArray("childTaskList");
            if (childTasks == null) {
                Log.record(TAG, "警告：第 " + currentRound + " 轮无子任务列表");
                return;
            }

            Log.record(TAG, "开始处理第 " + currentRound + " 轮的 " + childTasks.length() + " 个子任务");

            for (int i = 0; i < childTasks.length(); i++) {
                JSONObject child = childTasks.optJSONObject(i);
                if (child == null || !"TODO".equals(child.optString("taskStatus"))) {
                    continue;
                }

                String childTaskId = child.optString("taskId", "未知ID");
                String actionType = child.optString("actionType");
                String groupId = child.optString("groupId");
                String sceneCode = child.optString("sceneCode");

                if ("GROUP_1_STEP_3_GAME_WZZT_30s".equals(groupId)) continue;

                Log.record(TAG, "------ 开始处理子任务 " + i + " | ID=" + childTaskId + " ------");

                switch (actionType) {
                    case "SPREAD_MANURE":
                        int taskRequire = child.optInt("taskRequire", 0);
                        int taskProgress = child.optInt("taskProgress", 0);
                        int need = taskRequire - taskProgress;
                        if (need > 0) {
                            Log.record(TAG, "施肥任务需补充 " + need + " 次");
                            for (int j = 0; j < need; j++) {
                                String spreadResultStr = AntOrchardRpcCall.orchardSpreadManure("", "ch_appcenter__chsub_9patch");
                                // Log.record(TAG, "施肥第 " + (j + 1) + " 次结果：" + spreadResultStr);
                                JSONObject resultJson = new JSONObject(spreadResultStr);
                                if (!"100".equals(resultJson.optString("resultCode"))) {
                                    Log.record(TAG, "芭芭农场 orchardSpreadManure 错误：" + resultJson.optString("resultDesc"));
                                    return;
                                }
                            }
                            Log.record(TAG, "施肥任务成功完成 " + need + " 次");
                        }
                        break;

                    case "GAME_CENTER":
                        String r = AntOrchardRpcCall.noticeGame("2021004165643274");
                        JSONObject jr = new JSONObject(r);
                        if (jr.optBoolean("success")) {
                            Log.record(TAG, "游戏任务触发成功 → 子任务应当自动完成");
                        } else {
                            Log.record(TAG, "游戏任务触发失败，返回: " + r);
                        }
                        break;

                    case "VISIT":
                        JSONObject displayCfg = child.optJSONObject("taskDisplayConfig");
                        if (displayCfg == null || displayCfg.optString("targetUrl", "").isEmpty()) {
                            Log.record(TAG, "任务没有 taskDisplayConfig，无法继续");
                            continue;
                        }
                        String targetUrl = displayCfg.optString("targetUrl");
                        
                        // 修复点 3：使用 getParamValue
                        String finalUrl = UrlUtil.INSTANCE.getParamValue(targetUrl, "url");
                        if (finalUrl == null) finalUrl = "";
                        Log.record(TAG, "解析到完整落地页 url = " + finalUrl);
                        
                        // 修复点 3：使用 getParamValue
                        String spaceCodeFeeds = (!finalUrl.isEmpty()) ? UrlUtil.INSTANCE.getParamValue(finalUrl, "spaceCodeFeeds") : null;
                        Log.record(TAG, "解析到 spaceCodeFeeds = " + (spaceCodeFeeds != null ? spaceCodeFeeds : "null"));
                        
                        String finalSpaceCode = spaceCodeFeeds;
                        if (finalSpaceCode == null) {
                             // 修复点 3：使用 getParamValue
                             finalSpaceCode = UrlUtil.INSTANCE.getParamValue(targetUrl, "spaceCodeFeeds");
                        }
                        if (finalSpaceCode == null || finalSpaceCode.isEmpty()) {
                            Log.record(TAG, "spaceCodeFeeds 解析失败，跳过此任务");
                            continue;
                        }
                        
                        String xlightResponse = XLightRpcCall.INSTANCE.xlightPlugin(finalUrl, "ch_url-https://render.alipay.com/p/yuyan/180020010001263018/game.html", "u_41ba1_2f33e", finalSpaceCode);
                        
                        JSONObject xlightJo = new JSONObject(xlightResponse);
                        Log.record(TAG, "广告任务触发成功 → 即将调用 finishTask() 完成任务");

                        JSONObject playingResult = xlightJo.optJSONObject("resData") != null ? xlightJo.optJSONObject("resData").optJSONObject("playingResult") : xlightJo.optJSONObject("playingResult");

                        if (playingResult == null) {
                            Log.record(TAG, "playingResult 为空，无法 finishTask");
                            continue;
                        }
                        String playingBizId = playingResult.optString("playingBizId", "");
                        if (playingBizId.isEmpty()) {
                             Log.record(TAG, "playingBizId 为空，无法 finishTask");
                            continue;
                        }

                        JSONObject eventRewardDetail = playingResult.optJSONObject("eventRewardDetail");
                        JSONArray infoListArray = (eventRewardDetail != null) ? eventRewardDetail.optJSONArray("eventRewardInfoList") : null;

                        if (infoListArray == null || infoListArray.length() == 0) {
                            Log.record(TAG, "eventRewardInfoList 为空，无法 finishTask");
                            continue;
                        }
                        JSONObject playEventInfo = infoListArray.getJSONObject(0);
                        
                        String finishResultStr = XLightRpcCall.INSTANCE.finishTask(playingBizId, playEventInfo, sceneCode, groupId);
                        JSONObject fr = new JSONObject(finishResultStr);

                        if (fr.optBoolean("success")) {
                            Log.record(TAG, "finishTask 完成成功 → 浏览广告任务完成");
                        } else {
                             Log.record(TAG, "finishTask 完成失败: " + finishResultStr);
                        }
                        break;
                        
                    default:
                        Log.record(TAG, "无法处理的任务类型：" + childTaskId + " | actionType=" + actionType);
                        break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "limitedTimeChallenge err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void querySubplotsActivity(int taskRequire) {
        try {
            String s = AntOrchardRpcCall.querySubplotsActivity(treeLevel);
            JSONObject jo = new JSONObject(s);
            if ("100".equals(jo.getString("resultCode"))) {
                JSONArray subplotsActivityList = jo.getJSONArray("subplotsActivityList");
                for (int i = 0; i < subplotsActivityList.length(); i++) {
                    JSONObject jo2 = subplotsActivityList.getJSONObject(i);
                    if (!"WISH".equals(jo2.getString("activityType"))) continue;
                    
                    String activityId = jo2.getString("activityId");
                    String status = jo2.getString("status");
                    
                    if ("NOT_STARTED".equals(status)) {
                        String extend = jo2.getString("extend");
                        JSONObject jo3 = new JSONObject(extend);
                        JSONArray wishActivityOptionList = jo3.getJSONArray("wishActivityOptionList");
                        String optionKey = null;
                        for (int j = 0; j < wishActivityOptionList.length(); j++) {
                            JSONObject jo4 = wishActivityOptionList.getJSONObject(j);
                            if (taskRequire == jo4.getInt("taskRequire")) {
                                optionKey = jo4.getString("optionKey");
                                break;
                            }
                        }
                        if (optionKey != null) {
                            JSONObject jo5 = new JSONObject(AntOrchardRpcCall.triggerSubplotsActivity(activityId, "WISH", optionKey));
                            if ("100".equals(jo5.getString("resultCode"))) {
                                Log.farm("芭芭农场许愿✨[每日施肥" + taskRequire + "次]");
                            } else {
                                Log.record(TAG, jo5.getString("resultDesc"));
                            }
                        }
                    } else if ("FINISHED".equals(status)) {
                        JSONObject jo3 = new JSONObject(AntOrchardRpcCall.receiveOrchardRights(activityId, "WISH"));
                        if ("100".equals(jo3.getString("resultCode"))) {
                            Log.farm("许愿奖励✨[肥料" + jo3.getInt("amount") + "g]");
                            querySubplotsActivity(taskRequire);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
        }
    }

    private void orchardassistFriend() {
        try {
            if (!Status.canAntOrchardAssistFriendToday()) {
                Log.record(TAG, "今日已助力，跳过芭芭农场助力");
                return;
            }
            Set<String> friendSet = assistFriendList.getValue();
            for (String uid : friendSet) {
                String shareId = Base64.encodeToString((uid + "-" + RandomUtil.getRandomInt(5) + "ANTFARM_ORCHARD_SHARE_P2P").getBytes(), Base64.NO_WRAP);
                String str = AntOrchardRpcCall.achieveBeShareP2P(shareId);
                JSONObject jsonObject = new JSONObject(str);
                GlobalThreadPools.sleep(800);
                String name = UserMap.getMaskName(uid);
                
                if (!jsonObject.optBoolean("success")) {
                    String code = jsonObject.optString("code");
                    if ("600000027".equals(code)) {
                        Log.record(TAG, "芭芭农场助力💪今日助力他人次数上限");
                        Status.antOrchardAssistFriendToday();
                        return;
                    }
                    Log.record(TAG, "芭芭农场助力😔失败[" + name + "]" + jsonObject.optString("desc"));
                    continue;
                }
                Log.farm("芭芭农场助力💪[助力:" + name + "]");
            }
            Status.antOrchardAssistFriendToday();
        } catch (Throwable t) {
            Log.runtime(TAG, "orchardassistFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private String createAnimalInfoJson(String animalUserId, int earnManureCount, String groupId, String orchardUserId) {
        return "{\"animalUserId\":\"" + animalUserId + "\",\"earnManureCount\":" + earnManureCount + ",\"groupId\":\"" + groupId + "\",\"orchardUserId\":\"" + orchardUserId + "\"}";
    }

    private void batchHireAnimalRecommend() {
        try {
            JSONObject jo = new JSONObject(AntOrchardRpcCall.batchHireAnimalRecommend(UserMap.getCurrentUid()));
            if ("100".equals(jo.getString("resultCode"))) {
                JSONArray recommendGroupList = jo.optJSONArray("recommendGroupList");
                if (recommendGroupList != null && recommendGroupList.length() > 0) {
                    List<String> groupList = new ArrayList<>();
                    for (int i = 0; i < recommendGroupList.length(); i++) {
                        JSONObject item = recommendGroupList.getJSONObject(i);
                        String animalUserId = item.getString("animalUserId");
                        if (dontHireList.getValue().contains(animalUserId)) continue;
                        
                        int earnManureCount = item.getInt("earnManureCount");
                        String groupId = item.getString("groupId");
                        String orchardUserId = item.getString("orchardUserId");
                        if (dontWeedingList.getValue().contains(orchardUserId)) continue;
                        
                        groupList.add(createAnimalInfoJson(animalUserId, earnManureCount, groupId, orchardUserId));
                    }
                    if (!groupList.isEmpty()) {
                        jo = new JSONObject(AntOrchardRpcCall.batchHireAnimal(groupList));
                        if ("100".equals(jo.getString("resultCode"))) {
                            Log.farm("一键捉鸡🐣[除草]");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "batchHireAnimalRecommend err:");
            Log.printStackTrace(TAG, t);
        }
    }
}