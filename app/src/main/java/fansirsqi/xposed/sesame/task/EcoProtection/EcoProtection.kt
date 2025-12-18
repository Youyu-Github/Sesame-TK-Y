package fansirsqi.xposed.sesame.task.EcoProtection

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.AreaCode
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EcoProtection : ModelTask() {
    override fun getName(): String {
        return "生态保护"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "EcoProtection.png"
    }

    private var ancientTreeOnlyWeek: BooleanModelField? = null
    private var ancientTreeCityCodeList: SelectModelField? = null
    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(BooleanModelField("ancientTreeOnlyWeek", "仅星期一、三、五运行保护古树", false).also { ancientTreeOnlyWeek = it })
        modelFields.addField(
            SelectModelField(
                "ancientTreeCityCodeList",
                "古树区划代码列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AreaCode.getList() }).also { ancientTreeCityCodeList = it })
        return modelFields
    }

    override fun check(): Boolean {
        if (!TaskCommon.IS_ENERGY_TIME && TaskCommon.IS_AFTER_8AM) {
            if (!ancientTreeOnlyWeek!!.value) {
                return true
            }
            val sdf_week = SimpleDateFormat("EEEE", Locale.getDefault())
            val week = sdf_week.format(Date())
            return "星期一" == week || "星期三" == week || "星期五" == week
        }
        return false
    }

    override fun run() {
        try {
            Log.record(TAG,"开始执行"+getName())
            ancientTree(ancientTreeCityCodeList!!.value)
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "结束执行" + getName())
        }
    }

    companion object {
        private val TAG: String = EcoProtection::class.java.getSimpleName()
        private fun ancientTree(ancientTreeCityCodeList: MutableCollection<String?>) {
            try {
                for (cityCode in ancientTreeCityCodeList) {
                    if (!Status.canAncientTreeToday(cityCode)) continue
                    ancientTreeProtect(cityCode)
                    GlobalThreadPools.sleep(1000L)
                }
            } catch (th: Throwable) {
                Log.runtime(TAG, "ancientTree err:")
                Log.printStackTrace(TAG, th)
            }
        }

        private fun ancientTreeProtect(cityCode: String?) {
            try {
                val jo = JSONObject(EcoProtectionRpcCall.homePage(cityCode))
                if (ResChecker.checkRes(TAG, jo)) {
                    val data = jo.getJSONObject("data")
                    if (!data.has("districtBriefInfoList")) {
                        return
                    }
                    val districtBriefInfoList = data.getJSONArray("districtBriefInfoList")
                    for (i in 0..<districtBriefInfoList.length()) {
                        val districtBriefInfo = districtBriefInfoList.getJSONObject(i)
                        val userCanProtectTreeNum = districtBriefInfo.optInt("userCanProtectTreeNum", 0)
                        if (userCanProtectTreeNum < 1) continue
                        val districtInfo = districtBriefInfo.getJSONObject("districtInfo")
                        val districtCode = districtInfo.getString("districtCode")
                        districtDetail(districtCode)
                        GlobalThreadPools.sleep(1000L)
                    }
                    Status.ancientTreeToday(cityCode)
                }
            } catch (th: Throwable) {
                Log.runtime(TAG, "ancientTreeProtect err:")
                Log.printStackTrace(TAG, th)
            }
        }

        private fun districtDetail(districtCode: String?) {
            try {
                var jo = JSONObject(EcoProtectionRpcCall.districtDetail(districtCode))
                if (ResChecker.checkRes(TAG, jo)) {
                    var data = jo.getJSONObject("data")
                    if (!data.has("ancientTreeList")) {
                        return
                    }
                    val districtInfo = data.getJSONObject("districtInfo")
                    var cityCode = districtInfo.getString("cityCode")
                    val cityName = districtInfo.getString("cityName")
                    val districtName = districtInfo.getString("districtName")
                    val ancientTreeList = data.getJSONArray("ancientTreeList")
                    for (i in 0..<ancientTreeList.length()) {
                        val ancientTreeItem = ancientTreeList.getJSONObject(i)
                        if (ancientTreeItem.getBoolean("hasProtected")) continue
                        val ancientTreeControlInfo = ancientTreeItem.getJSONObject("ancientTreeControlInfo")
                        val quota = ancientTreeControlInfo.optInt("quota", 0)
                        val useQuota = ancientTreeControlInfo.optInt("useQuota", 0)
                        if (quota <= useQuota) continue
                        val itemId = ancientTreeItem.getString("projectId")
                        val ancientTreeDetail = JSONObject(EcoProtectionRpcCall.projectDetail(itemId, cityCode))
                        if (ResChecker.checkRes(TAG, ancientTreeDetail)) {
                            data = ancientTreeDetail.getJSONObject("data")
                            if (data.getBoolean("canProtect")) {
                                val currentEnergy = data.getInt("currentEnergy")
                                val ancientTree = data.getJSONObject("ancientTree")
                                val activityId = ancientTree.getString("activityId")
                                val projectId = ancientTree.getString("projectId")
                                val ancientTreeInfo = ancientTree.getJSONObject("ancientTreeInfo")
                                val name = ancientTreeInfo.getString("name")
                                val age = ancientTreeInfo.getInt("age")
                                val protectExpense = ancientTreeInfo.getInt("protectExpense")
                                cityCode = ancientTreeInfo.getString("cityCode")
                                if (currentEnergy < protectExpense) break
                                GlobalThreadPools.sleep(200)
                                jo = JSONObject(EcoProtectionRpcCall.protect(activityId, projectId, cityCode))
                                if (ResChecker.checkRes(TAG, jo)) {
                                    Log.forest(
                                        ("保护古树🎐[" + cityName + "-" + districtName
                                                + "]#" + age + "年" + name + ",消耗能量" + protectExpense + "g")
                                    )
                                } else {
                                    Log.record(jo.getString("resultDesc"))
                                    Log.runtime(jo.toString())
                                }
                            }
                        } else {
                            Log.record(jo.getString("resultDesc"))
                            Log.runtime(ancientTreeDetail.toString())
                        }
                        GlobalThreadPools.sleep(500L)
                    }
                }
            } catch (th: Throwable) {
                Log.runtime(TAG, "districtDetail err:")
                Log.printStackTrace(TAG, th)
            }
        }
    }
}
