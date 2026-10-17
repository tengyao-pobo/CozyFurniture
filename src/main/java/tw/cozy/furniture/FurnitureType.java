package tw.cozy.furniture;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import tw.cozy.furniture.model.Model;
import tw.cozy.furniture.model.Models;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/** 所有傢俱種類。 */
public enum FurnitureType {
    SMALL_TABLE("小茶几", WoodType::trapdoor, Models::smallTable,
            List.of("矮矮的茶几，附下層置物板。", "右鍵手持物品可擺放在桌上。")),
    SQUARE_TABLE("方桌", WoodType::fence, Models::squareTable,
            List.of("標準高度的四腳方桌。", "右鍵手持物品可擺放在桌上。")),
    LARGE_TABLE("長桌", WoodType::slab, Models::largeTable,
            List.of("佔兩格的大長桌，適合餐廳。", "右鍵手持物品可擺放在桌上。")),
    ROUND_TABLE("圓桌", WoodType::pressurePlate, Models::roundTable,
            List.of("單柱腳的圓形桌。", "右鍵手持物品可擺放在桌上。")),
    CHAIR("椅子", WoodType::stairs, Models::chair,
            List.of("有靠背的餐椅。", "右鍵坐下。")),
    STOOL("凳子", WoodType::button, Models::stool,
            List.of("小巧的圓凳。", "右鍵坐下。")),
    BENCH("長椅", w -> w.log(), Models::bench,
            List.of("兩人座的公園長椅。", "右鍵坐下。")),
    SOFA("沙發", WoodType::fabric, () -> Models.sofa(2),
            List.of("柔軟的雙人布沙發。", "右鍵坐下，布料顏色依木材而定。")),
    ARMCHAIR("單人沙發", WoodType::carpet, () -> Models.sofa(1),
            List.of("厚實的單人扶手沙發。", "右鍵坐下，布料顏色依木材而定。")),
    BED("床", WoodType::bed, Models::bed,
            List.of("有床頭板與棉被的木床。", "右鍵設定重生點，夜晚可以睡覺跳過黑夜。")),
    TABLE_LAMP("檯燈", w -> Material.LANTERN, Models::tableLamp,
            List.of("小檯燈，可以直接放在桌上。", "右鍵開關，會真的照亮周圍。")),
    FLOOR_LAMP("落地燈", w -> Material.END_ROD, Models::floorLamp,
            List.of("兩格高的落地燈。", "右鍵開關，會真的照亮周圍。")),
    BOOKSHELF("書櫃", w -> Material.BOOKSHELF, Models::bookshelf,
            List.of("三層書櫃，可收納 27 本書。", "放進去的書會實際排在書架上！")),
    SWING("鞦韆", WoodType::hangingSign, Models::swing,
            List.of("A 字木架鞦韆，會隨風輕輕搖晃。", "坐上後按住 W 用力盪、按 S 煞車。")),
    TREE_SWING("樹鞦韆", WoodType::sapling, Models::treeSwing,
            List.of("一棵掛著鞦韆的小樹。", "坐上後按住 W 用力盪、按 S 煞車。"));

    private final String displayName;
    private final Function<WoodType, Material> icon;
    private final Supplier<Model> factory;
    private final List<String> lore;
    private Model model;

    FurnitureType(String displayName, Function<WoodType, Material> icon, Supplier<Model> factory, List<String> lore) {
        this.displayName = displayName;
        this.icon = icon;
        this.factory = factory;
        this.lore = lore;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public Material icon(WoodType wood) {
        return icon.apply(wood);
    }

    public List<String> lore() {
        return lore;
    }

    public Model model() {
        if (model == null) {
            model = factory.get();
        }
        return model;
    }

    public static @Nullable FurnitureType byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (FurnitureType t : values()) {
            if (t.id().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
