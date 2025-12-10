package fansirsqi.xposed.sesame.task.antMember;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntMemberRpcCall {
    private static String getUniqueId() {
        return String.valueOf(System.currentTimeMillis()) + RandomUtil.nextLong();
    }

    /* ant member point */
    public static String queryPointCert(int page, int pageSize) {
        String args1 = "[{\"page\":" + page + ",\"pageSize\":" + pageSize + "}]";
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.queryPointCert", args1);
    }

    public static String receivePointByUser(String certId) {
        String args1 = "[{\"certId\":" + certId + "}]";
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.receivePointByUser", args1);
    }

    public static String receiveAllPointByUser() throws JSONException {
//        [{"bizSource":"myTab","sourcePassMap":{"innerSource":"","passInfo":"{\"tc\":\"EXPIRING_POINT\"}","source":"myTab","unid":""}}]
        JSONObject args = new JSONObject();
        args.put("bizSource", "myTab");
        JSONObject passMap = new JSONObject();
        passMap.put("innerSource", "");
        JSONObject passInfo = new JSONObject();
        passInfo.put("tc", "EXPIRING_POINT");
        passMap.put("passInfo", passInfo);
        passMap.put("source", "myTab");
        passMap.put("unid", "");
        args.put("sourcePassMap", passMap);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.pointcert.h5.receiveAllPointByUser", params);
    }

    public static String queryMemberSigninCalendar() {
        return RequestManager.requestString("com.alipay.amic.biz.rpc.signin.h5.queryMemberSigninCalendar",
                "[{\"autoSignIn\":true,\"invitorUserId\":\"\",\"sceneCode\":\"QUERY\"}]");
    }

    /* 商家开门打卡任务 */
    public static String signIn(String activityNo) {
        return RequestManager.requestString("alipay.merchant.kmdk.signIn",
                "[{\"activityNo\":\"" + activityNo + "\"}]");
    }

    public static String signUp(String activityNo) {
        return RequestManager.requestString("alipay.merchant.kmdk.signUp",
                "[{\"activityNo\":\"" + activityNo + "\"}]");
    }

    /* 商家服务 */
    public static String transcodeCheck() {
        return RequestManager.requestString("alipay.mrchservbase.mrchbusiness.sign.transcode.check",
                "[{}]");
    }

    public static String merchantSign() {
        return RequestManager.requestString("alipay.mrchservbase.mrchpoint.sqyj.homepage.signin.v1",
                "[{}]");
    }

    public static String zcjSignInQuery() {
        return RequestManager.requestString("alipay.mrchservbase.zcj.view.invoke",
                "[{\"compId\":\"ZCJ_SIGN_IN_QUERY\"}]");
    }

    public static String zcjSignInExecute() {
        return RequestManager.requestString("alipay.mrchservbase.zcj.view.invoke",
                "[{\"compId\":\"ZCJ_SIGN_IN_EXECUTE\"}]");
    }

    public static String taskListQuery() {
        return RequestManager.requestString("alipay.mrchservbase.task.more.query",
                "[{\"paramMap\":{\"platform\":\"Android\"},\"taskItemCode\":\"\"}]");
    }

    public static String queryActivity() {
        return RequestManager.requestString("alipay.merchant.kmdk.query.activity",
                "[{\"scene\":\"activityCenter\"}]");
    }

    /* 商家服务任务 */
    public static String taskFinish(String bizId) {
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.task.finish",
                "[{\"bizId\":\"" + bizId + "\"}]");
    }

    public static String taskReceive(String taskCode) {
        return RequestManager.requestString("alipay.mrchservbase.sqyj.task.receive",
                "[{\"compId\":\"ZTS_TASK_RECEIVE\",\"extInfo\":{\"taskCode\":\"" + taskCode + "\"}}]");
    }

    public static String actioncode(String actionCode) {
        return RequestManager.requestString("alipay.mrchservbase.task.query.by.actioncode",
                "[{\"actionCode\":\"" + actionCode + "\"}]");
    }

    public static String produce(String actionCode) {
        return RequestManager.requestString("alipay.mrchservbase.biz.task.action.produce",
                "[{\"actionCode\":\"" + actionCode + "\"}]");
    }

    public static String ballReceive(String ballIds) {
        return RequestManager.requestString("alipay.mrchservbase.mrchpoint.ball.receive",
                "[{\"ballIds\":[\"" + ballIds
                        + "\"],\"channel\":\"MRCH_SELF\",\"outBizNo\":\"" + getUniqueId() + "\"}]");
    }

    /* 会员任务 */
    public static String signPageTaskList() {
        return RequestManager.requestString("alipay.antmember.biz.rpc.membertask.h5.signPageTaskList",
                "[{\"sourceBusiness\":\"antmember\",\"spaceCode\":\"ant_member_xlight_task\"}]");
    }

    public static String applyTask(String darwinName, Long taskConfigId) {
        return RequestManager.requestString("alipay.antmember.biz.rpc.membertask.h5.applyTask",
                "[{\"darwinExpParams\":{\"darwinName\":\"" + darwinName
                        + "\"},\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"},\"taskConfigId\":"
                        + taskConfigId + "}]");
    }

    public static String executeTask(String bizParam, String bizSubType, String bizType, Long taskConfigId) {
        return RequestManager.requestString("alipay.antmember.biz.rpc.membertask.h5.executeTask",
                "[{\"bizOutNo\":\"" + TimeUtil.getFormatDate().replaceAll("-", "") +
                        "\",\"bizParam\":\"" + bizParam + "\",\"bizSubType\":\"" + bizSubType + "\",\"bizType\":\"" + bizType +
                        "\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"}" +
                        ",\"syncProcess\":true,\"taskConfigId\":\"" + taskConfigId + "\"}]");
    }

    public static String queryAllStatusTaskList() {
        return RequestManager.requestString("alipay.antmember.biz.rpc.membertask.h5.queryAllStatusTaskList",
                "[{\"sourceBusiness\":\"signInAd\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"}}]");
    }

    /**
     * 游戏中心签到查询
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.querySignInBall
     */
    public static String querySignInBall() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.querySignInBall",
                "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]");
    }

    /**
     * 游戏中心签到
     * 对应: com.alipay.gamecenteruprod.biz.rpc.continueSignIn
     */
    public static String continueSignIn() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.continueSignIn",
                "[{\"sceneId\":\"GAME_CENTER\",\"signType\":\"NORMAL_SIGN\",\"source\":\"ch_alipaysearch__chsub_normal\"}]");
    }

    /**
     * 游戏中心任务列表
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v4.queryTaskList
     */
    public static String queryGameCenterTaskList() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v4.queryTaskList",
                "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]");
    }

    /**
     * 游戏中心查询待领取乐豆列表
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.queryPointBallList
     */
    public static String queryPointBallList() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.queryPointBallList",
                "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]");
    }

    /**
     * 收取单个乐豆球
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.receivePointBall
     */
    public static String receivePointBall(String ballId) {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.receivePointBall",
                "[{\"ballId\":\"" + ballId + "\",\"source\":\"ch_alipaysearch__chsub_normal\"}]");
    }

    /**
     * 游戏中心全部领取
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall
     */
    public static String batchReceivePointBall() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall",
                "[{}]");
    }

    /**
     * 游戏中心普通平台任务完成（如貔貅任务）
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSend
     */
    public static String doTaskSend(String taskId) {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSend",
                "[{\"taskId\":\"" + taskId + "\"}]");
    }

    /**
     * 游戏中心签到类平台任务完成（needSignUp = true）
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSignup
     */
    public static String doTaskSignup(String taskId) {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSignup",
                "[{\"source\":\"ch_alipaysearch__chsub_normal\",\"taskId\":\"" + taskId + "\"}]");
    }

    /**
     * 查询芝麻分进度任务列表
     * @return RPC响应
     */
    public static String queryGrowthBehaviorToDoList() {
        String requestData = "[{\"guideBehaviorId\":\"\",\"invokeVersion\":\"1.0.2025.10.27\",\"switchNewPage\":true}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.queryToDoList", requestData);
    }
    
    /**
     * 接受/开启一个行为任务（例如：公益任务）
     * @param behaviorId 任务ID
     * @return RPC响应
     */
    public static String openBehaviorCollect(String behaviorId) {
        String requestData = "[{\"behaviorId\":\"" + behaviorId + "\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.openBehaviorCollect", requestData);
    }

    /**
     * 查询视频答题的题目信息
     * @return RPC响应
     */
    public static String queryDailyQuiz() {
        String requestData = "[{\"behaviorId\":\"shipingwenda\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.queryDailyQuiz", requestData);
    }

    /**
     * 提交视频答题的答案
     * @param bizDate 从queryDailyQuiz获取的bizDate
     * @param questionId 问题ID
     * @param answerId 选择的答案ID
     * @return RPC响应
     */
    public static String pushDailyQuizAnswer(long bizDate, String questionId, String answerId) {
        String extInfo = "{\"answerId\":\"" + answerId + "\",\"answerStatus\":\"RIGHT\",\"questionId\":\"" + questionId + "\"}";
        String requestData = "[{\"behaviorId\":\"shipingwenda\",\"bizDate\":" + bizDate + ",\"extInfo\":" + extInfo + "}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask", requestData);
    }

    /**
     * 查询当前可领取的进度球
     * @return RPC响应
     */
    public static String queryScoreProgress() {
        String requestData = "[{\"needTotalProcess\":\"TRUE\",\"queryGuideInfo\":true,\"switchNewPage\":true}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryScoreProgress", requestData);
    }

    /**
     * [已统一] 收集一个或多个进度球
     * @param ballIdList 包含一个或多个进度球ID的JSONArray
     * @return RPC响应
     */
    public static String collectProgressBall(JSONArray ballIdList) {
        if (ballIdList == null || ballIdList.length() == 0) {
            return "{\"success\":false, \"resultView\":\"ballIdList为空\"}";
        }
        String requestData = "[{\"ballIdList\":" + ballIdList.toString() + "}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.collectProgressBall", requestData);
    }

    /**
     * 芝麻信用首页
     */
    public static String queryHome() {
        return RequestManager.requestString("com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV7RpcManager.queryHome",
                "[{\"invokeSource\":\"zmHome\",\"miniZmGrayInside\":\"\",\"version\":\"week\"}]");
    }

    // ==================== 修改/新增部分开始 ====================

    /**
     * 芝麻信用-查询签到领粒任务列表
     * @return RPC调用结果字符串
     */
    public static String checkInQueryTaskLists() {
        // "version"参数来源于抓包数据
        String requestData = "[{\"version\":\"2025-10-22\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.queryTaskLists", requestData);
    }

    /**
     * 芝麻信用-完成签到领粒任务
     * @param checkInDate 签到日期，格式为 "yyyyMMdd"
     * @return RPC调用结果字符串
     */
    public static String checkInCompleteTask(String checkInDate) {
        String requestData = "[{\"checkInDate\":\"" + checkInDate + "\",\"sceneCode\":\"zml\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.completeTask", requestData);
    }

    /**
     * 获取芝麻信用任务列表
     */
    public static String queryAvailableSesameTask() {
        // [重要] 恢复chInfo参数，确保服务器返回带有recordId的完整任务信息
        String requestData = "[{\"chInfo\":\"ch_zmxy_zmlsy__chsub_zmsy_jingangwei_lianjin\",\"deliverStatus\":\"\",\"deliveryTemplateId\":\"\",\"sceneCode\":\"DAILY_MUST_DO_CARD\",\"searchGuidePopFlag\":true,\"searchSubscribeTask\":true,\"version\":\"new\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3", requestData);
    }

    /**
     * 芝麻信用领取任务
     */
    public static String joinSesameTask(String taskTemplateId) {
        // 根据抓包日志，增加了sceneCode
        String requestData = "[{\"chInfo\":\"seasameList\",\"joinFromOuter\":false,\"sceneCode\":\"zml\",\"templateId\":\"" + taskTemplateId + "\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.joinActivity", requestData);
    }

    /**
     * 芝麻信用获取任务回调
     */
    public static String feedBackSesameTask(String taskTemplateId) {
        // 根据抓包日志，补全了bizType, sceneCode, version等参数
        String requestData = "[{\"actionType\":\"TO_COMPLETE\",\"bizType\":\"LIFE_RECORD\",\"sceneCode\":\"zml\",\"templateId\":\"" + taskTemplateId + "\",\"version\":\"new\"}]";
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.taskFeedback",
                requestData, "zmmemberop", "taskFeedback", "CreditAccumulateStrategyRpcManager");
    }
    
    // ==================== 修改/新增部分结束 ====================

    /**
     * 芝麻信用完成任务
     */
    public static String finishSesameTask(String recordId) {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.pushActivity",
                "[{\"recordId\":\"" + recordId + "\"}]");
    }

    /**
     * 查询可收取的芝麻粒
     */
    public static String queryCreditFeedback() {
        return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.queryCreditFeedback",
                "[{\"queryPotential\":false,\"size\":20,\"status\":\"UNCLAIMED\"}]");
    }

    /**
     * 一键收取芝麻粒
     */
    public static String collectAllCreditFeedback() {
        return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
                "[{\"collectAll\":true,\"status\":\"UNCLAIMED\"}]");
    }

    /**
     * 收取芝麻粒
     *
     * @param creditFeedbackId creditFeedbackId
     */
    public static String collectCreditFeedback(String creditFeedbackId) {
        return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
                "[{\"collectAll\":false,\"creditFeedbackId\":\"" + creditFeedbackId + "\",\"status\":\"UNCLAIMED\"}]");
    }

    /**
     * 获取保障金信息
     */
    public static String queryInsuredHome() {
        return RequestManager.requestString("com.alipay.insplatformbff.insgift.accountService.queryAccountForPlat",
                "[{\"includePolicy\":true,\"specialChannel\":\"wealth_entry\"}]");
    }

    /**
     * 获取所有可领取的保障金
     */
    public static String queryAvailableCollectInsuredGold() {
        return RequestManager.requestString("com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
                "[{\"entrance\":\"wealth_entry\",\"eventToWaitParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"helpChildParamDTO\":{\"giftProdCode\":\"GIFT_HEALTH_GOLD_CHILD\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"priorityChannelParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"signInParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]}}]",
                "insgiftbff", "queryMultiSceneWaitToGainList", "insgiftMain");
    }

    /**
     * 领取保障金
     */
    public static String collectInsuredGold(JSONObject goldBallObj) {
        return RequestManager.requestString("com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
                goldBallObj.toString(), "insgiftbff", "gainMyAndFamilySumInsured", "insgiftMain");
    }

    /**
     * 查询生活记录
     *
     * @return 结果
     */
    public static String promiseQueryHome() {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.queryHome", null);
    }

    /**
     * 查询生活记录明细
     *
     * @param recordId recordId
     * @return 结果
     */
    public static String promiseQueryDetail(String recordId) {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.queryDetail",
                "[{\"recordId\":\"" + recordId + "\"}]");
    }

    /**
     * 生活记录加入新纪录
     *
     * @param data data
     * @return 结果
     */
    public static String promiseJoin(String data) {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.join",
                "[" + data + "]");
    }

    /**
     * 查询待领取的保障金
     *
     * @return 结果
     */
    public static String queryMultiSceneWaitToGainList() {
        return RequestManager.requestString("com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
                "[{\"entrance\":\"jkj_zhima_dairy66\",\"eventToWaitParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\"," +
                        "\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\"," +
                        "\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]}," +
                        "\"helpChildParamDTO\":{\"giftProdCode\":\"GIFT_HEALTH_GOLD_CHILD\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\"," +
                        "\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\"," +
                        "\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"priorityChannelParamDTO\":{\"giftProdCode\":" +
                        "\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\"," +
                        "\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\"," +
                        "\"UNIVERSAL_FRAUD_LIABILITY\"]},\"signInParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\"," +
                        "\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\"," +
                        "\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]}}]");
    }

    /**
     * 领取保障金
     *
     * @param jsonObject jsonObject
     * @return 结果
     */
    public static String gainMyAndFamilySumInsured(JSONObject jsonObject) throws JSONException {
        jsonObject.put("disabled", false);
        jsonObject.put("entrance", "jkj_zhima_dairy66");
        return RequestManager.requestString("com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
                "[" + jsonObject + "]");
    }

    // 安心豆
    public static String querySignInProcess(String appletId, String scene) {
        return RequestManager.requestString("com.alipay.insmarketingbff.bean.querySignInProcess",
                "[{\"appletId\":\"" + appletId + "\",\"scene\":\"" + scene + "\"}]");
    }

    public static String signInTrigger(String appletId, String scene) {
        return RequestManager.requestString("com.alipay.insmarketingbff.bean.signInTrigger",
                "[{\"appletId\":\"" + appletId + "\",\"scene\":\"" + scene + "\"}]");
    }

    public static String beanExchangeDetail(String itemId) {
        return RequestManager.requestString("com.alipay.insmarketingbff.onestop.planTrigger",
                "[{\"extParams\":{\"itemId\":\"" + itemId + "\"},"
                        + "\"planCode\":\"bluebean_onestop\",\"planOperateCode\":\"exchangeDetail\"}]");
    }

    public static String beanExchange(String itemId, int pointAmount) {
        return RequestManager.requestString("com.alipay.insmarketingbff.onestop.planTrigger",
                "[{\"extParams\":{\"itemId\":\"" + itemId + "\",\"pointAmount\":\"" + Integer.toString(pointAmount) + "\"},"
                        + "\"planCode\":\"bluebean_onestop\",\"planOperateCode\":\"exchange\"}]");
    }

    public static String queryUserAccountInfo(String pointProdCode) {
        return RequestManager.requestString("com.alipay.insmarketingbff.point.queryUserAccountInfo",
                "[{\"channel\":\"HiChat\",\"pointProdCode\":\"" + pointProdCode + "\",\"pointUnitType\":\"COUNT\"}]");
    }

    /**
     * 查询会员信息
     */
    public static String queryMemberInfo() {
        String data = "[{\"needExpirePoint\":true,\"needGrade\":true,\"needPoint\":true,\"queryScene\":\"POINT_EXCHANGE_SCENE\",\"source\":\"POINT_EXCHANGE_SCENE\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"\",\"unid\":\"\"}}]";
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.member.h5.queryMemberInfo", data);
    }

    /**
     * 查询0元兑公益道具列表
     *
     * @param userId       userId
     * @param pointBalance 当前可用会员积分
     */
    public static String queryShandieEntityList(String userId, String pointBalance) {
        String uniqueId = System.currentTimeMillis() + userId + "94000SR202501061144200394000SR2025010611458003";
        String data = "[{\"blackIds\":[],\"deliveryIdList\":[\"94000SR2025010611442003\",\"94000SR2025010611458003\"],\"filterCityCode\":false,\"filterPointNoEnough\":false,\"filterStockNoEnough\":false,\"pageNum\":1,\"pageSize\":18,\"point\":" + pointBalance + ",\"previewCopyDbId\":\"\",\"queryType\":\"DELIVERY_ID_LIST\",\"source\":\"member_day\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"0yuandui\",\"unid\":\"\"},\"topIds\":[],\"uniqueId\":\"" + uniqueId + "\"}]";
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.config.h5.queryShandieEntityList", data);
    }

    /**
     * 会员积分兑换道具
     *
     * @param benefitId benefitId
     * @param itemId    itemId
     * @return 结果
     */
    public static String exchangeBenefit(String benefitId, String itemId) {
        String requestId = "requestId" + System.currentTimeMillis();
        String alipayClientVersion = ApplicationHook.getAlipayVersion().getVersionString();
        String data = "[{\"benefitId\":\"" + benefitId + "\",\"cityCode\":\"\",\"exchangeType\":\"POINT_PAY\",\"itemId\":\"" + itemId + "\",\"miniAppId\":\"\",\"orderSource\":\"\",\"requestId\":\"" + requestId + "\",\"requestSourceInfo\":\"\",\"sourcePassMap\":{\"alipayClientVersion\":\"" + alipayClientVersion + "\",\"innerSource\":\"\",\"mobileOsType\":\"Android\",\"source\":\"\",\"unid\":\"\"},\"userOutAccount\":\"\"}]";
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.exchange.h5.exchangeBenefit", data);
    }

    /**
     * 芝麻炼金 - 查询主页信息
     * (这个方法在上次是正确的，保持不变)
     */
    public static String alchemyQueryHome() {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.queryHome", "[{}]");
    }

    /**
     * 芝麻炼金 - 查询攒粒日常任务列表
     * (这个方法在上次是正确的，保持不变)
     */
    public static String alchemyQueryTasks() {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3",
                "[{\"chInfo\":\"\",\"deliverStatus\":\"\",\"deliveryTemplateId\":\"\",\"searchSubscribeTask\":true,\"version\":\"alchemy\"}]");
    }

    /**
     * 芝麻炼金 - 查询签到任务状态
     * (这个方法在上次是正确的，保持不变)
     */
    public static String alchemyQueryCheckInTasks() {
        // 版本日期可能需要动态更新，但暂时使用抓包中的静态值
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.queryTaskLists",
                "[{\"sceneCode\":\"alchemy\",\"version\":\"2025-10-22\"}]");
    }

    /**
     * [修正] 芝麻炼金 - 完成签到任务
     * @param checkInDate YYYYMMDD格式的日期字符串
     */
    public static String completeAlchemyCheckIn(String checkInDate) {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.completeTask",
                "[{\"checkInDate\":\"" + checkInDate + "\",\"sceneCode\":\"alchemy\"}]");
    }

    /**
     * [新增] 芝麻炼金 - 查询场景弹窗 (用于检查次日礼包)
     */
    /*public static String alchemyQueryPopUp() {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.CommonPopUpRpcManager.query",
                "[{\"sceneCode\":\"zm_gold_v1\"}]");
    }*/

    /**
     * [新增] 芝麻炼金 - 领取奖励 (用于领取次日礼包)
     */
    public static String alchemyClaimAward() {
        // 根据抓包数据，requestData 是一个空对象数组
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.claimAward",
                "[{}]");
    }

    /**
     * [新增] 芝麻炼金 - 查询限时任务(早/中/晚饭)
     */
    public static String alchemyQueryTimeLimitedTask() {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.queryTask", "[{}]");
    }

    /**
     * [新增] 芝麻炼金 - 完成限时任务(早/中/晚饭)
     * @param templateId 任务模板ID, 例如 "wujianli" (午饭)
     */
    public static String alchemyCompleteTimeLimitedTask(String templateId) {
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.completeTask",
                "[{\"templateId\":\"" + templateId + "\"}]");
    }
    
    /**
     * [修正] 芝麻炼金 - 执行炼金动作
     * 此版本根据您提供的最新日志精确实现。
     */
    public static String doAlchemy() {
        // 根据抓包日志，requestData 是 [null]
        return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.alchemy", "[null]");
    }

    /**
     * 芝麻树通用触发器
     * @param operation 操作类型
     * @param extInfoJson 额外信息JSON字符串
     * @return RPC响应
     */
    private static String sesameTreeTrigger(String operation, String extInfoJson) {
        String playInfo = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3";
        String refer = "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei";
        String requestData = String.format("[{\"operation\":\"%s\",\"playInfo\":\"%s\",\"refer\":\"%s\",\"extInfo\":%s}]",
                operation, playInfo, refer, extInfoJson);
        return RequestManager.requestString("alipay.promoprod.play.trigger", requestData);
    }

    /**
     * 获取芝麻树主页信息
     * @return RPC响应
     */
    public static String getSesameTreeHomePage() {
        return sesameTreeTrigger("ZHIMA_TREE_HOME_PAGE", "{}");
    }

    /**
     * 获取芝麻树任务列表
     * @return RPC响应
     */
    public static String getSesameTreeTaskList() {
        String extInfo = "{\"batchId\":\"\",\"chInfo\":\"ch_zmzltf__chsub_xinyongsyyingxiaowei\"}";
        return sesameTreeTrigger("RENT_GREEN_TASK_LIST_QUERY", extInfo);
    }

    /**
     * 净化芝麻树（清理电子垃圾）
     * @param trashCode 垃圾代码
     * @param trashCampId 垃圾活动ID
     * @return RPC响应
     */
    public static String cleanSesameTreeTrash(String trashCode, String trashCampId) {
        String extInfo = String.format(
            "{\"clickNum\":\"1\",\"trashCampId\":\"%s\",\"trashCode\":\"%s\",\"treeCode\":\"ZHIMA_TREE\"}",
            trashCampId, trashCode);
        return sesameTreeTrigger("ZHIMA_TREE_CLEAN_AND_PUSH", extInfo);
    }

    /**
     * 新增：净化芝麻树（通过点击按钮，消耗净化次数）
     * @return RPC响应
     */
    public static String cleanSesameTreeByClick() {
        String extInfo = "{\"clickNum\":\"1\",\"treeCode\":\"ZHIMA_TREE\"}";
        return sesameTreeTrigger("ZHIMA_TREE_CLEAN_AND_PUSH", extInfo);
    }

    /**
     * 新增：完成芝麻树任务
     * @param taskId 任务ID
     * @return RPC响应
     */
    public static String finishSesameTreeTask(String taskId) {
        String chInfo = "ch_zmzltf__chsub_xinyongsyyingxiaowei";
        String extInfo = String.format(
            "{\"chInfo\":\"%s\",\"stageCode\":\"send\",\"taskId\":\"%s\"}",
            chInfo, taskId);
        return sesameTreeTrigger("RENT_GREEN_TASK_FINISH", extInfo);
    }

    /**
     * 新增：领取芝麻树任务奖励
     * @param taskId 任务ID
     * @return RPC响应
     */
    public static String receiveSesameTreeTaskReward(String taskId) {
        String chInfo = "ch_zmzltf__chsub_xinyongsyyingxiaowei";
        String extInfo = String.format(
            "{\"chInfo\":\"%s\",\"stageCode\":\"receive\",\"taskId\":\"%s\"}",
            chInfo, taskId);
        return sesameTreeTrigger("RENT_GREEN_TASK_FINISH", extInfo);
    }

    // ================= 年度回顾（任务中心） =================
    public static final String ANNUAL_REVIEW_OPERATION_IDENTIFY =
            "independent_component_program2025111803036407";
    public static final String ANNUAL_REVIEW_COMPONENT_PREFIX =
            "independent_component_task_reward_v2_02888775";
    public static final String ANNUAL_REVIEW_QUERY_COMPONENT =
            ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_query";
    public static final String ANNUAL_REVIEW_APPLY_COMPONENT =
            ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_apply";
    public static final String ANNUAL_REVIEW_PROCESS_COMPONENT =
            ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_process";
    public static final String ANNUAL_REVIEW_Get =
            ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_process";
    public static final String ANNUAL_REVIEW_GET_REWARD_COMPONENT =
            ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_get_reward";

    private static JSONObject buildAnnualReviewBasePayload() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("channel", "share");
        root.put("cityCode", "110000");
        root.put("operationParamIdentify", ANNUAL_REVIEW_OPERATION_IDENTIFY);
        // 默认 source 为查询组件，具体请求中可覆盖
        root.put("source", ANNUAL_REVIEW_QUERY_COMPONENT);
        return root;
    }

    /**
     * 年度回顾 - 查询任务列表
     *
     * 对应文档示例：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_query
     */
    public static String annualReviewQueryTasks() {
        try {
            JSONObject body = buildAnnualReviewBasePayload();
            JSONObject components = new JSONObject();
            components.put(ANNUAL_REVIEW_QUERY_COMPONENT, new JSONObject());
            body.put("components", components);
            body.put("source", ANNUAL_REVIEW_QUERY_COMPONENT);

            return RequestManager.requestString(
                    "alipay.imasp.program.programInvoke",
                    new JSONArray().put(body).toString()
            );
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 年度回顾 - 领取单个任务（apply）
     *
     * 请求示例参见文档：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_apply
     */
    public static String annualReviewApplyTask(String code) {
        try {
            JSONObject body = buildAnnualReviewBasePayload();

            JSONObject compBody = new JSONObject();
            compBody.put("code", code);
            compBody.put("consultAfterLuckDraw", "false");
            compBody.put("skipLuckDrawConsult", "true");

            JSONObject components = new JSONObject();
            components.put(ANNUAL_REVIEW_APPLY_COMPONENT, compBody);

            body.put("components", components);
            body.put("source", ANNUAL_REVIEW_APPLY_COMPONENT);

            return RequestManager.requestString(
                    "alipay.imasp.program.programInvoke",
                    new JSONArray().put(body).toString()
            );
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 年度回顾 - 提交任务完成（process）
     *
     * 请求示例参见文档：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_process
     */
    public static String annualReviewProcessTask(String code, String recordNo) {
        try {
            JSONObject body = buildAnnualReviewBasePayload();

            JSONObject compBody = new JSONObject();
            compBody.put("code", code);
            compBody.put("recordNo", recordNo);

            JSONObject components = new JSONObject();
            components.put(ANNUAL_REVIEW_PROCESS_COMPONENT, compBody);

            body.put("components", components);
            body.put("source", ANNUAL_REVIEW_PROCESS_COMPONENT);

            return RequestManager.requestString(
                    "alipay.imasp.program.programInvoke",
                    new JSONArray().put(body).toString()
            );
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 年度回顾 - 领取奖励（get_reward）
     *
     * 在任务完成后，根据 code + recordNo 领取成长值奖励。
     */
    public static String annualReviewGetReward(String code, String recordNo) {
        try {
            JSONObject body = buildAnnualReviewBasePayload();

            JSONObject compBody = new JSONObject();
            compBody.put("code", code);
            compBody.put("consultAfterLuckDraw", "false");
            compBody.put("recordNo", recordNo);
            compBody.put("skipLuckDrawConsult", "true");

            JSONObject components = new JSONObject();
            components.put(ANNUAL_REVIEW_GET_REWARD_COMPONENT, compBody);

            body.put("components", components);
            body.put("source", ANNUAL_REVIEW_GET_REWARD_COMPONENT);

            return RequestManager.requestString(
                    "alipay.imasp.program.programInvoke",
                    new JSONArray().put(body).toString()
            );
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * [新] 福利中心首页
     */
    public static String queryWelfareHome() {
        try {
            JSONObject args = new JSONObject();
            args.put("isResume", true);
            // 接口: com.alipay.finaggexpbff.needle.welfareCenter.index
            return RequestManager.requestString("com.alipay.finaggexpbff.needle.welfareCenter.index",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * [新] 任务查询推送
     */
    public static String taskQueryPush(String taskId) {
        try {
            JSONObject args = new JSONObject();
            args.put("mode", 1); // 固定参数
            args.put("taskId", taskId);

            // 接口: com.alipay.wealthgoldtwa.needle.taskQueryPush
            return RequestManager.requestString("com.alipay.wealthgoldtwa.needle.taskQueryPush",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 签到 / 领取奖励
     * @param type "SIGN"
     */
    public static String welfareCenterTrigger(String type) {
        try {
            JSONObject args = new JSONObject();
            args.put("type", type);
            return RequestManager.requestString("com.alipay.finaggexpbff.needle.welfareCenter.trigger",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 任务触发/报名
     */
    public static String goldBillTaskTrigger(String taskId) {
        try {
            JSONObject args = new JSONObject();
            args.put("taskId", taskId);
            return RequestManager.requestString("com.alipay.wealthgoldtwa.goldbill.v4.task.trigger",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * [新增] 查询黄金票提取页信息
     * 用于获取最新的可用数量、基金ID (productId) 和 赠送份数 (bonusAmount)
     */
    public static String queryConsumeHome() {
        try {
            JSONObject args = new JSONObject();
            args.put("tabBubbleDeliverParam", new JSONObject());
            args.put("tabTypeDeliverParam", new JSONObject());
            // 接口: com.alipay.wealthgoldtwa.needle.consume.query
            return RequestManager.requestString("com.alipay.wealthgoldtwa.needle.consume.query",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * [新增] 提交提取黄金
     * @param amount 提取数量 (如 100, 200, 2900)
     * @param productId 基金ID
     * @param bonusAmount 额外赠送数量
     */
    public static String submitConsume(int amount, String productId, int bonusAmount) {
        try {
            JSONObject args = new JSONObject();
            args.put("exchangeAmount", amount);
            // 计算金额：100份 = 0.10元。公式：份数 / 1000.0
            args.put("exchangeMoney", String.format("%.2f", amount / 1000.0));
            args.put("prizeType", "GOLD"); // 固定为黄金
            args.put("productId", productId);
            args.put("bonusAmount", bonusAmount);
            // 接口: com.alipay.wealthgoldtwa.needle.consume.submit
            return RequestManager.requestString("com.alipay.wealthgoldtwa.needle.consume.submit",
                    new JSONArray().put(args).toString());
        } catch (Exception e) {
            return null;
        }
    }
}