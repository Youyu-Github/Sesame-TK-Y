package fansirsqi.xposed.sesame.task.consumeGold;
import java.util.UUID;
import fansirsqi.xposed.sesame.hook.RequestManager;
public class ConsumeGoldRpcCall {
    private static final String ALIPAY_VERSION = "10.6.80.8000";
    private static String getRequestId() {
        StringBuilder sb = new StringBuilder();
        for (String str : UUID.randomUUID().toString().split("-")) {
            sb.append(str.substring(str.length() / 2));
        }
        return sb.toString().toUpperCase();
    }
    private static String getClientTraceId() {
        return UUID.randomUUID().toString();
    }
    /**
     * 获取签到状态
     */
    public static String signinCalendar() {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.task.signin.calendar",
                "[{\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"clientTraceId\":\""
                        + getClientTraceId() + "\",\"clientVersion\":\"6.5.0\"}]");
    }
    /**
     * 签到
     */
    public static String taskOpenBoxAward() {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.task.openBoxAward",
                "[{\"actionAwardDetails\":[{\"actionType\":\"date_sign_start\"}],\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"bizType\":\"CONSUME_GOLD\",\"boxType\":\"CONSUME_GOLD_SIGN_DATE\",\"timeScaleType\":0,\"userType\":\"old\"}]");
    }
    /**
     * 消费金首页
     */
    public static String promoIndex() {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.home.promo.index",
                "[{\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\""
                        + getClientTraceId() + "\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"UnFavorite\"}]");
    }
    /**
     * 消费金抽奖
     */
    public static String promoTrigger() {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.index.promo.trigger",
                "[{\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\""
                        + UUID.randomUUID().toString()
                        + "\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"UnFavorite\",\"requestId\":\""
                        + getRequestId() + "\"}]");
    }
    public static String taskV2Index(String taskSceneCode) {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.taskV2.index",
                "[{\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\""
                        + getClientTraceId() + "\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"\",\"taskSceneCode\":\""
                        + taskSceneCode + "\"}]");
    }
    public static String taskV2Trigger(String taskId, String taskSceneCode, String action) {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.taskV2.trigger",
                "[{\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"clientTraceId\":\""
                        + getClientTraceId() + "\",\"clientVersion\":\"6.5.0\",\"taskId\":\""
                        + taskId + "\",\"taskSceneCode\":\""
                        + taskSceneCode + "\",\"triggerAction\":\""
                        + action + "\"}]");
    }
    /**
     * 补签
     *
     * @param actionType 动作类型
     *                   - check 表示[二次确认]
     *                   - repair 表示[确认]
     * @param repairDate 补签日期
     */
    public static String signinTrigger(String actionType, String repairDate) {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.repair.signin.trigger",
                "[{\"actionType\":\""
                        + actionType + "\",\"alipayAppVersion\":\"" + ALIPAY_VERSION + "\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"bizType\":\"CONSUME_GOLD\",\"boxType\":\"CONSUME_GOLD_SIGN_DATE\",\"clientTraceId\":\""
                        + getClientTraceId() + "\",\"clientVersion\":\"6.5.0\",\"repairDate\":\""
                        + repairDate + "\"}]");
    }
}