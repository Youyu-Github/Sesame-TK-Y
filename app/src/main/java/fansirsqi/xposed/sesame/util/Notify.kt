package fansirsqi.xposed.sesame.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.BaseModel
import kotlin.concurrent.Volatile

@SuppressLint("StaticFieldLeak")
object Notify {
    private val TAG: String = Notify::class.java.simpleName
    
    // 添加同步锁防止并发问题
    private val updateLock = Any()
    
    @SuppressLint("StaticFieldLeak")
    var context: Context? = null
    private const val NOTIFICATION_ID = 99
    private const val ERROR_NOTIFICATION_ID = 98
    private const val CHANNEL_ID = "fansirsqi.xposed.sesame.ANTFOREST_NOTIFY_CHANNEL"
    private var mNotifyManager: NotificationManager? = null

    @SuppressLint("StaticFieldLeak")
    private var builder: NotificationCompat.Builder? = null

    @Volatile
    private var isNotificationStarted = false

    private var lastUpdateTime: Long = 0
    private var nextExecTimeCache: Long = 0
    private var statusText: String = "" // 状态文本（第一行）
    private var detailText: String = "" // 详情文本（第二行）


    private fun checkPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.error(TAG, "Missing POST_NOTIFICATIONS permission to send new notification$context")
                Toast.show("请在设置中开启支付宝通知权限")
                return false
            }
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.error(TAG, "Notifications are disabled for this app.$context")
            Toast.show("请在设置中开启支付宝通知权限")
            return false
        }
        return true
    }

    @JvmStatic
    fun start(context: Context) {
        try {
            if (checkPermission(context)) {
                Notify.context = context
                stop()
                statusText = "🚀 启动中"
                detailText = "🔔 暂无消息"
                lastUpdateTime = System.currentTimeMillis()
                mNotifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                
                val it = Intent(Intent.ACTION_VIEW)
                it.setData("alipays://platformapi/startapp?appId=".toUri())
                val pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(CHANNEL_ID, "🔔 芝麻粒能量提醒", NotificationManager.IMPORTANCE_LOW)
                    notificationChannel.enableLights(false)
                    notificationChannel.enableVibration(false)
                    notificationChannel.setShowBadge(false)
                    mNotifyManager!!.createNotificationChannel(notificationChannel)
                }
                
                builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, android.R.drawable.sym_def_app_icon))
                    .setContentTitle("芝麻粒") // 固定标题
                    .setContentText(statusText) // 第一行内容
                    // .setSubText("芝麻粒") // 小小模块标题
                    .setAutoCancel(false)
                    .setContentIntent(pi)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detailText")) // 展开时显示完整内容
                
                if (BaseModel.enableOnGoing.value) {
                    builder!!.setOngoing(true)
                }
                
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder!!.build())
                isNotificationStarted = true
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 停止通知。 移除通知并停止前台服务。
     */
    @JvmStatic
    fun stop() {
        try {
            val localContext = context ?: return
            
            if (localContext is Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    localContext.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    localContext.stopSelf()
                }
            }
            
            NotificationManagerCompat.from(localContext).cancel(NOTIFICATION_ID)
            mNotifyManager = null
            isNotificationStarted = false
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 更新通知文本。 更新通知的状态文本和详情文本，并发送通知。
     *
     * @param status 要更新的状态文本。
     */
    @JvmStatic
    fun updateStatusText(status: String?) {
        if (context == null) return
        
        try {
            val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
            val finalStatus = if (forestPauseTime > System.currentTimeMillis()) {
                "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行"
            } else {
                status
            }
            
            synchronized(updateLock) {
                statusText = finalStatus ?: ""
            }
            
            sendText(true)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 更新下一次执行时间的文本。
     * 语义说明：
     * nextExecTime > 0  -> 更新并显示（参数以毫秒为准）
     * nextExecTime == 0 -> 清空显示（显示等待或默认）
     * nextExecTime == -1 -> 不改变缓存（只是刷新当前通知显示）
     */
    @JvmStatic
    fun updateNextExecText(nextExecTime: Long) {
        val localContext = context ?: return
        
        try {
            // 规范化：如果传进来的时间看起来像秒（小于 10^12），转换为毫秒
            fun normalizeTime(t: Long): Long {
                if (t <= 0L) return t
                // 1e12 ~ 2001-09-09 in ms; 当前 ms 值大于 1e12, 所以判定小于 1e12 为秒
                return if (t < 1_000_000_000_000L) t * 1000L else t
            }

            val normalized = when (nextExecTime) {
                -1L -> -1L // 表示保持缓存，不覆盖
                else -> normalizeTime(nextExecTime)
            }

            // 并发保护：只在一把锁内读写缓存
            synchronized(updateLock) {
                if (normalized > 0L) {
                    nextExecTimeCache = normalized
                } else if (normalized == 0L) {
                    // 客户端明确要求清空显示
                    nextExecTimeCache = 0L
                } // normalized == -1L -> 保持现有缓存
            }

            // 生成状态文本
            val newStatusText = synchronized(updateLock) {
                if (nextExecTimeCache > 0L) {
                    // 再次确保 TimeUtil 得到的是毫秒
                    val ts = nextExecTimeCache
                    val timeStr = try {
                        TimeUtil.getTimeStr(ts)
                    } catch (e: Exception) {
                        Log.error(TAG, "TimeUtil.getTimeStr 失败，ts=$ts")
                        Log.printStackTrace(e)
                        ""
                    }
                    if (timeStr.isNotEmpty()) "⏰ 下次执行 $timeStr" else "⏰ 下次执行（时间解析失败）"
                } else {
                    "⏰ 等待调度中..."
                }
            }
            
            synchronized(updateLock) {
                statusText = newStatusText
            }

            // 在主线程执行通知刷新（保证 NotificationManager/Builder 操作安全）
            Handler(Looper.getMainLooper()).post {
                try {
                    // 确保 manager 和 builder 可用（尝试懒初始化）
                    var localNotifyManager = mNotifyManager
                    if (localNotifyManager == null) {
                        initNotificationSystem(localContext)
                        localNotifyManager = mNotifyManager
                    }

                    var localBuilder = builder
                    if (localBuilder == null) {
                        createNotificationBuilder(localContext)
                        localBuilder = builder
                    }

                    if (localNotifyManager == null || localBuilder == null) {
                        Log.error(TAG, "无法刷新通知：manager 或 builder 为 null")
                        return@post
                    }

                    // 更新通知内容
                    localBuilder.setContentTitle("芝麻粒") // 固定标题
                    localBuilder.setContentText(statusText) // 第一行内容
                    localBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detailText")) // 展开时显示完整内容

                    // 使用固定 notify id
                    localNotifyManager.notify(NOTIFICATION_ID, localBuilder.build())
                } catch (inner: Exception) {
                    Log.error(TAG, "主线程更新通知失败")
                    Log.printStackTrace(inner)
                    // 尝试重建通知系统
                    resetNotificationSystem(localContext)
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "updateNextExecText 异常")
            Log.printStackTrace(e)
        }
    }

    /**
     * 初始化通知系统
     */
    private fun initNotificationSystem(context: Context) {
        try {
            mNotifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(CHANNEL_ID, "🔔 芝麻粒能量提醒", NotificationManager.IMPORTANCE_LOW)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                notificationChannel.setShowBadge(false)
                mNotifyManager!!.createNotificationChannel(notificationChannel)
            }
            
            val it = Intent(Intent.ACTION_VIEW)
            it.setData("alipays://platformapi/startapp?appId=".toUri())
            val pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            
            builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, android.R.drawable.sym_def_app_icon))
                .setContentTitle("芝麻粒") // 固定标题
                .setContentText(statusText) // 第一行内容
                // .setSubText("芝麻粒") // 小小模块标题
                .setAutoCancel(false)
                .setContentIntent(pi)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detailText")) // 展开时显示完整内容
            
            if (BaseModel.enableOnGoing.value) {
                builder!!.setOngoing(true)
            }
            
            isNotificationStarted = true
        } catch (e: Exception) {
            Log.error(TAG, "初始化通知系统失败")
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 创建通知构建器
     */
    private fun createNotificationBuilder(context: Context) {
        try {
            val it = Intent(Intent.ACTION_VIEW)
            it.setData("alipays://platformapi/startapp?appId=".toUri())
            val pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            
            builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, android.R.drawable.sym_def_app_icon))
                .setContentTitle("芝麻粒") // 固定标题
                .setContentText(statusText) // 第一行内容
                // .setSubText("芝麻粒") // 小小模块标题
                .setAutoCancel(false)
                .setContentIntent(pi)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detailText")) // 展开时显示完整内容
            
            if (BaseModel.enableOnGoing.value) {
                builder!!.setOngoing(true)
            }
        } catch (e: Exception) {
            Log.error(TAG, "创建通知构建器失败")
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 重置通知系统
     */
    private fun resetNotificationSystem(context: Context) {
        try {
            synchronized(updateLock) {
                mNotifyManager = null
                builder = null
                isNotificationStarted = false
            }
            initNotificationSystem(context)
        } catch (e: Exception) {
            Log.error(TAG, "重置通知系统失败")
            Log.printStackTrace(e)
        }
    }

    /**
     * 更新上一次执行的文本。
     *
     * @param content 上一次执行的内容。
     */
    @JvmStatic
    fun updateLastExecText(content: String?) {
        if (context == null) return
        
        try {
            synchronized(updateLock) {
                detailText = "📌 上次执行 " + TimeUtil.getTimeStr(System.currentTimeMillis()) + "\n🌾 " + (content ?: "")
            }
            sendText(false)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 设置状态文本为执行中。
     */
    @JvmStatic
    fun setStatusTextExec() {
        if (context == null) return
        
        try {
            val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)

            synchronized(updateLock) {
                statusText = if (forestPauseTime > System.currentTimeMillis()) {
                    "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行"
                } else {
                    "⚙️ 芝麻粒正在施法中..."
                }
            }
            
            sendText(true)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 设置状态文本为已禁用
     */
    @JvmStatic
    fun setStatusTextDisabled() {
        if (context == null) return
        
        try {
            synchronized(updateLock) {
                statusText = "🚫 芝麻粒已禁用"
            }
            
            sendText(true)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    @JvmStatic
    fun setStatusTextExec(content: String?) {
        updateStatusText("🔥 ${content ?: ""} 运行中...")
    }

    /**
     * 发送文本更新。 更新通知的内容文本，并重新发送通知。
     *
     * @param force 是否强制刷新
     */
    private fun sendText(force: Boolean) {
        val localContext = context ?: return
        
        try {
            if (!force && System.currentTimeMillis() - lastUpdateTime < 500) {
                return
            }
            
            lastUpdateTime = System.currentTimeMillis()
            
            // 确保通知系统可用
            var localNotifyManager = mNotifyManager
            var localBuilder = builder
            
            if (localNotifyManager == null || localBuilder == null) {
                initNotificationSystem(localContext)
                localNotifyManager = mNotifyManager
                localBuilder = builder
                
                if (localNotifyManager == null || localBuilder == null) {
                    Log.error(TAG, "无法发送文本：通知系统未初始化")
                    return
                }
            }
            
            // 更新通知内容
            synchronized(updateLock) {
                localBuilder.setContentTitle("芝麻粒") // 固定标题
                localBuilder.setContentText(statusText) // 第一行内容
                localBuilder.setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detailText")) // 展开时显示完整内容
            }
            
            localNotifyManager.notify(NOTIFICATION_ID, localBuilder.build())
        } catch (e: Exception) {
            Log.error(TAG, "发送文本失败")
            Log.printStackTrace(e)
            // 尝试重置通知系统
            resetNotificationSystem(localContext)
        }
    }

    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    fun sendErrorNotification(title: String?, content: String?) {
        try {
            val localContext = context ?: return
            
            if (!checkPermission(localContext)) return
            
            val errorNotifyManager = localContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(CHANNEL_ID, "‼️ 芝麻粒异常通知", NotificationManager.IMPORTANCE_LOW)
                errorNotifyManager!!.createNotificationChannel(notificationChannel)
            }
            
            val errorBuilder = NotificationCompat.Builder(localContext, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(localContext.resources, android.R.drawable.sym_def_app_icon))
                .setContentTitle(title)
                .setContentText(content)
                // .setSubText("芝麻粒") // 小小模块标题
                .setAutoCancel(true)
            
            if (localContext is Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    NotificationManagerCompat.from(localContext).notify(ERROR_NOTIFICATION_ID, errorBuilder.build())
                } else {
                    localContext.startForeground(ERROR_NOTIFICATION_ID, errorBuilder.build())
                }
            } else {
                NotificationManagerCompat.from(localContext).notify(ERROR_NOTIFICATION_ID, errorBuilder.build())
            }
        } catch (e: Exception) {
            Log.error(TAG, "发送错误通知失败")
            Log.printStackTrace(e)
        }
    }
}