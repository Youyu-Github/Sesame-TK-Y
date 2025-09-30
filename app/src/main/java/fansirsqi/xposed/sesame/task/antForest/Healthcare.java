package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.data.Statistics;

/**
 * @author Byseven
 * @date 2025/3/7
 * @apiNote
 */
public class Healthcare {

    public static final String TAG = Healthcare.class.getSimpleName();

    public static void queryForestEnergy(String scene) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.queryForestEnergy(scene));
            if (!ResChecker.checkRes(TAG + "查询森林医疗健康能量失败:", jo)) {
                return;
            }
            jo = jo.getJSONObject("data").getJSONObject("response");
            JSONArray ja = jo.getJSONArray("energyGeneratedList");
            if (ja.length() > 0) {
                harvestForestEnergy(scene, ja);
            }
            int remainBubble = jo.optInt("remainBubble");
            for (int i = 0; i < remainBubble; i++) {
                ja = produceForestEnergy(scene);
                if (ja.length() == 0 || !harvestForestEnergy(scene, ja)) {
                    return;
                }
                TimeUtil.sleep(1000);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "queryForestEnergy err:");
            Log.printStackTrace(TAG, th);
        }
    }

    private static JSONArray produceForestEnergy(String scene) {
        JSONArray energyGeneratedList = new JSONArray();
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.produceForestEnergy(scene));
            if (ResChecker.checkRes(TAG + "产生森林医疗健康能量失败:", jo)) {
                jo = jo.getJSONObject("data").getJSONObject("response");
                energyGeneratedList = jo.getJSONArray("energyGeneratedList");
                if (energyGeneratedList.length() > 0) {
                    String title = scene.equals("FEEDS") ? "绿色医疗" : "电子小票";
                    int cumulativeEnergy = jo.getInt("cumulativeEnergy");
                    Log.forest("医疗健康🚑完成[" + title + "]#产生[" + cumulativeEnergy + "g能量]");
                    Statistics.addData(Statistics.DataType.COLLECTED, cumulativeEnergy);
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "produceForestEnergy err:");
            Log.printStackTrace(TAG, th);
        }
        return energyGeneratedList;
    }

    private static Boolean harvestForestEnergy(String scene, JSONArray bubbles) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.harvestForestEnergy(scene, bubbles));
            if (!ResChecker.checkRes(TAG + "收取森林医疗健康能量失败:", jo)) {
                return false;
            }
            jo = jo.getJSONObject("data").getJSONObject("response");
            int collectedEnergy = jo.getInt("collectedEnergy");
            if (collectedEnergy > 0) {
                String title = scene.equals("FEEDS") ? "绿色医疗" : "电子小票";
                Log.forest("医疗健康🚑收取[" + title + "]#获得[" + collectedEnergy + "g能量]");
                return true;
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "harvestForestEnergy err:");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }


}
