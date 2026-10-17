package tw.cozy.furniture.model;

import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static tw.cozy.furniture.model.Part.box;
import static tw.cozy.furniture.model.Part.rot;

/**
 * 所有傢俱的外觀設計。
 * <p>
 * 座標系：原點 = 錨點方塊「底部中心」，+Y 向上，+Z 為傢俱正面（朝向放置者），
 * 一個方塊的範圍是 x、z ∈ [-0.5, 0.5]。
 */
public final class Models {

    private Models() {
    }

    // ─────────────────────────── 桌子 ───────────────────────────

    /** 小茶几：矮桌，附下層置物板。 */
    public static Model smallTable() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.44f, 0.42f, -0.44f, 0.44f, 0.49f, 0.44f));
        m.add(box(Tex.STRIPPED, -0.38f, 0.37f, -0.38f, 0.38f, 0.42f, 0.38f));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                float x = sx * 0.34f;
                float z = sz * 0.34f;
                m.add(box(Tex.STRIPPED, x - 0.04f, 0f, z - 0.04f, x + 0.04f, 0.37f, z + 0.04f));
            }
        }
        m.add(box(Tex.PLANKS, -0.34f, 0.1f, -0.34f, 0.34f, 0.14f, 0.34f));
        m.tableTop = 0.49f;
        m.solid(0, 0, 0).hitbox(0, 0, 0, 0.9f, 0.5f);
        return m;
    }

    /** 方桌：標準高度的四腳桌，有桌裙。 */
    public static Model squareTable() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.5f, 0.8f, -0.5f, 0.5f, 0.88f, 0.5f));
        addLegsAndApron(m, -0.4f, 0.4f, -0.4f, 0.4f);
        m.tableTop = 0.88f;
        m.solid(0, 0, 0).hitbox(0, 0, 0, 1f, 0.9f);
        return m;
    }

    /** 長桌：佔兩格的大桌子，中間有橫撐。 */
    public static Model largeTable() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.5f, 0.8f, -0.5f, 0.5f, 0.88f, 0.5f));
        m.add(box(Tex.PLANKS, 0.5f, 0.8f, -0.5f, 1.5f, 0.88f, 0.5f));
        addLegsAndApron(m, -0.4f, 1.4f, -0.4f, 0.4f);
        m.add(box(Tex.STRIPPED_X, -0.35f, 0.2f, -0.035f, 1.35f, 0.28f, 0.035f));
        m.add(box(Tex.STRIPPED_Z, -0.43f, 0.2f, -0.35f, -0.37f, 0.28f, 0.35f));
        m.add(box(Tex.STRIPPED_Z, 1.37f, 0.2f, -0.35f, 1.43f, 0.28f, 0.35f));
        m.tableTop = 0.88f;
        m.solid(0, 0, 0).solid(1, 0, 0);
        m.hitbox(0, 0, 0, 1f, 0.9f).hitbox(1, 0, 0, 1f, 0.9f);
        return m;
    }

    /** 圓桌：以木條拼出圓形桌面，單柱腳 + 十字底座。 */
    public static Model roundTable() {
        Model m = new Model();
        float r = 0.48f;
        int strips = 9;
        float w = 2 * 0.468f / strips;
        for (int i = 0; i < strips; i++) {
            float z0 = -0.468f + i * w;
            float zc = z0 + w / 2f;
            float half = (float) Math.sqrt(r * r - zc * zc);
            m.add(box(Tex.PLANKS, -half, 0.81f, z0, half, 0.88f, z0 + w));
        }
        m.add(box(Tex.STRIPPED, -0.24f, 0.76f, -0.24f, 0.24f, 0.81f, 0.24f));
        m.add(box(Tex.STRIPPED, -0.09f, 0.1f, -0.09f, 0.09f, 0.76f, 0.09f));
        m.add(rot(Tex.PLANKS, 0f, 0.05f, 0f, 0.78f, 0.1f, 0.16f, Geo.yRot(45)));
        m.add(rot(Tex.PLANKS, 0f, 0.045f, 0f, 0.16f, 0.09f, 0.78f, Geo.yRot(45)));
        m.tableTop = 0.88f;
        m.solid(0, 0, 0).hitbox(0, 0, 0, 1f, 0.9f);
        return m;
    }

    private static void addLegsAndApron(Model m, float x0, float x1, float z0, float z1) {
        float legTop = 0.8f;
        float t = 0.05f;
        for (float x : new float[]{x0, x1}) {
            for (float z : new float[]{z0, z1}) {
                m.add(box(Tex.STRIPPED, x - t, 0f, z - t, x + t, legTop, z + t));
            }
        }
        // 桌裙
        m.add(box(Tex.STRIPPED_X, x0 + t, 0.7f, z1 - 0.02f, x1 - t, legTop, z1 + 0.02f));
        m.add(box(Tex.STRIPPED_X, x0 + t, 0.7f, z0 - 0.02f, x1 - t, legTop, z0 + 0.02f));
        m.add(box(Tex.STRIPPED_Z, x0 - 0.02f, 0.7f, z0 + t, x0 + 0.02f, legTop, z1 - t));
        m.add(box(Tex.STRIPPED_Z, x1 - 0.02f, 0.7f, z0 + t, x1 + 0.02f, legTop, z1 - t));
    }

    // ─────────────────────────── 座椅 ───────────────────────────

    /** 椅子：有橫條靠背的餐椅。 */
    public static Model chair() {
        Model m = new Model();
        float l = 0.33f;
        float t = 0.045f;
        for (int sx = -1; sx <= 1; sx += 2) {
            m.add(box(Tex.STRIPPED, sx * l - t, 0f, l - t, sx * l + t, 0.42f, l + t));
            // 後腳一路延伸成靠背立柱
            m.add(box(Tex.STRIPPED, sx * l - t, 0f, -l - t, sx * l + t, 1.22f, -l + t));
        }
        m.add(box(Tex.PLANKS, -0.4f, 0.42f, -0.4f, 0.4f, 0.5f, 0.4f));
        m.add(box(Tex.PLANKS, -l + t, 0.68f, -0.37f, l - t, 0.8f, -0.33f));
        m.add(box(Tex.PLANKS, -l + t, 0.9f, -0.37f, l - t, 1.02f, -0.33f));
        m.add(box(Tex.PLANKS, -0.4f, 1.13f, -0.4f, 0.4f, 1.25f, -0.3f));
        // 椅腳橫撐
        m.add(box(Tex.STRIPPED_Z, -l - 0.025f, 0.14f, -l + t, -l + 0.025f, 0.19f, l - t));
        m.add(box(Tex.STRIPPED_Z, l - 0.025f, 0.14f, -l + t, l + 0.025f, 0.19f, l - t));
        m.seat(0f, 0.5f, 0.03f);
        m.clear(0, 0, 0).clear(0, 1, 0);
        m.hitbox(0, 0, 0, 0.85f, 1.25f);
        return m;
    }

    /** 凳子：外八腳的小圓凳。 */
    public static Model stool() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.29f, 0.56f, -0.29f, 0.29f, 0.64f, 0.29f));
        m.add(rot(Tex.PLANKS, 0f, 0.6f, 0f, 0.58f, 0.07f, 0.58f, Geo.yRot(45)));
        float a = 0.12f;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                Quaternionf q = new Quaternionf().rotateZ(sx * a).rotateX(-sz * a);
                m.add(rot(Tex.STRIPPED, sx * 0.235f, 0.28f, sz * 0.235f, 0.07f, 0.6f, 0.07f, q));
            }
        }
        m.add(box(Tex.STRIPPED_X, -0.26f, 0.18f, 0.235f, 0.26f, 0.22f, 0.275f));
        m.add(box(Tex.STRIPPED_X, -0.26f, 0.18f, -0.275f, 0.26f, 0.22f, -0.235f));
        m.add(box(Tex.STRIPPED_Z, 0.235f, 0.18f, -0.235f, 0.275f, 0.22f, 0.235f));
        m.add(box(Tex.STRIPPED_Z, -0.275f, 0.18f, -0.235f, -0.235f, 0.22f, 0.235f));
        m.seat(0f, 0.64f, 0f);
        m.clear(0, 0, 0);
        m.hitbox(0, 0, 0, 0.7f, 0.66f);
        return m;
    }

    /** 長椅：兩人座公園長椅，含扶手與靠背。 */
    public static Model bench() {
        Model m = new Model();
        // 座面木條
        m.add(box(Tex.PLANKS, -0.45f, 0.42f, 0.12f, 1.45f, 0.48f, 0.27f));
        m.add(box(Tex.PLANKS, -0.45f, 0.42f, -0.05f, 1.45f, 0.48f, 0.09f));
        m.add(box(Tex.PLANKS, -0.45f, 0.42f, -0.22f, 1.45f, 0.48f, -0.08f));
        for (float x : new float[]{-0.38f, 1.38f}) {
            m.add(box(Tex.LOG, x - 0.045f, 0f, 0.17f, x + 0.045f, 0.68f, 0.26f));
            m.add(box(Tex.LOG, x - 0.045f, 0f, -0.3f, x + 0.045f, 1.12f, -0.21f));
            m.add(box(Tex.STRIPPED_Z, x - 0.035f, 0.3f, -0.21f, x + 0.035f, 0.38f, 0.17f));
            // 扶手
            m.add(box(Tex.PLANKS, x - 0.06f, 0.68f, -0.21f, x + 0.06f, 0.73f, 0.32f));
        }
        // 靠背
        m.add(box(Tex.PLANKS, -0.335f, 0.6f, -0.29f, 1.335f, 0.7f, -0.23f));
        m.add(box(Tex.PLANKS, -0.335f, 0.8f, -0.29f, 1.335f, 0.9f, -0.23f));
        m.add(box(Tex.PLANKS, -0.43f, 1.0f, -0.31f, 1.43f, 1.1f, -0.2f));
        m.seat(0f, 0.48f, 0.04f).seat(1f, 0.48f, 0.04f);
        m.clear(0, 0, 0).clear(1, 0, 0).clear(0, 1, 0).clear(1, 1, 0);
        m.hitbox(0, 0, 0, 1f, 1.1f).hitbox(1, 0, 0, 1f, 1.1f);
        return m;
    }

    /** 沙發：布面沙發，寬度 1 = 單人沙發、2 = 雙人沙發。 */
    public static Model sofa(int width) {
        Model m = new Model();
        float x0 = -0.5f;
        float x1 = width - 0.5f;
        // 椅腳
        for (float x : new float[]{x0 + 0.1f, x1 - 0.1f}) {
            for (float z : new float[]{-0.34f, 0.34f}) {
                m.add(box(Tex.STRIPPED, x - 0.04f, 0f, z - 0.04f, x + 0.04f, 0.12f, z + 0.04f));
            }
        }
        // 底座、靠背、扶手
        m.add(box(Tex.FABRIC, x0 + 0.04f, 0.12f, -0.42f, x1 - 0.04f, 0.38f, 0.42f));
        m.add(box(Tex.FABRIC, x0 + 0.04f, 0.38f, -0.44f, x1 - 0.04f, 1.0f, -0.22f));
        m.add(box(Tex.FABRIC, x0, 0.38f, -0.42f, x0 + 0.14f, 0.72f, 0.44f));
        m.add(box(Tex.FABRIC, x1 - 0.14f, 0.38f, -0.42f, x1, 0.72f, 0.44f));
        // 每個座位一組坐墊與靠墊
        for (int i = 0; i < width; i++) {
            float a = i == 0 ? x0 + 0.15f : i - 0.48f;
            float b = i == width - 1 ? x1 - 0.15f : i + 0.48f;
            m.add(box(Tex.FABRIC, a, 0.38f, -0.22f, b, 0.53f, 0.4f));
            m.add(box(Tex.FABRIC, a + 0.02f, 0.53f, -0.25f, b - 0.02f, 0.92f, -0.13f));
            m.seat(i, 0.53f, 0.1f);
            m.clear(i, 0, 0);
            m.hitbox(i, 0, 0, 1f, 1f);
        }
        return m;
    }

    /** 床：一格寬、兩格長，有床頭板、床墊、棉被與枕頭。 */
    public static Model bed() {
        Model m = new Model();
        Tex white = Tex.fixed(Material.WHITE_WOOL);
        // 床腳
        for (float x : new float[]{-0.42f, 0.42f}) {
            for (float z : new float[]{-1.32f, 0.32f}) {
                m.add(box(Tex.STRIPPED, x - 0.05f, 0f, z - 0.05f, x + 0.05f, 0.12f, z + 0.05f));
            }
        }
        m.add(box(Tex.PLANKS, -0.47f, 0.12f, -1.38f, 0.47f, 0.3f, 0.38f));
        m.add(box(Tex.PLANKS, -0.5f, 0f, -1.5f, 0.5f, 0.95f, -1.38f));
        m.add(box(Tex.PLANKS, -0.5f, 0.85f, -1.52f, 0.5f, 1.0f, -1.36f));
        m.add(box(Tex.PLANKS, -0.5f, 0f, 0.38f, 0.5f, 0.55f, 0.5f));
        // 床墊、棉被、反折、枕頭
        m.add(box(white, -0.45f, 0.3f, -1.38f, 0.45f, 0.48f, 0.38f));
        m.add(box(Tex.FABRIC, -0.47f, 0.32f, -0.75f, 0.47f, 0.5f, 0.385f));
        m.add(box(white, -0.475f, 0.33f, -0.83f, 0.475f, 0.51f, -0.73f));
        m.add(box(white, -0.34f, 0.48f, -1.35f, 0.34f, 0.6f, -1.06f));
        m.bed = true;
        m.seat(0f, 0.5f, -0.35f);
        m.clear(0, 0, 0).clear(0, 0, -1);
        m.hitbox(0, 0, 0, 1f, 0.6f).hitbox(0, 0, -1, 1f, 1f);
        return m;
    }

    // ─────────────────────────── 燈具 ───────────────────────────

    /** 燈罩：上下兩圈開口的布罩，開燈時發光。 */
    private static void shade(Model m, float y0, float yMid, float y1, float lower, float upper) {
        ring(m, lower, y0, yMid);
        ring(m, upper, yMid, y1);
    }

    private static void ring(Model m, float half, float y0, float y1) {
        float t = 0.02f;
        m.add(box(Tex.FABRIC, -half, y0, half - t, half, y1, half).glowing(Tex.FABRIC));
        m.add(box(Tex.FABRIC, -half, y0, -half, half, y1, -half + t).glowing(Tex.FABRIC));
        m.add(box(Tex.FABRIC, -half, y0, -half + t, -half + t, y1, half - t).glowing(Tex.FABRIC));
        m.add(box(Tex.FABRIC, half - t, y0, -half + t, half, y1, half - t).glowing(Tex.FABRIC));
    }

    private static final Tex BULB_OFF = Tex.fixed(Material.WHITE_CONCRETE);
    private static final Tex BULB_ON = Tex.fixed(Material.SHROOMLIGHT);

    /** 檯燈：小巧的桌上燈，可以直接擺在桌面上。右鍵開關。 */
    public static Model tableLamp() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.13f, 0f, -0.13f, 0.13f, 0.04f, 0.13f));
        m.add(box(Tex.STRIPPED, -0.025f, 0.04f, -0.025f, 0.025f, 0.3f, 0.025f));
        m.add(box(BULB_OFF, -0.055f, 0.28f, -0.055f, 0.055f, 0.39f, 0.055f).glowing(BULB_ON));
        shade(m, 0.3f, 0.42f, 0.5f, 0.17f, 0.13f);
        m.lightBlock = new int[]{0, 0, 0};
        m.onTable = true;
        m.clear(0, 0, 0);
        m.hitbox(0, 0, 0, 0.4f, 0.52f);
        return m;
    }

    /** 落地燈：兩格高的立燈。右鍵開關。 */
    public static Model floorLamp() {
        Model m = new Model();
        m.add(box(Tex.PLANKS, -0.2f, 0f, -0.2f, 0.2f, 0.05f, 0.2f));
        m.add(rot(Tex.PLANKS, 0f, 0.03f, 0f, 0.4f, 0.06f, 0.4f, Geo.yRot(45)));
        m.add(box(Tex.STRIPPED, -0.03f, 0.06f, -0.03f, 0.03f, 1.45f, 0.03f));
        m.add(box(BULB_OFF, -0.065f, 1.38f, -0.065f, 0.065f, 1.52f, 0.065f).glowing(BULB_ON));
        shade(m, 1.4f, 1.6f, 1.75f, 0.27f, 0.2f);
        m.lightBlock = new int[]{0, 1, 0};
        m.clear(0, 0, 0).clear(0, 1, 0);
        m.hitbox(0, 0, 0, 0.55f, 1.78f);
        return m;
    }

    // ─────────────────────────── 書櫃 ───────────────────────────

    /** 書架高度：三層，從上到下，每層的底板頂面高度。 */
    public static final float[] SHELF_BASES = {1.36f, 0.74f, 0.12f};
    public static final float SHELF_INNER = 0.44f;

    /** 書櫃：兩格高、三層開放式書架，放入的書會實際顯示在架上。 */
    public static Model bookshelf() {
        Model m = new Model();
        float front = 0.3f;
        // 側板
        m.add(box(Tex.PLANKS, -0.5f, 0f, -0.5f, -SHELF_INNER, 1.92f, front));
        m.add(box(Tex.PLANKS, SHELF_INNER, 0f, -0.5f, 0.5f, 1.92f, front));
        // 背板
        m.add(box(Tex.STRIPPED, -SHELF_INNER, 0f, -0.5f, SHELF_INNER, 1.92f, -0.45f));
        // 底板、層板
        m.add(box(Tex.PLANKS, -SHELF_INNER, 0f, -0.45f, SHELF_INNER, 0.12f, front));
        m.add(box(Tex.PLANKS, -SHELF_INNER, 0.68f, -0.45f, SHELF_INNER, 0.74f, front));
        m.add(box(Tex.PLANKS, -SHELF_INNER, 1.30f, -0.45f, SHELF_INNER, 1.36f, front));
        // 頂板（稍微外凸的飾邊）
        m.add(box(Tex.PLANKS, -0.52f, 1.92f, -0.52f, 0.52f, 2.0f, front + 0.04f));
        // 踢腳板
        m.add(box(Tex.STRIPPED_X, -0.5f, 0f, front, 0.5f, 0.1f, front + 0.03f));
        m.bookshelf = true;
        m.solid(0, 0, 0).solid(0, 1, 0);
        m.hitbox(0, 0, 0, 1f, 2f);
        return m;
    }

    // ─────────────────────────── 鞦韆 ───────────────────────────

    /** 鞦韆：A 字型木架 + 鐵鍊吊掛的座板。 */
    public static Model swing() {
        Model m = new Model();
        float legX = 1.35f;
        float topY = 2.84f;
        float spread = 0.75f;
        float len = (float) Math.sqrt(spread * spread + topY * topY);
        float ang = (float) Math.toDegrees(Math.atan2(spread, topY));
        for (int s = -1; s <= 1; s += 2) {
            float x = s * legX;
            for (int fb = -1; fb <= 1; fb += 2) {
                m.add(rot(Tex.STRIPPED, x, topY / 2f, fb * spread / 2f, 0.16f, len, 0.16f, Geo.xRot(-fb * ang)));
            }
            m.add(box(Tex.STRIPPED_Z, x - 0.05f, 0.75f, -0.55f, x + 0.05f, 0.83f, 0.55f));
        }
        // 頂樑分三段，避免材質過度拉伸
        for (int i = -1; i <= 1; i++) {
            m.add(box(Tex.LOG_X, i - 0.5f, 2.76f, -0.11f, i + 0.5f, 2.98f, 0.11f));
        }

        // 擺動部分：鐵鍊 + 座板
        m.swing = true;
        m.pivot = new Vector3f(0f, 2.76f, 0f);
        float seatTop = 0.58f;
        float chainLen = 2.76f - 0.64f;
        int segs = 2;
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < segs; k++) {
                float seg = chainLen / segs;
                m.moving.add(rot(Tex.fixed(Material.IRON_CHAIN), s * 0.42f, 0.64f + seg * (k + 0.5f), 0f,
                        1f, seg, 1f, new Quaternionf()));
            }
            m.moving.add(box(Tex.STRIPPED, s * 0.42f - 0.04f, seatTop, -0.04f, s * 0.42f + 0.04f, 0.64f, 0.04f));
        }
        m.moving.add(box(Tex.PLANKS, -0.52f, 0.5f, -0.2f, 0.52f, seatTop, 0.2f));
        m.seat(0f, seatTop, 0f);
        m.ropeLength = 2.76f - seatTop;
        m.windiness = 1f;

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                m.clear(x, y, 0);
            }
        }
        m.hitbox(0, 0, 0, 1f, 1f);
        m.hitbox(-1, 0, 0, 1f, 3f);
        m.hitbox(1, 0, 0, 1f, 3f);
        return m;
    }

    /** 樹鞦韆：一棵小樹，橫向樹枝上用繩子吊著鞦韆。 */
    public static Model treeSwing() {
        Model m = new Model();
        // 樹幹
        for (int k = 0; k < 5; k++) {
            m.add(box(Tex.LOG, -0.4f, k, -0.4f, 0.4f, k + 1, 0.4f));
        }
        m.add(box(Tex.LOG, -0.33f, 5f, -0.33f, 0.33f, 5.8f, 0.33f));
        // 樹根
        m.add(box(Tex.LOG_X, 0.4f, 0f, -0.16f, 0.62f, 0.32f, 0.16f));
        m.add(box(Tex.LOG_X, -0.62f, 0f, -0.16f, -0.4f, 0.26f, 0.16f));
        m.add(box(Tex.LOG_Z, -0.16f, 0f, 0.4f, 0.16f, 0.28f, 0.6f));
        m.add(box(Tex.LOG_Z, -0.16f, 0f, -0.6f, 0.16f, 0.3f, -0.4f));
        // 橫向樹枝
        m.add(box(Tex.LOG_X, 0.4f, 3.95f, -0.2f, 1.2f, 4.35f, 0.2f));
        m.add(box(Tex.LOG_X, 1.2f, 3.95f, -0.2f, 2.0f, 4.35f, 0.2f));
        m.add(box(Tex.LOG_X, 2.0f, 3.97f, -0.18f, 2.75f, 4.33f, 0.18f));
        // 繩子綁在樹枝上的結
        Tex rope = Tex.fixed(Material.WHITE_WOOL);
        for (int s = -1; s <= 1; s += 2) {
            float x = 2f + s * 0.35f;
            m.add(box(rope, x - 0.05f, 3.92f, -0.23f, x + 0.05f, 4.38f, 0.23f));
        }
        // 樹冠
        int[][] layer1 = {{-1, 0}, {0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}, {2, -1}, {2, 0}, {2, 1}, {3, 0}};
        int[][] layer2 = {{-1, 0}, {0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}, {2, 0}};
        int[][] layer3 = {{0, 0}, {1, 0}};
        addLeaves(m, layer1, 4.9f, 1);
        addLeaves(m, layer2, 5.9f, 2);
        addLeaves(m, layer3, 6.85f, 3);

        // 擺動部分：兩條繩子 + 座板
        m.swing = true;
        m.pivot = new Vector3f(2f, 3.95f, 0f);
        float seatTop = 0.6f;
        for (int s = -1; s <= 1; s += 2) {
            float x = 2f + s * 0.35f;
            m.moving.add(box(rope, x - 0.035f, seatTop, -0.035f, x + 0.035f, 3.95f, 0.035f));
        }
        m.moving.add(box(Tex.PLANKS, 1.5f, 0.52f, -0.2f, 2.5f, seatTop, 0.2f));
        m.seat(2f, seatTop, 0f);
        m.ropeLength = 3.95f - seatTop;
        m.windiness = 1.6f;

        for (int y = 0; y <= 4; y++) {
            m.solid(0, y, 0);
        }
        m.clear(0, 5, 0).clear(1, 4, 0).clear(2, 4, 0);
        for (int y = 0; y <= 3; y++) {
            m.clear(2, y, 0);
        }
        m.hitbox(0, 0, 0, 0.9f, 5f);
        m.hitbox(2, 0, 0, 1f, 1.2f);
        return m;
    }

    private static void addLeaves(Model m, int[][] cells, float y, int seed) {
        for (int[] c : cells) {
            int h = hash(c[0] * 31 + c[1] * 17 + seed * 101);
            float jitter = ((h & 0xF) / 15f - 0.5f) * 0.14f;
            float size = 1f + ((h >> 4 & 0xF) / 15f) * 0.12f;
            float dy = ((h >> 8 & 0xF) / 15f - 0.5f) * 0.2f;
            m.add(rot(Tex.LEAVES, c[0] + jitter, y + dy, c[1] - jitter, size, size, size, new Quaternionf()));
        }
    }

    private static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }
}
