package fansirsqi.xposed.sesame.task.greenFinance;

import static fansirsqi.xposed.sesame.task.greenFinance.GreenFinanceRpcCall.taskQuery;
import static fansirsqi.xposed.sesame.task.greenFinance.GreenFinanceRpcCall.taskTrigger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;
/**
 * @author Constanline
 * @since 2023/09/08
 */
public class GreenFinance extends ModelTask {
    private static final String TAG = GreenFinance.class.getSimpleName();
    private BooleanModelField greenFinanceLsxd;
    private BooleanModelField greenFinanceLsbg;
    private BooleanModelField greenFinanceLscg;
    private BooleanModelField greenFinanceLswl;
    private BooleanModelField greenFinanceWdxd;
    private BooleanModelField greenFinanceDonation;
    /**
     * 是否收取好友金币
     */
    private BooleanModelField greenFinancePointFriend;
    /**
     * 获取任务名称
     *
     * @return 绿色经营任务名称
     */
    @Override
    public String getName() {
        return "绿色经营";
    }
    /**
     * 获取任务分组
     *
     * @return 其他分组
     */
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }
    /**
     * 获取任务图标
     *
     * @return 绿色经营任务图标文件名
     */
    @Override
    public String getIcon() {
        return "GreenFinance.png";
    }
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(greenFinanceLsxd = new BooleanModelField("greenFinanceLsxd", "打卡 | 绿色行动", false));
        modelFields.addField(greenFinanceLscg = new BooleanModelField("greenFinanceLscg", "打卡 | 绿色采购", false));
        modelFields.addField(greenFinanceLsbg = new BooleanModelField("greenFinanceLsbg", "打卡 | 绿色办公", false));
        modelFields.addField(greenFinanceWdxd = new BooleanModelField("greenFinanceWdxd", "打卡 | 绿色销售", false));
        modelFields.addField(greenFinanceLswl = new BooleanModelField("greenFinanceLswl", "打卡 | 绿色物流", false));
        modelFields.addField(greenFinancePointFriend = new BooleanModelField("greenFinancePointFriend", "收取 | 好友金币", false));
        modelFields.addField(greenFinanceDonation = new BooleanModelField("greenFinanceDonation", "捐助 | 快过期金币", false));
        return modelFields;
    }
    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行绿色经营任务
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
     * 执行绿色经营任务的主要逻辑
     */
    @Override
    public void  run() {
        try {
            Log.record(TAG,"执行开始-" + getName());
            String s = GreenFinanceRpcCall.greenFinanceIndex();
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.runtime(TAG, jo.optString("resultDesc"));
                return;
            }
            JSONObject result = jo.getJSONObject("result");
            if (!result.getBoolean("greenFinanceSigned")) {
                Log.other("绿色经营📊未开通");
                return;
            }
            JSONObject mcaGreenLeafResult = result.getJSONObject("mcaGreenLeafResult");
            JSONArray greenLeafList = mcaGreenLeafResult.getJSONArray("greenLeafList");
            String currentCode = "";
            JSONArray bsnIds = new JSONArray();
            for (int i = 0; i < greenLeafList.length(); i++) {
                JSONObject greenLeaf = greenLeafList.getJSONObject(i);
                String code = greenLeaf.getString("code");
                if (currentCode.equals(code) || bsnIds.length() == 0) {
                    bsnIds.put(greenLeaf.getString("bsnId"));
                } else {
                    batchSelfCollect(bsnIds);
                    bsnIds = new JSONArray();
                }
            }
            if (bsnIds.length() > 0) {
                batchSelfCollect(bsnIds);
            }
            signIn("PLAY102632271");
//            signIn("PLAY102932217");
            signIn("PLAY102232206");
            //执行打卡
            behaviorTick();
            //捐助
            donation();
            //收好友金币
            batchStealFriend();
            //评级奖品
            prizes();
            //绿色经营
            doTask("AP13159535", TAG, "绿色经营📊");
            GlobalThreadPools.sleep(500);
        } catch (Throwable th) {
            Log.runtime(TAG, "index err:");
            Log.printStackTrace(TAG, th);
        }finally {
            Log.record(TAG,"执行结束-" + getName());
        }
    }
    /**
     * 公共做任务
     * 使用taskQuery查询任务，taskTrigger触发任务（根据taskProcessStatus状态，报名signup->完成send->领奖receive）
     *
     * @param appletId appletId
     * @param tag 类名
     * @param name 中文说明
     */
    public static void doTask(String appletId, String tag, String name) {
        try {
            String s = taskQuery(appletId);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.runtime(tag + ".doTask.taskQuery", jo.optString("resultDesc"));
                return;
            }
            JSONObject result = jo.getJSONObject("result");
            JSONArray taskDetailList = result.getJSONArray("taskDetailList");
            for (int i = 0; i < taskDetailList.length(); i++) {
                JSONObject taskDetail = taskDetailList.getJSONObject(i);
                //EVENT_TRIGGER、USER_TRIGGER
                String type = taskDetail.getString("sendCampTriggerType");
                if (!"USER_TRIGGER".equals(type) && !"EVENT_TRIGGER".equals(type)) {
                    continue;
                }
                String status = taskDetail.getString("taskProcessStatus");
                String taskId = taskDetail.getString("taskId");
                if ("TO_RECEIVE".equals(status)) {
                    //领取奖品，任务待领奖
                    s = taskTrigger(taskId, "receive", appletId);
                    jo = new JSONObject(s);
                    if (!jo.optBoolean("success")) {
                        Log.runtime(tag + ".doTask.receive", jo.optString("resultDesc"));
                        continue;
                    }
                } else if ("NONE_SIGNUP".equals(status)) {
                    //没有报名的，先报名，再完成
                    s = taskTrigger(taskId, "signup", appletId);
                    jo = new JSONObject(s);
                    if (!jo.optBoolean("success")) {
                        Log.runtime(tag + ".doTask.signup", jo.optString("resultDesc"));
                        continue;
                    }
                }
                if ("SIGNUP_COMPLETE".equals(status) || "NONE_SIGNUP".equals(status)) {
                    //已报名，待完成，去完成
                    s = taskTrigger(taskId, "send", appletId);
                    jo = new JSONObject(s);
                    if (!jo.optBoolean("success")) {
                        Log.runtime(tag + ".doTask.send", jo.optString("resultDesc"));
                        continue;
                    }
                } else if (!"TO_RECEIVE".equals(status)) {
                    continue;
                }
                //RECEIVE_SUCCESS一次性已完成的
                Log.other(name + "[" + JsonUtil.getValueByPath(taskDetail, "taskExtProps.TASK_MORPHO_DETAIL.title") + "]任务完成");
            }
        } catch (Throwable th) {
            Log.runtime(tag, "doTask err:");
            Log.printStackTrace(tag, th);
        }
    }
    /**
     * 批量收取
     *
     * @param bsnIds Ids
     */
    private void batchSelfCollect(final JSONArray bsnIds) {
        String s = GreenFinanceRpcCall.batchSelfCollect(bsnIds);
        try {
            JSONObject joSelfCollect = new JSONObject(s);
            if (joSelfCollect.optBoolean("success")) {
                int totalCollectPoint = joSelfCollect.getJSONObject("result").getInt("totalCollectPoint");
                Log.other("绿色经营📊收集获得" + totalCollectPoint);
            } else {
                Log.runtime(TAG + ".batchSelfCollect", joSelfCollect.optString("resultDesc"));
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "batchSelfCollect err:");
            Log.printStackTrace(TAG, th);
        }
    }
    /**
     * 签到
     *
     * @param sceneId sceneId
     */
    private void signIn(final String sceneId) {
        try {
            String s = GreenFinanceRpcCall.signInQuery(sceneId);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.runtime(TAG + ".signIn.signInQuery", jo.optString("resultDesc"));
                return;
            }
            JSONObject result = jo.getJSONObject("result");
            if (result.getBoolean("isTodaySignin")) {
                return;
            }
            s = GreenFinanceRpcCall.signInTrigger(sceneId);
            GlobalThreadPools.sleep(300);
            jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                Log.other("绿色经营📊签到成功");
            } else {
                Log.runtime(TAG + ".signIn.signInTrigger", jo.optString("resultDesc"));
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "signIn err:");
            Log.printStackTrace(TAG, th);
        }
    }
    /**
     * 打卡
     */
    private void behaviorTick() {
        //绿色行动
        if (greenFinanceLsxd.getValue()) {
            doTick("lsxd");
        }
        //绿色采购
        if (greenFinanceLscg.getValue()) {
            doTick("lscg");
        }
        //绿色物流
        if (greenFinanceLswl.getValue()) {
            doTick("lswl");
        }
        //绿色办公
        if (greenFinanceLsbg.getValue()) {
            doTick("lsbg");
        }
        //绿色销售
        if (greenFinanceWdxd.getValue()) {
            doTick("wdxd");
        }
    }
    /**
     * 打卡绿色行为
     *
     * @param type 打开类型
     */
    private void doTick(final String type) {
        try {
            String str = GreenFinanceRpcCall.queryUserTickItem(type);
            JSONObject jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.runtime(TAG + ".doTick.queryUserTickItem", jsonObject.optString("resultDesc"));
                return;
            }
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                if ("Y".equals(jsonObject.getString("status"))) {
                    continue;
                }
                str = GreenFinanceRpcCall.submitTick(type, jsonObject.getString("behaviorCode"));
                GlobalThreadPools.sleep(1500);
                JSONObject object = new JSONObject(str);
                if (!object.optBoolean("success")
                        || !String.valueOf(true).equals(JsonUtil.getValueByPath(object, "result.result"))) {
                    Log.other("绿色经营📊[" + jsonObject.getString("title") + "]打卡失败");
                    break;
                }
                Log.other("绿色经营📊[" + jsonObject.getString("title") + "]打卡成功");
//                ThreadUtil.sleep(executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "doTick err:");
            Log.printStackTrace(TAG, th);
        }
    }
    /**
     * 捐助
     */
    private void donation() {
        if (!greenFinanceDonation.getValue()) {
            return;
        }
        try {
            String str = GreenFinanceRpcCall.queryExpireMcaPoint(1);
            GlobalThreadPools.sleep(300);
            JSONObject jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.runtime(TAG + ".donation.queryExpireMcaPoint", jsonObject.optString("resultDesc"));
                return;
            }
            String strAmount = JsonUtil.getValueByPath(jsonObject, "result.expirePoint.amount");
            if (strAmount.isEmpty() || !strAmount.matches("-?\\d+(\\.\\d+)?")) {
                return;
            }
            double amount = Double.parseDouble(strAmount);
            if (amount <= 0) {
                return;
            }
            //不管是否可以捐小于非100的倍数了，，第一次捐200，最后按amount-200*n
            Log.other("绿色经营📊1天内过期的金币[" + amount + "]");
            str = GreenFinanceRpcCall.queryAllDonationProjectNew();
            GlobalThreadPools.sleep(300);
            jsonObject = new JSONObject(str);
            if (!jsonObject.optBoolean("success")) {
                Log.runtime(TAG + ".donation.queryAllDonationProjectNew", jsonObject.optString("resultDesc"));
                return;
            }
            JSONArray result = jsonObject.getJSONArray("result");
            TreeMap<String, String> dicId = new TreeMap<>();
            for (int i = 0; i < result.length(); i++) {
                jsonObject = (JSONObject) JsonUtil.getValueByPathObject(result.getJSONObject(i),
                        "mcaDonationProjectResult.[0]");
                if (jsonObject == null) {
                    continue;
                }
                String pId = jsonObject.optString("projectId");
                if (pId.isEmpty()) {
                    continue;
                }
                dicId.put(pId, jsonObject.optString("projectName"));
            }
            int[] r = calculateDeductions((int) amount, dicId.size());
            String am = "200";
            for (int i = 0; i < r[0]; i++) {
                String id = new ArrayList<>(dicId.keySet()).get(i);
                String name = dicId.get(id);
                if (i == r[0] - 1) {
                    am = String.valueOf(r[1]);
                }
                str = GreenFinanceRpcCall.donation(id, am);
                GlobalThreadPools.sleep(1000);
                jsonObject = new JSONObject(str);
                if (!jsonObject.optBoolean("success")) {
                    Log.runtime(TAG + ".donation." + id, jsonObject.optString("resultDesc"));
                    return;
                }
                Log.other("绿色经营📊成功捐助[" + name + "]" + am + "金币");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "donation err:");
            Log.printStackTrace(TAG, th);
        }
    }
    /**
     * 评级奖品
     */
    private void prizes() {
    try {
        if (Status.canGreenFinancePrizesMap()) {
            return;
        }
        String campId = "CP14664674";
        String str = GreenFinanceRpcCall.queryPrizes(campId);
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
            Log.runtime(TAG + ".prizes.queryPrizes", jsonObject.optString("resultDesc"));
            return;
        }
        JSONArray prizes = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "result.prizes");
        if (prizes != null) {
            for (int i = 0; i < prizes.length(); i++) {
                jsonObject = prizes.getJSONObject(i);
                String bizTime = jsonObject.getString("bizTime");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date dateTime = formatter.parse(bizTime);
                if (TimeUtil.getWeekNumber(dateTime) == TimeUtil.getWeekNumber(new Date())) {
                    Status.greenFinancePrizesMap();
                    return;
                }
            }
        }
        str = GreenFinanceRpcCall.campTrigger(campId);
        jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
            Log.runtime(TAG + ".prizes.campTrigger", jsonObject.optString("resultDesc"));
            return;
        }
        JSONObject object = (JSONObject) JsonUtil.getValueByPathObject(jsonObject, "result.prizes.[0]");
        if (object == null) {
            return;
        }
        Log.other("绿色经营🍬评级奖品[" + object.getString("prizeName") + "]" + object.getString("price"));
    } catch (Throwable th) {
        Log.runtime(TAG, "prizes err:");
        Log.printStackTrace(TAG, th);
    }
}
    /**
     * 收好友金币
     */
    private void batchStealFriend() {
        try {
            if (Status.canGreenFinancePointFriend() || !greenFinancePointFriend.getValue()) {
                return;
            }
            int n = 0;
            while (true) {
                try {
                    String str = GreenFinanceRpcCall.queryRankingList(n);
                    GlobalThreadPools.sleep(1500);
                    JSONObject jsonObject = new JSONObject(str);
                    if (!jsonObject.optBoolean("success")) {
                        Log.other("绿色经营🙋好友金币巡查失败");
                        break;
                    }
                    JSONObject result = jsonObject.getJSONObject("result");
                    if (result.getBoolean("lastPage")) {
                        Log.other("绿色经营🙋好友金币巡查完成");
                        Status.greenFinancePointFriend();
                        return;
                    }
                    n = result.getInt("nextStartIndex");
                    JSONArray list = result.getJSONArray("rankingList");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject object = list.getJSONObject(i);
                        if (!object.getBoolean("collectFlag")) {
                            continue;
                        }
                        String friendId = object.optString("uid");
                        if (friendId.isEmpty()) {
                            continue;
                        }
                        str = GreenFinanceRpcCall.queryGuestIndexPoints(friendId);
                        GlobalThreadPools.sleep(1000);
                        jsonObject = new JSONObject(str);
                        if (!jsonObject.optBoolean("success")) {
                            Log.runtime(TAG + ".batchStealFriend.queryGuestIndexPoints", jsonObject.optString("resultDesc"));
                            continue;
                        }
                        JSONArray points = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "result.pointDetailList");
                        if (points == null) {
                            continue;
                        }
                        JSONArray jsonArray = new JSONArray();
                        for (int j = 0; j < points.length(); j++) {
                            jsonObject = points.getJSONObject(j);
                            if (!jsonObject.getBoolean("collectFlag")) {
                                jsonArray.put(jsonObject.getString("bsnId"));
                            }
                        }
                        if (jsonArray.length() == 0) {
                            continue;
                        }
                        str = GreenFinanceRpcCall.batchSteal(jsonArray, friendId);
                        GlobalThreadPools.sleep(1000);
                        jsonObject = new JSONObject(str);
                        if (!jsonObject.optBoolean("success")) {
                            Log.runtime(TAG + ".batchStealFriend.batchSteal", jsonObject.optString("resultDesc"));
                            continue;
                        }
                        Log.other("绿色经营🤩收[" + object.optString("nickName") + "]" +
                                JsonUtil.getValueByPath(jsonObject, "result.totalCollectPoint") + "金币");
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                    break;
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "batchStealFriend err:");
            Log.printStackTrace(TAG, th);
        }
    }
    /**
     * 计算次数和金额
     *
     * @param amount        最小金额
     * @param maxDeductions 最大次数
     * @return [次数，最后一次的金额]
     */
    private int[] calculateDeductions(int amount, int maxDeductions) {
        if (amount < 200) {
            // 小于 200 时特殊处理
            return new int[]{1, 200};
        }
        // 实际扣款次数，不能超过最大次数
        int actualDeductions = Math.min(maxDeductions, (int) Math.ceil((double) (amount) / 200));
        // 剩余金额
        int remainingAmount = amount - actualDeductions * 200;
        // 调整剩余金额为 100 的倍数，且不小于 200
        if (remainingAmount % 100 != 0) {
            // 向上取整到最近的 100 倍数
            remainingAmount = ((remainingAmount + 99) / 100) * 100;
        }
        if (remainingAmount < 200) {
            remainingAmount = 200;
        }
        // 如果调整后的剩余金额需要扣除更多次数，则调整实际扣款次数
        if (remainingAmount < amount - actualDeductions * 200) {
            actualDeductions = (amount - remainingAmount) / 200;
        }
        return new int[]{actualDeductions, remainingAmount};
    }
}