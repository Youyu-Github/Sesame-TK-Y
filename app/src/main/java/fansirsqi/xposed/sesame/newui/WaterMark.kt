package fansirsqi.xposed.sesame.newui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import fansirsqi.xposed.sesame.data.ViewAppInfo.verifyId
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.data.ViewAppInfo.verifuids
import fansirsqi.xposed.sesame.util.TimeUtil
import kotlin.random.Random

class WatermarkView(context: Context) : View(context) {

    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    // 定义白天和黑夜的颜色
    private var dayColor = "#3e273f47".toColorInt()   // 深灰蓝（适合亮色背景）
    private var nightColor = "#808a9a9e".toColorInt() // 浅灰（适合暗色背景）

    private var textLines: List<String> = emptyList()

    var watermarkText: String? = null
        set(_) {
            // 固定前缀
            val prefixLines = mutableListOf(
                "免费模块仅供学习,勿在国内平台传播!!",
                "该版不是官方版,是个人学习使用分支!!",
                "版本号: ${BuildConfig.VERSION}.${BuildConfig.BUILD_TYPE}"
                // "VID: $verifyId"
            )
            // 当前时间
            val suffix = "${TimeUtil.getFormatDateTime()}"
            // UID 列表，如果为空就显示“未载入账号”，否则带索引显示
            val uidLines = if (verifuids.isEmpty()) {
                listOf(
                    "未载入账号",
                    "请启用模块后重启一次支付宝",
                    "确保模块生成对应账号配置"
                )
            } else {
                verifuids.mapIndexed { index, uid ->
                    "UID${index + 1}: $uid"
                }
            }
            val combinedLines = prefixLines + uidLines + suffix
            field = combinedLines.joinToString("\n")
            // [修复一] 移除了对不存在的 rebuildColorCache() 的调用
            textLines = combinedLines
            updateTextColor() // 根据当前模式更新颜色
            invalidate()
        }

    fun setDayNightColors(day: Int, night: Int) {
        dayColor = day
        nightColor = night
        updateTextColor()
    }

    /** 控制整体水印稀疏度（越大越密集，越小越稀疏） */
    private var densityFactor: Float = 2f

    /** 旋转角度 */
    var rotationAngle: Float = -30f

    init {
        watermarkText = watermarkText // 初始化文本
    }

    fun setInfo(tags: List<String>) {
        watermarkText = buildString {
            appendLine("免费模块仅供学习\n勿在国内平台传播,倒卖必死全家!!")
            appendLine(tags.joinToString(" | "))
        }
    }

    fun setWatermarkStyle(color: Int, size: Float) {
        paint.color = color
        paint.textSize = size
        invalidate()
    }

    /** 调整整体密度，默认 1f = 正常，0.5f = 稀疏，2f = 更密 */
    fun setDensity(density: Float = 3f) {
        densityFactor = density.coerceAtLeast(0.1f) // 防止过稀
        invalidate()
    }

    private fun updateTextColor() {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        paint.color = if (isNight) nightColor else dayColor
    }

    private val offsetX = Random.nextInt(-200, 200) // 根据需要调整最大偏移量
    private val offsetY = Random.nextInt(-200, 200)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || textLines.isEmpty()) return

        val maxLineWidth = textLines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val lineHeight = paint.fontSpacing
        val totalTextHeight = lineHeight * textLines.size

        var horizontalSpacing = (maxLineWidth * 1.2f / densityFactor).toInt()
        var verticalSpacing = (totalTextHeight * 1.5f / densityFactor).toInt()

        horizontalSpacing = horizontalSpacing.coerceAtMost(width)
        verticalSpacing = verticalSpacing.coerceAtMost(height)

        canvas.withRotation(rotationAngle) {
            var y = -height.toFloat() + offsetY
            var yIndex = 0 // [修复二] 在此声明并初始化 yIndex
            while (y < height * 2) {
                var x = -width.toFloat() + offsetX
                if (yIndex % 2 == 1) x += horizontalSpacing / 2
                while (x < width * 2) {
                    val baseY = y - totalTextHeight / 2
                    for ((i, line) in textLines.withIndex()) {
                        drawText(line, x, baseY + i * lineHeight, paint)
                    }
                    x += horizontalSpacing
                }
                y += verticalSpacing
                yIndex++
            }
        }
    }

    /**
     * 刷新水印：重新检测日夜模式并重绘
     */
    fun refresh() {
        updateTextColor()
        invalidate()
    }

    companion object {
        // [关键修复] 添加一个固定的TAG，用于识别我们添加的水印视图
        private const val WATERMARK_VIEW_TAG = "fansirsqi_watermark_view_tag"

        @JvmStatic
        @JvmOverloads
        fun install(
            activity: Activity,
            text: String = "",
            dayColor: Int = "#3e273f47".toColorInt(),      // 明确命名：白天色
            nightColor: Int = "#7e8a9a9e".toColorInt(),    // 明确命名：夜晚色
            fontSize: Float = 27f,
            density: Float = 0.9f
        ): WatermarkView {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

            // [关键修复] 在添加新水印前，先查找并移除已存在的旧水印
            val existingWatermark = rootView.findViewWithTag<WatermarkView>(WATERMARK_VIEW_TAG)
            if (existingWatermark != null) {
                rootView.removeView(existingWatermark)
            }

            val watermarkView = WatermarkView(activity).apply {
                // [关键修复] 设置TAG，方便下次查找和移除
                tag = WATERMARK_VIEW_TAG

                // 先设置日夜颜色（必须在 watermarkText 之前！）
                setDayNightColors(day = dayColor, night = nightColor)
                // 再设置文本，内部会调用 updateTextColor()
                watermarkText = text
                paint.textSize = fontSize
                setDensity(density)
            }

            rootView.addView(
                watermarkView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            return watermarkView
        }
    }
}