package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.ExtendFunctionItem
import fansirsqi.xposed.sesame.newui.WatermarkView
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.ui.widget.ExtendFunctionAdapter
import fansirsqi.xposed.sesame.util.Detector.getApi
import fansirsqi.xposed.sesame.util.FansirsqiUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil

/**
 * 扩展功能页面
 */
class ExtendActivity : BaseActivity() {
    private val TAG = ExtendActivity::class.java.simpleName
    private var debugTips: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var extendFunctionAdapter: ExtendFunctionAdapter
    private val extendFunctions = mutableListOf<ExtendFunctionItem>()

    /**
     * 初始化Activity
     *
     * @param savedInstanceState 保存的实例状态
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extend) // 设置布局文件
        debugTips = getString(R.string.debug_tips)
        baseTitle = getString(R.string.extended_func)
        setupRecyclerView()
        populateExtendFunctions()
        WatermarkView.install(this)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView_extend_functions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        extendFunctionAdapter = ExtendFunctionAdapter(extendFunctions)
        recyclerView.adapter = extendFunctionAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateExtendFunctions() {
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.query_the_remaining_amount_of_saplings)) {
                sendItemsBroadcast("getTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_new_items_on_saplings)) {
                sendItemsBroadcast("getNewTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_regions)) {
                sendItemsBroadcast("queryAreaTrees")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_items)) {
                sendItemsBroadcast("getUnlockTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.clear_photo)) {
                // 取出当前条数
                val currentCount = DataStore
                    .getOrCreate("guangPanPhoto", object : TypeReference<List<Map<String, String>>>() {})
                    .size

                AlertDialog.Builder(this)
                    .setTitle(R.string.clear_photo)
                    .setMessage("确认清空 $currentCount 组光盘行动图片？")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        // 直接从持久化里删掉 key
                        DataStore.remove("guangPanPhoto")
                        ToastUtil.showToast(this, "光盘行动图片清空成功")
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        )
        //调试功能往里加
        if (BuildConfig.DEBUG) {
            // 新增：RPC 调试入口（Method + requestData）
            extendFunctions.add(
                ExtendFunctionItem("RPC调试") {
                    // 构建包含两个输入框的自定义视图
                    val container = android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(48, 24, 48, 0)
                    }
                    val etMethod = EditText(this).apply {
                        hint = "Method 例如：alipay.antforest.forest.h5.queryMiscInfo"
                        setText("")
                    }
                    val etRequestData = EditText(this).apply {
                        hint = "requestData 例如：[{}]"
                        setText("")
                        minLines = 4
                        maxLines = 8
                        setHorizontallyScrolling(false)
                    }
                    container.addView(etMethod)
                    container.addView(etRequestData)
                    AlertDialog.Builder(this)
                        .setTitle("RPC调试")
                        .setView(container)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val method = etMethod.text?.toString()?.trim().orEmpty()
                            val requestData = etRequestData.text?.toString()?.trim().orEmpty()
                            if (method.isEmpty() || requestData.isEmpty()) {
                                ToastUtil.showToast(this, "Method 和 requestData 不能为空")
                                return@setPositiveButton
                            }
                            // 通过广播交由支付宝进程执行，避免本进程无 rpcBridge 的问题
                            val intent = Intent("com.eg.android.AlipayGphone.sesame.rpctest")
                            intent.putExtra("method", method)
                            intent.putExtra("data", requestData)
                            intent.putExtra("type", "Rpc")
                            sendBroadcast(intent)
                            ToastUtil.showToast(this, "已发送，请在调试日志查看结果")
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )
            extendFunctions.add(
                ExtendFunctionItem("写入光盘") {
                    AlertDialog.Builder(this)
                        .setTitle("Test")
                        .setMessage("xxxx")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val newPhotoEntry = mapOf(
                                "before" to "before${FansirsqiUtil.getRandomString(10)}",
                                "after" to "after${FansirsqiUtil.getRandomString(10)}"
                            )

                            // 取出已有列表（空时返回空 MutableList）
                            val existingPhotos = DataStore.getOrCreate(
                                "guangPanPhoto",
                                object : TypeReference<MutableList<Map<String, String>>>() {})
                            existingPhotos.add(newPhotoEntry)

                            // 写回持久化
                            DataStore.put("guangPanPhoto", existingPhotos)
                            ToastUtil.showToast(this, "写入成功$newPhotoEntry")
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )

            //我想在这加一个编辑框，里面支持输入文字，下面的展示随机光盘的字段从编辑框里面取

            extendFunctions.add(
                ExtendFunctionItem("获取DataStore字段") {
                    val inputEditText = EditText(this)
                    AlertDialog.Builder(this)
                        .setTitle("输入字段Key")
                        .setView(inputEditText)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val key = inputEditText.text.toString()
                            val value: Any? = try {
                                // 若不知道类型，可先按 Map 读；失败时再按 String 读
                                DataStore.getOrCreate(key, object : TypeReference<Map<*, *>>() {})
                            } catch (e: Exception) {
                                DataStore.getOrCreate(key, object : TypeReference<String>() {})
                            }
                            ToastUtil.showToast(this, "$value \n输入内容: $key")
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )


            extendFunctions.add(
                ExtendFunctionItem("获取BaseUrl") {
                    val inputEditText = EditText(this)
                    AlertDialog.Builder(this)
                        .setTitle("请输入Key")
                        .setView(inputEditText)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val inputText = inputEditText.text.toString()
                            Log.debug(TAG, "获取BaseUrl：$inputText")
                            val key = inputText.toIntOrNull(16)  // 支持输入 0x11 这样的十六进制
                            Log.debug(TAG, "获取BaseUrl key：$key")
                            if (key != null) {
                                val output = getApi(key)
                                ToastUtil.showToast(this, "$output \n输入内容: $inputText")
                            } else {
                                ToastUtil.showToast(this, "输入内容: $inputText , 请输入正确的十六进制数字")
                            }

                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )
        }
        extendFunctionAdapter.notifyDataSetChanged()
    }

    /**
     * 发送广播事件
     *
     * @param type 广播类型
     */
    private fun sendItemsBroadcast(type: String) {
        val intent = Intent("com.eg.android.AlipayGphone.sesame.rpctest")
        intent.putExtra("method", "")
        intent.putExtra("data", "")
        intent.putExtra("type", type)
        sendBroadcast(intent) // 发送广播
        Log.debug(TAG, "扩展工具主动调用广播查询📢：$type")
    }
}
