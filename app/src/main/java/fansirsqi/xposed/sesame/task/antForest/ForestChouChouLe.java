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

    void chouChouLe() {
        try {
            // 处理普通森林抽抽乐
            processChouChouLeActivity("普通森林抽抽乐", 
                "2025112701", 
                "ANTFOREST_NORMAL_DRAW", 
                "ANTFOREST_NORMAL_DRAW_TASK",
                "FOREST_NORMAL_DRAW");
            
            // 处理活动森林抽抽乐
            processChouChouLeActivity("活动森林抽抽乐", 
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
                presetBad.add("FOREST_NORMAL_DRAW_QYJZFM_ZH");  //【限时】玩游戏得2次机会机会（屏蔽）
                presetBad.add("FOREST_NORMAL_DRAW_LJZC_ZH");  //【限时】玩游戏得2次机会机会（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_SQYT");   //逛逛神奇鱼塘（屏蔽）
            } else if ("活动森林抽抽乐".equals(activityName)) {
                presetBad.add("FOREST_ACTIVITY_DRAW_SHARE"); // 邀请好友任务（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_XSSLXCC");  //【限时】玩游戏得新机会（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_XSSLLXX");  //【限时】玩游戏得新机会（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_KDQB_ZH");  //【限时】玩游戏得2次机会机会（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_BWXRK_ZH");  //【限时】玩游戏得2次机会机会（屏蔽）
                presetBad.add("FOREST_ACTIVITY_DRAW_SQYT");   //逛逛神奇鱼塘（屏蔽）
            }
            // =====================================================

            // 根据活动类型选择对应的进入方法
            JSONObject jo;
            if ("活动森林抽抽乐".equals(activityName)) {
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
                    if ("活动森林抽抽乐".equals(activityName)) {
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
                                if ("活动森林抽抽乐".equals(activityName)) {
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
                                    if ("活动森林抽抽乐".equals(activityName)) {
                                        result = AntForestRpcCall.finishTask4Qianli(taskType, taskScene);
                                    } else {
                                        result = AntForestRpcCall.finishTask4Chouchoule(taskType, taskScene);
                                    }
                                } else {
                                    if ("活动森林抽抽乐".equals(activityName)) {
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
            if ("活动森林抽抽乐".equals(activityName)) {
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
                    if ("活动森林抽抽乐".equals(activityName)) {
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
