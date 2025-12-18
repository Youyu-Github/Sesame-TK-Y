package fansirsqi.xposed.sesame.task.antCooperate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Objects;

import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntCooperate extends ModelTask {
    private static final String TAG = AntCooperate.class.getSimpleName();
    private static int num;
    private static int limitNum;

    /**
     * 获取任务名称
     *
     * @return 合种任务名称
     */
    @Override
    public String getName() {
        return "森林合种";
    }

    /**
     * 获取任务分组
     *
     * @return 森林分组
     */
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    /**
     * 获取任务图标
     *
     * @return 合种任务图标文件名
     */
    @Override
    public String getIcon() {
        return "AntCooperate.png";
    }

    // 新增真爱合种字段
    private final BooleanModelField loveCooperateWater = new BooleanModelField("loveCooperateWater", "真爱合种浇水 | 开启", false);
    private final IntegerModelField loveCooperateWaterCount = new IntegerModelField("loveCooperateWaterCount", "设置真爱合种浇水能量克数", 20);
    // 森林组队版配置
    private final BooleanModelField teamVersionWater = new BooleanModelField("teamVersionWater", "森林组队版浇水 | 开启", false);
    private final IntegerModelField teamVersionWaterCount = new IntegerModelField("teamVersionWaterCount", "设置森林组队版浇水投能量克数", 200, 200, 5000);
    private final BooleanModelField cooperateWater = new BooleanModelField("cooperateWater", "合种浇水 | 开启", false);
    private final SelectAndCountModelField cooperateWaterList = new SelectAndCountModelField("cooperateWaterList", "合种浇水列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList(), "开启合种浇水后执行一次重载");
    private final SelectAndCountModelField cooperateWaterTotalLimitList = new SelectAndCountModelField("cooperateWaterTotalLimitList", "浇水总量限制列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList());
    private final BooleanModelField cooperateSendCooperateBeckon = new BooleanModelField("cooperateSendCooperateBeckon", "合种 | 召唤队友浇水| 仅队长 ", false);
    
    
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        // 新增真爱合种字段
        modelFields.addField(loveCooperateWater);
        modelFields.addField(loveCooperateWaterCount);
        // 森林组队版
        modelFields.addField(teamVersionWater);
        modelFields.addField(teamVersionWaterCount);
        
        modelFields.addField(cooperateWater);
        modelFields.addField(cooperateWaterList);
        modelFields.addField(cooperateWaterTotalLimitList);
        modelFields.addField(cooperateSendCooperateBeckon);
        return modelFields;
    }

    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行合种任务
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
     * 执行合种任务的主要逻辑
     */
    @Override
    public void run() {
        try {
            Log.record(TAG, "执行开始-" + getName());

            // 真爱合种浇水逻辑（优先执行）
            if (loveCooperateWater.getValue()) {
                runLoveCooperateWater();
            }

            // 森林组队版浇水逻辑
            if (teamVersionWater.getValue()) {
                runTeamVersionWater();
            }

            // 普通合种浇水逻辑
            if (cooperateWater.getValue()) {
                String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                JSONObject jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG, jo)) {
                    // 1. 获取当前能量，设为局部变量，因为浇水后需要扣减，否则下一个合种会误判能量充足
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                    JSONArray ja = jo.getJSONArray("cooperatePlants");
                    Log.runtime(TAG, "获取合种列表成功: " + ja.length() + " 颗合种");
                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject plant = ja.getJSONObject(i);
                        String cooperationId = plant.getString("cooperationId");
                        // 补全缺失的合种名称信息
                        if (!plant.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            plant = new JSONObject(s).getJSONObject("cooperatePlant");
                        }

                        String name = plant.getString("name");
                        String admin = plant.getString("admin");

                        // 2. 合种打招呼逻辑 (独立判断，不影响浇水主流程)
                        if (cooperateSendCooperateBeckon.getValue() && Objects.equals(UserMap.getCurrentUid(), admin)) {
                            cooperateSendCooperateBeckon(cooperationId, name);
                        }

                        // 3. 记录合种信息到本地 Map
                        CooperateMap.getInstance(CooperateMap.class).add(cooperationId, name);

                        // 4. 检查是否满足“今日是否可浇水”的本地状态缓存
                        if (!Status.canCooperateWaterToday(UserMap.getCurrentUid(), cooperationId)) {
                            // Log.runtime(TAG, name + " 今日已标记为不可浇水/已浇完");
                            continue;
                        }

                        // 获取服务端限制
                        int waterDayLimit = plant.getInt("waterDayLimit"); // 今日剩余可浇水量
                        int waterLimit = plant.getJSONObject("cooperateTemplate").getInt("waterLimit"); // 每日总上限
                        Log.runtime(TAG, "获取合种[" + name + "] 浇水信息: 剩余可浇 " + waterDayLimit + " g / 总限制 " + waterLimit + " g");

                        // 5. 获取配置
                        Integer configPerRound = cooperateWaterList.getValue().get(cooperationId); // 本轮配置浇水量
                        Integer configTotalLimit = cooperateWaterTotalLimitList.getValue().get(cooperationId); // 配置的总浇水上限(累计)

                        if (configPerRound == null) {
                            Log.runtime(TAG, "浇水列表中没有为[" + name + "]配置，跳过");
                            continue;
                        }

                        // 6. 计算本轮目标浇水量 (Target Water)
                        int planToWater;

                        if (configTotalLimit == null) {
                            // 逻辑保持原意：如果没有配置总限制，则直接把今日剩余额度拉满
                            Log.runtime(TAG, "未配置 " + name + " 限制总浇水，目标为填满今日额度");
                            planToWater = waterDayLimit;
                        } else {
                            Log.runtime(TAG, "载入配置 " + name + " 限制总浇水[" + configTotalLimit + "]g");
                            int totalWatered = getTotalWatering(cooperationId); // 获取已累计浇水

                            if (totalWatered < 0) {
                                Log.runtime(TAG, "无法获取用户[" + UserMap.getCurrentUid() + "]的累计浇水数据，跳过 " + name);
                                continue;
                            }

                            int remainingQuota = configTotalLimit - totalWatered;
                            if (remainingQuota <= 0) {
                                Log.forest(TAG, name + " 累计浇水已达标(" + totalWatered + "/" + configTotalLimit + ")，跳过");
                                continue;
                            }

                            // 目标水量 = 剩余额度
                            planToWater = remainingQuota;
                        }

                        // 7. 最终数值修正 (核心优化：统一使用 min 逻辑)
                        // 实际浇水量 = Min(计划量, 今日剩余可浇量, 当前背包能量)
                        int actualWater = planToWater;

                        if (actualWater > waterDayLimit) {
                            actualWater = waterDayLimit;
                        }
                        if (actualWater > userCurrentEnergy) {
                            actualWater = userCurrentEnergy;
                        }

                        Log.runtime(TAG, "[" + name + "] 结算: 计划 " + planToWater + ", 剩余限额 " + waterDayLimit + ", 背包 " + userCurrentEnergy + " -> 实际: " + actualWater);


                        // 8. 执行浇水
                        if (actualWater > 0) {
                            cooperateWater(cooperationId, actualWater, name);
                            // !!! 关键修正：本地扣除能量，供下一次循环判断使用 !!!
                            userCurrentEnergy -= actualWater;
                        } else {
                            Log.runtime(TAG, "计算后实际可浇水量为0，跳过[" + name + "]");
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            CooperateMap.getInstance(CooperateMap.class).save(UserMap.getCurrentUid());
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    /**
     * 执行真爱合种浇水
     */
    private void runLoveCooperateWater() {
        try {
            Log.runtime(TAG, "开始执行真爱合种浇水");

            // 获取真爱合种首页信息
            String loveHomeResponse = AntCooperateRpcCall.loveHome();
            JSONObject loveHomeJo = new JSONObject(loveHomeResponse);

            if (ResChecker.checkRes(TAG, loveHomeJo)) {
                JSONObject teamInfo = loveHomeJo.getJSONObject("teamInfo");
                String teamId = teamInfo.getString("teamId");
                String teamStatus = teamInfo.getString("teamStatus");

                if (!"ACTIVATED".equals(teamStatus)) {
                    Log.runtime(TAG, "真爱合种队伍状态异常: " + teamStatus);
                    return;
                }

                // 检查今日是否已浇水
                JSONObject waterInfo = teamInfo.getJSONObject("waterInfo");
                JSONObject todayWaterMap = waterInfo.getJSONObject("todayWaterMap");
                String currentUserId = UserMap.getCurrentUid();

                if (todayWaterMap.has(currentUserId) && todayWaterMap.getInt(currentUserId) > 0) {
                    Log.runtime(TAG, "真爱合种今日已浇水💦");
                    return;
                }

                // 获取用户设置的能量值（直接获取数字）
                int waterCount = loveCooperateWaterCount.getValue();

                // IntegerModelField 已经保证了最小值20，这里不需要再检查
                // 但为了保险，还是加一个日志
                Log.runtime(TAG, "真爱合种浇水能量: " + waterCount + "g");

                // 检查用户当前能量是否足够
                int userCurrentEnergy = getCurrentEnergy();
                if (userCurrentEnergy < waterCount) {
                    Log.runtime(TAG, "当前能量不足，无法进行真爱合种浇水。当前能量: " + userCurrentEnergy + "g，需要: " + waterCount + "g");
                    return;
                }

                // 执行真爱合种浇水
                loveCooperateWater(teamId, waterCount);

            } else {
                Log.error(TAG, "获取真爱合种信息失败: " + loveHomeJo.getString("resultDesc"));
            }

        } catch (Throwable t) {
            Log.runtime(TAG, "runLoveCooperateWater err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 获取用户当前能量
     */
    private int getCurrentEnergy() {
        try {
            String indexResponse = AntCooperateRpcCall.queryUserCooperatePlantList();
            JSONObject jo = new JSONObject(indexResponse);
            if (ResChecker.checkRes(TAG, jo)) {
                return jo.getInt("userCurrentEnergy");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "getCurrentEnergy err:");
            Log.printStackTrace(TAG, t);
        }
        return 0;
    }

    /**
     * 真爱合种浇水
     */
    private static void loveCooperateWater(String teamId, int count) {
        try {
            String s = AntCooperateRpcCall.loveTeamWater(teamId, count);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                Log.forest("真爱合种浇水💖[" + count + "g]成功");
                // 记录浇水状态，避免重复浇水
                Status.cooperateWaterToday(UserMap.getCurrentUid(), "love_" + teamId);
            } else {
                Log.runtime(TAG, "真爱合种浇水失败: " + jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "loveCooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            GlobalThreadPools.sleep(1500);
        }
    }

    /**
     * 执行森林组队版浇水 (智能无感检测版)
     */
    private void runTeamVersionWater() {
        try {
            Log.runtime(TAG, "检查森林组队版浇水...");

            // 1. 查询主页
            String homeJson = AntCooperateRpcCall.queryForestHomePage();
            if (homeJson == null) return;
            JSONObject homeJo = new JSONObject(homeJson);
            if (!ResChecker.checkRes(TAG, homeJo)) return;

            // 2. 获取 TeamID 和 当前模式
            JSONObject teamHomeResult = homeJo.optJSONObject("teamHomeResult");
            if (teamHomeResult == null) {
                Log.record(TAG, "未加入森林组队，跳过");
                return;
            }
            JSONObject teamBaseInfo = teamHomeResult.optJSONObject("teamBaseInfo");
            if (teamBaseInfo == null) {
                Log.record(TAG, "获取组队信息失败");
                return;
            }
            String teamId = teamBaseInfo.optString("teamId");
            if (teamId == null || teamId.isEmpty()) return;

            // 判断是否处于组队版 (存在 memberList 即为组队版)
            boolean isTeamMode = teamHomeResult.has("memberList");
            
            // 3. 获取今日已浇水量
            int myTodayEnergy = 0;
            String currentUid = UserMap.getCurrentUid();

            if (isTeamMode) {
                // 如果是组队版，直接从主页数据读取
                JSONArray memberList = teamHomeResult.optJSONArray("memberList");
                if (memberList != null) {
                    for (int i = 0; i < memberList.length(); i++) {
                        JSONObject member = memberList.getJSONObject(i);
                        if (Objects.equals(member.optString("userId"), currentUid)) {
                            myTodayEnergy = member.optInt("todayEnergy", 0);
                            break;
                        }
                    }
                }
            } else {
                // 如果是个人版，调用 queryTeamMemberList 查询 (不需要切换模式)
                String memberJson = AntCooperateRpcCall.queryTeamMemberList(teamId);
                if (memberJson != null) {
                    JSONObject memberJo = new JSONObject(memberJson);
                    if (ResChecker.checkRes(TAG, memberJo)) {
                        JSONArray memberList = memberJo.optJSONArray("memberList");
                        if (memberList != null) {
                            for (int i = 0; i < memberList.length(); i++) {
                                JSONObject member = memberList.getJSONObject(i);
                                if (Objects.equals(member.optString("userId"), currentUid)) {
                                    myTodayEnergy = member.optInt("todayEnergy", 0);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // 4. 判断达标情况
            int configTarget = teamVersionWaterCount.getValue();
            // 边界修正
            if (configTarget < 200) configTarget = 200;
            if (configTarget > 5000) configTarget = 5000;

            if (myTodayEnergy >= configTarget) {
                Log.record(TAG, "森林组队版: 今日已达标 (已浇" + myTodayEnergy + "g / 目标" + configTarget + "g)，停止浇水");
                return;
            }

            int neededEnergy = configTarget - myTodayEnergy;
            Log.record(TAG, "森林组队版: 今日已浇 " + myTodayEnergy + "g，还需要 " + neededEnergy + "g");

            // 5. 计算可浇水量
            // 获取余额
            int currentEnergy = 0;
            if (homeJo.has("userEnergy")) {
                currentEnergy = homeJo.getJSONObject("userEnergy").optInt("energySummation");
            } else if (homeJo.has("userBaseInfo")) {
                currentEnergy = homeJo.getJSONObject("userBaseInfo").optInt("currentEnergy");
            }

            // 获取服务端限制
            int serverRemaining = 0;
            String miscInfoJson = AntCooperateRpcCall.queryTeamMiscInfo(teamId);
            if (miscInfoJson != null) {
                JSONObject miscInfoJo = new JSONObject(miscInfoJson);
                if (ResChecker.checkRes(TAG, miscInfoJo)) {
                    JSONObject combineMap = miscInfoJo.optJSONObject("combineHandlerVOMap");
                    if (combineMap != null) {
                        JSONObject teamWaterInfo = combineMap.optJSONObject("teamCanWaterCount");
                        if (teamWaterInfo != null) {
                            serverRemaining = teamWaterInfo.optInt("waterCount", 0);
                        }
                    }
                }
            }

            if (serverRemaining <= 0) {
                Log.record(TAG, "服务端限制：今日该队伍已不可浇水");
                return;
            }

            // 取三者最小值
            int realWaterAmount = neededEnergy;
            if (realWaterAmount > currentEnergy) realWaterAmount = currentEnergy;
            if (realWaterAmount > serverRemaining) realWaterAmount = serverRemaining;

            if (realWaterAmount < 10) {
                Log.record(TAG, "计算后可浇水量不足(" + realWaterAmount + "g)，跳过");
                return;
            }

            // 6. 确定需要切换模式
            boolean needSwitch = !isTeamMode;

            if (needSwitch) {
                // Log.record(TAG, "当前为[个人版]，正在切换至[组队版]以进行浇水...");
                AntCooperateRpcCall.updateUserConfig("Y");
                TimeUtil.sleep(500); 
            }

            // 7. 执行浇水
            Log.record(TAG, "森林组队版开始浇水: " + realWaterAmount + "g");
            String res = AntCooperateRpcCall.teamWater(teamId, realWaterAmount);
            
            if (res != null) {
                JSONObject resJo = new JSONObject(res);
                if (ResChecker.checkRes(TAG, resJo)) {
                    Log.forest("森林组队版浇水成功🌲: " + realWaterAmount + "g");
                    Status.cooperateWaterToday(UserMap.getCurrentUid(), "teamVersion_" + teamId);
                } else {
                    Log.record(TAG, "森林组队版浇水失败: " + resJo.optString("resultDesc"));
                }
            }

            // 8. 如果切过模式，现在切回去
            if (needSwitch) {
                TimeUtil.sleep(500);
                // Log.record(TAG, "浇水完成，正在切回[个人版]...");
                AntCooperateRpcCall.updateUserConfig("N");
            }

        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 普通合种浇水
     */
    private static void cooperateWater(String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(UserMap.getCurrentUid(), coopId, count);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG,jo)) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                Status.cooperateWaterToday(UserMap.getCurrentUid(), coopId);
            } else {
                Log.runtime(TAG, "浇水失败[" + name + "]: " + jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            GlobalThreadPools.sleep(1500);
        }
    }

    /**
     * 计算合种需要浇水的克数
     */
    private static int getTotalWatering(String coopId) {
        try {
            String s = AntCooperateRpcCall.queryCooperateRank("A", coopId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success", false)) {
                JSONArray jaList = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    String userId = joItem.getString("userId");
                    if (userId.equals(UserMap.getCurrentUid())) {
                        // 未获取到累计浇水量 返回 -1 不执行浇水
                        int energySummation = joItem.optInt("energySummation", -1);
                        if (energySummation >= 0) {
                            Log.runtime(TAG, "当前用户[" + userId + "]的累计浇水能量: " + energySummation);
                        }
                        return energySummation;
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "计算合种需要浇水的克数err");
            Log.printStackTrace(TAG, t);
        }
        Log.runtime(TAG, "合种获取累计浇水量失败");
        return -1; // 未获取到累计浇水量，停止浇水
    }

    private static void cooperateSendCooperateBeckon(String cooperationId, String name) {
        try {
            if (TimeUtil.isNowBeforeTimeStr("1800")) {
                return;
            }
            TimeUtil.sleep(500);
            JSONObject jo = new JSONObject(AntCooperateRpcCall.queryCooperateRank("D", cooperationId));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray cooperateRankInfos = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < cooperateRankInfos.length(); i++) {
                    JSONObject rankInfo = cooperateRankInfos.getJSONObject(i);
                    if (rankInfo.getBoolean("canBeckon")) {
                        jo = new JSONObject(AntCooperateRpcCall.sendCooperateBeckon(rankInfo.getString("userId"), cooperationId));
                        if (ResChecker.checkRes(TAG,jo)) {
                            Log.forest("合种🚿[" + name + "]#召唤队友[" + rankInfo.getString("displayName") + "]成功");
                        }
                        TimeUtil.sleep(1000);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateSendCooperateBeckon err:");
            Log.printStackTrace(TAG, t);
        }
    }
}