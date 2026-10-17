package tw.cozy.furniture;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 傢俱物品的建立與辨識。 */
public final class FurnitureItems {

    private FurnitureItems() {
    }

    public static ItemStack create(FurnitureType type, WoodType wood, int amount) {
        ItemStack item = ItemStack.of(Material.PAPER, Math.max(1, amount));
        item.editMeta(meta -> {
            meta.itemName(Component.text(wood.displayName() + type.displayName(), NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            for (String line : type.lore()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("右鍵方塊放置・左鍵傢俱拆除", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.setItemModel(type.icon(wood).getKey());
            meta.setMaxStackSize(16);
            meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, type.id());
            meta.getPersistentDataContainer().set(Keys.ITEM_WOOD, PersistentDataType.STRING, wood.id());
        });
        return item;
    }

    public static boolean isFurniture(@Nullable ItemStack item) {
        return type(item) != null;
    }

    public static @Nullable FurnitureType type(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return FurnitureType.byId(item.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING));
    }

    public static WoodType wood(ItemStack item) {
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        return WoodType.byId(pdc.get(Keys.ITEM_WOOD, PersistentDataType.STRING));
    }
}
