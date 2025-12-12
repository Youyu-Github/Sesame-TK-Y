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
        return "合种";
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
                if (ResChecker.checkRes(TAG,jo)) {
                    Log.runtime(TAG, "获取合种列表成功");
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                    JSONArray ja = jo.getJSONArray("cooperatePlants");
                    for (int i = 0; i < ja.length(); i++) {
                        jo = ja.getJSONObject(i);
                        String cooperationId = jo.getString("cooperationId");
                        if (!jo.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            jo = new JSONObject(s).getJSONObject("cooperatePlant");
                        }
                        String admin = jo.getString("admin");
                        String name = jo.getString("name");
                        if (cooperateSendCooperateBeckon.getValue() && Objects.equals(UserMap.getCurrentUid(), admin)) {
                            cooperateSendCooperateBeckon(cooperationId, name);
                        }
                        int waterDayLimit = jo.getInt("waterDayLimit");
                        Log.runtime(TAG, "合种[" + name + "]: 日限额:" + waterDayLimit);
                        CooperateMap.getInstance(CooperateMap.class).add(cooperationId, name);
                        if (!Status.canCooperateWaterToday(UserMap.getCurrentUid(), cooperationId)) {
                            Log.runtime(TAG, "[" + name + "]今日已浇水💦");
                            continue;
                        }
                        Integer waterId = cooperateWaterList.getValue().get(cooperationId);
                        if (waterId != null) {
                            Integer limitNum = cooperateWaterTotalLimitList.getValue().get(cooperationId);
                            if (limitNum != null) {
                                int cumulativeWaterAmount = calculatedWaterNum(cooperationId);
                                if (cumulativeWaterAmount < 0) {
                                    Log.runtime(TAG, "当前用户[" + UserMap.getCurrentUid() + "]的累计浇水能量获取失败,跳过本次浇水！");
                                    continue;
                                }
                                waterId = limitNum - cumulativeWaterAmount;
                                Log.runtime(TAG, "[" + name + "] 调整后的浇水数量: " + waterId);
                            }
                            if (waterId > waterDayLimit) {
                                waterId = waterDayLimit;
                            }
                            if (waterId > userCurrentEnergy) {
                                waterId = userCurrentEnergy;
                            }
                            if (waterId > 0) {
                                cooperateWater(cooperationId, waterId, name);
                            } else {
                                Log.runtime(TAG, "浇水数量为0，跳过[" + name + "]");
                            }
                        } else {
                            Log.runtime(TAG, "浇水列表中没有为[" + name + "]配置");
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            } else {
                Log.runtime(TAG, "合种浇水功能未开启");
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
     * 执行森林组队版浇水 (修复版)
     */
    private void runTeamVersionWater() {
        try {
            Log.runtime(TAG, "检查森林组队版浇水...");
            
            // 查询主页获取 TeamID 和 当前能量
            String homeJson = AntCooperateRpcCall.queryForestHomePage();
            if (homeJson == null) return;
            JSONObject homeJo = new JSONObject(homeJson);
            if (!ResChecker.checkRes(TAG, homeJo)) {
                return;
            }

            // 获取 TeamID
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
            if (teamId == null || teamId.isEmpty()) {
                Log.record(TAG, "TeamID为空");
                return;
            }
            
            // 检查今日是否已执行
            if (!Status.canCooperateWaterToday(UserMap.getCurrentUid(), "teamVersion_" + teamId)) {
                Log.record(TAG, "森林组队版今日已浇水💦");
                return;
            }

            // 1. 获取用户当前能量余额
            int currentEnergy = 0;
            if (homeJo.has("userEnergy")) {
                currentEnergy = homeJo.getJSONObject("userEnergy").optInt("energySummation");
            } else if (homeJo.has("userBaseInfo")) {
                currentEnergy = homeJo.getJSONObject("userBaseInfo").optInt("currentEnergy");
            }

            // 2. 获取用户配置的目标浇水量
            int configEnergy = teamVersionWaterCount.getValue();
            // 再次校验边界 (尽管ModelField已经做了限制)
            if (configEnergy < 200) configEnergy = 200;
            if (configEnergy > 5000) configEnergy = 5000;

            // 3. 获取服务端今日剩余可浇水额度
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
                            Log.runtime(TAG, "服务端限制: 今日剩余可浇 " + serverRemaining + "g");
                        }
                    }
                }
            }

            if (serverRemaining <= 0) {
                Log.record(TAG, "服务端限制：今日该队伍已不可浇水");
                return;
            }

            // 4. 核心逻辑：取三者最小值 (配置值，余额，服务端剩余)
            int realWaterAmount = configEnergy;
            
            // 如果余额不足，降级为余额
            if (realWaterAmount > currentEnergy) {
                realWaterAmount = currentEnergy;
            }
            // 如果超过服务端限制，降级为服务端限制
            if (realWaterAmount > serverRemaining) {
                realWaterAmount = serverRemaining;
            }

            // 5. 最终校验
            if (realWaterAmount < 10) { // 至少浇10g
                Log.record(TAG, "计算后可浇水量不足(" + realWaterAmount + "g)，跳过 (余额:" + currentEnergy + ", 限额:" + serverRemaining + ")");
                return;
            }

            // 6. 执行浇水
            Log.record(TAG, "森林组队版开始浇水: " + realWaterAmount + "g");
            String res = AntCooperateRpcCall.teamWater(teamId, realWaterAmount);
            if (res == null) return;
            
            JSONObject resJo = new JSONObject(res);
            if (ResChecker.checkRes(TAG, resJo)) {
                Log.forest("森林组队版浇水成功🌲: " + realWaterAmount + "g");
                Status.cooperateWaterToday(UserMap.getCurrentUid(), "teamVersion_" + teamId);
            } else {
                Log.record(TAG, "森林组队版浇水失败: " + resJo.optString("resultDesc"));
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

    private static int calculatedWaterNum(String coopId) {
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
            Log.runtime(TAG, "calculatedWaterNum err:");
            Log.printStackTrace(TAG, t);
        }
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