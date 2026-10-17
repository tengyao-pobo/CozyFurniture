package tw.cozy.furniture;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tw.cozy.furniture.model.Geo;
import tw.cozy.furniture.model.Model;
import tw.cozy.furniture.model.Part;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 傢俱的放置、拆除與查找。
 * <p>
 * 每件傢俱由一組實體構成：
 * <ul>
 *     <li>Interaction（root / hitbox）：負責點擊判定，root 上存放傢俱資料</li>
 *     <li>BlockDisplay（static / moving）：外觀零件，moving 為鞦韆擺動的部分</li>
 *     <li>BlockDisplay（book）、ItemDisplay（decor）：書櫃裡的書、桌上的擺飾</li>
 * </ul>
 * 所有實體都以 PDC 標記同一個傢俱 ID。
 */
public final class FurnitureManager {

    private static final double SEARCH_XZ = 8;
    private static final double SEARCH_Y = 10;

    private final CozyFurniture plugin;
    /** 觸發保護檢查用的 BlockBreakEvent 時，略過自己的監聽器。 */
    private boolean firingProtectionEvent;

    public FurnitureManager(CozyFurniture plugin) {
        this.plugin = plugin;
    }

    public boolean isFiringProtectionEvent() {
        return firingProtectionEvent;
    }

    // ─────────────────────────── 放置 ───────────────────────────

    public void place(Player player, ItemStack item, Block clicked, BlockFace face, EquipmentSlot hand) {
        FurnitureType type = FurnitureItems.type(item);
        if (type == null) {
            return;
        }
        WoodType wood = FurnitureItems.wood(item);
        Model model = type.model();
        Settings settings = plugin.settings();

        Block base;
        double anchorY;
        if (clicked.isReplaceable() && !clicked.isLiquid()) {
            base = clicked;
            anchorY = clicked.getY();
        } else {
            base = clicked.getRelative(face);
            anchorY = base.getY();
            if (face == BlockFace.UP) {
                // 放在地毯、半磚上時貼齊表面
                double top = clicked.getBoundingBox().getMaxY();
                if (top > clicked.getY() && top < clicked.getY() + 1) {
                    anchorY = top;
                }
            }
        }

        float yaw = snapYaw(player.getYaw() + 180f);
        World world = base.getWorld();

        List<Block> footprint = new ArrayList<>();
        for (int[] b : model.footprint()) {
            int[] r = Geo.rotBlock(b, yaw);
            Block blk = base.getRelative(r[0], r[1], r[2]);
            if (!isFree(blk)) {
                Msg.barError(player, "空間不足，無法放置" + type.displayName() + "。");
                return;
            }
            footprint.add(blk);
        }

        if (settings.respectProtection) {
            BlockState replaced = base.getState();
            BlockPlaceEvent event = new BlockPlaceEvent(base, replaced, clicked, item, player, true, hand);
            if (!event.callEvent() || !event.canBuild()) {
                Msg.barError(player, "你不能在這裡放置傢俱。");
                return;
            }
        }

        for (Block blk : footprint) {
            if (!blk.getType().isAir()) {
                blk.setType(Material.AIR, false);
            }
        }
        Location anchor = spawn(type, wood, base, anchorY, yaw, player.getUniqueId());

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.subtract();
        }
        player.swingHand(hand);
        world.playSound(Sound.sound(Key.key("block.wood.place"), Sound.Source.BLOCK, 1f, 0.9f),
                anchor.getX(), anchor.getY(), anchor.getZ());
    }

    /**
     * 直接生成一件傢俱（不檢查空間與權限）。
     *
     * @param base    錨點方塊
     * @param anchorY 傢俱底部的世界高度
     * @param yaw     朝向（0 / 90 / 180 / 270）
     * @return 錨點位置
     */
    public Location spawn(FurnitureType type, WoodType wood, Block base, double anchorY, float yaw, UUID owner) {
        Model model = type.model();
        String id = UUID.randomUUID().toString();
        World world = base.getWorld();
        Settings settings = plugin.settings();
        Location anchor = new Location(world, base.getX() + 0.5, anchorY, base.getZ() + 0.5, yaw, 0f);

        // 碰撞箱
        boolean first = true;
        for (Model.Hitbox h : model.hitboxes) {
            Vector3f off = Geo.rotY(new Vector3f(h.bx(), h.by(), h.bz()), yaw);
            Location loc = anchor.clone().add(off.x, off.y, off.z);
            boolean barrier = settings.collision && model.isSolid(h.bx(), h.by(), h.bz());
            float width = barrier ? 1.02f : h.width();
            float height = barrier ? (float) Math.ceil(h.height() - 1e-3) + 0.02f : h.height();
            boolean root = first;
            world.spawn(loc, Interaction.class, e -> {
                e.setInteractionWidth(width);
                e.setInteractionHeight(height);
                e.setResponsive(true);
                e.setPersistent(true);
                Keys.tag(e, id, type, wood, root ? Keys.ROLE_ROOT : Keys.ROLE_HITBOX);
                if (root) {
                    PersistentDataContainer pdc = e.getPersistentDataContainer();
                    pdc.set(Keys.YAW, PersistentDataType.FLOAT, yaw);
                    pdc.set(Keys.BASE, PersistentDataType.INTEGER_ARRAY, new int[]{base.getX(), base.getY(), base.getZ()});
                    pdc.set(Keys.OWNER, PersistentDataType.STRING, owner.toString());
                    if (model.isLamp()) {
                        pdc.set(Keys.LIT, PersistentDataType.BOOLEAN, true);
                    }
                }
            });
            first = false;
        }

        // 靜態外觀
        Quaternionf identity = new Quaternionf();
        for (int i = 0; i < model.parts.size(); i++) {
            Part part = model.parts.get(i);
            int index = i;
            if (part.lit() != null) {
                // 燈具零件：預設為開燈狀態
                Part lit = new Part(part.lit(), part.center(), part.size(), part.rotation(), part.lit());
                spawnPart(anchor, lit, identity, part.center(), wood, e -> {
                    Keys.tag(e, id, type, wood, Keys.ROLE_LAMP);
                    e.getPersistentDataContainer().set(Keys.INDEX, PersistentDataType.INTEGER, index);
                    e.setBrightness(FULL_BRIGHT);
                });
            } else {
                spawnPart(anchor, part, identity, part.center(), wood, e -> Keys.tag(e, id, type, wood, Keys.ROLE_STATIC));
            }
        }

        // 鞦韆擺動部分：實體放在支點上，零件以支點為圓心旋轉
        if (model.swing) {
            Vector3f pv = Geo.rotY(model.pivot, yaw);
            Location pivot = anchor.clone().add(pv.x, pv.y, pv.z);
            for (int i = 0; i < model.moving.size(); i++) {
                Part part = model.moving.get(i);
                int index = i;
                BlockDisplay d = spawnPart(pivot, part, identity, new Vector3f(part.center()).sub(model.pivot), wood, e -> {
                    Keys.tag(e, id, type, wood, Keys.ROLE_MOVING);
                    e.getPersistentDataContainer().set(Keys.INDEX, PersistentDataType.INTEGER, index);
                });
                plugin.swings().track(d);
            }
        }

        if (settings.collision) {
            for (int[] b : model.solid) {
                int[] r = Geo.rotBlock(b, yaw);
                base.getRelative(r[0], r[1], r[2]).setType(Material.BARRIER, false);
            }
        }
        if (model.isLamp()) {
            setLight(lightBlock(base, model, yaw), true);
        }

        return anchor;
    }

    private BlockDisplay spawnPart(Location at, Part part, Quaternionf swing, Vector3f relative, WoodType wood,
                                   Consumer<BlockDisplay> extra) {
        Transformation tr = Geo.transform(part, swing, relative);
        return at.getWorld().spawn(at, BlockDisplay.class, e -> {
            e.setBlock(part.tex().apply(wood));
            e.setTransformation(tr);
            e.setPersistent(true);
            extra.accept(e);
        });
    }

    private boolean isFree(Block block) {
        if (!(block.getType().isAir() || (block.isReplaceable() && !block.isLiquid()))) {
            return false;
        }
        BoundingBox box = BoundingBox.of(block).expand(-0.05);
        return block.getWorld().getNearbyEntities(box, e -> e instanceof Interaction && Keys.isFurniture(e)).isEmpty();
    }

    private static float snapYaw(float yaw) {
        float snapped = Math.round(yaw / 90f) * 90f;
        snapped %= 360f;
        if (snapped < 0) {
            snapped += 360f;
        }
        return snapped;
    }

    // ─────────────────────────── 燈具 ───────────────────────────

    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

    private static Block lightBlock(Block base, Model model, float yaw) {
        int[] r = Geo.rotBlock(model.lightBlock, yaw);
        return base.getRelative(r[0], r[1], r[2]);
    }

    private static void setLight(Block block, boolean on) {
        if (on) {
            if (block.getType().isAir() || block.getType() == Material.LIGHT) {
                Light light = (Light) Material.LIGHT.createBlockData();
                light.setLevel(15);
                block.setBlockData(light, false);
            }
        } else if (block.getType() == Material.LIGHT) {
            block.setType(Material.AIR, false);
        }
    }

    public boolean isLit(Interaction root) {
        return Boolean.TRUE.equals(root.getPersistentDataContainer().get(Keys.LIT, PersistentDataType.BOOLEAN));
    }

    /** 開關燈：切換燈泡外觀、燈罩亮度與光源方塊。 */
    public void toggleLamp(Player player, Interaction root) {
        FurnitureType type = Keys.type(root);
        Model model = type.model();
        WoodType wood = Keys.wood(root);
        boolean on = !isLit(root);
        root.getPersistentDataContainer().set(Keys.LIT, PersistentDataType.BOOLEAN, on);
        for (Entity e : parts(root, Keys.ROLE_LAMP)) {
            Integer index = e.getPersistentDataContainer().get(Keys.INDEX, PersistentDataType.INTEGER);
            if (!(e instanceof BlockDisplay d) || index == null || index >= model.parts.size()) {
                continue;
            }
            Part part = model.parts.get(index);
            d.setBlock((on ? part.lit() : part.tex()).apply(wood));
            d.setBrightness(on ? FULL_BRIGHT : null);
        }
        setLight(lightBlock(base(root), model, Keys.yaw(root)), on);
        player.swingMainHand();
        Location l = root.getLocation();
        root.getWorld().playSound(Sound.sound(Key.key(on ? "block.copper_bulb.turn_on" : "block.copper_bulb.turn_off"),
                Sound.Source.BLOCK, 0.8f, 1.1f), l.getX(), l.getY(), l.getZ());
    }

    /** 把可擺桌面的傢俱（檯燈）放到桌上。 */
    public void placeOnTable(Player player, ItemStack item, Interaction table, @Nullable Location click) {
        FurnitureType type = FurnitureItems.type(item);
        FurnitureType tableType = Keys.type(table);
        if (type == null || tableType == null || !type.model().onTable) {
            return;
        }
        Location at = click != null ? click : table.getLocation();
        Block cell = at.getWorld().getBlockAt(at.getBlockX(), table.getLocation().getBlockY(), at.getBlockZ());
        if (!solidBlocks(table).contains(cell) && !cell.equals(base(table))) {
            cell = base(table);
        }
        Block base = cell.getRelative(BlockFace.UP);
        if (!isFree(base)) {
            Msg.barError(player, "桌上這個位置已經有東西了。");
            return;
        }
        if (plugin.settings().respectProtection) {
            BlockPlaceEvent event = new BlockPlaceEvent(base, base.getState(), cell, item, player, true, EquipmentSlot.HAND);
            if (!event.callEvent() || !event.canBuild()) {
                Msg.barError(player, "你不能在這裡放置傢俱。");
                return;
            }
        }
        double anchorY = table.getLocation().getY() + tableType.model().tableTop;
        Location anchor = spawn(type, FurnitureItems.wood(item), base, anchorY, snapYaw(player.getYaw() + 180f),
                player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.subtract();
        }
        player.swingMainHand();
        anchor.getWorld().playSound(Sound.sound(Key.key("block.wood.place"), Sound.Source.BLOCK, 1f, 1.1f),
                anchor.getX(), anchor.getY(), anchor.getZ());
    }

    // ─────────────────────────── 查找 ───────────────────────────

    /** 由傢俱的任一部分找到 root 互動實體。 */
    public @Nullable Interaction findRoot(Entity part) {
        if (part instanceof Interaction it && Keys.ROLE_ROOT.equals(Keys.role(it))) {
            return it;
        }
        String id = Keys.id(part);
        if (id == null) {
            return null;
        }
        for (Entity e : part.getWorld().getNearbyEntities(part.getLocation(), SEARCH_XZ, SEARCH_Y, SEARCH_XZ,
                e -> e instanceof Interaction && id.equals(Keys.id(e)) && Keys.ROLE_ROOT.equals(Keys.role(e)))) {
            return (Interaction) e;
        }
        return null;
    }

    public List<Entity> parts(Interaction root) {
        String id = Keys.id(root);
        return new ArrayList<>(root.getWorld().getNearbyEntities(root.getLocation(), SEARCH_XZ, SEARCH_Y, SEARCH_XZ,
                e -> id != null && id.equals(Keys.id(e))));
    }

    public List<Entity> parts(Interaction root, String role) {
        List<Entity> list = parts(root);
        list.removeIf(e -> !role.equals(Keys.role(e)));
        return list;
    }

    public Block base(Interaction root) {
        int[] b = root.getPersistentDataContainer().get(Keys.BASE, PersistentDataType.INTEGER_ARRAY);
        if (b == null || b.length < 3) {
            return root.getLocation().getBlock();
        }
        return root.getWorld().getBlockAt(b[0], b[1], b[2]);
    }

    public List<Block> solidBlocks(Interaction root) {
        List<Block> list = new ArrayList<>();
        FurnitureType type = Keys.type(root);
        if (type == null) {
            return list;
        }
        Block base = base(root);
        float yaw = Keys.yaw(root);
        for (int[] b : type.model().solid) {
            int[] r = Geo.rotBlock(b, yaw);
            list.add(base.getRelative(r[0], r[1], r[2]));
        }
        return list;
    }

    /** 找出佔用此屏障方塊的傢俱。 */
    public @Nullable Interaction findByBlock(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (Entity e : block.getWorld().getNearbyEntities(center, SEARCH_XZ, SEARCH_Y, SEARCH_XZ,
                e -> e instanceof Interaction && Keys.ROLE_ROOT.equals(Keys.role(e)))) {
            Interaction root = (Interaction) e;
            if (solidBlocks(root).contains(block)) {
                return root;
            }
        }
        return null;
    }

    /** 擺在這張桌子上的其他傢俱（檯燈）。 */
    private List<Interaction> itemsOnTable(Interaction table) {
        List<Block> above = new ArrayList<>();
        above.add(base(table).getRelative(BlockFace.UP));
        for (Block b : solidBlocks(table)) {
            above.add(b.getRelative(BlockFace.UP));
        }
        List<Interaction> list = new ArrayList<>();
        String id = Keys.id(table);
        for (Entity e : table.getWorld().getNearbyEntities(table.getLocation(), 3, 3, 3,
                e -> e instanceof Interaction && Keys.ROLE_ROOT.equals(Keys.role(e)) && !id.equals(Keys.id(e)))) {
            Interaction other = (Interaction) e;
            FurnitureType t = Keys.type(other);
            if (t != null && t.model().onTable && above.contains(base(other))) {
                list.add(other);
            }
        }
        return list;
    }

    /** 世界座標中的座位表面位置（不含鞦韆擺動）。 */
    public Location staticSeat(Interaction root, int index) {
        FurnitureType type = Keys.type(root);
        float yaw = Keys.yaw(root);
        Vector3f s = Geo.rotY(type.model().seats.get(index), yaw);
        Location loc = root.getLocation().add(s.x, s.y, s.z);
        loc.setYaw(yaw);
        loc.setPitch(0f);
        return loc;
    }

    // ─────────────────────────── 拆除 ───────────────────────────

    public boolean canBreak(Player player, Interaction root) {
        if (!plugin.settings().respectProtection) {
            return true;
        }
        Block block = base(root);
        firingProtectionEvent = true;
        try {
            BlockBreakEvent event = new BlockBreakEvent(block, player);
            event.setDropItems(false);
            return event.callEvent();
        } finally {
            firingProtectionEvent = false;
        }
    }

    public void breakFurniture(Interaction root, boolean dropItem) {
        FurnitureType type = Keys.type(root);
        WoodType wood = Keys.wood(root);
        String id = Keys.id(root);
        Location loc = root.getLocation();
        World world = root.getWorld();

        plugin.seats().ejectAll(id);
        List<ItemStack> drops = new ArrayList<>();
        if (type != null && type.model().bookshelf) {
            drops.addAll(plugin.bookshelves().takeAll(root));
        }
        for (Entity e : parts(root)) {
            if (e instanceof ItemDisplay d && Keys.ROLE_DECOR.equals(Keys.role(d))) {
                ItemStack it = d.getItemStack();
                if (!it.isEmpty()) {
                    drops.add(it);
                }
            }
        }
        List<Block> barriers = solidBlocks(root);
        List<Interaction> onTop = type != null && type.model().isTable() ? itemsOnTable(root) : List.of();
        Block light = type != null && type.model().isLamp() ? lightBlock(base(root), type.model(), Keys.yaw(root)) : null;
        for (Entity e : parts(root)) {
            e.remove();
        }
        if (light != null) {
            setLight(light, false);
        }
        for (Interaction other : onTop) {
            breakFurniture(other, dropItem);
        }
        for (Block b : barriers) {
            if (b.getType() == Material.BARRIER) {
                b.setType(Material.AIR, false);
            }
        }
        plugin.swings().forget(id);

        if (dropItem && type != null) {
            drops.add(FurnitureItems.create(type, wood, 1));
        }
        Location dropAt = loc.clone().add(0, 0.5, 0);
        for (ItemStack it : drops) {
            world.dropItemNaturally(dropAt, it);
        }
        world.spawnParticle(Particle.BLOCK, dropAt, 40, 0.4, 0.4, 0.4, wood.planks().createBlockData());
        world.playSound(Sound.sound(Key.key("block.wood.break"), Sound.Source.BLOCK, 1f, 0.9f),
                loc.getX(), loc.getY(), loc.getZ());
    }

    // ─────────────────────────── 桌面擺飾 ───────────────────────────

    public void useTable(Player player, Interaction clicked, Interaction root, @Nullable Location click) {
        FurnitureType type = Keys.type(root);
        Model model = type.model();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (click == null) {
            click = clicked.getLocation();
        }
        double topY = root.getLocation().getY() + model.tableTop;

        if (hand.isEmpty()) {
            // 空手：拿回最近的擺飾
            ItemDisplay nearest = null;
            double best = 0.45 * 0.45;
            for (Entity e : parts(root, Keys.ROLE_DECOR)) {
                double dx = e.getLocation().getX() - click.getX();
                double dz = e.getLocation().getZ() - click.getZ();
                double d = dx * dx + dz * dz;
                if (d < best && e instanceof ItemDisplay d2) {
                    best = d;
                    nearest = d2;
                }
            }
            if (nearest != null) {
                ItemStack it = nearest.getItemStack();
                nearest.remove();
                if (player.getGameMode() != GameMode.CREATIVE && !it.isEmpty()) {
                    player.getInventory().addItem(it).values()
                            .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                }
                player.swingMainHand();
                player.playSound(Sound.sound(Key.key("entity.item.pickup"), Sound.Source.PLAYER, 0.6f, 1.2f));
            }
            return;
        }
        if (FurnitureItems.isFurniture(hand)) {
            return;
        }
        if (parts(root, Keys.ROLE_DECOR).size() >= plugin.settings().maxDecorations) {
            Msg.barError(player, "這張桌子已經擺滿了。");
            return;
        }

        ItemStack one = hand.asOne();
        boolean block = hand.getType().isBlock();
        float scale = block ? 0.35f : 0.45f;
        Quaternionf rotation = block ? new Quaternionf() : Geo.xRot(-90);
        Vector3f translation = new Vector3f(0f, block ? scale / 2f : 0.02f, 0f);
        Location at = new Location(root.getWorld(), click.getX(), topY, click.getZ(), player.getYaw() + 180f, 0f);
        String id = Keys.id(root);
        WoodType wood = Keys.wood(root);
        root.getWorld().spawn(at, ItemDisplay.class, e -> {
            e.setItemStack(one);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            e.setTransformation(new Transformation(translation, rotation, new Vector3f(scale), new Quaternionf()));
            e.setPersistent(true);
            Keys.tag(e, id, type, wood, Keys.ROLE_DECOR);
        });
        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.subtract();
        }
        player.swingMainHand();
        player.playSound(Sound.sound(Key.key("block.wood.hit"), Sound.Source.BLOCK, 0.6f, 1.4f));
    }

    public void runLater(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }
}
