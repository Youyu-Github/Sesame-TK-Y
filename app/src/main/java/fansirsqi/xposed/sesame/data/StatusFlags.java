package fansirsqi.xposed.sesame.data;

/**
 * 用于统一管理所有每日 Flag / 状态标记的常量。
 *
 * 建议所有标记都写在这里，避免在项目里到处写字符串。
 * 命名规范：
 *  - 常量名：全大写 + 下划线
 *  - 常量值：实际保存到本地的字符串（可以保持小写）
 */
public final class StatusFlags {

    private StatusFlags() {
        // 禁止实例化
    }

    public static final String FLAG_NEVERLAND_STEPCOUNT = "Flag_Neverland_StepCount";  // 今日步数任务


    public static final String FLAG_AntMember_doAllAvailableSesameTask = "AntMember::doAllAvailableSesameTask";  //芝麻信用任务

    // 运动任务大厅-今日是否已尝试循环处理
    public static final String FLAG_ANTSPORTS_TASKCENTER_DONE = "Flag_AntSports_TaskCenter_Done";

    // --------------------------------------------
    // 农场 小组件回访
    // --------------------------------------------
    public static final String FLAG_ANTORCHARD_WIDGET_DAILY_AWARD = "Flag_Antorchard_Widget_Daily_Award";  // 农场小组件每日奖励领取

    // 蚂蚁新村-今日丢肥料是否已达上限
    public static final String FLAG_ANTSTALL_THROW_MANURE_LIMIT = "Flag_AntStall_Throw_Manure_Limit";

}