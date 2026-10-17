package tw.cozy.furniture;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.Set;

/** 讀取 config.yml 後的設定值。 */
public final class Settings {

    public final boolean collision;
    public final boolean breakRequireSneak;
    public final boolean respectProtection;
    public final boolean unlockRecipes;
    public final double seatOffset;
    public final double swingIdle;
    public final double swingRider;
    public final double swingMax;
    public final double stormMultiplier;
    public final int maxDecorations;
    public final Set<Material> bookshelfItems = EnumSet.noneOf(Material.class);

    public Settings(FileConfiguration c) {
        collision = c.getBoolean("collision", true);
        breakRequireSneak = c.getBoolean("break-require-sneak", false);
        respectProtection = c.getBoolean("respect-protection", true);
        unlockRecipes = c.getBoolean("unlock-recipes", true);
        seatOffset = c.getDouble("seat-height-offset", 0.0);
        swingIdle = c.getDouble("swing.idle-amplitude", 7.0);
        swingRider = c.getDouble("swing.rider-amplitude", 16.0);
        swingMax = c.getDouble("swing.max-amplitude", 55.0);
        stormMultiplier = c.getDouble("swing.storm-multiplier", 2.2);
        maxDecorations = c.getInt("table.max-decorations", 8);
        for (String name : c.getStringList("bookshelf.allowed-items")) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                bookshelfItems.add(m);
            }
        }
    }
}
