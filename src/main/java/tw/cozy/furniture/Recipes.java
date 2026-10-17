package tw.cozy.furniture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/** 每種傢俱 × 每種木材的工作台配方。 */
public final class Recipes {

    private final Plugin plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public Recipes(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        for (FurnitureType type : FurnitureType.values()) {
            for (WoodType wood : WoodType.values()) {
                NamespacedKey key = new NamespacedKey(plugin, type.id() + "_" + wood.id());
                ShapedRecipe r = new ShapedRecipe(key, FurnitureItems.create(type, wood, type == FurnitureType.STOOL ? 2 : 1));
                r.setGroup("cozyfurniture_" + type.id());
                r.setCategory(CraftingBookCategory.MISC);
                shape(r, type, wood);
                Bukkit.removeRecipe(key);
                if (Bukkit.addRecipe(r)) {
                    keys.add(key);
                }
            }
        }
    }

    private static void shape(ShapedRecipe r, FurnitureType type, WoodType w) {
        switch (type) {
            case SMALL_TABLE -> r.shape("SSS", "T T").setIngredient('S', w.slab()).setIngredient('T', Material.STICK);
            case SQUARE_TABLE -> r.shape("PPP", "T T", "T T").setIngredient('P', w.planks()).setIngredient('T', Material.STICK);
            case LARGE_TABLE -> r.shape("SSS", "PPP", "T T").setIngredient('S', w.slab()).setIngredient('P', w.planks())
                    .setIngredient('T', Material.STICK);
            case ROUND_TABLE -> r.shape("SSS", " L ", " S ").setIngredient('S', w.slab()).setIngredient('L', w.strippedLog());
            case CHAIR -> r.shape("P  ", "PPP", "T T").setIngredient('P', w.planks()).setIngredient('T', Material.STICK);
            case STOOL -> r.shape("S", "T").setIngredient('S', w.slab()).setIngredient('T', Material.STICK);
            case BENCH -> r.shape("T  ", "SSS", "T T").setIngredient('S', w.slab()).setIngredient('T', Material.STICK);
            case SOFA -> r.shape("W W", "WWW", "S S").setIngredient('W', w.fabric()).setIngredient('S', w.slab());
            case ARMCHAIR -> r.shape("W W", "WSW", "T T").setIngredient('W', w.fabric()).setIngredient('S', w.slab())
                    .setIngredient('T', Material.STICK);
            case BED -> r.shape("W  ", "WWW", "PPP").setIngredient('W', w.fabric()).setIngredient('P', w.planks());
            case TABLE_LAMP -> r.shape("W", "H", "S").setIngredient('W', w.fabric()).setIngredient('H', Material.TORCH)
                    .setIngredient('S', w.slab());
            case FLOOR_LAMP -> r.shape("WWW", " H ", " T ").setIngredient('W', w.fabric()).setIngredient('H', Material.TORCH)
                    .setIngredient('T', w.strippedLog());
            case BOOKSHELF -> r.shape("PSP", "PBP", "PSP").setIngredient('P', w.planks()).setIngredient('S', w.slab())
                    .setIngredient('B', Material.BOOK);
            case SWING -> r.shape("LLL", "C C", "CSC").setIngredient('L', w.log()).setIngredient('C', Material.IRON_CHAIN)
                    .setIngredient('S', w.slab());
            case TREE_SWING -> r.shape("XXX", "LRL", "LSL").setIngredient('X', w.leaves()).setIngredient('L', w.log())
                    .setIngredient('R', Material.LEAD).setIngredient('S', w.slab());
        }
    }

    public void unlock(Player player) {
        player.discoverRecipes(keys);
    }

    public void unregister() {
        keys.forEach(Bukkit::removeRecipe);
        keys.clear();
    }
}
