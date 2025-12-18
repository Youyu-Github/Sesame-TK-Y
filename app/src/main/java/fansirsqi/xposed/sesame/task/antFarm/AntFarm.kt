package fansirsqi.xposed.sesame.task.antFarm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.farmFamilyOption
import fansirsqi.xposed.sesame.entity.ParadiseCoinBenefit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit.addIntervalLimit
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField.ListJoinCommaToStringModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.newutil.DataStore.getOrCreate
import fansirsqi.xposed.sesame.newutil.DataStore.put
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.ListUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.StringUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VipDataIdMap
import lombok.ToString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import fansirsqi.xposed.sesame.model.modelFieldExt.PriorityModelField
import fansirsqi.xposed.sesame.util.TimeCounter

class AntFarm : ModelTask() {

    override fun getName(): String {
        return "蚂蚁庄园"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FARM
    }

    override fun getIcon(): String {
        return "AntFarm.png"
    }

    private var ownerFarmId: String? = null
    private var animals: Array<Animal>? = null
    private var ownerAnimal = Animal()

    /**
     * 小鸡饲料g
     */
    private var foodStock = 0
    private var foodStockLimit = 0
    private var rewardProductNum: String? = null
    private var rewardList: Array<RewardFriend>? = null


    /**
     * 慈善评分
     */
    private var benevolenceScore = 0.0
    private var harvestBenevolenceScore = 0.0

    /**
     * 未领取的饲料奖励
     */
    private var unreceiveTaskAward = 0

    /**
     * 小鸡心情值
     */
    private var finalScore = 0.0
    private var familyGroupId: String? = null
    private var farmTools: Array<FarmTool> = emptyArray()

    /*
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        bizKeyList = new ArrayList<>();
        bizKeyList.add("ADD_GONGGE_NEW");
        bizKeyList.add("USER_STARVE_PUSH");
        bizKeyList.add("YEB_PURCHASE");
        bizKeyList.add("WIDGET_addzujian");//添加庄园小组件
        bizKeyList.add("HIRE_LOW_ACTIVITY");//雇佣小鸡拿饲料
        bizKeyList.add("DIANTAOHUANDUAN");//去点淘逛一逛
        bizKeyList.add("TAO_GOLDEN_V2");//去逛一逛淘金币小镇
        bizKeyList.add("TAOBAO_tab2gzy");// 去逛一逛淘宝视频
        bizKeyList.add("YITAO_appgyg");// 去一淘APP逛逛
        bizKeyList.add("ANTFARM_chouchoule");// 【抽抽乐】好运装扮来袭！
        bizKeyList.add("TB_qiandao2023");// 去淘宝签到逛一逛
        bizKeyList.add("BABAFARM_TB");// 去逛一逛淘宝芭芭农场
        bizKeyList.add("TB_chongzhi");// 逛一逛小羊农场
        bizKeyList.add("ALIPAIMAI_gygzy");// 逛一逛淘花岛
        bizKeyList.add("BABA_FARM_SPREAD_MANURE");// 去芭芭农场给果树施肥
        bizKeyList.add("ELM_hudong2024");// 去饿了么游乐园逛一逛
        bizKeyList.add("2024XIANYU_huanduan");// 去闲鱼逛一逛
        bizKeyList.add("JINGTAN_FEED_FISH");// 去鲸探喂鱼集福气
        bizKeyList.add("UC_gygzy");// 逛一逛UC浏览器
        bizKeyList.add("TAOBAO_renshenggyg");// 去淘宝人生逛一逛
        bizKeyList.add("TOUTIAO_daoduan");// 去今日头条极速版逛一逛
        bizKeyList.add("SLEEP");// 让小鸡去睡觉
        bizKeyList.add("HUABEI2023");// 去花呗花花卡逛一逛，完成可得90g饲料
        bizKeyList.add("XJLY_meishiqiyuji30");// 去小鸡乐园开2次宝箱，完成可得90g饲料
        bizKeyList.add("25WFYX_xiaojinuoche");// 去小鸡乐园开2次宝箱，完成可得90g饲料
        bizKeyList.add("25WFYX_xiaojiliaoli_v2");// 去小鸡乐园开2次宝箱，完成可得90g饲料
        bizKeyList.add("25WFYX_shiguangzahuodian");// 去小鸡乐园开2次宝箱，完成可得90g饲料
        bizKeyList.add("XJLYKBX1_sl90");// 去小鸡乐园开2次宝箱，完成可得90g饲料
        bizKeyList.add("CAINIAOGUOGUO2023V2");// 去菜鸟做公益，完成可得90g饲料
        bizKeyList.add("VIDEO_TASK");// 看庄园小视频，浏览15s可得90g饲料
        bizKeyList.add("25WFYX_duiudipeng");// 玩对对碰乐园完成20次消除，完成得1个乐园宝箱和限时180g饲料
        bizKeyList.add("mangheshipai");// 家庭小盲盒实拍照片来啦，通过家庭活动获得小盲盒，还可获得90g饲料
        bizKeyList.add("25WFYX_baoweixiangrikui");// 玩向日葵通过1关主线新关卡，完成可得1个小鸡乐园宝箱和90g饲料
        bizKeyList.add("2025618dacu");// 去淘金币618赢10亿，完成浏览可得90g饲料
        bizKeyList.add("BBNC_gyg");// 去芭芭农场逛一逛，完成可得90g饲料
        bizKeyList.add("COOK");// 小鸡厨房，每天做美食可得90g饲料
        bizKeyList.add("ANTMEMBER_RICHANGQIANDAO");// 去支付宝会员签到，完成可得90g饲料
        bizKeyList.add("tab3_gyg");// 逛一逛热门短视频，完成看视频进度最高可得240g饲料
        bizKeyList.add("baiduditu");// 去百度地图逛一逛，完成可得90g饲料
        //bizKeyList.add("OFFLINE_PAY");// 到店付款，完成可得180g饲料
        //bizKeyList.add("ONLINE_PAY");// 线上支付，完成可得180g饲料
        //bizKeyList.add("30001229221356342088142133303848");// 一起拿小鸡饲料，每天可给5位好友送饲料，7天内好友完成领取，自己可获1包饲料（不扣自己的饲料，且奖励不会过期）（注意改id2088...）
        //bizKeyList.add("30001935487934202088142133303848");// 庄园小课堂，每天答题最高可得180g饲料（注意改id2088...）
        bizKeyList.add("SHANGYEHUA_90_1");// 去杂货铺逛一逛，浏览15s可得90g饲料
        bizKeyList.add("chouchoule_xiaritianqi");// 抽抽乐每日抽1次可得90g饲料
        //bizKeyList.add("HEART_DONATION_ADVANCED_FOOD_V2");// 每天单笔捐赠1元可得爱心美食（为保证项目正常运行，禁止使用此项！）
        //bizKeyList.add("HEART_DONATE");// 爱心捐赠（每天2次），捐任意金额可得180g饲料（为保证项目正常运行，禁止使用此项！）
        //bizKeyList.add("SHANGOU_xiadan");// 秋天第一杯奶茶
    }
    */

    /**
     * 小鸡睡觉时间
     */
    private var sleepTime: StringModelField? = null

    /**
     * 小鸡睡觉时长
     */
    private var sleepMinutes: IntegerModelField? = null

    /**
     * 自动喂鸡
     */
    private var feedAnimal: BooleanModelField? = null

    /**
     * 打赏好友
     */
    // private var rewardFriend: PriorityModelField? = null
    private var rewardFriend: BooleanModelField? = null

    /**
     * 遣返小鸡
     */
    private var sendBackAnimal: PriorityModelField? = null

    /**
     * 遣返方式
     */
    private var sendBackAnimalWay: ChoiceModelField? = null

    /**
     * 遣返动作
     */
    private var sendBackAnimalType: ChoiceModelField? = null

    /**
     * 遣返好友列表
     */
    private var sendBackAnimalList: SelectModelField? = null

    /**
     * 召回小鸡
     */
    private var recallAnimalType: ChoiceModelField? = null

    /**
     * s收取道具奖励
     */
    private var receiveFarmToolReward: PriorityModelField? = null

    /**
     * 游戏改分
     */
    private var recordFarmGame: PriorityModelField? = null
    // private var recordFarmGame: BooleanModelField? = null

    /**
     * 小鸡游戏时间
     */
    private var farmGameTime: ListJoinCommaToStringModelField? = null

    /**
     * 小鸡厨房
     */
    private var kitchen: PriorityModelField? = null

    /**
     * 使用特殊食品
     */
    private var useSpecialFood: BooleanModelField? = null
    private var useNewEggCard: BooleanModelField? = null
    private var harvestProduce: BooleanModelField? = null
    private var donation: BooleanModelField? = null
    private var donationCount: ChoiceModelField? = null

    /**
     * 收取饲料奖励
     */
    private var receiveFarmTaskAward: PriorityModelField? = null
    private var useAccelerateTool: BooleanModelField? = null
    private var useBigEaterTool: BooleanModelField? = null // ✅ 新增加饭卡
    private var useAccelerateToolContinue: BooleanModelField? = null
    private var useAccelerateToolWhenMaxEmotion: BooleanModelField? = null

    /**
     * 喂鸡列表
     */
    private var feedFriendAnimalList: SelectAndCountModelField? = null
    private var notifyFriend: PriorityModelField? = null
    private var notifyFriendType: ChoiceModelField? = null
    private var notifyFriendList: SelectModelField? = null
    private var acceptGift: BooleanModelField? = null
    private var visitFriendList: SelectAndCountModelField? = null
    private var chickenDiary: PriorityModelField? = null
    private var diaryTietie: BooleanModelField? = null
    private var collectChickenDiary: ChoiceModelField? = null
    private var enableChouchoule: PriorityModelField? = null
    private var listOrnaments: BooleanModelField? = null
    //private var hireAnimal: PriorityModelField? = null
    private var hireAnimal: BooleanModelField? = null
    private var hireAnimalType: ChoiceModelField? = null
    private var hireAnimalList: SelectModelField? = null
    private var enableDdrawGameCenterAward: PriorityModelField? = null
    private var getFeed: PriorityModelField? = null
    private var getFeedlList: SelectModelField? = null
    private var getFeedType: ChoiceModelField? = null
    private var family: BooleanModelField? = null
    private var familyOptions: SelectModelField? = null
    private var notInviteList: SelectModelField? = null
    private var paradiseCoinExchangeBenefit: PriorityModelField? = null
    private var paradiseCoinExchangeBenefitList: SelectModelField? = null
    private var visitAnimal: PriorityModelField? = null

    // 在方法外或类中作为字段缓存当天任务次数（不持久化）
    private val farmTaskTryCount: MutableMap<String?, AtomicInteger?> = ConcurrentHashMap<String?, AtomicInteger?>()


    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("sleepTime", "小鸡睡觉时间(关闭:-1)", "2330").also { sleepTime = it })
        modelFields.addField(IntegerModelField("sleepMinutes", "小鸡睡觉时长(分钟)", 10 * 36, 1, 10 * 60).also { sleepMinutes = it })
        modelFields.addField(ChoiceModelField("recallAnimalType", "召回小鸡", RecallAnimalType.Companion.ALWAYS, RecallAnimalType.Companion.nickNames).also { recallAnimalType = it })
        // modelFields.addField(PriorityModelField("rewardFriend", "打赏好友", priorityType.PRIORITY_2, priorityType.nickNames).also { rewardFriend = it })
        modelFields.addField(BooleanModelField("rewardFriend", "打赏好友", false).also { rewardFriend = it })
        modelFields.addField(BooleanModelField("feedAnimal", "自动喂小鸡", false).also { feedAnimal = it })
        modelFields.addField(SelectAndCountModelField("feedFriendAnimalList", "喂小鸡好友列表", LinkedHashMap<String?, Int?>()) { AlipayUser.getList() }.also { feedFriendAnimalList = it })
        modelFields.addField(PriorityModelField("getFeed", "一起拿饲料", priorityType.PRIORITY_2, priorityType.nickNames).also { getFeed = it })
        modelFields.addField(ChoiceModelField("getFeedType", "一起拿饲料 | 动作", GetFeedType.Companion.GIVE, GetFeedType.Companion.nickNames).also { getFeedType = it })
        modelFields.addField(SelectModelField("getFeedlList", "一起拿饲料 | 好友列表", LinkedHashSet<String?>()) { AlipayUser.getList() }.also { getFeedlList = it })
        modelFields.addField(BooleanModelField("acceptGift", "收麦子", false).also { acceptGift = it })
        modelFields.addField(SelectAndCountModelField("visitFriendList", "送麦子好友列表", LinkedHashMap<String?, Int?>()) { AlipayUser.getList() }.also { visitFriendList = it })
        // modelFields.addField(PriorityModelField("hireAnimal", "雇佣小鸡 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { hireAnimal = it })
        modelFields.addField(BooleanModelField("hireAnimal", "雇佣小鸡 | 开启", false).also { hireAnimal = it })
        modelFields.addField(ChoiceModelField("hireAnimalType", "雇佣小鸡 | 动作", HireAnimalType.Companion.DONT_HIRE, HireAnimalType.Companion.nickNames).also { hireAnimalType = it })
        modelFields.addField(SelectModelField("hireAnimalList", "雇佣小鸡 | 好友列表", LinkedHashSet<String?>()) { AlipayUser.getList() }.also { hireAnimalList = it })
        modelFields.addField(PriorityModelField("sendBackAnimal", "遣返 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { sendBackAnimal = it })
        modelFields.addField(ChoiceModelField("sendBackAnimalWay", "遣返 | 方式", SendBackAnimalWay.Companion.NORMAL, SendBackAnimalWay.Companion.nickNames).also { sendBackAnimalWay = it })
        modelFields.addField(ChoiceModelField("sendBackAnimalType", "遣返 | 动作", SendBackAnimalType.Companion.NOT_BACK, SendBackAnimalType.Companion.nickNames).also { sendBackAnimalType = it })
        modelFields.addField(SelectModelField("dontSendFriendList", "遣返 | 好友列表", LinkedHashSet<String?>()) { AlipayUser.getList() }.also { sendBackAnimalList = it })
        modelFields.addField(PriorityModelField("notifyFriend", "通知赶鸡 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { notifyFriend = it })
        modelFields.addField(ChoiceModelField("notifyFriendType", "通知赶鸡 | 动作", NotifyFriendType.Companion.NOTIFY, NotifyFriendType.Companion.nickNames).also { notifyFriendType = it })
        modelFields.addField(SelectModelField("notifyFriendList", "通知赶鸡 | 好友列表", LinkedHashSet<String?>()) { AlipayUser.getList() }.also { notifyFriendList = it })
        modelFields.addField(BooleanModelField("donation", "每日捐蛋 | 开启", false).also { donation = it })
        modelFields.addField(ChoiceModelField("donationCount", "每日捐蛋 | 次数", DonationCount.Companion.ONE, DonationCount.Companion.nickNames).also { donationCount = it })
        modelFields.addField(BooleanModelField("useAccelerateTool", "加速卡 | 使用", false).also { useAccelerateTool = it })
        modelFields.addField(BooleanModelField("useAccelerateToolContinue", "加速卡 | 连续使用", false).also { useAccelerateToolContinue = it })
        modelFields.addField(BooleanModelField("useAccelerateToolWhenMaxEmotion", "加速卡 | 仅在满状态时使用", false).also { useAccelerateToolWhenMaxEmotion = it })
        modelFields.addField(BooleanModelField("useBigEaterTool", "加饭卡 | 使用", false).also { useBigEaterTool = it })
        modelFields.addField(BooleanModelField("useSpecialFood", "使用特殊食品", false).also { useSpecialFood = it })
        modelFields.addField(BooleanModelField("useNewEggCard", "使用新蛋卡", false).also { useNewEggCard = it })
        modelFields.addField(PriorityModelField("receiveFarmTaskAward", "收取饲料奖励", priorityType.PRIORITY_2, priorityType.nickNames).also { receiveFarmTaskAward = it })
        modelFields.addField(PriorityModelField("receiveFarmToolReward", "收取道具奖励", priorityType.PRIORITY_2, priorityType.nickNames).also { receiveFarmToolReward = it })
        modelFields.addField(BooleanModelField("harvestProduce", "收获爱心鸡蛋", false).also { harvestProduce = it })
        modelFields.addField(PriorityModelField("kitchen", "小鸡厨房", priorityType.PRIORITY_2, priorityType.nickNames).also { kitchen = it })
        modelFields.addField(PriorityModelField("chickenDiary", "小鸡日记", priorityType.PRIORITY_2, priorityType.nickNames).also { chickenDiary = it })
        modelFields.addField(BooleanModelField("diaryTietie", "小鸡日记 | 贴贴", false).also { diaryTietie = it })
        modelFields.addField(ChoiceModelField("collectChickenDiary", "小鸡日记 | 点赞", CollectChickenDiaryType.Companion.ONCE, CollectChickenDiaryType.Companion.nickNames).also { collectChickenDiary = it })
        modelFields.addField(PriorityModelField("enableChouchoule", "开启小鸡抽抽乐", priorityType.PRIORITY_2, priorityType.nickNames).also { enableChouchoule = it })
        modelFields.addField(BooleanModelField("listOrnaments", "小鸡每日换装", false).also { listOrnaments = it })
        modelFields.addField(PriorityModelField("enableDdrawGameCenterAward", "开宝箱", priorityType.PRIORITY_2, priorityType.nickNames).also { enableDdrawGameCenterAward = it })
        modelFields.addField(PriorityModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", priorityType.PRIORITY_2, priorityType.nickNames).also { recordFarmGame = it })
        // modelFields.addField(BooleanModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", false).also { recordFarmGame = it })
        modelFields.addField(ListJoinCommaToStringModelField("farmGameTime", "小鸡游戏时间(范围)", ListUtil.newArrayList<String?>("2200-2400")).also { farmGameTime = it })
        modelFields.addField(BooleanModelField("family", "家庭 | 开启", false).also { family = it })
        modelFields.addField(SelectModelField("familyOptions", "家庭 | 选项", LinkedHashSet<String?>(), farmFamilyOption()).also { familyOptions = it })
        modelFields.addField(SelectModelField("notInviteList", "家庭 | 好友分享排除列表", LinkedHashSet<String?>()) { AlipayUser.getList() }.also { notInviteList = it })
        // modelFields.addField(giftFamilyDrawFragment = new StringModelField("giftFamilyDrawFragment", "家庭 | 扭蛋碎片赠送用户ID(配置目录查看)", ""));
        modelFields.addField(PriorityModelField("paradiseCoinExchangeBenefit", "小鸡乐园 | 兑换权益", priorityType.PRIORITY_2, priorityType.nickNames).also { paradiseCoinExchangeBenefit = it })
        modelFields.addField(SelectModelField("paradiseCoinExchangeBenefitList", "小鸡乐园 | 权益列表", LinkedHashSet<String?>()) { ParadiseCoinBenefit.getList() }.also { paradiseCoinExchangeBenefitList = it })
        modelFields.addField(PriorityModelField("visitAnimal", "到访小鸡送礼", priorityType.PRIORITY_2, priorityType.nickNames).also { visitAnimal = it })
        return modelFields
    }

    override fun boot(classLoader: ClassLoader?) {
        super.boot(classLoader)
        addIntervalLimit("com.alipay.antfarm.enterFarm", 2000)
    }

    override fun check(): Boolean {
        if (TaskCommon.IS_ENERGY_TIME) {
            return false
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            return false
        } else {
            return true
        }
    }

    override fun run() {
        try {
            val tc = TimeCounter(TAG)
            val userId = UserMap.currentUid
            Log.record(TAG, "执行开始-蚂蚁" + getName())
            if (enterFarm() == null) {
                return
            }
            listFarmTool() //装载道具信息
            tc.countDebug("装载道具信息")

            // if (getRunCents() >= rewardFriend!!.value) {
            if (rewardFriend?.value == true) { // 优化: 使用 ?. 进行安全调用
                rewardFriend()
                tc.countDebug("打赏好友")
            }
            if (getRunCents() >= sendBackAnimal!!.value) {
                sendBackAnimal()
                tc.countDebug("遣返")
            }

            if (getRunCents() >= receiveFarmToolReward!!.value) {
                receiveToolTaskReward()
                tc.countDebug("收取道具奖励")
            }

            // if (recordFarmGame!!.value) {
            if (getRunCents() >= recordFarmGame!!.value) {
                farmGameTime?.value?.forEach { time -> // 优化: 使用 forEach 循环
                    if (TimeUtil.checkNowInTimeRange(time)) {
                        recordFarmGame(GameType.starGame)
                        recordFarmGame(GameType.jumpGame)
                        recordFarmGame(GameType.flyGame)
                        recordFarmGame(GameType.hitGame)
                        return@forEach // 优化: 找到符合条件的时间后即可退出循环
                    }
                }
                tc.countDebug("游戏改分(星星球、登山赛、飞行赛、揍小鸡)")
            }

            /*
            if (getRunCents() >= kitchen!!.value) {
                collectDailyFoodMaterial()
                collectDailyLimitedFoodMaterial()
                tc.countDebug("小鸡厨房")
            }
            */

            if (getRunCents() >= kitchen!!.value) {
                // 检查小鸡是否在睡觉，如果在睡觉则跳过厨房功能
                if (AnimalFeedStatus.SLEEPY.name == ownerAnimal.animalFeedStatus) {
                    Log.record(TAG, "小鸡厨房🐔[小鸡正在睡觉中，跳过厨房功能]")
                } else {
                    collectDailyFoodMaterial()
                    collectDailyLimitedFoodMaterial()
                    cook()
                }
                tc.countDebug("小鸡厨房")
            }

            if (getRunCents() >= chickenDiary!!.value) {
                doChickenDiary()
                tc.countDebug("小鸡日记")
            }

            if (useNewEggCard?.value == true) { // 优化: 安全调用
                // 检查小鸡是否在睡觉，如果在睡觉则跳过使用新蛋卡
                if (AnimalFeedStatus.SLEEPY.name == ownerAnimal.animalFeedStatus) {
                    Log.record(TAG, "小鸡厨房🐔[小鸡正在睡觉中，跳过使用新蛋卡]")
                } else {
                    useFarmTool(ownerFarmId, ToolType.NEWEGGTOOL)
                    syncAnimalStatus(ownerFarmId)
                }
                tc.countDebug("使用新蛋卡")
            }
            if (harvestProduce?.value == true && benevolenceScore >= 1) { // 优化: 安全调用
                Log.record(TAG, "有可收取的爱心鸡蛋")
                harvestProduce(ownerFarmId)
                tc.countDebug("收鸡蛋")
            }
            if (donation?.value == true && Status.canDonationEgg(userId) && harvestBenevolenceScore >= 1) { // 优化: 安全调用
                handleDonation(donationCount!!.value)
                tc.countDebug("每日捐蛋")
            }

            if (getRunCents() >= receiveFarmTaskAward!!.value) {
                doFarmTasks()
                tc.countDebug("饲料任务")
                receiveFarmAwards()
                tc.countDebug("收取饲料奖励")
            }

            recallAnimal()
            tc.countDebug("召回小鸡")

            // 雇佣小鸡
            if (hireAnimal?.value == true) { // 优化: 安全调用
                hireAnimal()
                tc.countDebug("雇佣小鸡")
            }

            handleAutoFeedAnimal()
            tc.countDebug("喂食")

            // 到访小鸡送礼
            if (getRunCents() >= visitAnimal!!.value) {
                visitAnimal();
                tc.countDebug("到访小鸡送礼");
                // 送麦子
                visit();
                tc.countDebug("送麦子");
            }
            // 帮好友喂鸡
            feedFriend()
            tc.countDebug("帮好友喂鸡")
            // 通知好友赶鸡
            if (getRunCents() >= notifyFriend!!.value) {
                notifyFriend()
                tc.countDebug("通知好友赶鸡")
            }

            // 抽抽乐
            if (getRunCents() >= enableChouchoule!!.value) {
                val ccl = ChouChouLe()
                ccl.chouchoule()
                tc.countDebug("抽抽乐")
            }

            if (getRunCents() >= getFeed!!.value) {
                letsGetChickenFeedTogether()
                tc.countDebug("一起拿饲料")
            }
            //家庭
            if (family?.value == true) { // 优化: 安全调用
//                family();
                AntFarmFamily.run(familyOptions!!, notInviteList!!)
                tc.countDebug("家庭任务")
            }
            // 开宝箱
            if (getRunCents() >= enableDdrawGameCenterAward!!.value) {
                drawGameCenterAward()
                tc.countDebug("开宝箱")
            }
            // 小鸡乐园道具兑换
            if (getRunCents() >= paradiseCoinExchangeBenefit!!.value) {
                paradiseCoinExchangeBenefit()
                tc.countDebug("小鸡乐园道具兑换")
            }
            //小鸡睡觉&起床
            animalSleepAndWake()
            tc.countDebug("小鸡睡觉&起床")
            tc.stop()
        } catch (t: Throwable) {
            Log.runtime(TAG, "AntFarm.start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-蚂蚁" + getName())
        }
    }


    /**
     * 召回小鸡
     */
    private fun recallAnimal() {
        try {
            // 召回小鸡相关操作
            if (AnimalInteractStatus.HOME.name != ownerAnimal.animalInteractStatus) { // 如果小鸡不在家
                if ("ORCHARD" == ownerAnimal.locationType) {
                    Log.farm("庄园通知📣[你家的小鸡给拉去除草了！]")
                    val joRecallAnimal = JSONObject(AntFarmRpcCall.orchardRecallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmMasterUserId))
                    val manureCount = joRecallAnimal.getInt("manureCount")
                    Log.farm("召回小鸡📣[收获:肥料${manureCount}g]") // 优化: 使用字符串模板
                } else {
                    syncAnimalStatus(ownerFarmId)
                    var guest = false
                    ownerAnimal.subAnimalType?.let {
                        // 优化: 使用 when 表达式，更具可读性
                        when (SubAnimalType.valueOf(it)) {
                            SubAnimalType.GUEST -> {
                                guest = true
                                Log.record(TAG, "小鸡到好友家去做客了")
                            }
                            SubAnimalType.NORMAL -> Log.record(TAG, "小鸡太饿，离家出走了")
                            SubAnimalType.PIRATE -> Log.record(TAG, "小鸡外出探险了")
                            SubAnimalType.WORK -> Log.record(TAG, "小鸡出去工作啦")
                        }
                    }

                    var hungry = false
                    val userName = UserMap.getMaskName(AntFarmRpcCall.farmId2UserId(ownerAnimal.currentFarmId))
                    ownerAnimal.animalFeedStatus?.let {
                        // 优化: 使用 when 表达式
                        when (AnimalFeedStatus.valueOf(it)) {
                            AnimalFeedStatus.HUNGRY -> {
                                hungry = true
                                Log.record(TAG, "小鸡在[${userName}]的庄园里挨饿") // 优化: 使用字符串模板
                            }
                            AnimalFeedStatus.EATING -> Log.record(TAG, "小鸡在[${userName}]的庄园里吃得津津有味")
                            AnimalFeedStatus.SLEEPY -> Log.record(TAG, "小鸡在[${userName}]的庄园")
                            AnimalFeedStatus.NONE -> Log.record(TAG, "小鸡在[${userName}]不知道在干嘛")
                        }
                    }
                    // 2. 优化recall变量的赋值方式，并简化Companion object的调用
                    // 优化: 使用 when 表达式直接赋值，代码更简洁
                    val recall = when (recallAnimalType?.value) {
                        RecallAnimalType.ALWAYS -> true
                        RecallAnimalType.WHEN_THIEF -> !guest
                        RecallAnimalType.WHEN_HUNGRY -> hungry
                        else -> false
                    }

                    if (recall) {
                        recallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmId, ownerFarmId, userName)
                        syncAnimalStatus(ownerFarmId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "recallAnimal err:", e)
        }
    }

    private fun paradiseCoinExchangeBenefit() {
        try {
            val jo = JSONObject(AntFarmRpcCall.mallHome)
            if (!ResChecker.checkRes(TAG + "查询小鸡乐园商城失败:", jo)) {
                Log.error(TAG, "小鸡乐园币💸[未获取到可兑换权益]")
                return
            }
            val mallItemSimpleList = jo.getJSONArray("mallItemSimpleList")
            // 优化: 使用 for-each 循环遍历 JSONArray
            for (i in 0 until mallItemSimpleList.length()) {
                val mallItemInfo = mallItemSimpleList.getJSONObject(i)
                val spuName = mallItemInfo.getString("spuName")
                val minPrice = mallItemInfo.getInt("minPrice")
                val controlTag = mallItemInfo.getString("controlTag")
                val spuId = mallItemInfo.getString("spuId")
                // 优化: 使用多行字符串和模板，更清晰
                val oderInfo = """
                    $spuName
                    价格${minPrice}乐园币
                    $controlTag
                """.trimIndent()
                val idMap = IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java)
                idMap.add(spuId, oderInfo)
                val itemStatusList = mallItemInfo.getJSONArray("itemStatusList")
                if (!Status.canParadiseCoinExchangeBenefitToday(spuId) ||
                    paradiseCoinExchangeBenefitList?.value?.contains(spuId) != true || // 优化: 安全调用
                    isExchange(itemStatusList, spuId, spuName)
                ) {
                    continue
                }
                var exchangedCount = 0
                while (exchangeBenefit(spuId)) {
                    exchangedCount += 1
                    Log.farm("乐园币兑换💸#花费[${minPrice}乐园币]#第${exchangedCount}次兑换[$spuName]") // 优化: 使用字符串模板
                    TimeUtil.sleep(3000)
                }
            }
            IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).save(UserMap.currentUid)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "paradiseCoinExchangeBenefit err:", t)
        }
    }

    private fun exchangeBenefit(spuId: String): Boolean {
        try {
            val jo = JSONObject(AntFarmRpcCall.getMallItemDetail(spuId))
            if (!ResChecker.checkRes(TAG + "查询商品详情失败:", jo)) {
                return false
            }
            val mallItemDetail = jo.getJSONObject("mallItemDetail")
            val mallSubItemDetailList = mallItemDetail.getJSONArray("mallSubItemDetailList")
            // 优化: 使用 for-each 循环
            for (i in 0 until mallSubItemDetailList.length()) {
                val mallSubItemDetail = mallSubItemDetailList.getJSONObject(i)
                val skuId = mallSubItemDetail.getString("skuId")
                val skuName = mallSubItemDetail.getString("skuName")
                val itemStatusList = mallSubItemDetail.getJSONArray("itemStatusList")

                if (isExchange(itemStatusList, spuId, skuName)) {
                    return false
                }

                if (exchangeBenefit(spuId, skuId)) {
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "exchangeBenefit err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private fun exchangeBenefit(spuId: String, skuId: String?): Boolean {
        try {
            val jo = JSONObject(AntFarmRpcCall.exchangeBenefit(spuId, skuId))
            return ResChecker.checkRes(TAG + "兑换权益失败:", jo)
        } catch (t: Throwable) {
            Log.runtime(TAG, "exchangeBenefit err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private fun isExchange(itemStatusList: JSONArray, spuId: String?, spuName: String?): Boolean {
        try {
            // 优化: 使用 for-each 循环
            for (j in 0 until itemStatusList.length()) {
                val itemStatus = itemStatusList.getString(j)
                if (PropStatus.REACH_LIMIT.name == itemStatus
                    || PropStatus.REACH_USER_HOLD_LIMIT.name == itemStatus
                    || PropStatus.NO_ENOUGH_POINT.name == itemStatus
                ) {
                    Log.record(TAG, "乐园兑换💸[${spuName}]停止:${PropStatus.valueOf(itemStatus).nickName()}") // 优化: 字符串模板
                    if (PropStatus.REACH_LIMIT.name == itemStatus) {
                        Status.setFlagToday("farm::paradiseCoinExchangeLimit::$spuId")
                    }
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "isItemExchange err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private fun animalSleepAndWake() {
        try {
            val sleepTimeStr = sleepTime?.value ?: return // 优化: 使用 Elvis 运算符提前返回
            if ("-1" == sleepTimeStr) {
                Log.runtime(TAG, "当前已关闭小鸡睡觉")
                return
            }
            val now = TimeUtil.getNow()
            val animalSleepTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(sleepTimeStr)
            if (animalSleepTimeCalendar == null) {
                Log.record(TAG, "小鸡睡觉时间格式错误，请重新设置")
                return
            }
            val sleepMinutesInt = sleepMinutes?.value ?: return // 优化: Elvis 运算符
            val animalWakeUpTimeCalendar = animalSleepTimeCalendar.clone() as Calendar
            animalWakeUpTimeCalendar.add(Calendar.MINUTE, sleepMinutesInt)
            val animalSleepTime = animalSleepTimeCalendar.timeInMillis
            val animalWakeUpTime = animalWakeUpTimeCalendar.timeInMillis
            if (animalSleepTime > animalWakeUpTime) {
                Log.record(TAG, "小鸡睡觉设置有误，请重新设置")
                return
            }
            val afterSleepTime = now > animalSleepTimeCalendar
            val afterWakeUpTime = now > animalWakeUpTimeCalendar
            if (afterSleepTime && afterWakeUpTime) {
                if (!Status.canAnimalSleep()) {
                    return
                }
                Log.record(TAG, "已错过小鸡今日睡觉时间")
                return
            }
            val sleepTaskId = "AS|$animalSleepTime"
            val wakeUpTaskId = "AW|$animalWakeUpTime"
            if (!hasChildTask(sleepTaskId) && !afterSleepTime) {
                addChildTask(ChildModelTask(sleepTaskId, "AS", { this.animalSleepNow() }, animalSleepTime))
                Log.record(TAG, "添加定时睡觉🛌[${UserMap.getCurrentMaskName()}]在[${TimeUtil.getCommonDate(animalSleepTime)}]执行") // 优化: 字符串模板
            }
            if (!hasChildTask(wakeUpTaskId) && !afterWakeUpTime) {
                addChildTask(ChildModelTask(wakeUpTaskId, "AW", { this.animalWakeUpNow() }, animalWakeUpTime))
                Log.record(TAG, "添加定时起床🛌[${UserMap.getCurrentMaskName()}]在[${TimeUtil.getCommonDate(animalWakeUpTime)}]执行") // 优化: 字符串模板
            }
            if (afterSleepTime) {
                if (Status.canAnimalSleep()) {
                    animalSleepNow()
                }
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "animalSleepAndWake err:")
            Log.printStackTrace(e)
        }
    }

    /**
     * 初始化庄园
     *
     * @return 庄园信息
     */
    private fun enterFarm(): JSONObject? {
        try {
            val userId = UserMap.currentUid
            val jo = JSONObject(AntFarmRpcCall.enterFarm(userId, userId))
            if (ResChecker.checkRes(TAG + "进入庄园失败:", jo)) {
                rewardProductNum = jo.getJSONObject("dynamicGlobalConfig").getString("rewardProductNum")
                val joFarmVO = jo.getJSONObject("farmVO")
                val familyInfoVO = jo.getJSONObject("familyInfoVO")
                foodStock = joFarmVO.getInt("foodStock")
                foodStockLimit = joFarmVO.getInt("foodStockLimit")
                harvestBenevolenceScore = joFarmVO.getDouble("harvestBenevolenceScore")

                parseSyncAnimalStatusResponse(joFarmVO)

                familyGroupId = familyInfoVO.optString("groupId", "")
                // 领取活动食物
                val activityData = jo.optJSONObject("activityData")
                activityData?.keys()?.forEach { key -> // 优化: 使用 forEach 循环和安全调用
                    if (key.contains("Gifts")) {
                        val gifts = activityData.optJSONArray(key)
                        gifts?.let {
                            for (i in 0 until it.length()) {
                                clickForGiftV2(it.optJSONObject(i))
                            }
                        }
                    }
                }
                if (useSpecialFood?.value == true) { //使用特殊食品 // 优化: 安全调用
                    val cuisineList = jo.getJSONArray("cuisineList")
                    if (AnimalFeedStatus.SLEEPY.name != ownerAnimal.animalFeedStatus) useSpecialFood(cuisineList)
                }

                jo.optJSONObject("lotteryPlusInfo")?.let { drawLotteryPlus(it) } // 优化: 使用 optJSONObject 和 let

                if (acceptGift?.value == true && joFarmVO.getJSONObject("subFarmVO").has("giftRecord") // 优化: 安全调用
                    && foodStockLimit - foodStock >= 10
                ) {
                    acceptGift()
                }
                return jo
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
        return null
    }

    /**
     * 自动喂鸡
     */
    private fun handleAutoFeedAnimal() {
        // 优化: 使用卫语句（Guard Clause）提前返回，减少嵌套
        if (ownerAnimal.animalId == null) {
            Log.record(TAG, "🐔 当前ownerAnimal信息 - ID: ${ownerAnimal.animalId}, 主人农场ID: ${ownerAnimal.masterFarmId}, 当前农场ID: ${ownerAnimal.currentFarmId}")
            syncAnimalStatus(ownerFarmId)
            if (ownerAnimal.animalId == null) {
                Log.record(TAG, "🐔 错误：无法获取小鸡信息，跳过自动喂食")
                return
            }
        }

        // 打印小鸡状态和ID信息
        Log.record(TAG, "🐔 小鸡状态检查 - ID: ${ownerAnimal.animalId}, 互动状态: ${ownerAnimal.animalInteractStatus}, 饲料状态: ${ownerAnimal.animalFeedStatus}, 位置类型: ${ownerAnimal.locationType}")
        
        // 优化: 合并空值检查
        if (ownerAnimal.animalInteractStatus == null || ownerAnimal.animalFeedStatus == null) {
            Log.record(TAG, "🐔 警告：小鸡状态信息不完整，尝试重新同步")
            syncAnimalStatus(ownerFarmId)
            if (ownerAnimal.animalInteractStatus == null || ownerAnimal.animalFeedStatus == null) {
                Log.record(TAG, "🐔 错误：无法获取完整的小鸡状态信息，跳过自动喂食")
                return
            }
        }

        if (AnimalInteractStatus.HOME.name != ownerAnimal.animalInteractStatus) {
            Log.record(TAG, "🐔 小鸡不在家，跳过自动喂食逻辑")
            return
        }

        var needReload = false
        // 1. 判断是否需要喂食
        if (AnimalFeedStatus.HUNGRY.name == ownerAnimal.animalFeedStatus) {
            Log.record(TAG, "🐔 小鸡饥饿状态，当前饲料库存: ${foodStock}g")
            if (feedAnimal?.value == true) { // 优化: 安全调用
                Log.record("小鸡在挨饿~Tk 尝试为你自动喂食")
                if (feedAnimal(ownerFarmId)) {
                    needReload = true
                }
            }
        }

        // 2. 使用加饭卡（仅当正在吃饭且开启配置）
        if (useBigEaterTool?.value == true && AnimalFeedStatus.EATING.name == ownerAnimal.animalFeedStatus) { // 优化: 安全调用
            Log.record(TAG, "🐔 小鸡正在吃饭，尝试使用加饭卡")
            if (useFarmTool(ownerFarmId, ToolType.BIG_EATER_TOOL)) {
                Log.farm("追加使用🍚「加饭卡」🥣成功#剩余饲料${foodStock}g") // 优化: 字符串模板
                GlobalThreadPools.sleep(1000)
                needReload = true
            } else {
                Log.record("⚠️使用🍚「加饭卡」失败（卡片饲料不足或已在使用中～）")
            }
        }

        // 3. 判断是否需要使用加速道具
        if (useAccelerateTool?.value == true) { // 优化: 安全调用
            when (ownerAnimal.animalFeedStatus) {
                AnimalFeedStatus.SLEEPY.name -> Log.record(TAG, "小鸡厨房🐔[小鸡正在睡觉中，跳过使用加速道具]")
                AnimalFeedStatus.HUNGRY.name -> { /* 饥饿时不使用 */ }
                else -> {
                    Log.record(TAG, "🐔 检查是否需要使用加速道具，当前状态: ${ownerAnimal.animalFeedStatus}")
                    if (useAccelerateTool()) {
                        needReload = true
                    }
                }
            }
        }

        // 4. 如果有操作导致状态变化，则刷新庄园信息
        if (needReload) {
            Log.record(TAG, "🐔 状态发生变化，刷新庄园信息")
            enterFarm()
            syncAnimalStatus(ownerFarmId)
        }

        // 5. 计算并安排下一次自动喂食任务
        try {
            val startEatTime = ownerAnimal.startEatTime ?: return // 优化: 如果时间为空则不继续
            val currentAnimals = animals ?: return // 优化: 如果动物列表为空则不继续

            var allFoodHaveEatten = 0.0
            var allConsumeSpeed = 0.0

            for (animal in currentAnimals) {
                allFoodHaveEatten += animal.foodHaveEatten ?: 0.0 // 优化: 提供默认值
                allConsumeSpeed += animal.consumeSpeed ?: 0.0 // 优化: 提供默认值
            }

            Log.record(TAG, "🐔 喂食计算 - 已吃饲料: ${allFoodHaveEatten}g, 消耗速度: ${allConsumeSpeed}g/s, 开始时间: ${TimeUtil.getCommonDate(startEatTime)}")

            if (allConsumeSpeed > 0) {
                val nextFeedTime = startEatTime + ((180 - allFoodHaveEatten) / allConsumeSpeed).toLong() * 1000
                val taskId = "FA|$ownerFarmId"
                // 优化: 无论任务是否存在，都执行 addChildTask，它会覆盖已有任务
                addChildTask(ChildModelTask(taskId, "FA", { feedAnimal(ownerFarmId) }, nextFeedTime))
                Log.record(TAG, "添加/更新蹲点投喂🥣[${UserMap.getCurrentMaskName()}]在[${TimeUtil.getCommonDate(nextFeedTime)}]执行") // 优化: 字符串模板和日志信息
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

        // 6. 其他功能（换装、领取饲料）
        if (listOrnaments?.value == true && Status.canOrnamentToday()) { // 优化: 安全调用
            listOrnaments()
        }
        if (unreceiveTaskAward > 0) {
            Log.record(TAG, "还有待领取的饲料")
            receiveFarmAwards()
        }
    }

    private fun animalSleepNow() {
        try {
            var s = AntFarmRpcCall.queryLoveCabin(UserMap.currentUid)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询爱心小屋失败:", jo)) {
                val sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo")
                if (sleepNotifyInfo.optBoolean("canSleep", false)) {
                    val groupId = jo.optString("groupId")
                    s = if (groupId.isNotEmpty()) {
                        AntFarmRpcCall.sleep(groupId)
                    } else {
                        AntFarmRpcCall.sleep()
                    }
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "小鸡睡觉失败:", jo)) {
                        if (groupId.isNotEmpty()) {
                            Log.farm("家庭🏡小鸡睡觉🛌")
                        } else {
                            Log.farm("小鸡睡觉🛌")
                        }
                        Status.animalSleep()
                    }
                } else {
                    Log.farm("小鸡无需睡觉🛌")
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "animalSleepNow err:")
            Log.printStackTrace(t)
        }
    }

    private fun animalWakeUpNow() {
        try {
            var s = AntFarmRpcCall.queryLoveCabin(UserMap.currentUid)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询爱心小屋失败:", jo)) {
                val sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo")
                if (!sleepNotifyInfo.optBoolean("canSleep", true)) {
                    s = AntFarmRpcCall.wakeUp()
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "小鸡起床失败:", jo)) {
                        Log.farm("小鸡起床 🛏")
                    }
                } else {
                    Log.farm("小鸡无需起床 🛏")
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "animalWakeUpNow err:")
            Log.printStackTrace(t)
        }
    }


    /**
     * 同步小鸡状态通用方法
     *
     * @param farmId 庄园id
     */
    private fun syncAnimalStatus(farmId: String?, operTag: String?, operateType: String?): JSONObject? {
        return try { // 优化: 使用表达式函数体
            JSONObject(AntFarmRpcCall.syncAnimalStatus(farmId, operTag, operateType))
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            null
        }
    }

    private fun syncAnimalStatus(farmId: String?) {
        try {
            val jo = syncAnimalStatus(farmId, "SYNC_RESUME", "QUERY_ALL")
            jo?.let { parseSyncAnimalStatusResponse(it) } // 优化: 使用安全调用和 let
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "syncAnimalStatus err:", t)
        }
    }

    private fun syncAnimalStatusAfterFeedAnimal(farmId: String?): JSONObject? {
        return try { // 优化: 使用表达式函数体
            syncAnimalStatus(farmId, "SYNC_AFTER_FEED_ANIMAL", "QUERY_EMOTION_INFO|QUERY_ORCHARD_RIGHTS")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            null
        }
    }

    private fun syncAnimalStatusQueryFamilyAnimals(farmId: String?): JSONObject? {
        return try { // 优化: 使用表达式函数体
            syncAnimalStatus(farmId, "SYNC_RESUME_FAMILY", "QUERY_ALL|QUERY_FAMILY_ANIMAL")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            null
        }
    }


    private fun syncAnimalStatusAtOtherFarm(userId: String?, friendUserId: String?) {
        try {
            val s = AntFarmRpcCall.enterFarm(userId, friendUserId)
            var jo = JSONObject(s)
            Log.runtime(TAG, "DEBUG$jo")
            jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
            val jaAnimals = jo.getJSONArray("animals")
            // 优化: 使用 for-each 循环
            for (i in 0 until jaAnimals.length()) {
                val jaAnimaJson = jaAnimals.getJSONObject(i)
                if (jaAnimaJson.getString("masterFarmId") == ownerFarmId) { // 过滤出当前用户的小鸡
                    val animal = jaAnimals.getJSONObject(i)
                    ownerAnimal = objectMapper.readValue(animal.toString(), Animal::class.java)
                    break // 优化: 找到后即可跳出循环
                }
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", e)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", t)
        }
    }

    private fun rewardFriend() {
        try {
            rewardList?.forEach { rewardFriend -> // 优化: 使用 forEach 和安全调用
                val s = AntFarmRpcCall.rewardFriend(rewardFriend.consistencyKey, rewardFriend.friendId, rewardProductNum, rewardFriend.time)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "打赏好友失败:", jo)) {
                    val rewardCount = benevolenceScore - jo.getDouble("farmProduct")
                    benevolenceScore -= rewardCount
                    Log.farm(String.format(Locale.CHINA, "打赏好友💰[%s]# 得%.2f颗爱心鸡蛋", UserMap.getMaskName(rewardFriend.friendId), rewardCount))
                } else {
                    Log.record(jo.optString("memo")) // 优化: 使用 optString 避免异常
                    Log.runtime(s)
                }
            }
            rewardList = null
        } catch (t: Throwable) {
            Log.runtime(TAG, "rewardFriend err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun recallAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?, user: String?) {
        try {
            val s = AntFarmRpcCall.recallAnimal(animalId, currentFarmId, masterFarmId)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "召回小鸡失败:", jo)) {
                val foodHaveStolen = jo.getDouble("foodHaveStolen")
                Log.farm("召回小鸡📣，偷吃[${user}]#${foodHaveStolen}g") // 优化: 字符串模板
                // 这里不需要加
                // add2FoodStock((int)foodHaveStolen);
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "recallAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun sendBackAnimal() {
        // 优化: 使用卫语句提前返回
        val currentAnimals = animals ?: return

        try {
            for (animal in currentAnimals) {
                if (animal.animalInteractStatus == AnimalInteractStatus.STEALING.name &&
                    animal.subAnimalType != SubAnimalType.GUEST.name &&
                    animal.subAnimalType != SubAnimalType.WORK.name) {
                    // 赶鸡
                    var user = AntFarmRpcCall.farmId2UserId(animal.masterFarmId)
                    var isSendBackAnimal = sendBackAnimalList?.value?.contains(user) ?: false // 优化: 安全调用并提供默认值
                    if (sendBackAnimalType?.value == SendBackAnimalType.BACK) { // 优化: 安全调用
                        isSendBackAnimal = !isSendBackAnimal
                    }
                    if (isSendBackAnimal) {
                        continue
                    }
                    val sendTypeInt = sendBackAnimalWay?.value ?: SendBackAnimalWay.NORMAL // 优化: 安全调用并提供默认值
                    user = UserMap.getMaskName(user)
                    val s = AntFarmRpcCall.sendBackAnimal(SendBackAnimalWay.nickNames[sendTypeInt], animal.animalId, animal.currentFarmId, animal.masterFarmId)
                    val jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "遣返小鸡失败:", jo)) {
                        val logMessage = if (sendTypeInt == SendBackAnimalWay.HIT) {
                            if (jo.has("hitLossFood")) {
                                if (jo.has("finalFoodStorage")) foodStock = jo.getInt("finalFoodStorage")
                                "胖揍小鸡🤺[$user]，掉落[${jo.getInt("hitLossFood")}g]"
                            } else "[$user]的小鸡躲开了攻击"
                        } else {
                            "驱赶小鸡🧶[$user]"
                        }
                        Log.farm(logMessage)
                    } else {
                        Log.record(jo.optString("memo")) // 优化: 使用 optString
                        Log.runtime(s)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "sendBackAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun receiveToolTaskReward() {
        try {
            val s = AntFarmRpcCall.listToolTaskDetails()
            val jo = JSONObject(s)

            if (ResChecker.checkRes(TAG + "查询道具任务失败:", jo)) {
                val jaList = jo.getJSONArray("list")

                // 优化: 使用 for-each 循环
                for (i in 0 until jaList.length()) {
                    val joItem = jaList.getJSONObject(i)

                    if (joItem.optString("taskStatus") == TaskStatus.FINISHED.name) {
                        val bizInfo = JSONObject(joItem.getString("bizInfo"))
                        val awardType = bizInfo.getString("awardType")
                        val toolType = ToolType.valueOf(awardType)

                        // 优化: 使用 any 函数判断是否已满，更简洁
                        val isFull = farmTools.any { it.toolType == toolType && it.toolCount >= it.toolHoldLimit }
                        if (isFull) {
                            Log.record(TAG, "领取道具[${toolType.nickName()}]#已满，暂不领取")
                            continue
                        }
                        val awardCount = bizInfo.getInt("awardCount")
                        val taskType = joItem.getString("taskType")
                        val taskTitle = bizInfo.getString("taskTitle")
                        val receiveResultStr = AntFarmRpcCall.receiveToolTaskReward(awardType, awardCount, taskType)
                        val receiveResultJo = JSONObject(receiveResultStr)
                        if (ResChecker.checkRes(TAG + "领取道具任务奖励失败:", receiveResultJo)) {
                            Log.farm("领取道具🎖️[${taskTitle}-${toolType.nickName()}]#${awardCount} 张") // 优化: 字符串模板
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "receiveToolTaskReward err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun harvestProduce(farmId: String?) {
        try {
            val s = AntFarmRpcCall.harvestProduce(farmId)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "收获爱心鸡蛋失败:", jo)) {
                val harvest = jo.getDouble("harvestBenevolenceScore")
                harvestBenevolenceScore = jo.getDouble("finalBenevolenceScore")
                Log.farm("收取鸡蛋🥚[${harvest}颗]#剩余${harvestBenevolenceScore}颗") // 优化: 字符串模板
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "harvestProduce err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /* 捐赠爱心鸡蛋 */
    private fun handleDonation(donationType: Int) {
        try {
            val s = AntFarmRpcCall.listActivityInfo()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询捐赠活动失败:", jo)) {
                val jaActivityInfos = jo.getJSONArray("activityInfos")
                var activityId: String? = null
                var isDonation = false
                // 优化: for-each
                for (i in 0 until jaActivityInfos.length()) {
                    jo = jaActivityInfos.getJSONObject(i)
                    // 优化: 使用 opt... 方法避免类型转换异常
                    if (jo.optInt("donationTotal") != jo.optInt("donationLimit")) {
                        activityId = jo.getString("activityId")
                        val activityName = jo.optString("projectName", activityId)
                        if (performDonation(activityId, activityName)) {
                            isDonation = true
                            if (donationType == DonationCount.ONE) { // 优化: 移除 Companion
                                break
                            }
                        }
                    }
                }
                if (isDonation) {
                    val userId = UserMap.currentUid
                    Status.donationEgg(userId)
                }
                if (activityId == null) {
                    Log.record(TAG, "今日已无可捐赠的活动")
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "donation err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun performDonation(activityId: String?, activityName: String?): Boolean {
        try {
            val s = AntFarmRpcCall.donation(activityId, 1)
            val donationResponse = JSONObject(s)
            if (ResChecker.checkRes(TAG + "捐赠爱心鸡蛋失败:", donationResponse)) {
                val donationDetails = donationResponse.getJSONObject("donation")
                harvestBenevolenceScore = donationDetails.getDouble("harvestBenevolenceScore")
                Log.farm("捐赠活动❤️[${activityName}]#累计捐赠${donationDetails.getInt("donationTimesStat")}次") // 优化: 字符串模板
                return true
            }
        } catch (t: Throwable) {
            Log.printStackTrace(t)
        }
        return false
    }

    private fun answerQuestion(activityId: String?) {
        try {
            val today = TimeUtil.getDateStr2()
            val tomorrow = TimeUtil.getDateStr2(1)
            val farmAnswerCache = DataStore.getOrCreate<MutableMap<String, String>>(FARM_ANSWER_CACHE_KEY) as MutableMap<String, String>
            cleanOldAnswers(farmAnswerCache, today)
            // 检查是否今天已经答过题
            if (Status.hasFlagToday(ANSWERED_FLAG)) {
                if (!Status.hasFlagToday(CACHED_FLAG)) {
                    val jo = JSONObject(DadaDailyRpcCall.home(activityId))
                    if (ResChecker.checkRes(TAG + "查询答题活动失败:", jo)) {
                        val operationConfigList = jo.getJSONArray("operationConfigList")
                        updateTomorrowAnswerCache(operationConfigList, tomorrow)
                        Status.setFlagToday(CACHED_FLAG)
                    }
                }
                return
            }

            // 获取题目信息
            val jo = JSONObject(DadaDailyRpcCall.home(activityId))
            if (!ResChecker.checkRes(TAG + "获取答题题目失败:", jo)) return

            val question = jo.getJSONObject("question")
            val questionId = question.getLong("questionId")
            val labels = question.getJSONArray("label")
            val title = question.getString("title")

            var answer: String? = null
            var cacheHit = false
            val cacheKey = "$title|$today"

            // 改进的缓存匹配逻辑
            if (farmAnswerCache.containsKey(cacheKey)) {
                val cachedAnswer = farmAnswerCache[cacheKey] ?: "" // 优化: 提供默认值
                Log.farm("🎉 缓存[$cachedAnswer] 🎯 题目：$cacheKey")
                val options = (0 until labels.length()).map { labels.getString(it) }

                // 1. 首先尝试精确匹配
                answer = options.firstOrNull { it == cachedAnswer }

                // 2. 如果精确匹配失败，尝试模糊匹配
                if (answer == null) {
                    answer = options.firstOrNull { it.contains(cachedAnswer) || cachedAnswer.contains(it) }
                    if (answer != null) {
                        Log.farm("⚠️ 缓存模糊匹配成功：$cachedAnswer → $answer")
                    }
                }
                if (answer != null) {
                    cacheHit = true
                }
            }

            // 缓存未命中时调用AI
            if (!cacheHit) {
                Log.record(TAG, "缓存未命中，尝试使用AI答题：$title")
                // [修复一] 替换JsonUtil.jsonArrayToList
                val options = (0 until labels.length()).map { labels.getString(it) }
                answer = AnswerAI.getAnswer(title, options, "farm").takeIf { !it.isNullOrEmpty() } ?: options.firstOrNull() // 优化: 使用 takeIf 和 Elvis
            }
            
            // 如果最终没有答案，则无法继续
            if (answer == null) {
                Log.error(TAG, "无法确定答案，跳过答题: $title")
                return
            }

            // 提交答案
            val joDailySubmit = JSONObject(DadaDailyRpcCall.submit(activityId, answer, questionId))
            Status.setFlagToday(ANSWERED_FLAG)
            if (ResChecker.checkRes(TAG + "提交答题答案失败:", joDailySubmit)) {
                val extInfo = joDailySubmit.getJSONObject("extInfo")
                val correct = joDailySubmit.getBoolean("correct")
                Log.farm("饲料任务答题：" + (if (correct) "正确" else "错误") + "领取饲料［" + extInfo.getString("award") + "g］")
                val operationConfigList = joDailySubmit.getJSONArray("operationConfigList")
                updateTomorrowAnswerCache(operationConfigList, tomorrow)
                Status.setFlagToday(CACHED_FLAG)
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "答题出错", e)
        }
    }

    /**
    * 更新明日答案缓存
    *
    * @param operationConfigList 操作配置列表
    * @param date                日期字符串，格式 "yyyy-MM-dd"
    */
    private fun updateTomorrowAnswerCache(operationConfigList: JSONArray, date: String?) {
        try {
            Log.runtime(TAG, "updateTomorrowAnswerCache 开始更新缓存")
            val farmAnswerCache = DataStore.getOrCreate<MutableMap<String, String>>(FARM_ANSWER_CACHE_KEY)
            // 优化: for-each
            for (j in 0 until operationConfigList.length()) {
                val operationConfig = operationConfigList.getJSONObject(j)
                if ("PREVIEW_QUESTION" == operationConfig.optString("type")) { // 优化: 使用 optString
                    val previewTitle = "${operationConfig.getString("title")}|${date}"
                    val actionTitle = JSONArray(operationConfig.getString("actionTitle"))
                    // 优化: for-each
                    for (k in 0 until actionTitle.length()) {
                        val joActionTitle = actionTitle.getJSONObject(k)
                        if (joActionTitle.getBoolean("correct")) {
                            val nextAnswer = joActionTitle.getString("title")
                            farmAnswerCache[previewTitle] = nextAnswer // 优化: 使用map的[]语法
                            break // 新增: 找到正确答案后即可跳出内层循环
                        }
                    }
                }
            }
            put(FARM_ANSWER_CACHE_KEY, farmAnswerCache)
            Log.runtime(TAG, "updateTomorrowAnswerCache 缓存更新完毕")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "updateTomorrowAnswerCache 错误:", e)
        }
    }


    /**
    * 清理缓存超过7天的B答案
    */
    private fun cleanOldAnswers(farmAnswerCache: MutableMap<String, String>?, today: String?) {
        // 新增中文注释: 这是一个规范化重构的例子，使用Java 8的日期API，使代码更健壮、易读
        try {
            Log.runtime(TAG, "cleanOldAnswers 开始清理缓存")
            if (farmAnswerCache.isNullOrEmpty()) return

            val todayDate = try {
                LocalDate.parse(today)
            } catch (e: DateTimeParseException) {
                Log.error(TAG, "无法解析当前日期: $today")
                return
            }
            val daysToKeep = 7L // 优化: 定义为Long类型

            // [修复二] 使用迭代器安全删除并正确计算删除数量
            val initialSize = farmAnswerCache.size
            val iterator = farmAnswerCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val key = entry.key
                val dateStr = key.substringAfter('|', "")
                if (dateStr.isEmpty()) {
                    continue // 不是有效的带日期key，不删除
                }
                try {
                    val entryDate = LocalDate.parse(dateStr)
                    val daysBetween = ChronoUnit.DAYS.between(entryDate, todayDate)
                    if (daysBetween > daysToKeep) {
                        Log.runtime(TAG, "删除过期答案: $key")
                        iterator.remove() // 使用迭代器安全删除
                    }
                } catch (e: DateTimeParseException) {
                    Log.runtime(TAG, "无法解析缓存中的日期: $dateStr, 保留该条目")
                    // 日期格式错误，不删除
                }
            }

            val removedCount = initialSize - farmAnswerCache.size
            if (removedCount > 0) {
                put(FARM_ANSWER_CACHE_KEY, farmAnswerCache)
            }
            Log.runtime(TAG, "cleanOldAnswers 清理缓存完毕, 删除了 $removedCount 条")

        } catch (e: Exception) {
            Log.printStackTrace(TAG, "cleanOldAnswers error:", e)
        }
    }

    /**
     * 将日期字符串转为数字格式
     *
     * @param dateStr 日期字符串，格式 "yyyy-MM-dd"
     * @return 日期数字格式，如 "2025-04-05" → 20250405
     */
     // 新增中文注释: 这个方法在 cleanOldAnswers 重构后不再需要，但暂时保留以防其他地方调用
    private fun convertDateToInt(dateStr: String?): Int {
        Log.runtime(TAG, "convertDateToInt 开始转换日期：$dateStr")
        if (dateStr == null || !dateStr.matches("""\d{4}-\d{2}-\d{2}""".toRegex())) { // 优化: 使用正则表达式检查格式
            Log.error("日期格式错误：$dateStr")
            return -1
        }
        return try {
            // 优化: 使用更简洁的方式进行转换
            dateStr.replace("-", "").toInt()
        } catch (e: NumberFormatException) {
            Log.error(TAG, "日期转换失败：" + dateStr + e.message)
            -1
        }
    }

    private fun recordFarmGame(gameType: GameType) {
        try {
            do {
                try {
                    var jo = JSONObject(AntFarmRpcCall.initFarmGame(gameType.name))
                    if (ResChecker.checkRes(TAG + "初始化庄园游戏失败:", jo)) {
                        if (jo.getJSONObject("gameAward").optBoolean("level3Get", false)) { // 优化: optBoolean
                            return
                        }
                        if (jo.optInt("remainingGameCount", 1) == 0) {
                            return
                        }
                        jo = JSONObject(AntFarmRpcCall.recordFarmGame(gameType.name))
                        if (ResChecker.checkRes(TAG + "记录庄园游戏失败:", jo)) {
                            val awardInfos = jo.getJSONArray("awardInfos")
                            val award = StringBuilder()
                            // 优化: for-each
                            for (i in 0 until awardInfos.length()) {
                                val awardInfo = awardInfos.getJSONObject(i)
                                award.append(awardInfo.getString("awardName")).append("*").append(awardInfo.getInt("awardCount"))
                            }
                            if (jo.has("receiveFoodCount")) {
                                award.append(";肥料*").append(jo.getString("receiveFoodCount"))
                            }
                            Log.farm("庄园游戏🎮[${gameType.nickName}]#$award") // 优化: 字符串模板
                            if (jo.optInt("remainingGameCount", 0) > 0) {
                                continue
                            }
                        }
                    }
                    break
                } finally {
                    GlobalThreadPools.sleep(2000)
                }
            } while (true)
        } catch (t: Throwable) {
            Log.runtime(TAG, "recordFarmGame err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 庄园任务，目前支持i
     * 视频，杂货铺，抽抽乐，家庭，618会场，芭芭农场，小鸡厨房
     * 添加组件，雇佣，会员签到，逛咸鱼，今日头条极速版，UC浏览器
     * 一起拿饲料，到店付款，线上支付，鲸探
     */
    private fun doFarmTasks() {
        try {
            //手动屏蔽以下任务，防止死循环
            val presetBad: Set<String> = setOf( // 优化: 使用 setOf 创建不可变集合
                "HEART_DONATION_ADVANCED_FOOD_V2",  //香草芒果冰糕任务
                "HEART_DONATE",  //爱心捐赠
                "SHANGOU_xiadan",  //去买秋天第一杯奶茶
                "HUABEI_MAP_180", //用花呗完成一笔支付
                "OFFLINE_PAY",  //到店付款,线下支付
                "ONLINE_PAY",  //在线支付
                "xincun2023"
            )

            val badTaskSet = DataStore.getOrCreate<MutableSet<String>>("badFarmTaskSet")
            badTaskSet.addAll(presetBad)
            put("badFarmTaskSet", badTaskSet)
            val jo = JSONObject(AntFarmRpcCall.listFarmTask())
            if (ResChecker.checkRes(TAG + "查询庄园任务失败:", jo)) {
                val farmTaskList = jo.getJSONArray("farmTaskList")
                // 优化: for-each
                for (i in 0 until farmTaskList.length()) {
                    val task = farmTaskList.getJSONObject(i)
                    val title = task.optString("title", "未知任务")
                    val bizKey = task.getString("bizKey")
                    
                    // 优化: 逻辑合并和卫语句
                    if (badTaskSet.contains(bizKey)) {
                        Log.runtime(TAG, "跳过屏蔽的任务：$title")
                        continue
                    }
                    if (Status.hasFlagToday("farm::task::limit::$bizKey")) {
                        Log.runtime(TAG, "达上限的任务：$title")
                        continue
                    }
                    if (task.getString("taskStatus") == TaskStatus.TODO.name) {
                        if ("VIDEO_TASK" == bizKey) {
                            val taskVideoDetailjo = JSONObject(AntFarmRpcCall.queryTabVideoUrl())
                            if (ResChecker.checkRes(TAG + "查询视频任务失败:", taskVideoDetailjo)) {
                                val videoUrl = taskVideoDetailjo.getString("videoUrl")
                                // 优化: 使用 Kotlin 的字符串处理函数
                                val contentId = videoUrl.substringAfter("&contentId=").substringBefore("&refer")
                                val videoDetailjo = JSONObject(AntFarmRpcCall.videoDeliverModule(contentId))
                                if (ResChecker.checkRes(TAG + "视频投递失败:", videoDetailjo)) {
                                    //等待15s
                                    GlobalThreadPools.sleep(15 * 1000L)
                                    val resultVideojo = JSONObject(AntFarmRpcCall.videoTrigger(contentId))
                                    if (ResChecker.checkRes(TAG + "视频触发失败:", resultVideojo)) {
                                        Log.farm("庄园任务🧾[$title]")
                                    }
                                }
                                GlobalThreadPools.sleep(1000)
                            }
                        } else if ("ANSWER" == bizKey) {
                            answerQuestion("100") //答题
                            GlobalThreadPools.sleep(1000)
                        } else {
                            // 安全计数，避免 NPE 警告
                            val count = farmTaskTryCount.computeIfAbsent(bizKey) { AtomicInteger(0) }!!.incrementAndGet()
                            val taskDetailjo = JSONObject(AntFarmRpcCall.doFarmTask(bizKey))
                            if (count > 1) {
                                // 超过 1 次视为失败任务
                                Log.error("庄园任务(超过1次)标记失败：$title\n$taskDetailjo")
                                badTaskSet.add(bizKey)
                                put("badFarmTaskSet", badTaskSet)
                            } else {
                                Log.farm("庄园任务🧾[$title]")
                            }
                            GlobalThreadPools.sleep(1000)
                        }
                    }
                    if ("ANSWER" == bizKey && !Status.hasFlagToday(CACHED_FLAG)) { //单独处理答题任务
                        answerQuestion("100") //答题
                        GlobalThreadPools.sleep(1000)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "doFarmTasks 错误:", t)
        }
    }

    private fun receiveFarmAwards() {
        try {
            var doubleCheck: Boolean
            var isFeedFull = false // 添加饲料槽已满的标志
            do {
                doubleCheck = false
                val jo = JSONObject(AntFarmRpcCall.listFarmTask())
                if (ResChecker.checkRes(TAG + "查询庄园任务失败:", jo)) {
                    val farmTaskList = jo.getJSONArray("farmTaskList")
                    jo.optJSONObject("signList")?.let { farmSign(it) } // 优化: 安全调用

                    // 优化: for-each
                    for (i in 0 until farmTaskList.length()) {
                        if (isFeedFull) break // 如果饲料槽已满，跳过后续任务的领取

                        val task = farmTaskList.getJSONObject(i)
                        if (task.getString("taskStatus") == TaskStatus.FINISHED.name) {
                            val taskTitle = task.optString("title", "未知任务")
                            val awardCount = task.optInt("awardCount", 0)
                            val taskId = task.optString("taskId")

                            if (task.optString("awardType") == "ALLPURPOSE") {
                                if (awardCount + foodStock > foodStockLimit) {
                                    unreceiveTaskAward++
                                    Log.record(TAG, "${taskTitle}领取${awardCount}g饲料后将超过[${foodStockLimit}g]上限!终止领取") // 优化: 字符串模板
                                    break
                                }
                            }
                            val receiveTaskAwardjo = JSONObject(AntFarmRpcCall.receiveFarmTaskAward(taskId))
                            if (ResChecker.checkRes(TAG + "领取庄园任务奖励失败:", receiveTaskAwardjo)) {
                                add2FoodStock(awardCount)
                                Log.farm("庄园奖励[${taskTitle}]#${awardCount}g") // 优化: 字符串模板
                                doubleCheck = true
                                if (unreceiveTaskAward > 0) unreceiveTaskAward--
                            } else {
                                val resultCode = receiveTaskAwardjo.optString("resultCode", "")
                                val memo = receiveTaskAwardjo.optString("memo", "")
                                if ("331" == resultCode) {
                                    isFeedFull = true
                                    Log.record(TAG, "检测到饲料槽已满，停止领取任务奖励: $memo")
                                    break
                                }
                            }
                        }
                        GlobalThreadPools.sleep(1000)
                    }
                }
            } while (doubleCheck && !isFeedFull) // 如果饲料槽已满，不再进行双重检查
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "receiveFarmAwards 错误:", t)
        }
    }

    private fun farmSign(signList: JSONObject) {
        try {
            val flag = "farm::sign"
            if (Status.hasFlagToday(flag)) return
            val jaFarmSignList = signList.getJSONArray("signList")
            val currentSignKey = signList.getString("currentSignKey")
            // 优化: for-each
            for (i in 0 until jaFarmSignList.length()) {
                val jo = jaFarmSignList.getJSONObject(i)
                val signKey = jo.getString("signKey")
                if (currentSignKey == signKey) {
                    if (!jo.getBoolean("signed")) {
                        val awardCount = jo.getString("awardCount")
                        val signResponse = JSONObject(AntFarmRpcCall.sign()) // 优化: 直接创建JSONObject
                        if (ResChecker.checkRes(TAG + "庄园签到失败:", signResponse)) {
                            Log.farm("庄园签到📅获得饲料${awardCount}g") // 优化: 字符串模板
                            Status.setFlagToday(flag)
                        }
                    }
                    return // 优化: 找到当前签到日期后即可退出
                }
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "庄园签到 JSON解析错误:", e)
        }
    }

    /**
     * 喂鸡
     *
     * @param farmId 庄园ID
     * @return true: 喂鸡成功，false: 喂鸡失败
     */
    private fun feedAnimal(farmId: String?): Boolean {
        try {
            // 优化: 使用卫语句和 when 表达式，使逻辑更清晰
            when (ownerAnimal.animalFeedStatus) {
                AnimalFeedStatus.SLEEPY.name -> {
                    Log.runtime(TAG, "投喂小鸡🥣[小鸡正在睡觉中，跳过投喂]")
                    return false
                }
                AnimalFeedStatus.EATING.name -> {
                    Log.runtime(TAG, "投喂小鸡🥣[小鸡正在吃饭中，跳过投喂]")
                    return false
                }
            }

            if (foodStock < 180) {
                Log.record(TAG, "喂鸡饲料不足")
                return false
            }

            val jo = JSONObject(AntFarmRpcCall.feedAnimal(farmId))
            if (ResChecker.checkRes(TAG + "喂鸡失败:", jo)) {
                val remainingFoodStock = jo.optInt("foodStock", foodStock - 180)
                Log.farm("投喂小鸡🥣[180g]#剩余${remainingFoodStock}g")
                return true
            } else {
                val resultCode = jo.optString("resultCode", "")
                val memo = jo.optString("memo", "")
                if ("311" == resultCode) {
                    Log.record(TAG, "投喂小鸡🥣[$memo]")
                } else {
                    Log.runtime(TAG, "投喂小鸡失败: $jo")
                }
                return false
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "feedAnimal err:", t)
        }
        return false
    }

    /**
     * 加载持有道具信息
     */
    private fun listFarmTool() {
        try {
            val jo = JSONObject(AntFarmRpcCall.listFarmTool())
            if (ResChecker.checkRes(TAG + "查询道具列表失败:", jo)) {
                val jaToolList = jo.getJSONArray("toolList")
                // 优化: 使用 map 创建数组，代码更简洁
                farmTools = Array(jaToolList.length()) { i ->
                    val toolJson = jaToolList.getJSONObject(i)
                    FarmTool().apply {
                        toolId = toolJson.optString("toolId", "")
                        toolType = ToolType.valueOf(toolJson.getString("toolType"))
                        toolCount = toolJson.getInt("toolCount")
                        toolHoldLimit = toolJson.optInt("toolHoldLimit", 20)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "listFarmTool err:", t)
        }
    }

    /**
     * 连续使用加速卡
     *
     * @return true: 使用成功，false: 使用失败
     */
    private fun useAccelerateTool(): Boolean {
        if (Status.hasFlagToday("accelerateToolUsedUp")) {
            Log.record(TAG, "使用加速卡已达今日使用上限，跳过使用。")
            return false
        }
        if (!Status.canUseAccelerateTool()) return false
        
        // 优化: 安全调用
        if (useAccelerateToolContinue?.value == false && AnimalBuff.ACCELERATING.name == ownerAnimal.animalBuff) {
            return false
        }
        
        syncAnimalStatus(ownerFarmId)
        val currentAnimals = animals ?: return false // 优化: 卫语句
        var consumeSpeed = 0.0
        var allFoodHaveEatten = 0.0
        val nowTime = System.currentTimeMillis() / 1000
        
        for (animal in currentAnimals) {
            if (animal.masterFarmId == ownerFarmId) {
                consumeSpeed = animal.consumeSpeed ?: 0.0 // 优化: 提供默认值
            }
            allFoodHaveEatten += animal.foodHaveEatten ?: 0.0 // 优化: 提供默认值
            allFoodHaveEatten += (animal.consumeSpeed ?: 0.0) * (nowTime - (animal.startEatTime ?: 0L).toDouble() / 1000) // 优化: 提供默认值
        }
        
        var isUseAccelerateTool = false
        // consumeSpeed: g/s
        // AccelerateTool: -1h = -60m = -3600s
        while (180 - allFoodHaveEatten >= consumeSpeed * 3600) {
            // 优化: 安全调用
            if (useAccelerateToolWhenMaxEmotion?.value == true && finalScore != 100.0) {
                break
            }
            if (useFarmTool(ownerFarmId, ToolType.ACCELERATETOOL)) {
                allFoodHaveEatten += consumeSpeed * 3600
                isUseAccelerateTool = true
                Status.useAccelerateTool()
                GlobalThreadPools.sleep(1000)
            } else {
                break
            }
            // 优化: 安全调用
            if (useAccelerateToolContinue?.value == false) {
                break
            }
        }
        return isUseAccelerateTool
    }

    private fun useFarmTool(targetFarmId: String?, toolType: ToolType): Boolean {
        try {
            val s = AntFarmRpcCall.listFarmTool()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询道具列表失败:", jo)) {
                val jaToolList = jo.getJSONArray("toolList")
                // 优化: for-each
                for (i in 0 until jaToolList.length()) {
                    jo = jaToolList.getJSONObject(i)
                    if (toolType.name == jo.getString("toolType")) {
                        val toolCount = jo.getInt("toolCount")
                        if (toolCount > 0) {
                            val toolId = jo.optString("toolId", "")
                            val useResultStr = AntFarmRpcCall.useFarmTool(targetFarmId, toolId, toolType.name)
                            val useResultJo = JSONObject(useResultStr)
                            if (ResChecker.checkRes(TAG + "使用道具失败:", useResultJo)) {
                                Log.farm("使用道具🎭[${toolType.nickName()}]#剩余${toolCount - 1}张") // 优化: 字符串模板
                                return true
                            } else {
                                Log.record(useResultJo.optString("memo")) // 优化: optString
                                val resultCode = useResultJo.optString("resultCode")
                                if (toolType == ToolType.ACCELERATETOOL && "3D16" == resultCode) {
                                    // Log.record(TAG, "加速卡今日已达使用上限，设置标志位。")
                                    Status.setFlagToday("accelerateToolUsedUp") // "accelerateToolUsedUp" 是我们自定义的标志名
                                }
                            }
                            Log.runtime(s)
                        }
                        break // 优化: 找到对应类型的道具后即可退出循环
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "useFarmTool err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private fun feedFriend() {
        try {
            val feedFriendAnimalMap = feedFriendAnimalList?.value ?: return // 优化: 卫语句
            for ((userId, count) in feedFriendAnimalMap) { // 优化: 使用解构声明
                if (userId == UserMap.currentUid) continue
                
                if (!Status.canFeedFriendToday(userId, count)) continue

                val jo = JSONObject(AntFarmRpcCall.enterFarm(userId, userId))
                GlobalThreadPools.sleep(3000L)
                if (ResChecker.checkRes(TAG + "进入好友庄园失败:", jo)) {
                    val subFarmVOjo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                    val friendFarmId = subFarmVOjo.getString("farmId")
                    val jaAnimals = subFarmVOjo.getJSONArray("animals")
                    // 优化: for-each
                    for (j in 0 until jaAnimals.length()) {
                        val animalsjo = jaAnimals.getJSONObject(j)

                        if (animalsjo.getString("masterFarmId") == friendFarmId) {
                            val animalStatusVO = animalsjo.getJSONObject("animalStatusVO")
                            val animalInteractStatus = animalStatusVO.getString("animalInteractStatus")
                            val animalFeedStatus = animalStatusVO.getString("animalFeedStatus")
                            if (animalInteractStatus == AnimalInteractStatus.HOME.name && animalFeedStatus == AnimalFeedStatus.HUNGRY.name) {
                                val user = UserMap.getMaskName(userId)
                                if (foodStock < 180 && unreceiveTaskAward > 0) {
                                    Log.record(TAG, "✨还有待领取的饲料")
                                    receiveFarmAwards()
                                }
                                
                                if (foodStock >= 180) {
                                    if (Status.hasFlagToday("farm::feedFriendLimit")) return
                                    
                                    val feedFriendAnimaljo = JSONObject(AntFarmRpcCall.feedFriendAnimal(friendFarmId))
                                    if (ResChecker.checkRes(TAG + "帮喂好友小鸡失败:", feedFriendAnimaljo)) {
                                        foodStock = feedFriendAnimaljo.getInt("foodStock")
                                        Log.farm("帮喂好友🥣[${user}]的小鸡[180g]#剩余${foodStock}g") // 优化: 字符串模板
                                        Status.feedFriendToday(AntFarmRpcCall.farmId2UserId(friendFarmId))
                                    } else {
                                        Log.error(TAG, "😞喂[$user]的鸡失败$feedFriendAnimaljo")
                                        Status.setFlagToday("farm::feedFriendLimit")
                                    }
                                } else {
                                    Log.record(TAG, "😞喂鸡[$user]饲料不足")
                                }
                            }
                            break // 优化: 找到好友的小鸡后即可退出
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "feedFriendAnimal err:", t)
        }
    }


    private fun notifyFriend() {
        if (foodStock >= foodStockLimit) return
        try {
            var hasNext: Boolean
            var pageStartSum = 0
            do {
                val s = AntFarmRpcCall.rankingList(pageStartSum)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "查询排行榜失败:", jo)) {
                    hasNext = jo.getBoolean("hasNext")
                    val jaRankingList = jo.getJSONArray("rankingList")
                    pageStartSum += jaRankingList.length()
                    // 优化: for-each
                    for (i in 0 until jaRankingList.length()) {
                        val rankingItem = jaRankingList.getJSONObject(i)
                        val userId = rankingItem.getString("userId")
                        val userName = UserMap.getMaskName(userId)
                        
                        var isNotifyFriend = notifyFriendList?.value?.contains(userId) ?: false // 优化: 安全调用
                        if (notifyFriendType?.value == NotifyFriendType.DONT_NOTIFY) { // 优化: 安全调用
                            isNotifyFriend = !isNotifyFriend
                        }
                        if (!isNotifyFriend || userId == UserMap.currentUid) continue

                        val starve = rankingItem.has("actionType") && "starve_action" == rankingItem.getString("actionType")
                        if (rankingItem.getBoolean("stealingAnimal") && !starve) {
                            val farmStr = AntFarmRpcCall.enterFarm(userId, userId)
                            val farmJo = JSONObject(farmStr)
                            if (ResChecker.checkRes(TAG + "进入好友庄园失败:", farmJo)) {
                                val subFarmVO = farmJo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                                val friendFarmId = subFarmVO.getString("farmId")
                                val jaAnimals = subFarmVO.getJSONArray("animals")
                                var notified = (notifyFriend?.value == 0) // 优化: 安全调用
                                // 优化: for-each
                                for (j in 0 until jaAnimals.length()) {
                                    if (notified) continue
                                    val animalJo = jaAnimals.getJSONObject(j)
                                    val masterFarmId = animalJo.getString("masterFarmId")
                                    if (masterFarmId != friendFarmId && masterFarmId != ownerFarmId) {
                                        val animalId = animalJo.getString("animalId")
                                        notified = notifyFriend(animalJo.getJSONObject("animalStatusVO"), friendFarmId, animalId, userName)
                                    }
                                }
                            } else {
                                Log.record(farmJo.optString("memo")) // 优化: optString
                                Log.runtime(s)
                            }
                        }
                    }
                } else {
                    Log.record(jo.optString("memo")) // 优化: optString
                    Log.runtime(s)
                    hasNext = false // 新增: 发生错误时，应终止循环
                }
            } while (hasNext)
            Log.record(TAG, "饲料剩余[${foodStock}g]") // 优化: 字符串模板
        } catch (t: Throwable) {
            Log.runtime(TAG, "notifyFriend err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun notifyFriend(joAnimalStatusVO: JSONObject, friendFarmId: String?, animalId: String, user: String?): Boolean {
        try {
            if (joAnimalStatusVO.getString("animalInteractStatus") == AnimalInteractStatus.STEALING.name && 
                joAnimalStatusVO.getString("animalFeedStatus") == AnimalFeedStatus.EATING.name) {
                val jo = JSONObject(AntFarmRpcCall.notifyFriend(animalId, friendFarmId))
                if (ResChecker.checkRes(TAG + "通知好友失败:", jo)) {
                    val rewardCount = jo.getDouble("rewardCount")
                    if (jo.getBoolean("refreshFoodStock")) {
                        foodStock = jo.getDouble("finalFoodStock").toInt()
                    } else {
                        add2FoodStock(rewardCount.toInt())
                    }
                    Log.farm("通知好友📧[${user}]被偷吃#奖励${rewardCount}g") // 优化: 字符串模板
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "notifyFriend err:", t)
        }
        return false
    }

    /**
     * 解析同步响应状态
     *
     * @param jo 同步响应状态
     */
    private fun parseSyncAnimalStatusResponse(jo: JSONObject) {
        try {
            // 优化: 使用 opt... 系列方法和 let 语句，使代码更安全简洁
            val subFarmVO = jo.optJSONObject("subFarmVO") ?: return

            jo.optJSONObject("emotionInfo")?.let {
                finalScore = it.optDouble("finalScore")
            }
            
            foodStock = subFarmVO.optInt("foodStock", foodStock)

            subFarmVO.optJSONObject("manureVO")?.optJSONArray("manurePotList")?.let { manurePotList ->
                for (i in 0 until manurePotList.length()) {
                    val manurePot = manurePotList.getJSONObject(i)
                    if (manurePot.optInt("manurePotNum") >= 100) {
                        val joManurePot = JSONObject(AntFarmRpcCall.collectManurePot(manurePot.getString("manurePotNO")))
                        if (ResChecker.checkRes(TAG + "收集粪肥失败:", joManurePot)) {
                            val collectManurePotNum = joManurePot.getInt("collectManurePotNum")
                            Log.farm("打扫鸡屎🧹[$collectManurePotNum g] 第${i + 1}次")
                        } else {
                            Log.runtime(TAG, "打扫鸡屎失败: 第${i + 1}次 $joManurePot")
                        }
                    }
                }
            }

            ownerFarmId = subFarmVO.getString("farmId")
            
            subFarmVO.optJSONObject("farmProduce")?.let {
                benevolenceScore = it.optDouble("benevolenceScore")
            }

            rewardList = subFarmVO.optJSONArray("rewardList")?.let { jaRewardList ->
                Array(jaRewardList.length()) { i ->
                    val joReward = jaRewardList.getJSONObject(i)
                    RewardFriend().apply {
                        consistencyKey = joReward.getString("consistencyKey")
                        friendId = joReward.getString("friendId")
                        time = joReward.getString("time")
                    }
                }
            } ?: emptyArray()

            val jaAnimals = subFarmVO.getJSONArray("animals")
            Log.record(TAG, "🐔 解析小鸡列表，共${jaAnimals.length()}只小鸡")
            animals = Array(jaAnimals.length()) { i ->
                val animalJson = jaAnimals.getJSONObject(i)
                val animal = objectMapper.readValue(animalJson.toString(), Animal::class.java)

                // 手动从animalStatusVO中获取状态信息
                animalJson.optJSONObject("animalStatusVO")?.let {
                    animal.animalInteractStatus = it.optString("animalInteractStatus")
                    animal.animalFeedStatus = it.optString("animalFeedStatus")
                } ?: Log.record(TAG, "🐔 小鸡${i + 1} 没有animalStatusVO对象")
                
                Log.record(TAG, "🐔 小鸡${i+1} - ID: ${animal.animalId}, 主人农场ID: ${animal.masterFarmId}, 当前农场ID: ${animal.currentFarmId}, 互动状态: ${animal.animalInteractStatus}, 饲料状态: ${animal.animalFeedStatus}")

                if (animal.masterFarmId == ownerFarmId) {
                    ownerAnimal = animal
                }
                animal
            }

        } catch (t: Throwable) {
            Log.runtime(TAG, "parseSyncAnimalStatusResponse err:")
            Log.printStackTrace(TAG, t)
        }
    }


    private fun add2FoodStock(i: Int) {
        // 优化: 使用 coerceIn 保证值在范围内
        foodStock = (foodStock + i).coerceIn(0, foodStockLimit)
    }


    /**
     * 收集每日食材
     */
    private fun collectDailyFoodMaterial() {
        try {
            val userId = UserMap.currentUid
            var jo = JSONObject(AntFarmRpcCall.enterKitchen(userId))
            if (ResChecker.checkRes(TAG + "进入小鸡厨房失败:", jo)) {
                // 优化: 使用 opt... 方法
                val canCollectDailyFoodMaterial = jo.optBoolean("canCollectDailyFoodMaterial")
                val dailyFoodMaterialAmount = jo.optInt("dailyFoodMaterialAmount")
                val garbageAmount = jo.optInt("garbageAmount", 0)
                
                jo.optJSONObject("orchardFoodMaterialStatus")?.let {
                    if ("FINISHED" == it.optString("foodStatus")) {
                        val collectJo = JSONObject(AntFarmRpcCall.farmFoodMaterialCollect())
                        if (ResChecker.checkRes(TAG + "领取农场食材失败:", collectJo)) {
                            Log.farm("小鸡厨房👨🏻‍🍳[领取农场食材]#${collectJo.optInt("foodMaterialAddCount")}g") // 优化: 字符串模板和optInt
                        }
                    }
                }
                if (canCollectDailyFoodMaterial) {
                    val collectJo = JSONObject(AntFarmRpcCall.collectDailyFoodMaterial(dailyFoodMaterialAmount))
                    if (ResChecker.checkRes(TAG + "领取今日食材失败:", collectJo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取今日食材]#${dailyFoodMaterialAmount}g") // 优化: 字符串模板
                    }
                }
                if (garbageAmount > 0) {
                    val collectJo = JSONObject(AntFarmRpcCall.collectKitchenGarbage())
                    if (ResChecker.checkRes(TAG + "领取肥料失败:", collectJo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取肥料]#${collectJo.optInt("recievedKitchenGarbageAmount")}g") // 优化: 字符串模板和optInt
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "收集每日食材", t)
        }
    }

    /**
     * 领取爱心食材店食材
     */
    private fun collectDailyLimitedFoodMaterial() {
        try {
            var jo = JSONObject(AntFarmRpcCall.queryFoodMaterialPack())
            if (ResChecker.checkRes(TAG + "查询食材包失败:", jo)) {
                if (jo.optBoolean("canCollectDailyLimitedFoodMaterial")) { // 优化: 直接在if中使用
                    val dailyLimitedFoodMaterialAmount = jo.getInt("dailyLimitedFoodMaterialAmount")
                    jo = JSONObject(AntFarmRpcCall.collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount))
                    if (ResChecker.checkRes(TAG + "领取爱心食材店食材失败:", jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取爱心食材店食材]#${dailyLimitedFoodMaterialAmount}g") // 优化: 字符串模板
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "领取爱心食材店食材", t)
        }
    }

    private fun cook() {
        try {
            val userId = UserMap.currentUid
            var jo = JSONObject(AntFarmRpcCall.enterKitchen(userId))
            if (ResChecker.checkRes(TAG + "进入小鸡厨房失败:", jo)) {
                val cookTimesAllowed = jo.optInt("cookTimesAllowed", 0) // 优化: optInt
                if (cookTimesAllowed > 0) {
                    repeat(cookTimesAllowed) { // 优化: 使用 repeat 循环
                        jo = JSONObject(AntFarmRpcCall.cook(userId, "VILLA"))
                        if (ResChecker.checkRes(TAG + "制作美食失败:", jo)) {
                            val cuisineVO = jo.getJSONObject("cuisineVO")
                            Log.farm("小鸡厨房👨🏻‍🍳[${cuisineVO.getString("name")}]制作成功") // 优化: 字符串模板
                        } else {
                            Log.runtime(TAG, "小鸡厨房制作$jo")
                        }
                        GlobalThreadPools.sleep(RandomUtil.delay().toLong())
                    }
                }
            } else {
                Log.runtime(TAG, "小鸡厨房制作1$jo")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "cook err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun useSpecialFood(cuisineList: JSONArray) {
        try {
            // 优化: for-each
            for (i in 0 until cuisineList.length()) {
                val jo = cuisineList.getJSONObject(i)
                if (jo.optInt("count") <= 0) continue // 优化: optInt
                
                val cookbookId = jo.getString("cookbookId")
                val cuisineId = jo.getString("cuisineId")
                val name = jo.getString("name")
                val useJo = JSONObject(AntFarmRpcCall.useFarmFood(cookbookId, cuisineId)) // 优化: 变量名
                if (ResChecker.checkRes(TAG + "使用特殊食品失败:", useJo)) {
                    val deltaProduce = useJo.getJSONObject("foodEffect").getDouble("deltaProduce")
                    Log.farm("使用美食🍱[${name}]#加速${deltaProduce}颗爱心鸡蛋") // 优化: 字符串模板
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "useFarmFood err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun drawLotteryPlus(lotteryPlusInfo: JSONObject) {
        try {
            // 优化: 使用 opt... 和 let
            lotteryPlusInfo.optJSONObject("userSevenDaysGiftsItem")?.let { userSevenDaysGiftsItem ->
                val itemId = lotteryPlusInfo.getString("itemId")
                val userEverydayGiftItems = userSevenDaysGiftsItem.getJSONArray("userEverydayGiftItems")
                // 优化: for-each
                for (i in 0 until userEverydayGiftItems.length()) {
                    val giftItem = userEverydayGiftItems.getJSONObject(i)
                    if (giftItem.getString("itemId") == itemId) {
                        if (!giftItem.optBoolean("received")) {
                            val singleDesc = giftItem.getString("singleDesc")
                            val awardCount = giftItem.getInt("awardCount")
                            if (singleDesc.contains("饲料") && awardCount + foodStock > foodStockLimit) {
                                Log.record(TAG, "暂停领取[${awardCount}]g饲料，上限为[${foodStockLimit}]g") // 优化: 字符串模板
                                break
                            }
                            val drawResult = JSONObject(AntFarmRpcCall.drawLotteryPlus())
                            if ("SUCCESS" == drawResult.optString("memo")) {
                                Log.farm("惊喜礼包🎁[${singleDesc}*${awardCount}]") // 优化: 字符串模板
                            }
                        }
                        break // 优化: 找到就退出
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "drawLotteryPlus err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 送麦子
     */
    private fun visit() {
        try {
            val map = visitFriendList?.value ?: return // 优化: 卫语句
            val currentUid = UserMap.currentUid
            for ((userId, count) in map) { // 优化: 解构
                // 跳过自己和非法数量
                if (userId == currentUid || count <= 0) continue
                // 限制最大访问次数
                val visitCount = min(count, 3)
                // 如果今天还可以访问
                if (Status.canVisitFriendToday(userId, visitCount)) {
                    val remaining = visitFriend(userId, visitCount)
                    if (remaining > 0) {
                        Status.visitFriendToday(userId, remaining)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "visit err:")
            Log.printStackTrace(TAG, t)
        }
    }


    private fun visitFriend(userId: String?, count: Int): Int {
        var visitedTimes = 0
        try {
            var jo = JSONObject(AntFarmRpcCall.enterFarm(userId, userId))
            if (ResChecker.checkRes(TAG + "进入好友庄园失败:", jo)) {
                val farmVO = jo.getJSONObject("farmVO")
                foodStock = farmVO.getInt("foodStock")
                val subFarmVO = farmVO.getJSONObject("subFarmVO")
                if (subFarmVO.optBoolean("visitedToday", true)) return 3
                val farmId = subFarmVO.getString("farmId")
                repeat(count) { // 优化: repeat 循环
                    if (foodStock < 10) return@repeat // 优化: 提前退出 repeat
                    jo = JSONObject(AntFarmRpcCall.visitFriend(farmId))
                    if (ResChecker.checkRes(TAG + "赠送麦子失败:", jo)) {
                        foodStock = jo.getInt("foodStock")
                        Log.farm("赠送麦子🌾[${UserMap.getMaskName(userId)}]#${jo.getInt("giveFoodNum")}g") // 优化: 字符串模板
                        visitedTimes++
                        if (jo.optBoolean("isReachLimit")) {
                            Log.record(TAG, "今日给[${UserMap.getMaskName(userId)}]送麦子已达上限")
                            visitedTimes = 3
                            return@repeat // 优化: 提前退出 repeat
                        }
                    }
                    GlobalThreadPools.sleep(800L)
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "visitFriend err:")
            Log.printStackTrace(TAG, t)
        }
        return visitedTimes
    }

    private fun acceptGift() {
        try {
            val jo = JSONObject(AntFarmRpcCall.acceptGift())
            if (ResChecker.checkRes(TAG + "收取麦子失败:", jo)) {
                val receiveFoodNum = jo.getInt("receiveFoodNum")
                Log.farm("收取麦子🌾[${receiveFoodNum}g]") // 优化: 字符串模板
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "acceptGift err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 贴贴小鸡
     *
     * @param queryDayStr 日期，格式：yyyy-MM-dd
     */
    private fun diaryTietie(queryDayStr: String?) {
        try {
            var jo = JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr))
            if (ResChecker.checkRes(TAG + "查询小鸡日记失败:", jo)) {
                val data = jo.getJSONObject("data")
                val chickenDiary = data.getJSONObject("chickenDiary")
                val diaryDateStr = chickenDiary.getString("diaryDateStr")
                
                if (data.has("hasTietie") && !data.optBoolean("hasTietie", true)) {
                    val tietieJo = JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, "NEW"))
                    if (ResChecker.checkRes(TAG + "贴贴小鸡失败:", tietieJo)) {
                        val prizeType = tietieJo.getString("prizeType")
                        val prizeNum = tietieJo.optInt("prizeNum", 0)
                        Log.farm("[$diaryDateStr]贴贴小鸡💞[$prizeType*$prizeNum]")
                    } else {
                        Log.runtime(TAG, "贴贴小鸡失败: ${tietieJo.optString("memo", tietieJo.toString())}") // 优化: 记录更详细的错误
                    }
                    
                    chickenDiary.optJSONArray("statisticsList")?.let { statisticsList ->
                        for (i in 0 until statisticsList.length()) {
                            val tietieStatus = statisticsList.getJSONObject(i)
                            val tietieRoleId = tietieStatus.getString("tietieRoleId")
                            val roleTietieJo = JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, tietieRoleId))
                            if (ResChecker.checkRes(TAG + "贴贴小鸡失败:", roleTietieJo)) {
                                val prizeType = roleTietieJo.getString("prizeType")
                                val prizeNum = roleTietieJo.optInt("prizeNum", 0)
                                Log.farm("[$diaryDateStr]贴贴小鸡💞[$prizeType*$prizeNum]")
                            } else {
                                Log.runtime(TAG, "贴贴小鸡失败: ${roleTietieJo.optString("memo", roleTietieJo.toString())}")
                            }
                        }
                    }
                }
            } else {
                Log.runtime(TAG, "贴贴小鸡-获取小鸡日记详情 err: ${jo.optString("resultDesc", jo.toString())}")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryChickenDiary err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 点赞小鸡日记
     *
     * @param queryDayStr
     * @return
     */
    private fun collectChickenDiary(queryDayStr: String?): String? {
        var diaryDateStr: String? = null
        try {
            var jo = JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr))
            if (ResChecker.checkRes(TAG + "查询小鸡日记失败:", jo)) {
                val data = jo.getJSONObject("data")
                val chickenDiary = data.getJSONObject("chickenDiary")
                diaryDateStr = chickenDiary.getString("diaryDateStr")
                // 点赞小鸡日记
                if (!chickenDiary.optBoolean("collectStatus", true)) {
                    val diaryId = chickenDiary.getString("diaryId")
                    jo = JSONObject(AntFarmRpcCall.collectChickenDiary(diaryId))
                    if (jo.optBoolean("success", false)) { // 优化: 提供默认值
                        Log.farm("[$diaryDateStr]点赞小鸡日记💞成功")
                    }
                }
            } else {
                Log.runtime(TAG, "日记点赞-获取小鸡日记详情 err: ${jo.optString("resultDesc", jo.toString())}") // 优化: 记录更详细的错误
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryChickenDiary err:")
            Log.printStackTrace(TAG, t)
        }
        return diaryDateStr
    }

    private fun queryChickenDiaryList(
        queryMonthStr: String?,
        action: (String) -> Unit // ✅ 使用 Kotlin 函数类型, 返回值改为Unit
    ): Boolean {
        var hasPreviousMore = false
        try {
            val jo = if (StringUtil.isEmpty(queryMonthStr)) {
                JSONObject(AntFarmRpcCall.queryChickenDiaryList())
            } else {
                JSONObject(AntFarmRpcCall.queryChickenDiaryList(queryMonthStr))
            }

            if (ResChecker.checkRes(TAG + "查询小鸡日记列表失败:", jo)) {
                val data = jo.getJSONObject("data")
                hasPreviousMore = data.optBoolean("hasPreviousMore", false)
                data.optJSONArray("chickenDiaryBriefList")?.let { chickenDiaryBriefList ->
                    // 优化: for-each
                    for (i in 0 until chickenDiaryBriefList.length()) {
                        val item = chickenDiaryBriefList.getJSONObject(i)
                        // 优化: 使用 optBoolean
                        if (!item.optBoolean("read", true) || !item.optBoolean("collectStatus", true)) {
                            val dateStr = item.getString("dateStr")
                            action(dateStr) // ✅ 直接调用
                            GlobalThreadPools.sleep(300)
                        }
                    }
                }
            } else {
                Log.runtime(jo.optString("resultDesc", jo.toString()))
            }
        } catch (t: Throwable) {
            hasPreviousMore = false
            Log.runtime(TAG, "queryChickenDiaryList err:")
            Log.printStackTrace(TAG, t)
        }
        return hasPreviousMore
    }


    private fun doChickenDiary() {
        if (diaryTietie?.value == true) { // 贴贴小鸡 // 优化: 安全调用
            diaryTietie("")
        }

        // 小鸡日记点赞
        try {
            val collectDiaryType = collectChickenDiary?.value ?: return // 优化: 卫语句

            if (collectDiaryType >= CollectChickenDiaryType.ONCE) {
                GlobalThreadPools.sleep(300)
                val dateStr = collectChickenDiary("")
                
                if (collectDiaryType >= CollectChickenDiaryType.MONTH) {
                    var yearMonth = try {
                        dateStr?.let { YearMonth.from(LocalDate.parse(it)) } ?: YearMonth.now()
                    } catch (e: Exception) {
                        Log.error(TAG, "小鸡日记点赞-dateStr解析失败，使用当前月份: $dateStr")
                        YearMonth.now()
                    }
                    
                    var hasMore = queryChickenDiaryList(yearMonth.toString()) { collectChickenDiary(it) }
                    
                    if (collectDiaryType >= CollectChickenDiaryType.ALL) {
                        while (hasMore) {
                            GlobalThreadPools.sleep(300)
                            yearMonth = yearMonth.minusMonths(1)
                            hasMore = queryChickenDiaryList(yearMonth.toString()) { collectChickenDiary(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "doChickenDiary err:")
            Log.printStackTrace(TAG, e)
        }
    }

    private fun visitAnimal() {
        try {
            var jo = JSONObject(AntFarmRpcCall.visitAnimal())
            if (ResChecker.checkRes(TAG + "查询小鸡到访失败:", jo)) {
                val talkConfigs = jo.optJSONArray("talkConfigs") ?: return
                val talkNodes = jo.optJSONArray("talkNodes") ?: return
                
                if (talkConfigs.length() == 0) return
                
                val data = talkConfigs.getJSONObject(0)
                val farmId = data.getString("farmId")
                jo = JSONObject(AntFarmRpcCall.feedFriendAnimalVisit(farmId))
                if (ResChecker.checkRes(TAG + "喂食小鸡到访失败:", jo)) {
                    // 优化: for-each
                    for (i in 0 until talkNodes.length()) {
                        jo = talkNodes.getJSONObject(i)
                        if ("FEED" != jo.optString("type")) continue
                        
                        val consistencyKey = jo.getString("consistencyKey")
                        val prizeJo = JSONObject(AntFarmRpcCall.visitAnimalSendPrize(consistencyKey))
                        if (ResChecker.checkRes(TAG + "发送小鸡到访奖励失败:", prizeJo)) {
                            val prizeName = prizeJo.getString("prizeName")
                            Log.farm("小鸡到访💞[$prizeName]")
                        } else {
                            Log.runtime(prizeJo.optString("memo", prizeJo.toString()))
                        }
                    }
                } else {
                    Log.runtime(jo.optString("memo", jo.toString()))
                }
            } else {
                Log.runtime(jo.optString("resultDesc", jo.toString()))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "visitAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /* 雇佣好友小鸡 */
    private fun hireAnimal() {
        val animals = try { // 优化: 使用 try-catch 表达式获取 animals
            val jsonObject = enterFarm() ?: return
            if ("SUCCESS" == jsonObject.getString("memo")) {
                jsonObject.getJSONObject("farmVO").getJSONObject("subFarmVO").getJSONArray("animals")
            } else {
                Log.record(jsonObject.getString("memo"))
                null
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "getAnimalCount err:")
            Log.printStackTrace(TAG, t)
            null
        } ?: return

        try {
            // 优化: for-each
            for (i in 0 until animals.length()) {
                val joo = animals.getJSONObject(i)
                if (joo.getString("subAnimalType") == "WORK") {
                    val taskId = "HIRE|${joo.getString("animalId")}"
                    val beHiredEndTime = joo.getLong("beHiredEndTime")
                    // 优化: 无论任务是否存在，都用新时间更新
                    addChildTask(ChildModelTask(taskId, "HIRE", { hireAnimal() }, beHiredEndTime))
                    if (!hasChildTask(taskId)) {
                        Log.record(TAG, "添加蹲点雇佣👷在[${TimeUtil.getCommonDate(beHiredEndTime)}]执行")
                    }
                }
            }
            
            var animalCount = animals.length()
            if (animalCount >= 3) return

            Log.farm("雇佣小鸡👷[当前可雇佣小鸡数量:${3 - animalCount}只]")
            if (foodStock < 50) {
                Log.record(TAG, "饲料不足，暂不雇佣")
                return
            }
            
            val hireAnimalSet = hireAnimalList?.value ?: return // 优化: 卫语句
            var hasNext: Boolean
            var pageStartSum = 0
            do {
                val s = AntFarmRpcCall.rankingList(pageStartSum)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "查询排行榜失败:", jo)) {
                    hasNext = jo.getBoolean("hasNext")
                    val jaRankingList = jo.getJSONArray("rankingList")
                    pageStartSum += jaRankingList.length()
                    // 优化: for-each
                    for (i in 0 until jaRankingList.length()) {
                        val joo = jaRankingList.getJSONObject(i)
                        val userId = joo.getString("userId")
                        
                        var isHireAnimal = hireAnimalSet.contains(userId)
                        if (hireAnimalType?.value == HireAnimalType.DONT_HIRE) { // 优化: 安全调用
                            isHireAnimal = !isHireAnimal
                        }
                        if (!isHireAnimal || userId == UserMap.currentUid) continue

                        val actionTypeListStr = joo.getJSONArray("actionTypeList").toString()
                        if (actionTypeListStr.contains("can_hire_action")) {
                            if (hireAnimalAction(userId)) {
                                animalCount++
                                if (animalCount >= 3) break // 优化: 如果已雇佣满，跳出内层循环
                            }
                        }
                    }
                } else {
                    Log.record(jo.optString("memo"))
                    Log.runtime(s)
                    break
                }
            } while (hasNext && animalCount < 3)
            
            if (animalCount < 3) {
                Log.farm("雇佣小鸡失败，没有足够的小鸡可以雇佣")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "hireAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun hireAnimalAction(userId: String?): Boolean {
        try {
            val s = AntFarmRpcCall.enterFarm(userId, userId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "进入好友庄园失败:", jo)) {
                val subFarmVO = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                val farmId = subFarmVO.getString("farmId")
                val animals = subFarmVO.getJSONArray("animals")
                // 优化: 使用 for-each 查找
                for (i in 0 until animals.length()) {
                    val animal = animals.getJSONObject(i)
                    if (animal.getJSONObject("masterUserInfoVO").getString("userId") == userId) {
                        val animalStatusVo = animal.getJSONObject("animalStatusVO")
                        if (animalStatusVo.getString("animalInteractStatus") != AnimalInteractStatus.HOME.name) {
                            Log.record("${UserMap.getMaskName(userId)}的小鸡不在家")
                            return false
                        }
                        val animalId = animal.getString("animalId")
                        jo = JSONObject(AntFarmRpcCall.hireAnimal(farmId, animalId))
                        if (ResChecker.checkRes(TAG + "雇佣小鸡失败:", jo)) {
                            Log.farm("雇佣小鸡👷[${UserMap.getMaskName(userId)}] 成功") // 优化: 字符串模板
                            val newAnimals = jo.getJSONArray("animals")
                            // 优化: for-each
                            for (ii in 0 until newAnimals.length()) {
                                val joo = newAnimals.getJSONObject(ii)
                                if (joo.getString("animalId") == animalId) {
                                    val beHiredEndTime = joo.getLong("beHiredEndTime")
                                    addChildTask(ChildModelTask("HIRE|$animalId", "HIRE", { hireAnimal() }, beHiredEndTime))
                                    Log.record(TAG, "添加蹲点雇佣👷在[${TimeUtil.getCommonDate(beHiredEndTime)}]执行")
                                    break
                                }
                            }
                            return true
                        } else {
                            Log.record(jo.optString("memo"))
                            Log.runtime(s)
                        }
                        return false // 优化: 无论成功失败，找到小鸡后都应返回
                    }
                }
            } else {
                Log.record(jo.optString("memo"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "hireAnimal err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private fun drawGameCenterAward() {
        try {
            var jo = JSONObject(AntFarmRpcCall.queryGameList())
            GlobalThreadPools.sleep(3000)
            if (jo.optBoolean("success")) {
                val gameDrawAwardActivity = jo.getJSONObject("gameDrawAwardActivity")
                var canUseTimes = gameDrawAwardActivity.getInt("canUseTimes")
                while (canUseTimes > 0) {
                    try {
                        jo = JSONObject(AntFarmRpcCall.drawGameCenterAward())
                        GlobalThreadPools.sleep(3000)
                        if (jo.optBoolean("success")) {
                            canUseTimes = jo.getInt("drawRightsTimes")
                            val gameCenterDrawAwardList = jo.getJSONArray("gameCenterDrawAwardList")
                            val awards = mutableListOf<String>() // 优化: 使用可变列表
                            for (i in 0 until gameCenterDrawAwardList.length()) {
                                val gameCenterDrawAward = gameCenterDrawAwardList.getJSONObject(i)
                                val awardCount = gameCenterDrawAward.getInt("awardCount")
                                val awardName = gameCenterDrawAward.getString("awardName")
                                awards.add("$awardName*$awardCount")
                            }
                            Log.farm("庄园小鸡🎁[开宝箱:获得${awards.joinToString(",")}]") // 优化: 使用joinToString
                        } else {
                            Log.runtime(TAG, "drawGameCenterAward falsed result: $jo")
                            break // 新增: 失败时应跳出循环
                        }
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, t)
                        break // 新增: 异常时应跳出循环
                    }
                }
            } else {
                Log.runtime(TAG, "queryGameList falsed result: $jo")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryChickenDiaryList err:")
            Log.printStackTrace(TAG, t)
        }
    }

    // 小鸡换装
    private fun listOrnaments() {
        try {
            val jsonObject = JSONObject(AntFarmRpcCall.queryLoveCabin(UserMap.currentUid))
            if (ResChecker.checkRes(TAG + "查询爱心小屋失败:", jsonObject)) {
                val ownAnimal = jsonObject.getJSONObject("ownAnimal")
                val animalId = ownAnimal.getString("animalId")
                val farmId = ownAnimal.getString("farmId")
                val listResult = AntFarmRpcCall.listOrnaments()
                val jolistOrnaments = JSONObject(listResult)
                
                val achievementOrnaments = jolistOrnaments.optJSONArray("achievementOrnaments") ?: return
                
                val random = Random()
                val possibleOrnaments = mutableListOf<String>() // 收集所有可保存的套装组合
                // 优化: for-each
                for (i in 0 until achievementOrnaments.length()) {
                    val ornament = achievementOrnaments.getJSONObject(i)
                    if (ornament.getBoolean("acquired")) {
                        val sets = ornament.getJSONArray("sets")
                        // 优化: 使用 filter 和 map 简化
                        val availableSets = (0 until sets.length())
                            .map { sets.getJSONObject(it) }
                            .filter { it.getString("subType") in listOf("cap", "coat") }

                        if (availableSets.size >= 2) {
                            for (j in 0 until availableSets.size - 1) {
                                val selectedCoat = availableSets[j]
                                val selectedCap = availableSets[j + 1]
                                val id1 = selectedCoat.getString("id") // 外套 ID
                                val id2 = selectedCap.getString("id") // 帽子 ID
                                possibleOrnaments.add("$id1,$id2")
                            }
                        }
                    }
                }
                
                if (possibleOrnaments.isNotEmpty()) {
                    val ornamentsToSave = possibleOrnaments.random() // 优化: 使用 .random()
                    val saveResult = AntFarmRpcCall.saveOrnaments(animalId, farmId, ornamentsToSave)
                    val saveResultJson = JSONObject(saveResult)

                    if (saveResultJson.optBoolean("success")) {
                        val ornamentIds = ornamentsToSave.split(',')
                        var wholeSetName = ""
                        // 优化: 使用 for-each 和标签跳出
                        findSetName@ for (i in 0 until achievementOrnaments.length()) {
                            val ornament = achievementOrnaments.getJSONObject(i)
                            val sets = ornament.getJSONArray("sets")
                            if (sets.length() >= 2) {
                                val setIds = (0 until sets.length()).map { sets.getJSONObject(it).getString("id") }
                                if (setIds.containsAll(ornamentIds)) {
                                    wholeSetName = ornament.getString("name")
                                    break@findSetName
                                }
                            }
                        }
                        Log.farm("庄园小鸡💞[换装:$wholeSetName]")
                        Status.setOrnamentToday()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "listOrnaments err: ", t)
        }
    }

    // 一起拿小鸡饲料
    private fun letsGetChickenFeedTogether() {
        try {
            val jo = JSONObject(AntFarmRpcCall.letsGetChickenFeedTogether())
            if (ResChecker.checkRes(TAG + "查询一起拿饲料失败:", jo)) {
                val bizTraceId = jo.getString("bizTraceId")
                val p2pCanInvitePersonDetailList = jo.getJSONArray("p2pCanInvitePersonDetailList")
                
                var hasInvitedCount = 0
                val canInviteUserIds = mutableListOf<String>() // 保存可邀请的 userId
                // 优化: for-each
                for (i in 0 until p2pCanInvitePersonDetailList.length()) {
                    val personDetail = p2pCanInvitePersonDetailList.getJSONObject(i)
                    when (personDetail.getString("inviteStatus")) {
                        "CAN_INVITE" -> canInviteUserIds.add(personDetail.getString("userId"))
                        "HAS_INVITED" -> hasInvitedCount++
                    }
                }
                
                val remainingInvites = 5 - hasInvitedCount
                if (remainingInvites <= 0 || canInviteUserIds.isEmpty()) return

                val getFeedSet = getFeedlList?.value ?: emptySet() // 优化: 安全调用
                val usersToSend = if (getFeedType?.value == GetFeedType.GIVE) { // 优化: 安全调用
                    canInviteUserIds.filter { getFeedSet.contains(it) }
                } else {
                    canInviteUserIds.shuffled() // 随机打乱
                }.take(remainingInvites) // 取不超过剩余次数的用户

                for (userId in usersToSend) {
                    val giftJo = JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId))
                    if (giftJo.optBoolean("success")) {
                        Log.farm("一起拿小鸡饲料🥡 [送饲料：${UserMap.getMaskName(userId)}]") // 优化: 字符串模板
                    }
                }
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "letsGetChickenFeedTogether err:", e)
        }
    }

    interface DonationCount {
        companion object {
            const val ONE: Int = 0
            const val ALL: Int = 1
            val nickNames: Array<String?> = arrayOf("随机一次", "随机多次")
        }
    }

    interface RecallAnimalType {
        companion object {
            const val ALWAYS: Int = 0
            const val WHEN_THIEF: Int = 1
            const val WHEN_HUNGRY: Int = 2
            const val NEVER: Int = 3
            val nickNames: Array<String?> = arrayOf("始终召回", "偷吃召回", "饥饿召回", "暂不召回")
        }
    }

    interface SendBackAnimalWay {
        companion object {
            const val HIT: Int = 0
            const val NORMAL: Int = 1
            val nickNames: Array<String?> = arrayOf("攻击", "常规")
        }
    }

    interface SendBackAnimalType {
        companion object {
            const val BACK: Int = 0
            const val NOT_BACK: Int = 1
            val nickNames: Array<String?> = arrayOf("选中遣返", "选中不遣返")
        }
    }

    interface CollectChickenDiaryType {
        companion object {
            const val CLOSE: Int = 0
            const val ONCE: Int = 0
            const val MONTH: Int = 1
            const val ALL: Int = 2
            val nickNames: Array<String?> = arrayOf("不开启", "一次", "当月", "所有")
        }
    }

    enum class AnimalBuff {
        //小鸡buff
        ACCELERATING, INJURED, NONE
    }

    enum class AnimalFeedStatus {
        // HUNGRY, EATING, SLEEPY
        HUNGRY, // 饥饿状态：小鸡需要投喂，可以正常喂食
        EATING, // 进食状态：小鸡正在吃饭，此时不能重复投喂，会返回"不要着急，还没吃完呢"
        SLEEPY, // 睡觉状态：小鸡正在睡觉，不能投喂，需要等待醒来
        NONE // 无状态：未知或其他状态
    }
 
    enum class AnimalInteractStatus {
        //小鸡关互动状态
        HOME, // 在家：小鸡在自己的庄园里，正常状态
        GOTOSTEAL, // 去偷吃：小鸡离开庄园，准备去别的庄园偷吃
        STEALING // 偷吃中：小鸡正在别人的庄园里偷吃饲料
    }

    enum class SubAnimalType {
        NORMAL, // 普通：正常的小鸡状态
        GUEST, // 客人：小鸡去好友家做客
        PIRATE, // 海盗：小鸡外出探险
        WORK // 工作：小鸡被雇佣去工作
    }

    enum class ToolType {
        STEALTOOL, ACCELERATETOOL, SHARETOOL, FENCETOOL, NEWEGGTOOL, DOLLTOOL, ORDINARY_ORNAMENT_TOOL, ADVANCE_ORNAMENT_TOOL, BIG_EATER_TOOL, RARE_ORNAMENT_TOOL;

        fun nickName(): CharSequence? {
            return nickNames[ordinal]
        }

        companion object {
            val nickNames: Array<CharSequence?> =
                arrayOf("蹭饭卡", "加速卡", "救济卡", "篱笆卡", "新蛋卡", "公仔补签卡", "普通装扮补签卡", "高级装扮补签卡", "加饭卡", "稀有装扮补签卡")
        }
    }

    enum class GameType(val nickName: String) {
        starGame("星星球"), jumpGame("登山赛"), flyGame("飞行赛"), hitGame("欢乐揍小鸡");
    }


    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Animal {
        @JsonProperty("animalId")
        var animalId: String? = null

        @JsonProperty("currentFarmId")
        var currentFarmId: String? = null

        @JsonProperty("masterFarmId")
        var masterFarmId: String? = null

        @JsonProperty("animalBuff")
        var animalBuff: String? = null

        @JsonProperty("subAnimalType")
        var subAnimalType: String? = null

        @JsonProperty("currentFarmMasterUserId")
        var currentFarmMasterUserId: String? = null

        @JsonProperty("locationType")
        var locationType: String? = null

        @JsonProperty("startEatTime")
        var startEatTime: Long? = null

        @JsonProperty("consumeSpeed")
        var consumeSpeed: Double? = null

        @JsonProperty("foodHaveEatten")
        var foodHaveEatten: Double? = null

        // 状态信息从animalStatusVO中获取
        var animalFeedStatus: String? = null
        var animalInteractStatus: String? = null

    }

    private class RewardFriend {
        var consistencyKey: String? = null
        var friendId: String? = null
        var time: String? = null
    }

    private class FarmTool {
        var toolType: ToolType? = null
        var toolId: String? = null
        var toolCount: Int = 0
        var toolHoldLimit: Int = 0
    }

    @Suppress("unused")
    interface HireAnimalType {
        companion object {
            const val HIRE: Int = 0
            const val DONT_HIRE: Int = 1
            val nickNames: Array<String?> = arrayOf("选中雇佣", "选中不雇佣")
        }
    }

    @Suppress("unused")
    interface GetFeedType {
        companion object {
            const val GIVE: Int = 0
            const val RANDOM: Int = 1
            val nickNames: Array<String?> = arrayOf("选中赠送", "随机赠送")
        }
    }

    interface NotifyFriendType {
        companion object {
            const val NOTIFY: Int = 0
            const val DONT_NOTIFY: Int = 1
            val nickNames: Array<String?> = arrayOf("选中通知", "选中不通知")
        }
    }

    enum class PropStatus {
        REACH_USER_HOLD_LIMIT, NO_ENOUGH_POINT, REACH_LIMIT;

        fun nickName(): CharSequence? {
            return nickNames[ordinal]
        }

        companion object {
            val nickNames: Array<CharSequence?> = arrayOf("达到用户持有上限", "乐园币不足", "兑换达到上限")
        }
    }


    /**
     * 点击领取活动食物
     *
     * @param gift
     */
    private fun clickForGiftV2(gift: JSONObject?) {
        if (gift == null) return
        try {
            val resultJson = JSONObject(AntFarmRpcCall.clickForGiftV2(gift.getString("foodType"), gift.getInt("giftIndex")))
            if (ResChecker.checkRes(TAG + "领取活动食物失败:", resultJson)) {
                Log.farm("领取活动食物成功,已领取${resultJson.optInt("foodCount")}") // 优化: 字符串模板
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "clickForGiftV2 err:")
            Log.printStackTrace(TAG, e)
        }
    }

    companion object {
        private val TAG: String = AntFarm::class.java.simpleName // 优化: 使用 simpleName
        private val objectMapper = ObjectMapper()

        // 抽抽乐 / 广告任务使用的 referToken（从 VipDataIdMap 读取并缓存）
        private var antFarmReferToken: String? = null

        /**
         * 加载农场抽抽乐广告 referToken
         *
         * AntFarmReferToken：
         *  - 如果本地已有缓存，直接返回
         *  - 否则从 VipDataIdMap 加载当前账号下保存的 AntFarmReferToken
         */
        @JvmStatic
        fun loadAntFarmReferToken(): String? {
            // 优化: 使用 takeIf 和 Elvis 运算符，更简洁
            return antFarmReferToken.takeIf { !it.isNullOrEmpty() } ?: run {
                val uid = UserMap.currentUid
                val vipData = IdMapManager.getInstance(VipDataIdMap::class.java)
                vipData.load(uid)
                vipData.get("AntFarmReferToken").also { antFarmReferToken = it }
            }
        }

        init {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private const val FARM_ANSWER_CACHE_KEY = "farmQuestionCache"
        private const val ANSWERED_FLAG = "farmQuestion::answered" // 今日是否已答题
        private const val CACHED_FLAG = "farmQuestion::cache" // 是否已缓存明日答案
    }
}