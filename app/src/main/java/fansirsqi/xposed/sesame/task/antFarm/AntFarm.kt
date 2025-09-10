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
import lombok.ToString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
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
        return "庄园"
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
        modelFields.addField(ChoiceModelField("recallAnimalType", "召回小鸡", RecallAnimalType.Companion.ALWAYS, RecallAnimalType.Companion.nickNames).also {
            recallAnimalType = it
        })
        // modelFields.addField(PriorityModelField("rewardFriend", "打赏好友", priorityType.PRIORITY_2, priorityType.nickNames).also { rewardFriend = it })
        modelFields.addField(BooleanModelField("rewardFriend", "打赏好友", false).also { rewardFriend = it })
        modelFields.addField(BooleanModelField("feedAnimal", "自动喂小鸡", false).also { feedAnimal = it })
        modelFields.addField(
            SelectAndCountModelField(
                "feedFriendAnimalList",
                "喂小鸡好友列表",
                LinkedHashMap<String?, Int?>()
            ) { AlipayUser.getList() }.also { feedFriendAnimalList = it })
        modelFields.addField(PriorityModelField("getFeed", "一起拿饲料", priorityType.PRIORITY_2, priorityType.nickNames).also { getFeed = it })
        modelFields.addField(ChoiceModelField("getFeedType", "一起拿饲料 | 动作", GetFeedType.Companion.GIVE, GetFeedType.Companion.nickNames).also { getFeedType = it })
        modelFields.addField(
            SelectModelField(
                "getFeedlList",
                "一起拿饲料 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { getFeedlList = it })
        modelFields.addField(BooleanModelField("acceptGift", "收麦子", false).also { acceptGift = it })
        modelFields.addField(
            SelectAndCountModelField(
                "visitFriendList",
                "送麦子好友列表",
                LinkedHashMap<String?, Int?>()
            ) { AlipayUser.getList() }.also { visitFriendList = it })
        // modelFields.addField(PriorityModelField("hireAnimal", "雇佣小鸡 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { hireAnimal = it })
        modelFields.addField(BooleanModelField("hireAnimal", "雇佣小鸡 | 开启", false).also { hireAnimal = it })
        modelFields.addField(ChoiceModelField("hireAnimalType", "雇佣小鸡 | 动作", HireAnimalType.Companion.DONT_HIRE, HireAnimalType.Companion.nickNames).also {
            hireAnimalType = it
        })
        modelFields.addField(
            SelectModelField(
                "hireAnimalList",
                "雇佣小鸡 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { hireAnimalList = it })
        modelFields.addField(PriorityModelField("sendBackAnimal", "遣返 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { sendBackAnimal = it })
        modelFields.addField(
            ChoiceModelField(
                "sendBackAnimalWay",
                "遣返 | 方式",
                SendBackAnimalWay.Companion.NORMAL,
                SendBackAnimalWay.Companion.nickNames
            ).also { sendBackAnimalWay = it })
        modelFields.addField(
            ChoiceModelField(
                "sendBackAnimalType",
                "遣返 | 动作",
                SendBackAnimalType.Companion.NOT_BACK,
                SendBackAnimalType.Companion.nickNames
            ).also { sendBackAnimalType = it })
        modelFields.addField(
            SelectModelField(
                "dontSendFriendList",
                "遣返 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { sendBackAnimalList = it })
        modelFields.addField(PriorityModelField("notifyFriend", "通知赶鸡 | 开启", priorityType.PRIORITY_2, priorityType.nickNames).also { notifyFriend = it })
        modelFields.addField(
            ChoiceModelField(
                "notifyFriendType",
                "通知赶鸡 | 动作",
                NotifyFriendType.Companion.NOTIFY,
                NotifyFriendType.Companion.nickNames
            ).also { notifyFriendType = it })
        modelFields.addField(
            SelectModelField(
                "notifyFriendList",
                "通知赶鸡 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { notifyFriendList = it })
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
        modelFields.addField(
            ChoiceModelField(
                "collectChickenDiary",
                "小鸡日记 | 点赞",
                CollectChickenDiaryType.Companion.ONCE,
                CollectChickenDiaryType.Companion.nickNames
            ).also { collectChickenDiary = it })
        modelFields.addField(PriorityModelField("enableChouchoule", "开启小鸡抽抽乐", priorityType.PRIORITY_2, priorityType.nickNames).also { enableChouchoule = it })
        modelFields.addField(BooleanModelField("listOrnaments", "小鸡每日换装", false).also { listOrnaments = it })
        modelFields.addField(PriorityModelField("enableDdrawGameCenterAward", "开宝箱", priorityType.PRIORITY_2, priorityType.nickNames).also { enableDdrawGameCenterAward = it })
        modelFields.addField(PriorityModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", priorityType.PRIORITY_2, priorityType.nickNames).also { recordFarmGame = it })
        // modelFields.addField(BooleanModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", false).also { recordFarmGame = it })
        modelFields.addField(ListJoinCommaToStringModelField("farmGameTime", "小鸡游戏时间(范围)", ListUtil.newArrayList<String?>("2200-2400")).also { farmGameTime = it })
        modelFields.addField(BooleanModelField("family", "家庭 | 开启", false).also { family = it })
        modelFields.addField(SelectModelField("familyOptions", "家庭 | 选项", LinkedHashSet<String?>(), farmFamilyOption()).also { familyOptions = it })
        modelFields.addField(
            SelectModelField(
                "notInviteList",
                "家庭 | 好友分享排除列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { notInviteList = it })
        //        modelFields.addField(giftFamilyDrawFragment = new StringModelField("giftFamilyDrawFragment", "家庭 | 扭蛋碎片赠送用户ID(配置目录查看)", ""));
        modelFields.addField(PriorityModelField("paradiseCoinExchangeBenefit", "小鸡乐园 | 兑换权益", priorityType.PRIORITY_2, priorityType.nickNames).also { paradiseCoinExchangeBenefit = it })
        modelFields.addField(
            SelectModelField(
                "paradiseCoinExchangeBenefitList",
                "小鸡乐园 | 权益列表",
                LinkedHashSet<String?>()
            ) { ParadiseCoinBenefit.getList() }.also { paradiseCoinExchangeBenefitList = it })
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

            // if (getRunCnts() >= rewardFriend!!.value) {
            if (rewardFriend!!.value) {
                rewardFriend()
                tc.countDebug("打赏好友")
            }
            if (getRunCnts() >= sendBackAnimal!!.value) {
                sendBackAnimal()
                tc.countDebug("遣返")
            }

            if (getRunCents() >= receiveFarmToolReward!!.value) {
                receiveToolTaskReward()
                tc.countDebug("收取道具奖励")
            }

            // if (recordFarmGame!!.value) {
            if (getRunCents() >= recordFarmGame!!.value) {
                for (time in farmGameTime!!.value) {
                    if (TimeUtil.checkNowInTimeRange(time)) {
                        recordFarmGame(GameType.starGame)
                        recordFarmGame(GameType.jumpGame)
                        recordFarmGame(GameType.flyGame)
                        recordFarmGame(GameType.hitGame)
                        break
                    }
                }
                tc.countDebug("游戏改分(星星球、登山赛、飞行赛、揍小鸡)")
            }

            if (getRunCnts() >= kitchen!!.value) {
                collectDailyFoodMaterial()
                collectDailyLimitedFoodMaterial()
                cook()
                tc.countDebug("小鸡厨房")
            }

            if (getRunCents() >= chickenDiary!!.value) {
                doChickenDiary()
                tc.countDebug("小鸡日记")
            }

            if (useNewEggCard!!.value) {
                useFarmTool(ownerFarmId, ToolType.NEWEGGTOOL)
                syncAnimalStatus(ownerFarmId)
                tc.countDebug("使用新蛋卡")
            }
            if (harvestProduce!!.value && benevolenceScore >= 1) {
                Log.record(TAG, "有可收取的爱心鸡蛋")
                harvestProduce(ownerFarmId)
                tc.countDebug("收鸡蛋")
            }
            if (donation!!.value && Status.canDonationEgg(userId) && harvestBenevolenceScore >= 1) {
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

            // 雇佣小鸡
            if (hireAnimal!!.value) {
                hireAnimal()
                tc.countDebug("雇佣小鸡")
            }
            // if (getRunCnts() >= getFeed!!.value) {
            if (getRunCents() >= getFeed!!.value) {
                letsGetChickenFeedTogether()
                tc.countDebug("一起拿饲料")
            }
            //家庭
            if (family!!.value) {
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
                    Log.farm("召回小鸡📣[收获:肥料${manureCount}g]") // 使用字符串模板更简洁
                } else {
                    syncAnimalStatus(ownerFarmId)
                    var guest = false
                    ownerAnimal.subAnimalType?.let {
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
                        when (AnimalFeedStatus.valueOf(it)) {
                            AnimalFeedStatus.HUNGRY -> {
                                hungry = true
                                Log.record(TAG, "小鸡在[$userName]的庄园里挨饿")
                            }

                            AnimalFeedStatus.EATING -> Log.record(TAG, "小鸡在[$userName]的庄园里吃得津津有味")
                            AnimalFeedStatus.SLEEPY -> Log.record(TAG, "小鸡在[$userName]的庄园")
                            AnimalFeedStatus.NONE -> Log.record(TAG, "小鸡[$userName]不知道在干嘛")
                        }
                    }
                    // 2. 优化recall变量的赋值方式，并简化Companion object的调用
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
            for (i in 0..<mallItemSimpleList.length()) {
                val mallItemInfo = mallItemSimpleList.getJSONObject(i)
                val oderInfo: String?
                val spuName = mallItemInfo.getString("spuName")
                val minPrice = mallItemInfo.getInt("minPrice")
                val controlTag = mallItemInfo.getString("controlTag")
                val spuId = mallItemInfo.getString("spuId")
                oderInfo = spuName + "\n价格" + minPrice + "乐园币\n" + controlTag
                val idMap = IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java)
                idMap.add(spuId, oderInfo)
                val itemStatusList = mallItemInfo.getJSONArray("itemStatusList")
                if (!Status.canParadiseCoinExchangeBenefitToday(spuId) ||
                    !paradiseCoinExchangeBenefitList!!.value.contains(spuId) ||
                    isExchange(itemStatusList, spuId, spuName)
                ) {
                    continue
                }
                var exchangedCount = 0
                while (exchangeBenefit(spuId)) {
                    exchangedCount += 1
                    Log.farm("乐园币兑换💸#花费[" + minPrice + "乐园币]" + "#第" + exchangedCount + "次兑换" + "[" + spuName + "]")
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
            for (i in 0..<mallSubItemDetailList.length()) {
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
            for (j in 0..<itemStatusList.length()) {
                val itemStatus = itemStatusList.getString(j)
                if (PropStatus.REACH_LIMIT.name == itemStatus
                    || PropStatus.REACH_USER_HOLD_LIMIT.name == itemStatus
                    || PropStatus.NO_ENOUGH_POINT.name == itemStatus
                ) {
                    Log.record(TAG, "乐园兑换💸[" + spuName + "]停止:" + PropStatus.valueOf(itemStatus).nickName())
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
            val sleepTimeStr = sleepTime!!.value
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
            val sleepMinutesInt = sleepMinutes!!.value
            val animalWakeUpTimeCalendar = animalSleepTimeCalendar.clone() as Calendar
            animalWakeUpTimeCalendar.add(Calendar.MINUTE, sleepMinutesInt)
            val animalSleepTime = animalSleepTimeCalendar.getTimeInMillis()
            val animalWakeUpTime = animalWakeUpTimeCalendar.getTimeInMillis()
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
                Log.record(TAG, "添加定时睡觉🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalSleepTime) + "]执行")
            }
            if (!hasChildTask(wakeUpTaskId) && !afterWakeUpTime) {
                addChildTask(ChildModelTask(wakeUpTaskId, "AW", { this.animalWakeUpNow() }, animalWakeUpTime))
                Log.record(TAG, "添加定时起床🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalWakeUpTime) + "]执行")
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
                if (activityData != null) {
                    val it = activityData.keys()
                    while (it.hasNext()) {
                        val key = it.next()
                        if (key.contains("Gifts")) {
                            val gifts = activityData.optJSONArray(key)
                            if (gifts == null) {
                                continue
                            }
                            for (i in 0..<gifts.length()) {
                                val gift = gifts.optJSONObject(i)
                                clickForGiftV2(gift)
                            }
                        }
                    }
                }
                if (useSpecialFood!!.value) { //使用特殊食品
                    val cuisineList = jo.getJSONArray("cuisineList")
                    if (AnimalFeedStatus.SLEEPY.name != ownerAnimal.animalFeedStatus) useSpecialFood(cuisineList)
                }

                if (jo.has("lotteryPlusInfo")) { //彩票附加信息
                    drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"))
                }

                if (acceptGift!!.value && joFarmVO.getJSONObject("subFarmVO").has("giftRecord")
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
        // 检查ownerAnimal是否已正确初始化
        if (ownerAnimal.animalId == null) {
            Log.record(TAG, "🐔 当前ownerAnimal信息 - ID: ${ownerAnimal.animalId}, 主人农场ID: ${ownerAnimal.masterFarmId}, 当前农场ID: ${ownerAnimal.currentFarmId}")
            syncAnimalStatus(ownerFarmId)
            // 重新检查
            if (ownerAnimal.animalId == null) {
                Log.record(TAG, "🐔 错误：无法获取小鸡信息，跳过自动喂食")
                return
            }
        }

        // 打印小鸡状态和ID信息
        Log.record(TAG, "🐔 小鸡状态检查 - ID: ${ownerAnimal.animalId}, 互动状态: ${ownerAnimal.animalInteractStatus}, 饲料状态: ${ownerAnimal.animalFeedStatus}, 位置类型: ${ownerAnimal.locationType}")
        // 检查状态是否为空
        if (ownerAnimal.animalInteractStatus == null || ownerAnimal.animalFeedStatus == null) {
            Log.record(TAG, "🐔 警告：小鸡状态信息不完整，尝试重新同步")
            syncAnimalStatus(ownerFarmId)
            // 重新检查
            if (ownerAnimal.animalInteractStatus == null || ownerAnimal.animalFeedStatus == null) {
                Log.record(TAG, "🐔 错误：无法获取完整的小鸡状态信息，跳过自动喂食")
                return
            }
        }

        if (AnimalInteractStatus.HOME.name != ownerAnimal.animalInteractStatus) {
            Log.record(TAG, "🐔 小鸡不在家，跳过自动喂食逻辑")
            return  // 小鸡不在家，不执行喂养逻辑
        }

        var needReload = false
        // 1. 判断是否需要喂食
        if (AnimalFeedStatus.HUNGRY.name == ownerAnimal.animalFeedStatus) {
            Log.record(TAG, "🐔 小鸡饥饿状态，当前饲料库存: ${foodStock}g")
            if (feedAnimal!!.value) {
                Log.record("小鸡在挨饿~Tk 尝试为你自动喂食")
                if (feedAnimal(ownerFarmId)) {
                    needReload = true
                }
            }
        }

        // 2. 使用加饭卡（仅当正在吃饭且开启配置）
        if (useBigEaterTool!!.value && AnimalFeedStatus.EATING.name == ownerAnimal.animalFeedStatus) {
            Log.record(TAG, "🐔 小鸡正在吃饭，尝试使用加饭卡")
            val result = useFarmTool(ownerFarmId, ToolType.BIG_EATER_TOOL)
            if (result) {
                Log.farm("追加使用🍚「加饭卡」🥣成功#剩余饲料" + foodStock + "g")
                GlobalThreadPools.sleep(1000)
                needReload = true
            } else {
                Log.record("⚠️使用🍚「加饭卡」失败（卡片饲料不足或已在使用中～）")
            }
        }

        // 3. 判断是否需要使用加速道具
        if (useAccelerateTool!!.value && AnimalFeedStatus.HUNGRY.name != ownerAnimal.animalFeedStatus) {
            Log.record(TAG, "🐔 检查是否需要使用加速道具，当前状态: ${ownerAnimal.animalFeedStatus}")
            if (useAccelerateTool()) {
                needReload = true
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
            val startEatTime = ownerAnimal.startEatTime!!
            var allFoodHaveEatten = 0.0
            var allConsumeSpeed = 0.0

            for (animal in animals!!) {
                allFoodHaveEatten += animal.foodHaveEatten!!
                allConsumeSpeed += animal.consumeSpeed!!
            }

            Log.record(TAG, "🐔 喂食计算 - 已吃饲料: ${allFoodHaveEatten}g, 消耗速度: ${allConsumeSpeed}g/s, 开始时间: ${TimeUtil.getCommonDate(startEatTime)}")

            if (allConsumeSpeed > 0) {
                val nextFeedTime = startEatTime + ((180 - allFoodHaveEatten) / allConsumeSpeed).toLong() * 1000
                val taskId = "FA|$ownerFarmId"

                if (!hasChildTask(taskId)) {
                    addChildTask(ChildModelTask(taskId, "FA", { feedAnimal(ownerFarmId) }, nextFeedTime))
                    Log.record(TAG, "添加蹲点投喂🥣[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(nextFeedTime) + "]执行")
                } else {
                    // 更新时间即可
                    addChildTask(ChildModelTask(taskId, "FA", { feedAnimal(ownerFarmId) }, nextFeedTime))
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

        // 6. 其他功能（换装、领取饲料）
        // 小鸡换装
        if (listOrnaments!!.value && Status.canOrnamentToday()) {
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
                    s = AntFarmRpcCall.sleep()
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "小鸡睡觉失败:", jo)) {
                        Log.farm("小鸡睡觉🛌")
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
        try {
            return JSONObject(AntFarmRpcCall.syncAnimalStatus(farmId, operTag, operateType))
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            return null
        }
    }

    private fun syncAnimalStatus(farmId: String?) {
        try {
            val jo = syncAnimalStatus(farmId, "SYNC_RESUME", "QUERY_ALL")
            parseSyncAnimalStatusResponse(jo!!)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "syncAnimalStatus err:", t)
        }
    }

    private fun syncAnimalStatusAfterFeedAnimal(farmId: String?): JSONObject? {
        try {
            return syncAnimalStatus(farmId, "SYNC_AFTER_FEED_ANIMAL", "QUERY_EMOTION_INFO|QUERY_ORCHARD_RIGHTS")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return null
    }

    private fun syncAnimalStatusQueryFamilyAnimals(farmId: String?): JSONObject? {
        try {
            return syncAnimalStatus(farmId, "SYNC_RESUME_FAMILY", "QUERY_ALL|QUERY_FAMILY_ANIMAL")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return null
    }


    private fun syncAnimalStatusAtOtherFarm(userId: String?, friendUserId: String?) {
        try {
            val s = AntFarmRpcCall.enterFarm(userId, friendUserId)
            var jo = JSONObject(s)
            Log.runtime(TAG, "DEBUG$jo")
            jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
            val jaAnimals = jo.getJSONArray("animals")
            for (i in 0..<jaAnimals.length()) {
                val jaAnimaJson = jaAnimals.getJSONObject(i)
                if (jaAnimaJson.getString("masterFarmId") == ownerFarmId) { // 过滤出当前用户的小鸡
                    val animal = jaAnimals.getJSONObject(i)
                    ownerAnimal = objectMapper.readValue(animal.toString(), Animal::class.java)
                    break
                }
            }
        } catch (j: JSONException) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", j)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", t)
        }
    }

    private fun rewardFriend() {
        try {
            if (rewardList != null) {
                for (rewardFriend in rewardList) {
                    val s = AntFarmRpcCall.rewardFriend(rewardFriend.consistencyKey, rewardFriend.friendId, rewardProductNum, rewardFriend.time)
                    val jo = JSONObject(s)
                    val memo = jo.getString("memo")
                    if (ResChecker.checkRes(TAG + "打赏好友失败:", jo)) {
                        val rewardCount = benevolenceScore - jo.getDouble("farmProduct")
                        benevolenceScore -= rewardCount
                        Log.farm(String.format(Locale.CHINA, "打赏好友💰[%s]# 得%.2f颗爱心鸡蛋", UserMap.getMaskName(rewardFriend.friendId), rewardCount))
                    } else {
                        Log.record(memo)
                        Log.runtime(s)
                    }
                }
                rewardList = null
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "rewardFriend err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun recallAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?, user: String?) {
        try {
            val s = AntFarmRpcCall.recallAnimal(animalId, currentFarmId, masterFarmId)
            val jo = JSONObject(s)
            val memo = jo.getString("memo")
            if (ResChecker.checkRes(TAG + "召回小鸡失败:", jo)) {
                val foodHaveStolen = jo.getDouble("foodHaveStolen")
                Log.farm("召回小鸡📣，偷吃[" + user + "]#" + foodHaveStolen + "g")
                // 这里不需要加
                // add2FoodStock((int)foodHaveStolen);
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "recallAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun sendBackAnimal() {
        if (animals == null) {
            return
        }
        try {
            for (animal in animals) {
                if (AnimalInteractStatus.STEALING.name == animal.animalInteractStatus && (SubAnimalType.GUEST.name != animal.subAnimalType) && (SubAnimalType.WORK.name != animal.subAnimalType)) {
                    // 赶鸡
                    var user = AntFarmRpcCall.farmId2UserId(animal.masterFarmId)
                    var isSendBackAnimal = sendBackAnimalList!!.value.contains(user)
                    if (sendBackAnimalType!!.value == SendBackAnimalType.Companion.BACK) {
                        isSendBackAnimal = !isSendBackAnimal
                    }
                    if (isSendBackAnimal) {
                        continue
                    }
                    val sendTypeInt = sendBackAnimalWay!!.value
                    user = UserMap.getMaskName(user)
                    var s = AntFarmRpcCall.sendBackAnimal(SendBackAnimalWay.Companion.nickNames[sendTypeInt], animal.animalId, animal.currentFarmId, animal.masterFarmId)
                    val jo = JSONObject(s)
                    val memo = jo.getString("memo")
                    if (ResChecker.checkRes(TAG + "遣返小鸡失败:", jo)) {
                        if (sendTypeInt == SendBackAnimalWay.Companion.HIT) {
                            if (jo.has("hitLossFood")) {
                                s = "胖揍小鸡🤺[" + user + "]，掉落[" + jo.getInt("hitLossFood") + "g]"
                                if (jo.has("finalFoodStorage")) foodStock = jo.getInt("finalFoodStorage")
                            } else s = "[$user]的小鸡躲开了攻击"
                        } else {
                            s = "驱赶小鸡🧶[$user]"
                        }
                        Log.farm(s)
                    } else {
                        Log.record(memo)
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
            var s = AntFarmRpcCall.listToolTaskDetails()
            var jo = JSONObject(s)
            var memo = jo.optString("memo", "")

            if (ResChecker.checkRes(TAG + "查询道具任务失败:", jo)) {
                val jaList = jo.getJSONArray("list")

                for (i in 0 until jaList.length()) {
                    val joItem = jaList.getJSONObject(i)

                    if (joItem.optString("taskStatus") == TaskStatus.FINISHED.name) {
                        val bizInfo = JSONObject(joItem.getString("bizInfo"))
                        val awardType = bizInfo.getString("awardType")
                        val toolType = ToolType.valueOf(awardType)

                        val isFull = farmTools.any { it.toolType == toolType && it.toolCount == it.toolHoldLimit }
                        if (isFull) {
                            Log.record(TAG, "领取道具[${toolType.nickName()}]#已满，暂不领取")
                            continue
                        }
                        val awardCount = bizInfo.getInt("awardCount")
                        val taskType = joItem.getString("taskType")
                        val taskTitle = bizInfo.getString("taskTitle")
                        s = AntFarmRpcCall.receiveToolTaskReward(awardType, awardCount, taskType)
                        jo = JSONObject(s)
                        if (ResChecker.checkRes(TAG + "领取道具任务奖励失败:", jo)) {
                            Log.farm("领取道具🎖️[$taskTitle-${toolType.nickName()}]#$awardCount 张")
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
            val memo = jo.getString("memo")
            if (ResChecker.checkRes(TAG + "收获爱心鸡蛋失败:", jo)) {
                val harvest = jo.getDouble("harvestBenevolenceScore")
                harvestBenevolenceScore = jo.getDouble("finalBenevolenceScore")
                Log.farm("收取鸡蛋🥚[" + harvest + "颗]#剩余" + harvestBenevolenceScore + "颗")
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
            val memo = jo.getString("memo")
            if (ResChecker.checkRes(TAG + "查询捐赠活动失败:", jo)) {
                val jaActivityInfos = jo.getJSONArray("activityInfos")
                var activityId: String? = null
                var activityName: String?
                var isDonation = false
                for (i in 0..<jaActivityInfos.length()) {
                    jo = jaActivityInfos.getJSONObject(i)
                    if (jo.get("donationTotal") != jo.get("donationLimit")) {
                        activityId = jo.getString("activityId")
                        activityName = jo.optString("projectName", activityId)
                        if (performDonation(activityId, activityName)) {
                            isDonation = true
                            if (donationType == DonationCount.Companion.ONE) {
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
            val memo = donationResponse.getString("memo")
            if (ResChecker.checkRes(TAG + "捐赠爱心鸡蛋失败:", donationResponse)) {
                val donationDetails = donationResponse.getJSONObject("donation")
                harvestBenevolenceScore = donationDetails.getDouble("harvestBenevolenceScore")
                Log.farm("捐赠活动❤️[" + activityName + "]#累计捐赠" + donationDetails.getInt("donationTimesStat") + "次")
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
                val cachedAnswer = farmAnswerCache[cacheKey]
                Log.farm("🎉 缓存[$cachedAnswer] 🎯 题目：$cacheKey")

                // 1. 首先尝试精确匹配
                for (i in 0..<labels.length()) {
                    val option = labels.getString(i)
                    if (option == cachedAnswer) {
                        answer = option
                        cacheHit = true
                        break
                    }
                }

                // 2. 如果精确匹配失败，尝试模糊匹配
                if (!cacheHit) {
                    for (i in 0..<labels.length()) {
                        val option = labels.getString(i)
                        if (option.contains(cachedAnswer!!) || cachedAnswer.contains(option)) {
                            answer = option
                            cacheHit = true
                            Log.farm("⚠️ 缓存模糊匹配成功：$cachedAnswer → $option")
                            break
                        }
                    }
                }
            }

            // 缓存未命中时调用AI
            if (!cacheHit) {
                Log.record(TAG, "缓存未命中，尝试使用AI答题：$title")
                answer = AnswerAI.getAnswer(title, JsonUtil.jsonArrayToList(labels), "farm")
                if (answer == null || answer.isEmpty()) {
                    answer = labels.getString(0) // 默认选择第一个选项
                }
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
            for (j in 0..<operationConfigList.length()) {
                val operationConfig = operationConfigList.getJSONObject(j)
                val type = operationConfig.getString("type")
                if ("PREVIEW_QUESTION" == type) {
                    val previewTitle = operationConfig.getString("title") + "|" + date
                    val actionTitle = JSONArray(operationConfig.getString("actionTitle"))
                    for (k in 0..<actionTitle.length()) {
                        val joActionTitle = actionTitle.getJSONObject(k)
                        val isCorrect = joActionTitle.getBoolean("correct")
                        if (isCorrect) {
                            val nextAnswer = joActionTitle.getString("title")
                            farmAnswerCache.put(previewTitle, nextAnswer) // 缓存下一个问题的答案
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
        try {
            Log.runtime(TAG, "cleanOldAnswers 开始清理缓存")
            if (farmAnswerCache == null || farmAnswerCache.isEmpty()) return
            // 将今天日期转为数字格式：20250405
            val todayInt = convertDateToInt(today) // 如 "2025-04-05" → 20250405
            // 设置保留天数（例如7天）
            val daysToKeep = 7
            val cleanedMap: MutableMap<String?, String?> = HashMap()
            for (entry in farmAnswerCache.entries) {
                val key: String = entry.key
                if (key.contains("|")) {
                    val parts: Array<String?> = key.split("\\|".toRegex(), limit = 2).toTypedArray()
                    if (parts.size == 2) {
                        val dateStr = parts[1] //获取日期部分 20
                        val dateInt = convertDateToInt(dateStr)
                        if (dateInt == -1) continue
                        if (todayInt - dateInt <= daysToKeep) {
                            cleanedMap.put(entry.key, entry.value) //保存7天内的答案
                            Log.runtime(TAG, "保留 日期：" + todayInt + "缓存日期：" + dateInt + " 题目：" + parts[0])
                        }
                    }
                }
            }
            put(FARM_ANSWER_CACHE_KEY, cleanedMap)
            Log.runtime(TAG, "cleanOldAnswers 清理缓存完毕")
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
    private fun convertDateToInt(dateStr: String?): Int {
        Log.runtime(TAG, "convertDateToInt 开始转换日期：$dateStr")
        if (dateStr == null || dateStr.length != 10 || dateStr[4] != '-' || dateStr[7] != '-') {
            Log.error("日期格式错误：$dateStr")
            return -1 // 格式错误
        }
        try {
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(5, 7).toInt()
            val day = dateStr.substring(8, 10).toInt()
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                Log.error("日期无效：$dateStr")
                return -1 // 日期无效
            }
            return year * 10000 + month * 100 + day
        } catch (e: NumberFormatException) {
            Log.error(TAG, "日期转换失败：" + dateStr + e.message)
            return -1
        }
    }

    private fun recordFarmGame(gameType: GameType) {
        try {
            do {
                try {
                    var jo = JSONObject(AntFarmRpcCall.initFarmGame(gameType.name))
                    if (ResChecker.checkRes(TAG + "初始化庄园游戏失败:", jo)) {
                        if (jo.getJSONObject("gameAward").getBoolean("level3Get")) {
                            return
                        }
                        if (jo.optInt("remainingGameCount", 1) == 0) {
                            return
                        }
                        jo = JSONObject(AntFarmRpcCall.recordFarmGame(gameType.name))
                        if (ResChecker.checkRes(TAG + "记录庄园游戏失败:", jo)) {
                            val awardInfos = jo.getJSONArray("awardInfos")
                            val award = StringBuilder()
                            for (i in 0..<awardInfos.length()) {
                                val awardInfo = awardInfos.getJSONObject(i)
                                award.append(awardInfo.getString("awardName")).append("*").append(awardInfo.getInt("awardCount"))
                            }
                            if (jo.has("receiveFoodCount")) {
                                award.append(";肥料*").append(jo.getString("receiveFoodCount"))
                            }
                            // Log.farm("庄园游戏🎮[" + gameType.gameName() + "]#" + award)
                            Log.farm("庄园游戏🎮[" + gameType.nickName + "]#" + award)
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
            val presetBad: MutableSet<String> = linkedSetOf(
                "HEART_DONATION_ADVANCED_FOOD_V2",  //香草芒果冰糕任务
                "HEART_DONATE",  //爱心捐赠
                "SHANGOU_xiadan",  //去买秋天第一杯奶茶
                "HUABEI_MAP_180", //用花呗完成一笔支付
                "OFFLINE_PAY",  //到店付款,线下支付
                "ONLINE_PAY"  //在线支付
            )

            val badTaskSet = DataStore.getOrCreate<MutableSet<String>>("badFarmTaskSet")
            badTaskSet.addAll(presetBad)
            put("badFarmTaskSet", badTaskSet)
            val jo = JSONObject(AntFarmRpcCall.listFarmTask())
            if (ResChecker.checkRes(TAG + "查询庄园任务失败:", jo)) {
                val farmTaskList = jo.getJSONArray("farmTaskList")
                for (i in 0..<farmTaskList.length()) {
                    val task = farmTaskList.getJSONObject(i)
                    val title = task.optString("title", "未知任务")
                    val taskStatus = task.getString("taskStatus")
                    val bizKey = task.getString("bizKey")
                    val taskMode = task.optString("taskMode")
                    // 跳过已被屏蔽的任务
                    if (badTaskSet.contains(bizKey)) continue
                    if (TaskStatus.TODO.name == taskStatus) {
                        if (!badTaskSet.contains(bizKey)) {
                            if ("VIDEO_TASK" == bizKey) {
                                val taskVideoDetailjo = JSONObject(AntFarmRpcCall.queryTabVideoUrl())
                                if (ResChecker.checkRes(TAG + "查询视频任务失败:", taskVideoDetailjo)) {
                                    val videoUrl = taskVideoDetailjo.getString("videoUrl")
                                    val contentId = videoUrl.substring(videoUrl.indexOf("&contentId=") + 11, videoUrl.indexOf("&refer"))
                                    val videoDetailjo = JSONObject(AntFarmRpcCall.videoDeliverModule(contentId))
                                    if (ResChecker.checkRes(TAG + "视频投递失败:", videoDetailjo)) {
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
                                val count = farmTaskTryCount.computeIfAbsent(bizKey) { k: kotlin.String? -> java.util.concurrent.atomic.AtomicInteger(0) }!!
                                    .incrementAndGet()
                                val taskDetailjo = JSONObject(AntFarmRpcCall.doFarmTask(bizKey))
                                if (count > 1) {
                                    // 超过 1 次视为失败任务
                                    Log.error("庄园任务(超过1次)标记失败：$title\n$taskDetailjo")
                                    badTaskSet.add(bizKey)
                                    put("badFarmTaskSet", badTaskSet)
                                    GlobalThreadPools.sleep(1000)
                                } else {
                                    Log.farm("庄园任务🧾[$title]")
                                    GlobalThreadPools.sleep(1000)
                                }
                            }
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

    /*
    private fun receiveFarmAwards() {
        try {
            var doubleCheck: Boolean
            do {
                doubleCheck = false
                val jo = JSONObject(AntFarmRpcCall.listFarmTask())
                if (ResChecker.checkRes(TAG + "查询庄园任务失败:", jo)) {
                    val farmTaskList = jo.getJSONArray("farmTaskList")
                    val signList = jo.getJSONObject("signList")
                    farmSign(signList)
                    for (i in 0..<farmTaskList.length()) {
                        val task = farmTaskList.getJSONObject(i)
                        val taskStatus = task.getString("taskStatus")
                        val taskTitle = task.optString("title", "未知任务")
                        val awardCount = task.optInt("awardCount", 0)
                        val taskId = task.optString("taskId")
                        if (TaskStatus.FINISHED.name == taskStatus) {
                            if (task.optString("awardType") == "ALLPURPOSE") {
                                if (awardCount + foodStock > foodStockLimit) {
                                    unreceiveTaskAward++
                                    Log.record(TAG, taskTitle + "领取" + awardCount + "g饲料后将超过[" + foodStockLimit + "g]上限!终止领取")
                                    break
                                }
                            }
                            val receiveTaskAwardjo = JSONObject(AntFarmRpcCall.receiveFarmTaskAward(taskId))
                            if (ResChecker.checkRes(TAG + "领取庄园任务奖励失败:", receiveTaskAwardjo)) {
                                add2FoodStock(awardCount)
                                Log.farm("庄园奖励🎖️[" + taskTitle + "]#" + awardCount + "g")
                                doubleCheck = true
                                if (unreceiveTaskAward > 0) unreceiveTaskAward--
                            }
                        }
                        GlobalThreadPools.sleep(1000)
                    }
                }
            } while (doubleCheck)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "receiveFarmAwards 错误:", t)
        }
    }
    */
    private fun receiveFarmAwards() {
        try {
            var doubleCheck: Boolean
            var isFeedFull = false // 添加饲料槽已满的标志
            do {
                doubleCheck = false
                val jo = JSONObject(AntFarmRpcCall.listFarmTask())
                if (ResChecker.checkRes(TAG + "查询庄园任务失败:", jo)) {
                    val farmTaskList = jo.getJSONArray("farmTaskList")
                    val signList = jo.getJSONObject("signList")
                    farmSign(signList)
                    for (i in 0..<farmTaskList.length()) {
                        // 如果饲料槽已满，跳过后续任务的领取
                        if (isFeedFull) {
                            //Log.record(TAG, "饲料槽已满，跳过后续任务奖励领取")
                            break
                        }

                        val task = farmTaskList.getJSONObject(i)
                        val taskStatus = task.getString("taskStatus")
                        val taskTitle = task.optString("title", "未知任务")
                        val awardCount = task.optInt("awardCount", 0)
                        val taskId = task.optString("taskId")
                        if (TaskStatus.FINISHED.name == taskStatus) {
                            if (task.optString("awardType") == "ALLPURPOSE") {
                                if (awardCount + foodStock > foodStockLimit) {
                                    unreceiveTaskAward++
                                    Log.record(TAG, taskTitle + "领取" + awardCount + "g饲料后将超过[" + foodStockLimit + "g]上限!终止领取")
                                    break
                                }
                            }
                            val receiveTaskAwardjo = JSONObject(AntFarmRpcCall.receiveFarmTaskAward(taskId))
                            // 检查领取结果
                            if (ResChecker.checkRes(TAG + "领取庄园任务奖励失败:", receiveTaskAwardjo)) {
                                add2FoodStock(awardCount)
                                Log.farm("庄园奖励[" + taskTitle + "]#" + awardCount + "g")
                                doubleCheck = true
                                if (unreceiveTaskAward > 0) unreceiveTaskAward--
                            } else {
                                // 检查是否是饲料槽已满的错误
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
            for (i in 0..<jaFarmSignList.length()) {
                val jo = jaFarmSignList.getJSONObject(i)
                val signKey = jo.getString("signKey")
                val signed = jo.getBoolean("signed")
                val awardCount = jo.getString("awardCount")
                if (currentSignKey == signKey) {
                    if (!signed) {
                        val signResponse = AntFarmRpcCall.sign()
                        if (ResChecker.checkRes(TAG + "庄园签到失败:", signResponse)) {
                            Log.farm("庄园签到📅获得饲料" + awardCount + "g")
                            Status.setFlagToday(flag)
                        }
                    }
                    return
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
            if (foodStock < 180) {
                Log.record(TAG, "喂鸡饲料不足")
                return false
            } else {
                val jo = JSONObject(AntFarmRpcCall.feedAnimal(farmId))
                if (ResChecker.checkRes(TAG + "喂鸡失败:", jo)) {
                    val remainingFoodStock = jo.optInt("foodStock", foodStock - 180)
                    Log.farm("投喂小鸡🥣[180g]#剩余${remainingFoodStock}g")
                    return true
                } else {
                    Log.record(TAG, "喂鸡失败: ${jo.optString("memo", "未知错误")}")
                    return false
                }
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
        if (!Status.canUseAccelerateTool()) {
            return false
        }
        if (!useAccelerateToolContinue!!.value && AnimalBuff.ACCELERATING.name == ownerAnimal.animalBuff) {
            return false
        }
        syncAnimalStatus(ownerFarmId)
        var consumeSpeed = 0.0
        var allFoodHaveEatten = 0.0
        val nowTime = System.currentTimeMillis() / 1000
        for (animal in animals!!) {
            if (animal.masterFarmId == ownerFarmId) {
                consumeSpeed = animal.consumeSpeed!!
            }
            allFoodHaveEatten += animal.foodHaveEatten!!
            allFoodHaveEatten += animal.consumeSpeed!! * (nowTime - animal.startEatTime!!.toDouble() / 1000)
        }
        // consumeSpeed: g/s
        // AccelerateTool: -1h = -60m = -3600s
        var isUseAccelerateTool = false
        while (180 - allFoodHaveEatten >= consumeSpeed * 3600) {
            if ((useAccelerateToolWhenMaxEmotion!!.value && finalScore != 100.0)) {
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
            if (!useAccelerateToolContinue!!.value) {
                break
            }
        }
        return isUseAccelerateTool
    }

    private fun useFarmTool(targetFarmId: String?, toolType: ToolType): Boolean {
        try {
            var s = AntFarmRpcCall.listFarmTool()
            var jo = JSONObject(s)
            var memo = jo.getString("memo")
            if (ResChecker.checkRes(TAG + "查询道具列表失败:", jo)) {
                val jaToolList = jo.getJSONArray("toolList")
                for (i in 0..<jaToolList.length()) {
                    jo = jaToolList.getJSONObject(i)
                    if (toolType.name == jo.getString("toolType")) {
                        val toolCount = jo.getInt("toolCount")
                        if (toolCount > 0) {
                            var toolId = ""
                            if (jo.has("toolId")) toolId = jo.getString("toolId")
                            s = AntFarmRpcCall.useFarmTool(targetFarmId, toolId, toolType.name)
                            jo = JSONObject(s)
                            memo = jo.getString("memo")
                            if (ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                                Log.farm("使用道具🎭[" + toolType.nickName() + "]#剩余" + (toolCount - 1) + "张")
                                return true
                            } else {
                                Log.record(memo)
                            }
                            Log.runtime(s)
                        }
                        break
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
            val feedFriendAnimalMap = feedFriendAnimalList!!.value
            for (entry in feedFriendAnimalMap.entries) {
                val userId: String = entry.key!!
                if (userId == UserMap.currentUid)  //跳过自己
                    continue
                if (!Status.canFeedFriendToday(userId, entry.value!!)) continue
                val jo = JSONObject(AntFarmRpcCall.enterFarm(userId, userId))
                GlobalThreadPools.sleep(3 * 1000L) //延迟3秒
                if (ResChecker.checkRes(TAG + "进入好友庄园失败:", jo)) {
                    val subFarmVOjo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                    val friendFarmId = subFarmVOjo.getString("farmId")
                    val jaAnimals = subFarmVOjo.getJSONArray("animals")
                    for (j in 0..<jaAnimals.length()) {
                        val animalsjo = jaAnimals.getJSONObject(j)

                        val masterFarmId = animalsjo.getString("masterFarmId")
                        if (masterFarmId == friendFarmId) { //遍历到的鸡 如果在自己的庄园
                            val animalStatusVO = animalsjo.getJSONObject("animalStatusVO")
                            val animalInteractStatus = animalStatusVO.getString("animalInteractStatus") //动物互动状态
                            val animalFeedStatus = animalStatusVO.getString("animalFeedStatus") //动物饲料状态
                            if (AnimalInteractStatus.HOME.name == animalInteractStatus && AnimalFeedStatus.HUNGRY.name == animalFeedStatus) { //状态是饥饿 并且在庄园
                                val user = UserMap.getMaskName(userId) //喂 给我喂
                                if (foodStock < 180) {
                                    if (unreceiveTaskAward > 0) {
                                        Log.record(TAG, "✨还有待领取的饲料")
                                        receiveFarmAwards() //先去领个饲料
                                    }
                                }
                                //第二次检查
                                if (foodStock >= 180) {
                                    if (Status.hasFlagToday("farm::feedFriendLimit")) {
                                        return
                                    }
                                    val feedFriendAnimaljo = JSONObject(AntFarmRpcCall.feedFriendAnimal(friendFarmId))
                                    if (ResChecker.checkRes(TAG + "帮喂好友小鸡失败:", feedFriendAnimaljo)) {
                                        foodStock = feedFriendAnimaljo.getInt("foodStock")
                                        Log.farm("帮喂好友🥣[" + user + "]的小鸡[180g]#剩余" + foodStock + "g")
                                        Status.feedFriendToday(AntFarmRpcCall.farmId2UserId(friendFarmId))
                                    } else {
                                        Log.error(TAG, "😞喂[$user]的鸡失败$feedFriendAnimaljo")
                                        Status.setFlagToday("farm::feedFriendLimit")
                                        break
                                    }
                                } else {
                                    Log.record(TAG, "😞喂鸡[$user]饲料不足")
                                }
                            }
                            break
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
            var hasNext = false
            var pageStartSum = 0
            var s: String?
            var jo: JSONObject
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum)
                jo = JSONObject(s)
                var memo = jo.getString("memo")
                if (ResChecker.checkRes(TAG + "查询排行榜失败:", jo)) {
                    hasNext = jo.getBoolean("hasNext")
                    val jaRankingList = jo.getJSONArray("rankingList")
                    pageStartSum += jaRankingList.length()
                    for (i in 0..<jaRankingList.length()) {
                        jo = jaRankingList.getJSONObject(i)
                        val userId = jo.getString("userId")
                        val userName = UserMap.getMaskName(userId)
                        var isNotifyFriend = notifyFriendList!!.value.contains(userId)
                        if (notifyFriendType!!.value == NotifyFriendType.Companion.DONT_NOTIFY) {
                            isNotifyFriend = !isNotifyFriend
                        }
                        if (!isNotifyFriend || userId == UserMap.currentUid) {
                            continue
                        }
                        val starve = jo.has("actionType") && "starve_action" == jo.getString("actionType")
                        if (jo.getBoolean("stealingAnimal") && !starve) {
                            s = AntFarmRpcCall.enterFarm(userId, userId)
                            jo = JSONObject(s)
                            memo = jo.getString("memo")
                            if (ResChecker.checkRes(TAG + "进入好友庄园失败:", jo)) {
                                jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                                val friendFarmId = jo.getString("farmId")
                                val jaAnimals = jo.getJSONArray("animals")
                                var notified = (0 == notifyFriend!!.value)
                                for (j in 0..<jaAnimals.length()) {
                                    jo = jaAnimals.getJSONObject(j)
                                    val animalId = jo.getString("animalId")
                                    val masterFarmId = jo.getString("masterFarmId")
                                    if (masterFarmId != friendFarmId && masterFarmId != ownerFarmId) {
                                        if (notified) continue
                                        jo = jo.getJSONObject("animalStatusVO")
                                        notified = notifyFriend(jo, friendFarmId, animalId, userName)
                                    }
                                }
                            } else {
                                Log.record(memo)
                                Log.runtime(s)
                            }
                        }
                    }
                } else {
                    Log.record(memo)
                    Log.runtime(s)
                }
            } while (hasNext)
            Log.record(TAG, "饲料剩余[" + foodStock + "g]")
        } catch (t: Throwable) {
            Log.runtime(TAG, "notifyFriend err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun notifyFriend(joAnimalStatusVO: JSONObject, friendFarmId: String?, animalId: String, user: String?): Boolean {
        try {
            if (AnimalInteractStatus.STEALING.name == joAnimalStatusVO.getString("animalInteractStatus") && AnimalFeedStatus.EATING.name == joAnimalStatusVO.getString("animalFeedStatus")) {
                val jo = JSONObject(AntFarmRpcCall.notifyFriend(animalId, friendFarmId))
                if (ResChecker.checkRes(TAG + "通知好友失败:", jo)) {
                    val rewardCount = jo.getDouble("rewardCount")
                    if (jo.getBoolean("refreshFoodStock")) foodStock = jo.getDouble("finalFoodStock").toInt()
                    else add2FoodStock(rewardCount.toInt())
                    Log.farm("通知好友📧[" + user + "]被偷吃#奖励" + rewardCount + "g")
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
            if (!jo.has("subFarmVO")) return

            // 小鸡心情
            if (jo.has("emotionInfo")) {
                finalScore = jo.getJSONObject("emotionInfo").getDouble("finalScore")
            }

            val subFarmVO = jo.getJSONObject("subFarmVO")

            // 食物库存
            if (subFarmVO.has("foodStock")) {
                foodStock = subFarmVO.getInt("foodStock")
            }

            // 粪肥
            if (subFarmVO.has("manureVO")) {
                val manurePotList = subFarmVO.getJSONObject("manureVO").getJSONArray("manurePotList")
                for (i in 0 until manurePotList.length()) {
                    val manurePot = manurePotList.getJSONObject(i)
                    if (manurePot.getInt("manurePotNum") >= 100) {
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

            // 农场ID
            ownerFarmId = subFarmVO.getString("farmId")

            // 农场产物
            val farmProduce = subFarmVO.getJSONObject("farmProduce")
            benevolenceScore = farmProduce.getDouble("benevolenceScore")

            // 奖励列表
            rewardList = if (subFarmVO.has("rewardList")) {
                val jaRewardList = subFarmVO.getJSONArray("rewardList")
                Array(jaRewardList.length()) { i ->
                    val joReward = jaRewardList.getJSONObject(i)
                    RewardFriend().apply {
                        consistencyKey = joReward.getString("consistencyKey")
                        friendId = joReward.getString("friendId")
                        time = joReward.getString("time")
                    }
                }
            } else {
                emptyArray()
            }

            // 小鸡列表
            val jaAnimals = subFarmVO.getJSONArray("animals")
            Log.record(TAG, "🐔 解析小鸡列表，共${jaAnimals.length()}只小鸡")
            animals = Array(jaAnimals.length()) { i ->
                val animalJson = jaAnimals.getJSONObject(i)
               // Log.record(TAG, "🐔 小鸡${i+1} 原始JSON: $animalJson")
                val animal: Animal = objectMapper.readValue(animalJson.toString(), Animal::class.java)

                // 手动从animalStatusVO中获取状态信息
                if (animalJson.has("animalStatusVO")) {
                    val animalStatusVO = animalJson.getJSONObject("animalStatusVO")
                    animal.animalInteractStatus = animalStatusVO.getString("animalInteractStatus")
                    animal.animalFeedStatus = animalStatusVO.getString("animalFeedStatus")
                } else {
                    Log.record(TAG, "🐔 小鸡${i+1} 没有animalStatusVO对象")
                }
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
        foodStock += i
        if (foodStock > foodStockLimit) {
            foodStock = foodStockLimit
        }
        if (foodStock < 0) {
            foodStock = 0
        }
    }


    /**
     * 收集每日食材
     */
    private fun collectDailyFoodMaterial() {
        try {
            val userId = UserMap.currentUid
            var jo = JSONObject(AntFarmRpcCall.enterKitchen(userId))
            if (ResChecker.checkRes(TAG + "进入小鸡厨房失败:", jo)) {
                val canCollectDailyFoodMaterial = jo.getBoolean("canCollectDailyFoodMaterial")
                val dailyFoodMaterialAmount = jo.getInt("dailyFoodMaterialAmount")
                val garbageAmount = jo.optInt("garbageAmount", 0)
                if (jo.has("orchardFoodMaterialStatus")) {
                    val orchardFoodMaterialStatus = jo.getJSONObject("orchardFoodMaterialStatus")
                    if ("FINISHED" == orchardFoodMaterialStatus.optString("foodStatus")) {
                        jo = JSONObject(AntFarmRpcCall.farmFoodMaterialCollect())
                        if (ResChecker.checkRes(TAG + "领取农场食材失败:", jo)) {
                            Log.farm("小鸡厨房👨🏻‍🍳[领取农场食材]#" + jo.getInt("foodMaterialAddCount") + "g")
                        }
                    }
                }
                if (canCollectDailyFoodMaterial) {
                    jo = JSONObject(AntFarmRpcCall.collectDailyFoodMaterial(dailyFoodMaterialAmount))
                    if (ResChecker.checkRes(TAG + "领取今日食材失败:", jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取今日食材]#" + dailyFoodMaterialAmount + "g")
                    }
                }
                if (garbageAmount > 0) {
                    jo = JSONObject(AntFarmRpcCall.collectKitchenGarbage())
                    if (ResChecker.checkRes(TAG + "领取肥料失败:", jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取肥料]#" + jo.getInt("recievedKitchenGarbageAmount") + "g")
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
                val canCollectDailyLimitedFoodMaterial = jo.getBoolean("canCollectDailyLimitedFoodMaterial")
                if (canCollectDailyLimitedFoodMaterial) {
                    val dailyLimitedFoodMaterialAmount = jo.getInt("dailyLimitedFoodMaterialAmount")
                    jo = JSONObject(AntFarmRpcCall.collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount))
                    if (ResChecker.checkRes(TAG + "领取爱心食材店食材失败:", jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取爱心食材店食材]#" + dailyLimitedFoodMaterialAmount + "g")
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
                val cookTimesAllowed = jo.getInt("cookTimesAllowed")
                if (cookTimesAllowed > 0) {
                    for (i in 0..<cookTimesAllowed) {
                        jo = JSONObject(AntFarmRpcCall.cook(userId, "VILLA"))
                        if (ResChecker.checkRes(TAG + "制作美食失败:", jo)) {
                            val cuisineVO = jo.getJSONObject("cuisineVO")
                            Log.farm("小鸡厨房👨🏻‍🍳[" + cuisineVO.getString("name") + "]制作成功")
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
            var jo: JSONObject
            var cookbookId: String?
            var cuisineId: String?
            var name: String?
            for (i in 0..<cuisineList.length()) {
                jo = cuisineList.getJSONObject(i)
                if (jo.getInt("count") <= 0) continue
                cookbookId = jo.getString("cookbookId")
                cuisineId = jo.getString("cuisineId")
                name = jo.getString("name")
                jo = JSONObject(AntFarmRpcCall.useFarmFood(cookbookId, cuisineId))
                if (ResChecker.checkRes(TAG + "使用特殊食品失败:", jo)) {
                    val deltaProduce = jo.getJSONObject("foodEffect").getDouble("deltaProduce")
                    Log.farm("使用美食🍱[" + name + "]#加速" + deltaProduce + "颗爱心鸡蛋")
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "useFarmFood err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun drawLotteryPlus(lotteryPlusInfo: JSONObject) {
        try {
            if (!lotteryPlusInfo.has("userSevenDaysGiftsItem")) return
            val itemId = lotteryPlusInfo.getString("itemId")
            var userSevenDaysGiftsItem = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem")
            val userEverydayGiftItems = userSevenDaysGiftsItem.getJSONArray("userEverydayGiftItems")
            for (i in 0..<userEverydayGiftItems.length()) {
                userSevenDaysGiftsItem = userEverydayGiftItems.getJSONObject(i)
                if (userSevenDaysGiftsItem.getString("itemId") == itemId) {
                    if (!userSevenDaysGiftsItem.getBoolean("received")) {
                        val singleDesc = userSevenDaysGiftsItem.getString("singleDesc")
                        val awardCount = userSevenDaysGiftsItem.getInt("awardCount")
                        if (singleDesc.contains("饲料") && awardCount + foodStock > foodStockLimit) {
                            Log.record(TAG, "暂停领取[$awardCount]g饲料，上限为[$foodStockLimit]g")
                            break
                        }
                        userSevenDaysGiftsItem = JSONObject(AntFarmRpcCall.drawLotteryPlus())
                        if ("SUCCESS" == userSevenDaysGiftsItem.getString("memo")) {
                            Log.farm("惊喜礼包🎁[$singleDesc*$awardCount]")
                        }
                    }
                    break
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
            val map = visitFriendList!!.value
            if (map == null || map.isEmpty()) return
            val currentUid = UserMap.currentUid
            for (entry in map.entries) {
                val userId: String = entry.key!!
                val count: Int = entry.value!!
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
                for (i in 0..<count) {
                    if (foodStock < 10) break
                    jo = JSONObject(AntFarmRpcCall.visitFriend(farmId))
                    if (ResChecker.checkRes(TAG + "赠送麦子失败:", jo)) {
                        foodStock = jo.getInt("foodStock")
                        Log.farm("赠送麦子🌾[" + UserMap.getMaskName(userId) + "]#" + jo.getInt("giveFoodNum") + "g")
                        visitedTimes++
                        if (jo.optBoolean("isReachLimit")) {
                            Log.record(TAG, "今日给[" + UserMap.getMaskName(userId) + "]送麦子已达上限")
                            visitedTimes = 3
                            break
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
                Log.farm("收取麦子🌾[" + receiveFoodNum + "g]")
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
        val diaryDateStr: String?
        try {
            var jo = JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr))
            if (ResChecker.checkRes(TAG + "查询小鸡日记失败:", jo)) {
                val data = jo.getJSONObject("data")
                val chickenDiary = data.getJSONObject("chickenDiary")
                diaryDateStr = chickenDiary.getString("diaryDateStr")
                if (data.has("hasTietie")) {
                    if (!data.optBoolean("hasTietie", true)) {
                        jo = JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, "NEW"))
                        if (ResChecker.checkRes(TAG + "贴贴小鸡失败:", jo)) {
                            val prizeType = jo.getString("prizeType")
                            val prizeNum = jo.optInt("prizeNum", 0)
                            Log.farm("[$diaryDateStr]贴贴小鸡💞[$prizeType*$prizeNum]")
                        } else {
                            Log.runtime(TAG, "贴贴小鸡失败:")
                            Log.runtime(jo.getString("memo"), jo.toString())
                        }
                        if (!chickenDiary.has("statisticsList")) return
                        val statisticsList = chickenDiary.getJSONArray("statisticsList")
                        if (statisticsList.length() > 0) {
                            for (i in 0..<statisticsList.length()) {
                                val tietieStatus = statisticsList.getJSONObject(i)
                                val tietieRoleId = tietieStatus.getString("tietieRoleId")
                                jo = JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, tietieRoleId))
                                if (ResChecker.checkRes(TAG + "贴贴小鸡失败:", jo)) {
                                    val prizeType = jo.getString("prizeType")
                                    val prizeNum = jo.optInt("prizeNum", 0)
                                    Log.farm("[$diaryDateStr]贴贴小鸡💞[$prizeType*$prizeNum]")
                                } else {
                                    Log.runtime(TAG, "贴贴小鸡失败:")
                                    Log.runtime(jo.getString("memo"), jo.toString())
                                }
                            }
                        }
                    }
                }
            } else {
                Log.runtime(TAG, "贴贴小鸡-获取小鸡日记详情 err:")
                Log.runtime(jo.getString("resultDesc"), jo.toString())
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
                    if (jo.optBoolean("success", true)) {
                        Log.farm("[$diaryDateStr]点赞小鸡日记💞成功")
                    }
                }
            } else {
                Log.runtime(TAG, "日记点赞-获取小鸡日记详情 err:")
                Log.runtime(jo.getString("resultDesc"), jo.toString())
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryChickenDiary err:")
            Log.printStackTrace(TAG, t)
        }
        return diaryDateStr
    }

    private fun queryChickenDiaryList(
        queryMonthStr: String?,
        action: (String) -> String // ✅ 使用 Kotlin 函数类型
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
                val chickenDiaryBriefList = data.optJSONArray("chickenDiaryBriefList")

                if (chickenDiaryBriefList != null && chickenDiaryBriefList.length() > 0) {
                    for (i in chickenDiaryBriefList.length() - 1 downTo 0) {
                        val item = chickenDiaryBriefList.getJSONObject(i)
                        if (!item.optBoolean("read", true) || !item.optBoolean("collectStatus")) {
                            val dateStr = item.getString("dateStr")
                            action(dateStr) // ✅ 直接调用
                            GlobalThreadPools.sleep(300)
                        }
                    }
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString())
            }
        } catch (t: Throwable) {
            hasPreviousMore = false
            Log.runtime(TAG, "queryChickenDiaryList err:")
            Log.printStackTrace(TAG, t)
        }
        return hasPreviousMore
    }


    private fun doChickenDiary() {
        if (diaryTietie!!.value) { // 贴贴小鸡
            diaryTietie("")
        }

        // 小鸡日记点赞
        var dateStr: String? = null
        var yearMonth = YearMonth.now()
        var previous = false
        try {
            if (collectChickenDiary!!.value >= CollectChickenDiaryType.Companion.ONCE) {
                GlobalThreadPools.sleep(300)
                dateStr = collectChickenDiary("")
            }
            if (collectChickenDiary!!.value >= CollectChickenDiaryType.Companion.MONTH) {
                if (dateStr == null) {
                    Log.error(TAG, "小鸡日记点赞-dateStr为空，使用当前日期")
                } else {
                    yearMonth = YearMonth.from(LocalDate.parse(dateStr))
                }
                GlobalThreadPools.sleep(300)
                previous = queryChickenDiaryList(yearMonth.toString()) { dateStr ->
                    collectChickenDiary(dateStr).toString()
                }
            }
            if (collectChickenDiary!!.value >= CollectChickenDiaryType.Companion.ALL) {
                while (previous) {
                    GlobalThreadPools.sleep(300)
                    yearMonth = yearMonth.minusMonths(1)
                    previous = queryChickenDiaryList(yearMonth.toString()) { dateStr ->
                        collectChickenDiary(dateStr).toString()
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
                if (!jo.has("talkConfigs")) return
                val talkConfigs = jo.getJSONArray("talkConfigs")
                val talkNodes = jo.getJSONArray("talkNodes")
                val data = talkConfigs.getJSONObject(0)
                val farmId = data.getString("farmId")
                jo = JSONObject(AntFarmRpcCall.feedFriendAnimalVisit(farmId))
                if (ResChecker.checkRes(TAG + "喂食小鸡到访失败:", jo)) {
                    for (i in 0..<talkNodes.length()) {
                        jo = talkNodes.getJSONObject(i)
                        if ("FEED" != jo.getString("type")) continue
                        val consistencyKey = jo.getString("consistencyKey")
                        jo = JSONObject(AntFarmRpcCall.visitAnimalSendPrize(consistencyKey))
                        if (ResChecker.checkRes(TAG + "发送小鸡到访奖励失败:", jo)) {
                            val prizeName = jo.getString("prizeName")
                            Log.farm("小鸡到访💞[$prizeName]")
                        } else {
                            Log.runtime(jo.getString("memo"), jo.toString())
                        }
                    }
                } else {
                    Log.runtime(jo.getString("memo"), jo.toString())
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString())
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "visitAnimal err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /* 雇佣好友小鸡 */
    private fun hireAnimal() {
        var animals: JSONArray? = null
        try {
            val jsonObject = enterFarm()
            if (jsonObject == null) {
                return
            }
            if ("SUCCESS" == jsonObject.getString("memo")) {
                val farmVO = jsonObject.getJSONObject("farmVO")
                val subFarmVO = farmVO.getJSONObject("subFarmVO")
                animals = subFarmVO.getJSONArray("animals")
            } else {
                Log.record(jsonObject.getString("memo"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "getAnimalCount err:")
            Log.printStackTrace(TAG, t)
            return
        }
        if (animals == null) {
            return
        }
        try {
            var i = 0
            val len = animals.length()
            while (i < len) {
                val joo = animals.getJSONObject(i)
                if (joo.getString("subAnimalType") == "WORK") {
                    val taskId = "HIRE|" + joo.getString("animalId")
                    val beHiredEndTime = joo.getLong("beHiredEndTime")
                    if (!hasChildTask(taskId)) {
                        addChildTask(ChildModelTask(taskId, "HIRE", Runnable {
                            //if (hireAnimal!!.value) {
                            //    hireAnimal()
                            //}
                            hireAnimal()
                        }, beHiredEndTime))
                        Log.record(TAG, "添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行")
                    } else {
                        addChildTask(ChildModelTask(taskId, "HIRE", Runnable {
                            //if (hireAnimal!!.value) {
                            //    hireAnimal()
                            //}
                            hireAnimal()
                        }, beHiredEndTime))
                    }
                }
                i++
            }
            var animalCount = animals.length()
            if (animalCount >= 3) {
                return
            }
            Log.farm("雇佣小鸡👷[当前可雇佣小鸡数量:" + (3 - animalCount) + "只]")
            if (foodStock < 50) {
                Log.record(TAG, "饲料不足，暂不雇佣")
                return
            }
            val hireAnimalSet = hireAnimalList!!.value
            var hasNext: Boolean
            var pageStartSum = 0
            var s: String?
            var jo: JSONObject?
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum)
                jo = JSONObject(s)
                val memo = jo.getString("memo")
                if (ResChecker.checkRes(TAG + "查询排行榜失败:", jo)) {
                    hasNext = jo.getBoolean("hasNext")
                    val jaRankingList = jo.getJSONArray("rankingList")
                    pageStartSum += jaRankingList.length()
                    for (i in 0..<jaRankingList.length()) {
                        val joo = jaRankingList.getJSONObject(i)
                        val userId = joo.getString("userId")
                        var isHireAnimal = hireAnimalSet.contains(userId)
                        if (hireAnimalType!!.value == HireAnimalType.Companion.DONT_HIRE) {
                            isHireAnimal = !isHireAnimal
                        }
                        if (!isHireAnimal || userId == UserMap.currentUid) {
                            continue
                        }
                        val actionTypeListStr = joo.getJSONArray("actionTypeList").toString()
                        if (actionTypeListStr.contains("can_hire_action")) {
                            if (hireAnimalAction(userId)) {
                                animalCount++
                                break
                            }
                        }
                    }
                } else {
                    Log.record(memo)
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
                val farmVO = jo.getJSONObject("farmVO")
                val subFarmVO = farmVO.getJSONObject("subFarmVO")
                val farmId = subFarmVO.getString("farmId")
                val animals = subFarmVO.getJSONArray("animals")
                var i = 0
                val len = animals.length()
                while (i < len) {
                    val animal = animals.getJSONObject(i)
                    if (animal.getJSONObject("masterUserInfoVO").getString("userId") == userId) {
                        val animalId = animal.getString("animalId")
                        jo = JSONObject(AntFarmRpcCall.hireAnimal(farmId, animalId))
                        if (ResChecker.checkRes(TAG + "雇佣小鸡失败:", jo)) {
                            Log.farm("雇佣小鸡👷[" + UserMap.getMaskName(userId) + "] 成功")
                            val newAnimals = jo.getJSONArray("animals")
                            var ii = 0
                            val newLen = newAnimals.length()
                            while (ii < newLen) {
                                val joo = newAnimals.getJSONObject(ii)
                                if (joo.getString("animalId") == animalId) {
                                    val beHiredEndTime = joo.getLong("beHiredEndTime")
                                    addChildTask(ChildModelTask("HIRE|$animalId", "HIRE", Runnable {
                                        hireAnimal()
                                    }, beHiredEndTime))
                                    Log.record(TAG, "添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行")
                                    break
                                }
                                ii++
                            }
                            return true
                        } else {
                            Log.record(jo.getString("memo"))
                            Log.runtime(s)
                        }
                        return false
                    }
                    i++
                }
            } else {
                Log.record(jo.getString("memo"))
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
                            val awards = ArrayList<String?>()
                            for (i in 0..<gameCenterDrawAwardList.length()) {
                                val gameCenterDrawAward = gameCenterDrawAwardList.getJSONObject(i)
                                val awardCount = gameCenterDrawAward.getInt("awardCount")
                                val awardName = gameCenterDrawAward.getString("awardName")
                                awards.add("$awardName*$awardCount")
                            }
                            Log.farm("庄园小鸡🎁[开宝箱:获得" + StringUtil.collectionJoinString(",", awards) + "]")
                        } else {
                            Log.runtime(TAG, "drawGameCenterAward falsed result: $jo")
                        }
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, t)
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
                // 检查是否有 achievementOrnaments 数组
                if (!jolistOrnaments.has("achievementOrnaments")) {
                    return  // 数组为空，直接返回
                }
                val achievementOrnaments = jolistOrnaments.getJSONArray("achievementOrnaments")
                val random = Random()
                val possibleOrnaments: MutableList<String> = ArrayList() // 收集所有可保存的套装组合
                for (i in 0..<achievementOrnaments.length()) {
                    val ornament = achievementOrnaments.getJSONObject(i)
                    if (ornament.getBoolean("acquired")) {
                        val sets = ornament.getJSONArray("sets")
                        val availableSets: MutableList<JSONObject> = ArrayList()
                        // 收集所有带有 cap 和 coat 的套装组合
                        for (j in 0..<sets.length()) {
                            val set = sets.getJSONObject(j)
                            if ("cap" == set.getString("subType") || "coat" == set.getString("subType")) {
                                availableSets.add(set)
                            }
                        }
                        // 如果有可用的帽子和外套套装组合
                        if (availableSets.size >= 2) {
                            // 将所有可保存的套装组合添加到 possibleOrnaments 列表中
                            for (j in 0..<availableSets.size - 1) {
                                val selectedCoat = availableSets[j]
                                val selectedCap = availableSets[j + 1]
                                val id1 = selectedCoat.getString("id") // 外套 ID
                                val id2 = selectedCap.getString("id") // 帽子 ID
                                val ornaments = "$id1,$id2"
                                possibleOrnaments.add(ornaments)
                            }
                        }
                    }
                }
                // 如果有可保存的套装组合，则随机选择一个进行保存
                if (!possibleOrnaments.isEmpty()) {
                    val ornamentsToSave = possibleOrnaments[random.nextInt(possibleOrnaments.size)]
                    val saveResult = AntFarmRpcCall.saveOrnaments(animalId, farmId, ornamentsToSave)
                    val saveResultJson = JSONObject(saveResult)
                    // 判断保存是否成功并输出日志
                    if (saveResultJson.optBoolean("success")) {
                        // 获取保存的整套服装名称
                        val ornamentIds: Array<String?> = ornamentsToSave.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        var wholeSetName = "" // 整套服装名称
                        // 遍历 achievementOrnaments 查找对应的套装名称
                        for (i in 0..<achievementOrnaments.length()) {
                            val ornament = achievementOrnaments.getJSONObject(i)
                            val sets = ornament.getJSONArray("sets")
                            // 找到对应的整套服装名称
                            if (sets.length() == 2 && sets.getJSONObject(0).getString("id") == ornamentIds[0]
                                && sets.getJSONObject(1).getString("id") == ornamentIds[1]
                            ) {
                                wholeSetName = ornament.getString("name")
                                break
                            }
                        }
                        // 输出日志
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
            var jo = JSONObject(AntFarmRpcCall.letsGetChickenFeedTogether())
            if (ResChecker.checkRes(TAG + "查询一起拿饲料失败:", jo)) {
                val bizTraceId = jo.getString("bizTraceId")
                val p2pCanInvitePersonDetailList = jo.getJSONArray("p2pCanInvitePersonDetailList")
                var canInviteCount = 0
                var hasInvitedCount = 0
                val userIdList: MutableList<String?> = ArrayList<String?>() // 保存 userId
                for (i in 0..<p2pCanInvitePersonDetailList.length()) {
                    val personDetail = p2pCanInvitePersonDetailList.getJSONObject(i)
                    val inviteStatus = personDetail.getString("inviteStatus")
                    val userId = personDetail.getString("userId")
                    if (inviteStatus == "CAN_INVITE") {
                        userIdList.add(userId)
                        canInviteCount++
                    } else if (inviteStatus == "HAS_INVITED") {
                        hasInvitedCount++
                    }
                }
                val invitedToday = hasInvitedCount
                val remainingInvites = 5 - invitedToday
                var invitesToSend = min(canInviteCount, remainingInvites)
                if (invitesToSend == 0) {
                    return
                }
                val getFeedSet = getFeedlList!!.value
                if (getFeedType!!.value == GetFeedType.Companion.GIVE) {
                    for (userId in userIdList) {
                        if (invitesToSend <= 0) {
//                            Log.record(TAG,"已达到最大邀请次数限制，停止发送邀请。");
                            break
                        }
                        if (getFeedSet.contains(userId)) {
                            jo = JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId))
                            if (jo.optBoolean("success")) {
                                Log.farm("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]")
                                invitesToSend-- // 每成功发送一次邀请，减少一次邀请次数
                            }
                        }
                    }
                } else {
                    val random = Random()
                    for (j in 0..<invitesToSend) {
                        val randomIndex = random.nextInt(userIdList.size)
                        val userId = userIdList[randomIndex]
                        jo = JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId))
                        if (jo.optBoolean("success")) {
                            Log.farm("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]")
                        }
                        userIdList.removeAt(randomIndex)
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
            val nickNames: Array<String?> = arrayOf<String?>("随机一次", "随机多次")
        }
    }

    interface RecallAnimalType {
        companion object {
            const val ALWAYS: Int = 0
            const val WHEN_THIEF: Int = 1
            const val WHEN_HUNGRY: Int = 2
            const val NEVER: Int = 3
            val nickNames: Array<String?> = arrayOf<String?>("始终召回", "偷吃召回", "饥饿召回", "暂不召回")
        }
    }

    interface SendBackAnimalWay {
        companion object {
            const val HIT: Int = 0
            const val NORMAL: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("攻击", "常规")
        }
    }

    interface SendBackAnimalType {
        companion object {
            const val BACK: Int = 0
            const val NOT_BACK: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中遣返", "选中不遣返")
        }
    }

    interface CollectChickenDiaryType {
        companion object {
            const val CLOSE: Int = 0
            const val ONCE: Int = 0
            const val MONTH: Int = 1
            const val ALL: Int = 2
            val nickNames: Array<String?> = arrayOf<String?>("不开启", "一次", "当月", "所有")
        }
    }

    enum class AnimalBuff {
        //小鸡buff
        ACCELERATING, INJURED, NONE
    }

    enum class AnimalFeedStatus {
        // HUNGRY, EATING, SLEEPY
        HUNGRY, EATING, SLEEPY, NONE
    }

    enum class AnimalInteractStatus {
        //小鸡关互动状态
        HOME, GOTOSTEAL, STEALING
    }

    enum class SubAnimalType {
        NORMAL, GUEST, PIRATE, WORK
    }

    enum class ToolType {
        STEALTOOL, ACCELERATETOOL, SHARETOOL, FENCETOOL, NEWEGGTOOL, DOLLTOOL, ORDINARY_ORNAMENT_TOOL, ADVANCE_ORNAMENT_TOOL, BIG_EATER_TOOL, RARE_ORNAMENT_TOOL;

        fun nickName(): CharSequence? {
            return nickNames[ordinal]
        }

        companion object {
            val nickNames: Array<CharSequence?> =
                arrayOf<CharSequence?>("蹭饭卡", "加速卡", "救济卡", "篱笆卡", "新蛋卡", "公仔补签卡", "普通装扮补签卡", "高级装扮补签卡", "加饭卡", "稀有装扮补签卡")
        }
    }

    /*
    enum class GameType {
        starGame, jumpGame, flyGame, hitGame;

        companion object {
            val gameNames = arrayOf("星星球", "登山赛", "飞行赛", "欢乐揍小鸡")
        }

        fun gameName() = gameNames[ordinal]
    }
    */
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
            val nickNames: Array<String?> = arrayOf<String?>("选中雇佣", "选中不雇佣")
        }
    }

    @Suppress("unused")
    interface GetFeedType {
        companion object {
            const val GIVE: Int = 0
            const val RANDOM: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中赠送", "随机赠送")
        }
    }

    interface NotifyFriendType {
        companion object {
            const val NOTIFY: Int = 0
            const val DONT_NOTIFY: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中通知", "选中不通知")
        }
    }

    enum class PropStatus {
        REACH_USER_HOLD_LIMIT, NO_ENOUGH_POINT, REACH_LIMIT;

        fun nickName(): CharSequence? {
            return nickNames[ordinal]
        }

        companion object {
            val nickNames: Array<CharSequence?> = arrayOf<CharSequence?>("达到用户持有上限", "乐园币不足", "兑换达到上限")
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
                Log.farm("领取活动食物成功," + "已领取" + resultJson.optInt("foodCount"))
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "clickForGiftV2 err:")
            Log.printStackTrace(TAG, e)
        }
    }

    companion object {
        private val TAG: String = AntFarm::class.java.getSimpleName()
        private val objectMapper = ObjectMapper()

        init {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private const val FARM_ANSWER_CACHE_KEY = "farmQuestionCache"
        private const val ANSWERED_FLAG = "farmQuestion::answered" // 今日是否已答题
        private const val CACHED_FLAG = "farmQuestion::cache" // 是否已缓存明日答案
    }
}
