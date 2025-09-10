package fansirsqi.xposed.sesame.task.antForest;

import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard;
import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.data.Statistics;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity;
import fansirsqi.xposed.sesame.entity.KVMap;
import fansirsqi.xposed.sesame.entity.OtherEntityProvider;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.entity.VitalityStore;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.hook.Toast;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.FixedOrRangeIntervalLimit;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.Average;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;
/// lzw add begin
import fansirsqi.xposed.sesame.model.modelFieldExt.PriorityModelField;
import fansirsqi.xposed.sesame.util.TimeCounter;
import java.text.SimpleDateFormat;
import java.util.*;
/// lzw add end
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit;

import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard;
import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield;
import java.util.HashMap;

/**
 * 蚂蚁森林V2
 */
public class AntForest extends ModelTask {
    public static final String TAG = AntForest.class.getSimpleName();

    private static final Average offsetTimeMath = new Average(5);

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private String selfId;
    private Integer tryCountInt;
    private Integer retryIntervalInt;
    private Integer advanceTimeInt;
    /**
     * 执行间隔-分钟
     */
    // private Integer checkIntervalInt;
    // private FixedOrRangeIntervalLimit collectIntervalEntity;
    // private FixedOrRangeIntervalLimit doubleCollectIntervalEntity;
    private IntervalLimit collectIntervalEntity;
    private IntervalLimit doubleCollectIntervalEntity;
    /**
     * 双击卡结束时间
     */
    private volatile long doubleEndTime = 0;
    /**
     * 隐身卡结束时间
     */
    private volatile long stealthEndTime = 0;
    /**
     * 保护罩结束时间
     */
    private volatile long shieldEndTime = 0;
    /**
     * 炸弹卡结束时间
     */
    private volatile long energyBombCardEndTime = 0;
    /**
     * 1.1倍能量卡结束时间
     */
    private volatile long robExpandCardEndTime = 0;
    /// lzw add begin
    private volatile boolean _is_monday = false;
    /// lzw add end

    private final Average delayTimeMath = new Average(5);
    // private final ObjReference<Long> collectEnergyLockLimit = new ObjReference<>(0L);
    private final AtomicLong collectEnergyLockLimit = new AtomicLong(0L);
    private final Object doubleCardLockObj = new Object();
    
    /**
     * 一小时毫秒数
     */
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;
    /**
     * 一天毫秒数
     */
    private static final long ONE_DAY = 24 * ONE_HOUR_MS;
    /** 保护罩续写阈值（HHmm），例如 2355 表示 23小时55分 */
    private static final int SHIELD_RENEW_THRESHOLD_HHMM = 2359;
    
    private BooleanModelField expiredEnergy; // 收取过期能量
    private PriorityModelField collectEnergy;
    private BooleanModelField energyRain;
    private IntegerModelField advanceTime;
    private IntegerModelField tryCount;
    private IntegerModelField retryInterval;
    private SelectModelField dontCollectList;
    private BooleanModelField collectWateringBubble;
    private BooleanModelField batchRobEnergy;
    private BooleanModelField balanceNetworkDelay;
    private BooleanModelField closeWhackMole;
    private StringModelField queryInterval;
    private StringModelField collectInterval;
    private StringModelField doubleCollectInterval;
    private ChoiceModelField doubleCard; // 双击卡
    private ListModelField.ListJoinCommaToStringModelField doubleCardTime; // 双击卡时间
    @Getter
    private IntegerModelField doubleCountLimit; // 双击卡次数限制
    private BooleanModelField doubleCardConstant; // 双击卡永动机
    private ChoiceModelField stealthCard; // 隐身卡
    private BooleanModelField stealthCardConstant; // 隐身卡永动机
    private ChoiceModelField shieldCard; // 保护罩
    private BooleanModelField shieldCardConstant;// 限时保护永动机
    private ChoiceModelField helpFriendCollectType;
    private SelectModelField helpFriendCollectList;
    /// lzw add begin	
    private SelectModelField alternativeAccountList;
    // 显示背包内容
    private BooleanModelField showBagList;
    /// lzw add end
    private SelectAndCountModelField vitalityExchangeList;
    private IntegerModelField returnWater33;
    private IntegerModelField returnWater18;
    private IntegerModelField returnWater10;
    private PriorityModelField receiveForestTaskAward;
    private SelectAndCountModelField waterFriendList;
    private IntegerModelField waterFriendCount;
    public static SelectModelField giveEnergyRainList; //能量雨赠送列表
    private PriorityModelField vitalityExchange;
    private PriorityModelField userPatrol;
    private BooleanModelField collectGiftBox;
    private PriorityModelField medicalHealth; //医疗健康开关
    public static SelectModelField medicalHealthOption; //医疗健康选项
    private PriorityModelField ForestMarket;
    private PriorityModelField combineAnimalPiece;
    private PriorityModelField consumeAnimalProp;
    private SelectModelField whoYouWantToGiveTo;
    // private PriorityModelField dailyCheckIn;//青春特权签到
    private BooleanModelField dailyCheckIn;//青春特权签到
    // private PriorityModelField youthPrivilege;//青春特权 森林道具
    private BooleanModelField youthPrivilege;//青春特权 森林道具
    private ChoiceModelField bubbleBoostCard;//加速卡
    public static SelectModelField ecoLifeOption;
    private PriorityModelField ecoLife;
    private PriorityModelField giveProp;

    private ChoiceModelField robExpandCard;//1.1倍能量卡
    private ListModelField robExpandCardTime; //1.1倍能量卡时间
    private BooleanModelField pkEnergy; // PK能量
    private PriorityModelField collectProp;
    // private BooleanModelField collectProp;
    private IntegerModelField cycleinterval; // 循环间隔
    
    /**
     * 异常返回检测开关
     **/
    private static Boolean errorWait = false;
    public static BooleanModelField ecoLifeOpen;
    private BooleanModelField energyRainChance;
    /**
     * 能量炸弹卡
     */
    private ChoiceModelField energyBombCardType;

    private final Map<String, String> cacheCollectedMap = new HashMap<>();

    /**
     * 空森林缓存，用于记录在本轮任务中已经确认没有能量的好友。
     * 在每轮蚂蚁森林任务开始时清空（见run方法finally块）。
     * “一轮任务”通常指由"执行间隔"触发的一次完整的好友遍历。
     */
    private final Map<String, Long> emptyForestCache = new ConcurrentHashMap<>();

    /**
     * 加速器定时
     */
    private ListModelField.ListJoinCommaToStringModelField bubbleBoostTime;

    // private BooleanModelField forestChouChouLe;//森林抽抽乐
    private PriorityModelField forestChouChouLe;
    
    private static boolean canConsumeAnimalProp;
    private static int totalCollected = 0;
    private static int totalHelpCollected = 0;
    private static int totalWatered = 0;
    // private static final int totalHelpCollected = 0;
    // private static final int totalWatered = 0;

    private final Map<String, AtomicInteger> forestTaskTryCount = new ConcurrentHashMap<>();

    @Getter
    // private Set<String> dontCollectMap = new HashSet<>();
    private Set<String> dsontCollectMap = new HashSet<>();
    ArrayList<String> emojiList = new ArrayList<>(Arrays.asList(
            "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
            "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
            "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
            "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
            "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
            "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
            "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
            "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥"
    ));
    private final Random random = new Random();

    @Override
    public String getName() {
        return "森林";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "AntForest.png";
    }

    private static final int MAX_BATCH_SIZE = 6;

    @SuppressWarnings("unused")
    public interface applyPropType {
        int CLOSE = 0;
        int ALL = 1;
        int ONLY_LIMIT_TIME = 2;
        String[] nickNames = {"关闭", "所有道具", "限时道具"};
    }

    public interface HelpFriendCollectType {
        int NONE = 0;
        int HELP = 1;
        int DONT_HELP = 2;
        String[] nickNames = {"关闭", "选中复活", "选中不复活"};
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectEnergy = new PriorityModelField("collectEnergy", "收集能量 | 开关", priorityType.CLOSE, priorityType.nickNames));
        modelFields.addField(batchRobEnergy = new BooleanModelField("batchRobEnergy", "一键收取 | 开关", false));
        modelFields.addField(pkEnergy = new BooleanModelField("pkEnergy", "收集PK榜森友能量 | 开关", false));
        modelFields.addField(closeWhackMole = new BooleanModelField("closeWhackMole", "自动关闭6秒拼手速 | 开关", false));
        modelFields.addField(energyRain = new BooleanModelField("energyRain", "能量雨 | 开关", false));
        modelFields.addField(dontCollectList = new SelectModelField("dontCollectList", "不收能量 | 配置列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(giveEnergyRainList = new SelectModelField("giveEnergyRainList", "赠送能量雨 | 配置列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(energyRainChance = new BooleanModelField("energyRainChance", "兑换使用能量雨次卡 | 开关", false));
        modelFields.addField(collectWateringBubble = new BooleanModelField("collectWateringBubble", "收取浇水金球 | 开关", false));
        // modelFields.addField(expiredEnergy = new BooleanModelField("expiredEnergy", "收取过期能量 | 开关", false));
        modelFields.addField(doubleCard = new ChoiceModelField("doubleCard", "双击卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(doubleCountLimit = new IntegerModelField("doubleCountLimit", "双击卡 | 使用次数", 6));
        modelFields.addField(doubleCardTime = new ListModelField.ListJoinCommaToStringModelField("doubleCardTime", "双击卡 | 使用时间/范围", ListUtil.newArrayList(
                "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));
        modelFields.addField(doubleCardConstant = new BooleanModelField("DoubleCardConstant", "限时双击永动机 | 开关", false));

        modelFields.addField(bubbleBoostCard = new ChoiceModelField("bubbleBoostCard", "加速器开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(bubbleBoostTime = new ListModelField.ListJoinCommaToStringModelField("bubbleBoostTime", "加速器 | 使用时间/不能范围", ListUtil.newArrayList(
                "0030,0630", "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));

        modelFields.addField(shieldCard = new ChoiceModelField("shieldCard", "保护罩开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(shieldCardConstant = new BooleanModelField("shieldCardConstant", "限时保护永动机 | 开关", false));

        modelFields.addField(energyBombCardType = new ChoiceModelField("energyBombCardType", "炸弹卡开关 | 消耗类型", applyPropType.CLOSE,
                applyPropType.nickNames, "若开启了保护罩，则不会使用炸弹卡"));

        modelFields.addField(robExpandCard = new ChoiceModelField("robExpandCard", "1.1倍能量卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(robExpandCardTime = new ListModelField.ListJoinCommaToStringModelField("robExpandCardTime", "1.1倍能量卡 | 使用时间/不能范围",
                ListUtil.newArrayList("0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));

        modelFields.addField(stealthCard = new ChoiceModelField("stealthCard", "隐身卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(stealthCardConstant = new BooleanModelField("stealthCardConstant", "限时隐身永动机 | 开关", false));

        modelFields.addField(returnWater10 = new IntegerModelField("returnWater10", "返水 | 10克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater18 = new IntegerModelField("returnWater18", "返水 | 18克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater33 = new IntegerModelField("returnWater33", "返水 | 33克需收能量(关闭:0)", 0));
        modelFields.addField(waterFriendList = new SelectAndCountModelField("waterFriendList", "浇水 | 好友列表", new LinkedHashMap<>(), AlipayUser::getList, "设置浇水次数"));
        modelFields.addField(waterFriendCount = new IntegerModelField("waterFriendCount", "浇水 | 克数(10 18 33 66)", 66));
        modelFields.addField(giveProp = new PriorityModelField("giveProp", "赠送道具", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(whoYouWantToGiveTo = new SelectModelField("whoYouWantToGiveTo", "赠送 | 道具", new LinkedHashSet<>(), AlipayUser::getList, "所有可赠送的道具将全部赠"));
        modelFields.addField(collectProp = new PriorityModelField("collectProp", "收集道具", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(helpFriendCollectType = new ChoiceModelField("helpFriendCollectType", "复活能量 | 选项", HelpFriendCollectType.NONE, HelpFriendCollectType.nickNames));
        modelFields.addField(helpFriendCollectList = new SelectModelField("helpFriendCollectList", "复活能量 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        /// lzw add begin
        modelFields.addField(alternativeAccountList = new SelectModelField("alternativeAccountList", "小号列表", new LinkedHashSet<>(), AlipayUser::getList));
        /// lzw add end	
        modelFields.addField(vitalityExchange = new PriorityModelField("vitalityExchange", "活力值 | 兑换开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(vitalityExchangeList = new SelectAndCountModelField("vitalityExchangeList", "活力值 | 兑换列表", new LinkedHashMap<>(), VitalityStore::getList, "兑换次数"));
        modelFields.addField(userPatrol = new PriorityModelField("userPatrol", "保护地巡护", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(combineAnimalPiece = new PriorityModelField("combineAnimalPiece", "合成动物碎片", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(consumeAnimalProp = new PriorityModelField("consumeAnimalProp", "派遣动物伙伴", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(receiveForestTaskAward = new PriorityModelField("receiveForestTaskAward", "森林任务", priorityType.PRIORITY_2, priorityType.nickNames));

        modelFields.addField(forestChouChouLe = new PriorityModelField("forestChouChouLe", "森林寻宝任务", priorityType.PRIORITY_2, priorityType.nickNames));

        modelFields.addField(collectGiftBox = new BooleanModelField("collectGiftBox", "领取礼盒", false));

        modelFields.addField(medicalHealth = new PriorityModelField("medicalHealth", "健康医疗任务 | 开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(medicalHealthOption = new SelectModelField("medicalHealthOption", "健康医疗 | 选项", new LinkedHashSet<>(), OtherEntityProvider.listHealthcareOptions(), "医疗健康需要先完成一次医疗打卡"));

        modelFields.addField(ForestMarket = new PriorityModelField("ForestMarket", "森林集市", priorityType.PRIORITY_2, priorityType.nickNames));
        // modelFields.addField(youthPrivilege = new PriorityModelField("youthPrivilege", "青春特权 | 森林道具", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(youthPrivilege = new BooleanModelField("youthPrivilege", "青春特权 | 森林道具", false));
        // modelFields.addField(dailyCheckIn = new PriorityModelField("studentCheckIn", "青春特权 | 签到红包", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(dailyCheckIn = new BooleanModelField("studentCheckIn", "青春特权 | 签到红包", false));

        modelFields.addField(ecoLife = new PriorityModelField("ecoLife", "绿色行动 | 开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(ecoLifeOpen = new BooleanModelField("ecoLifeOpen", "绿色任务 |  自动开通", false));
        modelFields.addField(ecoLifeOption = new SelectModelField("ecoLifeOption", "绿色行动 | 选项", new LinkedHashSet<>(), OtherEntityProvider.listEcoLifeOptions(), "光盘行动需要先完成一次光盘打卡"));


        modelFields.addField(queryInterval = new StringModelField("queryInterval", "查询间隔(毫秒或毫秒范围)", "1000-2000"));
        modelFields.addField(collectInterval = new StringModelField("collectInterval", "收取间隔(毫秒或毫秒范围)", "1000-1500"));
        modelFields.addField(doubleCollectInterval = new StringModelField("doubleCollectInterval", "双击间隔(毫秒或毫秒范围)", "800-2400"));
        modelFields.addField(balanceNetworkDelay = new BooleanModelField("balanceNetworkDelay", "平衡网络延迟", true));
        modelFields.addField(advanceTime = new IntegerModelField("advanceTime", "提前时间(毫秒)", 0, Integer.MIN_VALUE, 500));
        modelFields.addField(tryCount = new IntegerModelField("tryCount", "尝试收取(次数)", 1, 0, 5));
        modelFields.addField(retryInterval = new IntegerModelField("retryInterval", "重试间隔(毫秒)", 1200, 0, 10000));
        modelFields.addField(cycleinterval = new IntegerModelField("cycleinterval", "循环间隔(毫秒)", 10000, 0, 60000));
        modelFields.addField(showBagList = new BooleanModelField("showBagList", "显示背包内容", false));
        return modelFields;
    }

    /*
    @Override
    public Boolean check() {
        if (RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime) > System.currentTimeMillis()) {
            Log.record(getName() + "任务-异常等待中，暂不执行检测！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }
    */
    @Override
    public Boolean check() {
        long currentTime = System.currentTimeMillis();

        // -----------------------------
        // 先更新时间状态，保证 IS_ENERGY_TIME 正确
        // -----------------------------
        TaskCommon.update();

        // 1️⃣ 异常等待状态
        long forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime);
        if (forestPauseTime > currentTime) {
            Log.record(getName() + "任务-异常等待中，暂不执行检测！");
            return false;
        }

        // 2️⃣ 模块休眠时间
        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        }

        // -----------------------------
        // 3️⃣ 只收能量时间段判断
        // -----------------------------
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        // boolean isEnergyTime = TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30;
        boolean isEnergyTime = TaskCommon.IS_ENERGY_TIME || 
                      hour == 0 && minute < 10 || 
                      hour == 7 && minute < 35;

        if (isEnergyTime) {
            Log.record(TAG, "⏸ 当前为只收能量时间【00:00-00:10、07:00-07:35】，开始循环收取自己、好友和PK好友的能量");

            while (true) {
                // 每次循环更新状态
                TaskCommon.update();

                // 如果不在能量时间段，退出循环
                now = Calendar.getInstance();
                hour = now.get(Calendar.HOUR_OF_DAY);
                minute = now.get(Calendar.MINUTE);
                if (!(TaskCommon.IS_ENERGY_TIME || 
                      hour == 0 && minute < 10 || 
                      hour == 7 && minute < 35)) {
                    Log.record(TAG, "当前不在只收能量时间段，退出循环");
                    break;
                }

                // 收取自己能量
                JSONObject selfHomeObj = querySelfHome();
                if (selfHomeObj != null) {
                    collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");
                }

                selfId = UserMap.getCurrentUid();
                usePropBeforeCollectEnergy(selfId); // 使用道具卡
                // 收取好友和PK好友能量
                collectFriendEnergy();
                collectPKEnergy();

                // 循环间隔
                int sleepMillis = cycleinterval.getValue();
                GlobalThreadPools.sleep(sleepMillis);
            }

            Log.record(TAG, "只收能量时间循环结束");
            return false; // 只收能量期间不执行正常任务
        }
        return true;
    }


    /*
    @Override
    public Boolean isSync() {
        return true;
    }
    */

    @Override
    public int getPriority() {
        return 1;
    }

    /**
     * 创建区间限制对象
     *
     * @param intervalStr 区间字符串，如 "1000-2000"
     * @param defaultMin 默认最小值
     * @param defaultMax 默认最大值
     * @param description 描述，用于日志
     * @return 区间限制对象
     */
    private FixedOrRangeIntervalLimit createSafeIntervalLimit(String intervalStr, int defaultMin, int defaultMax, String description) {
        // 记录原始输入值
        Log.record(TAG, description + "原始设置值: [" + intervalStr + "]");

        // 使用自定义区间限制类，处理所有边界情况
        FixedOrRangeIntervalLimit limit = new FixedOrRangeIntervalLimit(intervalStr, defaultMin, defaultMax);
        Log.record(TAG, description + "成功创建区间限制");
        return limit;
    }

    @Override
    public void boot(ClassLoader classLoader) {
        super.boot(classLoader);
        // FixedOrRangeIntervalLimit queryIntervalLimit = new FixedOrRangeIntervalLimit(queryInterval.getValue(), 200, 10000);//限制查询间隔
        // 安全创建各种区间限制
            FixedOrRangeIntervalLimit queryIntervalLimit = createSafeIntervalLimit(
            queryInterval.getValue(), 10, 10000, "查询间隔");

        // 添加RPC间隔限制
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.queryHomePage", queryIntervalLimit);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.queryFriendHomePage", queryIntervalLimit);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antmember.forest.h5.collectEnergy", 300);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antmember.forest.h5.queryEnergyRanking", 300);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.fillUserRobFlag", 500);
        // 设置其他参数
        tryCountInt = tryCount.getValue();
        retryIntervalInt = retryInterval.getValue();
        advanceTimeInt = advanceTime.getValue();
        // checkIntervalInt = BaseModel.getCheckInterval().getValue();
        // dontCollectMap = dontCollectList.getValue();
        // collectIntervalEntity = new FixedOrRangeIntervalLimit(collectInterval.getValue(), 200, 10000);//收取间隔
        // doubleCollectIntervalEntity = new FixedOrRangeIntervalLimit(doubleCollectInterval.getValue(), 200, 5000);//双击间隔
        dsontCollectMap = dontCollectList.getValue();
        
        // 创建收取间隔实体
        collectIntervalEntity = createSafeIntervalLimit(
            collectInterval.getValue(), 50, 10000, "收取间隔");

        // 创建双击收取间隔实体
        doubleCollectIntervalEntity = createSafeIntervalLimit(
            doubleCollectInterval.getValue(), 10, 5000, "双击间隔");
        delayTimeMath.clear();
        AntForestRpcCall.init();
    }

    @Override
    public void run() {
        try {
            errorWait = false;
            
            // 每次运行时检查并更新计数器
            checkAndUpdateCounters();
            // 午夜强制任务
            if (isMidnight()) {
                JSONObject selfHomeObj = querySelfHome();
                if (selfHomeObj != null) {
                    collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");  // 收自己
                }
                selfId = UserMap.getCurrentUid();
                usePropBeforeCollectEnergy(selfId); // 使用道具卡
                collectFriendEnergy(); // 好友能量收取
                collectPKEnergy(); // PK好友能量
                Log.record(TAG, "午夜任务刷新，强制执行收取PK好友能量和好友能量");
            }

            errorWait = false;

            // 计数器和时间记录
            /// lzw add begin
            //if(isMonday()) {
            //    _is_monday = true;
            //}
            _is_monday = true;
            TimeCounter tc = new TimeCounter(TAG);
            if(showBagList.getValue()) {
                showBag();
            }
            /// lzw add end

            Log.record(TAG, "执行开始-蚂蚁" + getName());
            Statistics.load();
            totalCollected = Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.COLLECTED);
            totalHelpCollected = Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.HELPED);
            totalWatered = Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.WATERED);
            
            taskCount.set(0);

            selfId = UserMap.getCurrentUid();
            usePropBeforeCollectEnergy(selfId);
            tc.countDebug("使用道具卡");

            collectPKEnergy();
            tc.countDebug("收PK榜森友能量");

            collectFriendEnergy();
            tc.countDebug("收取好友能量");

            JSONObject selfHomeObj = querySelfHome();
            tc.countDebug("获取自己主页对象信息");

            selfHomeObj = collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self"); //收取自己的能量
            tc.countDebug("收取自己的能量");

            if (selfHomeObj != null) {

                if (collectWateringBubble.getValue()) {
                    wateringBubbles(selfHomeObj);//浇水金球
                    tc.countDebug("收取浇水金球");
                }
                if (getRunCents() >= collectProp.getValue()) {
                    givenProps(selfHomeObj);//收取道具
                    tc.countDebug("收取道具");
                }
                if (getRunCents() >= userPatrol.getValue()) {
                    queryUserPatrol();//动物巡护任务[保护地巡护]
                    tc.countDebug("动物巡护任务");
                }
                //森林巡护
                if (canConsumeAnimalProp &&  (getRunCents() >= consumeAnimalProp.getValue())) {
                    queryAndConsumeAnimal();
                    tc.countDebug("森林巡护");
                } else {
                    String _msg = "已经有动物伙伴在巡护森林~";
                    Log.record(_msg);
                }

                handleUserProps(selfHomeObj);//收取动物派遣能量
                tc.countDebug("收取动物派遣能量");

                //合成动物碎片
                if (getRunCents() >= combineAnimalPiece.getValue()) {
                    queryAnimalAndPiece();
                    tc.countDebug("合成动物碎片");
                }
                //收取过期能量
//                if (expiredEnergy.getValue()) {
//                    popupTask();
//                    tc.countDebug("收取过期能量");
//                }

                //森林任务
                if (getRunCents() >= receiveForestTaskAward.getValue()) {
                    receiveTaskAward();
                    tc.countDebug("森林任务");
                }
                //绿色行动
                if (getRunCents() >= ecoLife.getValue()) {
                    EcoLife.ecoLife();
                    tc.countDebug("绿色行动");
                }
                // 浇水列表
                waterFriends();
                tc.countDebug("给好友浇水");
                //赠送道具
                if(getRunCents() >= giveProp.getValue()) {
                    giveProp();
                    tc.countDebug("赠送道具");
                }
                //活力值兑换开关
                if (getRunCents() >= vitalityExchange.getValue()) {
                    handleVitalityExchange();
                    tc.countDebug("活力值兑换");
                }
                //能量雨
                if (energyRain.getValue()) {
                    EnergyRain.energyRain();
                    if (energyRainChance.getValue()) {
                        useEnergyRainChanceCard();
                        tc.countDebug("使用能量雨卡");
                    }
                    tc.countDebug("能量雨");
                }
                // 森林集市
                if ((getRunCents() >= ForestMarket.getValue())) {
                    GreenLife.ForestMarket("GREEN_LIFE");
                    GreenLife.ForestMarket("ANTFOREST");
                    tc.countDebug("森林集市");
                }
                //医疗健康
                if (getRunCents() >= medicalHealth.getValue()) {
                    // 医疗健康 绿色医疗 16g*6能量
                    if (medicalHealthOption.getValue().contains("FEEDS")) {
                        Healthcare.queryForestEnergy("FEEDS");
                        tc.countDebug("绿色医疗");
                    }
                    // 医疗健康 电子小票 4g*10能量
                    if (medicalHealthOption.getValue().contains("BILL")) {
                        Healthcare.queryForestEnergy("BILL");
                        tc.countDebug("电子小票");
                    }
                }
                //青春特权森林道具领取
                // if (getRunCnts() >= youthPrivilege.getValue()) {
                if (youthPrivilege.getValue()) {
                    Privilege.INSTANCE.youthPrivilege();
                    tc.countDebug("青春特权森林道具领取");
                }
                //青春特权每日签到红包
                // if (getRunCnts() >= dailyCheckIn.getValue()) {
                if (dailyCheckIn.getValue()) {
                    Privilege.INSTANCE.studentSignInRedEnvelope();
                    tc.countDebug("青春特权每日签到红包");
                }
                if (getRunCents() >= forestChouChouLe.getValue()) {
                    ForestChouChouLe chouChouLe = new ForestChouChouLe();
                    chouChouLe.chouChouLe();
                    tc.countDebug("抽抽乐");
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "执行蚂蚁森林任务时发生错误: ", t);
        } finally {
            try {
                synchronized (AntForest.this) {
                    int count = taskCount.get();
                    if (count > 0) {
                        AntForest.this.wait(TimeUnit.MINUTES.toMillis(30));
                        count = taskCount.get();
                    }
                    if (count > 0) {
                        Log.record(TAG, "执行超时-蚂蚁森林");
                    } else if (count == 0) {
                        Log.record(TAG, "执行结束-蚂蚁森林");
                    } else {
                        Log.record(TAG, "执行完成-蚂蚁森林");
                    }
                }
            } catch (InterruptedException ie) {
                Log.record(TAG, "执行中断-蚂蚁森林");
            }
            Statistics.save();
            cacheCollectedMap.clear();
            // 清空本轮的空森林缓存，以便下一轮（如下次"执行间隔"到达）重新检查所有好友
            emptyForestCache.clear();
            String str_totalCollected = "本次总 收:" + totalCollected + "g 帮:" + totalHelpCollected + "g 浇:" + totalWatered + "g";
            Notify.updateLastExecText(str_totalCollected);
        }
    }

    /**
     * 每日重置
     */
    private void checkAndUpdateCounters() {
        long currentTime = System.currentTimeMillis();
        long midnight = getMidnightTime(); // 计算当前日期的午夜时间戳

        if (currentTime >= midnight) {
            // 如果时间已经过了午夜，重置计数器
            resetTaskCounters();
            Log.record(TAG, "午夜重置计数器");
        }
    }

    // 判断当前时间是否已经过午夜
    private boolean isMidnight() {
        long currentTime = System.currentTimeMillis();
        long midnightTime = getMidnightTime();
        return currentTime >= midnightTime;
    }

    // 获取午夜时间戳
    private long getMidnightTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // 重置任务计数器（你需要根据具体任务的计数器来调整）
    private void resetTaskCounters() {
        taskCount.set(0); // 重置任务计数
        Log.record(TAG, "任务计数器已重置");
    }

    /**
     * 定义一个 处理器接口
     */
    @FunctionalInterface
    private interface JsonArrayHandler {
        void handle(JSONArray array);
    }


    private void processJsonArray(JSONObject initialObj, String arrayKey, JsonArrayHandler handler) {
        boolean hasMore;
        JSONObject currentObj = initialObj;
        do {
            JSONArray jsonArray = currentObj.optJSONArray(arrayKey);
            if (jsonArray != null && jsonArray.length() > 0) {
                handler.handle(jsonArray);
                // 判断是否还有更多数据（比如返回满20个）
                hasMore = jsonArray.length() >= 20;
            } else {
                hasMore = false;
            }
            if (hasMore) {
                GlobalThreadPools.sleep(2000L); // 防止请求过快被限制
                currentObj = querySelfHome(); // 获取下一页数据
            }
        } while (hasMore);
    }

    private void wateringBubbles(JSONObject selfHomeObj) {
        processJsonArray(selfHomeObj, "wateringBubbles", this::collectWateringBubbles);
    }

    private void givenProps(JSONObject selfHomeObj) {
        processJsonArray(selfHomeObj, "givenProps", this::collectGivenProps);
    }


    /**
     * 收取回赠能量，好友浇水金秋，好友复活能量
     *
     * @param wateringBubbles 包含不同类型金球的对象数组
     */
    private void collectWateringBubbles(JSONArray wateringBubbles) {
        for (int i = 0; i < wateringBubbles.length(); i++) {
            try {
                JSONObject wateringBubble = wateringBubbles.getJSONObject(i);
                String bizType = wateringBubble.getString("bizType");
                switch (bizType) {
                    case "jiaoshui":
                        collectWater(wateringBubble);
                        break;
                    case "fuhuo":
                        collectRebornEnergy();
                        break;
                    case "baohuhuizeng":
                        collectReturnEnergy(wateringBubble);
                        break;
                    default:
                        Log.record(TAG, "未知bizType: " + bizType);
                        continue;
                }
                GlobalThreadPools.sleep(1000L);
            } catch (JSONException e) {
                Log.record(TAG, "浇水金球JSON解析错误: " + e.getMessage());
            } catch (RuntimeException e) {
                Log.record(TAG, "浇水金球处理异常: " + e.getMessage());
            }
        }
    }

    private void collectWater(JSONObject wateringBubble) {
        try {
            long id = wateringBubble.getLong("id");
            String response = AntForestRpcCall.collectEnergy("jiaoshui", selfId, id);
            processCollectResult(response, "收取金球🍯浇水");
        } catch (JSONException e) {
            Log.record(TAG, "收取浇水JSON解析错误: " + e.getMessage());
        }
    }

    private void collectRebornEnergy() {
        try {
            String response = AntForestRpcCall.collectRebornEnergy();
            processCollectResult(response, "收取金球🍯复活");
        } catch (RuntimeException e) {
            Log.record(TAG, "收取金球运行时异常: " + e.getMessage());
        }
    }

    private void collectReturnEnergy(JSONObject wateringBubble) {
        try {
            String friendId = wateringBubble.getString("userId");
            long id = wateringBubble.getLong("id");
            String response = AntForestRpcCall.collectEnergy("baohuhuizeng", selfId, id);
            processCollectResult(response, "收取金球🍯[" + UserMap.getMaskName(friendId) + "]复活回赠");
        } catch (JSONException e) {
            Log.record(TAG, "收取金球回赠JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 处理金球-浇水、收取结果
     *
     * @param response       收取结果
     * @param successMessage 成功提示信息
     */
    private void processCollectResult(String response, String successMessage) {
        try {
            JSONObject joEnergy = new JSONObject(response);
            if (ResChecker.checkRes(TAG + "收集能量失败:", joEnergy)) {
                JSONArray bubbles = joEnergy.getJSONArray("bubbles");
                if (bubbles.length() > 0) {
                    int collected = bubbles.getJSONObject(0).getInt("collectedEnergy");
                    if (collected > 0) {
                        String msg = successMessage + "[" + collected + "g]";
                        Log.forest(msg);
                        Toast.show(msg);
                        Statistics.addData(Statistics.DataType.COLLECTED, collected);
                    } else {
                        Log.record(successMessage + "失败");
                    }
                } else {
                    Log.record(successMessage + "失败: 未找到金球信息");
                }
            } else {
                Log.record(successMessage + "失败:" + joEnergy.getString("resultDesc"));
                Log.runtime(response);
            }
        } catch (JSONException e) {
            Log.runtime(TAG, "JSON解析错误: " + e.getMessage());
        } catch (Exception e) {
            Log.runtime(TAG, "处理收能量结果错误: " + e.getMessage());
        }
    }

    /**
     * 领取道具
     *
     * @param givenProps 给的道具
     */
    private void collectGivenProps(JSONArray givenProps) {
        try {
            for (int i = 0; i < givenProps.length(); i++) {
                JSONObject jo = givenProps.getJSONObject(i);
                String giveConfigId = jo.getString("giveConfigId");
                String giveId = jo.getString("giveId");
                JSONObject propConfig = jo.getJSONObject("propConfig");
                String propName = propConfig.getString("propName");
                try {
                    String response = AntForestRpcCall.collectProp(giveConfigId, giveId);
                    JSONObject responseObj = new JSONObject(response);
                    if (ResChecker.checkRes(TAG + "领取道具失败:", responseObj)) {
                        String str = "领取道具🎭[" + propName + "]";
                        Log.forest(str);
                        Toast.show(str);
                    } else {
                        Log.record(TAG, "领取道具🎭[" + propName + "]失败:" + responseObj.getString("resultDesc"));
                        Log.runtime(response);
                    }
                } catch (Exception e) {
                    Log.record(TAG, "领取道具时发生错误: " + e.getMessage());
                    Log.printStackTrace(e);
                }
                GlobalThreadPools.sleep(1000L);
            }
        } catch (JSONException e) {
            Log.record(TAG, "givenProps JSON解析错误: " + e.getMessage());
            Log.printStackTrace(e);
        }
    }

    /**
     * 处理用户派遣道具, 如果用户有派遣道具，则收取派遣动物滴能量
     *
     * @param selfHomeObj 用户主页信息的JSON对象
     */
    private void handleUserProps(JSONObject selfHomeObj) {
        try {
            JSONArray usingUserProps = selfHomeObj.optJSONArray("usingUserPropsNew");
            if (usingUserProps == null || usingUserProps.length() == 0) {
                return; // 如果没有使用中的用户道具，直接返回
            }
//            Log.runtime(TAG, "尝试遍历使用中的道具:" + usingUserProps);
            for (int i = 0; i < usingUserProps.length(); i++) {
                JSONObject jo = usingUserProps.getJSONObject(i);
                if (!"animal".equals(jo.getString("propGroup"))) {
                    continue; // 如果当前道具不是动物类型，跳过
                }
                JSONObject extInfo = new JSONObject(jo.getString("extInfo"));
                if (extInfo.optBoolean("isCollected")) {
                    Log.runtime(TAG, "动物派遣能量已被收取");
                    continue; // 如果动物能量已经被收取，跳过
                }
                canConsumeAnimalProp = false; // 设置标志位，表示不可再使用动物道具
                String propId = jo.getString("propId");
                String propType = jo.getString("propType");
                String shortDay = extInfo.getString("shortDay");
                String animalName = extInfo.getJSONObject("animal").getString("name");
                String response = AntForestRpcCall.collectAnimalRobEnergy(propId, propType, shortDay);
                JSONObject responseObj = new JSONObject(response);
                if (ResChecker.checkRes(TAG + "收取动物派遣能量失败:", responseObj)) {
                    int energy = extInfo.optInt("energy", 0);
                    totalCollected += energy;
                    String str = "收取[" + animalName + "]派遣能量🦩[" + energy + "g]";
                    Toast.show(str);
                    Log.forest(str);
                    Statistics.addData(Statistics.DataType.COLLECTED, energy);
                } else {
                    Log.record(TAG, "收取动物能量失败: " + responseObj.getString("resultDesc"));
                    Log.runtime(response);
                }
                GlobalThreadPools.sleep(300L);
                break; // 收取到一个动物能量后跳出循环
            }
        } catch (JSONException e) {
            Log.printStackTrace(e);
        } catch (Exception e) {
            Log.runtime(TAG, "handleUserProps err");
            Log.printStackTrace(e);
        }
    }

    /**
     * 给好友浇水
     */
    private void waterFriends() {
        try {
            Map<String, Integer> friendMap = waterFriendList.getValue();
            for (Map.Entry<String, Integer> friendEntry : friendMap.entrySet()) {
                String uid = friendEntry.getKey();
                if (selfId.equals(uid)) {
                    continue;
                }
                Integer waterCount = friendEntry.getValue();
                if (waterCount == null || waterCount <= 0) {
                    continue;
                }
                waterCount = Math.min(waterCount, 3);
                if (Status.canWaterFriendToday(uid, waterCount)) {
                    try {
                        String response = AntForestRpcCall.queryFriendHomePage(uid, null);
                        JSONObject jo = new JSONObject(response);
                        if (ResChecker.checkRes(TAG, jo)) {
                            String bizNo = jo.getString("bizNo");
                            KVMap<Integer, Boolean> waterCountKVNode = returnFriendWater(uid, bizNo, waterCount, waterFriendCount.getValue());
                            int actualWaterCount = waterCountKVNode.getKey();
                            if (actualWaterCount > 0) {
                                Status.waterFriendToday(uid, actualWaterCount);
                            }
                            if (Boolean.FALSE.equals(waterCountKVNode.getValue())) {
                                break;
                            }
                        } else {
                            Log.record(jo.getString("resultDesc"));
                        }
                    } catch (JSONException e) {
                        Log.runtime(TAG, "waterFriends JSON解析错误: " + e.getMessage());
                    } catch (Throwable t) {
                        Log.printStackTrace(TAG, t);
                    }
                }
            }
        } catch (Exception e) {
            Log.record(TAG, "未知错误: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    private void handleVitalityExchange() {
        try {
//            JSONObject bag = getBag();

            Vitality.initVitality("SC_ASSETS");
            Map<String, Integer> exchangeList = vitalityExchangeList.getValue();
//            Map<String, Integer> maxLimitList = vitalityExchangeMaxList.getValue();
            for (Map.Entry<String, Integer> entry : exchangeList.entrySet()) {
                String skuId = entry.getKey();
                Integer count = entry.getValue();
                if (count == null || count <= 0) {
                    Log.record(TAG, "无效的count值: skuId=" + skuId + ", count=" + count);
                    continue;
                }
                // 处理活力值兑换
                while (Status.canVitalityExchangeToday(skuId, count)) {
                    if (!Vitality.handleVitalityExchange(skuId)) {
                        Log.record(TAG, "活力值兑换失败: " + VitalityStore.getNameById(skuId));
                        break;
                    }
                    GlobalThreadPools.sleep(5000L);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "handleVitalityExchange err");
            Log.printStackTrace(TAG, t);
        }
    }

    private void notifyMain() {
        if (taskCount.decrementAndGet() < 1) {
            synchronized (AntForest.this) {
                AntForest.this.notifyAll();
            }
        }
    }

    /**
     * 获取自己主页对象信息
     *
     * @return 用户的主页信息，如果发生错误则返回null。
     */
    private JSONObject querySelfHome() {
        JSONObject userHomeObj = null;
        try {
            long start = System.currentTimeMillis();
            userHomeObj = new JSONObject(AntForestRpcCall.queryHomePage());
            updateSelfHomePage(userHomeObj);
            long end = System.currentTimeMillis();
            long serverTime = userHomeObj.getLong("now");
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.runtime(TAG, "服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return userHomeObj;
    }

    /**
     * 更新好友主页信息
     *
     * @param userId 好友ID
     * @return 更新后的好友主页信息，如果发生错误则返回null。
     */

    private JSONObject queryFriendHome(String userId, String fromAct) {
        JSONObject friendHomeObj = null;
        try {
            long start = System.currentTimeMillis();
            friendHomeObj = new JSONObject(AntForestRpcCall.queryFriendHomePage(userId, fromAct));
            long end = System.currentTimeMillis();
            long serverTime = friendHomeObj.getLong("now");
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.runtime(TAG, "服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return friendHomeObj; // 返回用户主页对象
    }


    /**
     * 格式化时间差为人性化的字符串
     *
     * @param milliseconds 时差毫秒
     */
    private String formatTimeDifference(long milliseconds) {
        long seconds = Math.abs(milliseconds) / 1000;
        String sign = milliseconds >= 0 ? "+" : "-";
        if (seconds < 60) {
            return sign + seconds + "秒";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return sign + minutes + "分钟";
        } else {
            long hours = seconds / 3600;
            return sign + hours + "小时";
        }
    }

    /**
     * 收集能量前，是否执行拼手速操作
     *
     * @return 首次收取后用户的能量信息，如果发生错误则返回null。
     */
    private JSONObject collectSelfEnergy() {
        try {

            JSONObject selfHomeObj = querySelfHome();
            if (selfHomeObj != null) {
                if (closeWhackMole.getValue()) {
                    JSONObject propertiesObject = selfHomeObj.optJSONObject("properties");
                    if (propertiesObject != null) {
                        // 如果用户主页的属性中标记了“whackMole”
                        if (Objects.equals("Y", propertiesObject.optString("whackMoleEntry"))) {
                            // 尝试关闭“6秒拼手速”功能
                            boolean success = WhackMole.closeWhackMole();
                            Log.record(success ? "6秒拼手速关闭成功" : "6秒拼手速关闭失败");
                        }
                    }
                }
                String nextAction = selfHomeObj.optString("nextAction");
                if ("WhackMole".equalsIgnoreCase(nextAction)) {
                    Log.record(TAG, "检测到6秒拼手速强制弹窗，先执行拼手速");
                    WhackMole.startWhackMole();
                }
                return collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return null;
    }


    /**
     * 收取用户的蚂蚁森林能量。
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象，包含用户的蚂蚁森林信息
     * @return 更新后的用户主页JSON对象，如果发生异常返回null
     */
    private JSONObject collectEnergy(String userId, JSONObject userHomeObj, String fromTag) {
        try {
            // 1. 检查接口返回是否成功
            if (!ResChecker.checkRes(TAG + "载入用户主页失败:", userHomeObj)) {
                Log.debug(TAG, "载入失败: " + userHomeObj.getString("resultDesc"));
                return userHomeObj;
            }
            long serverTime = userHomeObj.getLong("now");
            boolean isSelf = Objects.equals(userId, selfId);
            String userName, bizType;
            if (!fromTag.equals("pk")) {
                userName = UserMap.getMaskName(userId);
            } else {
                userName = "PK榜森友|" + userHomeObj.getJSONObject("userEnergy").getString("displayName");

            }
            bizType = "GREEN";
            if (cacheCollectedMap.containsKey(userId)) {
                Log.record(TAG, "[" + userName + "]在本轮run中已被处理，跳过");
                return userHomeObj;
            }
            // 缓存用户名，确保异步任务能获取到
            cacheCollectedMap.put(userId, userName);
            // Log.record(TAG, "进入[" + userName + "]的蚂蚁森林");
            // 3. 判断是否允许收取能量
            // if ((collectEnergy.getValue() <= 0) || dontCollectMap.contains(userId)) {
            if ((collectEnergy.getValue() <= 0) || dsontCollectMap.contains(userId)) {
                return userHomeObj;
            }

            // 4. 获取所有可收集的能量球
            List<Long> availableBubbles = new ArrayList<>();
            List<Pair<Long, Long>> waitingBubbles = new ArrayList<>();
            extractBubbleInfo(userHomeObj, serverTime, availableBubbles, waitingBubbles, userId);

            // 如果没有任何能量球（可收或待收），则标记为空林并直接返回
            if (availableBubbles.isEmpty() && waitingBubbles.isEmpty()) {
                // Log.record(TAG, "  - [" + userName + "] 白跑一趟，啥也没有，标记为空林。");
                emptyForestCache.put(userId, System.currentTimeMillis());
                return userHomeObj;
            }

            // 打印调试信息
            Log.record(TAG, "[" + userName + "] 📊能量统计: 可收取=" + availableBubbles.size() + "个, 等待成熟=" + waitingBubbles.size() + "个");
            if (!waitingBubbles.isEmpty()) {
                // Log.record(TAG, "[" + userName + "] 等待成熟的能量球列表:");
                for (Pair<Long, Long> pair : waitingBubbles) {
                    long remainingTime = pair.second() - System.currentTimeMillis();
                    // Log.record(TAG, "  🔄 bubbleId=" + pair.first()+ " 成熟时间=" + TimeUtil.getCommonDate(pair.second()) + " 剩余=" + (remainingTime / 1000) + "秒");
                }
            }
            // 5. 先添加蹲点任务（无论是否有保护罩都要蹲点，因为保护罩会过期）
            scheduleWaitingBubbles(userId, waitingBubbles, userName);
            // 6. 检查是否有能量罩保护（影响当前收取，但不影响蹲点）
            boolean hasProtection = false;
            if (!isSelf) {
                if (hasShield(userHomeObj, serverTime)) {
                    Log.record(TAG, "[" + userName + "]被能量罩🛡保护着哟，跳过当前收取但已添加蹲点");
                    hasProtection = true;
                    return userHomeObj;
                }
                if (hasBombCard(userHomeObj, serverTime)) {
                    Log.record(TAG, "[" + userName + "]开着炸弹卡💣哟，跳过当前收取但已添加蹲点");
                    hasProtection = true;
                    return userHomeObj;
                }
            }
            // 7. 只有没有保护时才收集当前可用能量
            if (!hasProtection) {
                collectVivaEnergy(userId, userHomeObj, availableBubbles, bizType);
            }
            return userHomeObj;
        } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(TAG, "collectUserEnergy JSON解析错误", e);
            return null;
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "collectUserEnergy 出现异常", t);
            return null;
        }
    }

    /**
     * 提取能量球状态
     *
     * @param userHomeObj      用户主页的JSON对象
     * @param serverTime       服务器时间
     * @param availableBubbles 可收集的能量球ID列表
     * @param waitingBubbles   等待成熟的能量球ID列表
     * @throws JSONException JSON解析异常
     */

    private void extractBubbleInfo(JSONObject userHomeObj, long serverTime, List<Long> availableBubbles, List<Pair<Long, Long>> waitingBubbles, String userId) throws JSONException {
        if (!userHomeObj.has("bubbles")) return;
        JSONArray jaBubbles = userHomeObj.getJSONArray("bubbles");
        if (jaBubbles.length() == 0) return;
        // int checkInterval = checkIntervalInt + checkIntervalInt / 2;
        for (int i = 0; i < jaBubbles.length(); i++) {
            JSONObject bubble = jaBubbles.getJSONObject(i);
            long bubbleId = bubble.getLong("id");
            long produceTime = bubble.getLong("produceTime");//成熟时间
            String statusStr = bubble.getString("collectStatus");
            CollectStatus status = CollectStatus.valueOf(statusStr);
            switch (status) {
                case AVAILABLE:
                    availableBubbles.add(bubbleId);
                    break;
                case WAITING://此处适合增加加速卡的处理，但是需要注意 需要 userid==selfId
                    // 修改逻辑：蹲所有等待成熟的能量，不再限制时间范围
                    waitingBubbles.add(new Pair<>(bubbleId, produceTime));
                    String userName = UserMap.getMaskName(userId);
                    if (userName == null) {
                        JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
                        if (userEnergy != null) {
                            userName = userEnergy.optString("displayName");
                        }
                        if (userName == null || userName.isEmpty()) {
                            userName = userId;
                        }
                    }
                    Log.debug(TAG, "用户[" + userName + "]能量id: [" + bubbleId + "]成熟时间: " + TimeUtil.getCommonDate(produceTime) + " 剩余时间: " + (produceTime - serverTime) + "ms");
                    break;
            }
        }

    }

    private record Pair<F, S>(F first, S second) {
    }

    /**
     * 调度蹲点收取 - 多线程版本
     *
     * @param userId         用户ID
     * @param waitingBubbles 等待成熟的能量球ID列表
     * @param userName       用户显示名称
     */
    private void scheduleWaitingBubbles(String userId, List<Pair<Long, Long>> waitingBubbles, String userName) {
        if (waitingBubbles.isEmpty()) {
            Log.record(TAG, "用户[" + userName + "]没有等待成熟的能量球");
            return;
        }
        
        final int bubbleCount = waitingBubbles.size();
        Log.record(TAG, "开始为用户[" + userName + "]添加" + bubbleCount + "个蹲点任务");

        // 创建计数器用于跟踪任务完成情况
        final AtomicInteger completedTasks = new AtomicInteger(0);
        final String finalUserName = userName; // 在lambda中使用的final变量
        
        // 并行处理所有等待的能量球
        for (Pair<Long, Long> pair : waitingBubbles) {
            final long bubbleId = pair.first();
            final long produceTime = pair.second();
            final String tid = AntForest.getEnergyTimerTid(userId, bubbleId);
            final long remainingTime = produceTime - System.currentTimeMillis();

            // 使用线程池异步添加任务
            GlobalThreadPools.execute(() -> {
                try {
                    if (!hasChildTask(tid)) {
                        addChildTask(new EnergyTimerTask(userId, bubbleId, produceTime));
                        Log.record(TAG,
                                "✅添加蹲点⏰ -> [" + finalUserName + "]"
                                        + " bubble=" + bubbleId
                                        + " 成熟时间/蹲守时间=" + TimeUtil.getCommonDate(produceTime)
                                        + " 剩余=" + (remainingTime / 1000) + "秒"
                                        + " tid=" + tid);
                    } else {
                        Log.record(TAG,
                                "⚠️蹲点⏰已存在 -> [" + finalUserName + "]"
                                        + " bubble=" + bubbleId
                                        + " 成熟时间/蹲守时间=" + TimeUtil.getCommonDate(produceTime)
                                        + " 剩余=" + (remainingTime / 1000) + "秒"
                                        + " tid=" + tid);
                    }
                } catch (Exception e) {
                    Log.printStackTrace(TAG, "添加蹲点任务异常: ", e);
                } finally {
                    // 任务计数增加，用于统计完成情况
                    int completed = completedTasks.incrementAndGet();
                    if (completed == bubbleCount) {
                        Log.record(TAG, "用户[" + finalUserName + "]的所有蹲点任务已并行添加完成");
                    }
                }
            });
        }
    }


    /**
     * 批量或逐一收取能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象
     * @param bubbleIds   能量球ID列表
     */
    private void collectVivaEnergy(String userId, JSONObject userHomeObj, List<Long> bubbleIds, String bizType) throws JSONException {
        if (bubbleIds.isEmpty()) return;
        boolean isBatchCollect = batchRobEnergy.getValue();
        if (isBatchCollect) {
            for (int i = 0; i < bubbleIds.size(); i += MAX_BATCH_SIZE) {
                List<Long> subList = bubbleIds.subList(i, Math.min(i + MAX_BATCH_SIZE, bubbleIds.size()));
                collectEnergy(new CollectEnergyEntity(userId, userHomeObj, AntForestRpcCall.batchEnergyRpcEntity(bizType, userId, subList)));
            }
        } else {
            for (Long id : bubbleIds) {
                collectEnergy(new CollectEnergyEntity(userId, userHomeObj, AntForestRpcCall.energyRpcEntity(bizType, userId, id)));
            }
        }
    }

    private void collectPKEnergy() {
        try {
            JSONObject pkObject = new JSONObject(AntForestRpcCall.queryTopEnergyChallengeRanking());
            if (!ResChecker.checkRes(TAG + "获取PK排行榜失败:", pkObject)) {
                Log.error(TAG, "获取PK排行榜失败: " + pkObject.optString("resultDesc"));
            } else {
                if (!pkObject.getString("rankMemberStatus").equals("JOIN")) {
                    Log.runtime(TAG, "未加入PK排行榜,跳过,尝试关闭");
                    pkEnergy.setValue(false);
                }
                collectUserEnergy(pkObject, "pk");
                //继续处理靠后的PK好友
                JSONArray totalData = pkObject.optJSONArray("totalData");
                if (totalData == null || totalData.length() == 0) {
                    Log.runtime(TAG, "pk好友排行榜为空，跳过");
                    return;
                }

                List<String> pkIdList = new ArrayList<>();
                for (int pos = 20; pos < totalData.length(); pos++) {
                    JSONObject pkFriend = totalData.getJSONObject(pos);
                    String userId = pkFriend.getString("userId");
                    if (Objects.equals(userId, selfId)) continue; //如果是自己则跳过
                    pkIdList.add(userId);
                    if (pkIdList.size() == 20) {
                        // processLastdEnergy(pkIdList, "pk");//20个id 一次处理
                        processLastEnergy(pkIdList, "pk");//20个id 一次处理
                        pkIdList.clear();
                    }
                }
                if (!pkIdList.isEmpty()) {
                    // processLastdEnergy(pkIdList, "pk");
                    processLastEnergy(pkIdList, "pk");
                }
                Log.runtime(TAG, "收取PK能量完成！");
            }
        } catch (Exception e) {
            String pkObjec = AntForestRpcCall.queryTopEnergyChallengeRanking();
            Log.error(TAG, "获取PK排行榜失败: " + pkObjec);
            Log.printStackTrace(TAG, "collectPKEnergy 异常", e);
        }
    }

    
    /**
     * 收集好友排行榜中的能量
     * <p>
     * 该方法首先获取好友排行榜，然后分批处理好友的能量：
     * 1. 先处理排名前20的好友
     * 2. 再分批处理剩余好友（每批20个）
     * 3. 包含重试机制，最多尝试3次，每次重试间隔3秒
     * </p>
     */
    private void collectFriendEnergy() {
        try {
            TimeCounter tc = new TimeCounter(TAG);
            /*
            JSONObject friendsObject = new JSONObject(AntForestRpcCall.queryFriendsEnergyRanking());
            if (!ResChecker.checkRes(TAG + "获取好友排行榜失败:", friendsObject)) {
                Log.error(TAG, "获取好友排行榜失败: " + friendsObject.optString("resultDesc"));
            */
            // 添加重试机制，最多尝试3次
            String rankingResponse = null;
            JSONObject friendsObject = null;
            boolean success = false;
            
            for (int retry = 0; retry < 3 && !success; retry++) {
                if (retry > 0) {
                    Log.record(TAG, "获取好友排行榜第" + (retry + 1) + "次尝试");
                    // 重试前等待一段时间
                    GlobalThreadPools.sleep(3000);
                }
                
                rankingResponse = AntForestRpcCall.queryFriendsEnergyRanking();
                if (StringUtil.isEmpty(rankingResponse)) {
                    Log.error(TAG, "获取好友排行榜返回为空，准备重试");
                    continue;
                }
                
                try {
                    friendsObject = new JSONObject(rankingResponse);
                    if (ResChecker.checkRes(TAG + "获取好友排行榜失败:", friendsObject)) {
                        success = true;
                        Log.record(TAG, "成功获取好友排行榜");
                    } else {
                        String errorMsg = friendsObject.optString("errorMessage", "未知错误");
                        Log.error(TAG, "获取好友排行榜失败: " + errorMsg + "，准备重试");
                    }
                } catch (JSONException e) {
                    Log.error(TAG, "解析好友排行榜JSON异常，准备重试: " + e.getMessage());
                }
            }
            
            if (!success) {
                Log.error(TAG, "获取好友排行榜失败，已重试3次，放弃");
                return;
            }
            tc.countDebug("获取好友排行榜");
            // 处理排名靠前的好友（通常自己也在其中） 20个
            Log.record(TAG, "开始处理排名靠前好友");
            collectUserEnergy(friendsObject, "");
            tc.countDebug("处理排名靠前的好友");
            // 分批处理其他好友（从第20位开始）
            /*
            JSONArray totalDatas = friendsObject.optJSONArray("totalDatas");
            if (totalDatas == null || totalDatas.length() == 0) {
                Log.runtime(TAG, "好友排行榜为空，跳过");
            */
            JSONArray totalData = friendsObject.optJSONArray("totalDatas");
            if (totalData == null || totalData.length() == 0) {
                Log.record(TAG, "好友排行榜为空，跳过");
                return;
            }
            Log.record(TAG, "开始处理其他好友，共" + totalData.length() + "个");
            List<String> idList = new ArrayList<>();
            // for (int pos = 20; pos < totalDatas.length(); pos++) {
            //     JSONObject friend = totalDatas.getJSONObject(pos);
            for (int pos = 20; pos < totalData.length(); pos++) {
                JSONObject friend = totalData.getJSONObject(pos);
                String userId = friend.getString("userId");
                if (Objects.equals(userId, selfId)) continue; //如果是自己则跳过
                idList.add(userId);
                if (idList.size() == 20) {
                    // processLastdEnergy(idList, "");//20个id 一次处理
                    processLastEnergy(idList, "");//20个id 一次处理
                    idList.clear();
                }
            }
            tc.countDebug("分批处理其他好友"+!idList.isEmpty());
            if (!idList.isEmpty()) {
                // processLastdEnergy(idList, "");
                processLastEnergy(idList, "");
            }
            tc.countDebug("分批处理其他好友空" );
            Log.record(TAG, "收取好友能量完成！");
        } catch (JSONException e) {
            String friendsObjec = AntForestRpcCall.queryFriendsEnergyRanking();
            Log.error(TAG, "解析好友排行榜 JSON 异常:"+ friendsObjec );
            Log.printStackTrace(TAG, "解析好友排行榜 JSON 异常", e);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "queryEnergyRanking 异常", t);
        }
    }


    /**
     * 收取排名靠后的能量
     * <p>
     * 该方法处理一批用户的能量收集：
     * 1. 调用fillUserRobFlag API获取用户能量信息
     * 2. 遍历每个用户并处理其能量
     * 3. 包含重试机制，最多尝试3次，每次重试间隔2秒
     * </p>
     *
     * @param userIds 用户id列表（最多20个）
     * @param flag 标志，"pk"表示PK榜好友，""表示普通好友
     */
    // private void processLastdEnergy(List<String> userIds, String flag) {
    private void processLastEnergy(List<String> userIds, String flag) {
        try {
            if (errorWait) return;
            /*
            String jsonStr;
            if (flag.equals("pk")) {
                jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds), true);
            } else {
                jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds));
            */
            // 添加重试机制，最多尝试3次
            String jsonStr = null;
            JSONObject batchObj = null;
            boolean success = false;
            
            for (int retry = 0; retry < 3 && !success; retry++) {
                if (retry > 0) {
                    Log.record(TAG, "获取好友能量信息第" + (retry + 1) + "次尝试");
                    // 重试前等待一段时间
                    GlobalThreadPools.sleep(2000);
                }
                
                try {
                    if (flag.equals("pk")) {
                        jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds), true);
                    } else {
                        jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds));
                    }
                    
                    if (StringUtil.isEmpty(jsonStr)) {
                        Log.error(TAG, "获取好友能量信息返回为空，准备重试");
                        continue;
                    }
                    
                    batchObj = new JSONObject(jsonStr);
                    success = true;
                } catch (Exception e) {
                    Log.error(TAG, "获取好友能量信息异常，准备重试: " + e.getMessage());
                }
            }
            
            if (!success) {
                Log.error(TAG, "获取好友能量信息失败，已重试3次，放弃");
                return;
            }
            // JSONObject batchObj = new JSONObject(jsonStr);
            JSONArray friendList = batchObj.optJSONArray("friendRanking");
            // if (friendList == null) return;
            if (friendList == null) {
                Log.record(TAG, "好友能量列表为空");
                return;
            }
            
            Log.record(TAG, "开始处理" + friendList.length() + "个好友的能量");
            for (int i = 0; i < friendList.length(); i++) {
                JSONObject friendObj = friendList.getJSONObject(i);
                processEnergy(friendObj, flag);
            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "解析批量好友数据失败", e);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理批量好友出错", e);
        }
    }

    /**
     * 处理单个好友 - 收能量
     * <p>
     * 该方法是能量收集的最终决策点，根据不同条件判断是否需要收取能量：
     * 1. 区分普通好友和PK好友的处理逻辑
     * 2. 检查是否需要收集能量、帮助保护或领取礼盒
     * 3. 根据条件调用相应的方法执行具体操作
     * </p>
     *
     * @param obj 好友/PK好友的JSON对象，包含能量信息
     * @param flag 标志，"pk"表示PK榜好友，""表示普通好友
     */
    private void processEnergy(JSONObject obj, String flag) {
        try {
            if (errorWait) return;
            String userId = obj.getString("userId");
            if (Objects.equals(userId, selfId)) return; // 跳过自己
            String userName = obj.optString("displayName", UserMap.getMaskName(userId));

            if (emptyForestCache.containsKey(userId)) {
                //  Log.record(TAG, "  processEnergy 用户: [" + userName + "], 本轮已知为空林，跳过");
                return;
            }
            boolean isPk = "pk".equals(flag);

            if (isPk) {
                userName = "PK榜好友|" + userName;
            }
            // Log.record(TAG, "  processEnergy 开始处理用户: [" + userName + "], 类型: " + (isPk ? "PK" : "普通"));
            if (isPk) {
                boolean needCollectEnergy = (collectEnergy.getValue() > 0) && pkEnergy.getValue();
                if (!needCollectEnergy) {
                    Log.record(TAG, "    PK好友: [" + userName + "], 不满足收取条件，跳过");
                    return;
                }

                collectEnergy(userId, queryFriendHome(userId, "PKContest"), "pk");
            } else { // 普通好友
                // boolean needCollectEnergy = (collectEnergy.getValue() > 0 ) && !dontCollectMap.contains(userId); //开启了收能量功能并且不在排除名单中
                boolean needCollectEnergy = (collectEnergy.getValue() > 0) && !dsontCollectMap.contains(userId);
                boolean needHelpProtect = helpFriendCollectType.getValue() != HelpFriendCollectType.NONE && obj.optBoolean("canProtectBubble") && Status.canProtectBubbleToday(selfId);
                // Log.forest("needHelpProtect:"+needHelpProtect+" value:"+helpFriendCollectType.getValue()+" can:"+friendObj.optBoolean("canProtectBubble")+" has:" + Status.canProtectBubbleToday(selfId));
                boolean needCollectGiftBox = collectGiftBox.getValue() && obj.optBoolean("canCollectGiftBox");
                if (!needCollectEnergy && !needHelpProtect && !needCollectGiftBox) {
                    Log.record(TAG, "    普通好友: [" + userName + "], 所有条件不满足，跳过");
                    return;
                }
                JSONObject userHomeObj = null;
                // 只要开启了收能量，就进去看看，以便添加蹲点
                if (needCollectEnergy) {
                    // 即使排行榜信息显示没有可收能量，也进去检查，以便添加蹲点任务
                    userHomeObj = collectEnergy(userId, queryFriendHome(userId, null), "friend");
                }

                if (needHelpProtect) {
                    boolean isProtected = isIsProtected(userId);
                    if (isProtected) {
                        if (userHomeObj == null) {
                            userHomeObj = queryFriendHome(userId, null);
                        }
                        if (userHomeObj != null) {
                            collectGiftBox(userHomeObj);
                        }
                    }
                }
                // 尝试领取礼物盒
                if (needCollectGiftBox) {
                    if (userHomeObj == null) {
                        userHomeObj = queryFriendHome(userId, null);
                    }
                    if (userHomeObj != null) {
                        collectGiftBox(userHomeObj);
                    }
                }
            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "处理单个好友[" + obj.optString("userId") + "]出错", e);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理好友异常", e);
        }
    }

    private boolean isIsProtected(String userId) {
        // boolean isProtected = false;
        boolean isProtected;
        // Log.forest("is_monday:"+_is_monday);
        if(_is_monday) {
            isProtected = alternativeAccountList.getValue().contains(userId);
        } else {
            isProtected = helpFriendCollectList.getValue().contains(userId);
            if (helpFriendCollectType.getValue() != HelpFriendCollectType.HELP) {
                isProtected = !isProtected;
            }
        }
        return isProtected;
    }

    /// lzw add begin
    public static boolean isMonday() {
        SimpleDateFormat sdf_week = new SimpleDateFormat("EEEE", Locale.getDefault());
        String week = sdf_week.format(new Date());
        // Log.forest("today is:" + week);
        return "星期一".equals(week);
    }
    /// lzw add end

    /**
     * 收取排名靠前好友能量
     *
     * @param friendsObject 好友列表的JSON对象
     */
    private void collectUserEnergy(JSONObject friendsObject, String flag) {
        try {
            if (errorWait) return;
            JSONArray friendRanking = friendsObject.optJSONArray("friendRanking");
            if (friendRanking == null) {
                Log.runtime(TAG, "无好友数据可处理");
                return;
            }
            for (int i = 0; i < friendRanking.length(); i++) {
                JSONObject friendObj = friendRanking.getJSONObject(i);
                processEnergy(friendObj, flag); // 处理单个好友
            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "解析好友排行榜子项失败", e);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理好友列表异常", e);
        }
    }


    private void collectGiftBox(JSONObject userHomeObj) {
        try {
            JSONObject giftBoxInfo = userHomeObj.optJSONObject("giftBoxInfo");
            JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserMap.getCurrentUid() : userEnergy.optString("userId");
            if (giftBoxInfo != null) {
                JSONArray giftBoxList = giftBoxInfo.optJSONArray("giftBoxList");
                if (giftBoxList != null && giftBoxList.length() > 0) {
                    for (int ii = 0; ii < giftBoxList.length(); ii++) {
                        try {
                            JSONObject giftBox = giftBoxList.getJSONObject(ii);
                            String giftBoxId = giftBox.getString("giftBoxId");
                            String title = giftBox.getString("title");
                            JSONObject giftBoxResult = new JSONObject(AntForestRpcCall.collectFriendGiftBox(giftBoxId, userId));
                            if (!ResChecker.checkRes(TAG + "领取好友礼盒失败:", giftBoxResult)) {
                                Log.record(giftBoxResult.getString("resultDesc"));
                                Log.runtime(giftBoxResult.toString());
                                continue;
                            }
                            int energy = giftBoxResult.optInt("energy", 0);
                            Log.forest("礼盒能量🎁[" + UserMap.getMaskName(userId) + "-" + title + "]#" + energy + "g");
                            Statistics.addData(Statistics.DataType.COLLECTED, energy);
                        } catch (Throwable t) {
                            Log.printStackTrace(t);
                            break;
                        } finally {
                            GlobalThreadPools.sleep(500L);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void protectFriendEnergy(JSONObject userHomeObj) {
        try {
            JSONArray wateringBubbles = userHomeObj.optJSONArray("wateringBubbles");
            JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserMap.getCurrentUid() : userEnergy.optString("userId");
            if (wateringBubbles != null && wateringBubbles.length() > 0) {
                for (int j = 0; j < wateringBubbles.length(); j++) {
                    try {
                        JSONObject wateringBubble = wateringBubbles.getJSONObject(j);
                        if (!"fuhuo".equals(wateringBubble.getString("bizType"))) {
                            continue;
                        }
                        if (wateringBubble.getJSONObject("extInfo").optInt("restTimes", 0) == 0) {
                            Status.protectBubbleToday(selfId);
                        }
                        if (!wateringBubble.getBoolean("canProtect")) {
                            continue;
                        }
                        JSONObject joProtect = new JSONObject(AntForestRpcCall.protectBubble(userId));
                        if (!ResChecker.checkRes(TAG + "复活能量失败:", joProtect)) {
                            Log.record(joProtect.getString("resultDesc"));
                            Log.runtime(joProtect.toString());
                            continue;
                        }
                        int vitalityAmount = joProtect.optInt("vitalityAmount", 0);
                        int fullEnergy = wateringBubble.optInt("fullEnergy", 0);
                        String str = "复活能量🚑[" + UserMap.getMaskName(userId) + "-" + fullEnergy + "g]" + (vitalityAmount > 0 ? "#活力值+" + vitalityAmount : "");
                        Log.forest(str);
                        Statistics.addData(Statistics.DataType.HELPED, fullEnergy);
                        break;
                    } catch (Throwable t) {
                        Log.printStackTrace(t);
                        break;
                    } finally {
                        GlobalThreadPools.sleep(500);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void collectEnergy(CollectEnergyEntity collectEnergyEntity) {
        if (errorWait) {
            Log.record(TAG, "异常⌛等待中...不收取能量");
            return;
        }
        Runnable runnable =
                () -> {
                    try {
                        String userId = collectEnergyEntity.getUserId();
                        usePropBeforeCollectEnergy(userId);
                        RpcEntity rpcEntity = collectEnergyEntity.getRpcEntity();
                        boolean needDouble = collectEnergyEntity.getNeedDouble();
                        boolean needRetry = collectEnergyEntity.getNeedRetry();
                        int tryCount = collectEnergyEntity.addTryCount();
                        int collected = 0;
                        long startTime;
                        synchronized (collectEnergyLockLimit) {
                            long sleep;
                            if (needDouble) {
                                collectEnergyEntity.unsetNeedDouble();
                                // sleep = doubleCollectIntervalEntity.getInterval() - System.currentTimeMillis() + collectEnergyLockLimit.get();
                                Integer interval = doubleCollectIntervalEntity.getInterval();
                                sleep = (interval != null ? interval : 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get();
                            } else if (needRetry) {
                                collectEnergyEntity.unsetNeedRetry();
                                sleep = retryIntervalInt - System.currentTimeMillis() + collectEnergyLockLimit.get();
                            } else {
                                // sleep = collectIntervalEntity.getInterval() - System.currentTimeMillis() + collectEnergyLockLimit.get();
                                Integer interval = collectIntervalEntity.getInterval();
                                sleep = (interval != null ? interval : 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get();
                            }
                            if (sleep > 0) {
                                GlobalThreadPools.sleep(sleep);
                            }
                            startTime = System.currentTimeMillis();
                            collectEnergyLockLimit.set(startTime);
                        }
                        RequestManager.requestString(rpcEntity, 0, 0);
                        long spendTime = System.currentTimeMillis() - startTime;
                        if (balanceNetworkDelay.getValue()) {
                            delayTimeMath.nextInteger((int) (spendTime / 3));
                        }
                        if (rpcEntity.getHasError()) {
                            String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
//                            String errorCode = rpcEntity.getErrorCode();
                            if ("1004".equals(errorCode)) {
                                if (BaseModel.getWaitWhenException().getValue() > 0) {
                                    long waitTime = System.currentTimeMillis() + BaseModel.getWaitWhenException().getValue();
                                    RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime);
                                    Notify.updateStatusText("异常");
                                    Log.record(TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime));
                                    errorWait = true;
                                    return;
                                }
                                GlobalThreadPools.sleep(600 + RandomUtil.delay());
                            }
                            if (tryCount < tryCountInt) {
                                collectEnergyEntity.setNeedRetry();
                                collectEnergy(collectEnergyEntity);
                            }
                            return;
                        }
                        JSONObject jo = new JSONObject(rpcEntity.getResponseString());
                        String resultCode = jo.getString("resultCode");
                        if (!"SUCCESS".equalsIgnoreCase(resultCode)) {
                            if ("PARAM_ILLEGAL2".equals(resultCode)) {
                                Log.record(TAG, "[" + cacheCollectedMap.get(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"));
                                return;
                            }
                            Log.record(TAG, "[" + cacheCollectedMap.get(userId) + "]" + jo.getString("resultDesc"));
                            if (tryCount < tryCountInt) {
                                collectEnergyEntity.setNeedRetry();
                                collectEnergy(collectEnergyEntity);
                            }
                            return;
                        }
                        JSONArray jaBubbles = jo.getJSONArray("bubbles");
                        int jaBubbleLength = jaBubbles.length();
                        if (jaBubbleLength > 1) {
                            List<Long> newBubbleIdList = new ArrayList<>();
                            for (int i = 0; i < jaBubbleLength; i++) {
                                JSONObject bubble = jaBubbles.getJSONObject(i);
                                if (bubble.getBoolean("canBeRobbedAgain")) {
                                    newBubbleIdList.add(bubble.getLong("id"));
                                }
                                collected += bubble.getInt("collectedEnergy");
                            }
                            if (collected > 0) {
                                int randomIndex = random.nextInt(emojiList.size());
                                String randomEmoji = emojiList.get(randomIndex);
                                String str = "一键收取️" + randomEmoji + collected + "g[" + cacheCollectedMap.get(userId) + "]#";
                                totalCollected += collected;
                                if (needDouble) {
                                    Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                                    Toast.show(str + "[双击]");
                                } else {
                                    Log.forest(str + "耗时[" + spendTime + "]ms");
                                    Toast.show(str);
                                }
                                Statistics.addData(Statistics.DataType.COLLECTED, collected);
                            } else {
                                Log.record(TAG, "一键收取❌[" + cacheCollectedMap.get(userId) + "]的能量失败" + " " + "，UserID：" + userId + "，BubbleId：" + newBubbleIdList);
                            }
                            if (!newBubbleIdList.isEmpty()) {
                                collectEnergyEntity.setRpcEntity(AntForestRpcCall.batchEnergyRpcEntity("", userId, newBubbleIdList));
                                collectEnergyEntity.setNeedDouble();
                                collectEnergyEntity.resetTryCount();
                                collectEnergy(collectEnergyEntity);
                            }
                        } else if (jaBubbleLength == 1) {
                            JSONObject bubble = jaBubbles.getJSONObject(0);
                            collected += bubble.getInt("collectedEnergy");
                            if (collected > 0) {
                                int randomIndex = random.nextInt(emojiList.size());
                                String randomEmoji = emojiList.get(randomIndex);
                                String str = "普通收取" + randomEmoji + collected + "g[" + cacheCollectedMap.get(userId) + "]";
                                totalCollected += collected;
                                if (needDouble) {
                                    Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                                    Toast.show(str + "[双击]");
                                } else {
                                    Log.forest(str + "耗时[" + spendTime + "]ms");
                                    Toast.show(str);
                                }
                                Statistics.addData(Statistics.DataType.COLLECTED, collected);
                            } else {
                                Log.record(TAG, "普通收取❌[" + cacheCollectedMap.get(userId) + "]的能量失败");
                                Log.runtime(TAG, "，UserID：" + userId + "，BubbleId：" + bubble.getLong("id"));
                            }
                            if (bubble.getBoolean("canBeRobbedAgain")) {
                                collectEnergyEntity.setNeedDouble();
                                collectEnergyEntity.resetTryCount();
                                collectEnergy(collectEnergyEntity);
                                return;
                            }
                            
                            JSONObject userHome = collectEnergyEntity.getUserHome();
                            if (userHome == null) {
                                return;
                            }
                            String bizNo = userHome.optString("bizNo");
                            if (bizNo.isEmpty()) {
                                return;
                            }
                            int returnCount = getReturnCount(collected);
                            if (returnCount > 0) {
                                returnFriendWater(userId, bizNo, 1, returnCount);
                            }
                        }
                    } catch (Exception e) {
                        Log.runtime(TAG, "collectEnergy err");
                        Log.printStackTrace(e);
                    } finally {
                        Statistics.save();
                        String str_totalCollected = "本次总 收:" + totalCollected + "g 帮:" + totalHelpCollected + "g 浇:" + totalWatered + "g";
                        Notify.updateLastExecText(str_totalCollected);
                        notifyMain();
                    }
                };
        taskCount.incrementAndGet();
        runnable.run();
    }

    private int getReturnCount(int collected) {
        int returnCount = 0;
        if (returnWater33.getValue() > 0 && collected >= returnWater33.getValue()) {
            returnCount = 33;
        } else if (returnWater18.getValue() > 0 && collected >= returnWater18.getValue()) {
            returnCount = 18;
        } else if (returnWater10.getValue() > 0 && collected >= returnWater10.getValue()) {
            returnCount = 10;
        }
        return returnCount;
    }

    /**
     * 更新使用中的的道具剩余时间
     */
    private void updateSelfHomePage() throws JSONException {
        String s = AntForestRpcCall.queryHomePage();
        GlobalThreadPools.sleep(100);
        JSONObject joHomePage = new JSONObject(s);
        updateSelfHomePage(joHomePage);
    }

    /**
     * 更新使用中的的道具剩余时间
     *
     * @param joHomePage 首页 JSON 对象
     */
    private void updateSelfHomePage(JSONObject joHomePage) {
        try {
            JSONArray usingUserPropsNew = joHomePage.getJSONArray("loginUserUsingPropNew");
            if (usingUserPropsNew.length() == 0) {
                usingUserPropsNew = joHomePage.getJSONArray("usingUserPropsNew");
            }
            for (int i = 0; i < usingUserPropsNew.length(); i++) {
                JSONObject userUsingProp = usingUserPropsNew.getJSONObject(i);
                String propGroup = userUsingProp.getString("propGroup");
                switch (propGroup) {
                    case "doubleClick": // 双击卡
                        doubleEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "双击卡剩余时间⏰：" + formatTimeDifference(doubleEndTime - System.currentTimeMillis()));
                        break;
                    case "stealthCard": // 隐身卡
                        stealthEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "隐身卡剩余时间⏰️：" + formatTimeDifference(stealthEndTime - System.currentTimeMillis()));
                        break;
                    case "shield": // 能量保护罩
                        shieldEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "保护罩剩余时间⏰：" + formatTimeDifference(shieldEndTime - System.currentTimeMillis()));
                        break;
                    case "energyBombCard": // 能量炸弹卡
                        energyBombCardEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "能量炸弹卡剩余时间⏰：" + formatTimeDifference(energyBombCardEndTime - System.currentTimeMillis()));
                        break;
                    case "robExpandCard": // 1.1倍能量卡
                        String extInfo = userUsingProp.optString("extInfo");
                        robExpandCardEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "1.1倍能量卡剩余时间⏰：" + formatTimeDifference(robExpandCardEndTime - System.currentTimeMillis()));
                        if (!extInfo.isEmpty()) {
                            JSONObject extInfoObj = new JSONObject(extInfo);
                            double leftEnergy = Double.parseDouble(extInfoObj.optString("leftEnergy", "0"));
                            if (leftEnergy > 3000 || ("true".equals(extInfoObj.optString("overLimitToday", "false")) && leftEnergy >= 1)) {
                                String propId = userUsingProp.getString("propId");
                                String propType = userUsingProp.getString("propType");
                                JSONObject jo = new JSONObject(AntForestRpcCall.collectRobExpandEnergy(propId, propType));
                                if (ResChecker.checkRes(TAG, jo)) {
                                    int collectEnergy = jo.optInt("collectEnergy");
                                    Log.forest("额外能量🌳[" + collectEnergy + "g][1.1倍能量卡]");
                                    Statistics.addData(Statistics.DataType.COLLECTED, collectEnergy);
                                }
                            }
                        }
                        break;
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "updateDoubleTime err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 弹出任务列表方法，用于处理森林任务。
     */
    /*
    private void popupTask() {
        try {
            JSONObject resData = new JSONObject(AntForestRpcCall.popupTask());
            if (ResChecker.checkRes(TAG, resData)) {
                JSONArray forestSignVOList = resData.optJSONArray("forestSignVOList");
                if (forestSignVOList != null) {
                    for (int i = 0; i < forestSignVOList.length(); i++) {
                        JSONObject forestSignVO = forestSignVOList.getJSONObject(i);
                        String signId = forestSignVO.getString("signId");
                        String currentSignKey = forestSignVO.getString("currentSignKey");
                        JSONArray signRecords = forestSignVO.getJSONArray("signRecords");
                        for (int j = 0; j < signRecords.length(); j++) {
                            JSONObject signRecord = signRecords.getJSONObject(j);
                            String signKey = signRecord.getString("signKey");
                            if (signKey.equals(currentSignKey) && !signRecord.getBoolean("signed")) {
                                JSONObject resData2 = new JSONObject(AntForestRpcCall.antiepSign(signId, UserMap.getCurrentUid()));
                                GlobalThreadPools.sleep(100L);
                                if (ResChecker.checkRes(TAG, resData2)) {
                                    Log.forest("收集过期能量💊[" + signRecord.getInt("awardCount") + "g]");
                                }
                                break;
                            }
                        }
                    }
                }
            } else {
                Log.record(TAG, "任务弹出失败: " + resData.getString("resultDesc"));
                Log.runtime(resData.toString());
            }
        } catch (JSONException e) {
            Log.runtime(TAG, "popupTask JSON错误:");
            Log.printStackTrace(TAG, e);
        } catch (Exception e) {
            Log.runtime(TAG, "popupTask 错误:");
            Log.printStackTrace(TAG, e);
        }
    }
    */

    /**
     * 为好友浇水并返回浇水次数和是否可以继续浇水的状态。
     *
     * @param userId      好友的用户ID
     * @param bizNo       业务编号
     * @param count       需要浇水的次数
     * @param waterEnergy 每次浇水的能量值
     * @return KVNode 包含浇水次数和是否可以继续浇水的状态
     */
    private KVMap<Integer, Boolean> returnFriendWater(String userId, String bizNo, int count, int waterEnergy) {
        // 如果业务编号为空，则直接返回默认值
        if (bizNo == null || bizNo.isEmpty()) {
            return new KVMap<>(0, true);
        }
        int wateredTimes = 0; // 已浇水次数
        boolean isContinue = true; // 是否可以继续浇水
        try {
            // 获取能量ID
            int energyId = getEnergyId(waterEnergy);
            // 循环浇水操作
            label:
            for (int waterCount = 1; waterCount <= count; waterCount++) {
                // 调用RPC进行浇水操作
                String rpcResponse = AntForestRpcCall.transferEnergy(userId, bizNo, energyId);
                GlobalThreadPools.sleep(1200L);
                JSONObject jo = new JSONObject(rpcResponse);
                String resultCode = jo.getString("resultCode");
                switch (resultCode) {
                    case "SUCCESS":
                        String currentEnergy = jo.getJSONObject("treeEnergy").getString("currentEnergy");
                        Log.forest("好友浇水🚿[" + UserMap.getMaskName(userId) + "]#" + waterEnergy + "g，剩余能量[" + currentEnergy + "g]");
                        wateredTimes++;
                        Statistics.addData(Statistics.DataType.WATERED, waterEnergy);
                        break;
                    case "WATERING_TIMES_LIMIT":
                        Log.record(TAG, "好友浇水🚿今日给[" + UserMap.getMaskName(userId) + "]浇水已达上限");
                        wateredTimes = 3; // 假设上限为3次
                        break label;
                    case "ENERGY_INSUFFICIENT":
                        Log.record(TAG, "好友浇水🚿" + jo.getString("resultDesc"));
                        isContinue = false;
                        break label;
                    default:
                        Log.record(TAG, "好友浇水🚿" + jo.getString("resultDesc"));
                        Log.runtime(jo.toString());
                        break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "returnFriendWater err");
            Log.printStackTrace(TAG, t);
        }
        return new KVMap<>(wateredTimes, isContinue);
    }

    /**
     * 获取能量ID
     */
    private int getEnergyId(int waterEnergy) {
        if (waterEnergy <= 0) return 0;
        if (waterEnergy >= 66) return 42;
        if (waterEnergy >= 33) return 41;
        if (waterEnergy >= 18) return 40;
        return 39;
    }

    /**
     * 兑换能量保护罩
     * 类别 spuid skuid price
     * 限时 CR20230517000497  CR20230516000370  166
     * 永久 CR20230517000497  CR20230516000371  500
     */
    private boolean exchangeEnergyShield() {
        String spuId = "CR20230517000497";
        String skuId = "CR20230516000370";
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false;
        }
        return Vitality.VitalityExchange(spuId, skuId, "保护罩");
    }

    /**
     * 兑换隐身卡
     */
    private boolean exchangeStealthCard() {
        String skuId = "SK20230521000206";
        String spuId = "SP20230521000082";
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false;
        }
        return Vitality.VitalityExchange(spuId, skuId, "隐身卡");
    }

    /**
     * 执行当天森林签到任务
     *
     * @param forestSignVOList 森林签到列表
     * @return 获得的能量，如果签到失败或已签到则返回 0
     */
    private int dailyTask(JSONArray forestSignVOList) {
        try {
            JSONObject forestSignVO = forestSignVOList.getJSONObject(0);
            String currentSignKey = forestSignVO.getString("currentSignKey"); // 当前签到的 key
            String signId = forestSignVO.getString("signId"); // 签到ID
            String sceneCode = forestSignVO.getString("sceneCode"); // 场景代码
            JSONArray signRecords = forestSignVO.getJSONArray("signRecords"); // 签到记录
            for (int i = 0; i < signRecords.length(); i++) { //遍历签到记录
                JSONObject signRecord = signRecords.getJSONObject(i);
                String signKey = signRecord.getString("signKey");
                int awardCount = signRecord.optInt("awardCount", 0);
                if (signKey.equals(currentSignKey) && !signRecord.getBoolean("signed")) {
                    JSONObject joSign = new JSONObject(AntForestRpcCall.antiepSign(signId, UserMap.getCurrentUid(), sceneCode));
                    GlobalThreadPools.sleep(300); // 等待300毫秒
                    if (ResChecker.checkRes(TAG + "森林签到失败:", joSign)) {
                        Log.forest("森林签到📆成功");
                        return awardCount;
                    }
                    break;
                }
            }
            return 0; // 如果没有签到，则返回 0
        } catch (Exception e) {
            Log.printStackTrace(e);
            return 0;
        }
    }

    // 提取奖励解析逻辑到单独的方法
    private int parseEnergyReward(JSONObject joSign) {
        int energyReward = 0;
        JSONObject signModel = joSign.optJSONObject("signModel");
        if (signModel != null) {
            JSONObject signAward = signModel.optJSONObject("signAward");
            if (signAward != null) {
                energyReward = signAward.optInt("count", 0);
            }
        }
        return energyReward;
    }

    /**
     * 森林任务:
     * 逛支付宝会员,去森林寻宝抽1t能量
     * 防治荒漠化和干旱日,给随机好友一键浇水
     * 开通高德活动领,去吉祥林许个愿
     * 逛森林集市得能量,逛一逛618会场
     * 逛一逛点淘得红包,去一淘签到领红包
     */
    private void receiveTaskAward() {
        try {
            // 修复：使用new HashSet包装从缓存获取的数据，兼容List/Set类型
            Set<String> presetBad = new LinkedHashSet<>(List.of(
                    "ENERGYRAIN", //能量雨
                    "ENERGY_XUANJIAO", //践行绿色行为
                    "FOREST_TOTAL_COLLECT_ENERGY_3",//累积3天收自己能量
                    "TEST_LEAF_TASK",//逛农场得落叶肥料
                    "SHARETASK" //邀请好友助力
            ));

            /* 3️⃣ 失败任务集合：空文件时自动创建空 HashSet 并立即落盘 */
            TypeReference<Set<String>> typeRef = new TypeReference<>() {};
            Set<String> badTaskSet = DataStore.INSTANCE.getOrCreate("badForestTaskSet", typeRef);
            /* 3️⃣ 首次运行时把预设黑名单合并进去并立即落盘 */
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad);
                DataStore.INSTANCE.put("badForestTaskSet", badTaskSet);   // 持久化
            }

            while (true) {
                boolean doubleCheck = false; // 标记是否需要再次检查任务
                String s = AntForestRpcCall.queryTaskList(); // 查询任务列表
                JSONObject jo = new JSONObject(s); // 解析响应为 JSON 对象

                if (!ResChecker.checkRes(TAG + "查询森林任务失败:", jo)) {
                    Log.record(jo.getString("resultDesc")); // 记录失败描述
                    Log.runtime(s); // 打印响应内容
                    break;
                }

                // 提取森林任务列表
                JSONArray forestSignVOList = jo.getJSONArray("forestSignVOList");
                int SumawardCount = 0;
                int DailyawardCount = dailyTask(forestSignVOList); // 执行每日任务
                SumawardCount = DailyawardCount + SumawardCount;

                // 提取森林任务
                JSONArray forestTasksNew = jo.optJSONArray("forestTasksNew");
                if (forestTasksNew == null || forestTasksNew.length() == 0) {
                    break; // 如果没有新任务，则返回
                }

                // 遍历任务
                for (int i = 0; i < forestTasksNew.length(); i++) {
                    JSONObject forestTask = forestTasksNew.getJSONObject(i);
                    JSONArray taskInfoList = forestTask.getJSONArray("taskInfoList"); // 获取任务信息列表

                    for (int j = 0; j < taskInfoList.length(); j++) {
                        JSONObject taskInfo = taskInfoList.getJSONObject(j);

                        JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo"); // 获取任务基本信息
                        String taskType = taskBaseInfo.getString("taskType"); // 获取任务类型
                        String sceneCode = taskBaseInfo.getString("sceneCode"); // 获取场景代码
                        String taskStatus = taskBaseInfo.getString("taskStatus"); // 获取任务状态

                        JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo")); // 获取业务信息
                        String taskTitle = bizInfo.optString("taskTitle", taskType); // 获取任务标题

                        JSONObject taskRights = new JSONObject(taskInfo.getString("taskRights")); // 获取任务权益
                        int awardCount = taskRights.optInt("awardCount", 0); // 获取奖励数量

                        // 判断任务状态
                        if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                            // 领取任务奖励
                            JSONObject joAward = new JSONObject(AntForestRpcCall.receiveTaskAward(sceneCode, taskType)); // 领取奖励请求
                            if (ResChecker.checkRes(TAG + "领取森林任务奖励失败:", joAward)) {
                                Log.forest("森林奖励🎖️[" + taskTitle + "]# " + awardCount + "活力值");
                                SumawardCount += awardCount;
                                doubleCheck = true; // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "领取失败: " + taskTitle); // 记录领取失败信息
                                Log.runtime(joAward.toString()); // 打印奖励响应
                            }
                            GlobalThreadPools.sleep(500);

                        } else if (TaskStatus.TODO.name().equals(taskStatus)) {
                            // 跳过已失败的任务
                            if (badTaskSet.contains(taskType)) continue;

                            if (!badTaskSet.contains(taskType)) {
                                String bizKey = sceneCode + "_" + taskType;
                                int count = forestTaskTryCount
                                        .computeIfAbsent(bizKey, k -> new AtomicInteger(0))
                                        .incrementAndGet();

                                // 完成任务请求
                                JSONObject joFinishTask = new JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType)); // 完成任务请求
                                if (count > 1) {
                                    Log.error(TAG, "完成森林任务失败超过1次" + taskTitle + "\n" + joFinishTask); // 记录完成任务失败信息
                                    badTaskSet.add(taskType);
                                    DataStore.INSTANCE.put("badForestTaskSet", badTaskSet);
                                } else {
                                    Log.forest("森林任务🧾️[" + taskTitle + "]");
                                    doubleCheck = true; // 标记需要重新检查任务
                                }
                            }
                        }

                        // 如果是游戏任务类型，查询并处理游戏任务
                        if ("mokuai_senlin_hlz".equals(taskType)) {
                            // 游戏任务跳转
                            String gameUrl = bizInfo.getString("taskJumpUrl");
                            Log.runtime(TAG, "跳转到游戏: " + gameUrl);

                            // 模拟跳转游戏任务URL（根据需要可能需要在客户端实际触发）
                            Log.runtime(TAG, "等待30S");
                            GlobalThreadPools.sleep(30000); // 等待任务完成
                            // 完成任务请求
                            JSONObject joFinishTask = new JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType)); // 完成任务请求
                            if (ResChecker.checkRes(TAG + "完成游戏任务失败:", joFinishTask)) {
                                Log.forest("游戏任务完成 🎮️[" + taskTitle + "]# " + awardCount + "活力值");
                                SumawardCount += awardCount;
                                doubleCheck = true; // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "游戏任务完成失败: " + taskTitle); // 记录任务完成失败信息
                            }
                        }
                    }
                }

                if (!doubleCheck) break;
            }

        } catch (JSONException e) {
            Log.error(TAG, "JSON解析错误: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        } catch (Throwable t) {
            Log.error(TAG, "receiveTaskAward 错误:");
            Log.printStackTrace(TAG, t); // 打印异常栈
        }
    }

     /**
     * 在收集能量之前使用道具。
     * 这个方法检查是否需要使用增益卡
     * 并在需要时使用相应的道具。
     *
     * @param userId 用户的ID。
     */
    private void usePropBeforeCollectEnergy(String userId) {
        try {
            /*
             * 在收集能量之前决定是否使用增益类道具卡。
             *
             * 主要逻辑:
             * 1. 定义时间常量，用于判断道具剩余有效期。
             * 2. 获取当前时间及各类道具的到期时间，计算剩余时间。
             * 3. 根据以下条件判断是否需要使用特定道具:
             *    - needDouble: 双击卡开关已打开，且当前没有生效的双击卡。
             *    - needrobExpand: 1.1倍能量卡开关已打开，且当前没有生效的卡。
             *    - needStealth: 隐身卡开关已打开，且当前没有生效的隐身卡。
             *    - needShield: 保护罩开关已打开，炸弹卡开关已关闭，且保护罩剩余时间不足一天。
             *    - needEnergyBombCard: 炸弹卡开关已打开，保护罩开关已关闭，且炸弹卡剩余时间不足三天。
             *    - needBubbleBoostCard: 加速卡开关已打开。
             * 4. 如果有任何一个道具需要使用，则同步查询背包信息，并调用相应的使用道具方法。
             */

            long now = System.currentTimeMillis();
            // 双击卡判断
            boolean needDouble = !doubleCard.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewDoubleCard(doubleEndTime, now);

            boolean needrobExpand = !robExpandCard.getValue().equals(applyPropType.CLOSE)
                    && robExpandCardEndTime < now;
            boolean needStealth = !stealthCard.getValue().equals(applyPropType.CLOSE)
                    && stealthEndTime < now;

            // 保护罩判断
            boolean needShield = !shieldCard.getValue().equals(applyPropType.CLOSE)
                    && energyBombCardType.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewShield(shieldEndTime, now);
            // 炸弹卡判断
            boolean needEnergyBombCard = !energyBombCardType.getValue().equals(applyPropType.CLOSE)
                    && shieldCard.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewEnergyBomb(energyBombCardEndTime, now);
            boolean needBubbleBoostCard = !bubbleBoostCard.getValue().equals(applyPropType.CLOSE);

            Log.runtime(TAG, "道具使用检查: needDouble=" + needDouble + ", needrobExpand=" + needrobExpand +
                    ", needStealth=" + needStealth + ", needShield=" + needShield +
                    ", needEnergyBombCard=" + needEnergyBombCard + ", needBubbleBoostCard=" + needBubbleBoostCard);
            if (needDouble || needStealth || needShield || needEnergyBombCard || needrobExpand || needBubbleBoostCard) {
                synchronized (doubleCardLockObj) {
                    JSONObject bagObject = queryPropList();
                    // Log.runtime(TAG, "bagObject=" + (bagObject == null ? "null" : bagObject.toString()));

                    if (needDouble) useDoubleCard(bagObject);
                    if (needrobExpand) useCardBoot(robExpandCardTime.getValue(), "1.1倍能量卡", this::userobExpandCard);
                    if (needStealth) useStealthCard(bagObject);
                    if (needBubbleBoostCard) useCardBoot(bubbleBoostTime.getValue(), "加速卡", this::useBubbleBoostCard);
                    if (needShield) {
                        Log.runtime(TAG, "尝试使用保护罩罩");
                        useShieldCard(bagObject);
                    } else if (needEnergyBombCard) {
                        Log.runtime(TAG, "准备使用能量炸弹卡");
                        useEnergyBombCard(bagObject);
                    }
                }
            } else {
                Log.runtime(TAG, "没有需要使用的道具");
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 保护罩剩余时间判断
     * 以整数 HHmm 指定保护罩续写阈值。
     * 例如：2355 表示 23 小时 55 分钟，0955 可直接写为 955。
     * 校验规则：0 ≤ HH ≤ 99，0 ≤ mm ≤ 59；非法值将回退为 23 小时。
     */
    /*
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewShield(long shieldEnd, long nowMillis) {
        int hours = 23, minutes = 0;
        if (SHIELD_RENEW_THRESHOLD_HHMM >= 0 && SHIELD_RENEW_THRESHOLD_HHMM <= 9959) {
            try {
                int abs = Math.abs(SHIELD_RENEW_THRESHOLD_HHMM);
                minutes = abs % 100;
            } catch (Exception ignored) {}
        }
        long thresholdMs = hours * ONE_HOUR_MS + minutes * 60_000L;
        if (shieldEnd <= nowMillis) { // 未生效或已过期
            Log.record(TAG, "[保护罩] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(shieldEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        long remain = shieldEnd - nowMillis;
        Log.record(TAG, "[保护罩] 剩余= " + formatTimeDifference(remain) + ", 阈值=" + String.format("%02d小时%02d分", hours, minutes));
        boolean needRenew = remain <= thresholdMs;
        Log.record(TAG, "[保护罩] 比较: "+remain+" <= "+thresholdMs+" == " + needRenew);
        return needRenew;
    }
    */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewShield(long shieldEnd, long nowMillis) {
        // 设置阈值为23小时59分钟
        int hours = 23;
        int minutes = 59;
        
        // 如果配置了自定义阈值，则使用自定义值
        if (SHIELD_RENEW_THRESHOLD_HHMM >= 0 && SHIELD_RENEW_THRESHOLD_HHMM <= 9959) {
            try {
                // 正确解析小时和分钟部分
                hours = SHIELD_RENEW_THRESHOLD_HHMM / 100;
                minutes = SHIELD_RENEW_THRESHOLD_HHMM % 100;
            } catch (Exception ignored) {
                // 解析失败时使用默认值
                hours = 23;
                minutes = 59;
            }
        }
        
        // 计算阈值毫秒数
        long thresholdMs = hours * ONE_HOUR_MS + minutes * 60_000L;
        
        if (shieldEnd <= nowMillis) { // 未生效或已过期
            Log.record(TAG, "[保护罩] 未生效/已过期，立即续写；end=" + 
                    TimeUtil.getCommonDate(shieldEnd) + ", now=" + 
                    TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        
        long remain = shieldEnd - nowMillis;
        Log.record(TAG, "[保护罩] 剩余= " + formatTimeDifference(remain) + 
                ", 阈值=" + String.format("%02d小时%02d分", hours, minutes));
        
        boolean needRenew = remain <= thresholdMs;
        Log.record(TAG, "[保护罩] 比较: "+remain+" <= "+thresholdMs+" == " + needRenew);

        return needRenew;
    }
    
    /**
     * 炸弹卡剩余时间判断
     * 当炸弹卡剩余时间低于3天时，需要续用
     * 最多可续用到4天
     */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewEnergyBomb(long bombEnd, long nowMillis) {
        // 炸弹卡最长有效期为4天
        long MAX_BOMB_DURATION = 4 * ONE_DAY;
        // 炸弹卡续用阈值为3天
        long BOMB_RENEW_THRESHOLD = 3 * ONE_DAY;
        if (bombEnd <= nowMillis) { // 未生效或已过期
            Log.runtime(TAG, "[炸弹卡] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(bombEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        long remain = bombEnd - nowMillis;
        Log.runtime(TAG, "[炸弹卡] 剩余= " + formatTimeDifference(remain) + ", 阈值=" + formatTimeDifference(BOMB_RENEW_THRESHOLD));

        // 如果剩余时间小于阈值且当前总时长未超过最大有效期，则需要续用
        boolean needRenew = remain <= BOMB_RENEW_THRESHOLD && (bombEnd - nowMillis + remain) <= MAX_BOMB_DURATION;
        Log.runtime(TAG, "[炸弹卡] 比较: " + remain + " <= " + BOMB_RENEW_THRESHOLD + " == " + needRenew +
                ", 总时长检查: " + (bombEnd - nowMillis + remain) + " <= " + MAX_BOMB_DURATION);
        return needRenew;
    }
    
    /**
     * 双击卡剩余时间判断
     * 当双击卡剩余时间低于31天时，需要续用
     * 最多可续用到31+31天，但不建议，因为平时有5分钟、3天、7天等短期双击卡
     */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewDoubleCard(long doubleEnd, long nowMillis) {
        // 双击卡最长有效期为62天（31+31）
        // long MAX_DOUBLE_DURATION = 62 * ONE_DAY;
        // 双击卡续用阈值为31天
        long DOUBLE_RENEW_THRESHOLD = 31 * ONE_DAY;
        
        if (doubleEnd <= nowMillis) { // 未生效或已过期
            Log.runtime(TAG, "[双击卡] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(doubleEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        
        long remain = doubleEnd - nowMillis;
        Log.runtime(TAG, "[双击卡] 剩余= " + formatTimeDifference(remain) + ", 阈值=" + formatTimeDifference(DOUBLE_RENEW_THRESHOLD));
        
        // 如果剩余时间小于阈值且当前总时长未超过最大有效期，则需要续用
        boolean needRenew = remain <= DOUBLE_RENEW_THRESHOLD;
        Log.runtime(TAG, "[双击卡] 比较: " + remain + " <= " + DOUBLE_RENEW_THRESHOLD + " == " + needRenew);
        return needRenew;
    }
    
    /**
     * 检查当前时间是否在设置的使用双击卡时间内
     *
     * @return 如果当前时间在双击卡的有效时间范围内，返回true；否则返回false。
     */
    private boolean hasDoubleCardTime() {
        long currentTimeMillis = System.currentTimeMillis();
        return TimeUtil.checkInTimeRange(currentTimeMillis, doubleCardTime.getValue());
    }

    private void giveProp() {
        Set<String> set = whoYouWantToGiveTo.getValue();
        if (!set.isEmpty()) {
            for (String userId : set) {
                if (!selfId.equals(userId)) {
                    giveProp(userId);
                    break;
                }
            }
        }
    }

    /**
     * 向指定用户赠送道具。 这个方法首先查询可用的道具列表，然后选择一个道具赠送给目标用户。 如果有多个道具可用，会尝试继续赠送，直到所有道具都赠送完毕。
     *
     * @param targetUserId 目标用户的ID。
     */
    private void giveProp(String targetUserId) {
        try {
            do {
                // 查询道具列表
                JSONObject propListJo = new JSONObject(AntForestRpcCall.queryPropList(true));
                if (ResChecker.checkRes(TAG + "查询道具列表失败:", propListJo)) {
                    JSONArray forestPropVOList = propListJo.optJSONArray("forestPropVOList");
                    if (forestPropVOList != null && forestPropVOList.length() > 0) {
                        JSONObject propJo = forestPropVOList.getJSONObject(0);
                        String giveConfigId = propJo.getJSONObject("giveConfigVO").getString("giveConfigId");
                        int holdsNum = propJo.optInt("holdsNum", 0);
                        String propName = propJo.getJSONObject("propConfigVO").getString("propName");
                        String propId = propJo.getJSONArray("propIdList").getString(0);
                        JSONObject giveResultJo = new JSONObject(AntForestRpcCall.giveProp(giveConfigId, propId, targetUserId));
                        if (ResChecker.checkRes(TAG + "赠送道具失败:", giveResultJo)) {
                            Log.forest("赠送道具🎭[" + UserMap.getMaskName(targetUserId) + "]#" + propName);
                            GlobalThreadPools.sleep(1500);
                        } else {
                            String rt = giveResultJo.getString("resultDesc");
                            Log.record(rt);
                            Log.runtime(giveResultJo.toString());
                            if (rt.contains("异常")) {
                                return;
                            }
                        }
                        // 如果持有数量大于1或道具列表中有多于一个道具，则继续赠送
                        if (holdsNum <= 1 && forestPropVOList.length() == 1) {
                            break;
                        }
                    }
                } else {
                    // 如果查询道具列表失败，则记录失败的日志
                    Log.record(TAG, "赠送道具查询结果" + propListJo.getString("resultDesc"));
                }
                // 等待1.5秒后再继续
                GlobalThreadPools.sleep(1500);
            } while (true);
        } catch (Throwable th) {
            // 打印异常信息
            Log.runtime(TAG, "giveProp err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 查询并管理用户巡护任务
     */
    private void queryUserPatrol() {
        long waitTime = 300L;//增大查询等待时间，减少异常
        try {
            do {
                // 查询当前巡护任务
                JSONObject jo = new JSONObject(AntForestRpcCall.queryUserPatrol());
                // GlobalThreadPools.sleep(waitTime);
                // 如果查询成功
                if (ResChecker.checkRes(TAG + "查询巡护任务失败:", jo)) {
                    // 查询我的巡护记录
                    JSONObject resData = new JSONObject(AntForestRpcCall.queryMyPatrolRecord());
                    // GlobalThreadPools.sleep(waitTime);
                    if (resData.optBoolean("canSwitch")) {
                        JSONArray records = resData.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            JSONObject userPatrol = record.getJSONObject("userPatrol");
                            // 如果存在未到达的节点，且当前模式为"silent"，则尝试切换巡护地图
                            if (userPatrol.getInt("unreachedNodeCount") > 0) {
                                if ("silent".equals(userPatrol.getString("mode"))) {
                                    JSONObject patrolConfig = record.getJSONObject("patrolConfig");
                                    String patrolId = patrolConfig.getString("patrolId");
                                    resData = new JSONObject(AntForestRpcCall.switchUserPatrol(patrolId));
                                    GlobalThreadPools.sleep(waitTime);
                                    // 如果切换成功，打印日志并继续
                                    if (ResChecker.checkRes(TAG + "切换巡护地图失败:", resData)) {
                                        Log.forest("巡护⚖️-切换地图至" + patrolId);
                                    }
                                    continue; // 跳过当前循环
                                }
                                break; // 如果当前不是silent模式，则结束循环
                            }
                        }
                    }
                    // 获取用户当前巡护状态信息
                    JSONObject userPatrol = jo.getJSONObject("userPatrol");
                    int currentNode = userPatrol.getInt("currentNode");
                    String currentStatus = userPatrol.getString("currentStatus");
                    int patrolId = userPatrol.getInt("patrolId");
                    JSONObject chance = userPatrol.getJSONObject("chance");
                    int leftChance = chance.getInt("leftChance");
                    int leftStep = chance.getInt("leftStep");
                    int usedStep = chance.getInt("usedStep");
                    if ("STANDING".equals(currentStatus)) {// 当前巡护状态为"STANDING"
                        if (leftChance > 0) {// 如果还有剩余的巡护次数，则开始巡护
                            jo = new JSONObject(AntForestRpcCall.patrolGo(currentNode, patrolId));
                            GlobalThreadPools.sleep(waitTime);
                            patrolKeepGoing(jo.toString(), currentNode, patrolId); // 继续巡护
                            continue; // 跳过当前循环
                        } else if (leftStep >= 2000 && usedStep < 10000) {// 如果没有剩余的巡护次数但步数足够，则兑换巡护次数
                            jo = new JSONObject(AntForestRpcCall.exchangePatrolChance(leftStep));
                            GlobalThreadPools.sleep(waitTime);
                            if (ResChecker.checkRes(TAG + "兑换巡护次数失败:", jo)) {// 兑换成功，增加巡护次数
                                int addedChance = jo.optInt("addedChance", 0);
                                Log.forest("步数兑换⚖️[巡护次数*" + addedChance + "]");
                                continue; // 跳过当前循环
                            } else {
                                Log.runtime(TAG, jo.getString("resultDesc"));
                            }
                        }
                    }
                    // 如果巡护状态为"GOING"，继续巡护
                    else if ("GOING".equals(currentStatus)) {
                        patrolKeepGoing(null, currentNode, patrolId);
                    }
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
                break; // 完成一次巡护任务后退出循环
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "queryUserPatrol err");
            Log.printStackTrace(TAG, t); // 打印异常堆栈
        }
    }

    /**
     * 持续巡护森林，直到巡护状态不再是“进行中”
     *
     * @param s         巡护请求的响应字符串，若为null将重新请求
     * @param nodeIndex 当前节点索引
     * @param patrolId  巡护任务ID
     */
    private void patrolKeepGoing(String s, int nodeIndex, int patrolId) {
        try {
            do {
                if (s == null) {
                    s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image");
                }
                JSONObject jo;
                try {
                    jo = new JSONObject(s);
                } catch (JSONException e) {
                    Log.record(TAG, "JSON解析错误: " + e.getMessage());
                    Log.printStackTrace(TAG, e);
                    return; // 解析失败，退出循环
                }
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                    break;
                }
                JSONArray events = jo.optJSONArray("events");
                if (events == null || events.length() == 0) {
                    return; // 无事件，退出循环
                }
                JSONObject event = events.getJSONObject(0);
                JSONObject userPatrol = jo.getJSONObject("userPatrol");
                int currentNode = userPatrol.getInt("currentNode");
                // 获取奖励信息，并处理动物碎片奖励
                JSONObject rewardInfo = event.optJSONObject("rewardInfo");
                if (rewardInfo != null) {
                    JSONObject animalProp = rewardInfo.optJSONObject("animalProp");
                    if (animalProp != null) {
                        JSONObject animal = animalProp.optJSONObject("animal");
                        if (animal != null) {
                            Log.forest("巡护森林🏇🏻[" + animal.getString("name") + "碎片]");
                        }
                    }
                }
                // 如果巡护状态不是“进行中”，则退出循环
                if (!"GOING".equals(jo.getString("currentStatus"))) {
                    return;
                }
                // 请求继续巡护
                JSONObject materialInfo = event.getJSONObject("materialInfo");
                String materialType = materialInfo.optString("materialType", "image");
                s = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType);
                GlobalThreadPools.sleep(100); // 等待100毫秒后继续巡护
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "patrolKeepGoing err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询并派遣伙伴
     */
    private void queryAndConsumeAnimal() {
        try {
            // 查询动物属性列表
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalPropList());
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.runtime(TAG, jo.getString("resultDesc"));
                return;
            }
            // 获取所有动物属性并选择可以派遣的伙伴
            JSONArray animalProps = jo.getJSONArray("animalProps");
            JSONObject bestAnimalProp = null;
            for (int i = 0; i < animalProps.length(); i++) {
                jo = animalProps.getJSONObject(i);
                if (bestAnimalProp == null || jo.getJSONObject("main").getInt("holdsNum") > bestAnimalProp.getJSONObject("main").getInt("holdsNum")) {
                    bestAnimalProp = jo; // 默认选择最大数量的伙伴
                }
            }
            // 派遣伙伴
            consumeAnimalProp(bestAnimalProp);
        } catch (Throwable t) {
            Log.runtime(TAG, "queryAnimalPropList err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 派遣伙伴进行巡护
     *
     * @param animalProp 选择的动物属性
     */
    private void consumeAnimalProp(JSONObject animalProp) {
        if (animalProp == null) return; // 如果没有可派遣的伙伴，则返回
        try {
            // 获取伙伴的属性信息
            String propGroup = animalProp.getJSONObject("main").getString("propGroup");
            String propType = animalProp.getJSONObject("main").getString("propType");
            String name = animalProp.getJSONObject("partner").getString("name");
            // 调用API进行伙伴派遣
            JSONObject jo = new JSONObject(AntForestRpcCall.consumeProp(propGroup, propType, false));
            if (ResChecker.checkRes(TAG + "巡护派遣失败:", jo)) {
                Log.forest("巡护派遣🐆[" + name + "]");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "consumeAnimalProp err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询动物及碎片信息，并尝试合成可合成的动物碎片。
     */
    private void queryAnimalAndPiece() {
        try {
            // 调用远程接口查询动物及碎片信息
            JSONObject response = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(0));
            String resultCode = response.optString("resultCode");
            // 检查接口调用是否成功
            if (!"SUCCESS".equals(resultCode)) {
                Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"));
                return;
            }
            // 获取动物属性列表
            JSONArray animalProps = response.optJSONArray("animalProps");
            if (animalProps == null || animalProps.length() == 0) {
                Log.runtime(TAG, "动物属性列表为空");
                return;
            }
            // 遍历动物属性
            for (int i = 0; i < animalProps.length(); i++) {
                JSONObject animalObject = animalProps.optJSONObject(i);
                if (animalObject == null) {
                    continue;
                }
                JSONArray pieces = animalObject.optJSONArray("pieces");
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "动物碎片列表为空");
                    continue;
                }
                int animalId = Objects.requireNonNull(animalObject.optJSONObject("animal")).optInt("id", -1);
                if (animalId == -1) {
                    Log.runtime(TAG, "动物ID缺失");
                    continue;
                }
                // 检查碎片是否满足合成条件
                if (canCombinePieces(pieces)) {
                    combineAnimalPiece(animalId);
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "查询动物及碎片信息时发生错误:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 检查碎片是否满足合成条件。
     *
     * @param pieces 动物碎片数组
     * @return 如果所有碎片满足合成条件，返回 true；否则返回 false
     */
    private boolean canCombinePieces(JSONArray pieces) {
        for (int j = 0; j < pieces.length(); j++) {
            JSONObject pieceObject = pieces.optJSONObject(j);
            if (pieceObject == null || pieceObject.optInt("holdsNum", 0) <= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 合成动物碎片。
     *
     * @param animalId 动物ID
     */
    private void combineAnimalPiece(int animalId) {
        try {
            while (true) {
                // 查询动物及碎片信息
                JSONObject response = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId));
                String resultCode = response.optString("resultCode");
                if (!"SUCCESS".equals(resultCode)) {
                    Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"));
                    break;
                }
                JSONArray animalProps = response.optJSONArray("animalProps");
                if (animalProps == null || animalProps.length() == 0) {
                    Log.runtime(TAG, "动物属性数据为空");
                    break;
                }
                // 获取第一个动物的属性
                JSONObject animalProp = animalProps.getJSONObject(0);
                JSONObject animal = animalProp.optJSONObject("animal");
                assert animal != null;
                int id = animal.optInt("id", -1);
                String name = animal.optString("name", "未知动物");
                // 获取碎片信息
                JSONArray pieces = animalProp.optJSONArray("pieces");
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "碎片数据为空");
                    break;
                }
                boolean canCombineAnimalPiece = true;
                JSONArray piecePropIds = new JSONArray();
                // 检查所有碎片是否可用
                for (int j = 0; j < pieces.length(); j++) {
                    JSONObject piece = pieces.optJSONObject(j);
                    if (piece == null || piece.optInt("holdsNum", 0) <= 0) {
                        canCombineAnimalPiece = false;
                        Log.runtime(TAG, "碎片不足，无法合成动物");
                        break;
                    }
                    // 添加第一个道具ID
                    piecePropIds.put(Objects.requireNonNull(piece.optJSONArray("propIdList")).optString(0, ""));
                }
                // 如果所有碎片可用，则尝试合成
                if (canCombineAnimalPiece) {
                    JSONObject combineResponse = new JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()));
                    resultCode = combineResponse.optString("resultCode");
                    if ("SUCCESS".equals(resultCode)) {
                        Log.forest("成功合成动物💡[" + name + "]");
                        animalId = id;
                        GlobalThreadPools.sleep(100); // 等待一段时间再查询
                        continue;
                    } else {
                        Log.runtime(TAG, "合成失败: " + combineResponse.optString("resultDesc"));
                    }
                }
                break; // 如果不能合成或合成失败，跳出循环
            }
        } catch (Exception e) {
            Log.runtime(TAG, "合成动物碎片时发生错误:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 获取背包信息
     */
    private JSONObject queryPropList() {
        try {
            JSONObject bagObject = new JSONObject(AntForestRpcCall.queryPropList(false));
            if (ResChecker.checkRes(TAG + "查询背包失败:", bagObject)) {
                return bagObject;
            }
            Log.error(TAG, "获取背包信息失败: " + bagObject);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "获取背包信息失败:", e);
        }
        return null;
    }

    /**
     * 查找背包道具
     *
     * @param bagObject 背包对象
     * @param propType  道具类型 LIMIT_TIME_ENERGY_SHIELD_TREE,...
     */
    private JSONObject findPropBag(JSONObject bagObject, String propType) {
        if (Objects.isNull(bagObject)) {
            return null;
        }
        try {
            JSONArray forestPropVOList = bagObject.getJSONArray("forestPropVOList");
            for (int i = 0; i < forestPropVOList.length(); i++) {
                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                JSONObject propConfigVO = forestPropVO.getJSONObject("propConfigVO");
                String currentPropType = propConfigVO.getString("propType");
                // String propName = propConfigVO.getString("propName");
                if (propType.equals(currentPropType)) {
                    return forestPropVO; // 找到后直接返回
                }
            }
        } catch (Exception e) {
            Log.error(TAG, "查找背包道具出错:");
            Log.printStackTrace(TAG, e);
        }

        return null; // 未找到或出错时返回 null
    }

    /**
     * 返回背包道具信息
     */
    // private JSONObject showBag() {
    private void showBag() {
        JSONObject bagObject = queryPropList();
        if (Objects.isNull(bagObject)) {
            // return null;
            return;
        }
        try {
            // JSONArray forestPropVOList = bagObject.getJSONArray("forestPropVOList");
            JSONArray forestPropVOList = Objects.requireNonNull(bagObject).getJSONArray("forestPropVOList");
            for (int i = 0; i < forestPropVOList.length(); i++) {
                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                JSONObject propConfigVO = forestPropVO.getJSONObject("propConfigVO");
                String currentPropType = propConfigVO.getString("propType");
                String propName = propConfigVO.getString("propName");
                Log.record("propName:"+propName+",propType:"+currentPropType);
            }
        } catch (Exception e) {
            Log.error(TAG, "查找背包道具出错:");
            Log.printStackTrace(TAG, e);
        }

        // return null; // 未找到或出错时返回 null
    }

    /**
     * 使用背包道具
     *
     * @param propJsonObj 道具对象
     */
    private boolean usePropBag(JSONObject propJsonObj) {
        if (propJsonObj == null) {
            Log.record(TAG, "要使用的道具不存在！");
            return false;
        }
        try {
            String propId = propJsonObj.getJSONArray("propIdList").getString(0);
            JSONObject propConfigVO = propJsonObj.getJSONObject("propConfigVO");
            String propType = propConfigVO.getString("propType");
            String propName = propConfigVO.getString("propName");
            String tag = propEmoji(propName);
            JSONObject jo;
            boolean isRenewable = isRenewableProp(propType);
            Log.record(TAG, "道具 " + propName + " (类型: " + propType + "), 是否可续用: " + isRenewable);
            String propGroup = AntForestRpcCall.getPropGroup(propType);
            if (isRenewable) {
                // 第一步：发送检查/尝试使用请求 (secondConfirm=false)
                String checkResponseStr = AntForestRpcCall.consumeProp(propGroup, propId, propType, false);
                JSONObject checkResponse = new JSONObject(checkResponseStr);
                // Log.record(TAG, "发送检查请求: " + checkResponse);
                JSONObject resData = checkResponse.optJSONObject("resData");
                if (resData == null) {
                    resData = checkResponse;
                }

                String status = resData.optString("usePropStatus");
                Log.record(TAG, "查成功, 状态: " + status);

                if ("NEED_CONFIRM_CAN_PROLONG".equals(status)) {
                    // 情况1: 需要二次确认 (真正的续写)
                    Log.record(TAG, "需要二次确认，发送确认请求...");
                    GlobalThreadPools.sleep(2000);
                    String confirmResponseStr = AntForestRpcCall.consumeProp(propGroup, propId, propType, true);
                    jo = new JSONObject(confirmResponseStr);
                    // Log.record(TAG, "发送确认请求: " + jo);
                }  else {
                    // 其他所有情况都视为最终结果，通常是失败
                    Log.record(TAG, "道具状态异常或使用失败。");
                    jo = checkResponse;
                }
            } else {
                // 非续用类道具，直接使用
                Log.record(TAG, "非续用类道具，直接使用");
                String consumeResponse = AntForestRpcCall.consumeProp2(propGroup, propId, propType);
                jo = new JSONObject(consumeResponse);
            }

            // 统一结果处理
            if (ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                Log.forest("使用道具" + tag + "[" + propName + "]");
                updateSelfHomePage();
                return true;
            } else {
                JSONObject errorData = jo.optJSONObject("resData");
                if (errorData == null) {
                    errorData = jo;
                }
                String resultDesc = errorData.optString("resultDesc", "未知错误");
                Log.record("使用道具失败: " + resultDesc);
                Toast.show(resultDesc);
                return false;
            }

        } catch (Throwable th) {
            Log.runtime(TAG, "usePropBag err");
            Log.printStackTrace(TAG, th);
            return false;
        }
    }
    /**
     * 判断是否是可续用类道具
     */
    private boolean isRenewableProp(String propType) {
        return propType.contains("SHIELD")   // 保护罩
                || propType.contains("BOMB_CARD") // 炸弹卡
                || propType.contains("DOUBLE_CLICK");     // 双击卡
    }

    @NonNull
    private static String propEmoji(String propName) {
        String tag;
        if (propName.contains("保")) {
            tag = "🛡️";
        } else if (propName.contains("双")) {
            tag = "👥";
        } else if (propName.contains("加")) {
            tag = "🌪";
        } else if (propName.contains("雨")) {
            tag = "🌧️";
        } else if (propName.contains("炸")) {
            tag = "💥";
        } else {
            tag = "🥳";
        }
        return tag;
    }

    /**
     * 使用双击卡道具。 这个方法检查是否满足使用双击卡的条件，如果满足，则在背包中查找并使用双击卡。
     *
     * @param bagObject 背包的JSON对象。
     */
    private void useDoubleCard(JSONObject bagObject) {
        try {
            if (hasDoubleCardTime() && Status.canDoubleToday()) {
                Log.runtime(TAG, "尝试使用双击卡...");
                JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_DOUBLE_CLICK");
                if (jo == null && doubleCardConstant.getValue()) {//如果背包内没有双击卡
                    Log.runtime(TAG, "背包中没有限时双击卡，尝试兑换...");
                    if (Vitality.handleVitalityExchange("SK20240805004754")) {//就鸡巴兑换
                        jo = findPropBag(queryPropList(), "ENERGY_DOUBLE_CLICK_31DAYS");
                    } else if (Vitality.handleVitalityExchange("CR20230516000363")) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_DOUBLE_CLICK");
                    }
                }
                if (jo == null) {
                    Log.runtime(TAG, "未找到限时双击卡，尝试查找3天长效双击卡...");
                    jo = findPropBag(bagObject, "PKS_ENERGY_DOUBLE_CLICK_3DAYS");
                }
                if (jo == null) {
                    Log.runtime(TAG, "未找到限时双击卡，尝试查找7天长效双击卡...");
                    jo = findPropBag(bagObject, "8ZN_ENERGY_DOUBLE_CLICK_7DAYS");
                }
                if (jo == null) {
                    Log.runtime(TAG, "未找到限时双击卡，尝试查找普通双击卡...");
                    jo = findPropBag(bagObject, "ENERGY_DOUBLE_CLICK");
                }
                if (jo != null) {
                    Log.runtime(TAG, "找到双击卡，准备使用: " + jo);
                    if (usePropBag(jo)) {
                        doubleEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
                        Status.DoubleToday();
                    }
                } else {
                    Log.runtime(TAG, "背包中未找到任何可用双击卡。");
                    updateSelfHomePage();
                }
            } else {
                Log.runtime(TAG, "不满足使用双击卡条件: inDoubleTime=" + hasDoubleCardTime() + ", canDoubleToday=" + Status.canDoubleToday());
            }
        } catch (Throwable th) {
            Log.error(TAG + "useDoubleCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 使用隐身卡道具。 这个方法检查是否满足使用隐身卡的条件，如果满足，则在背包中查找并使用隐身卡。
     *
     * @param bagObject 背包的JSON对象。
     */
    private void useStealthCard(JSONObject bagObject) {
        try {
            Log.runtime(TAG, "尝试使用隐身卡...");
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_STEALTH_CARD");
            if (jo == null && stealthCardConstant.getValue()) {
                Log.runtime(TAG, "背包中没有限时隐身卡，尝试兑换...");
                if (exchangeStealthCard()) {
                    jo = findPropBag(queryPropList(), "LIMIT_TIME_STEALTH_CARD");
                }
            }
            if (jo == null) {
                Log.runtime(TAG, "未找到限时隐身卡，尝试查找普通隐身卡...");
                jo = findPropBag(bagObject, "STEALTH_CARD");
            }
            if (jo != null) {
                Log.runtime(TAG, "找到隐身卡，准备使用: " + jo);
                if (usePropBag(jo)) {
                    stealthEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用隐身卡。");
                updateSelfHomePage();
            }
        } catch (Throwable th) {
            Log.error(TAG + "useStealthCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 使用能量保护罩，一般是限时保护罩，打开青春特权森林道具领取
     */
    /*
    private void useShieldCard(JSONObject bagObject) {
        try {
            Log.record(TAG, "尝试使用保护罩...");
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");
            if (jo == null) {
                Log.record(TAG, "背包中没有森林保护罩(LIMIT_TIME_ENERGY_SHIELD_TREE)，继续查找其他类型...");
                if (youthPrivilege.getValue()) {
                    Log.runtime(TAG, "尝试通过青春特权获取保护罩...");
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                    }
                }
            }
            if (jo == null) {
                if (shieldCardConstant.getValue()) {
                    Log.record(TAG, "尝试通过活力值兑换保护罩...");
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD");
                    }
                }
            }
            if (jo == null) {
                Log.record(TAG, "尝试能量保护罩(ENERGY_SHIELD)...");
                jo = findPropBag(bagObject, "ENERGY_SHIELD");
            }
            if (jo != null) {
                Log.runtime(TAG, "找到保护罩，准备使用: " + jo);
                if (usePropBag(jo)) {
                    return; // 使用成功，直接返回
                }
            }
            Log.record(TAG, "背包中未找到任何可用保护罩。");
            // 如果未使用成功，也刷新一次
            updateSelfHomePage();
        } catch (Throwable th) {
            Log.error(TAG + "使用能量保护罩， err");
            Log.printStackTrace(th);
        }
    }
    */
    private void useShieldCard(JSONObject bagObject) {
        try {
            Log.record(TAG, "尝试使用保护罩...");
            JSONObject jo = null;
            
            // 1. 优先尝试 LIMIT_TIME_ENERGY_SHIELD_TREE
            Log.record(TAG, "优先尝试森林保护罩(LIMIT_TIME_ENERGY_SHIELD_TREE)...");
            jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");
            if (jo == null) {
                Log.record(TAG, "背包中没有森林保护罩(LIMIT_TIME_ENERGY_SHIELD_TREE)，继续查找其他类型...");
                if (youthPrivilege.getValue()) {
                    Log.runtime(TAG, "尝试通过青春特权获取森林保护罩...");
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                    }
                }
            }
            
            // 2. 尝试 LIMIT_TIME_ENERGY_SHIELD
            if (jo == null) {
                Log.record(TAG, "尝试限时能量保护罩(LIMIT_TIME_ENERGY_SHIELD)...");
                jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD");
                if (jo == null && shieldCardConstant.getValue()) {
                    Log.record(TAG, "尝试通过活力值兑换限时能量保护罩...");
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD");
                    }
                }
            }
            
            // 3. 尝试 shubao3rd_ENERGY_SHIELD
            if (jo == null) {
                Log.record(TAG, "尝试树宝保护罩(shubao3rd_ENERGY_SHIELD)...");
                jo = findPropBag(bagObject, "shubao3rd_ENERGY_SHIELD");
            }
            
            // 4. 尝试 ENERGY_SHIELD
            if (jo == null) {
                Log.record(TAG, "尝试普通能量保护罩(ENERGY_SHIELD)...");
                jo = findPropBag(bagObject, "ENERGY_SHIELD");
            }

            // 5. 最后尝试 MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE
            if (jo == null) {
                Log.record(TAG, "尝试敦煌飞天保护罩(MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE)...");
                jo = findPropBag(bagObject, "MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE");
            }

            if (jo != null) {
                Log.runtime(TAG, "找到保护罩，准备使用: " + jo);
                if (usePropBag(jo)) {
                    return; // 使用成功，直接返回
                }
            }
            Log.record(TAG, "背包中未找到任何可用保护罩。");
            // 如果未使用成功，也刷新一次
            updateSelfHomePage();
        } catch (Throwable th) {
            Log.error(TAG + "使用能量保护罩， err");
            Log.printStackTrace(th);
        }
    }

    /**
     * 使用能量保护罩，一般是限时保护罩，打开青春特权森林道具领取
     */
    /*
    private void useShieldCard(JSONObject bagObject) {
        try {
            // 在背包中查询限时保护罩
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");
            if (jo == null) {
                // if (youthPrivilege.getValue() > 0) {
                if (youthPrivilege.getValue()) {
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                    } // 重新查找
                } else if (shieldCardConstant.getValue()) {
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD");
                    }
                } else {
                    jo = findPropBag(bagObject, "ENERGY_SHIELD"); // 尝试查找 普通保护罩，一般用不到
                }
            }
            if (jo != null && usePropBag(jo)) {
                shieldEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
            } else {
                updateSelfHomePage();
            }
        } catch (Throwable th) {
            Log.error(TAG + "useShieldCard err");
        }
    }
    */
    /*
    private void useShieldCard(JSONObject bagObject) {
        try {
            // 在背包中查询限时保护罩
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");

            // 如果没找到限时保护罩，则根据不同条件查找其他保护罩
            if (jo == null) {
                // 查找青春特权保护罩
                // if (youthPrivilege.getValue() > 0) {
                if (youthPrivilege.getValue()) {
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                    }
                }
                // 查找普通保护罩
                else if (shieldCardConstant.getValue()) {
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD");
                    }
                }
                // 查找树宝保护罩
                else if (findPropBag(bagObject, "shubao3rd_ENERGY_SHIELD") != null) {
                    jo = findPropBag(bagObject, "shubao3rd_ENERGY_SHIELD");
                }
                // 查找敦煌飞天保护罩
                else if (findPropBag(bagObject, "MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE") != null) {
                    jo = findPropBag(bagObject, "MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE");
                }
                // 查找普通能量保护罩
                else {
                    jo = findPropBag(bagObject, "ENERGY_SHIELD"); // 普通保护罩，一般用不到
                }
            }

            // 使用保护罩，如果找到且使用成功
            if (jo != null && usePropBag(jo)) {
                shieldEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24; // 设置保护罩有效期为24小时
            } else {
                updateSelfHomePage(); // 更新主页
            }
        } catch (Throwable th) {
            Log.error(TAG + "useShieldCard err");
        }
    }
    */
    /*
    private void useShieldCard(JSONObject bagObject) {
        try {
            Log.record(TAG, "开始执行 useShieldCard，背包内容：" + bagObject);

            // 在背包中查询限时保护罩
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");
            Log.record(TAG, "初次查找限时保护罩 LIMIT_TIME_ENERGY_SHIELD_TREE，结果：" + jo);

            // 如果没找到限时保护罩，则根据不同条件查找其他保护罩
            if (jo == null) {
                // 查找青春特权保护罩
                // if (youthPrivilege.getValue() > 0) {
                if (youthPrivilege.getValue()) {
                    Log.record(TAG, "检测到青春特权，尝试使用青春特权保护罩");
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                        Log.record(TAG, "青春特权保护罩查找结果：" + jo);
                    } else {
                        Log.record(TAG, "青春特权条件不满足");
                    }
                }
                // 查找普通保护罩
                else if (shieldCardConstant.getValue()) {
                    Log.record(TAG, "检测到普通保护罩常量，尝试兑换普通保护罩");
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_SHIELD");
                        Log.record(TAG, "普通保护罩查找结果：" + jo);
                    } else {
                        Log.record(TAG, "兑换普通保护罩失败");
                    }
                }
                // 查找树宝保护罩
                else if (findPropBag(bagObject, "shubao3rd_ENERGY_SHIELD") != null) {
                    jo = findPropBag(bagObject, "shubao3rd_ENERGY_SHIELD");
                    Log.record(TAG, "找到树宝保护罩：" + jo);
                }
                // 查找敦煌飞天保护罩
                else if (findPropBag(bagObject, "MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE") != null) {
                    jo = findPropBag(bagObject, "MUSEUM_DUNHUANG_ENERGY_SHIELD_NO_EXPIRE");
                    Log.record(TAG, "找到敦煌飞天保护罩：" + jo);
                }
                // 查找普通能量保护罩
                else {
                    jo = findPropBag(bagObject, "ENERGY_SHIELD");
                    Log.record(TAG, "找到普通保护罩：" + jo);
                }
            }

            // 使用保护罩，如果找到且使用成功
            if (jo != null) {
                boolean success = usePropBag(jo);
                Log.record(TAG, "尝试使用保护罩：" + jo + "，结果：" + success);
                if (success) {
                    shieldEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24; // 设置保护罩有效期为24小时
                    Log.record(TAG, "保护罩使用成功，shieldEndTime：" + shieldEndTime);
                } else {
                    Log.record(TAG, "保护罩使用失败，刷新主页");
                    updateSelfHomePage(); // 更新主页
                }
            } else {
                Log.record(TAG, "未找到可用保护罩，刷新主页");
                updateSelfHomePage(); // 更新主页
            }
        } catch (Throwable th) {
            Log.error(TAG + "useShieldCard 异常");
        }
    }
    */


    public void useCardBoot(List<String> TargetTimeValue, String propName, Runnable func) {
        for (String targetTimeStr : TargetTimeValue) {
            if ("-1".equals(targetTimeStr)) {
                return;
            }
            Calendar targetTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(targetTimeStr);
            if (targetTimeCalendar == null) {
                return;
            }
            long targetTime = targetTimeCalendar.getTimeInMillis();
            long now = System.currentTimeMillis();
            if (now > targetTime) {
                continue;
            }
            String targetTaskId = "TAGET|" + targetTime;
            if (!hasChildTask(targetTaskId)) {
                addChildTask(new ChildModelTask(targetTaskId, "TAGET", func, targetTime));
                Log.record(TAG, "添加定时使用" + propName + "[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(targetTime) + "]执行");
            } else {
                addChildTask(new ChildModelTask(targetTaskId, "TAGET", func, targetTime));
            }
        }
    }

    private void useBubbleBoostCard() {
        useBubbleBoostCard(queryPropList());
    }

    private void userobExpandCard() {
        userobExpandCard(queryPropList());
    }

    private void useBubbleBoostCard(JSONObject bag) {
        try {
            // 在背包中查询限时加速器
            JSONObject jo = findPropBag(bag, "LIMIT_TIME_ENERGY_BUBBLE_BOOST");
            if (jo == null) {
                Privilege.INSTANCE.youthPrivilege();
                jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_BUBBLE_BOOST"); // 重新查找
                if (jo == null) {
                    jo = findPropBag(bag, "BUBBLE_BOOST"); // 尝试查找 普通加速器，一般用不到
                }
            }
            if (jo != null) {
                usePropBag(jo);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useBubbleBoostCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    private void userobExpandCard(JSONObject bag) {
        try {
            JSONObject jo = findPropBag(bag, "VITALITY_ROB_EXPAND_CARD_1.1_3DAYS");
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
            }
            jo = findPropBag(bag, "SHAMO_ROB_EXPAND_CARD_1.1_1DAYS");
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
            }
            jo = findPropBag(bag, "SHAMO_ROB_EXPAND_CARD_1.5_1DAYS");
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
            }
            jo = findPropBag(bag, "LG_ROB_EXPAND_CARD_1.1_7DAYS"); // 7天1.1倍能量翻倍卡
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7;
            }
            jo = findPropBag(bag, "8ZN_ROB_EXPAND_CARD_1.5_14DAYS"); // 14天1.5倍能量翻倍卡
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 14;
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useBubbleBoostCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    private void useEnergyRainChanceCard() {
        try {
            if (Status.hasFlagToday("AntForest::useEnergyRainChanceCard")) {
                return;
            }
            // 背包查找 限时能量雨机会
            JSONObject jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE");
            // 活力值商店兑换
            if (jo == null) {
                JSONObject skuInfo = Vitality.findSkuInfoBySkuName("能量雨次卡");
                if (skuInfo == null) {
                    return;
                }
                String skuId = skuInfo.getString("skuId");
                if (Status.canVitalityExchangeToday(skuId, 1) && Vitality.VitalityExchange(skuInfo.getString("spuId"), skuId, "限时能量雨机会")) {
                    jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE");
                }
            }
            // 使用 道具
            if (jo != null && usePropBag(jo)) {
                Status.setFlagToday("AntForest::useEnergyRainChanceCard");
                GlobalThreadPools.sleep(500);
                EnergyRain.startEnergyRain();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useEnergyRainChanceCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 炸弹卡使用
     */
    private void useEnergyBombCard(JSONObject bagObject) {
        try {
            Log.runtime(TAG, "尝试使用炸弹卡...");
            JSONObject jo = findPropBag(bagObject, "ENERGY_BOMB_CARD");
            if (jo == null) {
                Log.runtime(TAG, "背包中没有炸弹卡，尝试兑换...");
                JSONObject skuInfo = Vitality.findSkuInfoBySkuName("能量炸弹卡");
                if (skuInfo == null) {
                    Log.runtime(TAG, "活力值商店中未找到炸弹卡。");
                    return;
                }

                String skuId = skuInfo.getString("skuId");
                if (Status.canVitalityExchangeToday(skuId, 1)) {
                    if (Vitality.VitalityExchange(skuInfo.getString("spuId"), skuId, "能量炸弹卡")) {
                        jo = findPropBag(queryPropList(), "ENERGY_BOMB_CARD");
                    }
                } else {
                    Log.runtime(TAG, "今日炸弹卡兑换次数已达上限。");
                }
            }

            if (jo != null) {
                Log.runtime(TAG, "找到炸弹卡，准备使用: " + jo);
                if (usePropBag(jo)) {
                    // 使用成功后刷新真实结束时间
                    updateSelfHomePage();
                    Log.runtime(TAG, "能量炸弹卡使用成功，已刷新结束时间");
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用炸弹卡。");
                updateSelfHomePage();
            }
        } catch (Throwable th) {
            Log.error(TAG + "useEnergyBombCard err");
            Log.printStackTrace(th);
        }
    }

    private int tryOneCollect(RpcEntity rpcEntity, String userId, long bubbleId) {
        try {
            RequestManager.requestObject(rpcEntity, 0, 0);
            if (rpcEntity.getHasError()) {
                String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
                if ("1004".equals(errorCode)) {
                    if (BaseModel.getWaitWhenException().getValue() > 0) {
                        long waitTime = System.currentTimeMillis() + BaseModel.getWaitWhenException().getValue();
                        RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime);
                        Notify.updateStatusText("异常");
                        Log.record(TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime));
                        errorWait = true;
                        return -1;
                    }
                    GlobalThreadPools.sleep(600 + RandomUtil.delay());
                }
                return -2;
            }
            JSONObject jo = new JSONObject(rpcEntity.getResponseString());
            String resultCode = jo.getString("resultCode");
            if (!"SUCCESS".equalsIgnoreCase(resultCode)) {
                if ("PARAM_ILLEGAL2".equals(resultCode)) {
                    Log.record(TAG, "[" + UserMap.getMaskName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"));
                    return 1;
                }
                Log.record(TAG, "[" + UserMap.getMaskName(userId) + "]" + jo.getString("resultDesc"));
                return 0;
            }
            int collected = 0;
            JSONArray jaBubbles = jo.getJSONArray("bubbles");
            JSONObject bubble = jaBubbles.getJSONObject(0);
            collected += bubble.getInt("collectedEnergy");
            if (collected > 0) {
                int randomIndex = random.nextInt(emojiList.size());
                String randomEmoji = emojiList.get(randomIndex);
                Log.record(TAG, "单次收取" + randomEmoji + collected + "g[" + UserMap.getMaskName(userId) + "]");
                return collected;
            } else {
                Log.record(TAG, "单次收取❌[" + UserMap.getMaskName(userId) + "]的能量失败");
                Log.runtime(TAG, "，UserID：" + userId + "，BubbleId：" + bubble.getLong("id"));
            }
        } catch (Exception e) {
            Log.runtime(TAG, "collectEnergy err");
            Log.printStackTrace(e);
            return -1;
        }
        return 0;
    }

    /**
     * 收取状态的枚举类型
     */
    public enum CollectStatus {AVAILABLE, WAITING, INSUFFICIENT, ROBBED}

    /**
     * 能量定时任务类型
     */
    private class EnergyTimerTask extends ChildModelTask {

        private final String userId;

        private final long bubbleId;

        private final long produceTime;
        private final boolean is_self;

        /**
         * 实例化一个新的能量收取定时任务
         *
         * @param uid 用户id
         * @param bid 能量id
         * @param pt  能量产生时间
         */
        EnergyTimerTask(String uid, long bid, long pt) {
            // 调用父类构造方法，传入任务ID和提前执行时间
            super(AntForest.getEnergyTimerTid(uid, bid), pt - advanceTimeInt);
            userId = uid;
            bubbleId = bid;
            produceTime = pt;
            if (selfId == userId) {
                is_self = true;
            } else {
                is_self = false;
            }
        }

        @Override
        public Runnable setRunnable() {
            return () -> {
                String userName = UserMap.getMaskName(userId);
                int averageInteger = offsetTimeMath.getAverageInteger();
                long readyTime = produceTime - advanceTimeInt + averageInteger - delayTimeMath.getAverageInteger() - System.currentTimeMillis() + 70;
                if (readyTime > 0) {
                    try {
                        Thread.sleep(readyTime);
                    } catch (InterruptedException e) {
                        Log.runtime(TAG, "终止[" + userName + "]蹲点收取任务, 任务ID[" + getId() + "]");
                        return;
                    }
                }
                Log.record(TAG, "执行蹲点收取⏰ 任务ID " + getId() + " [" + userName + "]" + "时差[" + averageInteger + "]ms" + "提前[" + advanceTimeInt + "]ms");
                int rt = 0;
                if(is_self) {
                   Log.forest("自己的能量，多尝试收取几次");
                   long start_time = System.currentTimeMillis();
                   while (System.currentTimeMillis() - start_time < 3000) {
                        Log.record("持续时间"+(System.currentTimeMillis() - start_time));
                        try {
                            RpcEntity rpcEntity = AntForestRpcCall.energyRpcEntity("", userId, bubbleId);
                            rt = tryOneCollect(rpcEntity,userId, bubbleId);
                            if(-1 == rt) {
                                break;
                            }
                            if(rt >= 0) {
                                break;
                            }
                        } catch (Exception e) {
                            Log.runtime(TAG, "collectEnergy err");
                            Log.printStackTrace(e);
                            break;
                        }
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Log.runtime("线程延时失败");
                            break;
                        }
                   }

                }
                if(rt <= 0) {
                	collectEnergy(new CollectEnergyEntity(userId, null, AntForestRpcCall.energyRpcEntity("", userId, bubbleId)));
                }
            };
        }
    }

    /**
     * 获取能量收取任务ID
     */
    public static String getEnergyTimerTid(String uid, long bid) {
        return "BT|" + uid + "|" + bid;
    }
}
