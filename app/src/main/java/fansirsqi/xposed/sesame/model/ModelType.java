package fansirsqi.xposed.sesame.model;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

//不用考虑Getter 与kotlin 的兼容性
public enum ModelType {
    NORMAL(0, "普通模块"),
    TASK(1, "任务模块"),
    ;
    // 优化1: 使用 int 替代 Integer，避免拆装箱
    private final int code;
    private final String name;

    ModelType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }


    // 优化2: 缓存 values()，避免 getByCode 遍历时如果手动 values() 造成的数组克隆
    // 但由于我们使用了 Map 查找，这里主要是为了构建 Map 时更高效
    private static final Map<Integer, ModelType> MAP = new HashMap<>();


    static {
        for (ModelType value : ModelType.values()) {
            MAP.put(value.getCode(), value);
        }
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 标识码
     * @return 对应的枚举，如果未找到则返回 null
     */
    @Nullable
    public static ModelType getByCode(int code) {
        return MAP.get(code);
    }
}
