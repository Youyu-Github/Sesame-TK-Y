package fansirsqi.xposed.sesame.util.maps;

/**
 * 用于保存 VIP 抓包得到的私有数据（按用户隔离）。
 *
 * 每个用户一个 vipdata.json，内容为简单的 key-value 映射，例如：
 * {
 *   "antfishpond_riskToken": "xxxx"
 * }
 */
public class VipDataIdMap extends IdMapManager {

    @Override
    protected String thisFileName() {
        // 仅存放通过抓包获取的 VIP 相关数据
        return "vipdata.json";
    }
}