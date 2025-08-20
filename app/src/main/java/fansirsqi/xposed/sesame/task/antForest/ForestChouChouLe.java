package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;

public class ForestChouChouLe {

    private static final String TAG = ForestChouChouLe.class.getSimpleName();

    void chouChouLe() {
        try {
            boolean doublecheck;
            String source = "task_entry";
            JSONObject jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (!ResChecker.checkRes(TAG + "进入森林寻宝活动失败:", jo)) return;
            JSONObject drawScene = jo.getJSONObject("drawScene");
            JSONObject drawActivity = drawScene.getJSONObject("drawActivity");
            String activityId = drawActivity.getString("activityId");
            String sceneCode = drawActivity.getString("sceneCode"); // ANTFOREST_NORMAL_DRAW
            String listSceneCode = sceneCode + "_TASK";

            long startTime = drawActivity.getLong("startTime");
            long endTime = drawActivity.getLong("endTime");
            do {
                doublecheck = false;
                if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {// 时间范围内
                    Log.record("延时1S");
                    GlobalThreadPools.sleep(1000L);
                    JSONObject listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskopengreen(activityId, listSceneCode, source));
                    if (ResChecker.checkRes(TAG + "查询森林寻宝任务列表失败:", listTaskopengreen)) {
                        JSONArray taskList = listTaskopengreen.getJSONArray("taskInfoList");
                        // 处理任务列表
                        for (int i = 0; i < taskList.length(); i++) {
                            JSONObject taskInfo = taskList.getJSONObject(i);
                            JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                            JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                            String taskName = bizInfo.getString("title");
                            String taskSceneCode = taskBaseInfo.getString("sceneCode");// == listSceneCode ==ANTFOREST_NORMAL_DRAW_TASK
                            String taskStatus = taskBaseInfo.getString("taskStatus"); // 任务状态: TODO => FINISHED => RECEIVED
                            String taskType = taskBaseInfo.getString("taskType");

                            JSONObject taskRights = taskInfo.getJSONObject("taskRights");

                            int rightsTimes = taskRights.getInt("rightsTimes");//当完成行次数
                            int rightsTimesLimit = taskRights.getInt("rightsTimesLimit");//可完成行次数

                            // GlobalThreadPools.sleep(1000L * 3);

                            //注意这里的 taskSceneCode=listSceneCode = ANTFOREST_NORMAL_DRAW_TASK， sceneCode = ANTFOREST_NORMAL_DRAW

                            if (taskStatus.equals(TaskStatus.TODO.name())) { //适配签到任务
                                if(!("邀请好友助力得机会".equals(taskName))) {
                                    Log.record("任务延时3S:"+taskName);
                                    GlobalThreadPools.sleep(1000L * 3);
                                }
                                if (taskType.equals("NORMAL_DRAW_EXCHANGE_VITALITY")) {//活力值兑换次数
                                    String sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(activityId, sceneCode, source, taskSceneCode, taskType);
                                    if (ResChecker.checkRes(TAG + "森林寻宝活力值兑换失败:", sginRes)) {
                                        Log.forest( "森林寻宝🧾：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_XLIGHT_1")) {
                                    String sginRes = AntForestRpcCall.finishTask4Chouchoule(taskType, taskSceneCode);
                                    if (ResChecker.checkRes(TAG + "森林寻宝完成任务失败:", sginRes)) {
                                        Log.forest( "森林寻宝🧾：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_ANTTODO")) {
                                    String sginRes = AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode);
                                    if (ResChecker.checkRes(TAG + "森林寻宝完成任务失败:", sginRes)) {
                                        Log.forest( "森林寻宝🧾：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_CAR30s")) {// 去玩森林小车车
                                    String sginRes =  AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode);
                                    if (ResChecker.checkRes(TAG, sginRes)) {
                                        Log.forest( "森林寻宝🧾：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_XIAO30s")) {// 去玩森林消消乐
                                    String sginRes =  AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode);
                                    if (ResChecker.checkRes(TAG, sginRes)) {
                                        Log.forest( "森林寻宝🧾：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                            }

                            if (taskStatus.equals(TaskStatus.FINISHED.name())) {// 领取奖励
                                Log.record("奖励延时3S:"+taskName);
                                GlobalThreadPools.sleep(1000L * 3);
                                String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType);
                                if (ResChecker.checkRes(TAG + "森林寻宝领取任务奖励失败:", sginRes)) {
                                    Log.forest( "森林寻宝🧾：" + taskName);
                                    // 检查是否需要再次检测任务
                                    if (rightsTimesLimit - rightsTimes > 0) {
                                        doublecheck = true;
                                    }
                                }
                            }

                        }

                    }
                }

            } while (doublecheck);

            // 执行抽奖
            jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (ResChecker.checkRes(TAG + "进入森林寻宝活动失败:", jo)) {
                drawScene = jo.getJSONObject("drawScene");
                drawActivity = drawScene.getJSONObject("drawActivity");
                activityId = drawActivity.getString("activityId");
                sceneCode = drawActivity.getString("sceneCode");

                JSONObject drawAsset = jo.getJSONObject("drawAsset");
                int blance = drawAsset.optInt("blance", 0);
                while (blance > 0) {
                    jo = new JSONObject(AntForestRpcCall.drawopengreen(activityId, sceneCode, source, UserMap.getCurrentUid()));
                    if (ResChecker.checkRes(TAG + "森林寻宝抽奖失败:", jo)) {
                        drawAsset = jo.getJSONObject("drawAsset");
                        blance = drawAsset.getInt("blance");
                        JSONObject prizeVO = jo.getJSONObject("prizeVO");
                        String prizeName = prizeVO.getString("prizeName");
                        int prizeNum = prizeVO.getInt("prizeNum");
                        Log.forest("森林寻宝🎁[领取: " + prizeName + "*" + prizeNum + "]");
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

    }


}
