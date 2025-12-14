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

import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.adexchange.UrlUtil;
import fansirsqi.xposed.sesame.task.adexchange.XLightRpcCall;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;

public class AntOrchard extends ModelTask {
    private static final String TAG = AntOrchard.class.getSimpleName();

    // 任务黑名单：某些广告/外跳类任务后端不支持 finishTask 或需要前端行为配合
    private static final Set<String> ORCHARD_TASK_BLACKLIST = new HashSet<>();
    static {
        ORCHARD_TASK_BLACKLIST.add("70000");                        // 逛好物最高得1500肥料（XLIGHT）
        ORCHARD_TASK_BLACKLIST.add("ORCHARD_NORMAL_KUAISHOU_MAX");  // 逛一逛快手
        ORCHARD_TASK_BLACKLIST.add("ORCHARD_NORMAL_DIAOYU1");       // 钓鱼1次
        ORCHARD_TASK_BLACKLIST.add("ZHUFANG3IN1");                  // 添加农场小组件并访问
        ORCHARD_TASK_BLACKLIST.add("12172");                        // 逛助农好货得肥料
        ORCHARD_TASK_BLACKLIST.add("TOUTIAO");                      // 逛一逛今日头条
    }

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
        modelFields.addField(orchardSpreadManure = new BooleanModelField("orchardSpreadManure", "果树施肥", false));
        modelFields.addField(orchardSpreadManureCount = new IntegerModelField("orchardSpreadManureCount", "农场每日施肥次数", 0));
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

                //如果有🥚 则进行砸🥚
                JSONObject goldenEggInfo = jo.optJSONObject("goldenEggInfo"); 

                // 确保 goldenEggInfo 对象不是 null
                if (goldenEggInfo != null) {
                    int unsmashedGoldenEggs = goldenEggInfo.optInt("unsmashedGoldenEggs");

                    // 如果有未砸的金蛋，则执行砸蛋逻辑
                    if (unsmashedGoldenEggs > 0) {
                        smashedGoldenEgg(unsmashedGoldenEggs);
                    }
                }

                // 农场任务
                if (receiveOrchardTaskAward.getValue()) {
                    doOrchardDailyTask(userId);
                    triggerTbTask();
                }

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
            Log.printStackTrace(TAG, "农场主流程执行异常！", t);
        } finally {
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    /**
     * 获取 Wua，同步Kotlin逻辑：文件 -> null (Detector已移除)
     */
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
        // Detector.genWua() 已移除，返回 null
        return null;
    }

    private boolean canSpreadManureContinue(int stageBefore, int stageAfter) {
        if (stageAfter - stageBefore > 1) {
            return true;
        } else {
            Log.record(TAG, "施肥只加0.01%进度今日停止施肥！");
            return false;
        }
    }

    // 领取 reward 丰收礼包
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
                        Log.farm("农场丰收礼包🎁[返肥料奖励*" + awardCount + "]g");
                    } else {
                        Log.record(TAG, "农场 丰收礼包 错误：" + joo.optString("desc"));
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, "gotHarvest error", e);
        }
    }

    // 检查是否可以兑换
    private JSONObject checkCanExchange(JSONObject orchardIndexTaobaoData) throws Exception {
        JSONObject plantInfo = orchardIndexTaobaoData.getJSONObject("gameInfo").getJSONObject("plantInfo");
        boolean canExchange = plantInfo.getBoolean("canExchange");
        if (canExchange) {
            Log.farm("🎉 农场果树似乎可以兑换了！");
            // Notify.sendNewNotification("发生什么事了？", "芝麻粒TK提醒您：\n 🎉 农场果树似乎可以兑换了！");
        }
        return plantInfo;
    }

    private void orchardSpreadManureLogic() {
        try {
            // 创建一个不可变的列表
            List<String> sourceList = List.of(
                "DNHZ_NC_zhimajingnangSF",
                "widget_shoufei",
                "ch_appcenter__chsub_9patch"
            );
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
                    
                    // 如果已可兑换，checkCanExchange 会发通知，此处应检查是否继续
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
                        Log.runtime(TAG, "农场肥料不足以施肥 " + wateringCost);
                        return;
                    }
                    if (wateringLeftTimes == 0) {
                        Log.runtime(TAG, "剩余施肥次数为 0");
                        return;
                    }

                    if ((200 - wateringLeftTimes) < orchardSpreadManureCount.getValue()) {
                        String wua = getWua();
                        if (wua != null) {
                            Log.runtime(TAG, "set Wua " + wua);
                        }
                        
                        // 随机选一个来源，确保完成其他任务 例如芝麻信誉的跳转，小组件的跳转
                        String randomSource = sourceList.get(new Random().nextInt(sourceList.size()));

                        JSONObject spreadManureData = new JSONObject(AntOrchardRpcCall.orchardSpreadManure(wua, randomSource));
                        
                        if (!"100".equals(spreadManureData.getString("resultCode"))) {
                            Log.record(TAG, "农场 orchardSpreadManure 错误：" + spreadManureData.getString("resultDesc"));
                            return;
                        }

                        JSONObject spreadTaobaoData = new JSONObject(spreadManureData.getString("taobaoData"));
                        String stageText = spreadTaobaoData.getJSONObject("currentStage").getString("stageText");
                        int dailyAppWateringCount = spreadTaobaoData.getJSONObject("statistics").getInt("dailyAppWateringCount");
                        
                        Log.farm("今日农场已施肥💩 " + dailyAppWateringCount + " 次 [" + stageText + "]");
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
            Log.printStackTrace(TAG, "农场施肥异常！", t);
        }
    }

    private void extraInfoGet() {
        try {
            JSONObject jo = new JSONObject(AntOrchardRpcCall.extraInfoGet());
            if ("100".equals(jo.getString("resultCode"))) {
                JSONObject fertilizerPacket = jo.getJSONObject("data")
                        .getJSONObject("extraData").getJSONObject("fertilizerPacket");
                
                // Kotlin 逻辑：只在状态为 todayFertilizerWaitTake 时尝试领取
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
            Log.printStackTrace(TAG, "extraInfoGet err:", t);
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
                            JSONArray userEverydayGiftItems = jo3.getJSONObject("lotteryPlusInfo")
                                    .getJSONObject("userSevenDaysGiftsItem").getJSONArray("userEverydayGiftItems");
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

    /**
     * 执行日常任务
     */
    private void doOrchardDailyTask(String userId) {
        try {
            String s = AntOrchardRpcCall.orchardListTask();
            JSONObject jo = new JSONObject(s);
            if (!"100".equals(jo.optString("resultCode"))) {
                Log.record("doOrchardDailyTask响应异常", s);
                return;
            }

            boolean inTeam = jo.optBoolean("inTeam", false);
            Log.record(TAG, inTeam ? "当前为农场 team 模式（合种/帮帮种已开启）" : "当前为普通单人农场模式");

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

                // 黑名单检查
                if (ORCHARD_TASK_BLACKLIST.contains(groupId)) {
                    Log.record(TAG, "跳过黑名单任务[" + title + "] groupId=" + groupId);
                    continue;
                }

                // 广告类任务：VISIT / XLIGHT (浏览/逛好物)
                if ("VISIT".equals(actionType) || "XLIGHT".equals(actionType)) {
                    int rightsTimes = task.optInt("rightsTimes", 0);
                    int rightsTimesLimit = task.optInt("rightsTimesLimit", 0);

                    // 解析 extend 中的限制次数（字符串格式）
                    JSONObject extend = task.optJSONObject("extend");
                    if (extend != null && rightsTimesLimit <= 0) {
                        String limitStr = extend.optString("rightsTimesLimit", "");
                        if (!limitStr.isEmpty()) {
                            try {
                                rightsTimesLimit = Integer.parseInt(limitStr);
                            } catch (Exception ignored) {}
                        }
                    }

                    int timesToDo = (rightsTimesLimit > 0) ? (rightsTimesLimit - rightsTimes) : 1;
                    if (timesToDo <= 0) continue;

                    for (int cnt = 0; cnt < timesToDo; cnt++) {
                        JSONObject finishResponse = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
                        if (finishResponse.optBoolean("success")) {
                            Log.farm("农场广告任务📺[" + title + "] 第" + (rightsTimes + cnt + 1) + "次");
                        } else {
                            Log.record(TAG, "失败：农场广告任务📺[" + title + "] " + finishResponse.optString("desc"));
                            break;
                        }
                        GlobalThreadPools.sleep(executeIntervalInt);
                    }
                    continue;
                }

                // 普通任务
                if ("TRIGGER".equals(actionType) || "ADD_HOME".equals(actionType) || "PUSH_SUBSCRIBE".equals(actionType)) {
                    JSONObject finishResponse = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
                    if (finishResponse.optBoolean("success")) {
                        Log.farm("农场任务🧾[" + title + "]");
                    } else {
                        Log.record(TAG, "农场任务🧾[" + title + "]" + finishResponse.optString("desc"));
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
                    Log.farm("农场签到📅[获得肥料]#" + awardCount + "g");
                } else {
                    Log.runtime(TAG, joSign.toString());
                }
            } else {
                Log.record(TAG, "农场今日已签到");
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
            // 发起网络请求，这是一个阻塞操作
            String response = AntOrchardRpcCall.smashedGoldenEgg(count);
            JSONObject jo = new JSONObject(response);

            if (ResChecker.checkRes(TAG, jo)) {
                // 解析 batchSmashedList
                JSONArray batchSmashedList = jo.getJSONArray("batchSmashedList");
                for (int i = 0; i < batchSmashedList.length(); i++) {
                    JSONObject smashedItem = batchSmashedList.getJSONObject(i);
                    int manureCount = smashedItem.optInt("manureCount", 0);
                    boolean jackpot = smashedItem.optBoolean("jackpot", false);

                    // 输出信息
                    // 使用三元运算符来模拟 Kotlin 的 if-else 表达式
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

        } catch (JSONException e) {
            // 专门处理 JSON 解析异常，这是一种良好实践
            Log.runtime(TAG, "smashedGoldenEgg JSON parsing error:");
            Log.printStackTrace(TAG, e);
        } catch (Throwable t) {
            // 捕获所有其他可能的异常，如网络问题等
            Log.runtime(TAG, "smashedGoldenEgg err:");
            Log.printStackTrace(TAG, t);
        }
    }

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

                    // --- 从这里开始是补全和修正的代码 ---

                    String title = jo2.getJSONObject("taskDisplayConfig").getString("title");
                    String actionType = jo2.getString("actionType");
                    String taskId = jo2.getString("taskId");

                    // 判断任务类型，如果是 XLIGHT，走特殊的浏览广告逻辑
                    if ("XLIGHT".equals(actionType)) {
                        // 解析 targetUrl 获取 spaceCodeFeeds 和 pageurl
                        String targetUrl = jo2.getJSONObject("taskDisplayConfig").getString("targetUrl");
                        String spaceCodeFeeds = UrlUtil.INSTANCE.getParam(targetUrl, "spaceCodeFeeds");
                        String pageurl = UrlUtil.INSTANCE.getParam(targetUrl, "urlu");

                        // 如果关键参数缺失，则跳过此任务
                        if (spaceCodeFeeds == null || pageurl == null) {
                            continue;
                        }

                        // 调用广告插件
                        String xlightResponse = XLightRpcCall.INSTANCE.xlightPlugin("", pageurl, "ch_url-https://render.alipay.com/p/yuyan/180020010001263018/game.html", spaceCodeFeeds);
                        JSONObject xlightJo = new JSONObject(xlightResponse);
                        
                        JSONObject playingResult = xlightJo.getJSONObject("resData").getJSONObject("playingResult");
                        String playingBizId = playingResult.getString("playingBizId");
                        JSONArray rewardList = playingResult.getJSONObject("eventRewardDetail").getJSONArray("eventRewardInfoList");

                        // 遍历每个事件，单独提交完成
                        for (int j = 0; j < rewardList.length(); j++) {
                            JSONObject reward = rewardList.getJSONObject(j);
                            // 直接将整个事件对象传递
                            JSONObject playEventInfo = reward; 
                            
                            String finishResponse = XLightRpcCall.INSTANCE.finishTask(playingBizId, playEventInfo);
                            JSONObject finishJo = new JSONObject(finishResponse);

                            if ("100".equals(finishJo.getString("resultCode"))) {
                                JSONObject rewardRenderInfo = reward.getJSONObject("rewardRenderInfo");
                                int rewardNumber = rewardRenderInfo.getInt("rewardDisplayAmount");
                                String rewardText = rewardRenderInfo.getString("rewardDisplayText");

                                Log.forest(TAG, "领取奖励🎖️[" + title + "]#" + rewardNumber + rewardText);
                            } else {
                                Log.record(TAG, finishJo.toString());
                                Log.runtime(TAG, finishJo.toString());
                            }
                        }

                    } else {
                        // 普通任务逻辑
                        int awardCount = jo2.optInt("awardCount", 0);
                        String taskPlantType = jo2.getString("taskPlantType");
                        
                        String triggerResponse = AntOrchardRpcCall.triggerTbTask(taskId, taskPlantType);
                        JSONObject jo3 = new JSONObject(triggerResponse);
                        
                        if ("100".equals(jo3.getString("resultCode"))) {
                            // 注意：这里日志的类名是 Log.forest 而不是 Log.farm
                            Log.forest(TAG, "领取奖励🎖️[" + title + "]#" + awardCount + "g肥料");
                        } else {
                            Log.record(TAG, jo3.toString());
                            Log.runtime(TAG, jo3.toString());
                        }
                    }
                }
            } else {
                // 获取任务列表失败的日志记录
                Log.record(TAG, jo.getString("resultDesc"));
                Log.runtime(TAG, response);
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
                                Log.farm("农场许愿✨[每日施肥" + taskRequire + "次]");
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

    // 助力
    private void orchardassistFriend() {
        try {
            if (!Status.canAntOrchardAssistFriendToday()) {
                Log.record(TAG, "今日已助力，跳过农场助力");
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

    // --- 一键捉鸡辅助方法 ---

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