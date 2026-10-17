package tw.cozy.furniture.model;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** 一件傢俱的完整定義：外觀零件、佔用空間、碰撞箱與功能。 */
public final class Model {

    /** 互動碰撞箱（Interaction 實體），位於某個佔用方塊的中心。 */
    public record Hitbox(int bx, int by, int bz, float width, float height) {
    }

    public final List<Part> parts = new ArrayList<>();
    /** 鞦韆的擺動零件（座標同樣是模型座標）。 */
    public final List<Part> moving = new ArrayList<>();
    /** 座位表面位置。鞦韆的座位會跟著擺動。 */
    public final List<Vector3f> seats = new ArrayList<>();
    /** 實體方塊（開啟碰撞時放置屏障）。 */
    public final List<int[]> solid = new ArrayList<>();
    /** 需要淨空但不放屏障的方塊。 */
    public final List<int[]> clear = new ArrayList<>();
    public final List<Hitbox> hitboxes = new ArrayList<>();

    public float tableTop = -1f;
    public boolean bookshelf;
    public boolean swing;
    public Vector3f pivot = new Vector3f();
    public float ropeLength = 2f;
    /** 燈具的光源方塊位置（放置 LIGHT 方塊）；null 表示不是燈。 */
    public int[] lightBlock;
    /** 床：可睡覺、設定重生點。 */
    public boolean bed;
    /** 可以直接擺在桌面上（例如檯燈）。 */
    public boolean onTable;
    /** 無人時的搖晃倍率（樹上的鞦韆風比較大）。 */
    public float windiness = 1f;

    public boolean isTable() {
        return tableTop > 0;
    }

    public boolean isLamp() {
        return lightBlock != null;
    }

    public boolean isSeat() {
        return !seats.isEmpty();
    }

    public List<int[]> footprint() {
        List<int[]> all = new ArrayList<>(solid);
        all.addAll(clear);
        return all;
    }

    public boolean isSolid(int bx, int by, int bz) {
        for (int[] b : solid) {
            if (b[0] == bx && b[1] == by && b[2] == bz) {
                return true;
            }
        }
        return false;
    }

    Model add(Part part) {
        parts.add(part);
        return this;
    }

    Model solid(int x, int y, int z) {
        solid.add(new int[]{x, y, z});
        return this;
    }

    Model clear(int x, int y, int z) {
        clear.add(new int[]{x, y, z});
        return this;
    }

    Model hitbox(int x, int y, int z, float width, float height) {
        hitboxes.add(new Hitbox(x, y, z, width, height));
        return this;
    }

    Model seat(float x, float y, float z) {
        seats.add(new Vector3f(x, y, z));
        return this;
    }
}
