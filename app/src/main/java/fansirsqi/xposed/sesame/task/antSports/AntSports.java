package fansirsqi.xposed.sesame.task.antSports;

import android.annotation.SuppressLint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;//健康岛
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;//健康岛
import java.util.Random;
import java.util.Calendar;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.data.StatusFlags;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.TimeCounter;


public class AntSports extends ModelTask {
    private static final String TAG = AntSports.class.getSimpleName();
    private static final String SPORTS_TASKS_COMPLETED_DATE = "SPORTS_TASKS_COMPLETED_DATE"; // 运动任务完成日期缓存键
    private static final String TRAIN_FRIEND_ZERO_COIN_DATE = "TRAIN_FRIEND_ZERO_COIN_DATE"; // 训练好友0金币达上限日期缓存键
    private int tmpStepCount = -1;
    private BooleanModelField walk;
    private ChoiceModelField walkPathTheme;
    private String walkPathThemeId;
    private BooleanModelField walkCustomPath;
    private StringModelField walkCustomPathId;
    private BooleanModelField openTreasureBox;
    private BooleanModelField receiveCoinAsset;
    private BooleanModelField donateCharityCoin;
    private ChoiceModelField donateCharityCoinType;
    private IntegerModelField donateCharityCoinAmount;
    private IntegerModelField minExchangeCount;
    private IntegerModelField latestExchangeTime;
    private IntegerModelField syncStepCount;
    private BooleanModelField tiyubiz;
    private BooleanModelField battleForFriends; // 抢好友总开关
    private ChoiceModelField battleForFriendType;
    private SelectModelField originBossIdList;
    private BooleanModelField sportsTasks;

    // 训练好友相关变量
    private BooleanModelField trainFriend;
    private IntegerModelField zeroCoinLimit;

    // 记录训练好友获得0金币的次数
    private int zeroTrainCoinCount = 0;

    // 运动任务黑名单
    private StringModelField sportsTaskBlacklist;

    //健康岛任务
    private BooleanModelField neverlandTask;  //健康岛任务
    private BooleanModelField neverlandGrid;    //健康岛走路

    private IntegerModelField neverlandGridStepCount;   //健康岛


    private StringModelField sportSyncTime;  // 自定义同步时间

    /**
     * 获取任务名称
     *
     * @return 运动任务名称
     */
    @Override
    public String getName() {
        return "运动";
    }

    /**
     * 获取任务分组
     *
     * @return 运动分组
     */
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.SPORTS;
    }

    /**
     * 获取任务图标
     *
     * @return 运动任务图标文件名
     */
    @Override
    public String getIcon() {
        return "AntSports.png";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(walk = new BooleanModelField("walk", "行走路线 | 开启", false));
        modelFields.addField(walkPathTheme = new ChoiceModelField("walkPathTheme", "行走路线 | 主题", WalkPathTheme.DA_MEI_ZHONG_GUO, WalkPathTheme.nickNames));
        modelFields.addField(walkCustomPath = new BooleanModelField("walkCustomPath", "行走路线 | 开启自定义路线", false));
        modelFields.addField(walkCustomPathId = new StringModelField("walkCustomPathId", "行走路线 | 自定义路线代码(debug)", "p0002023122214520001"));
        modelFields.addField(openTreasureBox = new BooleanModelField("openTreasureBox", "开启宝箱", false));
        modelFields.addField(sportsTasks = new BooleanModelField("sportsTasks", "开启运动任务", false));
        modelFields.addField(sportsTaskBlacklist = new StringModelField("sportsTaskBlacklist", "运动任务黑名单 | 任务名称(用,分隔)", "开通包裹查询服务,添加支付宝小组件,领取价值1.7万元配置,支付宝积分可兑券"));
        modelFields.addField(receiveCoinAsset = new BooleanModelField("receiveCoinAsset", "收能量🎈", false));
        modelFields.addField(donateCharityCoin = new BooleanModelField("donateCharityCoin", "捐能量🎈 | 开启", false));
        modelFields.addField(donateCharityCoinType = new ChoiceModelField("donateCharityCoinType", "捐能量🎈 | 方式", DonateCharityCoinType.ONE, DonateCharityCoinType.nickNames));
        modelFields.addField(donateCharityCoinAmount = new IntegerModelField("donateCharityCoinAmount", "捐能量🎈 | 数量(每次)", 100));
        // 健康岛任务
        modelFields.addField(neverlandTask =  new BooleanModelField("neverlandTask", "健康岛 | 任务", false));
        modelFields.addField(neverlandGrid =  new BooleanModelField("neverlandGrid", "健康岛 | 自动走路建造", false));
        modelFields.addField(neverlandGridStepCount = new IntegerModelField("neverlandGridStepCount", "健康岛 | 今日走路最大次数", 20));
        // 抢好友相关配置
        modelFields.addField(battleForFriends = new BooleanModelField("battleForFriends", "抢好友 | 开启", false));
        modelFields.addField(battleForFriendType = new ChoiceModelField("battleForFriendType", "抢好友 | 动作", BattleForFriendType.ROB, BattleForFriendType.nickNames));
        modelFields.addField(originBossIdList = new SelectModelField("originBossIdList", "抢好友 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));

        // 训练好友相关配置
        modelFields.addField(trainFriend = new BooleanModelField("trainFriend", "训练好友 | 开启", false));
        modelFields.addField(zeroCoinLimit = new IntegerModelField("zeroCoinLimit", "训练好友 | 0金币上限次数当天关闭", 5));

        modelFields.addField(tiyubiz = new BooleanModelField("tiyubiz", "文体中心", false));
        modelFields.addField(minExchangeCount = new IntegerModelField("minExchangeCount", "最小捐步步数", 0));
        modelFields.addField(latestExchangeTime = new IntegerModelField("latestExchangeTime", "最晚捐步时间(24小时制)", 22));
        modelFields.addField(sportSyncTime = new StringModelField("sportSyncTime", "自定义同步时间(关闭:-1)", "0800"));
        modelFields.addField(syncStepCount = new IntegerModelField("syncStepCount", "自定义同步步数", 22000));
        // 本地变量，用于添加字段到模型
        BooleanModelField coinExchangeDoubleCard = new BooleanModelField("coinExchangeDoubleCard", "能量🎈兑换限时能量双击卡", false);
        modelFields.addField(coinExchangeDoubleCard);
        return modelFields;
    }

    /**
     * 启动任务，进行必要的初始化操作
     *
     * @param classLoader 类加载器
     */
    @Override
    public void boot(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.alibaba.health.pedometer.core.datasource.PedometerAgent", classLoader,
                    "readDailyStep", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            int originStep = (Integer) param.getResult();
                            int step = tmpStepCount();
                            if (TaskCommon.IS_AFTER_8AM && originStep < step) {//早于8点或步数小于自定义步数hook
                                param.setResult(step);
                            }
                        }
                    });
            Log.runtime(TAG, "hook readDailyStep successfully");
        } catch (Throwable t) {
            Log.runtime(TAG, "hook readDailyStep err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行运动任务
     */
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

    /**
     * 执行运动任务的主要逻辑
     */
    @Override
    public void run() {
        Log.record(TAG, "执行开始-" + getName());
        try {
            if (neverlandTask.getValue()||neverlandGrid.getValue()) {
                Log.record(TAG, "开始执行健康岛");
                NeverlandTaskHandler handler = new NeverlandTaskHandler();
                handler.runNeverland();
                Log.record(TAG, "健康岛结束");
            }

            sportSyncStepSchedule();

            if (sportsTasks.getValue()) {
                sportsTasks();
            }

            ClassLoader loader = ApplicationHook.getClassLoader();

            if (walk.getValue()) {
                getWalkPathThemeIdOnConfig();
                walk();
            }

            if (openTreasureBox.getValue() && !walk.getValue()) {
                queryMyHomePage(loader);
            }

            if (donateCharityCoin.getValue() && Status.canDonateCharityCoin()) {
                queryProjectList(loader);
            }

            if (minExchangeCount.getValue() > 0
                    && Status.canExchangeToday(UserMap.getCurrentUid())) {
                queryWalkStep(loader);
            }

            if (tiyubiz.getValue()) {
                userTaskGroupQuery("SPORTS_DAILY_SIGN_GROUP");
                userTaskGroupQuery("SPORTS_DAILY_GROUP");
                userTaskRightsReceive();
                pathFeatureQuery();
                participate();
            }

            if (battleForFriends.getValue()) {
                queryClubHome();
                queryTrainItem();
                buyMember();
            }

            if (receiveCoinAsset.getValue()) {
                receiveCoinAsset();
            }

        } catch (Throwable t) {
            Log.runtime(TAG, "runJava error:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    private void coinExchangeItem(String itemId) {
        try {
            JSONObject jo = new JSONObject(AntSportsRpcCall.queryItemDetail(itemId));
            if (!ResChecker.checkRes(TAG, jo)) {
                return;
            }
            jo = jo.getJSONObject("data");
            if (!"OK".equals(jo.optString("exchangeBtnStatus"))) {
                return;
            }
            jo = jo.getJSONObject("itemBaseInfo");
            String itemTitle = jo.getString("itemTitle");
            int valueCoinCount = jo.getInt("valueCoinCount");
            jo = new JSONObject(AntSportsRpcCall.exchangeItem(itemId, valueCoinCount));
            if (!ResChecker.checkRes(TAG, jo)) {
                return;
            }
            jo = jo.getJSONObject("data");
            if (jo.optBoolean("exgSuccess")) {
                Log.other(TAG, "运动好礼🎐兑换[" + itemTitle + "]花费" + valueCoinCount + "运动币");
            }
        } catch (Throwable t) {
            Log.error(TAG, "trainMember err:");
            Log.printStackTrace(TAG, t);
        }
    }


    /**
     * 定时同步步数
     */
    private void sportSyncStepSchedule() {
        try {
            String syncTimeStr = sportSyncTime.getValue();
            if ("-1".equals(syncTimeStr)) {
                Log.runtime(TAG, "当前已关闭定时同步步数");
                return;
            }

            Calendar now = TimeUtil.getNow();
            Calendar syncTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(syncTimeStr);
            if (syncTimeCalendar == null) {
                Log.record(TAG, "同步步数时间格式错误，请重新设置");
                return;
            }

            long syncTime = syncTimeCalendar.getTimeInMillis();
            String syncTaskId = "SYNC_STEP|" + syncTime;

            boolean afterSyncTime = now.compareTo(syncTimeCalendar) > 0;

            if (!hasChildTask(syncTaskId) && !afterSyncTime) {
                addChildTask(new ChildModelTask(syncTaskId, "SYNC_STEP", this::syncStepNow, syncTime));
                Log.record(TAG, "添加定时同步步数🏃🏻‍♂️[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(syncTime) + "]执行");
            }

            if (afterSyncTime) {
                if (!Status.hasFlagToday("sport::syncStep")) {
                    syncStepNow();
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "sportSyncStepSchedule err:");
            Log.printStackTrace(e);
        }
    }

    private void syncStepNow() {
        if (Status.hasFlagToday("sport::syncStep")) {
            return;
        }

        int step = tmpStepCount();
        try {
            ClassLoader classLoader = ApplicationHook.getClassLoader();
            Object rpcManager = XposedHelpers.callStaticMethod(
                    classLoader.loadClass("com.alibaba.health.pedometer.intergation.rpc.RpcManager"),
                    "a"
            );
            boolean success = (Boolean) XposedHelpers.callMethod(
                    rpcManager,
                    "a",
                    step, Boolean.FALSE, "system"
            );

            if (success) {
                Log.other(TAG, "同步步数🏃🏻‍♂️[" + step + "步]");
            } else {
                Log.error(TAG, "同步运动步数失败:" + step);
            }
            Status.setFlagToday("sport::syncStep");
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 获取临时步数
     *
     * @return 步数值
     */
    public int tmpStepCount() {
        if (tmpStepCount >= 0) {
            return tmpStepCount;
        }
        tmpStepCount = syncStepCount.getValue();
        if (tmpStepCount > 0) {
            tmpStepCount = RandomUtil.nextInt(tmpStepCount, tmpStepCount + 2000);
            if (tmpStepCount > 100000) {
                tmpStepCount = 100000;
            }
        }
        return tmpStepCount;
    }

    // 运动
    private void sportsTasks() {
        try {
            sportsCheck_in();
            // 运动任务查询
            JSONObject jo = new JSONObject(AntSportsRpcCall.queryCoinTaskPanel());
            //  Log.record(TAG,"运动任务响应："+jo);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                JSONArray taskList = data.getJSONArray("taskList");

                // 统计任务完成状态
                int totalTasks = 0;
                int completedTasks = 0;
                int availableTasks = 0; // 可执行的任务数

                for (int i = 0; i < taskList.length(); i++) {
                    JSONObject taskDetail = taskList.getJSONObject(i);
                    String taskId = taskDetail.getString("taskId");
                    String taskName = taskDetail.getString("taskName");
                    String prizeAmount = taskDetail.getString("prizeAmount");
                    String taskStatus = taskDetail.getString("taskStatus");
                    int currentNum = taskDetail.getInt("currentNum");
                    // 要完成的次数
                    int limitConfigNum = taskDetail.getInt("limitConfigNum") - currentNum;

                    // 统计总任务数（排除特殊任务类型）
                    String taskType = taskDetail.optString("taskType", "");
                    if (!taskType.equals("SETTLEMENT")) { // 排除步数和锻炼时长等自动完成的任务
                        totalTasks++;


                        // 获取按钮文本和assetId
                        String buttonText = taskDetail.getString("buttonText");


                        // 检查任务是否在黑名单中
                        String blacklistStr = sportsTaskBlacklist.getValue();
                        if (blacklistStr != null && !blacklistStr.trim().isEmpty()) {
                            String[] blacklist = blacklistStr.split(",");
                            boolean isBlacklisted = false;
                            for (String blackItem : blacklist) {
                                if (taskName.contains(blackItem.trim())) {
                                    isBlacklisted = true;
                                    break;
                                }
                            }
                            if (isBlacklisted) {
                                Log.record(TAG, "做任务得能量🎈[任务已屏蔽：" + taskName + "（在黑名单中）]");
                                completedTasks++; // 将黑名单任务视为已完成
                                continue;
                            }
                        }

                        // 跳过已完成的任务（检查状态和按钮文本）
                        if (buttonText.equals("任务已完成")) {
                            Log.record(TAG, "做任务得能量🎈[任务已完成：" + taskName + "，状态：" + taskStatus + "，按钮：" + buttonText + "]");
                            completedTasks++;
                            continue;
                        }

                        // 判断并领取奖励
                        if (buttonText.equals("领取奖励")) {
                            String assetId = taskDetail.getString("assetId");
                            String result = AntSportsRpcCall.pickBubbleTaskEnergy(assetId);
                            try {
                                JSONObject resultData = new JSONObject(result);
                                if (resultData.optBoolean("success", false)) {
                                    String changeAmount = resultData.optString("changeAmount", "0");
                                    Log.record(TAG, "做任务得能量🎈[领取成功：" + taskName +
                                            "，获得：" + changeAmount + "能量🎈]");
                                    completedTasks++;
                                } else {
                                    String errorMsg = resultData.optString("errorMsg", "未知错误");
                                    String errorCode = resultData.optString("errorCode", "");
                                    Log.record(TAG, "做任务得能量🎈[领取失败：" + taskName +
                                            "，错误：" + errorCode + " - " + errorMsg + "]");
                                    // 如果是不可重试的错误，标记为已完成避免重复尝试
                                    if (!resultData.optBoolean("retryable", true) ||
                                            "CAMP_TRIGGER_ERROR".equals(errorCode)) {
                                        completedTasks++;
                                        Log.record(TAG, "做任务得能量🎈[任务已标记完成，避免重复尝试：" + taskName + "]");
                                    }
                                }
                                continue;
                            } catch (Exception e) {
                                Log.record(TAG, "做任务得能量🎈[响应解析异常：" + taskName + "，错误：" + e.getMessage() + "]");
                            }
                        }

                        // 跳过不需要完成的任务状态
                        if (!taskStatus.equals("WAIT_RECEIVE") && !taskStatus.equals("WAIT_COMPLETE")) {
                            Log.record(TAG, "做任务得能量🎈[跳过任务：" + taskName + "，状态：" + taskStatus + "]");
                            continue;
                        }

                        // 检查是否需要执行任务
                        if (limitConfigNum <= 0) {
                            Log.record(TAG, "做任务得能量🎈[任务无需执行：" + taskName + "，已完成" + currentNum + "/" + taskDetail.getInt("limitConfigNum") + "]");
                            completedTasks++;
                            continue;
                        }
                        // 这是一个可执行的任务
                        availableTasks++;
                        Log.record(TAG, "做任务得能量🎈[开始执行任务：" + taskName + "，需完成" + limitConfigNum + "次]");
                        for (int i1 = 0; i1 < limitConfigNum; i1++) {
                            jo = new JSONObject(AntSportsRpcCall.completeExerciseTasks(taskId));
                            if (jo.optBoolean("success")) {
                                Log.record(TAG, "做任务得能量🎈[完成任务：" + taskName + "，得" + prizeAmount + "💰]#(" + (i1 + 1) + "/" + limitConfigNum + ")");
                                receiveCoinAsset();
                            } else {
                                Log.record(TAG, "做任务得能量🎈[任务执行失败：" + taskName + "]#(" + (i1 + 1) + "/" + limitConfigNum + ")");
                                break; // 失败时跳出循环
                            }
                            if (limitConfigNum > 1 && i1 < limitConfigNum - 1) {
                                GlobalThreadPools.sleep(10000);
                            }
                        }
                        // 任务执行完成后，增加完成计数
                        completedTasks++;
                    }
                }
                // 检查是否所有可执行任务都已完成
                Log.record(TAG, "运动任务完成情况：" + completedTasks + "/" + totalTasks + "，可执行任务：" + availableTasks);
                // 如果所有可执行的任务都已完成（没有可执行的任务了），记录当天日期，今日不再执行
                if (totalTasks > 0 && completedTasks >= totalTasks && availableTasks == 0) {
                    String today = TimeUtil.getDateStr2();
                    DataStore.INSTANCE.put(SPORTS_TASKS_COMPLETED_DATE, today);
                    Log.record(TAG, "✅ 所有运动任务已完成，今日不再执行，明日自动恢复");
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void sportsCheck_in() {
        try {
            JSONObject jo = new JSONObject(AntSportsRpcCall.sportsCheck_in());
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.getBoolean("signed")) {
                    JSONObject subscribeConfig;
                    if (data.has("subscribeConfig")) {
                        subscribeConfig = data.getJSONObject("subscribeConfig");
                        Log.record(TAG, "做任务得能量🎈能量🎈[完成任务：签到" + subscribeConfig.getString("subscribeExpireDays") + "天，" + data.getString("toast") + "💰]");
                    }
                } else {
                    Log.record(TAG, "运动签到今日已签到");
                }
            } else {
                Log.record(jo.toString());
            }
        } catch (Exception e) {
            Log.record(TAG, "sportsCheck_in err");
            Log.printStackTrace(e);
        }
    }

    private void receiveCoinAsset() {
        try {
            String s = AntSportsRpcCall.queryCoinBubbleModule();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询金币气泡模块失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.has("receiveCoinBubbleList"))
                    return;
                JSONArray ja = data.getJSONArray("receiveCoinBubbleList");
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    String assetId = jo.getString("assetId");
                    int coinAmount = jo.getInt("coinAmount");
                    jo = new JSONObject(AntSportsRpcCall.receiveCoinAsset(assetId, coinAmount));
                    if (jo.optBoolean("success")) {
                        Log.other(TAG, "收集金币💰[" + coinAmount + "个]");
                    } else {
                        Log.record(TAG, "首页收集金币" + " " + jo);
                    }
                }
            } else {
                Log.runtime(TAG, s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveCoinAsset err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /*
     * 新版行走路线 -- begin
     */
    private void walk() {
        try {
            String userResponse = AntSportsRpcCall.queryUser();
            if (userResponse == null || userResponse.trim().isEmpty()) {
                Log.record(TAG, "查询用户信息失败：返回空响应");
                return;
            }

            JSONObject user = new JSONObject(userResponse);
            if (!user.optBoolean("success")) {
                return;
            }
            String joinedPathId = user.getJSONObject("data").getString("joinedPathId");
            JSONObject path = queryPath(joinedPathId);
            if (path == null) {
                return;
            }
            JSONObject userPathStep = path.getJSONObject("userPathStep");
            if ("COMPLETED".equals(userPathStep.getString("pathCompleteStatus"))) {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + userPathStep.getString("pathName") + "]已完成");
                String pathId = queryJoinPath(walkPathThemeId);
                joinPath(pathId);
                return;
            }
            int minGoStepCount = path.getJSONObject("path").getInt("minGoStepCount");
            int pathStepCount = path.getJSONObject("path").getInt("pathStepCount");
            int forwardStepCount = userPathStep.getInt("forwardStepCount");
            int remainStepCount = userPathStep.getInt("remainStepCount");
            int needStepCount = pathStepCount - forwardStepCount;
            if (remainStepCount >= minGoStepCount) {
                int useStepCount = Math.min(remainStepCount, needStepCount);
                walkGo(userPathStep.getString("pathId"), useStepCount, userPathStep.getString("pathName"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "walk err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void walkGo(String pathId, int useStepCount, String pathName) {
        try {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String walkGoResponse = AntSportsRpcCall.walkGo("202312191135", sdf.format(date), pathId, useStepCount);
            if (walkGoResponse == null || walkGoResponse.trim().isEmpty()) {
                Log.record(TAG, "行走前进失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(walkGoResponse);
            if (jo.optBoolean("success")) {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + pathName + "]#前进了" + useStepCount + "步");
                queryPath(pathId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "walkGo err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private JSONObject queryWorldMap(String themeId) {
        JSONObject theme = null;
        try {
            String worldMapResponse = AntSportsRpcCall.queryWorldMap(themeId);
            if (worldMapResponse == null || worldMapResponse.trim().isEmpty()) {
                Log.record(TAG, "查询世界地图失败：返回空响应");
                return null;
            }

            JSONObject jo = new JSONObject(worldMapResponse);
            if (jo.optBoolean("success")) {
                theme = jo.getJSONObject("data");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryWorldMap err:");
            Log.printStackTrace(TAG, t);
        }
        return theme;
    }

    private JSONObject queryCityPath(String cityId) {
        JSONObject city = null;
        try {
            String cityPathResponse = AntSportsRpcCall.queryCityPath(cityId);
            if (cityPathResponse == null || cityPathResponse.trim().isEmpty()) {
                Log.record(TAG, "查询城市路径失败：返回空响应");
                return null;
            }

            JSONObject jo = new JSONObject(cityPathResponse);
            if (jo.optBoolean("success")) {
                city = jo.getJSONObject("data");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryCityPath err:");
            Log.printStackTrace(TAG, t);
        }
        return city;
    }

    private JSONObject queryPath(String pathId) {
        JSONObject path = null;
        try {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String pathResponse = AntSportsRpcCall.queryPath("202312191135", sdf.format(date), pathId);
            if (pathResponse == null || pathResponse.trim().isEmpty()) {
                Log.record(TAG, "查询路径失败：返回空响应");
                return null;
            }

            JSONObject jo = new JSONObject(pathResponse);
            if (jo.optBoolean("success")) {
                path = jo.getJSONObject("data");
                JSONArray ja = jo.getJSONObject("data").getJSONArray("treasureBoxList");
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject treasureBox = ja.getJSONObject(i);
                    receiveEvent(treasureBox.getString("boxNo"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryPath err:");
            Log.printStackTrace(TAG, t);
        }
        return path;
    }

    private void receiveEvent(String eventBillNo) {
        try {
            String eventResponse = AntSportsRpcCall.receiveEvent(eventBillNo);
            if (eventResponse == null || eventResponse.trim().isEmpty()) {
                Log.record(TAG, "接收事件失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(eventResponse);
            if (!jo.optBoolean("success")) {
                return;
            }
            JSONArray ja = jo.getJSONObject("data").getJSONArray("rewards");
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);
                Log.record(TAG, "行走路线🎁开启宝箱[" + jo.getString("rewardName") + "]*" + jo.getInt("count"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveEvent err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private String queryJoinPath(String themeId) {
        if (walkCustomPath.getValue()) {
            return walkCustomPathId.getValue();
        }
        String pathId = null;
        try {
            JSONObject theme = queryWorldMap(walkPathThemeId);
            if (theme == null) {
                return null;
            }
            JSONArray cityList = theme.getJSONArray("cityList");
            for (int i = 0; i < cityList.length(); i++) {
                String cityId = cityList.getJSONObject(i).getString("cityId");
                JSONObject city = queryCityPath(cityId);
                if (city == null) {
                    continue;
                }
                JSONArray cityPathList = city.getJSONArray("cityPathList");
                for (int j = 0; j < cityPathList.length(); j++) {
                    JSONObject cityPath = cityPathList.getJSONObject(j);
                    pathId = cityPath.getString("pathId");
                    if (!"COMPLETED".equals(cityPath.getString("pathCompleteStatus"))) {
                        return pathId;
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryJoinPath err:");
            Log.printStackTrace(TAG, t);
        }
        return pathId;
    }

    private void joinPath(String pathId) {
        if (pathId == null) {
            // 龙年祈福线
            pathId = "p0002023122214520001";
        }
        try {
            String joinPathResponse = AntSportsRpcCall.joinPath(pathId);
            if (joinPathResponse == null || joinPathResponse.trim().isEmpty()) {
                Log.record(TAG, "加入路径失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(joinPathResponse);
            if (jo.optBoolean("success")) {
                JSONObject path = queryPath(pathId);
                if (path != null) {
                    Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + path.getJSONObject("path").getString("name") + "]已加入");
                }
            } else {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + pathId + "]有误，无法加入！");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "joinPath err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void getWalkPathThemeIdOnConfig() {
        if (walkPathTheme.getValue() == WalkPathTheme.DA_MEI_ZHONG_GUO) {
            walkPathThemeId = "M202308082226";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.GONG_YI_YI_XIAO_BU) {
            walkPathThemeId = "M202401042147";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.DENG_DING_ZHI_MA_SHAN) {
            walkPathThemeId = "V202405271625";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.WEI_C_DA_TIAO_ZHAN) {
            walkPathThemeId = "202404221422";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.LONG_NIAN_QI_FU) {
            walkPathThemeId = "WF202312050200";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.SHOU_HU_TI_YU_MENG) {
            walkPathThemeId = "V202409061650";
        }
    }

    /*
     * 新版行走路线 -- end
     */
    private void queryMyHomePage(ClassLoader loader) {
        try {
            String s = AntSportsRpcCall.queryMyHomePage();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询我的主页失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "查询运动主页失败:", jo)) {
                s = jo.getString("pathJoinStatus");
                if ("GOING".equals(s)) {
                    if (jo.has("pathCompleteStatus")) {
                        if ("COMPLETED".equals(jo.getString("pathCompleteStatus"))) {
                            jo = new JSONObject(AntSportsRpcCall.queryBaseList());
                            if (ResChecker.checkRes(TAG + "查询运动基地列表失败:", jo)) {
                                JSONArray allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList");
                                JSONArray otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList")
                                        .getJSONObject(0)
                                        .getJSONArray("allPathBaseInfoList");
                                join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, "");
                            } else {
                                Log.runtime(TAG, jo.getString("resultDesc"));
                            }
                        }
                    } else {
                        String rankCacheKey = jo.getString("rankCacheKey");
                        JSONArray ja = jo.getJSONArray("treasureBoxModelList");
                        for (int i = 0; i < ja.length(); i++) {
                            parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey);
                        }
                        JSONObject joPathRender = jo.getJSONObject("pathRenderModel");
                        String title = joPathRender.getString("title");
                        int minGoStepCount = joPathRender.getInt("minGoStepCount");
                        jo = jo.getJSONObject("dailyStepModel");
                        int consumeQuantity = jo.getInt("consumeQuantity");
                        int produceQuantity = jo.getInt("produceQuantity");
                        String day = jo.getString("day");
                        int canMoveStepCount = produceQuantity - consumeQuantity;
                        if (canMoveStepCount >= minGoStepCount) {
                            go(loader, day, rankCacheKey, canMoveStepCount, title);
                        }
                    }
                } else if ("NOT_JOIN".equals(s)) {
                    String firstJoinPathTitle = jo.getString("firstJoinPathTitle");
                    JSONArray allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList");
                    JSONArray otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList").getJSONObject(0)
                            .getJSONArray("allPathBaseInfoList");
                    join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, firstJoinPathTitle);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryMyHomePage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void join(ClassLoader loader, JSONArray allPathBaseInfoList, JSONArray otherAllPathBaseInfoList,
                      String firstJoinPathTitle) {
        try {
            int index = -1;
            String title = null;
            String pathId = null;
            JSONObject jo;
            for (int i = allPathBaseInfoList.length() - 1; i >= 0; i--) {
                jo = allPathBaseInfoList.getJSONObject(i);
                if (jo.getBoolean("unlocked")) {
                    title = jo.getString("title");
                    pathId = jo.getString("pathId");
                    index = i;
                    break;
                }
            }
            if (index < 0 || index == allPathBaseInfoList.length() - 1) {
                for (int j = otherAllPathBaseInfoList.length() - 1; j >= 0; j--) {
                    jo = otherAllPathBaseInfoList.getJSONObject(j);
                    if (jo.getBoolean("unlocked")) {
                        if (j != otherAllPathBaseInfoList.length() - 1 || index != allPathBaseInfoList.length() - 1) {
                            title = jo.getString("title");
                            pathId = jo.getString("pathId");
                        }
                        break;
                    }
                }
            }
            if (pathId != null) {
                String s;
                if (title != null && title.equals(firstJoinPathTitle)) {
                    s = AntSportsRpcCall.openAndJoinFirst();
                } else {
                    s = AntSportsRpcCall.join(pathId);
                }
                jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG + "加入运动线路失败:", jo)) {
                    Log.other(TAG, "加入线路🚶🏻‍♂️[" + title + "]");
                    queryMyHomePage(loader);
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            } else {
                Log.record(TAG, "好像没有可走的线路了！");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "join err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void go(ClassLoader loader, String day, String rankCacheKey, int stepCount, String title) {
        try {
            String s = AntSportsRpcCall.go(day, rankCacheKey, stepCount);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "行走前进失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "运动行走失败:", jo)) {
                Log.other(TAG, "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("goStepCount") + "步");
                boolean completed = "COMPLETED".equals(jo.getString("completeStatus"));
                JSONArray ja = jo.getJSONArray("allTreasureBoxModelList");
                for (int i = 0; i < ja.length(); i++) {
                    parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey);
                }
                if (completed) {
                    Log.other(TAG, "完成线路🚶🏻‍♂️[" + title + "]");
                    queryMyHomePage(loader);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "go err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void parseTreasureBoxModel(ClassLoader loader, JSONObject jo, String rankCacheKey) {
        try {
            String canOpenTime = jo.getString("canOpenTime");
            String issueTime = jo.getString("issueTime");
            String boxNo = jo.getString("boxNo");
            String userId = jo.getString("userId");
            if (canOpenTime.equals(issueTime)) {
                openTreasureBox(loader, boxNo, userId);
            } else {
                long cot = Long.parseLong(canOpenTime);
                long now = Long.parseLong(rankCacheKey);
                long delay = cot - now;
                if (delay <= 0) {
                    openTreasureBox(loader, boxNo, userId);
                    return;
                }
                if (delay < BaseModel.getCheckInterval().getValue()) {
                    String taskId = "BX|" + boxNo;
                    if (hasChildTask(taskId)) {
                        return;
                    }
                    Log.record(TAG, "还有 " + delay + "ms 开运动宝箱");
                    addChildTask(new ChildModelTask(taskId, "BX", () -> {
                        Log.record(TAG, "蹲点开箱开始");
                        long startTime = System.currentTimeMillis();
                        while (System.currentTimeMillis() - startTime < 5_000) {
                            if (openTreasureBox(loader, boxNo, userId) > 0) {
                                break;
                            }
                            GlobalThreadPools.sleep(200);
                        }
                    }, System.currentTimeMillis() + delay));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "parseTreasureBoxModel err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private int openTreasureBox(ClassLoader loader, String boxNo, String userId) {
        try {
            String s = AntSportsRpcCall.openTreasureBox(boxNo, userId);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "开启宝箱失败：返回空响应");
                return 0;
            }

            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "开启运动宝箱失败:", jo)) {
                JSONArray ja = jo.getJSONArray("treasureBoxAwards");
                int num = 0;
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    num += jo.getInt("num");
                    Log.other(TAG, "运动宝箱🎁[" + num + jo.getString("name") + "]");
                }
                return num;
            } else if ("TREASUREBOX_NOT_EXIST".equals(jo.getString("resultCode"))) {
                Log.record(jo.getString("resultDesc"));
                return 1;
            } else {
                Log.record(jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "openTreasureBox err:");
            Log.printStackTrace(TAG, t);
        }
        return 0;
    }

    private void queryProjectList(ClassLoader loader) {
        try {
            String projectListResponse = AntSportsRpcCall.queryProjectList(0);
            if (projectListResponse == null || projectListResponse.trim().isEmpty()) {
                Log.record(TAG, "查询项目列表失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(projectListResponse);
            if (ResChecker.checkRes(TAG + "查询运动项目列表失败:", jo)) {
                int charityCoinCount = jo.getInt("charityCoinCount");
                if (charityCoinCount < donateCharityCoinAmount.getValue()) {
                    return;
                }
                JSONArray ja = jo.getJSONObject("projectPage").getJSONArray("data");
                for (int i = 0; i < ja.length() && charityCoinCount >= donateCharityCoinAmount.getValue(); i++) {
                    jo = ja.getJSONObject(i).getJSONObject("basicModel");
                    if ("DONATE_COMPLETED".equals(jo.getString("footballFieldStatus"))) {
                        break;
                    }
                    donate(loader, donateCharityCoinAmount.getValue(), jo.getString("projectId"), jo.getString("title"));
                    Status.donateCharityCoin();
                    charityCoinCount -= donateCharityCoinAmount.getValue();
                    if (donateCharityCoinType.getValue() == DonateCharityCoinType.ONE) {
                        break;
                    }
                }
            } else {
                Log.record(TAG);
                Log.runtime(jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryProjectList err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void donate(ClassLoader loader, int donateCharityCoin, String projectId, String title) {
        try {
            String s = AntSportsRpcCall.donate(donateCharityCoin, projectId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "运动捐赠失败:", jo)) {
                Log.other(TAG, "捐赠活动❤️[" + title + "][" + donateCharityCoin + "能量🎈]");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "donate err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void queryWalkStep(ClassLoader loader) {
        try {
            String s = AntSportsRpcCall.queryWalkStep();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询行走步数失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "查询运动步数失败:", jo)) {
                jo = jo.getJSONObject("dailyStepModel");
                int produceQuantity = jo.getInt("produceQuantity");
                int hour = Integer.parseInt(TimeUtil.getFormatTime().split(":")[0]);

                if (produceQuantity >= minExchangeCount.getValue() || hour >= latestExchangeTime.getValue()) {
                    AntSportsRpcCall.walkDonateSignInfo(produceQuantity);
                    s = AntSportsRpcCall.donateWalkHome(produceQuantity);
                    jo = new JSONObject(s);
                    if (!jo.getBoolean("isSuccess"))
                        return;
                    JSONObject walkDonateHomeModel = jo.getJSONObject("walkDonateHomeModel");
                    JSONObject walkUserInfoModel = walkDonateHomeModel.getJSONObject("walkUserInfoModel");
                    if (!walkUserInfoModel.has("exchangeFlag")) {
                        Status.exchangeToday(UserMap.getCurrentUid());
                        return;
                    }
                    String donateToken = walkDonateHomeModel.getString("donateToken");
                    JSONObject walkCharityActivityModel = walkDonateHomeModel.getJSONObject("walkCharityActivityModel");
                    String activityId = walkCharityActivityModel.getString("activityId");
                    s = AntSportsRpcCall.exchange(activityId, produceQuantity, donateToken);
                    jo = new JSONObject(s);
                    if (jo.getBoolean("isSuccess")) {
                        JSONObject donateExchangeResultModel = jo.getJSONObject("donateExchangeResultModel");
                        int userCount = donateExchangeResultModel.getInt("userCount");
                        double amount = donateExchangeResultModel.getJSONObject("userAmount").getDouble("amount");
                        Log.other(TAG, "捐出活动❤️[" + userCount + "步]#兑换" + amount + "元公益金");
                        Status.exchangeToday(UserMap.getCurrentUid());
                    } else if (s.contains("已捐步")) {
                        Status.exchangeToday(UserMap.getCurrentUid());
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"));
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryWalkStep err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 文体中心 */// SPORTS_DAILY_SIGN_GROUP SPORTS_DAILY_GROUP
    private void userTaskGroupQuery(String groupId) {
        try {
            String s = AntSportsRpcCall.userTaskGroupQuery(groupId);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询任务组失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group");
                JSONArray userTaskList = jo.getJSONArray("userTaskList");
                for (int i = 0; i < userTaskList.length(); i++) {
                    jo = userTaskList.getJSONObject(i);
                    if (!"TODO".equals(jo.getString("status")))
                        continue;
                    JSONObject taskInfo = jo.getJSONObject("taskInfo");
                    String bizType = taskInfo.getString("bizType");
                    String taskId = taskInfo.getString("taskId");

                    String completeResponse = AntSportsRpcCall.userTaskComplete(bizType, taskId);
                    if (completeResponse == null || completeResponse.trim().isEmpty()) {
                        Log.record(TAG, "完成任务失败：返回空响应");
                        continue;
                    }

                    jo = new JSONObject(completeResponse);
                    if (jo.optBoolean("success")) {
                        String taskName = taskInfo.optString("taskName", taskId);
                        Log.other(TAG, "完成任务🧾[" + taskName + "]");
                    } else {
                        Log.record(TAG, "文体每日任务" + " " + jo);
                    }
                }
            } else {
                Log.record(TAG, "文体每日任务" + " " + s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "userTaskGroupQuery err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void participate() {
        try {
            String s = AntSportsRpcCall.queryAccount();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询账户信息失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                double balance = jo.getDouble("balance");
                if (balance < 100)
                    return;
                jo = new JSONObject(AntSportsRpcCall.queryRoundList());
                if (jo.optBoolean("success")) {
                    JSONArray dataList = jo.getJSONArray("dataList");
                    for (int i = 0; i < dataList.length(); i++) {
                        jo = dataList.getJSONObject(i);
                        if (!"P".equals(jo.getString("status")))
                            continue;
                        if (jo.has("userRecord"))
                            continue;
                        JSONArray instanceList = jo.getJSONArray("instanceList");
                        int pointOptions = 0;
                        String roundId = jo.getString("id");
                        String InstanceId = null;
                        String ResultId = null;
                        for (int j = instanceList.length() - 1; j >= 0; j--) {
                            jo = instanceList.getJSONObject(j);
                            if (jo.getInt("pointOptions") < pointOptions)
                                continue;
                            pointOptions = jo.getInt("pointOptions");
                            InstanceId = jo.getString("id");
                            ResultId = jo.getString("instanceResultId");
                        }
                        jo = new JSONObject(AntSportsRpcCall.participate(pointOptions, InstanceId, ResultId, roundId));
                        if (jo.optBoolean("success")) {
                            jo = jo.getJSONObject("data");
                            String roundDescription = jo.getString("roundDescription");
                            int targetStepCount = jo.getInt("targetStepCount");
                            Log.other(TAG, "走路挑战🚶🏻‍♂️[" + roundDescription + "]#" + targetStepCount);
                        } else {
                            Log.record(TAG, "走路挑战赛" + " " + jo);
                        }
                    }
                } else {
                    Log.record(TAG, "queryRoundList" + " " + jo);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "participate err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void userTaskRightsReceive() {
        try {
            String s = AntSportsRpcCall.userTaskGroupQuery("SPORTS_DAILY_GROUP");
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询任务组失败：返回空响应");
                return;
            }

            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group");
                JSONArray userTaskList = jo.getJSONArray("userTaskList");
                for (int i = 0; i < userTaskList.length(); i++) {
                    jo = userTaskList.getJSONObject(i);
                    if (!"COMPLETED".equals(jo.getString("status")))
                        continue;
                    String userTaskId = jo.getString("userTaskId");
                    JSONObject taskInfo = jo.getJSONObject("taskInfo");
                    String taskId = taskInfo.getString("taskId");
                    jo = new JSONObject(AntSportsRpcCall.userTaskRightsReceive(taskId, userTaskId));
                    if (jo.optBoolean("success")) {
                        String taskName = taskInfo.optString("taskName", taskId);
                        JSONArray rightsRuleList = taskInfo.getJSONArray("rightsRuleList");
                        StringBuilder award = new StringBuilder();
                        for (int j = 0; j < rightsRuleList.length(); j++) {
                            jo = rightsRuleList.getJSONObject(j);
                            award.append(jo.getString("rightsName")).append("*").append(jo.getInt("baseAwardCount"));
                        }
                        Log.other(TAG, "领取奖励🎖️[" + taskName + "]#" + award);
                    } else {
                        Log.record(TAG, "文体中心领取奖励");
                        Log.runtime(jo.toString());
                    }
                }
            } else {
                Log.record(TAG, "文体中心领取奖励");
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "userTaskRightsReceive err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathFeatureQuery() {
        try {
            String s = AntSportsRpcCall.pathFeatureQuery();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject path = jo.getJSONObject("path");
                String pathId = path.getString("pathId");
                String title = path.getString("title");
                int minGoStepCount = path.getInt("minGoStepCount");
                if (jo.has("userPath")) {
                    JSONObject userPath = jo.getJSONObject("userPath");
                    String userPathRecordStatus = userPath.getString("userPathRecordStatus");
                    if ("COMPLETED".equals(userPathRecordStatus)) {
                        pathMapHomepage(pathId);
                        pathMapJoin(title, pathId);
                    } else if ("GOING".equals(userPathRecordStatus)) {
                        pathMapHomepage(pathId);
                        String countDate = TimeUtil.getFormatDate();
                        jo = new JSONObject(AntSportsRpcCall.stepQuery(countDate, pathId));
                        if (jo.optBoolean("success")) {
                            int canGoStepCount = jo.getInt("canGoStepCount");
                            if (canGoStepCount >= minGoStepCount) {
                                String userPathRecordId = userPath.getString("userPathRecordId");
                                tiyubizGo(countDate, title, canGoStepCount, pathId, userPathRecordId);
                            }
                        }
                    }
                } else {
                    pathMapJoin(title, pathId);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathFeatureQuery err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathMapHomepage(String pathId) {
        try {
            String s = AntSportsRpcCall.pathMapHomepage(pathId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                if (!jo.has("userPathGoRewardList"))
                    return;
                JSONArray userPathGoRewardList = jo.getJSONArray("userPathGoRewardList");
                for (int i = 0; i < userPathGoRewardList.length(); i++) {
                    jo = userPathGoRewardList.getJSONObject(i);
                    if (!"UNRECEIVED".equals(jo.getString("status")))
                        continue;
                    String userPathRewardId = jo.getString("userPathRewardId");
                    jo = new JSONObject(AntSportsRpcCall.rewardReceive(pathId, userPathRewardId));
                    if (jo.optBoolean("success")) {
                        jo = jo.getJSONObject("userPathRewardDetail");
                        JSONArray rightsRuleList = jo.getJSONArray("userPathRewardRightsList");
                        StringBuilder award = new StringBuilder();
                        for (int j = 0; j < rightsRuleList.length(); j++) {
                            jo = rightsRuleList.getJSONObject(j).getJSONObject("rightsContent");
                            award.append(jo.getString("name")).append("*").append(jo.getInt("count"));
                        }
                        Log.other(TAG, "文体宝箱🎁[" + award + "]");
                    } else {
                        Log.record(TAG, "文体中心开宝箱");
                        Log.runtime(jo.toString());
                    }
                }
            } else {
                Log.record(TAG, "文体中心开宝箱");
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathMapHomepage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathMapJoin(String title, String pathId) {
        try {
            JSONObject jo = new JSONObject(AntSportsRpcCall.pathMapJoin(pathId));
            if (jo.optBoolean("success")) {
                Log.other(TAG, "加入线路🚶🏻‍♂️[" + title + "]");
                pathFeatureQuery();
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathMapJoin err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void tiyubizGo(String countDate, String title, int goStepCount, String pathId,
                           String userPathRecordId) {
        try {
            String s = AntSportsRpcCall.tiyubizGo(countDate, goStepCount, pathId, userPathRecordId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("userPath");
                Log.other(TAG, "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("userPathRecordForwardStepCount") + "步");
                pathMapHomepage(pathId);
                boolean completed = "COMPLETED".equals(jo.getString("userPathRecordStatus"));
                if (completed) {
                    Log.other(TAG, "完成线路🚶🏻‍♂️[" + title + "]");
                    pathFeatureQuery();
                }
            } else {
                Log.runtime(TAG, s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "tiyubizGo err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 抢好友大战 */
    private void queryClubHome() {
        try {
            // 检查是否已达到0金币上限（实时检查）
            int maxCount = zeroCoinLimit.getValue();
            if (zeroTrainCoinCount >= maxCount) {
                String today = TimeUtil.getDateStr2();
                DataStore.INSTANCE.put(TRAIN_FRIEND_ZERO_COIN_DATE, today);
                Log.record(TAG, "✅ 训练好友获得0金币已达" + maxCount + "次上限，今日不再执行");
                return;
            }
            // 发送 RPC 请求获取 club home 数据
            JSONObject clubHomeData = new JSONObject(AntSportsRpcCall.queryClubHome());
            // 处理 mainRoom 中的 bubbleList
            processBubbleList(clubHomeData.optJSONObject("mainRoom"));
            // 处理 roomList 中的每个房间的 bubbleList
            JSONArray roomList = clubHomeData.optJSONArray("roomList");
            if (roomList != null) {
                for (int i = 0; i < roomList.length(); i++) {
                    JSONObject room = roomList.optJSONObject(i);
                    processBubbleList(room);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryClubHome err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 训练好友-收金币
    private void processBubbleList(JSONObject object) {
        if (object != null && object.has("bubbleList")) {
            try {
                JSONArray bubbleList = object.getJSONArray("bubbleList");
                for (int j = 0; j < bubbleList.length(); j++) {
                    JSONObject bubble = bubbleList.getJSONObject(j);
                    // 获取 bubbleId
                    String bubbleId = bubble.optString("bubbleId");
                    // 调用 collectBubble 方法
                    AntSportsRpcCall.collectBubble(bubbleId);
                    // 输出日志信息
                    int fullCoin = bubble.optInt("fullCoin");
                    Log.other(TAG, "训练好友💰️[获得:" + fullCoin + "金币]");

                    // 记录0金币情况
                    if (fullCoin == 0) {
                        zeroTrainCoinCount++;
                        // 获取用户设置的0金币上限次数
                        int maxCount = zeroCoinLimit.getValue();
                        // 如果0金币次数达到设置的上限，记录今天日期，今日不再执行
                        if (zeroTrainCoinCount >= maxCount) {
                            String today = TimeUtil.getDateStr2();
                            DataStore.INSTANCE.put(TRAIN_FRIEND_ZERO_COIN_DATE, today);
                            Log.record(TAG, "✅ 训练好友获得0金币已超过" + maxCount + "次，今日不再执行，明日自动恢复");
                            return; // 立即退出处理
                        } else {
                            // 显示当前计数情况
                            Log.record(TAG, "训练好友0金币次数: " + zeroTrainCoinCount + "/" + maxCount);
                        }
                    }

                    // 添加 1 秒的等待时间
                    GlobalThreadPools.sleep(1000);
                }
            } catch (Throwable t) {
                Log.runtime(TAG, "processBubbleList err:");
                Log.printStackTrace(TAG, t);
            }
        }
    }

    // 训练好友-训练操作
    // 流程：
    // 1. 查询 clubHome，找到第一个可以训练的好友（trainInfo.training = false）
    // 2. 调用 alipay.antsports.club.train.queryTrainItem 拿到 bizId 和 trainItemList
    // 3. 从 trainItemList 中随便选一个（这里选 production 最大的），调用 trainMember 进行训练
    private void queryTrainItem() {
        try {
            // 发送 RPC 请求获取 club home 数据
            JSONObject clubHomeData = new JSONObject(AntSportsRpcCall.queryClubHome());
            JSONArray roomList = clubHomeData.optJSONArray("roomList");
            if (roomList == null || roomList.length() == 0) {
                return;
            }

            // 找到第一个可训练的好友
            for (int i = 0; i < roomList.length(); i++) {
                JSONObject room = roomList.optJSONObject(i);
                if (room == null) continue;
                JSONArray memberList = room.optJSONArray("memberList");
                if (memberList == null || memberList.length() == 0) continue;

                for (int j = 0; j < memberList.length(); j++) {
                    JSONObject member = memberList.optJSONObject(j);
                    if (member == null) continue;

                    JSONObject trainInfo = member.optJSONObject("trainInfo");
                    // 只有当前未在训练中的好友才需要发起训练
                    if (trainInfo == null || trainInfo.optBoolean("training", false)) {
                        continue;
                    }

                    String memberId = member.optString("memberId");
                    String originBossId = member.optString("originBossId");
                    String userName = UserMap.getMaskName(originBossId);

                    // 查询训练项目列表
                    String responseData = AntSportsRpcCall.queryTrainItem();
                    JSONObject responseJson = new JSONObject(responseData);
                    if (!responseJson.optBoolean("success")) {
                        Log.runtime(TAG, "queryTrainItem rpc failed: " + responseJson.optString("resultDesc"));
                        return;
                    }

                    // bizId 从响应顶层获取
                    String bizId = responseJson.optString("bizId", "");
                    if (bizId.isEmpty() && responseJson.has("taskDetail")) {
                        bizId = responseJson.getJSONObject("taskDetail").optString("taskId", "");
                    }

                    JSONArray trainItemList = responseJson.optJSONArray("trainItemList");
                    if (bizId.isEmpty() || trainItemList == null || trainItemList.length() == 0) {
                        Log.runtime(TAG, "queryTrainItem response missing bizId or trainItemList");
                        return;
                    }

                    // 这里随便选一个，这里选 production 最大的训练方式
                    JSONObject bestItem = null;
                    int bestProduction = -1;
                    for (int k = 0; k < trainItemList.length(); k++) {
                        JSONObject item = trainItemList.optJSONObject(k);
                        if (item == null) continue;
                        int production = item.optInt("production", 0);
                        if (production > bestProduction) {
                            bestProduction = production;
                            bestItem = item;
                        }
                    }

                    if (bestItem == null) {
                        return;
                    }

                    String itemType = bestItem.optString("itemType");
                    String trainItemName = bestItem.optString("name");

                    String trainMemberResponse = AntSportsRpcCall.trainMember(bizId, itemType, memberId, originBossId);
                    JSONObject trainMemberResponseJson = new JSONObject(trainMemberResponse);
                    if (!trainMemberResponseJson.optBoolean("success")) {
                        Log.runtime(TAG, "trainMember request failed: " + trainMemberResponseJson.optString("resultDesc"));
                        return;
                    }

                    Log.other(TAG, "训练好友🥋[训练:" + userName + " " + trainItemName + "]");
                    GlobalThreadPools.sleep(1000);
                    return; // 只训练一个好友，逻辑足够
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTrainItem err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 抢好友大战-抢购好友
    // 流程：
    // 1. 查询 clubHome 拿到当前余额 coinBalance 和房间列表
    // 2. 在空房间上，根据余额调用 queryMemberPriceRanking，拿到可买的好友列表
    // 3. 过滤出 originBossId 符合配置的好友，调用 queryClubMember → buyMember 完成抢购
    private void buyMember() {
        try {
            // 发送 RPC 请求获取 club home 数据
            String clubHomeResponse = AntSportsRpcCall.queryClubHome();
            GlobalThreadPools.sleep(500);
            JSONObject clubHomeJson = new JSONObject(clubHomeResponse);
            // 判断 clubAuth 字段是否为 "ENABLE"
            if (!"ENABLE".equals(clubHomeJson.optString("clubAuth"))) {

                Log.record(TAG, "抢好友大战🧑‍🤝‍🧑未授权开启");
                return;
            }

            JSONObject assetsInfo = clubHomeJson.optJSONObject("assetsInfo");
            if (assetsInfo == null) {
                return;
            }
            // 看我.txt：assetsInfo.energyBalance 是当前的能量值
            int coinBalance = assetsInfo.optInt("energyBalance", 0);
            if (coinBalance <= 0) {
                Log.record(TAG, "抢好友大战🧑‍🤝‍🧑当前能量为0，跳过抢好友");
                return;
            }

            JSONArray roomList = clubHomeJson.optJSONArray("roomList");
            if (roomList == null || roomList.length() == 0) {
                return;
            }

            for (int i = 0; i < roomList.length(); i++) {
                JSONObject room = roomList.optJSONObject(i);
                if (room == null) continue;

                JSONArray memberList = room.optJSONArray("memberList");
                // 只在空房间下手
                if (memberList != null && memberList.length() > 0) {
                    continue;
                }

                String roomId = room.optString("roomId");
                if (roomId.isEmpty()) continue;

                // 根据余额拉一批可抢好友
                String memberPriceResult = AntSportsRpcCall.queryMemberPriceRanking(coinBalance);
                GlobalThreadPools.sleep(500);
                JSONObject memberPriceJson = new JSONObject(memberPriceResult);
                if (!memberPriceJson.optBoolean("success", true)) {
                    Log.runtime(TAG, "queryMemberPriceRanking err: " + memberPriceJson.optString("resultDesc"));
                    continue;
                }

                JSONArray memberDetailList = memberPriceJson.optJSONArray("memberDetailList");
                if (memberDetailList == null || memberDetailList.length() == 0) {
                    Log.record(TAG, "抢好友大战🧑‍🤝‍🧑暂无可抢好友");
                    continue;
                }

                // 遍历候选好友
                for (int j = 0; j < memberDetailList.length(); j++) {
                    JSONObject detail = memberDetailList.optJSONObject(j);
                    if (detail == null) continue;

                    JSONObject memberModel = detail.optJSONObject("memberModel");
                    if (memberModel == null) continue;

                    String originBossId = memberModel.optString("originBossId");
                    String memberIdFromRank = memberModel.optString("memberId");
                    if (originBossId.isEmpty() || memberIdFromRank.isEmpty()) continue;

                    // 检查 originBossId 是否在配置的列表中
                    boolean isBattleForFriend = originBossIdList.getValue().contains(originBossId);
                    if (battleForFriendType.getValue() == BattleForFriendType.DONT_ROB) {
                        isBattleForFriend = !isBattleForFriend;
                    }
                    if (!isBattleForFriend) {
                        continue;
                    }

                    // 价格判断：price <= coinBalance 才抢
                    JSONObject priceInfoObj = memberModel.optJSONObject("priceInfo");
                    if (priceInfoObj == null) continue;
                    int price = priceInfoObj.optInt("price", Integer.MAX_VALUE);
                    if (price > coinBalance) {
                        continue;
                    }

                    // 查询玩家详情，拿到 currentBossId / memberId / priceInfo
                    String clubMemberResult = AntSportsRpcCall.queryClubMember(memberIdFromRank, originBossId);
                    GlobalThreadPools.sleep(500);
                    JSONObject clubMemberDetailJson = new JSONObject(clubMemberResult);
                    if (!clubMemberDetailJson.optBoolean("success", true) || !clubMemberDetailJson.has("member")) {
                        continue;
                    }

                    JSONObject memberObj = clubMemberDetailJson.getJSONObject("member");
                    String currentBossId = memberObj.optString("currentBossId");
                    String memberId = memberObj.optString("memberId");
                    JSONObject priceInfoFull = memberObj.optJSONObject("priceInfo");
                    if (currentBossId.isEmpty() || memberId.isEmpty() || priceInfoFull == null) {
                        continue;
                    }

                    String priceInfoStr = priceInfoFull.toString();

                    String buyMemberResult = AntSportsRpcCall.buyMember(currentBossId, memberId, originBossId, priceInfoStr, roomId);
                    GlobalThreadPools.sleep(500);
                    JSONObject buyMemberResponse = new JSONObject(buyMemberResult);

                    if (ResChecker.checkRes(TAG, buyMemberResponse)) {
                        String userName = UserMap.getMaskName(originBossId);
                        Log.other(TAG, "抢购好友🥋[成功:将 " + userName + " 抢回来]");
                        // 抢好友成功后，如果训练好友功能开启，则执行训练
                        if (trainFriend.getValue()) {
                            queryTrainItem();
                        }
                        return; // 抢到一个就够了
                    } else if ("CLUB_AMOUNT_NOT_ENOUGH".equals(buyMemberResponse.optString("resultCode"))) {
                        Log.record(TAG, "[能量🎈不足，无法完成抢购好友！]");
                        return;
                    } else if ("CLUB_MEMBER_TRADE_PROTECT".equals(buyMemberResponse.optString("resultCode"))) {
                        Log.record(TAG, "[暂时无法抢购好友，给Ta一段独处的时间吧！]");
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "buyMember err:");
            Log.printStackTrace(TAG, t);
        }
    }


    /**
     * 健康岛任务处理器
     * 整体流程（与 coinExchangeItem 风格保持一致）：
     * 1. 签到（querySign + takeSign）
     * 2. 任务大厅循环处理（queryTaskCenter + taskSend / adtask.finish）→ 新增循环重试+失败限制
     * 3. 捡泡泡（queryBubbleTask + pickBubbleTaskEnergy）
     * 优化点：
     * ✔ 任务完成后自动重新获取任务列表，直到无待完成任务
     * ✔ 失败次数限制（优先取 BaseModel.getSetMaxErrorCount()，默认5次）
     * ✔ 每次循环间隔短延时（避免接口QPS过高）
     * ✔ 保留原有所有校验逻辑和日志风格
     */
    public class NeverlandTaskHandler {

        private static final String TAG = "Neverland";
        // 失败次数限制（优先从 BaseModel 获取，无则默认5次）
        private static final int MAX_ERROR_COUNT = BaseModel.getSetMaxErrorCount().getValue() > 0
                ? BaseModel.getSetMaxErrorCount().getValue()
                : 5;
        // 循环间隔延时（ms）- 避免接口调用过频繁
        private static final long TASK_LOOP_DELAY = 1000;

        /**
         * 健康岛任务入口
         */
        public void runNeverland() {
            try {
                Log.record(TAG, "开始执行健康岛任务");

                if(neverlandTask.getValue())
                {
                    // 固定顺序：1.签到 → 2.循环处理任务大厅 → 3.捡泡泡
                    neverlandDoSign();                 // 签到
                    loopHandleTaskCenter();            // 循环处理任务
                   // handleHealthIslandTask();            // 循环处理任务中心的浏览任务
                    neverlandPickAllBubble();          // 拾取能量球
                }

                if(neverlandTask.getValue())
                {
                    neverlandAutoTask();               //执行健康岛建造
                }
                Log.record(TAG, "健康岛任务结束");
            } catch (Throwable t) {
                Log.error(TAG, "runNeverland err:");
                Log.printStackTrace(TAG, t);
            }
        }

        // -------------------------------------------------------------------------
        // 1. 健康岛签到（无变更）
        // -------------------------------------------------------------------------

        private void neverlandDoSign() {
            try {
                Log.record(TAG, "健康岛 · 检查签到状态");

                JSONObject jo = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.querySign(3, "jkdsportcard"));

                if (!ResChecker.checkRes(TAG + "查询签到失败:", jo)
                        || !jo.optBoolean("success", false)
                        || jo.optJSONObject("data") == null) {
                    Log.error(TAG, "querySign raw=" + jo);
                    return;
                }

                JSONObject data = jo.getJSONObject("data");
                JSONObject signInfo = data.optJSONObject("continuousSignInfo");

                if (signInfo != null && signInfo.optBoolean("signedToday", false)) {
                    Log.record(TAG, "今日已签到 ✔ 连续：" + signInfo.optInt("continuitySignedDayCount") + " 天");
                    return;
                }

                Log.record(TAG, "健康岛 · 正在签到…");
                JSONObject signRes = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.takeSign(3, "jkdsportcard"));

                if (!ResChecker.checkRes(TAG + "签到失败:", signRes)
                        || !signRes.optBoolean("success", false)
                        || signRes.optJSONObject("data") == null) {
                    Log.error(TAG, "takeSign raw=" + signRes);
                    return;
                }

                JSONObject signData = signRes.getJSONObject("data");
                JSONObject reward = signData.optJSONObject("continuousDoSignInVO");
                int rewardAmount = reward != null ? reward.optInt("rewardAmount", 0) : 0;
                String rewardType = reward != null ? reward.optString("rewardType", "") : "";
                JSONObject signInfoAfter = signData.optJSONObject("continuousSignInfo");
                int newContinuity = signInfoAfter != null ? signInfoAfter.optInt("continuitySignedDayCount", -1) : -1;

                Log.other(TAG, "健康岛签到成功 🎉 +" + rewardAmount + rewardType
                        + " 连续：" + newContinuity + " 天");

            } catch (Throwable t) {
                Log.error(TAG, "neverlandDoSign err:"+t.toString());
                Log.printStackTrace(TAG, t);
            }
        }

        // -------------------------------------------------------------------------
        // 2. 新增：循环处理任务大厅（核心优化）
        // -------------------------------------------------------------------------

        /**
         * 循环处理任务大厅：完成一批任务后重新获取列表，直到无待完成任务或达到失败次数限制
         * 只处理 PROMOKERNEL_TASK 和 LIGHT_TASK
         */
        private void loopHandleTaskCenter() {
            int errorCount = 0; // 累计失败次数
            int emptyTaskCount = 0; // 连续获取到空待完成任务的次数（连续2次则退出）

            Log.record(TAG, "开始循环处理任务大厅（失败限制：" + MAX_ERROR_COUNT + "次）");

            while (true) {
                try {
                    // 1. 检查失败次数是否超限
                    if (errorCount >= MAX_ERROR_COUNT) {
                        Log.error(TAG, "任务处理失败次数达到上限（" + MAX_ERROR_COUNT + "次），停止循环");
                        break;
                    }

                    // 2. 获取最新任务列表
                    JSONObject taskCenterResp = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryTaskCenter());
                    if (!ResChecker.checkRes(TAG + "获取任务列表失败:", taskCenterResp)
                            || !taskCenterResp.optBoolean("success", false)
                            || taskCenterResp.optJSONObject("data") == null) {
                        Log.error(TAG, "queryTaskCenter raw=" + taskCenterResp);
                        errorCount++;
                        Log.record(TAG, "获取任务列表失败，累计失败次数：" + errorCount);
                        Thread.sleep(TASK_LOOP_DELAY); // 失败后延时重试
                        continue;
                    }

                    JSONArray taskList = taskCenterResp.getJSONObject("data").optJSONArray("taskCenterTaskVOS");
                    if (taskList == null || taskList.length() == 0) {
                        Log.other(TAG, "任务中心为空，无任务可处理");
                        break;
                    }

                    // 3. 筛选出待完成的任务，只保留 PROMOKERNEL_TASK 和 LIGHT_TASK
                    List<JSONObject> pendingTasks = filterPendingTasks(taskList).stream()
                            .filter(task -> {
                                String type = task.optString("taskType", "");
                                return "PROMOKERNEL_TASK".equals(type) || "LIGHT_TASK".equals(type);
                            })
                            .toList();

                    // 4. 如果本次获取到的任务中没有可处理任务，则认为后续也无法执行，直接退出
                    if (pendingTasks.isEmpty()) {
                        Log.record(TAG, "本次获取到的任务中没有可处理的 PROMOKERNEL_TASK 或 LIGHT_TASK，停止循环");
                        break;
                    }

                    // 重置连续空任务计数（有可处理任务）
                    emptyTaskCount = 0;
                    Log.other(TAG, "本次获取到 " + pendingTasks.size() + " 个待完成任务，开始处理");

                    // 5. 处理当前批次的待完成任务
                    int currentBatchError = 0;
                    for (JSONObject task : pendingTasks) {
                        boolean handleSuccess = handleSingleTask(task);
                        if (!handleSuccess) {
                            currentBatchError++;
                        }
                    }

                    // 6. 统计当前批次失败情况
                    if (currentBatchError > 0) {
                        errorCount += currentBatchError;
                        Log.error(TAG, "本次批次处理失败 " + currentBatchError + " 个任务，累计失败次数：" + errorCount);
                    } else {
                        Log.other(TAG, "本次批次任务全部处理成功");
                    }

                    // 7. 任务批次处理完成，延时后重新获取列表
                    Log.record(TAG, "当前批次任务处理完毕，" + TASK_LOOP_DELAY + "ms后重新获取任务列表");
                    Thread.sleep(TASK_LOOP_DELAY);

                } catch (InterruptedException e) {
                    Log.printStackTrace(TAG, "任务循环被中断", e);
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    break;
                } catch (Throwable t) {
                    errorCount++;
                    Log.printStackTrace(TAG, "任务循环处理异常，累计失败次数：" + errorCount, t);
                    try {
                        Thread.sleep(TASK_LOOP_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            Log.record(TAG, "任务大厅循环处理结束");
        }

        /**
         * 处理健康岛浏览任务
         */
        private void handleHealthIslandTask() {
            try {
                Log.record(TAG, "开始检查健康岛浏览任务");

                // 1. 查询健康岛任务信息
                JSONObject taskInfoResp = new JSONObject(
                        AntSportsRpcCall.NeverlandRpcCall.queryTaskInfo("health-island", "LIGHT_FEEDS_TASK")
                );

                if (!ResChecker.checkRes(TAG + "查询健康岛浏览任任务失败:", taskInfoResp)
                        || !taskInfoResp.optBoolean("success", false)
                        || taskInfoResp.optJSONObject("data") == null) {


                    Log.other(TAG, "健康岛浏览任务查询失败 ["+taskInfoResp+"] 请关闭此功能"+taskInfoResp);
                    return;
                }

                JSONArray taskInfos = taskInfoResp.getJSONObject("data").optJSONArray("taskInfos");
                if (taskInfos == null || taskInfos.length() == 0) {
                    Log.other(TAG, "健康岛任务列表为空");
                    return;
                }

                // 2. 遍历处理每个任务
                for (int i = 0; i < taskInfos.length(); i++) {
                    JSONObject taskInfo = taskInfos.getJSONObject(i);
                    String encryptValue = taskInfo.optString("encryptValue");
                    int energyNum = taskInfo.optInt("energyNum", 0);
                    int viewSec = taskInfo.optInt("viewSec", 15);

                    if (encryptValue.isEmpty()) {
                        Log.error(TAG, "健康岛任务 encryptValue 为空，跳过");
                        continue;
                    }

                    Log.record(TAG, "健康岛浏览任务：能量+" + energyNum + "，需等待" + viewSec + "秒");

                    // 3. 等待浏览时间
                    Thread.sleep(viewSec * 1000L);

                    // 4. 领取奖励
                    JSONObject receiveResp = new JSONObject(
                            AntSportsRpcCall.NeverlandRpcCall.energyReceive(encryptValue, energyNum, "LIGHT_FEEDS_TASK")
                    );

                    if (ResChecker.checkRes(TAG + "领取健康岛任务奖励:", receiveResp)
                            && receiveResp.optBoolean("success", false)) {
                        Log.other(TAG, "✅ 健康岛任务完成，获得能量+" + energyNum);
                    } else {
                        Log.error(TAG, "健康岛任务领取失败: " + receiveResp);
                    }

                    Thread.sleep(1000); // 任务间隔
                }

            } catch (Throwable t) {
                Log.printStackTrace(TAG, "处理健康岛任务异常", t);
            }
        }


        /**
         * 筛选待完成的任务（状态为 SIGNUP_COMPLETE）
         */
        private List<JSONObject> filterPendingTasks(JSONArray taskList) {
            List<JSONObject> pendingTasks = new ArrayList<>();
            try {
                for (int i = 0; i < taskList.length(); i++) {
                    JSONObject task = taskList.getJSONObject(i);
                    if ("SIGNUP_COMPLETE".equals(task.optString("taskStatus"))) {
                        pendingTasks.add(task);
                    }
                }
            } catch (Exception e) {
                Log.printStackTrace(TAG, "筛选待完成任务失败", e);
            }
            return pendingTasks;
        }

        /**
         * 处理单个任务（提取原 doNeverlandTasks 核心逻辑）
         *
         * @return true：处理成功；false：处理失败
         */
        private boolean handleSingleTask(JSONObject task) {
            try {
                String title = task.optString("title", "未知任务");
                String type = task.optString("taskType", "");
                String jumpLink = task.optString("jumpLink", "");

                Log.record(TAG, "开始处理任务：" + title + "  类型=" + type);

                // 按任务类型处理
                switch (type) {
                    case "PROMOKERNEL_TASK":
                        return handlePromoKernelTask(task, title);
                    case "LIGHT_TASK":
                        return handleLightTask(task, title, jumpLink);
                    case "GAME_TASK":
                        Log.record(TAG, "跳过 GAME_TASK：" + title);
                        return true; // 跳过不算失败
                    default:
                        Log.error(TAG, "未处理的任务类型：" + type + " 任务名：" + title);
                        return false; // 未知类型算失败
                }
            } catch (Exception e) {
                Log.printStackTrace(TAG, "处理单个任务失败（任务名：" + task.optString("title") + "）", e);
                return false;
            }
        }

        /**
         * 处理 PROMOKERNEL_TASK（活动类任务）
         */
        private boolean handlePromoKernelTask(JSONObject task, String title) {
            try {
                // 补充必填参数 scene
                task.put("scene", "MED_TASK_HALL");
                JSONObject res = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.taskSend(task));

                if (res.optBoolean("success", false)) {
                    Log.other(TAG, "✔ 活动任务完成：" + title);
                    return true;
                } else {
                    Log.error(TAG, "taskSend 失败: " + res);
                    return false;
                }
            } catch (Exception e) {
                Log.printStackTrace(TAG, "处理 PROMOKERNEL_TASK 异常（" + title + "）", e);
                return false;
            }
        }

        /**
         * 处理 LIGHT_TASK（浏览类任务）
         */
        private boolean handleLightTask(JSONObject task, String title, String jumpLink) {
            try {
                String bizId = extractBizIdFromJumpLink(jumpLink);
                if (bizId == null || bizId.isEmpty()) {
                    Log.error(TAG, "LIGHT_TASK 未找到 bizId：" + title + " jumpLink=" + jumpLink);
                    return false;
                }

                JSONObject res = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.finish(bizId));
                if (res.optBoolean("success", false) || "0".equals(res.optString("errCode", ""))) {
                    Log.other(TAG, "✔ 浏览任务完成：" + title);
                    return true;
                } else {
                    Log.error(TAG, "完成 LIGHT_TASK 失败: " + res);
                    return false;
                }
            } catch (Exception e) {
                Log.printStackTrace(TAG, "处理 LIGHT_TASK 异常（" + title + "）", e);
                return false;
            }
        }

        // -------------------------------------------------------------------------
        // 3. 捡泡泡（无变更，仅调整执行时机）
        // -------------------------------------------------------------------------

        private void neverlandPickAllBubble() {
            try {
                Log.record(TAG, "健康岛 · 检查可领取泡泡");

                JSONObject jo = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryBubbleTask());

                if (!ResChecker.checkRes(TAG + "查询泡泡失败:", jo)
                        || !jo.optBoolean("success", false)
                        || jo.optJSONObject("data") == null) {
                    Log.error(TAG, "queryBubbleTask raw=" + jo);
                    return;
                }

                JSONArray arr = jo.getJSONObject("data").optJSONArray("bubbleTaskVOS");
                if (arr == null || arr.length() == 0) {
                    Log.other(TAG, "无泡泡可领取");
                    return;
                }

                List<String> ids = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    if (!item.optBoolean("initState") &&
                            item.optString("medEnergyBallInfoRecordId").length() > 0) {
                        ids.add(item.getString("medEnergyBallInfoRecordId"));
                    }
                }

                if (ids.isEmpty()) {
                    Log.record(TAG, "没有可领取的泡泡");
                    return;
                }

                Log.record(TAG, "健康岛 · 正在领取 " + ids.size() + " 个泡泡…");
                JSONObject pick = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.pickBubbleTaskEnergy(ids));

                if (!ResChecker.checkRes(TAG + "领取泡泡失败:", pick)
                        || !pick.optBoolean("success", false)
                        || pick.optJSONObject("data") == null) {
                    Log.error(TAG, "pickBubbleTaskEnergy raw=" + pick);
                    return;
                }

                JSONObject data = pick.getJSONObject("data");
                Log.other(TAG, "捡泡泡成功 🎈 +" +
                        data.optString("changeAmount") +
                        " 余额：" + data.optString("balance"));

            } catch (Throwable t) {
                Log.printStackTrace(TAG, "neverlandPickAllBubble err:", t);
            }
        }

        // -------------------------------------------------------------------------
        // 4. 自动走路任务处理
        // -------------------------------------------------------------------------
        // =========================================================================
        // 步数上限检查 - 公共方法
        // =========================================================================

        /**
         * 检查今日步数是否达到上限
         * @return 剩余可走步数,如果返回 0 或负数表示已达上限
         */
        private int checkDailyStepLimit() {
            Integer stepCount = Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEPCOUNT);
            if (stepCount == null) {
                stepCount = 0;
            }
            int maxStepLimit = neverlandGridStepCount.getValue();
            int remainSteps = maxStepLimit - stepCount;

            Log.record(TAG, String.format("今日步数统计: 已走 %d/%d 步, 剩余 %d 步",
                    stepCount, maxStepLimit, Math.max(0, remainSteps)));

            return remainSteps;
        }

        /**
         * 记录步数增加
         * @param addedSteps 本次增加的步数
         * @return 更新后的总步数
         */
        private int recordStepIncrease(int addedSteps) {
            if (addedSteps <= 0) {
                return Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEPCOUNT);
            }

            Integer currentSteps = Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEPCOUNT);
            if (currentSteps == null) {
                currentSteps = 0;
            }

            int newSteps = currentSteps + addedSteps;
            Status.setIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEPCOUNT, newSteps);

            int maxLimit = neverlandGridStepCount.getValue();
            Log.record(TAG, String.format("步数增加: +%d 步, 当前总计 %d/%d 步",
                    addedSteps, newSteps, maxLimit));

            return newSteps;
        }

        // =========================================================================
        // 健康岛自动任务
        // =========================================================================

        /**
         * 健康岛走路建造任务入口
         *
         * <p>功能说明:</p>
         * <ul>
         *   <li>自动检测游戏模式(新游戏建造模式 or 旧版行走模式)</li>
         *   <li>检查每日步数上限和能量余额</li>
         *   <li>根据模式自动执行对应任务</li>
         * </ul>
         *
         * <p>执行流程:</p>
         * <ol>
         *   <li>查询基础信息,判断游戏模式</li>
         *   <li>检查每日步数限额</li>
         *   <li>检查能量余额</li>
         *   <li>分发到对应的任务处理函数</li>
         * </ol>
         * 
         * @throws Exception 网络请求或数据解析异常
         */
        private void neverlandAutoTask() {
            try {
                Log.record(TAG, "健康岛 · 启动走路建造任务");

                // / ========== 1. 查询基础信息 ==========
                JSONObject baseInfo = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryBaseinfo());
                if (!ResChecker.checkRes(TAG + " 查询基础信息失败:", baseInfo)
                        || !baseInfo.optBoolean("success", false)
                        || baseInfo.optJSONObject("data") == null) {
                    Log.error(TAG, "queryBaseinfo 失败, 响应数据: =" + baseInfo);
                    return;
                }
                JSONObject baseData = baseInfo.getJSONObject("data");
                boolean isNewGame = baseData.optBoolean("newGame", false);
                String branchId = baseData.optString("branchId", "MASTER");
                String mapId = baseData.optString("mapId", "");
                String mapName = baseData.optString("mapName", "未知地图");

                Log.record(TAG, String.format("当前地图: [%s](%s) | 模式: %s",
                        mapName, mapId, isNewGame ? "新游戏建造" : "旧版行走"));

                // ========== 2. 检查每日步数上限 ==========
                int remainSteps = checkDailyStepLimit();
                if (remainSteps <= 0) {
                    Log.record(TAG, "今日步数已达上限, 任务结束");
                    return;
                }

                // ========== 3. 查询剩余能量 ==========
                int leftEnergy = queryUserEnergy();
                if (leftEnergy < 5) {
                    Log.record(TAG, "剩余能量不足(< 5), 无法执行任务");
                    return;
                }

                // ========== 4. 根据模式分发任务 ==========
                if (isNewGame) {
                    executeAutoBuild(branchId, mapId, remainSteps, leftEnergy);
                } else {
                    executeAutoWalk(branchId, remainSteps, leftEnergy);
                }

                 Log.record(TAG, "健康岛自动走路建造执行完成 ✓");

            } catch (Throwable t) {
                Log.error(TAG, "neverlandAutoTask 发生异常");
                Log.printStackTrace(TAG, t);
            }
        }

        // =========================================================================
        // 辅助函数
        // =========================================================================

        /**
         * 查询用户剩余能量
         *
         * @return 剩余能量值,查询失败返回 0
         */
        private int queryUserEnergy() {
            try {
                JSONObject energyResp = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryUserEnergy());
                if (!ResChecker.checkRes(TAG + " 查询用户能量失败:", energyResp)
                        || !energyResp.optBoolean("success", false)
                        || energyResp.optJSONObject("data") == null) {
                    Log.error(TAG, "queryUserEnergy 失败, 响应数据: " + energyResp);
                    return 0;
                }

                int balance = energyResp.getJSONObject("data").optInt("balance", 0);
                Log.other(TAG, "当前剩余能量: " + balance);
                return balance;

            } catch (Throwable t) {
                Log.error(TAG, "queryUserEnergy 发生异常");
                Log.printStackTrace(TAG, t);
                return 0;
            }
        }

        // =========================================================================
        // 旧版行走模式
        // =========================================================================

        /**
         * 执行自动行走任务(旧版模式)
         *
         * <p>功能说明:</p>
         * <ul>
         *   <li>获取当前地图列表</li>
         *   <li>查找 DOING 状态的地图,若无则选择 LOCKED 地图</li>
         *   <li>循环执行 walkGrid 直到能量或步数耗尽</li>
         * </ul>
         *
         * @param baseBranchId 基础分支 ID
         * @param remainSteps 剩余可用步数
         * @param leftEnergy 剩余能量
         */
        private void executeAutoWalk(String baseBranchId, int remainSteps, int leftEnergy) {
            try {
                Log.other(TAG, "开始执行旧版行走任务");

                // ========== 1. 获取地图列表 ==========
                JSONObject mapResp = new JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryMapList());
                if (!ResChecker.checkRes(TAG + " 查询地图失败:", mapResp)
                        || !mapResp.optBoolean("success", false)
                        || mapResp.optJSONObject("data") == null) {
                    Log.error(TAG, "queryMapList 失败, 响应数据: =" + mapResp);
                    return;
                }

                JSONArray mapList = mapResp.getJSONObject("data").optJSONArray("mapList");
                if (mapList == null || mapList.length() == 0) {
                    Log.error(TAG, "地图列表为空, 无法继续");
                    return;
                }

                // ========== 2. 查找或选择地图 ==========
                JSONObject currentMap = findOrChooseMap(mapList);
                if (currentMap == null) {
                    Log.error(TAG, "无可用地图, 任务终止");
                    return;
                }

                String branchId = currentMap.optString("branchId", baseBranchId);
                String mapId = currentMap.optString("mapId", "");
                Log.other(TAG, "使用地图 ID: " + mapId);

                // ========== 3. 自动走路循环 ==========
                boolean retriedMapNotCurrent = false;

                for (int i = 0; i < remainSteps; i++) {
                    if (leftEnergy < 5) {
                        Log.other(TAG, "能量不足(< 5), 停止走路任务");
                        break;
                    }

                    JSONObject walkResp = new JSONObject(
                            AntSportsRpcCall.NeverlandRpcCall.walkGrid(branchId, mapId, false));

                    if (!ResChecker.checkRes(TAG + " walkGrid 失败:", walkResp)
                            || !walkResp.optBoolean("success", false)
                            || walkResp.optJSONObject("data") == null) {
                        
                        String errorCode = walkResp.optString("errorCode", "");
                        
                        // 修复了这里的逻辑结构和括号问题
                        if ("MAP_NOT_CURRENT".equals(errorCode) && !retriedMapNotCurrent) {
                            // 当前 mapId 在服务端视角不是“当前地图”，尝试自动切一次
                            if (tryChooseMap(branchId, mapId)) {
                                retriedMapNotCurrent = true;
                                i--; // 重试这一步
                                Thread.sleep(300);
                                continue; // 跳过后续报错和break，继续循环
                            }
                        }

                        Log.error(TAG, String.format("walkGrid 失败, 错误码: %s, 响应数据: %s",
                                errorCode, walkResp));
                        break;
                    }

                    // ========== 4. 处理走路结果 ==========
                    JSONObject walkData = walkResp.getJSONObject("data");
                    leftEnergy = walkData.optInt("leftCount", leftEnergy);

                    int stepIncrease = extractStepIncrease(walkData);
                    int totalSteps = recordStepIncrease(stepIncrease);
                    JSONObject starData = walkData.optJSONObject("starData");
                    int currStar = starData != null ? starData.optInt("curr", 0) : 0;

                    Log.other(TAG, String.format("走路进度 🎉 能量: %d | 本次: +%d | 今日: %d/%d | 星星: %d",
                            leftEnergy, stepIncrease, totalSteps, neverlandGridStepCount.getValue(), currStar));

                    Thread.sleep(888); // 每次走路间隔888ms
                }

                Log.other(TAG, "自动走路任务完成 ✓");

            } catch (Throwable t) {
                Log.error(TAG, "executeAutoWalk 发生异常");
                Log.printStackTrace(TAG, t);
            }
        }

        /**
         * 查找 DOING 地图或随机选择 LOCKED 地图
         *
         * @param mapList 地图列表
         * @return 选中的地图对象,失败返回 null
         */
        private JSONObject findOrChooseMap(JSONArray mapList) {
            try {
                JSONObject currentMap = null;
                List<JSONObject> lockedMaps = new ArrayList<>();

                for (int i = 0; i < mapList.length(); i++) {
                    JSONObject map = mapList.getJSONObject(i);
                    String status = map.optString("status", "");
                    if ("DOING".equals(status)) {
                        currentMap = map;
                        break;
                    } else if ("LOCKED".equals(status)) {
                        lockedMaps.add(map);
                    }
                }

                if (currentMap == null && !lockedMaps.isEmpty()) {
                    int idx = new Random().nextInt(lockedMaps.size());
                    currentMap = lockedMaps.get(idx);
                    String branchId = currentMap.optString("branchId", "");
                    String mapId = currentMap.optString("mapId", "");

                    Log.other(TAG, String.format("未找到 DOING 地图, 随机选择 LOCKED 地图: %s", mapId));

                    if (!tryChooseMap(branchId, mapId)) {
                        return null;
                    }
                }

                return currentMap;

            } catch (Throwable t) {
                Log.error(TAG, "findOrChooseMap 发生异常");
                Log.printStackTrace(TAG, t);
                return null;
            }
        }

        /**
         * 尝试切换到指定地图
         *
         * @param branchId 分支 ID
         * @param mapId 地图 ID
         * @return 切换成功返回 true
         */
        private boolean tryChooseMap(String branchId, String mapId) {
            try {
                Log.other(TAG, "尝试切换地图: " + mapId);
                JSONObject chooseResp = new JSONObject(
                        AntSportsRpcCall.NeverlandRpcCall.chooseMap(branchId, mapId));

                if (chooseResp.optBoolean("success", false)) {
                    Log.record(TAG, "成功切换到地图: " + mapId);
                    return true;
                } else {
                    Log.error(TAG, "切换地图失败: " + chooseResp);
                    return false;
                }
            } catch (Throwable t) {
                Log.error(TAG, "tryChooseMap 发生异常");
                Log.printStackTrace(TAG, t);
                return false;
            }
        }

        /**
         * 从 walkData 中提取步数增量
         *
         * @param walkData 走路响应数据
         * @return 步数增量
         */
        private int extractStepIncrease(JSONObject walkData) {
            try {
                JSONArray mapAwards = walkData.optJSONArray("mapAwards");
                if (mapAwards != null && mapAwards.length() > 0) {
                    return mapAwards.getJSONObject(0).optInt("step", 0);
                }
            } catch (Throwable t) {
                Log.printStackTrace(TAG, t);
            }
            return 0;
        }

// =========================================================================
// 新游戏建造模式
// =========================================================================

        /**
         * 执行自动建造任务(新游戏模式)
         *
         * <p>功能说明:</p>
         * <ul>
         *   <li>根据剩余步数和能量计算建造倍数</li>
         *   <li>循环执行 build 直到能量或步数耗尽</li>
         *   <li>实时记录建造进度和奖励</li>
         * </ul>
         *
         * @param branchId 分支 ID
         * @param mapId 地图 ID
         * @param remainSteps 剩余可用步数
         * @param leftEnergy 剩余能量
         */
        private void executeAutoBuild(String branchId, String mapId, int remainSteps, int leftEnergy) {
            try {
                Log.other(TAG, String.format("开始执行建造任务, 地图: %s", mapId));

                while (remainSteps > 0 && leftEnergy >= 5) {
                    // 计算本次建造倍数 (1-10 倍)
                    int maxMulti = Math.min(10, remainSteps);
                    int energyBasedMulti = leftEnergy / 5;
                    int multiNum = Math.min(maxMulti, energyBasedMulti);

                    if (multiNum <= 0) {
                        Log.other(TAG, "能量不足或步数已达上限, 停止建造");
                        break;
                    }

                    JSONObject buildResp = new JSONObject(
                            AntSportsRpcCall.NeverlandRpcCall.build(branchId, mapId, multiNum));

                    if (!ResChecker.checkRes(TAG + " build 失败:", buildResp)
                            || !buildResp.optBoolean("success", false)) {
                        Log.error(TAG, String.format("build 失败, multiNum=%d, 响应: %s",
                                multiNum, buildResp));
                        break;
                    }

                    JSONObject buildData = buildResp.optJSONObject("data");
                    if (buildData == null) {
                        Log.error(TAG, "build 响应数据为空: " + buildResp);
                        break;
                    }

                    // 更新能量
                    int newLeftEnergy = buildData.optInt("leftCount", -1);
                    if (newLeftEnergy >= 0) {
                        leftEnergy = newLeftEnergy;
                    }

                    // 计算实际步数
                    int stepIncrease = calculateBuildSteps(buildData, multiNum);
                    int totalSteps = recordStepIncrease(stepIncrease);
                    remainSteps -= stepIncrease;

                    // 获取奖励信息
                    String awardInfo = extractAwardInfo(buildData);

                    Log.other(TAG, String.format("建造进度 🏗️ 倍数: x%d | 能量: %d | 本次: +%d | 今日: %d/%d%s",
                            multiNum, leftEnergy, stepIncrease, totalSteps,
                            neverlandGridStepCount.getValue(), awardInfo));

                    Thread.sleep(1000);
                }

                Log.other(TAG, "自动建造任务完成 ✓");

            } catch (Throwable t) {
                Log.error(TAG, "executeAutoBuild 发生异常");
                Log.printStackTrace(TAG, t);
            }
        }

        /**
         * 计算建造实际产生的步数
         *
         * @param buildData 建造响应数据
         * @param defaultMulti 默认倍数
         * @return 实际步数
         */
        private int calculateBuildSteps(JSONObject buildData, int defaultMulti) {
            try {
                JSONArray buildResults = buildData.optJSONArray("buildResults");
                if (buildResults != null && buildResults.length() > 0) {
                    return buildResults.length();
                }
            } catch (Throwable t) {
                Log.printStackTrace(TAG, t);
            }
            return defaultMulti;
        }

        /**
         * 提取建造奖励信息
         *
         * @param buildData 建造响应数据
         * @return 奖励描述字符串
         */
        private String extractAwardInfo(JSONObject buildData) {
            try {
                JSONArray awards = buildData.optJSONArray("awards");
                if (awards != null && awards.length() > 0) {
                    return String.format(" | 获得奖励: %d 项", awards.length());
                }
            } catch (Throwable t) {
                Log.printStackTrace(TAG, t);
            }
            return "";
        }

        // -------------------------------------------------------------------------
        // 工具函数（bizId提取逻辑无变更）
        // -------------------------------------------------------------------------

        private String extractBizIdFromJumpLink(String jumpLink) {
            if (jumpLink == null || jumpLink.isEmpty()) return null;

            try {
                // 格式1：直接提取 bizId 参数（含URL编码）
                int idx = jumpLink.indexOf("bizId=");
                if (idx < 0) idx = jumpLink.indexOf("bizId%3D");
                if (idx >= 0) {
                    int start = jumpLink.indexOf("=", idx) + 1;
                    int end = jumpLink.indexOf("&", start);
                    if (end < 0) end = jumpLink.length();
                    String bizId = URLDecoder.decode(jumpLink.substring(start, end), "UTF-8").trim();
                    if (!bizId.isEmpty()) return bizId;
                }

                // 格式2：从 cdpQueryParams 提取
                if (jumpLink.contains("cdpQueryParams=")) {
                    int cdpIdx = jumpLink.indexOf("cdpQueryParams=");
                    int cdpStart = jumpLink.indexOf("=", cdpIdx) + 1;
                    int cdpEnd = jumpLink.indexOf("&", cdpStart);
                    if (cdpEnd < 0) cdpEnd = jumpLink.length();
                    String cdpEncoded = jumpLink.substring(cdpStart, cdpEnd);
                    String cdpJson = URLDecoder.decode(cdpEncoded, "UTF-8");
                    JSONObject cdpObj = new JSONObject(cdpJson);
                    String bizId = cdpObj.optString("bizId", "").trim();
                    if (!bizId.isEmpty()) return bizId;
                }

                // 兼容32位hex格式
                String candidate = jumpLink.replaceAll("%26", "&");
                if (candidate.length() >= 32) {
                    for (int i = 0; i + 32 <= candidate.length(); i++) {
                        String sub = candidate.substring(i, i + 32);
                        if (sub.matches("[0-9a-fA-F]{32}")) return sub;
                    }
                }

            } catch (Exception e) {
                Log.error(TAG, "extractBizIdFromJumpLink 解析失败，jumpLink=" + jumpLink);
            }
            return null;
        }
    }


    public interface WalkPathTheme {
        int DA_MEI_ZHONG_GUO = 0;
        int GONG_YI_YI_XIAO_BU = 1;
        int DENG_DING_ZHI_MA_SHAN = 2;
        int WEI_C_DA_TIAO_ZHAN = 3;
        int LONG_NIAN_QI_FU = 4;
        int SHOU_HU_TI_YU_MENG = 5;
        String[] nickNames = {"大美中国", "公益一小步", "登顶芝麻山", "维C大挑战", "龙年祈福", "守护体育梦"};
    }

    public interface DonateCharityCoinType {
        int ONE = 0;
        int ALL = 1;
        String[] nickNames = {"捐赠一个项目", "捐赠所有项目"};
    }

    public interface BattleForFriendType {
        int ROB = 0;
        int DONT_ROB = 1;
        String[] nickNames = {"选中抢", "选中不抢"};
    }
}