package fansirsqi.xposed.sesame.task.antMember;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import fansirsqi.xposed.sesame.entity.MemberBenefit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.TimeCounter;

public class AntMember extends ModelTask {
  private static final String TAG = AntMember.class.getSimpleName();
  @Override
  public String getName() {
    return "会员";
  }
  @Override
  public ModelGroup getGroup() {
    return ModelGroup.MEMBER;
  }
  @Override
  public String getIcon() {
    return "AntMember.png";
  }
  private BooleanModelField memberSign;
  private BooleanModelField memberTask;
  private BooleanModelField memberPointExchangeBenefit;
  private SelectModelField memberPointExchangeBenefitList;
  private BooleanModelField collectSesame;
  private BooleanModelField collectSesameWithOneClick;
  private BooleanModelField sesameTask;
  private BooleanModelField collectInsuredGold;
  private BooleanModelField enableGoldTicket;
  private BooleanModelField enableGameCenter;
  private BooleanModelField merchantSign;
  private BooleanModelField merchantKmdk;
  private BooleanModelField merchantMoreTask;
  private BooleanModelField beanSignIn;
  private BooleanModelField beanExchangeBubbleBoost;
  private BooleanModelField sesameAlchemyTask;
  private BooleanModelField doSesameAlchemy;
  // 新增：芝麻树开关
  private BooleanModelField sesameTreeTask;
  private BooleanModelField purifySesameTree;
  private BooleanModelField sesameCreditScoreTask; // 新增：芝麻信用攒进度开关

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberTask = new BooleanModelField("memberTask", "会员任务", false));
    modelFields.addField(memberPointExchangeBenefit = new BooleanModelField("memberPointExchangeBenefit", "会员积分 | 兑换权益", false));
    modelFields.addField(memberPointExchangeBenefitList = new SelectModelField("memberPointExchangeBenefitList", "会员积分 | 权益列表", new LinkedHashSet<>(), MemberBenefit.Companion.getList()));
    // 在此处添加攒芝麻分进度开关
    modelFields.addField(sesameCreditScoreTask = new BooleanModelField("sesameCreditScoreTask", "芝麻信用 | 攒芝麻分进度", false));
    modelFields.addField(sesameTask = new BooleanModelField("sesameTask", "芝麻信用 | 芝麻粒信用任务", false));
    modelFields.addField(collectSesame = new BooleanModelField("collectSesame", "芝麻信用 | 芝麻粒领取", false));
    modelFields.addField(collectSesameWithOneClick = new BooleanModelField("collectSesameWithOneClick", "芝麻信用 | 芝麻粒领取使用一键收取", false));
    modelFields.addField(sesameAlchemyTask = new BooleanModelField("sesameAlchemyTask", "芝麻炼金 | 攒粒", false));
    modelFields.addField(doSesameAlchemy = new BooleanModelField("doSesameAlchemy", "芝麻炼金 | 炼金", false));
    // 新增：芝麻树开关
    modelFields.addField(sesameTreeTask = new BooleanModelField("sesameTreeTask", "芝麻树 | 攒净化值", false));
    modelFields.addField(purifySesameTree = new BooleanModelField("purifySesameTree", "芝麻树 | 净化芝麻树", false));
    modelFields.addField(collectInsuredGold = new BooleanModelField("collectInsuredGold", "蚂蚁保 | 保障金领取", false));
    modelFields.addField(enableGoldTicket = new BooleanModelField("enableGoldTicket", "黄金票签到", false));
    modelFields.addField(enableGameCenter = new BooleanModelField("enableGameCenter", "游戏中心签到", false));
    modelFields.addField(merchantSign = new BooleanModelField("merchantSign", "商家服务 | 签到", false));
    modelFields.addField(merchantKmdk = new BooleanModelField("merchantKmdk", "商家服务 | 开门打卡", false));
    modelFields.addField(merchantMoreTask = new BooleanModelField("merchantMoreTask", "商家服务 | 积分任务", false));
    modelFields.addField(beanSignIn = new BooleanModelField("beanSignIn", "安心豆签到", false));
    modelFields.addField(beanExchangeBubbleBoost = new BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false));
    return modelFields;
  }
  @Override
  public Boolean check() {
    if (TaskCommon.IS_ENERGY_TIME){
      Log.record(TAG,"⏸ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
      return false;
    }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
      Log.record(TAG,"💤 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
      return false;
    } else {
      return true;
    }
  }
  @Override
  public void run() {
    try {
      TimeCounter tc = new TimeCounter(TAG);
      Log.record(TAG,"执行开始-" + getName());
      if (memberSign.getValue()) {
        doMemberSign();
        tc.countDebug("会员签到");
      }
      if (memberTask.getValue()) {
        doAllMemberAvailableTask();
        tc.countDebug("会员任务");
      }
      if (memberPointExchangeBenefit.getValue()) {
        memberPointExchangeBenefit();
        tc.countDebug("会员积分 | 兑换权益");
      }
      // 新增的功能调用逻辑
      if (sesameCreditScoreTask.getValue()) {
        if (checkSesameCanRun()) {
          doSesameCreditScoreTask();
          tc.countDebug("芝麻信用 | 攒芝麻分进度");
        }
      }
      if ((sesameTask.getValue() || collectSesame.getValue()) && checkSesameCanRun()) {
        if (sesameTask.getValue()) {
          doAllAvailableSesameTask();
          tc.countDebug("芝麻信用|芝麻粒信用任务");
        }
        if (collectSesame.getValue()) {
          collectSesame(collectSesameWithOneClick.getValue());
          tc.countDebug("芝麻信用|芝麻粒领取");
        }
      }
      if (sesameAlchemyTask.getValue()) {
        doSesameAlchemyTasks();
        tc.countDebug("芝麻炼金|攒粒");
      }
      if (doSesameAlchemy.getValue()) {
        doSesameAlchemy();
        tc.countDebug("芝麻炼金|炼金");
      }
      // 新增：芝麻树任务执行逻辑
      if (sesameTreeTask.getValue() || purifySesameTree.getValue()) {
        if (checkSesameCanRun()) {
          handleSesameTree();
          tc.countDebug("芝麻树");
        }
      }
      if (collectInsuredGold.getValue()) {
        collectInsuredGold();
        tc.countDebug("蚂蚁保|保障金领取");
      }
      if (enableGoldTicket.getValue()) {
        goldTicket();
        tc.countDebug("黄金票签到");
      }
      if (enableGameCenter.getValue()) {
        enableGameCenter();
        tc.countDebug("游戏中心签到");
      }
      if (beanSignIn.getValue()) {
        beanSignIn();
        tc.countDebug("安心豆签到");
      }
      if (beanExchangeBubbleBoost.getValue()) {
        beanExchangeBubbleBoost();
        tc.countDebug("安心豆兑换时光加速器");
      }
      if (merchantSign.getValue() || merchantKmdk.getValue() || merchantMoreTask.getValue()) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.transcodeCheck());
        if (!jo.optBoolean("success")) {
          return;
        }
        JSONObject data = jo.getJSONObject("data");
        if (!data.optBoolean("isOpened")) {
          Log.record(TAG,"商家服务👪未开通");
          return;
        }
        if (merchantKmdk.getValue()) {
          if (TimeUtil.isNowAfterTimeStr("0600") && TimeUtil.isNowBeforeTimeStr("1200")) {
            kmdkSignIn();
            tc.countDebug("商家服务|开门打卡");
          }
          kmdkSignUp();
          tc.countDebug("开门打卡报名");
        }
        if (merchantSign.getValue()) {
          doMerchantSign();
          tc.countDebug("商家服务|签到");
        }
        if (merchantMoreTask.getValue()) {
          doMerchantMoreTask();
          tc.countDebug("商家服务|积分任务");
        }
      }
      tc.stop();
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }finally {
      Log.record(TAG,"执行结束-" + getName());
    }
  }

  /**
   * 会员积分0元兑，权益道具兑换
   */
  private void memberPointExchangeBenefit() {
    try {
      String userId = UserMap.getCurrentUid();
      JSONObject memberInfo = new JSONObject(AntMemberRpcCall.queryMemberInfo());
      if (!ResChecker.checkRes(TAG, memberInfo)) {
        return;
      }
      String pointBalance = memberInfo.getString("pointBalance");
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryShandieEntityList(userId, pointBalance));
      if (!ResChecker.checkRes(TAG, jo)) {
        return;
      }
      if (!jo.has("benefits")) {
        Log.record(TAG,"会员积分[未找到可兑换权益]");
        return;
      }
      JSONArray benefits = jo.getJSONArray("benefits");
      for (int i = 0; i < benefits.length(); i++) {
        JSONObject benefitInfo = benefits.getJSONObject(i);
        JSONObject pricePresentation = benefitInfo.getJSONObject("pricePresentation");
        String name = benefitInfo.getString("name");
        String benefitId = benefitInfo.getString("benefitId");
        IdMapManager.getInstance(MemberBenefitsMap.class).add(benefitId, name);
        if (!Status.canMemberPointExchangeBenefitToday(benefitId)
                || !memberPointExchangeBenefitList.getValue().contains(benefitId)) {
          continue;
        }
        String itemId = benefitInfo.getString("itemId");
        if (exchangeBenefit(benefitId, itemId)) {
          String point = pricePresentation.getString("point");
          Log.other("会员积分🎐兑换[" + name + "]#花费[" + point + "积分]");
        } else {
          Log.other("会员积分🎐兑换[" + name + "]失败！");
        }
      }
      IdMapManager.getInstance(MemberBenefitsMap.class).save(userId);
    } catch (JSONException e) {
      Log.record(TAG,"JSON解析错误: " + e.getMessage());
      Log.printStackTrace(TAG, e);
    } catch (Throwable t) {
      Log.runtime(TAG, "memberPointExchangeBenefit err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private Boolean exchangeBenefit(String benefitId, String itemId) {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.exchangeBenefit(benefitId, itemId));
      if (ResChecker.checkRes(TAG + "会员权益兑换失败:", jo)) {
        Status.memberPointExchangeBenefitToday(benefitId);
        return true;
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "exchangeBenefit err:");
      Log.printStackTrace(TAG, t);
    }
    return false;
  }

  /**
   * 会员签到
   */
  private void doMemberSign() {
    try {
      if (Status.canMemberSignInToday(UserMap.getCurrentUid())) {
        String s = AntMemberRpcCall.queryMemberSigninCalendar();
        // GlobalThreadPools.sleep(500);
        JSONObject jo = new JSONObject(s);
        if (ResChecker.checkRes(TAG + "会员签到失败:", jo)) {
          Log.other("会员签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString("signinSumDay") + "天");
          Status.memberSignInToday(UserMap.getCurrentUid());
        } else {
          Log.record(jo.getString("resultDesc"));
          Log.runtime(s);
        }
      }
      queryPointCert(1, 8);
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 会员任务-逛一逛
   * 单次执行 1
   */
  private void doAllMemberAvailableTask() {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      // GlobalThreadPools.sleep(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResChecker.checkRes(TAG, jsonObject)) {
        Log.error(TAG + ".doAllMemberAvailableTask", "会员任务响应失败: " + jsonObject.getString("resultDesc"));
        return;
      }
      if (!jsonObject.has("availableTaskList")) {
        return;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      for (int j = 0; j < taskList.length(); j++) {
        JSONObject task = taskList.getJSONObject(j);
        processTask(task);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "doAllMemberAvailableTask err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 会员积分收取
   * @param page 第几页
   * @param pageSize 每页数据条数
   */
  private static void queryPointCert(int page, int pageSize) {
    try {
      String s = AntMemberRpcCall.queryPointCert(page, pageSize);
      // GlobalThreadPools.sleep(500);
      JSONObject jo = new JSONObject(s);
              if (ResChecker.checkRes(TAG + "查询会员积分证书失败:", jo)) {
          boolean hasNextPage = jo.getBoolean("hasNextPage");
        JSONArray jaCertList = jo.getJSONArray("certList");
        for (int i = 0; i < jaCertList.length(); i++) {
          jo = jaCertList.getJSONObject(i);
          String bizTitle = jo.getString("bizTitle");
          String id = jo.getString("id");
          int pointAmount = jo.getInt("pointAmount");
          s = AntMemberRpcCall.receivePointByUser(id);
          jo = new JSONObject(s);
          if (ResChecker.checkRes(TAG + "会员积分领取失败:", jo)) {
            Log.other("会员积分🎖️[领取" + bizTitle + "]#" + pointAmount + "积分");
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(s);
          }
        }
        if (hasNextPage) {
          queryPointCert(page + 1, pageSize);
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "queryPointCert err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 执行芝麻信用攒进度的主方法（已修复方法名调用错误）
   */
  private void doSesameCreditScoreTask() {
      try {
          Log.record(TAG, "芝麻信用攒进度-开始执行任务...");

          // 步骤1: 查询初始任务列表
          String toDoListStr = AntMemberRpcCall.queryGrowthBehaviorToDoList(); // <--- 已修正
          JSONObject toDoListJo = new JSONObject(toDoListStr);

          if (!toDoListJo.optBoolean("success")) {
              Log.record(TAG, "芝麻信用攒进度-查询任务列表失败: " + toDoListJo.optString("resultView"));
              return;
          }

          JSONArray tasks = toDoListJo.optJSONArray("toDoList");
          if (tasks == null || tasks.length() == 0) {
              Log.record(TAG, "芝麻信用攒进度-没有发现可执行的任务");
          } else {
              // 步骤2: 接受所有“待领取”的公益任务
              List<String> behaviorIdsToOpen = Arrays.asList("mayisenlin_7d", "babanongchang_7d", "mayizhuangyuan_7d");
              for (int i = 0; i < tasks.length(); i++) {
                  JSONObject task = tasks.getJSONObject(i);
                  String behaviorId = task.optString("behaviorId");
                  String status = task.optString("status");
                  String title = task.optString("title", behaviorId);

                  if (behaviorIdsToOpen.contains(behaviorId) && "wait_receive".equals(status)) {
                      Log.record(TAG, "芝麻信用攒进度-开始接受任务: " + title);
                      String openResultStr = AntMemberRpcCall.openBehaviorCollect(behaviorId);
                      JSONObject openResultJo = new JSONObject(openResultStr);
                      if (openResultJo.optBoolean("success")) {
                          Log.record(TAG, "芝麻信用攒进度-接受任务[" + title + "]成功");
                      } else {
                          Log.record(TAG, "芝麻信用攒进度-接受任务[" + title + "]失败: " + openResultJo.optString("resultView"));
                      }
                      GlobalThreadPools.sleep(2000); // 等待任务状态更新
                  }
              }
          }

          // 步骤3: 重新查询任务列表，执行“待完成”的任务（如每日答题）
          toDoListStr = AntMemberRpcCall.queryGrowthBehaviorToDoList(); // <--- 已修正
          toDoListJo = new JSONObject(toDoListStr);
          tasks = toDoListJo.optJSONArray("toDoList");
          if (tasks != null) {
              for (int i = 0; i < tasks.length(); i++) {
                  JSONObject task = tasks.getJSONObject(i);
                  String behaviorId = task.optString("behaviorId");
                  String status = task.optString("status");

                  if ("meiriwenda".equals(behaviorId) && "wait_doing".equals(status)) {
                      handleDailyQuiz(); // 执行每日答题
                      GlobalThreadPools.sleep(2000); // 等待答题结果处理
                      collectTaskProgress(behaviorId); // 答题后立刻尝试领取进度
                      break; 
                  }
              }
          }
          
          // 步骤4: 一键领取所有剩余的进度球（作为补充和最终确认）
          Log.record(TAG, "芝麻信用攒进度-等待2秒后开始一键领取剩余进度...");
          GlobalThreadPools.sleep(2000);

          String progressStr = AntMemberRpcCall.queryScoreProgress();
          JSONObject progressJo = new JSONObject(progressStr);
          if (progressJo.optBoolean("success")) {
              JSONObject totalWaitVO = progressJo.optJSONObject("totalWaitProcessVO");
              if (totalWaitVO != null) {
                  JSONArray ballIdList = totalWaitVO.optJSONArray("totalProgressIdList");
                  if (ballIdList != null && ballIdList.length() > 0) {
                      Log.record(TAG, "芝麻信用攒进度-发现 " + ballIdList.length() + " 个可一键领取的进度球");
                      String collectResultStr = AntMemberRpcCall.collectProgressBall(ballIdList);
                      JSONObject collectResultJo = new JSONObject(collectResultStr);
                      if (collectResultJo.optBoolean("success")) {
                          String collectedProgress = collectResultJo.optString("collectedProgress", "?");
                          String totalProgress = collectResultJo.optString("totalProgress", "?");
                          Log.other("芝麻信用攒进度-一键领取成功! 本次领取 " + collectedProgress + "%，当前总进度 " + totalProgress + "%");
                      } else {
                          Log.record(TAG, "芝麻信用攒进度-一键领取失败: " + collectResultJo.optString("resultView"));
                      }
                  } else {
                      Log.record(TAG, "芝麻信用攒进度-没有发现可一键领取的进度球");
                  }
              }
          } else {
              Log.record(TAG, "芝麻信用攒进度-查询进度球信息失败: " + progressJo.optString("resultView"));
          }

          // 步骤5: 最后检查是否所有任务都已完成，如果是，则关闭开关
          GlobalThreadPools.sleep(2000);
          progressStr = AntMemberRpcCall.queryScoreProgress();
          progressJo = new JSONObject(progressStr);
          if (progressJo.optBoolean("success")) {
              JSONObject totalWaitVO = progressJo.optJSONObject("totalWaitProcessVO");
              if (totalWaitVO != null && totalWaitVO.optInt("totalCollectProcess", -1) == 0) {
                  // sesameCreditScoreTask.setValue(false); // 取消注释以启用自动关闭开关功能
                  Log.record(TAG, "芝麻信用攒进度-所有任务和进度已完成，可临时关闭开关");
              }
          }

      } catch (Throwable t) {
          Log.printStackTrace(TAG, t);
      }
  }

  /**
   * 新增：根据任务ID，查询并收取该任务产生的进度球
   * @param behaviorId 要收取进度的任务ID
   */
  private void collectTaskProgress(String behaviorId) {
      try {
          Log.record(TAG, "芝麻信用攒进度-检查任务["+ behaviorId +"]是否产生可领取的进度...");
          String toDoListStr = AntMemberRpcCall.queryGrowthBehaviorToDoList(); // <--- 已修正
          JSONObject toDoListJo = new JSONObject(toDoListStr);
          JSONArray tasks = toDoListJo.optJSONArray("toDoList");

          if (tasks == null) return;

          for (int i = 0; i < tasks.length(); i++) {
              JSONObject task = tasks.getJSONObject(i);
              if (behaviorId.equals(task.optString("behaviorId")) && "wait_collect".equals(task.optString("status"))) {
                  JSONObject scoreAwardVO = task.optJSONObject("scoreAwardVO");
                  if (scoreAwardVO != null) {
                      JSONArray awardIdList = scoreAwardVO.optJSONArray("awardIdList");
                      if (awardIdList != null && awardIdList.length() > 0) {
                          Log.record(TAG, "芝麻信用攒进度-开始领取任务["+ task.optString("title") +"]的进度球");
                          String collectResultStr = AntMemberRpcCall.collectProgressBall(awardIdList);
                          JSONObject collectResultJo = new JSONObject(collectResultStr);
                          if(collectResultJo.optBoolean("success")) {
                              String collectedProgress = collectResultJo.optString("collectedProgress", "?");
                              String totalProgress = collectResultJo.optString("totalProgress", "?");
                              Log.other("芝麻信用攒进度-领取["+ task.optString("title") +"]成功! 本次领取 " + collectedProgress + "%，当前总进度 " + totalProgress + "%");
                          } else {
                              Log.record(TAG, "芝麻信用攒进度-领取["+ task.optString("title") +"]失败: " + collectResultJo.optString("resultView"));
                          }
                          return; // 领取后退出
                      }
                  }
              }
          }
      } catch (Throwable t) {
          Log.printStackTrace(TAG, t);
      }
  }

  /**
   * 处理每日答题的逻辑（逻辑未变，根据新日志仍然有效）
   */
  private void handleDailyQuiz() {
      try {
          Log.record(TAG, "芝麻信用攒进度-开始处理每日答题");
          String quizStr = AntMemberRpcCall.queryDailyQuiz();
          JSONObject quizJo = new JSONObject(quizStr);

          if (!quizJo.optBoolean("success")) {
              Log.record(TAG, "芝麻信用攒进度-获取题目失败: " + quizJo.optString("resultView"));
              return;
          }

          JSONObject data = quizJo.getJSONObject("data");
          JSONObject questionVo = data.getJSONObject("questionVo");
          String questionId = questionVo.getString("questionId");
          String questionContent = questionVo.getString("questionContent");

          // 新版接口直接返回了rightAnswer，可以直接使用
          JSONObject rightAnswer = questionVo.optJSONObject("rightAnswer");
          if (rightAnswer == null) {
              Log.record(TAG, "芝麻信用攒进度-未找到正确答案，跳过答题");
              return; // 未找到答案则跳过
          }
          
          String answerId = rightAnswer.getString("answerId");
          String answerContent = rightAnswer.getString("answerContent");
          Log.record(TAG, "芝麻信用攒进度-已获取到正确答案: " + answerContent);
          
          long bizDate = data.getLong("bizDate");
          String pushResultStr = AntMemberRpcCall.pushDailyQuizAnswer(bizDate, questionId, answerId);
          JSONObject pushResultJo = new JSONObject(pushResultStr);

          if (pushResultJo.optBoolean("success")) {
              Log.other("芝麻信用攒进度-答题成功: " + questionContent + " -> " + answerContent);
          } else {
              Log.record(TAG, "芝麻信用攒进度-答题失败: " + pushResultJo.optString("resultView"));
          }

      } catch (Throwable t) {
          Log.printStackTrace(TAG, t);
      }
  }

  /**
   * 检查是否满足运行芝麻信用任务的条件
   * @return bool
   */
  private static Boolean checkSesameCanRun() {
    try {
      String s = AntMemberRpcCall.queryHome();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.other(TAG, "芝麻信用💳[首页响应失败]#" + jo.optString("errorMsg"));
        Log.error(TAG + ".checkSesameCanRun.queryHome", "芝麻信用💳[首页响应失败]#" + s);
        return false;
      }
      JSONObject entrance = jo.getJSONObject("entrance");
      if (!entrance.optBoolean("openApp")) {
        Log.other("芝麻信用💳[未开通芝麻信用]");
        return false;
      }
      return true;
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".checkSesameCanRun", t);
      return false;
    }
  }

  /**
   * 芝麻信用任务 - 重构版本
   * 实现了任务完成后自动关闭开关的逻辑
   */
  private void doAllAvailableSesameTask() {
    try {
      // 首先执行每日签到
      sesameCheckIn();
      GlobalThreadPools.sleep(500);

      // 查询任务列表
      String s = AntMemberRpcCall.queryAvailableSesameTask();
      JSONObject jo = new JSONObject(s);

      if (!jo.optBoolean("success")) {
        Log.record(TAG, "芝麻信用💳[查询任务响应失败]#" + jo.optString("resultView"));
        Log.error(TAG + ".doAllAvailableSesameTask", "芝麻信用💳[查询任务响应失败]#" + s);
        return;
      }

      JSONObject taskObj = jo.getJSONObject("data");
      JSONArray allTasks = new JSONArray();
      int availableTaskCount = 0;

      // 合并所有可能的任务列表到一个统一的列表中
      if (taskObj.has("toCompleteVOS")) {
        JSONArray tasks = taskObj.getJSONArray("toCompleteVOS");
        for(int i = 0; i < tasks.length(); i++) allTasks.put(tasks.getJSONObject(i));
      }
      if (taskObj.has("dailyTaskListVO")) {
        JSONObject dailyTaskListVO = taskObj.getJSONObject("dailyTaskListVO");
        if (dailyTaskListVO.has("waitJoinTaskVOS")) {
          JSONArray tasks = dailyTaskListVO.getJSONArray("waitJoinTaskVOS");
          for(int i = 0; i < tasks.length(); i++) allTasks.put(tasks.getJSONObject(i));
        }
        // waitCompleteTaskVOS 理论上和 toCompleteVOS 类似，为保险起见也加入
        if (dailyTaskListVO.has("waitCompleteTaskVOS")) {
          JSONArray tasks = dailyTaskListVO.getJSONArray("waitCompleteTaskVOS");
          for(int i = 0; i < tasks.length(); i++) allTasks.put(tasks.getJSONObject(i));
        }
      }

      // 统计所有可完成的任务数量
      for (int i = 0; i < allTasks.length(); i++) {
        JSONObject task = allTasks.getJSONObject(i);
        String taskTitle = task.optString("title", "未知任务");
        if (!task.optBoolean("finishFlag", false) 
          && !"已完成".equals(task.optString("actionText")) 
          && !isTaskInBlacklist(taskTitle)) {
          availableTaskCount++;
        }
      }
      
      Log.record(TAG, "芝麻信用💳[发现 " + allTasks.length() + " 个总任务，其中 " + availableTaskCount + " 个可完成]");

      if (availableTaskCount > 0) {
        int[] results = joinAndFinishSesameTaskWithResult(allTasks);
        Log.record(TAG, "芝麻信用💳[任务处理统计]#总尝试:" + allTasks.length() + "个, 成功:" + results[0] + "个, 跳过:" + results[1] + "个");
        // 如果还有未完成的任务，但本次没有成功完成任何一个，可能意味着逻辑卡住，也关闭开关避免死循环
        if(results[0] == 0 && (allTasks.length() - results[1] > 0)) {
          sesameTask.setValue(false);
          Log.record(TAG, "芝麻信用💳[未成功完成任何新任务，为避免卡死，开关临时关闭]");
          return;
        }
      }

      // 如果没有可做的任务了，临时关闭开关
      if (availableTaskCount == 0) {
        sesameTask.setValue(false);
        Log.record(TAG, "芝麻信用💳[已无更多可做任务，开关临时关闭]");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doAllAvailableSesameTask", t);
    }
  }
    
  /**
   * 不能完成的任务黑名单（根据title关键词匹配）
   */
  private static final String[] TASK_BLACKLIST = {
    "每日施肥领水果",           // 需要淘宝操作
    "坚持种水果",               // 需要淘宝操作  
    "坚持去玩休闲小游戏",        // 需要游戏操作
    "去AQapp提问",              // 需要下载APP
    "去AQ提问",                 // 需要下载APP
    "去AQApp对话一次",          // 需要下载APP
    "坚持看直播领福利",          // 需要淘宝直播
    "去淘金币逛一逛",            // 需要淘宝操作
    "浏览租赁商家小程序",        // 需要小程序操作
    "坚持攒保障金",
    "坚持攒保障",
    "芝麻租赁下单得芝麻粒",
    "订阅小组件",
    "邀请好友助力",
    "去玩小游戏",
    "雇佣芝麻大表鸽",
    "订阅炼金签到提醒",
    "订阅芝麻粒签到提醒",
    "开通淘宝先用后付",
    "坚持逛裹酱领福利",
    "芝麻租赁下单得芝麻粒",
    "去订阅芝麻小组件",
    "逛淘宝签到",              // 需要淘宝操作
    "坚持签到领奖励"            // 需要淘宝操作
  };

  /**
   * 检查任务是否在黑名单中
   * @param taskTitle 任务标题
   * @return true表示在黑名单中，应该跳过
   */
  private static boolean isTaskInBlacklist(String taskTitle) {
    if (taskTitle == null) return false;
    for (String blacklistItem : TASK_BLACKLIST) {
        if (taskTitle.contains(blacklistItem)) {
            return true;
        }
    }
    return false;
  }

  /**
   * 芝麻信用-每日签到任务
   * @throws JSONException JSON解析异常
   */
  private static void sesameCheckIn() throws JSONException {
    try {
      Log.record(TAG, "芝麻信用💳[开始检查签到领粒]");
      String queryResult = AntMemberRpcCall.checkInQueryTaskLists();
      JSONObject queryObj = new JSONObject(queryResult);

      if (!queryObj.optBoolean("success", false)) {
        Log.record(TAG, "芝麻信用💳[查询签到领粒状态失败]#" + queryResult);
        return;
      }

      JSONObject data = queryObj.optJSONObject("data");
      if (data == null || !data.has("currentDateCheckInTaskVO")) {
        Log.record(TAG, "芝麻信用💳[查询签到领粒状态响应格式错误]#" + queryResult);
        return;
      }

      JSONObject checkInTask = data.getJSONObject("currentDateCheckInTaskVO");
      String status = checkInTask.optString("status");

      if ("COMPLETED".equals(status)) {
        Log.record(TAG, "芝麻信用💳[今日签到领粒已完成]");
      } else if ("CAN_COMPLETE".equals(status) || "TODO".equals(status)) {
        Log.record(TAG, "芝麻信用💳[开始执行签到领粒操作]");
        String checkInDate = checkInTask.optString("checkInDate");

        String completeResult = AntMemberRpcCall.checkInCompleteTask(checkInDate);
        JSONObject completeObj = new JSONObject(completeResult);

        if (completeObj.optBoolean("success", false)) {
          JSONObject prizeData = completeObj.optJSONObject("data");
          if (prizeData != null && prizeData.has("prize")) {
              String num = prizeData.getJSONObject("prize").optString("num", "未知");
              Log.other("芝麻信用💳[签到领粒成功]#获得" + num + "粒");
          } else {
              Log.record(TAG, "芝麻信用💳[签到领粒成功]#" + completeObj.optString("resultView", "成功"));
          }
        } else {
          Log.record(TAG, "芝麻信用💳[签到领粒失败]#" + completeResult);
        }
      }
    } catch (Exception e) {
      Log.record(TAG, "芝麻信用💳[签到领粒任务出现异常]");
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 芝麻信用-领取并完成任务（重构版，智能判断是否需要Join）
   * @param taskList 任务列表
   * @return int数组 [完成数量, 跳过数量]
   * @throws JSONException JSON解析异常
   */
  private static int[] joinAndFinishSesameTaskWithResult(JSONArray taskList) throws JSONException {
    int completedCount = 0;
    int skippedCount = 0;

    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      String title = task.optString("title", "未知任务");

      // 1. 基本检查：是否已完成或在黑名单中
      if (task.optBoolean("finishFlag", false) || "已完成".equals(task.optString("actionText"))) {
        skippedCount++;
        continue;
      }
      if (isTaskInBlacklist(title)) {
        Log.record(TAG, "芝麻信用💳[跳过黑名单任务]#" + title);
        skippedCount++;
        continue;
      }

      String templateId = task.optString("templateId");
      if (templateId.isEmpty()) {
        Log.record(TAG, "芝麻信用💳[跳过缺少templateId任务]#" + title);
        skippedCount++;
        continue;
      }

      // 2. 获取recordId，如果不存在则尝试加入任务获取
      String recordId = task.optString("recordId");
      if (recordId.isEmpty()) {
        Log.record(TAG, "芝麻信用💳[任务 '" + title + "' 缺少recordId，尝试加入...]");
        String joinResult = AntMemberRpcCall.joinSesameTask(templateId);
        GlobalThreadPools.sleep(500);
        JSONObject joinResponse = new JSONObject(joinResult);
        if (joinResponse.optBoolean("success")) {
          recordId = joinResponse.getJSONObject("data").getString("recordId");
          Log.record(TAG, "芝麻信用💳[加入成功，获得recordId]");
        } else {
          Log.record(TAG, "芝麻信用💳[加入任务 '" + title + "' 失败]#" + joinResult);
          skippedCount++;
          continue;
        }
      }
      
      // 3. 任务反馈 (taskFeedback)
      AntMemberRpcCall.feedBackSesameTask(templateId);
      GlobalThreadPools.sleep(500);

      // 4. 模拟等待 (如果需要)
      int visitTime = task.optInt("vstTime", 0);
      if (visitTime > 0) {
        Log.record("芝麻信用💳[任务 '" + title + "']#模拟浏览" + (visitTime / 1000) + "秒...");
        GlobalThreadPools.sleep(visitTime);
      } else {
        GlobalThreadPools.sleep(2000); // 默认等待
      }

      // 5. 完成任务 (pushActivity)
      String finishResult = AntMemberRpcCall.finishSesameTask(recordId);
      JSONObject finishResponse = new JSONObject(finishResult);
      if (finishResponse.optBoolean("success")) {
        int reward = task.optInt("rewardAmount", 0);
        Log.other("芝麻信用💳[完成任务 '" + title + "']#获得 " + reward + " 芝麻粒");
        completedCount++;
      } else {
        Log.record(TAG, "芝麻信用💳[完成任务 '" + title + "' 失败]#" + finishResult);
        skippedCount++;
      }
      GlobalThreadPools.sleep(3000); // 任务间等待
    }
    return new int[]{completedCount, skippedCount};
  }

  /**
   * 芝麻粒收取
   * @param withOneClick 启用一键收取
   */
  private void collectSesame(Boolean withOneClick) {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryCreditFeedback());
      // GlobalThreadPools.sleep(500);
      if (!jo.optBoolean("success")) {
        Log.record(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
        Log.error(TAG + ".collectSesame.queryCreditFeedback", "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo);
        return;
      }
      JSONArray availableCollectList = jo.getJSONArray("creditFeedbackVOS");
      if (withOneClick) {
        // Log.record("延时2S 1");
        // GlobalThreadPools.sleep(2000);
        jo = new JSONObject(AntMemberRpcCall.collectAllCreditFeedback());
        // Log.record("延时2S 1");
        // GlobalThreadPools.sleep(2000);
        if (!jo.optBoolean("success")) {
          Log.record(TAG, "芝麻信用💳[一键收取芝麻粒响应失败]#" + jo);
          Log.error(TAG + ".collectSesame.collectAllCreditFeedback", "芝麻信用💳[一键收取芝麻粒响应失败]#" + jo);
          return;
        }
      }
      for (int i = 0; i < availableCollectList.length(); i++) {
        jo = availableCollectList.getJSONObject(i);
        if (!"UNCLAIMED".equals(jo.getString("status"))) {
          continue;
        }
        String title = jo.getString("title");
        String creditFeedbackId = jo.getString("creditFeedbackId");
        String potentialSize = jo.getString("potentialSize");
        if (!withOneClick) {
          jo = new JSONObject(AntMemberRpcCall.collectCreditFeedback(creditFeedbackId));
          Log.record("延时2S 3");
          GlobalThreadPools.sleep(2000);
          if (!jo.optBoolean("success")) {
            Log.record(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
            Log.error(TAG + ".collectSesame.collectCreditFeedback", "芝麻信用💳[收取芝麻粒响应失败]#" + jo);
            continue;
          }
        }
        Log.other("芝麻信用💳[" + title + "]#" + potentialSize + "粒" + (withOneClick ? "(一键收取)" : ""));
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectSesame", t);
    }
  }

  /**
   * [已完善] 芝麻炼金 - 攒粒 (签到、限时补贴和日常任务)
   */
  private void doSesameAlchemyTasks() {
    try {
      Log.record(TAG, "芝麻炼金-开始执行攒粒任务...");

      // 1. 执行签到
      String checkInListStr = AntMemberRpcCall.alchemyQueryCheckInTasks();
      JSONObject checkInListJo = new JSONObject(checkInListStr);
      if (checkInListJo.optBoolean("success")) {
        JSONObject taskData = checkInListJo.getJSONObject("data");
        if (taskData.has("currentDateCheckInTaskVO")) {
          JSONObject checkInTask = taskData.getJSONObject("currentDateCheckInTaskVO");
          if ("CAN_COMPLETE".equals(checkInTask.getString("status"))) {
            String currentDate = checkInTask.getString("checkInDate");
            String completeStr = AntMemberRpcCall.completeAlchemyCheckIn(currentDate);
            JSONObject completeJo = new JSONObject(completeStr);
            if (completeJo.optBoolean("success")) {
              String zmlNum = completeJo.getJSONObject("data").optString("zmlNum", "?");
              Log.other("芝麻炼金-攒粒✨[签到成功] #" + zmlNum + "粒");
            } else {
              Log.record(TAG, "芝麻炼金-攒粒✨[签到失败]: " + completeJo.optString("resultView"));
            }
          } else {
            Log.record(TAG, "芝麻炼金-攒粒✨[今日已签到]");
          }
        }
      }
      GlobalThreadPools.sleep(2000);

      // 2. 执行限时补贴 (早/中/晚饭)
      String timeLimitedTaskStr = AntMemberRpcCall.alchemyQueryTimeLimitedTask();
      JSONObject timeLimitedJo = new JSONObject(timeLimitedTaskStr);
      if (timeLimitedJo.optBoolean("success")) {
        JSONObject taskVo = timeLimitedJo.getJSONObject("data").getJSONObject("timeLimitedTaskVO");
        // state: 1 = 可领取, 2 = 未到时间, 3 = 已领取/已过期
        if (taskVo.getInt("state") == 1) {
          String templateId = taskVo.getString("templateId");
          String title = taskVo.getString("longTitle");
          String completeStr = AntMemberRpcCall.alchemyCompleteTimeLimitedTask(templateId);
          JSONObject completeJo = new JSONObject(completeStr);
          if (completeJo.optBoolean("success")) {
              String zmlNum = completeJo.getJSONObject("data").optString("zmlNum", "?");
              Log.other("芝麻炼金-攒粒✨[领取 " + title + " 成功] #" + zmlNum + "粒");
          } else {
              Log.record(TAG, "芝麻炼金-攒粒✨[领取 " + title + " 失败]: " + completeJo.optString("resultView"));
          }
        } else {
            String title = taskVo.getString("longTitle");
            Log.record(TAG, "芝麻炼金-攒粒✨[" + title + " 不可领取]");
        }
      }
      GlobalThreadPools.sleep(2000);

      // 3. 执行其他日常任务
      Log.record(TAG, "芝麻炼金-攒粒✨[开始处理其他日常任务]");
      String s = AntMemberRpcCall.alchemyQueryTasks();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.record(TAG, "芝麻炼金-攒粒✨[查询日常任务失败]: " + jo.optString("resultView"));
        return;
      }
      JSONArray toCompleteTasks = jo.getJSONObject("data").optJSONArray("toCompleteVOS");
      if (toCompleteTasks == null || toCompleteTasks.length() == 0) {
        Log.record(TAG, "芝麻炼金-攒粒✨[没有可做的日常任务]");
        sesameAlchemyTask.setValue(false);
        Log.record(TAG, "芝麻炼金 | 攒粒 [已全部完成，临时关闭]");
        return;
      }
      
      Log.record(TAG, "芝麻炼金-攒粒✨[发现 " + toCompleteTasks.length() + " 个日常任务]");

      // 复用现有的芝麻信用任务逻辑来完成
      int[] results = joinAndFinishSesameTaskWithResult(toCompleteTasks);
      // 为joinAndFinishSesameTaskWithResult方法添加第二个参数 'false'
      // int[] results = joinAndFinishSesameTaskWithResult(toCompleteTasks, false);

      // 如果完成过任务，再次检查是否还有剩余任务，如果没有则关闭开关
      if (results[0] > 0) { 
        GlobalThreadPools.sleep(3000); // 等待任务列表刷新
        s = AntMemberRpcCall.alchemyQueryTasks();
        jo = new JSONObject(s);
        toCompleteTasks = jo.optJSONObject("data").optJSONArray("toCompleteVOS");
      }
      if (toCompleteTasks == null || toCompleteTasks.length() == 0) {
        Log.record(TAG, "芝麻炼金-攒粒✨[所有日常任务已完成]");
        sesameAlchemyTask.setValue(false);
        Log.record(TAG, "芝麻炼金 | 攒粒 [已全部完成，临时关闭]");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * [已完善] 芝麻炼金 - 炼金
   */
  private void doSesameAlchemy() {
    try {
      Log.record(TAG, "芝麻炼金-开始执行炼金...");
      String homeStr = AntMemberRpcCall.alchemyQueryHome();
      JSONObject homeJo = new JSONObject(homeStr);

      if (!homeJo.optBoolean("success")) {
        Log.record(TAG, "芝麻炼金[获取炼金主页信息失败]: " + homeJo.optString("resultView"));
        return;
      }

      JSONObject data = homeJo.getJSONObject("data");
      int zmlBalance = data.getInt("zmlBalance");
      int alchemyCost = data.getInt("alchemyCostZml");
      int dailyCap = data.getInt("alchemyDailyCap");
      int finishedCount = data.getInt("finishAlchemyCount");

      if (finishedCount >= dailyCap) {
        Log.record(TAG, "芝麻炼金[今日炼金次数已达上限(" + finishedCount + "/" + dailyCap + ")]");
        doSesameAlchemy.setValue(false);
        Log.record(TAG, "芝麻炼金 | 炼金 [已全部完成，临时关闭]");
        return;
      }

      if (zmlBalance < alchemyCost) {
        Log.record(TAG, "芝麻炼金[芝麻粒不足]: 需要 " + alchemyCost + ", 当前 " + zmlBalance);
        return;
      }

      int remainingAttempts = dailyCap - finishedCount;
      Log.record(TAG, "芝麻炼金[开始炼金], 剩余次数: " + remainingAttempts);

      for (int i = 0; i < remainingAttempts; i++) {
        // 每次炼金前重新检查余额
        if (zmlBalance < alchemyCost) {
          Log.record(TAG, "芝麻炼金[芝麻粒不足]: 需要 " + alchemyCost + ", 当前 " + zmlBalance);
          break;
        }

        String alchemyResultStr = AntMemberRpcCall.doAlchemy();
        JSONObject resultJo = new JSONObject(alchemyResultStr);

        // 根据新日志，成功信息在 'data' -> 'success'
        if (resultJo.optBoolean("success") && resultJo.getJSONObject("data").optBoolean("success")) {
          JSONObject resultData = resultJo.getJSONObject("data");
          String goldNum = resultData.optString("goldNum", "未知");
          zmlBalance -= alchemyCost; // 本地模拟扣减，避免重复查询
          Log.other("芝麻炼金-炼金🔮[第 " + (finishedCount + i + 1) + " 次成功]#消耗 " + alchemyCost + " 粒, 获得黄金 " + goldNum);
        } else {
          Log.record(TAG, "芝麻炼金[第 " + (finishedCount + i + 1) + " 次失败]: " + resultJo.optString("resultView"));
          // 如果失败，很可能是因为某些条件不满足，直接退出循环
          break;
        }
        // 停顿一下，避免请求过于频繁
        GlobalThreadPools.sleep(3000);
      }

    } catch (Throwable t) {
        Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 新增：芝麻树主逻辑
   */
  private void handleSesameTree() {
    if (sesameTreeTask.getValue()) {
      doSesameTreeTasks();
    }
    if (purifySesameTree.getValue()) {
      purifySesameTree();
    }
  }

  /**
   * 修正：执行芝麻树任务以获取净化值（包含完成和领取两个步骤）
   */
  private void doSesameTreeTasks() {
    try {
      Log.record(TAG, "芝麻树-开始攒净化值任务");
      String taskListStr = AntMemberRpcCall.getSesameTreeTaskList();
      JSONObject taskListJo = new JSONObject(taskListStr);
      if (!taskListJo.optBoolean("success") || !taskListJo.has("extInfo")) {
        Log.record(TAG, "获取芝麻树任务列表失败或结构不符: " + taskListJo.toString());
        return;
      }

      JSONArray tasks = taskListJo.getJSONObject("extInfo").getJSONObject("taskDetailList").getJSONArray("taskDetailList");
      Log.record(TAG, "获取到 " + tasks.length() + " 个芝麻树任务");
      int unfinishedCount = 0;

      // 第一遍：完成所有可做的浏览任务
      for (int i = 0; i < tasks.length(); i++) {
        JSONObject task = tasks.getJSONObject(i);
        String taskProcessStatus = task.getString("taskProcessStatus");
        if (!"NOT_DONE".equals(taskProcessStatus)) {
          continue;
        }

        JSONObject taskMaterial = task.getJSONObject("taskMaterial");
        String title = taskMaterial.getString("title");
        String innerTaskType = task.getJSONObject("taskExtProps").getString("TASK_TYPE");

        if ("COMMON_COUNT_DOWN_VIEW".equals(innerTaskType)) {
          unfinishedCount++;
          Log.record("芝麻树🌳[发现可做任务: " + title + "]");
          String taskId = task.getString("taskId");

          String browseTimeStr = taskMaterial.optString("browseTime", "0");
          int browseTime = 0;
          if (!browseTimeStr.isEmpty()) {
            try {
              browseTime = Integer.parseInt(browseTimeStr);
            } catch (NumberFormatException e) { /* ignore */ }
          }

          if (browseTime > 0) {
            Log.record("芝麻树🌳#模拟浏览 " + browseTime + " 秒...");
            GlobalThreadPools.sleep(browseTime * 1000L);
          } else {
            Log.record("芝麻树🌳#模拟点击...");
            GlobalThreadPools.sleep(2000);
          }

          // 发送任务完成信号
          String finishResultStr = AntMemberRpcCall.finishSesameTreeTask(taskId);
          JSONObject finishResultJo = new JSONObject(finishResultStr);
          if (finishResultJo.optBoolean("success")) {
            Log.record(TAG, "任务'" + title + "'已完成, 准备领取奖励");
          } else {
            Log.record(TAG, "完成芝麻树任务'" + title + "'失败: " + finishResultJo.toString());
          }
          GlobalThreadPools.sleep(3000); // 任务间稍作等待
        }
      }

      // 如果没有可做的任务，则检查是否有可领取的
      if (unfinishedCount == 0) {
        Log.record(TAG, "芝麻树🌳[没有需要完成的任务，检查是否有待领取奖励...]");
      }

      // 第二遍：刷新列表，领取所有已完成的奖励
      GlobalThreadPools.sleep(3000); // 等待后台状态更新
      taskListStr = AntMemberRpcCall.getSesameTreeTaskList();
      taskListJo = new JSONObject(taskListStr);
      if (!taskListJo.optBoolean("success") || !taskListJo.has("extInfo")) return;
      
      tasks = taskListJo.getJSONObject("extInfo").getJSONObject("taskDetailList").getJSONArray("taskDetailList");
      boolean hasUnclaimed = false;
      for (int i = 0; i < tasks.length(); i++) {
        JSONObject task = tasks.getJSONObject(i);
        String taskProcessStatus = task.getString("taskProcessStatus");
        if ("TO_RECEIVE".equals(taskProcessStatus)) {
          hasUnclaimed = true;
          String taskId = task.getString("taskId");
          String title = task.getJSONObject("taskMaterial").getString("title");
          String reward = task.getJSONObject("taskMaterial").optString("finishOneTaskGetPurificationValue", "未知");

          Log.record("芝麻树🌳[发现可领取奖励的任务: " + title + "]");
          String receiveResultStr = AntMemberRpcCall.receiveSesameTreeTaskReward(taskId);
          JSONObject receiveResultJo = new JSONObject(receiveResultStr);
          if (receiveResultJo.optBoolean("success")) {
            Log.other("芝麻树🌳[领取奖励: " + title + "]#获得净化值+" + reward);
          } else {
            Log.record(TAG, "领取芝麻树奖励'" + title + "'失败: " + receiveResultJo.toString());
          }
          GlobalThreadPools.sleep(2000); // 领取之间稍作等待
        }
      }
      
      if (unfinishedCount == 0 && !hasUnclaimed) {
        sesameTreeTask.setValue(false);
        Log.record(TAG, "芝麻树🌳[已全部完成且无待领取奖励，临时关闭]");
      }

    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 修正：净化芝麻树（清理电子垃圾）
   */
  /*private void purifySesameTree() {
    try {
      Log.record(TAG, "芝麻树-开始净化电子垃圾");
      String s = AntMemberRpcCall.getSesameTreeHomePage();
      JSONObject jo = new JSONObject(s);

      if (!jo.optBoolean("success") || !jo.has("extInfo")) {
        Log.record(TAG, "获取芝麻树主页信息失败或结构不符：" + jo.toString());
        return;
      }
      
      JSONObject result = jo.getJSONObject("extInfo").getJSONObject("zhimaTreeHomePageQueryResult");
      JSONArray trees = result.optJSONArray("trees"); // 使用optJSONArray
      if (trees != null && trees.length() > 0) {
        JSONObject tree = trees.getJSONObject(0);
        int remainClick = tree.optInt("remainPurificationClickNum", 0); // 使用optInt
        
        // [修正] 使用optJSONArray安全获取trashList，并在不存在或为空时直接返回
        JSONArray trashList = tree.optJSONArray("trashList");
        if (trashList == null || trashList.length() == 0) {
          Log.record(TAG, "芝麻树🌳[没有发现电子垃圾]");
          // 如果没有垃圾，同时检查一下净化次数，如果次数也用完，则关闭开关
          if (remainClick <= 0) {
            purifySesameTree.setValue(false);
            Log.record(TAG, "芝麻树🌳[净化次数已用完且无垃圾，开关临时关闭]");
          }
          return;
        }
        
        if (remainClick <= 0) {
          Log.record(TAG, "芝麻树🌳[今日净化次数已用完]");
          return;
        }

        Log.record(TAG, "发现 " + trashList.length() + " 个电子垃圾，剩余净化次数 " + remainClick + "，开始净化...");

        for (int i = 0; i < trashList.length() && remainClick > 0; i++) {
          JSONObject trash = trashList.getJSONObject(i);
          String trashCode = trash.getString("trashCode");
          String trashCampId = trash.getString("relateCampId");

          String cleanResultStr = AntMemberRpcCall.cleanSesameTreeTrash(trashCode, trashCampId);
          GlobalThreadPools.sleep(2000);
          JSONObject cleanResultJo = new JSONObject(cleanResultStr);

          if (cleanResultJo.optBoolean("success") && cleanResultJo.has("extInfo")) {
            JSONObject cleanResult = cleanResultJo.getJSONObject("extInfo")
                  .getJSONObject("zhimaTreeCleanAndPushResult");
            int newScore = cleanResult.getJSONObject("currentTreeInfo").getInt("scoreSummary");
            int purificationScore = cleanResult.getInt("purificationScore");
            Log.other("净化芝麻树🗑️[成功净化1个垃圾]#剩余净化值" + purificationScore + ", 当前成长值:" + newScore);
            remainClick--;
          } else {
            Log.record(TAG, "净化失败: " + cleanResultJo.toString());
            break; // 一旦失败就停止尝试
          }
        }
        
        // 循环结束后，如果剩余点击次数为0，也关闭开关
        if(remainClick <= 0) {
          purifySesameTree.setValue(false);
          Log.record(TAG, "芝麻树🌳[净化次数已用完，开关临时关闭]");
        }
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }*/
  /**
   * 修正：净化芝麻树 (根据剩余净化次数)
   */
  private void purifySesameTree() {
    try {
      Log.record(TAG, "芝麻树-开始净化");
      String s = AntMemberRpcCall.getSesameTreeHomePage();
      JSONObject jo = new JSONObject(s);

      if (!jo.optBoolean("success") || !jo.has("extInfo")) {
        Log.record(TAG, "获取芝麻树主页信息失败或结构不符：" + jo.toString());
        return;
      }
      
      JSONObject result = jo.getJSONObject("extInfo").getJSONObject("zhimaTreeHomePageQueryResult");
      JSONArray trees = result.optJSONArray("trees");
      if (trees == null || trees.length() == 0) {
        Log.record(TAG, "芝麻树-未找到tree信息");
        return;
      }
      
      JSONObject tree = trees.getJSONObject(0);
      int remainClick = tree.optInt("remainPurificationClickNum", 0);
      
      if (remainClick <= 0) {
        Log.record(TAG, "芝麻树🌳[今日净化次数已用完，开关临时关闭]");
        purifySesameTree.setValue(false); // 次数用完，关闭开关
        return;
      }

      Log.record(TAG, "剩余净化次数 " + remainClick + "，开始净化...");

      // 根据剩余次数循环净化
      for (int i = 0; i < remainClick; i++) {
        // 调用新增的、不依赖电子垃圾的净化方法
        String cleanResultStr = AntMemberRpcCall.cleanSesameTreeByClick();
        GlobalThreadPools.sleep(2000); // 每次净化后等待2秒
        JSONObject cleanResultJo = new JSONObject(cleanResultStr);

        if (cleanResultJo.optBoolean("success") && cleanResultJo.has("extInfo")) {
          JSONObject cleanResult = cleanResultJo.getJSONObject("extInfo")
                .getJSONObject("zhimaTreeCleanAndPushResult");
          int newScore = cleanResult.getJSONObject("currentTreeInfo").getInt("scoreSummary");
          int purificationScore = cleanResult.getInt("purificationScore");
          Log.other("净化芝麻树🗑️[成功净化1次]#剩余净化值" + purificationScore + ", 当前成长值:" + newScore);
        } else {
          Log.record(TAG, "净化失败: " + cleanResultJo.toString());
          break; // 如果某次净化失败，则停止循环
        }
      }
      
      // 所有次数执行完毕后，关闭开关
      purifySesameTree.setValue(false);
      Log.record(TAG, "芝麻树🌳[净化次数已全部用完，开关临时关闭]");

    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }
    
  /**
   * 商家开门打卡签到
   */
  private static void kmdkSignIn() {
    try {
      String s = AntMemberRpcCall.queryActivity();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        if ("SIGN_IN_ENABLE".equals(jo.getString("signInStatus"))) {
          String activityNo = jo.getString("activityNo");
          JSONObject joSignIn = new JSONObject(AntMemberRpcCall.signIn(activityNo));
          if (joSignIn.optBoolean("success")) {
            Log.other("商家服务🏬[开门打卡签到成功]");
          } else {
            Log.record(joSignIn.getString("errorMsg"));
            Log.runtime(joSignIn.toString());
          }
        }
      } else {
        Log.record(TAG,"queryActivity" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 商家开门打卡报名
   */
  private static void kmdkSignUp() {
    try {
      for (int i = 0; i < 5; i++) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.queryActivity());
        if (jo.optBoolean("success")) {
          String activityNo = jo.getString("activityNo");
          if (!TimeUtil.getFormatDate().replace("-", "").equals(activityNo.split("_")[2])) {
            break;
          }
          if ("SIGN_UP".equals(jo.getString("signUpStatus"))) {
            break;
          }
          if ("UN_SIGN_UP".equals(jo.getString("signUpStatus"))) {
            String activityPeriodName = jo.getString("activityPeriodName");
            JSONObject joSignUp = new JSONObject(AntMemberRpcCall.signUp(activityNo));
            if (joSignUp.optBoolean("success")) {
              Log.other("商家服务🏬[" + activityPeriodName + "开门打卡报名]");
              GlobalThreadPools.sleep(500);
              return;
            } else {
              Log.record(joSignUp.getString("errorMsg"));
              Log.runtime(joSignUp.toString());
              GlobalThreadPools.sleep(500);
            }
          }
        } else {
          Log.record(TAG,"queryActivity");
        }
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignUp err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 商家积分签到
   */
  private static void doMerchantSign() {
    try {
      String s = AntMemberRpcCall.merchantSign();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.runtime(TAG, "doMerchantSign err:" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      String signResult = jo.getString("signInResult");
      String reward = jo.getString("todayReward");
      if ("SUCCESS".equals(signResult)) {
        Log.other("商家服务🏬[每日签到]#获得积分" + reward);
      } else {
        Log.record(s);
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 商家积分任务
   */
  private static void doMerchantMoreTask() {
    String s = AntMemberRpcCall.taskListQuery();
    try {
      boolean doubleCheck = false;
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        JSONArray taskList = jo.getJSONObject("data").getJSONArray("taskList");
        for (int i = 0; i < taskList.length(); i++) {
          JSONObject task = taskList.getJSONObject(i);
          if (!task.has("status")) {
            continue;
          }
          String title = task.getString("title");
          String reward = task.getString("reward");
          String taskStatus = task.getString("status");
          if ("NEED_RECEIVE".equals(taskStatus)) {
            if (task.has("pointBallId")) {
              jo = new JSONObject(AntMemberRpcCall.ballReceive(task.getString("pointBallId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
            }
          } else if ("PROCESSING".equals(taskStatus) || "UNRECEIVED".equals(taskStatus)) {
            if (task.has("extendLog")) {
              JSONObject bizExtMap = task.getJSONObject("extendLog").getJSONObject("bizExtMap");
              jo = new JSONObject(AntMemberRpcCall.taskFinish(bizExtMap.getString("bizId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
              doubleCheck = true;
            } else {
              String taskCode = task.getString("taskCode");
              switch (taskCode) {
                case "SYH_CPC_DYNAMIC":
                  // 逛一逛商品橱窗
                  taskReceive(taskCode, "SYH_CPC_DYNAMIC_VIEWED", title);
                  break;
                case "JFLLRW_TASK":
                  // 逛一逛得缴费红包
                  taskReceive(taskCode, "JFLL_VIEWED", title);
                  break;
                case "ZFBHYLLRW_TASK":
                  // 逛一逛支付宝会员
                  taskReceive(taskCode, "ZFBHYLL_VIEWED", title);
                  break;
                case "QQKLLRW_TASK":
                  // 逛一逛支付宝亲情卡
                  taskReceive(taskCode, "QQKLL_VIEWED", title);
                  break;
                case "SSLLRW_TASK":
                  // 逛逛领优惠得红包
                  taskReceive(taskCode, "SSLL_VIEWED", title);
                  break;
                case "ELMGYLLRW2_TASK":
                  // 去饿了么果园0元领水果
                  taskReceive(taskCode, "ELMGYLL_VIEWED", title);
                  break;
                case "ZMXYLLRW_TASK":
                  // 去逛逛芝麻攒粒攻略
                  taskReceive(taskCode, "ZMXYLL_VIEWED", title);
                  break;
                case "GXYKPDDYH_TASK":
                  // 逛信用卡频道得优惠
                  taskReceive(taskCode, "xykhkzd_VIEWED", title);
                  break;
                case "HHKLLRW_TASK":
                  // 49999元花呗红包集卡抽
                  taskReceive(taskCode, "HHKLLX_VIEWED", title);
                  break;
                case "TBNCLLRW_TASK":
                  // 去淘宝芭芭农场领水果百货
                  taskReceive(taskCode, "TBNCLLRW_TASK_VIEWED", title);
                  break;
              }
            }
          }
        }
        if (doubleCheck) {
          doMerchantMoreTask();
        }
      } else {
        Log.runtime(TAG,"taskListQuery err:" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskListQuery err:");
      Log.printStackTrace(TAG, t);
    } finally {
      try {
        GlobalThreadPools.sleep(1000);
      } catch (Exception e) {
        Log.printStackTrace(e);
      }
    }
  }
  /**
   * 完成商家积分任务
   * @param taskCode 任务代码
   * @param actionCode 行为代码
   * @param title 标题
   */
  private static void taskReceive(String taskCode, String actionCode, String title) {
    try {
      String s = AntMemberRpcCall.taskReceive(taskCode);
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        GlobalThreadPools.sleep(500);
        jo = new JSONObject(AntMemberRpcCall.actioncode(actionCode));
        if (jo.optBoolean("success")) {
          GlobalThreadPools.sleep(16000);
          jo = new JSONObject(AntMemberRpcCall.produce(actionCode));
          if (jo.optBoolean("success")) {
            Log.other("商家服务🏬[完成任务" + title + "]");
          }
        }
      } else {
        Log.record(TAG,"taskReceive" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskReceive err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 保障金领取
   */
  private void collectInsuredGold() {
    try {
      String s = AntMemberRpcCall.queryAvailableCollectInsuredGold();
      // GlobalThreadPools.sleep(200);
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.other(TAG + ".collectInsuredGold.queryInsuredHome", "保障金🏥[响应失败]#" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      JSONObject signInBall = jo.getJSONObject("signInDTO");
      JSONArray otherBallList = jo.getJSONArray("eventToWaitDTOList");
      if (1 == signInBall.getInt("sendFlowStatus") && 1 == signInBall.getInt("sendType")) {
        s = AntMemberRpcCall.collectInsuredGold(signInBall);
        GlobalThreadPools.sleep(2000);
        jo = new JSONObject(s);
        if (!jo.optBoolean("success")) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]#+" + gainGold + "元");
      }
      for (int i = 0; i <otherBallList.length(); i++) {
        JSONObject anotherBall = otherBallList.getJSONObject(i);
        s = AntMemberRpcCall.collectInsuredGold(anotherBall);
        GlobalThreadPools.sleep(2000);
        jo = new JSONObject(s);
        if (!jo.optBoolean("success")) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getJSONObject("gainSumInsuredDTO").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]+" + gainGold + "元");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectInsuredGold", t);
    }
  }
  /**
   * 执行会员任务 类型1
   * @param task 单个任务对象
   */
  private void processTask(JSONObject task) throws JSONException {
    JSONObject taskConfigInfo = task.getJSONObject("taskConfigInfo");
    String name = taskConfigInfo.getString("name");
    // Log.record("task name:"+name);
    if((name.equals("逛15秒赚积分"))) { ///做不成功
      return;
    }
    long id = taskConfigInfo.getLong("id");
    String awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint");
    String targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0);
    String[] targetBusinessArray = targetBusiness.split("#");
    if (targetBusinessArray.length < 3) {
      Log.runtime(TAG, "processTask target param err:" + Arrays.toString(targetBusinessArray));
      return;
    }
    String bizType = targetBusinessArray[0];
    String bizSubType = targetBusinessArray[1];
    String bizParam = targetBusinessArray[2];
    GlobalThreadPools.sleep(16000);
    String str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id);
    JSONObject jo = new JSONObject(str);
    if (!ResChecker.checkRes(TAG + "执行会员任务失败:", jo)) {
      Log.runtime(TAG, "执行任务失败:" + jo.optString("resultDesc"));
      return;
    }
    if (checkMemberTaskFinished(id)) {
      Log.other("会员任务🎖️[" + name + "]#获得积分" + awardParamPoint);
    }
  }

  /**
   * 查询指定会员任务是否完成
   * @param taskId 任务id
   */
  private boolean checkMemberTaskFinished(long taskId) {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      // GlobalThreadPools.sleep(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResChecker.checkRes(TAG + "查询会员任务状态失败:", jsonObject)) {
        Log.error(TAG + ".checkMemberTaskFinished", "会员任务响应失败: " + jsonObject.getString("resultDesc"));
      }
      if (!jsonObject.has("availableTaskList")) {
        return true;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      for (int i = 0; i < taskList.length(); i++) {
        JSONObject taskConfigInfo = taskList.getJSONObject(i).getJSONObject("taskConfigInfo");
        long id = taskConfigInfo.getLong("id");
        if (taskId == id) {
          return false;
        }
      }
      return true;
    } catch (JSONException e) {
      return false;
    }
  }
  public void kbMember() {
    try {
      if (!Status.canKbSignInToday()) {
        return;
      }
      String s = AntMemberRpcCall.rpcCall_signIn();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success", false)) {
        jo = jo.getJSONObject("data");
        Log.other("口碑签到📅[第" + jo.getString("dayNo") + "天]#获得" + jo.getString("value") + "积分");
        Status.KbSignInToday();
      } else if (s.contains("\"HAS_SIGN_IN\"")) {
        Status.KbSignInToday();
      } else {
        Log.runtime(TAG, jo.getString("errorMessage"));
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void goldTicket() {
    try {
      // 签到
      goldBillCollect("\"campId\":\"CP1417744\",\"directModeDisableCollect\":true,\"from\":\"antfarm\",");
      // 收取其他
      goldBillCollect("");
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }
  /** 收取黄金票 */
  private void goldBillCollect(String signInfo) {
    try {
      String str = AntMemberRpcCall.goldBillCollect(signInfo);
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".goldBillCollect.goldBillCollect", jsonObject.optString("resultDesc"));
        return;
      }
      JSONObject object = jsonObject.getJSONObject("result");
      JSONArray jsonArray = object.getJSONArray("collectedList");
      int length = jsonArray.length();
      if (length == 0) {
        return;
      }
      for (int i = 0; i < length; i++) {
        Log.other("黄金票🙈[" + jsonArray.getString(i) + "]");
      }
      Log.other("黄金票🏦本次总共获得[" + JsonUtil.getValueByPath(object, "collectedCamp.amount") + "]");
    } catch (Throwable th) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, th);
    }
  }
  private void enableGameCenter() {
    try {
      try {
        String str = AntMemberRpcCall.querySignInBall();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.querySignInBall", jsonObject.optString("resultDesc"));
          return;
        }
        str = JsonUtil.getValueByPath(jsonObject, "data.signInBallModule.signInStatus");
        if (String.valueOf(true).equals(str)) {
          return;
        }
        str = AntMemberRpcCall.continueSignIn();
        // GlobalThreadPools.sleep(300);
        jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.continueSignIn", jsonObject.optString("resultDesc"));
          return;
        }
        Log.other("游戏中心🎮签到成功");
      } catch (Throwable th) {
        Log.runtime(TAG, "signIn err:");
        Log.printStackTrace(TAG, th);
      }
      try {
        String str = AntMemberRpcCall.queryPointBallList();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".batchReceive.queryPointBallList", jsonObject.optString("resultDesc"));
          return;
        }
        JSONArray jsonArray = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "data.pointBallList");
        if (jsonArray == null || jsonArray.length() == 0) {
          return;
        }
        str = AntMemberRpcCall.batchReceivePointBall();
        GlobalThreadPools.sleep(300);
        jsonObject = new JSONObject(str);
        if (jsonObject.optBoolean("success")) {
          Log.other("游戏中心🎮全部领取成功[" + JsonUtil.getValueByPath(jsonObject, "data.totalAmount") + "]乐豆");
        } else {
          Log.runtime(TAG + ".batchReceive.batchReceivePointBall", jsonObject.optString("resultDesc"));
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "batchReceive err:");
        Log.printStackTrace(TAG, th);
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanSignIn() {
    try {
        // 1. 查询签到流程状态
        String signInProcessStr = AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN");
        JSONObject jo = new JSONObject(signInProcessStr);

        if (!jo.optBoolean("success", false)) {
            Log.runtime(TAG, "查询签到状态失败: " + signInProcessStr);
            return;
        }

        JSONObject result = jo.getJSONObject("result");

        // 2. 检查是否已经签到完成
        // 根据签到后的数据，"progress" 会变为 "FULLY_DONE"
        if ("FULLY_DONE".equals(result.optString("progress"))) {
            // 如果已经签到，尝试从taskDetailList中获取当天的奖励信息并记录
            try {
                JSONObject taskDetail = result.getJSONArray("taskDetailList").getJSONObject(0);
                if (taskDetail.optBoolean("hasSend", false)) {
                    JSONObject sendOrder = taskDetail.getJSONObject("sendOrder");
                    String prizeName = sendOrder.getString("prizeName");
                    Log.record(TAG, "安心豆🫘[今日已签到:" + prizeName + "]");
                    return; // 结束方法
                }
            } catch (JSONException e) {
                // 如果解析失败，也记录一个通用日志
                Log.record(TAG, "安心豆🫘[本月已签到够数]");
                Log.printStackTrace(TAG, e);
                return;
            }
        }

        // 3. 如果未签到，并且接口允许签到 ("canPush" is true)
        if (result.optBoolean("canPush", false)) {
            String signInTriggerStr = AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN");
            JSONObject triggerJo = new JSONObject(signInTriggerStr);

            if (triggerJo.optBoolean("success", false)) {
                // 假设 signInTrigger 成功后返回的结构与原代码预期一致
                // 注意：根据您提供的数据，奖励名称也可以在操作成功后再次调用 querySignInProcess 获得
                String prizeName = triggerJo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
                Log.other(TAG, "安心豆🫘[签到成功:" + prizeName + "]");
            } else {
                Log.runtime(TAG, "执行签到失败: " + signInTriggerStr);
            }
        }
      } catch (JSONException e) {
          Log.error(TAG, "安心豆🫘[JSON解析异常]");
          Log.printStackTrace(TAG, e);
      } catch (NullPointerException e) {
          Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化");
          Log.printStackTrace(TAG, e);
      } catch (Throwable t) {
          Log.runtime(TAG, "beanSignIn err:");
          Log.printStackTrace(TAG, t);
      }
  }

  private void beanExchangeBubbleBoost() {
    try {
      // 检查RPC调用是否可用
      try {
        String accountInfo = AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN");

          JSONObject jo = new JSONObject(accountInfo);
        if (!jo.optBoolean("success")) {
          Log.runtime(jo.toString());
          return;
        }
        
        int userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint");
        
        // 检查beanExchangeDetail调用
        String exchangeDetailStr = AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722");

          jo = new JSONObject(exchangeDetailStr);
        if (!jo.optBoolean("success")) {
          Log.runtime(jo.toString());
          return;
        }
        
        jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
        String itemId = jo.getString("itemId");
        String itemName = jo.getString("itemName");
        jo = jo.getJSONObject("itemExchangeConsultDTO");
        int realConsumePointAmount = jo.getInt("realConsumePointAmount");
        
        if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
          return;
        }
        
        String exchangeResult = AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount);

          jo = new JSONObject(exchangeResult);
        if (jo.optBoolean("success")) {
          Log.record(TAG,"安心豆🫘[兑换:" + itemName + "]");
        } else {
          Log.runtime(jo.toString());
        }
      } catch (NullPointerException e) {
        Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化");
        Log.printStackTrace(TAG, e);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanExchangeBubbleBoost err:");
      Log.printStackTrace(TAG, t);
    }
  }
}