package tw.cozy.furniture;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** 傢俱可用的木材種類，決定外觀用到的方塊。 */
public enum WoodType {
    OAK("橡木"),
    SPRUCE("雲杉木"),
    BIRCH("樺木"),
    JUNGLE("叢林木"),
    ACACIA("相思木"),
    DARK_OAK("黑橡木"),
    MANGROVE("紅樹林木"),
    CHERRY("櫻花木"),
    PALE_OAK("蒼白橡木"),
    CRIMSON("緋紅蕈木"),
    WARPED("扭曲蕈木");

    private final String displayName;

    WoodType(String displayName) {
        this.displayName = displayName;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    private boolean nether() {
        return this == CRIMSON || this == WARPED;
    }

    public Material planks() {
        return mat(id() + "_planks", Material.OAK_PLANKS);
    }

    public Material log() {
        return mat(id() + (nether() ? "_stem" : "_log"), Material.OAK_LOG);
    }

    public Material strippedLog() {
        return mat("stripped_" + id() + (nether() ? "_stem" : "_log"), Material.STRIPPED_OAK_LOG);
    }

    public Material slab() {
        return mat(id() + "_slab", Material.OAK_SLAB);
    }

    public Material leaves() {
        return switch (this) {
            case CRIMSON -> Material.NETHER_WART_BLOCK;
            case WARPED -> Material.WARPED_WART_BLOCK;
            default -> mat(id() + "_leaves", Material.OAK_LEAVES);
        };
    }

    /** 布料顏色：沙發、床單、燈罩使用。 */
    public Material fabric() {
        return switch (this) {
            case OAK -> Material.RED_WOOL;
            case SPRUCE -> Material.GREEN_WOOL;
            case BIRCH -> Material.LIGHT_GRAY_WOOL;
            case JUNGLE -> Material.LIME_WOOL;
            case ACACIA -> Material.ORANGE_WOOL;
            case DARK_OAK -> Material.BROWN_WOOL;
            case MANGROVE -> Material.MAGENTA_WOOL;
            case CHERRY -> Material.PINK_WOOL;
            case PALE_OAK -> Material.WHITE_WOOL;
            case CRIMSON -> Material.PURPLE_WOOL;
            case WARPED -> Material.CYAN_WOOL;
        };
    }

    public Material carpet() {
        return mat(fabric().name().replace("_WOOL", "_CARPET"), Material.RED_CARPET);
    }

    public Material bed() {
        return mat(fabric().name().replace("_WOOL", "_BED"), Material.RED_BED);
    }

    public Material stairs() {
        return mat(id() + "_stairs", Material.OAK_STAIRS);
    }

    public Material fence() {
        return mat(id() + "_fence", Material.OAK_FENCE);
    }

    public Material trapdoor() {
        return mat(id() + "_trapdoor", Material.OAK_TRAPDOOR);
    }

    public Material pressurePlate() {
        return mat(id() + "_pressure_plate", Material.OAK_PRESSURE_PLATE);
    }

    public Material button() {
        return mat(id() + "_button", Material.OAK_BUTTON);
    }

    public Material hangingSign() {
        return mat(id() + "_hanging_sign", Material.OAK_HANGING_SIGN);
    }

    public Material sapling() {
        return switch (this) {
            case CRIMSON -> Material.CRIMSON_FUNGUS;
            case WARPED -> Material.WARPED_FUNGUS;
            case MANGROVE -> Material.MANGROVE_PROPAGULE;
            default -> mat(id() + "_sapling", Material.OAK_SAPLING);
        };
    }

    private static Material mat(String name, Material fallback) {
        Material m = Material.matchMaterial(name);
        return m == null ? fallback : m;
    }

    public static WoodType byId(@Nullable String id) {
        if (id != null) {
            for (WoodType w : values()) {
                if (w.id().equalsIgnoreCase(id)) {
                    return w;
                }
            }
        }
        return OAK;
    }

    public static @Nullable WoodType parse(String id) {
        for (WoodType w : values()) {
            if (w.id().equalsIgnoreCase(id)) {
                return w;
            }
        }
        return null;
    }
}
