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

import org.json.JSONException;
import java.util.ArrayList;
import fansirsqi.xposed.sesame.data.DataCache;
import java.util.List;

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
    private BooleanModelField collectToFriend; // 是否帮好友抽卡
    private ChoiceModelField collectToFriendType; // 帮好友抽卡的类型选择
    private SelectModelField collectToFriendList; // 帮抽卡的好友列表
    private SelectModelField sendFriendCard; // 送卡片的好友列表
    private BooleanModelField useProp; // 是否使用道具
    private BooleanModelField usePropCollectTimes7Days; // 是否使用抽卡道具
    private BooleanModelField usePropCollectHistoryAnimal7Days; // 是否使用抽历史卡道具
    private BooleanModelField usePropCollectToFriendTimes7Days; // 是否使用抽好友卡道具
    private BooleanModelField autoGenerateBook; // 是否自动合成图鉴
    
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectToFriend = new BooleanModelField("collectToFriend", "帮抽卡 | 开启", false));
        modelFields.addField(collectToFriendType = new ChoiceModelField("collectToFriendType", "帮抽卡 | 动作", CollectToFriendType.COLLECT, CollectToFriendType.nickNames));
        modelFields.addField(collectToFriendList = new SelectModelField("collectToFriendList", "帮抽卡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(sendFriendCard = new SelectModelField("sendFriendCard", "送卡片好友列表(当前图鉴所有卡片)", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(useProp = new BooleanModelField("useProp", "使用道具 | 所有", false));
        modelFields.addField(usePropCollectTimes7Days = new BooleanModelField("usePropCollectTimes7Days", "使用道具 | 抽卡道具", false));
        modelFields.addField(usePropCollectHistoryAnimal7Days = new BooleanModelField("usePropCollectHistoryAnimal7Days", "使用道具 | 抽历史卡道具", false));
        modelFields.addField(usePropCollectToFriendTimes7Days = new BooleanModelField("usePropCollectToFriendTimes7Days", "使用道具 | 抽好友卡道具", false));
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
            Log.runtime(TAG, "start.run err:");
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
                Log.runtime(TAG, jo.getString("resultDesc"));
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
                            Log.runtime(TAG, jo.getString("resultDesc"));
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
                Log.runtime(TAG, jo.getString("resultDesc"));
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
                            Log.runtime(joAward.toString()); // 打印奖励响应
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
    private void propList() {
        try {
            th:
            do {
                JSONObject jo = new JSONObject(AntDodoRpcCall.propList());
                if (ResChecker.checkRes(TAG + "获取道具列表失败:", jo)) {
                    JSONArray propList = jo.getJSONObject("data").optJSONArray("propList");
                    if (propList == null) {
                        return;
                    }
                    for (int i = 0; i < propList.length(); i++) {
                        JSONObject prop = propList.getJSONObject(i);
                        String propType = prop.getString("propType");
                        boolean usePropType = isUsePropType(propType);
                        if (!usePropType) {
                            continue;
                        }
                        JSONArray propIdList = prop.getJSONArray("propIdList");
                        String propId = propIdList.getString(0);
                        String propName = prop.getJSONObject("propConfig").getString("propName");
                        int holdsNum = prop.optInt("holdsNum", 0);
                        jo = new JSONObject(AntDodoRpcCall.consumeProp(propId, propType));
                        GlobalThreadPools.sleep(300);
                        if (!ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                            Log.record(jo.getString("resultDesc"));
                            Log.runtime(jo.toString());
                            continue;
                        }
                        if ("COLLECT_TIMES_7_DAYS".equals(propType)) {
                            JSONObject useResult = jo.getJSONObject("data").getJSONObject("useResult");
                            JSONObject animal = useResult.getJSONObject("animal");
                            String ecosystem = animal.getString("ecosystem");
                            String name = animal.getString("name");
                            Log.forest("使用道具🎭[" + propName + "]#" + ecosystem + "-" + name);
                            Set<String> map = sendFriendCard.getValue();
                            for (String userId : map) {
                                if (!UserMap.getCurrentUid().equals(userId)) {
                                    int fantasticStarQuantity = animal.optInt("fantasticStarQuantity", 0);
                                    if (fantasticStarQuantity == 3) {
                                        sendCard(animal, userId); // 如果是3星卡片，发送给好友
                                    }
                                    break;
                                }
                            }
                        } else {
                            Log.forest("使用道具🎭[" + propName + "]");
                        }
                        if (holdsNum > 1) {
                            continue th; // 如果还有更多道具，继续循环
                        }
                    }
                }
                break;
            } while (true);
        } catch (Throwable th) {
            Log.runtime(TAG, "AntDodo PropList err:");
            Log.printStackTrace(TAG, th);
        }
    }
    
    /**
     * 判断是否存在使用道具类型
     * @param propType 道具类型
     * @return 是否使用该道具
     */
    private boolean isUsePropType(String propType) {
        boolean usePropType = useProp.getValue();
        usePropType = switch (propType) {
            case "COLLECT_TIMES_7_DAYS" -> usePropType || usePropCollectTimes7Days.getValue();
            case "COLLECT_HISTORY_ANIMAL_7_DAYS" -> usePropType || usePropCollectHistoryAnimal7Days.getValue();
            case "COLLECT_TO_FRIEND_TIMES_7_DAYS" -> usePropType || usePropCollectToFriendTimes7Days.getValue();
            case "UNIVERSAL_CARD_7_DAYS" -> false; // 万能卡，需要指定动物ID，应手动执行，所以跳过
            default -> usePropType;
        };
        return usePropType;
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
                Log.runtime(TAG, jo.getString("resultDesc"));
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
                Log.runtime(TAG, "神奇物种帮好友抽卡失败："+jo.getString("resultDesc"));
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
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "AntDodo CollectHelpFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    
    /**
     * 自动合成图鉴
     * 查询已集齐的图鉴，自动合成勋章
     */
    private void autoGenerateBook() {
        try {
            boolean hasMore;
            int pageSize = 18; // 固定每页请求数量
            int pageStart = 0; // 初始起始页
            do {
                // 调用接口，传入 pageSize 和 pageStart
                JSONObject jo = new JSONObject(AntDodoRpcCall.queryBookList(pageSize, String.valueOf(pageStart)));
                if (!ResChecker.checkRes(TAG + "查询图鉴列表失败:", jo)) {
                    break;
                }
                jo = jo.getJSONObject("data");
                hasMore = jo.getBoolean("hasMore"); // 是否有下一页
                JSONArray bookForUserList = jo.getJSONArray("bookForUserList");
                for (int i = 0; i < bookForUserList.length(); i++) {
                    JSONObject bookItem = bookForUserList.getJSONObject(i);
                    if (!"CAN_GENERATE".equals(bookItem.optString("medalGenerationStatus"))) {
                        continue; // 如果图鉴未集齐，跳过
                    }
                    JSONObject animalBookResult = bookItem.getJSONObject("animalBookResult");
                    String bookId = animalBookResult.getString("bookId");
                    String ecosystem = animalBookResult.getString("ecosystem");
                    JSONObject genResp = new JSONObject(AntDodoRpcCall.generateBookMedal(bookId));
                    if (!ResChecker.checkRes(TAG, genResp)) {
                        Log.error(TAG, "合成勋章失败: " + bookId);
                        continue; // 失败就跳过当前书籍
                    }
                    Log.forest("神奇物种🦕合成勋章[" + ecosystem + "]");
                }
                pageStart += pageSize; // 更新下一页起始
            } while (hasMore);
        } catch (Throwable t) {
            Log.runtime(TAG, "generateBookMedal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    
    /**
     * 帮好友抽卡类型枚举
     */
    public interface CollectToFriendType {
        int COLLECT = 0; // 选中帮抽卡
        int DONT_COLLECT = 1; // 选中不帮抽卡
        String[] nickNames = {"选中帮抽卡", "选中不帮抽卡"};
    }
}
