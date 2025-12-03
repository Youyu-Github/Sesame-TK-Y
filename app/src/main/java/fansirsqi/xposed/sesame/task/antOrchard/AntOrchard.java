package fansirsqi.xposed.sesame.task.antOrchard;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.data.Status;

public class AntOrchard extends ModelTask {
    private static final String TAG = AntOrchard.class.getSimpleName();

    private String userId;
    private String treeLevel;
    private String[] wuaList;
    private Integer executeIntervalInt;
    private IntegerModelField executeInterval;
    private BooleanModelField receiveOrchardTaskAward;
    private IntegerModelField orchardSpreadManureCount;
    private BooleanModelField batchHireAnimal;
    private SelectModelField dontHireList;
    private SelectModelField dontWeedingList;
    private SelectModelField assistFriendList;

    @Override
    public String getName() {
        return "农场";
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
        modelFields.addField(receiveOrchardTaskAward = new BooleanModelField("receiveOrchardTaskAward", "收取农场任务奖励", false));
        modelFields.addField(orchardSpreadManureCount = new IntegerModelField("orchardSpreadManureCount", "农场每日施肥次数", 0));
        modelFields.addField(assistFriendList = new SelectModelField("assistFriendList", "助力好友列表", new LinkedHashSet<>(), AlipayUser::getList));
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
        } else {
            return true;
        }
    }

    @Override
    public void run() {
        try {
            Log.record(TAG, "执行开始-" + getName());
            executeIntervalInt = Math.max(executeInterval.getValue(), 500);
            String s = AntOrchardRpcCall.orchardIndex();
            JSONObject jo = new JSONObject(s);
            if ("100".equals(jo.getString("resultCode"))) {
                if (jo.optBoolean("userOpenOrchard")) {
                    JSONObject taobaoData = new JSONObject(jo.getString("taobaoData"));
                    treeLevel = Integer.toString(taobaoData.getJSONObject("gameInfo").getJSONObject("plantInfo").getJSONObject("seedStage").getInt("stageLevel"));
                    
                    userId = UserMap.getCurrentUid();
                    if (userId == null || userId.isEmpty()) {
                        Log.record(TAG, "无法获取当前用户ID，停止执行");
                        return;
                    }

                    if (jo.has("lotteryPlusInfo")) drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"));
                    extraInfoGet();

                    if (receiveOrchardTaskAward.getValue()) {
                        doOrchardDailyTask(userId);
                        triggerTbTask();
                    }

                    Integer orchardSpreadManureCountValue = orchardSpreadManureCount.getValue();
                    if (orchardSpreadManureCountValue > 0 && Status.canSpreadManureToday(userId)) {
                        orchardSpreadManure();
                    }
                    
                    if (orchardSpreadManureCountValue >= 3 && orchardSpreadManureCountValue < 10) {
                        querySubplotsActivity(3);
                    } else if (orchardSpreadManureCountValue >= 10) {
                        querySubplotsActivity(10);
                    }
                    
                    orchardassistFriend();

                    if (batchHireAnimal.getValue()) {
                        try {
                            // JSONObject joo = new JSONObject(AntOrchardRpcCall.mowGrassInfo());
                            JSONObject joo = new JSONObject(AntOrchardRpcCall.friendList());
                            if ("100".equals(joo.getString("resultCode"))) {
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
                    Log.other("请先开启芭芭农场！");
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    // [新增] 硬编码的黑名单关键字列表
    // 如果需要添加新的屏蔽任务，请在这里添加关键字
    private static final String[] BLACK_LIST_KEYWORDS = {
        "开心消消乐过1关新关卡", 
        "逛一逛快手", 
        "添加农场小组件并访问", 
        "逛助农好货得肥料", 
        "体验番茄小说"
    };

    // [修改] 检查任务标题是否在硬编码的黑名单中
    private boolean isTaskBlacklisted(String title) {
        if (title == null || title.isEmpty()) return false;
        for (String keyword : BLACK_LIST_KEYWORDS) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String getWua() {
        if (wuaList == null) {
            try {
                String content = Files.readFromFile(Files.getWuaFile());
                wuaList = content.split("\n");
            } catch (Throwable ignored) {
                wuaList = new String[0];
            }
        }
        if (wuaList.length > 0) {
            return wuaList[RandomUtil.nextInt(0, wuaList.length - 1)];
        }
        Log.record("get wuaList return null");
        return "null";
    }

    private boolean canSpreadManureContinue(int stageBefore, int stageAfter) {
        if (stageAfter - stageBefore > 1) {
            return true;
        }
        Log.record(TAG, "施肥只加0.01%进度今日停止施肥！");
        return false;
    }

    private void orchardSpreadManure() {
        try {
            do {
                try {
                    JSONObject jo = new JSONObject(AntOrchardRpcCall.orchardIndex());
                    if (!"100".equals(jo.getString("resultCode"))) {
                        Log.runtime(TAG, jo.getString("resultDesc"));
                        return;
                    }
                    if (jo.has("spreadManureActivity")) {
                        JSONObject spreadManureStage = jo.getJSONObject("spreadManureActivity").getJSONObject("spreadManureStage");
                        if ("FINISHED".equals(spreadManureStage.getString("status"))) {
                            String sceneCode = spreadManureStage.getString("sceneCode");
                            String taskType = spreadManureStage.getString("taskType");
                            int awardCount = spreadManureStage.getInt("awardCount");
                            JSONObject joo = new JSONObject(AntOrchardRpcCall.receiveTaskAward(sceneCode, taskType));
                            if (joo.optBoolean("success")) {
                                Log.farm("丰收礼包🎁[肥料*" + awardCount + "]");
                            } else {
                                Log.record(joo.getString("desc"));
                                Log.runtime(joo.toString());
                            }
                        }
                    }
                    String taobaoData = jo.getString("taobaoData");
                    jo = new JSONObject(taobaoData);
                    JSONObject plantInfo = jo.getJSONObject("gameInfo").getJSONObject("plantInfo");
                    boolean canExchange = plantInfo.getBoolean("canExchange");
                    if (canExchange) {
                        Log.farm("🎉 农场果树似乎可以兑换了！");
                        return;
                    }
                    JSONObject seedStage = plantInfo.getJSONObject("seedStage");
                    treeLevel = Integer.toString(seedStage.getInt("stageLevel"));
                    JSONObject accountInfo = jo.getJSONObject("gameInfo").getJSONObject("accountInfo");
                    int happyPoint = Integer.parseInt(accountInfo.getString("happyPoint"));
                    int wateringCost = accountInfo.getInt("wateringCost");
                    int wateringLeftTimes = accountInfo.getInt("wateringLeftTimes");
                    if (happyPoint > wateringCost && wateringLeftTimes > 0 && (200 - wateringLeftTimes < orchardSpreadManureCount.getValue())) {
                        String wua_content = getWua();
                        if (wua_content.equals("null") || wua_content.equals("")) {
                            Log.record("get wua is null or empty:" + wua_content);
                        }
                        jo = new JSONObject(AntOrchardRpcCall.orchardSpreadManure(wua_content));
                        if (!"100".equals(jo.getString("resultCode"))) {
                            Log.record(jo.getString("resultDesc"));
                            Log.runtime(jo.toString());
                            return;
                        }
                        taobaoData = jo.getString("taobaoData");
                        jo = new JSONObject(taobaoData);
                        String stageText = jo.getJSONObject("currentStage").getString("stageText");
                        Log.farm("农场施肥💩[" + stageText + "]");
                        if (!canSpreadManureContinue(seedStage.getInt("totalValue"), jo.getJSONObject("currentStage").getInt("totalValue"))) {
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
            Log.runtime(TAG, "orchardSpreadManure err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void extraInfoGet() {
        try {
            String s = AntOrchardRpcCall.extraInfoGet();
            JSONObject jo = new JSONObject(s);
            if ("100".equals(jo.getString("resultCode"))) {
                JSONObject fertilizerPacket = jo.getJSONObject("data").getJSONObject("extraData").getJSONObject("fertilizerPacket");
                if (!"todayFertilizerWaitTake".equals(fertilizerPacket.getString("status"))) return;
                int todayFertilizerNum = fertilizerPacket.getInt("todayFertilizerNum");
                jo = new JSONObject(AntOrchardRpcCall.extraInfoSet());
                if ("100".equals(jo.getString("resultCode"))) {
                    Log.farm("每日肥料💩[" + todayFertilizerNum + "g]");
                } else {
                    Log.runtime(jo.getString("resultDesc"), jo.toString());
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
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
                jo = ja.getJSONObject(i);
                if (jo.getString("itemId").equals(itemId)) {
                    if (!jo.getBoolean("received")) {
                        jo = new JSONObject(AntOrchardRpcCall.drawLottery());
                        if ("100".equals(jo.getString("resultCode"))) {
                            JSONArray userEverydayGiftItems = jo.getJSONObject("lotteryPlusInfo").getJSONObject("userSevenDaysGiftsItem").getJSONArray("userEverydayGiftItems");
                            for (int j = 0; j < userEverydayGiftItems.length(); j++) {
                                jo = userEverydayGiftItems.getJSONObject(j);
                                if (jo.getString("itemId").equals(itemId)) {
                                    int awardCount = jo.optInt("awardCount", 1);
                                    Log.farm("七日礼包🎁[获得肥料]#" + awardCount + "g");
                                    break;
                                }
                            }
                        } else {
                            Log.runtime(jo.getString("resultDesc"), jo.toString());
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
            if ("100".equals(jo.getString("resultCode"))) {
                if (jo.has("signTaskInfo")) {
                    JSONObject signTaskInfo = jo.getJSONObject("signTaskInfo");
                    orchardSign(signTaskInfo);
                }

                if (jo.has("convertToManureTask")) {
                    JSONObject convertTask = jo.getJSONObject("convertToManureTask");
                    if ("TODO".equals(convertTask.getString("taskStatus"))) {
                        String sLeaf = AntOrchardRpcCall.triggerSubplotsActivity("antorchard_defoliation", "DEFOLIATION", null);
                        JSONObject joLeaf = new JSONObject(sLeaf);
                        if ("100".equals(joLeaf.getString("resultCode"))) {
                            Log.farm("农场任务🍂[领取落叶肥料]");
                        }
                    }
                }

                JSONArray jaTaskList = jo.getJSONArray("taskList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    JSONObject taskItem = jaTaskList.getJSONObject(i);
                    if (!"TODO".equals(taskItem.optString("taskStatus"))) continue;

                    // 1. 先安全获取标题
                    JSONObject displayConfig = taskItem.optJSONObject("taskDisplayConfig");
                    String title = (displayConfig != null) ? displayConfig.optString("title", "未知任务") : "未知任务";

                    // 2. 检查黑名单，如果标题包含黑名单关键字则跳过
                    if (isTaskBlacklisted(title)) {
                        Log.record(TAG, "🚫 跳过黑名单任务: " + title);
                        continue;
                    }

                    String actionType = taskItem.optString("actionType");
                    if ("TRIGGER".equals(actionType) || "ADD_HOME".equals(actionType) || "PUSH_SUBSCRIBE".equals(actionType) || "VISIT".equals(actionType)) {
                        String taskId = taskItem.getString("taskId");
                        String sceneCode = taskItem.getString("sceneCode");
                        
                        String responseStr = AntOrchardRpcCall.finishTask(userId, sceneCode, taskId);
                        JSONObject rpcResponse = new JSONObject(responseStr);
                        
                        if (rpcResponse.optBoolean("success")) {
                            Log.farm("完成农场任务🧾[" + title + "]");
                        } else {
                            Log.record(TAG, "农场任务[" + title + "]失败: " + rpcResponse.optString("desc") + " (Code: " + rpcResponse.optString("code") + ")");
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultCode"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doOrchardDailyTask err:");
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
                    Log.farm("农场签到📅[获得肥料]#" + awardCount + "g");
                } else {
                    Log.runtime(joSign.getString("resultDesc"), joSign.toString());
                }
            } else {
                Log.record(TAG, "农场今日已签到");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "orchardSign err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private static void triggerTbTask() {
        try {
            String s = AntOrchardRpcCall.orchardListTask();
            JSONObject jo = new JSONObject(s);
            if ("100".equals(jo.getString("resultCode"))) {
                JSONArray jaTaskList = jo.getJSONArray("taskList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    jo = jaTaskList.getJSONObject(i);
                    if (!"FINISHED".equals(jo.getString("taskStatus"))) continue;
                    
                    JSONObject displayConfig = jo.optJSONObject("taskDisplayConfig");
                    String title = (displayConfig != null) ? displayConfig.optString("title", "未知任务") : "未知任务";
                    
                    int awardCount = jo.optInt("awardCount", 0);
                    String taskId = jo.getString("taskId");
                    String taskPlantType = jo.getString("taskPlantType");
                    
                    jo = new JSONObject(AntOrchardRpcCall.triggerTbTask(taskId, taskPlantType));
                    if ("100".equals(jo.getString("resultCode"))) {
                        Log.farm("领取奖励🎖️[" + title + "]#" + awardCount + "g肥料");
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "triggerTbTask err:");
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
                    jo = subplotsActivityList.getJSONObject(i);
                    if (!"WISH".equals(jo.getString("activityType"))) continue;
                    String activityId = jo.getString("activityId");
                    if ("NOT_STARTED".equals(jo.getString("status"))) {
                        String extend = jo.getString("extend");
                        jo = new JSONObject(extend);
                        JSONArray wishActivityOptionList = jo.getJSONArray("wishActivityOptionList");
                        String optionKey = null;
                        for (int j = 0; j < wishActivityOptionList.length(); j++) {
                            jo = wishActivityOptionList.getJSONObject(j);
                            if (taskRequire == jo.getInt("taskRequire")) {
                                optionKey = jo.getString("optionKey");
                                break;
                            }
                        }
                        if (optionKey != null) {
                            jo = new JSONObject(AntOrchardRpcCall.triggerSubplotsActivity(activityId, "WISH", optionKey));
                            if ("100".equals(jo.getString("resultCode"))) {
                                Log.farm("农场许愿✨[每日施肥" + taskRequire + "次]");
                            } else {
                                Log.record(jo.getString("resultDesc"));
                                Log.runtime(jo.toString());
                            }
                        }
                    } else if ("FINISHED".equals(jo.getString("status"))) {
                        jo = new JSONObject(AntOrchardRpcCall.receiveOrchardRights(activityId, "WISH"));
                        if ("100".equals(jo.getString("resultCode"))) {
                            Log.farm("许愿奖励✨[肥料" + jo.getInt("amount") + "g]");
                            querySubplotsActivity(taskRequire);
                            return;
                        } else {
                            Log.record(jo.getString("resultDesc"));
                            Log.runtime(jo.toString());
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "triggerTbTask err:");
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
                    List<String> GroupList = new ArrayList<>();
                    for (int i = 0; i < recommendGroupList.length(); i++) {
                        jo = recommendGroupList.getJSONObject(i);
                        String animalUserId = jo.getString("animalUserId");
                        if (dontHireList.getValue().contains(animalUserId))
                            continue;
                        int earnManureCount = jo.getInt("earnManureCount");
                        String groupId = jo.getString("groupId");
                        String orchardUserId = jo.getString("orchardUserId");
                        if (dontWeedingList.getValue().contains(orchardUserId)) {
                            continue;
                        }
                        GroupList.add(createAnimalInfoJson(animalUserId, earnManureCount, groupId, orchardUserId));
                    }
                    if (!GroupList.isEmpty()) {
                        jo = new JSONObject(AntOrchardRpcCall.batchHireAnimal(GroupList));
                        if ("100".equals(jo.getString("resultCode"))) {
                            Log.farm("一键捉鸡🐣[除草]");
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "batchHireAnimalRecommend err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void orchardassistFriend() {
        try {
            if (!Status.canAntOrchardAssistFriendToday()) {
                return;
            }
            Set<String> friendSet = assistFriendList.getValue();
            for (String uid : friendSet) {
                String shareId = Base64.encodeToString((uid + "-" + RandomUtil.getRandomInt(5) + "ANTFARM_ORCHARD_SHARE_P2P").getBytes(), Base64.NO_WRAP);
                String str = AntOrchardRpcCall.achieveBeShareP2P(shareId);
                JSONObject jsonObject = new JSONObject(str);
                GlobalThreadPools.sleep(2000);
                String name = UserMap.getMaskName(uid);
                if (!jsonObject.optBoolean("success")) {
                    String code = jsonObject.getString("code");
                    if ("600000027".equals(code)) {
                        Log.record(TAG, "农场助力💪今日助力他人次数上限");
                        Status.antOrchardAssistFriendToday();
                        return;
                    }
                    Log.record(TAG, "农场助力😔失败[" + name + "]" + jsonObject.optString("desc"));
                    continue;
                }
                Log.farm("农场助力💪[助力:" + name + "]");
            }
            Status.antOrchardAssistFriendToday();
        } catch (Throwable t) {
            Log.runtime(TAG, "orchardassistFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
}