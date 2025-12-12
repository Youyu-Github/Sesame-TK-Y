package fansirsqi.xposed.sesame.task.antCooperate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

import fansirsqi.xposed.sesame.hook.RequestManager;
public class AntCooperateRpcCall {
    private static final String VERSION = "20230501";
    public static String queryUserCooperatePlantList() {
        return RequestManager.requestString("alipay.antmember.forest.h5.queryUserCooperatePlantList", "[{}]");
    }
    public static String queryCooperatePlant(String coopId) {
        String args1 = "[{\"cooperationId\":\"" + coopId + "\"}]";
        return RequestManager.requestString("alipay.antmember.forest.h5.queryCooperatePlant", args1);
    }
    public static String cooperateWater(String uid, String coopId, int count) {
        return RequestManager.requestString("alipay.antmember.forest.h5.cooperateWater",
                "[{\"bizNo\":\"" + uid + "_" + coopId + "_" + System.currentTimeMillis() + "\",\"cooperationId\":\""
                        + coopId + "\",\"energyCount\":" + count + ",\"source\":\"\",\"version\":\"" + VERSION
                        + "\"}]");
    }

    /**
     * 获取真爱合种首页信息
     */
    public static String loveHome() {
        return RequestManager.requestString("alipay.greenmatrix.rpc.h5.love.loveHome", "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    /**
     * 真爱合种浇水
     * @param teamId 队伍ID
     * @param count 浇水能量
     */
    public static String loveTeamWater(String teamId, int count) {
        return RequestManager.requestString("alipay.greenmatrix.rpc.h5.love.teamWater",
                "[{\"donateNum\":" + count + ",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"teamId\":\"" + teamId + "\"}]");
    }

    /**
     * 获取真爱合种森林首页信息（备用方法）
     */
    public static String loveForestHome() {
        return RequestManager.requestString("alipay.greenmatrix.rpc.h5.love.loveForestHome", "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    /**
     * 查询森林主页 (获取TeamID)
     */
    public static String queryForestHomePage() {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryHomePage",
                "[{\"configVersionMap\":{\"wateringBubbleConfig\":\"0\"},\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"20250818\"}]");
    }

    /**
     * 查询组队版杂项信息 (获取今日剩余浇水次数/额度)
     */
    public static String queryTeamMiscInfo(String teamId) {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryMiscInfo",
                "[{\"queryBizType\":\"teamCanWaterCount\",\"source\":\"SELF_HOME\",\"targetUserId\":\"" + teamId + "\"}]");
    }

    /**
     * 森林组队版浇水
     */
    public static String teamWater(String teamId, int count) {
        try {
            JSONObject args = new JSONObject();
            args.put("energyCount", count);
            
            // 1. 生成毫秒级时间戳
            long ts = System.currentTimeMillis();
            
            // 2. 生成 8 位随机 Hex 字符串 (利用 UUID 截取是生成 Hex 最快最安全的方法)
            String randomHex = UUID.randomUUID().toString().substring(0, 8);
            
            // 3. 拼接 sToken
            String sToken = ts + "_" + randomHex;
            
            args.put("sToken", sToken);
            args.put("source", "chInfo_ch_appcenter__chsub_9patch");
            args.put("teamId", teamId);
            
            return RequestManager.requestString("alipay.antforest.forest.h5.teamWater", "[" + args.toString() + "]");
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * 获取合种浇水量排行
     * @param bizType 参数：D/A,"D"为查询当天，"A"为查询所有
     * @param coopId 合种ID
     * @return
     */
    public static String queryCooperateRank(String bizType, String coopId) {
        return  RequestManager.requestString("alipay.antmember.forest.h5.queryCooperateRank",
                "[{\"bizType\":\""+ bizType + "\",\"cooperationId\":\"" + coopId + "\",\"source\":\"chInfo_ch_url-https://render.alipay.com/p/yuyan/180020010001247580/home.html\"}]");
    }

    /**
     * 召唤队友浇水
     * @param userId 用户ID
     * @param cooperationId 合种ID
     * @return requestString
     */
    public static String sendCooperateBeckon(String userId, String cooperationId) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("bizImage","https://gw.alipayobjects.com/zos/rmsportal/gzYPfxdAxLrkzFUeVkiY.jpg");
        jo.put("link","lipays://platformapi/startapp?appId=66666886&url=%2Fwww%2Fcooperation%2Findex.htm%3FcooperationId%3D"+cooperationId+"%26sourceName%3Dcard");
        jo.put("midTitle","快来给我们的树苗浇水，让它快快长大。");
        jo.put("noticeLink","alipays://platformapi/startapp?appId=60000002&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fc%2F17ussbd8vtfg%2Fmessage.html%3FsourceName%3Dcard&showOptionMenu=NO&transparentTitle=NO");
        jo.put("topTitle","树苗需要你的呵护");
        jo.put("source","chInfo_ch_url-https://render.alipay.com/p/yuyan/180020010001247580/home.html");
        jo.put("cooperationId",cooperationId);
        jo.put("userId",userId);
        return  RequestManager.requestString("alipay.antmember.forest.h5.sendCooperateBeckon",
                new JSONArray().put(jo).toString());
    }
}