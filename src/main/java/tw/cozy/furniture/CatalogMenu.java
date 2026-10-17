package tw.cozy.furniture;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** /furniture menu：依木材分頁的傢俱目錄。 */
public final class CatalogMenu implements InventoryHolder {

    private static final int[] TYPE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 31};
    private static final int[] WOOD_SLOTS = {36, 37, 38, 39, 40, 41, 42, 43, 44, 48, 50};

    private final WoodType wood;
    private final Inventory inventory;

    private CatalogMenu(WoodType wood) {
        this.wood = wood;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("傢俱目錄・" + wood.displayName(), NamedTextColor.DARK_GRAY));
        ItemStack pane = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        pane.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, pane);
        }
        FurnitureType[] types = FurnitureType.values();
        for (int i = 0; i < types.length && i < TYPE_SLOTS.length; i++) {
            inventory.setItem(TYPE_SLOTS[i], FurnitureItems.create(types[i], wood, 1));
        }
        WoodType[] woods = WoodType.values();
        for (int i = 0; i < woods.length && i < WOOD_SLOTS.length; i++) {
            WoodType w = woods[i];
            ItemStack icon = ItemStack.of(w.planks());
            boolean selected = w == wood;
            icon.editMeta(m -> {
                m.itemName(Component.text(w.displayName(), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
                m.lore(List.of(Component.text(selected ? "▶ 目前選擇" : "點擊切換木材", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
                m.setEnchantmentGlintOverride(selected);
            });
            inventory.setItem(WOOD_SLOTS[i], icon);
        }
        ItemStack help = ItemStack.of(Material.BOOK);
        help.editMeta(m -> {
            m.itemName(Component.text("使用說明", NamedTextColor.GOLD));
            m.lore(List.of(
                    line("左鍵：取得 1 個"),
                    line("Shift + 左鍵：取得 8 個"),
                    line("右鍵方塊放置，左鍵傢俱拆除"),
                    line("也可以在工作台合成！")));
        });
        inventory.setItem(4, help);
    }

    private static Component line(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public static void open(Player player, WoodType wood) {
        player.openInventory(new CatalogMenu(wood).inventory);
    }

    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != inventory) {
            return;
        }
        int slot = event.getRawSlot();
        WoodType[] woods = WoodType.values();
        for (int i = 0; i < WOOD_SLOTS.length && i < woods.length; i++) {
            if (WOOD_SLOTS[i] == slot) {
                open(player, woods[i]);
                return;
            }
        }
        FurnitureType[] types = FurnitureType.values();
        for (int i = 0; i < TYPE_SLOTS.length && i < types.length; i++) {
            if (TYPE_SLOTS[i] == slot) {
                int amount = event.isShiftClick() ? 8 : 1;
                player.getInventory().addItem(FurnitureItems.create(types[i], wood, amount)).values()
                        .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                return;
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
