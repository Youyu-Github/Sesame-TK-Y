package fansirsqi.xposed.sesame.task.antOrchard;

import java.util.List;
import fansirsqi.xposed.sesame.hook.RequestManager;

public class AntOrchardRpcCall {
        // 保持使用较新版本号
        private static final String VERSION = "20251209.01";

        public static String orchardIndex() {
                return RequestManager.requestString("com.alipay.antfarm.orchardIndex",
                        "[{\"inHomepage\":\"true\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        // Java版保留的一键捉鸡功能需要此方法
        public static String friendList() {
                return RequestManager.requestString("com.alipay.antorchard.friendList",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String extraInfoGet() {
                return RequestManager.requestString("com.alipay.antorchard.extraInfoGet",
                        "[{\"from\":\"entry\",\"requestType\":\"NORMAL\",\"sceneCode\":\"FUGUO\",\"source\":\"ch_alipaysearch__chsub_normal\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String batchHireAnimalRecommend(String orchardUserId) {
                return RequestManager.requestString("com.alipay.antorchard.batchHireAnimalRecommend",
                        "[{\"orchardUserId\":\"" + orchardUserId
                                + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"sceneType\":\"weed\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        /**
         * 批量雇佣小动物
         * [OPTIMIZED] 修复了原先 String.join 导致的 JSON 数组格式错误 (e.g., [id1,id2])
         * 修正为生成标准的 JSON 字符串数组 (e.g., ["id1","id2"])
         */
        public static String batchHireAnimal(List<String> recommendGroupList) {
                StringBuilder quotedGroups = new StringBuilder();
                if (recommendGroupList != null && !recommendGroupList.isEmpty()) {
                        for (int i = 0; i < recommendGroupList.size(); i++) {
                                quotedGroups.append("\"").append(recommendGroupList.get(i)).append("\"");
                                if (i < recommendGroupList.size() - 1) {
                                        quotedGroups.append(",");
                                }
                        }
                }
                return RequestManager.requestString("com.alipay.antorchard.batchHireAnimal",
                        "[{\"recommendGroupList\":[" + quotedGroups.toString()
                                + "],\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"sceneType\":\"weed\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String extraInfoSet() {
                return RequestManager.requestString("com.alipay.antorchard.extraInfoSet",
                        "[{\"bizCode\":\"fertilizerPacket\",\"bizParam\":{\"action\":\"queryCollectFertilizerPacket\"},\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String querySubplotsActivity(String treeLevel) {
                return RequestManager.requestString("com.alipay.antorchard.querySubplotsActivity",
                        "[{\"activityType\":[\"WISH\",\"BATTLE\",\"HELP_FARMER\",\"DEFOLIATION\",\"CAMP_TAKEOVER\"],\"inHomepage\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"treeLevel\":\""
                                + treeLevel + "\",\"version\":\"" + VERSION + "\"}]");
        }

        public static String triggerSubplotsActivity(String activityId, String activityType, String optionKey) {
                return RequestManager.requestString("com.alipay.antorchard.triggerSubplotsActivity",
                        "[{\"activityId\":\"" + activityId + "\",\"activityType\":\"" + activityType
                                + "\",\"optionKey\":\"" + (optionKey == null ? "" : optionKey)
                                + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String receiveOrchardRights(String activityId, String activityType) {
                return RequestManager.requestString("com.alipay.antorchard.receiveOrchardRights",
                        "[{\"activityId\":\"" + activityId + "\",\"activityType\":\"" + activityType
                                + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String drawLottery() {
                return RequestManager.requestString("com.alipay.antorchard.drawLottery",
                        "[{\"lotteryScene\":\"receiveLotteryPlus\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String orchardSyncIndex() {
                return RequestManager.requestString("com.alipay.antorchard.orchardSyncIndex",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"syncIndexTypes\":\"QUERY_MAIN_ACCOUNT_INFO\",\"version\":\""
                                + VERSION + "\"}]");
        }

        /**
         * 施肥
         * 注意：Kotlin代码中 "version":$VERSION (无引号)，此处严格同步
         */
        /*public static String orchardSpreadManure(String wua, String source) {
                return RequestManager.requestString("com.alipay.antfarm.orchardSpreadManure",
                        "[{\"plantScene\":\"main\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"" + source + "\",\"useBatchSpread\":false,\"version\":"
                                + VERSION + ",\"wua\":\"" + wua + "\"}]");
        }*/
        /**
         * 施肥
         * @param wua 用户标识
         * @param source 来源标识，可自定义
         * @return 服务器返回的响应字符串
         */
        public static String orchardSpreadManure(String wua, String source) {
                // 通过字符串拼接构建 JSON，以避免 String.format 的区域设置（Locale）问题
                // 这种方式能确保浮点数的小数点始终是 '.'，符合 JSON 规范
                String jsonPayload = "[{\"plantScene\":\"main\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\""
                                + source + "\",\"useBatchSpread\":false,\"version\":"
                                + VERSION + ",\"wua\":\""
                                + wua + "\"}]";

                return RequestManager.requestString(
                                "com.alipay.antfarm.orchardSpreadManure",
                                jsonPayload);
        }

        /**
         * 砸金蛋
         * @param count 砸蛋的数量
         * @return 服务器返回的响应字符串
         */
        public static String smashedGoldenEgg(int count) {
                // [FIXED] 使用字符串拼接，避免 String.format 因区域设置问题导致 JSON 格式错误
                String jsonArgs = "[{\"batchSmashCount\":" + count
                                + ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]";

                return RequestManager.requestString(
                                "com.alipay.antorchard.smashedGoldenEgg",
                                jsonArgs);
        }

        /**
         * 收取小组件的回访奖励
         * @return RPC 调用返回的字符串
         */
        public static String receiveOrchardVisitAward() {
                // [FIXED] 使用字符串拼接，避免 String.format 因区域设置问题导致 JSON 格式错误
                String args = "[{\"diversionSource\":\"widget\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"widget_shoufei\",\"version\":\""
                                + VERSION + "\"}]";

                return RequestManager.requestString("com.alipay.antorchard.receiveOrchardVisitAward", args);
        }

        /**
         * 同步农场索引数据，用于获取限时挑战等信息
         * @param wua 环境参数 Wua
         * @return RPC 调用返回的字符串
         */
        public static String orchardSyncIndex(String wua) {
                // [FIXED] 使用字符串拼接，避免 String.format 和 replace('\'', '\"') 带来的风险和性能开销
                String args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"syncIndexTypes\":\"LIMITED_TIME_CHALLENGE\",\"useWua\":true,\"version\":\""
                                + VERSION + "\",\"wua\":\"" + wua + "\"}]";

                return RequestManager.requestString("com.alipay.antorchard.orchardSyncIndex", args);
        }

        /**
         * 通知服务器开始了一个游戏任务
         * @param appId 游戏的 appId
         * @return RPC 调用返回的字符串
         */
        public static String noticeGame(String appId) {
                // [FIXED] 使用字符串拼接，避免 String.format 和 replace('\'', '\"') 带来的风险
                String args = "[{\"appId\":\"" + appId
                                + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]";

                return RequestManager.requestString("com.alipay.antorchard.noticeGame", args);
        }

        public static String receiveTaskAward(String sceneCode, String taskType) {
                return RequestManager.requestString("com.alipay.antiep.receiveTaskAward",
                        "[{\"ignoreLimit\":true,\"requestType\":\"NORMAL\",\"sceneCode\":\"" + sceneCode
                                + "\",\"source\":\"ch_alipaysearch__chsub_normal\",\"taskType\":\""
                                + taskType + "\",\"version\":\"" + VERSION + "\"}]");
        }

        public static String orchardListTask() {
                return RequestManager.requestString("com.alipay.antfarm.orchardListTask",
                        "[{\"plantHiddenMMC\":\"false\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String orchardSign() {
                return RequestManager.requestString("com.alipay.antfarm.orchardSign",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"signScene\":\"ANTFARM_ORCHARD_SIGN_V2\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String finishTask(String userId, String sceneCode, String taskType) {
                return RequestManager.requestString("com.alipay.antiep.finishTask",
                        "[{\"outBizNo\":\"" + userId + System.currentTimeMillis()
                                + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"" + sceneCode
                                + "\",\"source\":\"ch_appcenter__chsub_9patch\",\"taskType\":\""
                                + taskType + "\",\"userId\":\"" + userId + "\",\"version\":\"" + VERSION
                                + "\"}]");
        }

        public static String triggerTbTask(String taskId, String taskPlantType) {
                return RequestManager.requestString("com.alipay.antfarm.triggerTbTask",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"taskId\":\""
                                + taskId + "\",\"taskPlantType\":\"" + taskPlantType
                                + "\",\"version\":\"" + VERSION + "\"}]");
        }

        public static String orchardSelectSeed() {
                return RequestManager.requestString("com.alipay.antfarm.orchardSelectSeed",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"seedCode\":\"rp\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String queryGameCenter() {
                return RequestManager.requestString("com.alipay.antorchard.queryGameCenter",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION + "\"}]");
        }

        public static String submitUserAction(String gameId) {
                return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.submitUserAction",
                        "[{\"actionCode\":\"enterGame\",\"gameId\":\"" + gameId
                                + "\",\"paladinxVersion\":\"2.0.13\",\"source\":\"gameFramework\"}]");
        }

        public static String submitUserPlayDurationAction(String gameAppId, String source) {
                return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.submitUserPlayDurationAction",
                        "[{\"gameAppId\":\"" + gameAppId + "\",\"playTime\":32,\"source\":\"" + source
                                + "\",\"statisticTag\":\"\"}]");
        }

        public static String smashedGoldenEgg() {
                return RequestManager.requestString("com.alipay.antorchard.smashedGoldenEgg",
                        "[{\"requestType\":\"NORMAL\",\"seneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                                + VERSION
                                + "\"}]");
        }

        public static String achieveBeShareP2P(String shareId) {
                return RequestManager.requestString("com.alipay.antiep.achieveBeShareP2P",
                        "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_ORCHARD_SHARE_P2P\",\"shareId\":\""
                                + shareId
                                + "\",\"source\":\"share\",\"version\":\""
                                + VERSION + "\"}]");
        }
}