package tw.cozy.furniture;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tw.cozy.furniture.model.Geo;
import tw.cozy.furniture.model.Models;
import tw.cozy.furniture.model.Part;
import tw.cozy.furniture.model.Tex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 書櫃：27 格收納（3 層 × 9 本），書本內容存在 root 實體的 PDC 中，
 * 每次變動後重新生成書架上的書本模型。
 */
public final class BookshelfManager {

    public static final int SIZE = 27;
    private static final int PER_SHELF = 9;

    private static final Material[] PLAIN_COVERS = {
            Material.RED_TERRACOTTA, Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA,
            Material.BLUE_TERRACOTTA, Material.CYAN_TERRACOTTA, Material.BLACK_TERRACOTTA,
            Material.ORANGE_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.PURPLE_TERRACOTTA
    };

    /** 每個書櫃的共用背包，多人同時開啟時看到同一份內容。 */
    public static final class Holder implements InventoryHolder {
        final String id;
        Interaction root;
        Inventory inventory;

        Holder(String id, Interaction root) {
            this.id = id;
            this.root = root;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final CozyFurniture plugin;
    private final Map<String, Holder> open = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();

    public BookshelfManager(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Interaction root) {
        String id = Keys.id(root);
        Holder holder = open.computeIfAbsent(id, k -> {
            Holder h = new Holder(k, root);
            h.inventory = Bukkit.createInventory(h, SIZE, Component.text(Keys.wood(root).displayName() + "書櫃", NamedTextColor.DARK_GRAY));
            h.inventory.setContents(load(root));
            return h;
        });
        holder.root = root;
        player.openInventory(holder.inventory);
        player.playSound(Sound.sound(Key.key("item.book.page_turn"), Sound.Source.BLOCK, 1f, 1f));
    }

    public boolean allowed(@Nullable ItemStack item) {
        return item == null || item.isEmpty() || plugin.settings().bookshelfItems.contains(item.getType());
    }

    /** 背包內容變動：下一個 tick 存檔並更新外觀。 */
    public void markDirty(Holder holder) {
        if (dirty.add(holder.id)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                dirty.remove(holder.id);
                save(holder);
            });
        }
    }

    public void onClose(Holder holder, HumanEntity who) {
        save(holder);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (holder.inventory.getViewers().isEmpty()) {
                open.remove(holder.id);
            }
        });
        if (who instanceof Player p) {
            p.playSound(Sound.sound(Key.key("item.book.put"), Sound.Source.BLOCK, 0.8f, 1f));
        }
    }

    private void save(Holder holder) {
        if (!holder.root.isValid()) {
            return;
        }
        ItemStack[] contents = holder.inventory.getContents();
        store(holder.root, contents);
        refresh(holder.root, contents);
    }

    public void saveAll() {
        for (Holder h : new ArrayList<>(open.values())) {
            save(h);
            new ArrayList<>(h.inventory.getViewers()).forEach(HumanEntity::closeInventory);
        }
        open.clear();
    }

    /** 拆除書櫃時取出所有書本。 */
    public List<ItemStack> takeAll(Interaction root) {
        String id = Keys.id(root);
        ItemStack[] contents;
        Holder holder = open.remove(id);
        if (holder != null) {
            contents = holder.inventory.getContents();
            holder.inventory.clear();
            new ArrayList<>(holder.inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else {
            contents = load(root);
        }
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack it : contents) {
            if (it != null && !it.isEmpty()) {
                list.add(it);
            }
        }
        root.getPersistentDataContainer().remove(Keys.BOOKS);
        return list;
    }

    // ─────────────────────────── 存取 ───────────────────────────

    private static ItemStack[] load(Interaction root) {
        ItemStack[] out = new ItemStack[SIZE];
        byte[] bytes = root.getPersistentDataContainer().get(Keys.BOOKS, PersistentDataType.BYTE_ARRAY);
        if (bytes != null) {
            ItemStack[] stored = ItemStack.deserializeItemsFromBytes(bytes);
            System.arraycopy(stored, 0, out, 0, Math.min(SIZE, stored.length));
        }
        for (int i = 0; i < SIZE; i++) {
            if (out[i] == null) {
                out[i] = ItemStack.empty();
            }
        }
        return out;
    }

    private static void store(Interaction root, ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? ItemStack.empty() : contents[i];
        }
        root.getPersistentDataContainer().set(Keys.BOOKS, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(copy));
    }

    // ─────────────────────────── 書本外觀 ───────────────────────────

    /** 重新生成書架上的書本模型。 */
    public void refresh(Interaction root, ItemStack[] contents) {
        for (Entity e : plugin.furniture().parts(root, Keys.ROLE_BOOK)) {
            e.remove();
        }
        FurnitureType type = Keys.type(root);
        WoodType wood = Keys.wood(root);
        String id = Keys.id(root);
        float yaw = Keys.yaw(root);
        Location anchor = root.getLocation();
        anchor.setYaw(yaw);
        anchor.setPitch(0f);

        float slot = (Models.SHELF_INNER * 2f) / PER_SHELF;
        for (int i = 0; i < Math.min(SIZE, contents.length); i++) {
            ItemStack it = contents[i];
            if (it == null || it.isEmpty()) {
                continue;
            }
            int row = i / PER_SHELF;
            int col = i % PER_SHELF;
            int h = hash(i * 7919 + it.getType().ordinal());
            float height = 0.36f + (h & 0xF) / 15f * 0.14f;
            float thick = Math.min(slot - 0.008f, 0.07f + (h >> 4 & 0x3) * 0.006f + Math.min(it.getAmount() - 1, 3) * 0.004f);
            float depth = 0.32f + (h >> 6 & 0x3) * 0.015f;
            float x = -Models.SHELF_INNER + slot * (col + 0.5f);
            float baseY = Models.SHELF_BASES[row];

            Quaternionf rotation = new Quaternionf();
            boolean nextEmpty = col < PER_SHELF - 1 && i + 1 < contents.length
                    && (contents[i + 1] == null || contents[i + 1].isEmpty());
            boolean prevFull = col > 0 && contents[i - 1] != null && !contents[i - 1].isEmpty();
            float y = baseY + height / 2f;
            if (nextEmpty && !prevFull && (h >> 9 & 1) == 0) {
                // 旁邊空著的書會稍微傾斜靠著
                rotation.rotateZ(-0.22f);
                x += 0.035f;
                y -= 0.005f;
            }
            Part part = Part.rot(Tex.fixed(cover(it.getType(), h)), x, y, -0.1f, thick, height, depth, rotation);
            anchor.getWorld().spawn(anchor, BlockDisplay.class, e -> {
                e.setBlock(part.tex().apply(wood));
                e.setTransformation(Geo.transform(part, new Quaternionf(), new Vector3f(part.center())));
                e.setPersistent(true);
                Keys.tag(e, id, type, wood, Keys.ROLE_BOOK);
            });
        }
    }

    private static Material cover(Material item, int h) {
        return switch (item) {
            case ENCHANTED_BOOK -> (h & 1) == 0 ? Material.PURPLE_CONCRETE : Material.MAGENTA_TERRACOTTA;
            case WRITTEN_BOOK -> Material.BROWN_CONCRETE;
            case WRITABLE_BOOK -> Material.WHITE_TERRACOTTA;
            case KNOWLEDGE_BOOK -> Material.YELLOW_TERRACOTTA;
            case BOOK -> PLAIN_COVERS[Math.floorMod(h >> 11, PLAIN_COVERS.length)];
            default -> Material.WHITE_CONCRETE;
        };
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
