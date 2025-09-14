package fansirsqi.xposed.sesame.util;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class ResChecker {
    private static final String TAG = ResChecker.class.getSimpleName();

    private static boolean core(String TAG, JSONObject jo) {
        try {
//            Log.runtime(TAG, "Checking JSON success: " + jo);
            // 检查 success 或 isSuccess 字段为 true
            if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
                return true;
            }

            // 检查 resultCode
            Object resCode = jo.opt("resultCode");
            if (resCode != null) {
                if (resCode instanceof Integer && (Integer) resCode == 200) {
                    return true;
                } else if (resCode instanceof String &&
                        Pattern.matches("(?i)SUCCESS|100", (String) resCode)) {
                    return true;
                }
            }

            // 检查 memo 字段
            if ("SUCCESS".equalsIgnoreCase(jo.optString("memo", ""))) {
                return true;
            }

            // 获取调用栈信息以确定错误来源
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callerInfo = "";
            // 寻找第一个非ResChecker类且非Java系统类的调用者（真正的业务代码调用位置）
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement element = stackTrace[i];
                String className = element.getClassName();
                
                // 跳过ResChecker类和Java系统类
                if (!className.contains("ResChecker") && 
                    !className.startsWith("java.lang.") && 
                    !className.startsWith("java.util.") &&
                    className.contains("fansirsqi.xposed.sesame")) {
                    
                    // 获取简化的类名（不含包名）
                    String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                    callerInfo = simpleClassName + "." + element.getMethodName() + ":" + element.getLineNumber();
                    break;
                }
            }
            if (callerInfo.isEmpty()) {
                callerInfo = "未知来源";
            }
            
            Log.error(TAG, "Check failed: [来源: " + callerInfo + "] " + jo);
            return false;

        } catch (Throwable t) {
            Log.printStackTrace(TAG, "Error checking JSON success:", t);
            return false;
        }
    }

    @NonNull
    private static String getString(StackTraceElement[] stackTrace) {
        StringBuilder callerInfo = new StringBuilder();
        int foundCount = 0;
        // 最多显示4层调用栈
        final int MAX_STACK_DEPTH = 4;
        final String PROJECT_PACKAGE = "fansirsqi.xposed.sesame";
        
        // 寻找项目包名下的调用者
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 只显示项目包名下的类，跳过ResChecker
            if (className.startsWith(PROJECT_PACKAGE) && !className.contains("ResChecker")) {
                // 获取类名（保留项目包名后的部分）
                String relativeClassName = className.substring(PROJECT_PACKAGE.length() + 1);
                if (foundCount > 0) {
                    callerInfo.append(" <- ");
                }
                callerInfo.append(relativeClassName)
                         .append(".")
                         .append(element.getMethodName())
                         .append(":")
                         .append(element.getLineNumber());
                
                foundCount++;
                if (foundCount >= MAX_STACK_DEPTH) {
                    break;
                }
            }
        }
        return callerInfo.toString();
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    private static boolean checkRes(JSONObject jo) {
        return core(TAG, jo);
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, JSONObject jo) {
        return core(TAG, jo);
    }



    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jsonStr JSON对象的字符串表示
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, String jsonStr) throws JSONException {
        JSONObject jo = new JSONObject(jsonStr);
        return checkRes(TAG, jo);
    }


}
