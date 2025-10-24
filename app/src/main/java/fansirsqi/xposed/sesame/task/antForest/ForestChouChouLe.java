package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;

// by
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ForestChouChouLe {

    private static final String TAG = ForestChouChouLe.class.getSimpleName();

    // 任务尝试次数计数，避免重复失败
    private final java.util.Map<String, AtomicInteger> taskTryCount = new java.util.concurrent.ConcurrentHashMap<>();

    /*
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
                    if (ResChecker.checkRes(TAG, listTaskopengreen)) {
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
    */

    /*
    void chouChouLe() {
        try {
            boolean doublecheck;
            String source = "task_entry";

            // ==================== 手动屏蔽任务集合 ====================
            Set<String> presetBad = new LinkedHashSet<>();
            presetBad.add("FOREST_NORMAL_DRAW_SHARE");  // 邀请好友任务（屏蔽）
            // 你可以在这里继续添加更多要屏蔽的任务
            // presetBad.add("xxx");
            // =====================================================

            JSONObject jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (!ResChecker.checkRes(TAG, jo)) return;

            JSONObject drawScene = jo.getJSONObject("drawScene");
            JSONObject drawActivity = drawScene.getJSONObject("drawActivity");
            String activityId = drawActivity.getString("activityId");
            String sceneCode = drawActivity.getString("sceneCode"); // ANTFOREST_NORMAL_DRAW
            String listSceneCode = sceneCode + "_TASK";

            long startTime = drawActivity.getLong("startTime");
            long endTime = drawActivity.getLong("endTime");

            int loopCount = 0;           // 循环次数计数
            final int MAX_LOOP = 5;      // 最大循环次数，避免死循环

            do {
                doublecheck = false;
                if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {
                    Log.record("延时1S");
                    GlobalThreadPools.sleep(1000L);

                    JSONObject listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskopengreen(activityId, listSceneCode, source));
                    if (ResChecker.checkRes(TAG, listTaskopengreen)) {
                        JSONArray taskList = listTaskopengreen.getJSONArray("taskInfoList");

                        for (int i = 0; i < taskList.length(); i++) {
                            JSONObject taskInfo = taskList.getJSONObject(i);
                            JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                            JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                            String taskName = bizInfo.getString("title");
                            String taskSceneCode = taskBaseInfo.getString("sceneCode");
                            String taskStatus = taskBaseInfo.getString("taskStatus");
                            String taskType = taskBaseInfo.getString("taskType");

                            JSONObject taskRights = taskInfo.getJSONObject("taskRights");
                            int rightsTimes = taskRights.getInt("rightsTimes");
                            int rightsTimesLimit = taskRights.getInt("rightsTimesLimit");

                            // ==================== 屏蔽逻辑 ====================
                            if (presetBad.contains(taskType)) {
                                Log.record("已屏蔽任务，跳过：" + taskName);
                                continue;
                            }
                            // ==============================================

                            // ==================== 活力值兑换任务 ====================
                            if (taskType.equals("NORMAL_DRAW_EXCHANGE_VITALITY") && taskStatus.equals(TaskStatus.TODO.name())) {
                                String sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(
                                        activityId, sceneCode, source, taskSceneCode, taskType
                                );
                                if (ResChecker.checkRes(TAG + " 森林寻宝活力值兑换失败:", sginRes)) {
                                    Log.forest("森林寻宝🧾：" + taskName);
                                    doublecheck = true;
                                }
                                continue; // 防止进入下面的 FOREST_NORMAL_DRAW 分支
                            }
                            // =====================================================

                            // 统一处理 FOREST_NORMAL_DRAW 开头任务
                            if (taskType.startsWith("FOREST_NORMAL_DRAW") && taskStatus.equals(TaskStatus.TODO.name())) {
                                Log.record("任务延时30S模拟：" + taskName);
                                GlobalThreadPools.sleep(30 * 1000L);

                                // 调用对应完成接口
                                String result;
                                if (taskType.contains("XLIGHT")) {
                                    result = AntForestRpcCall.finishTask4Chouchoule(taskType, taskSceneCode);
                                } else {
                                    result = AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode);
                                }

                                if (ResChecker.checkRes(TAG, result)) {
                                    Log.forest("森林寻宝🧾：" + taskName);
                                    doublecheck = true;
                                } else {
                                    // 失败计数（不会自动屏蔽）
                                    taskTryCount.computeIfAbsent(taskType, k -> new AtomicInteger(0)).incrementAndGet();
                                }
                            }

                            // 已完成任务领取奖励
                            if (taskStatus.equals(TaskStatus.FINISHED.name())) {
                                Log.record("奖励延时3S:" + taskName);
                                GlobalThreadPools.sleep(3000L);
                                String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType);
                                if (ResChecker.checkRes(TAG, sginRes)) {
                                    Log.forest("森林寻宝🧾：" + taskName);
                                    if (rightsTimesLimit - rightsTimes > 0) {
                                        doublecheck = true;
                                    }
                                }
                            }
                        }
                    }
                }
            } while (doublecheck && ++loopCount < MAX_LOOP);

            // ==================== 执行抽奖 ====================
            jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (ResChecker.checkRes(TAG, jo)) {
                drawScene = jo.getJSONObject("drawScene");
                drawActivity = drawScene.getJSONObject("drawActivity");
                activityId = drawActivity.getString("activityId");
                sceneCode = drawActivity.getString("sceneCode");

                JSONObject drawAsset = jo.getJSONObject("drawAsset");
                int blance = drawAsset.optInt("blance", 0);
                while (blance > 0) {
                    jo = new JSONObject(AntForestRpcCall.drawopengreen(activityId, sceneCode, source, UserMap.getCurrentUid()));
                    if (ResChecker.checkRes(TAG, jo)) {
                        drawAsset = jo.getJSONObject("drawAsset");
                        blance = drawAsset.getInt("blance");
                        JSONObject prizeVO = jo.getJSONObject("prizeVO");
                        String prizeName = prizeVO.getString("prizeName");
                        int prizeNum = prizeVO.getInt("prizeNum");
                        Log.forest("森林寻宝🎁[领取: " + prizeName + "*" + prizeNum + "]");
                    }
                }
            }
            // ==============================================

        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    */

    void chouChouLe() {
        try {
            // 处理普通森林抽抽乐
            processChouChouLeActivity("普通森林抽抽乐", 
                "2025060301", 
                "ANTFOREST_NORMAL_DRAW", 
                "ANTFOREST_NORMAL_DRAW_TASK",
                "FOREST_NORMAL_DRAW");
            
            // 处理千里江山图活动
            processChouChouLeActivity("千里江山图", 
                "20251024", 
                "ANTFOREST_ACTIVITY_DRAW", 
                "ANTFOREST_ACTIVITY_DRAW_TASK",
                "FOREST_ACTIVITY_DRAW");
                
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 处理单个抽抽乐活动
     */
    void processChouChouLeActivity(String activityName, String activityId, String sceneCode, String taskSceneCode, String taskPrefix) {
        try {
            boolean doublecheck;
            String source = "task_entry";

            // ==================== 手动屏蔽任务集合 ====================
            Set<String> presetBad = new LinkedHashSet<>();
            if ("普通森林抽抽乐".equals(activityName)) {
                presetBad.add("FOREST_NORMAL_DRAW_SHARE");  // 邀请好友任务（屏蔽）
            } else if ("千里江山图".equals(activityName)) {
                presetBad.add("FOREST_ACTIVITY_DRAW_SHARE");  // 千里江山图邀请好友任务（屏蔽）
            }
            // =====================================================

            // 根据活动类型选择对应的进入方法
            JSONObject jo;
            if ("千里江山图".equals(activityName)) {
                jo = new JSONObject(AntForestRpcCall.enterDrawActivityQianli(source));
            } else {
                jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            }
            
            if (!ResChecker.checkRes(TAG, jo)) return;

            JSONObject drawScene = jo.getJSONObject("drawScene");
            JSONObject drawActivity = drawScene.getJSONObject("drawActivity");
            String currentActivityId = drawActivity.getString("activityId");
            String currentSceneCode = drawActivity.getString("sceneCode");
            String currentTaskSceneCode = currentSceneCode + "_TASK";

            long startTime = drawActivity.getLong("startTime");
            long endTime = drawActivity.getLong("endTime");

            int loopCount = 0;           // 循环次数计数
            final int MAX_LOOP = 5;      // 最大循环次数，避免死循环

            do {
                doublecheck = false;
                if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {
                    Log.record(activityName + "延时1S");
                    GlobalThreadPools.sleep(1000L);

                    // 根据活动类型选择对应的任务列表方法
                    JSONObject listTaskopengreen;
                    if ("千里江山图".equals(activityName)) {
                        listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskQianli(currentTaskSceneCode, source));
                    } else {
                        listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskopengreen(currentActivityId, currentTaskSceneCode, source));
                    }
                    
                    if (ResChecker.checkRes(TAG, listTaskopengreen)) {
                        JSONArray taskList = listTaskopengreen.getJSONArray("taskInfoList");

                        for (int i = 0; i < taskList.length(); i++) {
                            JSONObject taskInfo = taskList.getJSONObject(i);
                            JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                            JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                            String taskName = bizInfo.getString("title");
                            String taskScene = taskBaseInfo.getString("sceneCode");
                            String taskStatus = taskBaseInfo.getString("taskStatus");
                            String taskType = taskBaseInfo.getString("taskType");

                            JSONObject taskRights = taskInfo.getJSONObject("taskRights");
                            int rightsTimes = taskRights.getInt("rightsTimes");
                            int rightsTimesLimit = taskRights.getInt("rightsTimesLimit");

                            // ==================== 屏蔽逻辑 ====================
                            if (presetBad.contains(taskType)) {
                                Log.record(activityName + "已屏蔽任务，跳过：" + taskName);
                                continue;
                            }
                            // ==============================================

                            // ==================== 活力值兑换任务 ====================
                            if (taskType.equals("NORMAL_DRAW_EXCHANGE_VITALITY") && taskStatus.equals(TaskStatus.TODO.name())) {
                                String sginRes;
                                if ("千里江山图".equals(activityName)) {
                                    sginRes = AntForestRpcCall.exchangeTimesFromTaskQianli(
                                            currentActivityId, currentSceneCode, source, taskScene, taskType
                                    );
                                } else {
                                    sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(
                                            currentActivityId, currentSceneCode, source, taskScene, taskType
                                    );
                                }
                                if (ResChecker.checkRes(TAG + " " + activityName + "活力值兑换失败:", sginRes)) {
                                    Log.forest(activityName + "🧾：" + taskName);
                                    doublecheck = true;
                                }
                                continue;
                            }
                            // =====================================================

                            // 统一处理对应前缀的任务
                            if (taskType.startsWith(taskPrefix) && taskStatus.equals(TaskStatus.TODO.name())) {
                                Log.record(activityName + "任务延时30S模拟：" + taskName);
                                GlobalThreadPools.sleep(30 * 1000L);

                                // 调用对应完成接口
                                String result;
                                if (taskType.contains("XLIGHT")) {
                                    if ("千里江山图".equals(activityName)) {
                                        result = AntForestRpcCall.finishTask4Qianli(taskType, taskScene);
                                    } else {
                                        result = AntForestRpcCall.finishTask4Chouchoule(taskType, taskScene);
                                    }
                                } else {
                                    if ("千里江山图".equals(activityName)) {
                                        result = AntForestRpcCall.finishTaskQianli(taskType, taskScene);
                                    } else {
                                        result = AntForestRpcCall.finishTaskopengreen(taskType, taskScene);
                                    }
                                }

                                if (ResChecker.checkRes(TAG, result)) {
                                    Log.forest(activityName + "🧾：" + taskName);
                                    doublecheck = true;
                                } else {
                                    // 失败计数
                                    taskTryCount.computeIfAbsent(taskType, k -> new AtomicInteger(0)).incrementAndGet();
                                }
                            }

                            // 已完成任务领取奖励
                            if (taskStatus.equals(TaskStatus.FINISHED.name())) {
                                Log.record(activityName + "奖励延时3S:" + taskName);
                                GlobalThreadPools.sleep(3000L);
                                String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskScene, taskType);
                                if (ResChecker.checkRes(TAG, sginRes)) {
                                    Log.forest(activityName + "🧾：" + taskName);
                                    if (rightsTimesLimit - rightsTimes > 0) {
                                        doublecheck = true;
                                    }
                                }
                            }
                        }
                    }
                }
            } while (doublecheck && ++loopCount < MAX_LOOP);

            // ==================== 执行抽奖 ====================
            if ("千里江山图".equals(activityName)) {
                jo = new JSONObject(AntForestRpcCall.enterDrawActivityQianli(source));
            } else {
                jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            }
            
            if (ResChecker.checkRes(TAG, jo)) {
                drawScene = jo.getJSONObject("drawScene");
                drawActivity = drawScene.getJSONObject("drawActivity");
                currentActivityId = drawActivity.getString("activityId");
                currentSceneCode = drawActivity.getString("sceneCode");

                JSONObject drawAsset = jo.getJSONObject("drawAsset");
                int blance = drawAsset.optInt("blance", 0);
                while (blance > 0) {
                    JSONObject drawResult;
                    if ("千里江山图".equals(activityName)) {
                        drawResult = new JSONObject(AntForestRpcCall.drawQianli(currentActivityId, currentSceneCode, source, UserMap.getCurrentUid()));
                    } else {
                        drawResult = new JSONObject(AntForestRpcCall.drawopengreen(currentActivityId, currentSceneCode, source, UserMap.getCurrentUid()));
                    }
                    
                    if (ResChecker.checkRes(TAG, drawResult)) {
                        drawAsset = drawResult.getJSONObject("drawAsset");
                        blance = drawAsset.getInt("blance");
                        JSONObject prizeVO = drawResult.getJSONObject("prizeVO");
                        String prizeName = prizeVO.getString("prizeName");
                        int prizeNum = prizeVO.getInt("prizeNum");
                        Log.forest(activityName + "🎁[领取: " + prizeName + "*" + prizeNum + "]");
                    }
                }
            }
            // ==============================================

        } catch (Exception e) {
            Log.printStackTrace(e);
            Log.record(activityName + "处理异常: " + e.getMessage());
        }
    }


}
