package fansirsqi.xposed.sesame.task.antMember;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashSet;

import fansirsqi.xposed.sesame.entity.MemberBenefit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
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

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberTask = new BooleanModelField("memberTask", "会员任务", false));
    modelFields.addField(memberPointExchangeBenefit = new BooleanModelField("memberPointExchangeBenefit", "会员积分 | 兑换权益", false));
    modelFields.addField(memberPointExchangeBenefitList = new SelectModelField("memberPointExchangeBenefitList", "会员积分 | 权益列表", new LinkedHashSet<>(), MemberBenefit.Companion.getList()));
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
   * 芝麻信用任务
   */
  /*
  private static void doAllAvailableSesameTask() {
    try {
      String s = AntMemberRpcCall.queryAvailableSesameTask();
      GlobalThreadPools.sleep(500);
      JSONObject jo = new JSONObject(s);
      if (jo.has("resData")) {
        jo = jo.getJSONObject("resData");
      }
      if (!jo.optBoolean("success")) {
        Log.other(TAG, "芝麻信用💳[查询任务响应失败]#" + jo.getString("resultCode"));
        Log.error(TAG + ".doAllAvailableSesameTask.queryAvailableSesameTask", "芝麻信用💳[查询任务响应失败]#" + s);
        return;
      }
      JSONObject taskObj = jo.getJSONObject("data");
      // Log.record(TAG, "芝麻信用💳[任务数据]#" + taskObj);
      if (taskObj.has("dailyTaskListVO")) {
        JSONObject dailyTaskListVO = taskObj.getJSONObject("dailyTaskListVO");
       // Log.record(TAG, "芝麻信用💳[日常任务列表]#" + dailyTaskListVO);

        if (dailyTaskListVO.has("waitCompleteTaskVOS")) {
          Log.record(TAG, "芝麻信用💳[待完成任务]#开始处理");
          joinAndFinishSesameTask(dailyTaskListVO.getJSONArray("waitCompleteTaskVOS"));
        }
        
        if (dailyTaskListVO.has("waitJoinTaskVOS")) {
          Log.record(TAG, "芝麻信用💳[待加入任务]#开始处理");
          joinAndFinishSesameTask(dailyTaskListVO.getJSONArray("waitJoinTaskVOS"));
        }
      }
      if (taskObj.has("toCompleteVOS")) {
        Log.record(TAG, "芝麻信用💳[toCompleteVOS任务]#开始处理");
        joinAndFinishSesameTask(taskObj.getJSONArray("toCompleteVOS"));
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doAllAvailableSesameTask", t);
    }
  }
  */
  /**
   * 芝麻信用-领取并完成任务
   * @param taskList 任务列表
   * @throws JSONException JSON解析异常，上抛处理
   */
  /*
  private static void joinAndFinishSesameTask(JSONArray taskList) throws JSONException {
    try {
      // Log.record(TAG, "芝麻信用💳[任务列表]#" + taskList.toString());
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".joinAndFinishSesameTask", t);
    }

    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      // 添加检查，确保templateId存在
      if (!task.has("templateId")) {
        String taskTitle = task.has("title") ? task.getString("title") : "未知任务";
        Log.error(TAG, "芝麻信用💳[任务缺少templateId字段]#任务标题:" + taskTitle);
        continue;  // 跳过这个任务
      }
      String taskTemplateId = task.getString("templateId");
      String taskTitle = task.has("title") ? task.getString("title") : "未知任务";
      int needCompleteNum = task.has("needCompleteNum") ? task.getInt("needCompleteNum") : 1;
      int completedNum = task.optInt("completedNum", 0);
      String s;
      String recordId;
      JSONObject responseObj;

      // 无法完成的任务
      switch (taskTemplateId) {
        case "save_ins_universal_new": // 坚持攒保证金
        case "xiaofeijin_visit_new": // 坚持攒消费金金币
        case "xianyonghoufu_new": // 体验先用后付
          continue;
      }

      if (task.has("actionUrl") && task.getString("actionUrl").contains("jumpAction")) {
        // 跳转APP任务 依赖跳转的APP发送请求鉴别任务完成 仅靠hook支付宝无法完成
        Log.record(TAG, "芝麻信用💳[跳过跳转APP任务]#" + taskTitle);
        continue;
      }
      if (!task.has("todayFinish")) {
        // 领取任务
        s = AntMemberRpcCall.joinSesameTask(taskTemplateId);
        GlobalThreadPools.sleep(200);
        responseObj = new JSONObject(s);
        if (!responseObj.optBoolean("success")) {
          Log.other(TAG, "芝麻信用💳[领取任务" + taskTitle + "失败]#" + s);
          Log.error(TAG + ".joinAndFinishSesameTask.joinSesameTask", "芝麻信用💳[领取任务" + taskTitle + "失败]#" + s);
          continue;
        }
        recordId = responseObj.getJSONObject("data").getString("recordId");
      } else {
        if (!task.has("recordId")) {
          Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task);
          Log.error(TAG + ".joinAndFinishSesameTask", "芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task);
          continue;
        }
        recordId = task.getString("recordId");
      }
      s = AntMemberRpcCall.feedBackSesameTask(taskTemplateId);
      GlobalThreadPools.sleep(200);
      responseObj = new JSONObject(s);
      if (!responseObj.optBoolean("success")) {
        Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "回调失败]#" + responseObj.getString("errorMessage"));
        Log.error(TAG + ".joinAndFinishSesameTask.feedBackSesameTask", "芝麻信用💳[任务" + taskTitle + "回调失败]#" + s);
        continue;
      }

      // 是否为浏览15s任务
      boolean assistiveTouch = task.has("strategyRule") && task.getJSONObject("strategyRule").optBoolean("assistiveTouch");
      if (task.optBoolean("jumpToPushModel") || assistiveTouch) {
        s = AntMemberRpcCall.finishSesameTask(recordId);
        GlobalThreadPools.sleep(16000);
        responseObj = new JSONObject(s);
        if (!responseObj.optBoolean("success")) {
          Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "完成失败]#" + s);
          Log.error(TAG + ".joinAndFinishSesameTask.finishSesameTask", "芝麻信用💳[任务" + taskTitle + "完成失败]#" + s);
          continue;
        }
      }
      Log.other("芝麻信用💳[完成任务" + taskTitle + "]#(" + (completedNum + 1) + "/" + needCompleteNum + "天)");
    }
  }
  */

  /**
   * 芝麻信用任务 - 重构版本
   */
  private void doAllAvailableSesameTask() {
    try {
      String s = AntMemberRpcCall.queryAvailableSesameTask();
      GlobalThreadPools.sleep(500);
      JSONObject jo = new JSONObject(s);
      if (jo.has("resData")) {
        jo = jo.getJSONObject("resData");
      }
      if (!jo.optBoolean("success")) {
        Log.other(TAG, "芝麻信用💳[查询任务响应失败]#" + jo.getString("resultCode"));
        Log.error(TAG + ".doAllAvailableSesameTask.queryAvailableSesameTask", "芝麻信用💳[查询任务响应失败]#" + s);
        return;
      }

     // Log.record(TAG, "芝麻信用💳[查询任务响应]#" + s);

      JSONObject taskObj = jo.getJSONObject("data");
      int totalTasks = 0;
      int completedTasks = 0;
      int skippedTasks = 0;

      // 处理日常任务
      if (taskObj.has("dailyTaskListVO")) {
        JSONObject dailyTaskListVO = taskObj.getJSONObject("dailyTaskListVO");

        if (dailyTaskListVO.has("waitCompleteTaskVOS")) {
          JSONArray waitCompleteTaskVOS = dailyTaskListVO.getJSONArray("waitCompleteTaskVOS");
          totalTasks += waitCompleteTaskVOS.length();
          Log.record(TAG, "芝麻信用💳[待完成任务]#开始处理(" + waitCompleteTaskVOS.length() + "个)");
          int[] results = joinAndFinishSesameTaskWithResult(waitCompleteTaskVOS);
          completedTasks += results[0];
          skippedTasks += results[1];
        }

        if (dailyTaskListVO.has("waitJoinTaskVOS")) {
          JSONArray waitJoinTaskVOS = dailyTaskListVO.getJSONArray("waitJoinTaskVOS");
          totalTasks += waitJoinTaskVOS.length();
          Log.record(TAG, "芝麻信用💳[待加入任务]#开始处理(" + waitJoinTaskVOS.length() + "个)");
          int[] results = joinAndFinishSesameTaskWithResult(waitJoinTaskVOS);
          completedTasks += results[0];
          skippedTasks += results[1];
        }
      }

      // 处理toCompleteVOS任务
      if (taskObj.has("toCompleteVOS")) {
        JSONArray toCompleteVOS = taskObj.getJSONArray("toCompleteVOS");
        totalTasks += toCompleteVOS.length();
        Log.record(TAG, "芝麻信用💳[toCompleteVOS任务]#开始处理(" + toCompleteVOS.length() + "个)");
        int[] results = joinAndFinishSesameTaskWithResult(toCompleteVOS);
        completedTasks += results[0];
        skippedTasks += results[1];
      }

      // 统计结果并决定是否关闭开关
      Log.record(TAG, "芝麻信用💳[任务处理完成]#总任务:" + totalTasks + "个, 完成:" + completedTasks + "个, 跳过:" + skippedTasks + "个");
      
      // 如果所有任务都已完成或跳过（没有剩余可完成任务），关闭开关
      if (totalTasks > 0 && (completedTasks + skippedTasks) >= totalTasks) {
        sesameTask.setValue(false);
        Log.record(TAG, "芝麻信用💳[已全部完成任务，临时关闭]");
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
    "坚持种水果",              // 需要淘宝操作  
    "坚持去玩休闲小游戏",       // 需要游戏操作
    "去AQapp提问",            // 需要下载APP
    "去AQ提问",               // 需要下载APP
    "坚持看直播领福利",        // 需要淘宝直播
    "去淘金币逛一逛",          // 需要淘宝操作
    "浏览租赁商家小程序",        // 需要小程序操作
    "坚持攒保障金",
    "坚持攒保障",
    "芝麻租赁下单得芝麻粒",
    "坚持攒去玩休闲小游戏",
    "坚持看直播领福利"
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
   * 芝麻信用-领取并完成任务（带结果统计）
   * @param taskList 任务列表
   * @return int数组 [完成数量, 跳过数量]
   * @throws JSONException JSON解析异常，上抛处理
   */
  private static int[] joinAndFinishSesameTaskWithResult(JSONArray taskList) throws JSONException {
    int completedCount = 0;
    int skippedCount = 0;
    
    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      String taskTitle = task.has("title") ? task.getString("title") : "未知任务";
      
      // 打印任务状态信息用于调试
      boolean finishFlag = task.optBoolean("finishFlag", false);
      String actionText = task.optString("actionText", "");
    //  Log.record(TAG, "芝麻信用💳[任务状态调试]#" + taskTitle + " - finishFlag:" + finishFlag + ", actionText:" + actionText);
      
      // 检查任务是否已完成
      if (finishFlag || "已完成".equals(actionText)) {
        Log.record(TAG, "芝麻信用💳[跳过已完成任务]#" + taskTitle);
        skippedCount++;
        continue;
      }
      
      // 检查黑名单
      if (isTaskInBlacklist(taskTitle)) {
        Log.record(TAG, "芝麻信用💳[跳过黑名单任务]#" + taskTitle);
        skippedCount++;
        continue;
      }
      
      // 添加检查，确保templateId存在
      if (!task.has("templateId")) {
        Log.record(TAG, "芝麻信用💳[跳过缺少templateId任务]#" + taskTitle);
        skippedCount++;
        continue;
      }
      
      String taskTemplateId = task.getString("templateId");
      int needCompleteNum = task.has("needCompleteNum") ? task.getInt("needCompleteNum") : 1;
      int completedNum = task.optInt("completedNum", 0);
      String s;
      String recordId;
      JSONObject responseObj;


      if (task.has("actionUrl") && task.getString("actionUrl").contains("jumpAction")) {
        // 跳转APP任务 依赖跳转的APP发送请求鉴别任务完成 仅靠hook支付宝无法完成
        Log.record(TAG, "芝麻信用💳[跳过跳转APP任务]#" + taskTitle);
        skippedCount++;
        continue;
      }
      
      boolean taskCompleted = false;
      if (!task.has("todayFinish")) {
        // 领取任务
        s = AntMemberRpcCall.joinSesameTask(taskTemplateId);
        GlobalThreadPools.sleep(200);
        responseObj = new JSONObject(s);
        if (!responseObj.optBoolean("success")) {
          Log.other(TAG, "芝麻信用💳[领取任务" + taskTitle + "失败]#" + s);
          skippedCount++;
          continue;
        }
        recordId = responseObj.getJSONObject("data").getString("recordId");
      } else {
        if (!task.has("recordId")) {
          Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task);
          skippedCount++;
          continue;
        }
        recordId = task.getString("recordId");
      }

      // 完成任务
      for (int j = completedNum; j < needCompleteNum; j++) {
        s = AntMemberRpcCall.finishSesameTask(recordId);
        GlobalThreadPools.sleep(200);
        responseObj = new JSONObject(s);
        if (responseObj.optBoolean("success")) {
          Log.record(TAG, "芝麻信用💳[完成任务" + taskTitle + "]#(" + (j + 1) + "/" + needCompleteNum + "天)");
          taskCompleted = true;
        } else {
          Log.other(TAG, "芝麻信用💳[完成任务" + taskTitle + "失败]#" + s);
          break;
        }
      }
      
      if (taskCompleted) {
        completedCount++;
      } else {
        skippedCount++;
      }
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
        Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
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
          Log.other(TAG, "芝麻信用💳[一键收取芝麻粒响应失败]#" + jo);
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
            Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
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
   * 修正：净化芝麻树（清理电子垃圾）
   */
  private void purifySesameTree() {
      try {
          Log.record(TAG, "芝麻树-开始净化电子垃圾");
          String s = AntMemberRpcCall.getSesameTreeHomePage();
          JSONObject jo = new JSONObject(s);

          // 修改：直接检查根级别的success和extInfo字段
          if (!jo.optBoolean("success") || !jo.has("extInfo")) {
              Log.record(TAG, "获取芝麻树主页信息失败或结构不符：" + jo.toString());
              return;
          }
          
          JSONObject result = jo.getJSONObject("extInfo").getJSONObject("zhimaTreeHomePageQueryResult");
          JSONArray trees = result.getJSONArray("trees");
          if (trees.length() > 0) {
              JSONObject tree = trees.getJSONObject(0);
              int remainClick = tree.getInt("remainPurificationClickNum");
              if (remainClick <= 0) {
                  Log.record(TAG, "今日净化次数已用完");
                  return;
              }
              JSONArray trashList = tree.getJSONArray("trashList");
              if (trashList.length() == 0) {
                  Log.record(TAG, "没有发现电子垃圾");
                  return;
              }
              Log.record(TAG, "发现 " + trashList.length() + " 个电子垃圾，开始净化...");

              for (int i = 0; i < trashList.length() && remainClick > 0; i++) {
                  JSONObject trash = trashList.getJSONObject(i);
                  String trashCode = trash.getString("trashCode");
                  String trashCampId = trash.getString("relateCampId");

                  String cleanResultStr = AntMemberRpcCall.cleanSesameTreeTrash(trashCode, trashCampId);
                  GlobalThreadPools.sleep(2000);
                  JSONObject cleanResultJo = new JSONObject(cleanResultStr);

                  // 修改：直接检查根级别的success和extInfo字段
                  if (cleanResultJo.optBoolean("success") && cleanResultJo.has("extInfo")) {
                      JSONObject cleanResult = cleanResultJo.getJSONObject("extInfo")
                              .getJSONObject("zhimaTreeCleanAndPushResult");
                      int newScore = cleanResult.getJSONObject("currentTreeInfo").getInt("scoreSummary");
                      int purificationScore = cleanResult.getInt("purificationScore");
                      Log.other("净化芝麻树🗑️[成功净化1个垃圾]#获得净化值" + purificationScore + ", 当前成长值:" + newScore);
                      remainClick--;
                  } else {
                      Log.record(TAG, "净化失败: " + cleanResultJo.toString());
                      break;
                  }
              }
          }
      } catch (Throwable t) {
          Log.printStackTrace(TAG, t);
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
      try {
        String signInProcessStr = AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN");

          JSONObject jo = new JSONObject(signInProcessStr);
        if (!jo.optBoolean("success")) {
        } else {
          Log.runtime(jo.toString());
          return;
        }
        
        if (jo.getJSONObject("result").getBoolean("canPush")) {
          String signInTriggerStr = AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN");

            jo = new JSONObject(signInTriggerStr);
          if (jo.optBoolean("success")) {
            String prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
            Log.record(TAG,"安心豆🫘[" + prizeName + "]");
          } else {
            Log.runtime(jo.toString());
          }
        }
      } catch (NullPointerException e) {
        Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化");
        Log.printStackTrace(TAG, e);
      }
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