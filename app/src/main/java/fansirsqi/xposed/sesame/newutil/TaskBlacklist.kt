package fansirsqi.xposed.sesame.newutil

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.util.Log

/**
 * 通用任务黑名单管理器
 * 使用DataStore持久化存储黑名单数据
 */
object TaskBlacklist {
    private const val TAG = "TaskBlacklist"
    private const val BLACKLIST_KEY = "task_blacklist"

    /**
     * 获取黑名单列表
     * @return 黑名单任务集合
     */
    fun getBlacklist(): Set<String> {
        return try {
            DataStore.getOrCreate(BLACKLIST_KEY, object : TypeReference<Set<String>>() {})
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取黑名单失败，使用默认黑名单", e)
            defaultBlacklist
        }
    }
    
    
    
    /**
     * 保存黑名单列表
     * @param blacklist 要保存的黑名单集合
     */
    private fun saveBlacklist(blacklist: Set<String>) {
        try {
            DataStore.put(BLACKLIST_KEY, blacklist)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "保存黑名单失败", e)
        }
    }
    
    /**
     * 检查任务是否在黑名单中（精确匹配）
     * @param taskId 任务ID
     * @return true表示在黑名单中，应该跳过
     */
    fun isTaskInBlacklist(taskId: String?): Boolean {
        if (taskId == null) return false
        val blacklist = getBlacklist()
        return blacklist.contains(taskId)
    }
    
    /**
     * 检查任务是否在黑名单中（模糊匹配，适用于芝麻信用任务）
     * @param taskTitle 任务标题
     * @return true表示在黑名单中，应该跳过
     */
    fun isTaskInBlacklistFuzzy(taskTitle: String?): Boolean {
        if (taskTitle == null) return false
        
        val blacklist = getBlacklist()
        return blacklist.any { blacklistItem -> 
            taskTitle.contains(blacklistItem)
        }
    }
    
    /**
     * 添加任务到黑名单
     * @param taskId 要添加的任务ID
     */
    fun addToBlacklist(taskId: String) {
        if (taskId.isBlank()) return
        
        val currentBlacklist = getBlacklist().toMutableSet()
        if (currentBlacklist.add(taskId)) {
            saveBlacklist(currentBlacklist)
            Log.record(TAG, "任务[$taskId]已加入黑名单")
        }
    }
    
    /**
     * 从黑名单中移除任务
     * @param taskId 要移除的任务ID
     */
    fun removeFromBlacklist(taskId: String) {
        if (taskId.isBlank()) return
        
        val currentBlacklist = getBlacklist().toMutableSet()
        if (currentBlacklist.remove(taskId)) {
            saveBlacklist(currentBlacklist)
            Log.record(TAG, "任务[$taskId]已从黑名单移除")
        }
    }
    
    /**
     * 清空黑名单
     */
    fun clearBlacklist() {
        try {
            saveBlacklist(emptySet())
            Log.record(TAG, "黑名单已清空")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "清空黑名单失败", e)
        }
    }
    
    /**
     * 根据错误码自动添加任务到黑名单
     * @param taskId 任务ID
     * @param taskTitle 任务标题（可选，用于日志）
     * @param errorCode 错误码
     */
    fun autoAddToBlacklist(taskId: String, taskTitle: String = "", errorCode: String) {
        if (taskId.isBlank()) return
        
        // 检查是否是应该自动加入黑名单的错误码
        val shouldAutoAdd = when (errorCode) {
            "400000040" -> true // "不支持rpc调用" - 仅农场任务
            "OP_REPEAT_CHECK", "ILLEGAL_ARGUMENT", "PROMISE_HAS_PROCESSING_TEMPLATE" -> true
            else -> false
        }
        
        if (shouldAutoAdd) {
            addToBlacklist(taskId)
            val reason = when (errorCode) {
                "400000040" -> "不支持rpc调用"
                "OP_REPEAT_CHECK" -> "操作太频繁"
                "ILLEGAL_ARGUMENT" -> "参数错误"
                "PROMISE_HAS_PROCESSING_TEMPLATE" -> "存在进行中的生活记录"
                else -> "未知错误"
            }
            val taskInfo = if (taskTitle.isNotBlank()) "$taskId - $taskTitle" else taskId
            Log.record(TAG, "任务[$taskInfo]因$reason 自动加入黑名单")
        }
    }
}