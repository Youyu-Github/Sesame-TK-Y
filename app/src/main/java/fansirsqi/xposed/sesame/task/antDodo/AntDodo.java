package fansirsqi.xposed.sesame.task.antDodo;
import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import fansirsqi.xposed.sesame.data.DataCache;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;

/**
 * 神奇物种任务类
 * 负责处理支付宝神奇物种（蚂蚁森林生物多样性）相关的自动化任务
 */
public class AntDodo extends ModelTask {
    private static final String TAG = AntDodo.class.getSimpleName();
    
    /**
     * 获取任务名称
     *
     * @return 神奇物种任务名称
     */
    @Override
    public String getName() {
        return "神奇物种";
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
     * @return 神奇物种任务图标文件名
     */
    @Override
    public String getIcon() {
        return "AntDodo.png";
    }
    
    // 配置字段定义
    private BooleanModelField collectToFriend;                  // 是否帮好友抽卡
    private ChoiceModelField collectToFriendType;               // 帮好友抽卡的类型选择
    private SelectModelField collectToFriendList;               // 帮抽卡的好友列表
    private SelectModelField sendFriendCard;                    // 送卡片的好友列表
    private BooleanModelField usePropUNIVERSAL_CARD;            //万能卡
    private ChoiceModelField usePropUNIVERSALCARDType;          //万能卡使用类型
    private BooleanModelField usePropaddCOLLECTTOFRIENDLIMIT;   //抽好友道具卡
    private BooleanModelField autoGenerateBook;                 // 是否自动合成图鉴
    
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectToFriend = new BooleanModelField("collectToFriend", "帮抽卡 | 开启", false));
        modelFields.addField(collectToFriendType = new ChoiceModelField("collectToFriendType", "帮抽卡 | 动作", CollectToFriendType.COLLECT, CollectToFriendType.nickNames));
        modelFields.addField(collectToFriendList = new SelectModelField("collectToFriendList", "帮抽卡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(sendFriendCard = new SelectModelField("sendFriendCard", "送卡片好友列表(当前图鉴所有卡片)", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(usePropUNIVERSAL_CARD = new BooleanModelField("usePropUNIVERSAL_CARD", "使用道具 | 万能卡", false));
        modelFields.addField(usePropUNIVERSALCARDType = new ChoiceModelField("usePropUNIVERSALCARDType", "万能卡 | 使用方式", UniversalCardUseType.EXCLUDE_CURRENT, UniversalCardUseType.nickNames));
        modelFields.addField(usePropaddCOLLECTTOFRIENDLIMIT = new BooleanModelField("usePropaddCOLLECTTOFRIENDLIMIT", "使用道具 | 抽好友卡道具", false));
        modelFields.addField(autoGenerateBook = new BooleanModelField("autoGenerateBook", "自动合成图鉴", false));
        return modelFields;
    }
    
    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行神奇物种任务
     */
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME){
            Log.record(TAG,"⏸ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
            return false;
        }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG,"💤 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * 执行神奇物种任务的主要逻辑
     */
    @Override
    public void run() {
        try {
            Log.record(TAG,"执行开始-" + getName());
            receiveTaskAward(); // 领取任务奖励
            propList(); // 使用道具
            collect(); // 收集动物卡片
            if (collectToFriend.getValue()) {
                collectToFriend(); // 帮好友抽卡
            }
            if(autoGenerateBook.getValue()){
                autoGenerateBook(); // 自动合成图鉴
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start Dodo.run err:");
            Log.printStackTrace(TAG, t);
        }finally {
            Log.record(TAG,"执行结束-" + getName());
        }
    }
    
    /**
     * 判断是否为最后一天
     * @param endDate 结束日期
     * @return 是否为最后一天（距离结束时间小于24小时）
     */
    private boolean lastDay(String endDate) {
        long timeStemp = System.currentTimeMillis();
        long endTimeStemp = TimeUtil.timeToStamp(endDate);
        return timeStemp < endTimeStemp && (endTimeStemp - timeStemp) < 86400000L;
    }
    
    /**
     * 判断是否在8天内
     * @param endDate 结束日期
     * @return 是否在8天内（距离结束时间小于8天）
     */
    public boolean in8Days(String endDate) {
        long timeStemp = System.currentTimeMillis();
        long endTimeStemp = TimeUtil.timeToStamp(endDate);
        return timeStemp < endTimeStemp && (endTimeStemp - timeStemp) < 691200000L;
    }
    
    /**
     * 收集动物卡片的主方法
     * 检查今日是否已收集完成，如果未完成则进行收集
     */
    private void collect() {
        try {
            JSONObject jo = new JSONObject(AntDodoRpcCall.queryAnimalStatus());
            if (ResChecker.checkRes(TAG + "查询动物收集状态失败:", jo)) {
                JSONObject data = jo.getJSONObject("data");
                if (data.getBoolean("collect")) {
                    Log.record(TAG,"神奇物种卡片今日收集完成！");
                } else {
                    collectAnimalCard(); // 如果未收集完成，则进行收集
                }
            } else {
                Log.record(TAG, "collect错误" + jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "AntDodo Collect err:");
            Log.printStackTrace(TAG, t);
        }
    }
    
    /**
     * 收集动物卡片的具体实现
     * 包括查询主页信息、领取任务奖励、使用道具、抽卡、送卡片给好友等操作
     */
    private void collectAnimalCard() {
        try {
            JSONObject jo = new JSONObject(AntDodoRpcCall.homePage());
            if (ResChecker.checkRes(TAG + "获取神奇物种主页失败:", jo)) {
                JSONObject data = jo.getJSONObject("data");
                JSONObject animalBook = data.getJSONObject("animalBook");
                String bookId = animalBook.getString("bookId");
                String endDate = animalBook.getString("endDate") + " 23:59:59";
                receiveTaskAward(); // 领取任务奖励
                if (!in8Days(endDate) || lastDay(endDate))
                    propList(); // 如果不在8天内或是最后一天，使用道具
                JSONArray ja = data.getJSONArray("limit");
                int index = -1;
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if ("DAILY_COLLECT".equals(jo.getString("actionCode"))) {
                        index = i;
                        break;
                    }
                }
                Set<String> set = sendFriendCard.getValue();
                if (index >= 0) {
                    int leftFreeQuota = jo.getInt("leftFreeQuota");
                    for (int j = 0; j < leftFreeQuota; j++) {
                        jo = new JSONObject(AntDodoRpcCall.collect());
                        if (ResChecker.checkRes(TAG + "收集动物卡片失败:", jo)) {
                            data = jo.getJSONObject("data");
                            JSONObject animal = data.getJSONObject("animal");
                            String ecosystem = animal.getString("ecosystem");
                            String name = animal.getString("name");
                            Log.forest("神奇物种🦕[" + ecosystem + "]#" + name);
                            if (!set.isEmpty()) {
                                for (String userId : set) {
                                    if (!UserMap.getCurrentUid().equals(userId)) {
                                        int fantasticStarQuantity = animal.optInt("fantasticStarQuantity", 0);
                                        if (fantasticStarQuantity == 3) {
                                            sendCard(animal, userId); // 如果是3星卡片，发送给好友
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            Log.record(TAG, "collectAnimalCard错误"+ jo.getString("resultDesc"));
                        }
                    }
                }
                if (!set.isEmpty()) {
                    for (String userId : set) {
                        if (!UserMap.getCurrentUid().equals(userId)) {
                            sendAntDodoCard(bookId, userId); // 发送图鉴中的卡片给好友
                            break;
                        }
                    }
                }
            } else {
                Log.record(TAG, "collectAnimalCard错误2 "+ jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "AntDodo CollectAnimalCard err:");
            Log.printStackTrace(TAG, t);
        }
    }
    
    /**
     * 领取神奇物种任务奖励
     * 查询任务列表，完成待完成的任务，领取已完成任务的奖励
     */
    private void receiveTaskAward() {
        try {
            // 获取不能完成的任务列表
            Set<String> presetBad = new LinkedHashSet<>(List.of("HELP_FRIEND_COLLECT"));
            TypeReference<Set<String>> typeRef = new TypeReference<>() {};
            Set<String> badTaskSet = DataStore.INSTANCE.getOrCreate("badDodoTaskList", typeRef);
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad);
                DataStore.INSTANCE.put("badDodoTaskList", badTaskSet);
            }
            while (true) {
                boolean doubleCheck = false;
                String response = AntDodoRpcCall.taskList(); // 调用任务列表接口
                JSONObject jsonResponse = new JSONObject(response); // 解析响应为 JSON 对象
                // 检查响应结果码是否成功
                if (!ResChecker.checkRes(TAG + "查询任务列表失败:", jsonResponse)) {
                    Log.record(TAG, "查询任务列表失败：" + jsonResponse.getString("resultDesc"));
                    Log.runtime(response);
                    break;
                }
                // 获取任务组信息列表
                JSONArray taskGroupInfoList = jsonResponse.getJSONObject("data").optJSONArray("taskGroupInfoList");
                if (taskGroupInfoList == null) return; // 如果任务组为空则返回
                // 遍历每个任务组
                for (int i = 0; i < taskGroupInfoList.length(); i++) {
                    JSONObject antDodoTask = taskGroupInfoList.getJSONObject(i);
                    JSONArray taskInfoList = antDodoTask.getJSONArray("taskInfoList"); // 获取任务信息列表
                    // 遍历每个任务
                    for (int j = 0; j < taskInfoList.length(); j++) {
                        JSONObject taskInfo = taskInfoList.getJSONObject(j);
                        JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo"); // 获取任务基本信息
                        JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo")); // 获取业务信息
                        String taskType = taskBaseInfo.getString("taskType"); // 获取任务类型
                        String taskTitle = bizInfo.optString("taskTitle", taskType); // 获取任务标题
                        String awardCount = bizInfo.optString("awardCount", "1"); // 获取奖励数量
                        String sceneCode = taskBaseInfo.getString("sceneCode"); // 获取场景代码
                        String taskStatus = taskBaseInfo.getString("taskStatus"); // 获取任务状态
                        // 如果任务已完成，领取任务奖励
                        if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                            JSONObject joAward = new JSONObject(
                                    AntDodoRpcCall.receiveTaskAward(sceneCode, taskType)); // 领取奖励请求
                            if (joAward.optBoolean("success")) {
                                doubleCheck = true;
                                Log.forest("任务奖励🎖️[" + taskTitle + "]#" + awardCount + "个");
                            } else {
                                Log.record(TAG,"领取失败，" + response); // 记录领取失败信息
                            }
                            Log.record(TAG, joAward.toString()); // 打印奖励响应
                        }
                        // 如果任务待完成，处理特定类型的任务
                        else if (TaskStatus.TODO.name().equals(taskStatus)) {
                            if (!badTaskSet.contains(taskType)) {
                                // 尝试完成任务
                                JSONObject joFinishTask = new JSONObject(
                                        AntDodoRpcCall.finishTask(sceneCode, taskType)); // 完成任务请求
                                if (joFinishTask.optBoolean("success")) {
                                    Log.forest("物种任务🧾️[" + taskTitle + "]");
                                    doubleCheck = true;
                                } else {
                                    Log.record(TAG,"完成任务失败，" + taskTitle); // 记录完成任务失败信息
                                    badTaskSet.add(taskType);
                                    DataStore.INSTANCE.put("badDodoTaskList", badTaskSet);
                                }

                            }
                        }
                        GlobalThreadPools.sleep(500);
                    }
                }
                if (!doubleCheck) break;
            }
        } catch (JSONException e) {
            Log.error(TAG, "神奇物种 JSON解析错误: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        } catch (Throwable t) {
            Log.runtime(TAG, "AntDodo ReceiveTaskAward 错误:");
            Log.printStackTrace(TAG, t); // 打印异常栈
        }
    }
    
    /**
     * 使用道具列表
     * 查询用户拥有的道具，根据配置使用相应的道具
     */
    public void propList() {
        try {
            String s = AntDodoRpcCall.propList();
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray propList = jo.getJSONObject("data").getJSONArray("propList");
                for (int i = 0; i < propList.length(); i++) {
                    JSONObject prop = propList.getJSONObject(i);

                    // 注意：JSON 里的 propType 有多种（例如 UNIVERSAL_CARD_7_DAYS）
                    // 我们通过 propConfig 里的 propGroup 来分类更稳妥
                    JSONObject config = prop.optJSONObject("propConfig");
                    String propGroup = config != null ? config.optString("propGroup") : "";
                    String propType = prop.getString("propType");

                    // 拿到该类道具的所有 ID 列表
                    JSONArray propIdList = prop.getJSONArray("propIdList");
                    int holdsNum = prop.getInt("holdsNum");

                    if (holdsNum <= 0) continue;

                    // --- 逻辑分支开始 ---

                    // 1. 万能卡逻辑
                    if ("UNIVERSAL_CARD".equals(propGroup)) {
                        for (int j = 0; j < propIdList.length(); j++) {
                            String pId = propIdList.getString(j);
                            // 寻找缺失的动物 ID
                            String animalId = getTargetAnimalIdForUniversalCard();
                            if (!animalId.isEmpty()) {
                                // 调用带 animalId 的消耗方法
                                String res = AntDodoRpcCall.consumeProp(pId, propType, animalId);
                                if (ResChecker.checkRes(TAG, res)) {
                                    Log.forest(TAG, "万能卡使用成功，补全动物ID: " + animalId);
                                }
                                GlobalThreadPools.sleep(2*1000L);
                            }
                        }
                    }

                    // 2. 抽好友卡道具逻辑 (判断 UI 开关)
                    else if ("ADD_COLLECT_TO_FRIEND_LIMIT".equals(propGroup)) {
                        if (usePropaddCOLLECTTOFRIENDLIMIT.getValue()) {
                            for (int j = 0; j < propIdList.length(); j++) {
                                String pId = propIdList.getString(j);
                                // 调用不带 animalId 的专门方法
                                String res = AntDodoRpcCall.consumePropForFriend(pId, propType);
                                if (ResChecker.checkRes(TAG, new JSONObject(res))) {
                                    Log.record(TAG, "成功使用 [抽好友卡道具]");
                                }
                                GlobalThreadPools.sleep(2*1000L);
                            }
                        }
                    }

                    // 3. 其他基础道具 (按需扩展)

                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "propList 处理异常", t);
        }
    }
    
    /**
     * 发送神奇物种卡片给好友
     * @param bookId 卡片图鉴ID
     * @param targetUser 目标用户ID
     */
    private void sendAntDodoCard(String bookId, String targetUser) {
        try {
            JSONObject jo = new JSONObject(AntDodoRpcCall.queryBookInfo(bookId));
            if (ResChecker.checkRes(TAG + "查询图鉴信息失败:", jo)) {
                JSONArray animalForUserList = jo.getJSONObject("data").optJSONArray("animalForUserList");
                for (int i = 0; i < Objects.requireNonNull(animalForUserList).length(); i++) {
                    JSONObject animalForUser = animalForUserList.getJSONObject(i);
                    int count = animalForUser.getJSONObject("collectDetail").optInt("count");
                    if (count <= 0)
                        continue;
                    JSONObject animal = animalForUser.getJSONObject("animal");
                    for (int j = 0; j < count; j++) {
                        sendCard(animal, targetUser); // 发送卡片给好友
                        GlobalThreadPools.sleep(500L);
                    }
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "AntDodo SendAntDodoCard err:");
            Log.printStackTrace(TAG, th);
        }
    }
    
    /**
     * 发送单个动物卡片给好友
     * @param animal 动物信息JSON对象
     * @param targetUser 目标用户ID
     */
    private void sendCard(JSONObject animal, String targetUser) {
        try {
            String animalId = animal.getString("animalId");
            String ecosystem = animal.getString("ecosystem");
            String name = animal.getString("name");
            JSONObject jo = new JSONObject(AntDodoRpcCall.social(animalId, targetUser));
            if (ResChecker.checkRes(TAG + "发送卡片给好友失败:", jo)) {
                Log.forest("赠送卡片🦕[" + UserMap.getMaskName(targetUser) + "]#" + ecosystem + "-" + name);
            } else {
                Log.record(TAG, "sendCard错误" + jo.getString("resultDesc"));
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "AntDodo SendCard err:");
            Log.printStackTrace(TAG, th);
        }
    }
    
    /**
     * 帮好友抽卡
     * 查询好友列表，为指定好友进行抽卡操作
     */
    private void collectToFriend() {
        try {
            JSONObject jo = new JSONObject(AntDodoRpcCall.queryFriend());
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.record(TAG, "神奇物种帮好友抽卡失败："+jo.getString("resultDesc"));
                return;
            }

            // 获取可用次数
            int count = 0;
            JSONArray limitList = jo.getJSONObject("data").getJSONObject("extend").getJSONArray("limit");
            for (int i = 0; i < limitList.length(); i++) {
                JSONObject limit = limitList.getJSONObject(i);
                if ("COLLECT_TO_FRIEND".equals(limit.getString("actionCode"))) {
                    // 检查是否有开始时间限制
                    if (limit.has("startTime") && limit.getLong("startTime") > System.currentTimeMillis()) {
                        Log.record("神奇物种🦕帮好友抽卡未到开放时间: " + limit.getString("startTimeStr"));
                        return;
                    }
                    count = limit.getInt("leftLimit");
                    break;
                }
                }

            if (count <= 0) {
                Log.record("神奇物种🦕帮好友抽卡次数已用完");
                return;
            }

            // 遍历好友列表
            JSONArray friendList = jo.getJSONObject("data").getJSONArray("friends");
            for (int i = 0; i < friendList.length() && count > 0; i++) {
                JSONObject friend = friendList.getJSONObject(i);

                // 跳过今日已帮助的好友
                if (friend.getBoolean("dailyCollect")) {
                    continue;
                }

                String userId = friend.getString("userId");

                // 判断是否应该帮助该好友
                boolean inList = collectToFriendList.getValue().contains(userId);
                boolean shouldCollect = (collectToFriendType.getValue() == CollectToFriendType.COLLECT) ? inList : !inList;

                if (!shouldCollect) {
                    continue;
                }

                // 执行抽卡
                jo = new JSONObject(AntDodoRpcCall.collecttarget(userId));
                if (ResChecker.checkRes(TAG, jo)) {
                    String ecosystem = jo.getJSONObject("data").getJSONObject("animal").getString("ecosystem");
                    String name = jo.getJSONObject("data").getJSONObject("animal").getString("name");
                    String userName = UserMap.getMaskName(userId);
                    Log.forest("神奇物种🦕帮好友[" + userName + "]抽卡[" + ecosystem + "]#" + name);
                    count--;
                } else {
                    Log.record(TAG, "collecttarget错误" + jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "AntDodo CollectHelpFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    
    /**
     * 辅助逻辑：获取万能卡要兑换的精准动物ID
     */
    private String getTargetAnimalIdForUniversalCard() {
        try {
            JSONArray allBooks = getAllBookList();
            if (allBooks == null || allBooks.length() == 0) {
                Log.record(TAG, "万能卡：未获取到任何图鉴数据");
                return "";
            }

            String targetBookId = "";
            int strategy = usePropUNIVERSALCARDType.getValue();

            String currentDoingBookId = "";
            String bestOtherBookId = "";
            double maxOtherRate = -1.0;

            String bestOverallBookId = "";
            double maxOverallRate = -1.0;

            for (int i = 0; i < allBooks.length(); i++) {
                JSONObject book = allBooks.optJSONObject(i); // 使用 opt 防止 null
                if (book == null || isBookFinished(book)) continue;

                JSONObject result = book.optJSONObject("animalBookResult");
                if (result == null) continue;

                String bookId = result.optString("bookId");
                String status = book.optString("bookStatus");

                // --- 进度解析与计算 ---
                String prog = book.optString("collectProgress", "0/0");
                double rate = 0;
                try {
                    String[] p = prog.split("/");
                    if (p.length == 2) {
                        double current = Double.parseDouble(p[0]);
                        double total = Double.parseDouble(p[1]);
                        if (total > 0) {
                            rate = current / total;
                        }
                    }
                } catch (Exception ignored) {}

                // --- 策略分类收集 ---
                // 1. 识别当前正在进行的 (DOING)
                if ("DOING".equals(status)) {
                    currentDoingBookId = bookId;
                } else {
                    // 2. 识别非当前图鉴中进度最高的
                    if (rate > maxOtherRate) {
                        maxOtherRate = rate;
                        bestOtherBookId = bookId;
                    }
                }

                // 3. 识别全局进度最高的
                if (rate > maxOverallRate) {
                    maxOverallRate = rate;
                    bestOverallBookId = bookId;
                }
            }

            // --- 逻辑分支匹配 ---
            if (strategy == UniversalCardUseType.EXCLUDE_CURRENT) {
                targetBookId = bestOtherBookId;
                Log.record(TAG, "万能卡策略 [排除当前]: 选中非DOING最高进度图鉴 " + targetBookId);
            }
            else if (strategy == UniversalCardUseType.PRIORITY_MAX_PROGRESS) {
                targetBookId = bestOverallBookId;
                Log.record(TAG, "万能卡策略 [进度优先]: 选中全局最高进度图鉴 " + targetBookId);
            }
            else {
                // 模式：所有。优先进行中，进行中已满则选最高进度
                targetBookId = !currentDoingBookId.isEmpty() ? currentDoingBookId : bestOverallBookId;
                Log.record(TAG, "万能卡策略 [全部]: 优先进行中图鉴 " + targetBookId);
            }

            if (targetBookId.isEmpty()) return "";

            // --- 查询具体缺失卡片 ---
            String detailJson = AntDodoRpcCall.queryBookInfo(targetBookId);
            JSONObject detailObj = new JSONObject(detailJson);

            // 增加对 detail 接口返回结果的校验
            if (detailObj.optBoolean("success", false) || "SUCCESS".equals(detailObj.optString("resultCode"))) {
                JSONObject data = detailObj.optJSONObject("data");
                JSONArray animals = (data != null) ? data.optJSONArray("animalForUserList") : null;

                if (animals != null) {
                    for (int i = 0; i < animals.length(); i++) {
                        JSONObject item = animals.optJSONObject(i);
                        if (item == null) continue;

                        JSONObject collectDetail = item.optJSONObject("collectDetail");
                        // 只有 collect 为 false 才说明是缺的
                        if (collectDetail != null && !collectDetail.optBoolean("collect", false)) {
                            JSONObject animalInfo = item.optJSONObject("animal");
                            if (animalInfo != null) {
                                String animalId = animalInfo.optString("animalId");
                                String name = animalInfo.optString("name");
                                Log.record(TAG, "万能卡目标锁定: " + name + " (" + animalId + ")");
                                return animalId;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.record(TAG, "万能卡逻辑执行失败: " + e.getMessage());
        }
        return "";
    }

    /**
     * 判断某个图鉴是否已经“完成” (不需要再投入万能卡)
     */
    private static boolean isBookFinished(JSONObject book) {
        if (book == null) return true;

        // 1. 优先判断合成状态：如果已经可以合成或者已经合成，则认为该图鉴已完成
        String medalStatus = book.optString("medalGenerationStatus");
        if ("CAN_GENERATE".equals(medalStatus) || "GENERATED".equals(medalStatus)) {
            return true;
        }

        // 2. 判断数字进度：例如 "10/10"
        String progress = book.optString("collectProgress", "");
        if (progress.contains("/")) {
            try {
                String[] parts = progress.split("/");
                if (parts.length == 2) {
                    int current = Integer.parseInt(parts[0].trim());
                    int total = Integer.parseInt(parts[1].trim());
                    return current >= total; // 只要现有的不小于总数，就不需要万能卡
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /* 获取所有图鉴列表*/
    /**
     * 获取完整的图鉴数组 (自动处理翻页合并)
     * @return 包含所有图鉴对象的 JSONArray
     *
     * [
     *   {
     *     "animalBookResult": {
     *       "bookId": "dxmlyBook",
     *       "ecosystem": "东喜马拉雅高山森林生态系统",
     *       "name": "东喜马拉雅高山森林生态系统",
     *       "totalCount": 10,
     *       "magicCount": 1,
     *       "rareCount": 2,
     *       "commonCount": 7
     *       // ..
     *     },
     *     "bookStatus": "END",
     *     "bookCollectedStatus": "NOT_COMPLETED",
     *     "collectProgress": "1/10",
     *     "hasRedDot": false
     *   },
     *   {
     *     "animalBookResult": {
     *       "bookId": "zhbhtbhxcr202503",
     *       "name": "当前正在进行的某个图鉴",
     *       "totalCount": 10
     *       // ...
     *     },
     *     "bookStatus": "GOING",
     *     "bookCollectedStatus": "NOT_COMPLETED",
     *     "collectProgress": "5/10",
     *     "hasRedDot": true
     *   }
     *   // ...
     * ]
     */

    public static JSONArray getAllBookList() {
        JSONArray allBooks = new JSONArray();
        String pageStart = null; // 首页传 null
        boolean hasMore = true;

        try {
            while (hasMore) {
                // 调用上面修改后的接口
                String res = AntDodoRpcCall.queryBookList(64, pageStart);
                JSONObject jo = new JSONObject(res);

                if (!ResChecker.checkRes(TAG,jo)) {
                    Log.record(TAG, "queryBookList 失败: " + jo.optString("resultDesc"));
                    break;
                }

                JSONObject data = jo.optJSONObject("data");
                if (data == null) break;

                // 1. 提取并合并数据
                JSONArray currentList = data.optJSONArray("bookForUserList");
                if (currentList != null) {
                    for (int i = 0; i < currentList.length(); i++) {
                        allBooks.put(currentList.get(i));
                    }
                }

                // 2. 判断翻页逻辑
                hasMore = data.optBoolean("hasMore", false);
                pageStart = data.optString("nextPageStart", null);

                // 如果没有更多了，或者 nextPageStart 为空，直接跳出
                if (!hasMore || pageStart == null || pageStart.isEmpty()) {
                    break;
                }

                // 稍微控制一下频率
                GlobalThreadPools.sleep(300);
            }
        } catch (Throwable th) {
            Log.printStackTrace(TAG, "获取全量图鉴异常", th);
        }
        return allBooks;
    }

    /**
     * 自动合成图鉴
     */
    private void autoGenerateBook() {
        try {
            // 1. 直接获取所有页合并后的完整图鉴数组
            JSONArray allBooks = getAllBookList();

            if (allBooks.length() == 0) {
                return;
            }

            // 2. 遍历全量数组
            for (int i = 0; i < allBooks.length(); i++) {
                JSONObject bookItem = allBooks.getJSONObject(i);

                // 判断是否可以合成勋章
                if (!"CAN_GENERATE".equals(bookItem.optString("medalGenerationStatus"))) {
                    continue;
                }

                JSONObject animalBookResult = bookItem.optJSONObject("animalBookResult");
                if (animalBookResult == null) {
                    Log.record(TAG,"animalBookResult为空，停止合成");
                    continue;

                }

                String bookId = animalBookResult.optString("bookId");
                String ecosystem = animalBookResult.optString("ecosystem");

                // 3. 调用合成接口
                String res = AntDodoRpcCall.generateBookMedal(bookId);
                JSONObject genResp = new JSONObject(res);

                if (ResChecker.checkRes(TAG, genResp)) {
                    Log.forest("神奇物种🦕合成勋章[" + ecosystem + "]");
                } else {
                    Log.record(TAG, "合成勋章失败[" + ecosystem + "]: " + genResp.optString("resultDesc"));
                }

                // 合成操作建议稍微加一点点延迟，保护接口
                GlobalThreadPools.sleep(300);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "autoGenerateBook err:", t);
        }
    }

    public interface CollectToFriendType {
        int COLLECT = 0;
        int DONT_COLLECT = 1;
        String[] nickNames = {"选中帮抽卡", "选中不帮抽卡"};
    }

    //万能卡使用方法
    public interface UniversalCardUseType {

        /** 所有图鉴都可使用 */
        int ALL_COLLECTION = 0;

        /** 排除当前图鉴 */
        int EXCLUDE_CURRENT = 1;

        /** 优先合成进度最高的图鉴 */
        int PRIORITY_MAX_PROGRESS = 2;

        String[] nickNames = {
                "所有图鉴",
                "除当前图鉴",
                "优先合成进度最高"
        };
    }
}
