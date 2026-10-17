package tw.cozy.furniture.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 模型中的一個長方體零件（一個方塊展示實體）。
 *
 * @param tex      外觀
 * @param center   中心點（模型座標：原點為錨點方塊底部中心，+Z 為傢俱正面）
 * @param size     長寬高
 * @param rotation 以中心為軸的自身旋轉
 * @param lit      燈具開啟時改用的外觀（並以最大亮度顯示）；null 表示不是發光零件
 */
public record Part(Tex tex, Vector3f center, Vector3f size, Quaternionf rotation, @Nullable Tex lit) {

    public static Part box(Tex tex, float x0, float y0, float z0, float x1, float y1, float z1) {
        return new Part(tex,
                new Vector3f((x0 + x1) / 2f, (y0 + y1) / 2f, (z0 + z1) / 2f),
                new Vector3f(x1 - x0, y1 - y0, z1 - z0),
                new Quaternionf(), null);
    }

    public static Part rot(Tex tex, float cx, float cy, float cz, float sx, float sy, float sz, Quaternionf rotation) {
        return new Part(tex, new Vector3f(cx, cy, cz), new Vector3f(sx, sy, sz), rotation, null);
    }

    /** 標記為燈具的發光零件。 */
    public Part glowing(Tex litTex) {
        return new Part(tex, center, size, rotation, litTex);
    }
}
