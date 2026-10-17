package tw.cozy.furniture;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tw.cozy.furniture.model.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FurnitureListener implements Listener {

    private final CozyFurniture plugin;
    /** 同一次右鍵會先後觸發 InteractAt 與 Interact 兩個事件，用來去除重複。 */
    private final Map<UUID, Integer> lastInteractAt = new HashMap<>();

    public FurnitureListener(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────── 放置 ───────────────────────────

    @SuppressWarnings("deprecation") // Material#isInteractable 沒有替代 API
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getHand() == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (!FurnitureItems.isFurniture(item)) {
            return;
        }
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked.getType().isInteractable() && !player.isSneaking()) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("cozyfurniture.place")) {
            Msg.barError(player, "你沒有放置傢俱的權限。");
            return;
        }
        plugin.furniture().place(player, item, clicked, event.getBlockFace(), event.getHand());
    }

    // ─────────────────────────── 使用 ───────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction clicked) || !Keys.isFurniture(clicked)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        int tick = Bukkit.getCurrentTick();
        org.bukkit.Location click = null;
        if (event instanceof PlayerInteractAtEntityEvent at) {
            lastInteractAt.put(player.getUniqueId(), tick);
            click = clicked.getLocation().add(at.getClickedPosition());
        } else {
            Integer last = lastInteractAt.get(player.getUniqueId());
            if (last != null && tick - last <= 1) {
                return;
            }
        }
        if (!player.hasPermission("cozyfurniture.use")) {
            return;
        }
        Interaction root = plugin.furniture().findRoot(clicked);
        FurnitureType type = root == null ? null : Keys.type(root);
        if (type == null) {
            return;
        }
        Model model = type.model();
        ItemStack hand = player.getInventory().getItemInMainHand();
        FurnitureType holding = FurnitureItems.type(hand);
        if (model.isTable() && holding != null && holding.model().onTable) {
            if (player.hasPermission("cozyfurniture.place")) {
                plugin.furniture().placeOnTable(player, hand, root, click);
            }
        } else if (model.bookshelf) {
            plugin.bookshelves().open(player, root);
        } else if (model.isLamp()) {
            plugin.furniture().toggleLamp(player, root);
        } else if (model.bed) {
            plugin.beds().use(player, root, click);
        } else if (model.isSeat()) {
            if (!player.isSneaking()) {
                plugin.seats().sit(player, root, click);
            }
        } else if (model.isTable()) {
            plugin.furniture().useTable(player, clicked, root, click);
        }
    }

    // ─────────────────────────── 拆除 ───────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (!Keys.isFurniture(target)) {
            return;
        }
        event.setCancelled(true);
        if (!(target instanceof Interaction it) || !(event.getDamager() instanceof Player player)) {
            return;
        }
        tryBreak(player, it);
    }

    /** Paper 的攻擊前事件，作為保險：無論哪個事件先到，傢俱只會被拆一次。 */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        if (event.getAttacked() instanceof Interaction it && Keys.isFurniture(it)) {
            event.setCancelled(true);
            tryBreak(event.getPlayer(), it);
        }
    }

    private void tryBreak(Player player, Interaction part) {
        if (!part.isValid()) {
            return;
        }
        if (!player.hasPermission("cozyfurniture.break")) {
            Msg.barError(player, "你沒有拆除傢俱的權限。");
            return;
        }
        if (plugin.settings().breakRequireSneak && !player.isSneaking()) {
            Msg.bar(player, "蹲下 + 左鍵才能拆除傢俱");
            return;
        }
        Interaction root = plugin.furniture().findRoot(part);
        if (root == null) {
            part.remove();
            return;
        }
        if (!plugin.furniture().canBreak(player, root)) {
            Msg.barError(player, "你不能拆除這裡的傢俱。");
            return;
        }
        plugin.furniture().breakFurniture(root, player.getGameMode() != GameMode.CREATIVE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBarrierBreak(BlockBreakEvent event) {
        if (plugin.furniture().isFiringProtectionEvent() || event.getBlock().getType() != Material.BARRIER) {
            return;
        }
        Interaction root = plugin.furniture().findByBlock(event.getBlock());
        if (root == null) {
            return;
        }
        event.setCancelled(true);
        tryBreak(event.getPlayer(), root);
    }

    // ─────────────────────────── 座位 ───────────────────────────

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && plugin.seats().isSeat(event.getDismounted())) {
            plugin.seats().release(player, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.seats().release(event.getPlayer(), false);
        lastInteractAt.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.settings().unlockRecipes) {
            plugin.recipes().unlock(event.getPlayer());
        }
    }

    // ─────────────────────────── 區塊 ───────────────────────────

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity e : event.getEntities()) {
            if (Keys.isFurniture(e)) {
                if (Keys.ROLE_SEAT.equals(Keys.role(e))) {
                    e.remove();
                } else {
                    plugin.swings().track(e);
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity e : event.getEntities()) {
            if (Keys.isFurniture(e)) {
                plugin.swings().untrack(e);
            }
        }
    }

    // ─────────────────────────── 背包 ───────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder(false) instanceof CatalogMenu menu) {
            menu.click(event);
            return;
        }
        if (!(top.getHolder(false) instanceof BookshelfManager.Holder holder)) {
            return;
        }
        BookshelfManager shelves = plugin.bookshelves();
        boolean clickedTop = event.getClickedInventory() == top;
        if (clickedTop) {
            ItemStack incoming = event.getClick() == ClickType.NUMBER_KEY
                    ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton())
                    : event.getClick() == ClickType.SWAP_OFFHAND
                    ? event.getWhoClicked().getInventory().getItemInOffHand()
                    : event.getCursor();
            if (!shelves.allowed(incoming)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !shelves.allowed(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        shelves.markDirty(holder);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder(false) instanceof CatalogMenu) {
            event.setCancelled(true);
            return;
        }
        if (!(top.getHolder(false) instanceof BookshelfManager.Holder holder)) {
            return;
        }
        boolean touchesTop = event.getRawSlots().stream().anyMatch(s -> s < top.getSize());
        if (touchesTop && !plugin.bookshelves().allowed(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        plugin.bookshelves().markDirty(holder);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof BookshelfManager.Holder holder) {
            plugin.bookshelves().onClose(holder, event.getPlayer());
        }
    }

    /** 傢俱物品的本體是紙，避免被拿去合成書本、地圖等。 */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) {
            return;
        }
        for (ItemStack it : event.getInventory().getMatrix()) {
            if (FurnitureItems.isFurniture(it)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
