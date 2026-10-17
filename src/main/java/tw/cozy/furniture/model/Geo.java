package tw.cozy.furniture.model;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 座標旋轉工具，與展示實體在客戶端的旋轉方式一致。 */
public final class Geo {

    private Geo() {
    }

    /** 把模型座標依傢俱朝向（yaw）旋轉成世界偏移量。 */
    public static Vector3f rotY(Vector3f v, float yawDeg) {
        double r = Math.toRadians(yawDeg);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        return new Vector3f(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    public static int[] rotBlock(int[] b, float yawDeg) {
        Vector3f v = rotY(new Vector3f(b[0], b[1], b[2]), yawDeg);
        return new int[]{Math.round(v.x), Math.round(v.y), Math.round(v.z)};
    }

    /**
     * 計算零件的展示變換。
     *
     * @param part     零件
     * @param swing    額外套用的整體旋轉（鞦韆擺動），靜態零件傳入單位四元數
     * @param relative 零件中心相對於實體位置的偏移
     */
    public static Transformation transform(Part part, Quaternionf swing, Vector3f relative) {
        Quaternionf left = new Quaternionf(swing).mul(part.rotation());
        Vector3f half = new Vector3f(part.size()).mul(0.5f).rotate(left);
        Vector3f translation = new Vector3f(relative).rotate(swing).sub(half);
        return new Transformation(translation, left, new Vector3f(part.size()), new Quaternionf());
    }

    public static Quaternionf yRot(float deg) {
        return new Quaternionf().rotationY((float) Math.toRadians(deg));
    }

    public static Quaternionf xRot(float deg) {
        return new Quaternionf().rotationX((float) Math.toRadians(deg));
    }
}
