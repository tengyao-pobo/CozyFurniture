package tw.cozy.furniture;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/** 所有 PersistentDataContainer 用到的鍵值與小工具。 */
public final class Keys {

    public static final String ROLE_ROOT = "root";
    public static final String ROLE_HITBOX = "hitbox";
    public static final String ROLE_STATIC = "static";
    public static final String ROLE_MOVING = "moving";
    public static final String ROLE_BOOK = "book";
    public static final String ROLE_DECOR = "decor";
    public static final String ROLE_SEAT = "seat";
    public static final String ROLE_LAMP = "lamp";

    public static NamespacedKey ID;
    public static NamespacedKey TYPE;
    public static NamespacedKey WOOD;
    public static NamespacedKey ROLE;
    public static NamespacedKey INDEX;
    public static NamespacedKey YAW;
    public static NamespacedKey BASE;
    public static NamespacedKey OWNER;
    public static NamespacedKey BOOKS;
    public static NamespacedKey LIT;
    public static NamespacedKey ITEM_TYPE;
    public static NamespacedKey ITEM_WOOD;

    private Keys() {
    }

    static void init(Plugin plugin) {
        ID = new NamespacedKey(plugin, "id");
        TYPE = new NamespacedKey(plugin, "type");
        WOOD = new NamespacedKey(plugin, "wood");
        ROLE = new NamespacedKey(plugin, "role");
        INDEX = new NamespacedKey(plugin, "index");
        YAW = new NamespacedKey(plugin, "yaw");
        BASE = new NamespacedKey(plugin, "base");
        OWNER = new NamespacedKey(plugin, "owner");
        BOOKS = new NamespacedKey(plugin, "books");
        LIT = new NamespacedKey(plugin, "lit");
        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
        ITEM_WOOD = new NamespacedKey(plugin, "item_wood");
    }

    public static @Nullable String str(PersistentDataHolder holder, NamespacedKey key) {
        return holder.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static @Nullable String id(PersistentDataHolder holder) {
        return str(holder, ID);
    }

    public static @Nullable String role(PersistentDataHolder holder) {
        return str(holder, ROLE);
    }

    public static boolean isFurniture(PersistentDataHolder holder) {
        return holder.getPersistentDataContainer().has(ID, PersistentDataType.STRING);
    }

    public static @Nullable FurnitureType type(PersistentDataHolder holder) {
        return FurnitureType.byId(str(holder, TYPE));
    }

    public static WoodType wood(PersistentDataHolder holder) {
        return WoodType.byId(str(holder, WOOD));
    }

    public static float yaw(PersistentDataHolder holder) {
        Float f = holder.getPersistentDataContainer().get(YAW, PersistentDataType.FLOAT);
        return f == null ? 0f : f;
    }

    public static void tag(PersistentDataHolder holder, String id, FurnitureType type, WoodType wood, String role) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        pdc.set(ID, PersistentDataType.STRING, id);
        pdc.set(TYPE, PersistentDataType.STRING, type.id());
        pdc.set(WOOD, PersistentDataType.STRING, wood.id());
        pdc.set(ROLE, PersistentDataType.STRING, role);
    }
}
