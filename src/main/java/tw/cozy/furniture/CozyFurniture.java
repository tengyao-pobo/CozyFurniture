package tw.cozy.furniture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class CozyFurniture extends JavaPlugin {

    private Settings settings;
    private FurnitureManager furniture;
    private SeatManager seats;
    private SwingManager swings;
    private BookshelfManager bookshelves;
    private BedManager beds;
    private Recipes recipes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);
        settings = new Settings(getConfig());

        furniture = new FurnitureManager(this);
        seats = new SeatManager(this);
        swings = new SwingManager(this);
        bookshelves = new BookshelfManager(this);
        beds = new BedManager(this);
        recipes = new Recipes(this);
        recipes.register();

        getServer().getPluginManager().registerEvents(new FurnitureListener(this), this);
        registerCommand("furniture", "CozyFurniture 傢俱指令", List.of("cozyfurniture", "fur"), new FurnitureCommand(this));

        // 伺服器已載入的區塊（例如 /reload 或延遲載入）
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (Keys.isFurniture(e)) {
                    if (Keys.ROLE_SEAT.equals(Keys.role(e))) {
                        e.remove();
                    } else {
                        swings.track(e);
                    }
                }
            }
        }
        if (settings.unlockRecipes) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                recipes.unlock(p);
            }
        }

        Bukkit.getScheduler().runTaskTimer(this, swings::tick, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, beds::tick, 20L, 20L);
        getLogger().info("CozyFurniture 已啟用，共 " + FurnitureType.values().length + " 種傢俱 × "
                + WoodType.values().length + " 種木材。");
    }

    @Override
    public void onDisable() {
        if (seats != null) {
            seats.ejectEverything();
        }
        if (bookshelves != null) {
            bookshelves.saveAll();
        }
        if (recipes != null) {
            recipes.unregister();
        }
    }

    public void reload() {
        reloadConfig();
        settings = new Settings(getConfig());
    }

    public Settings settings() {
        return settings;
    }

    public FurnitureManager furniture() {
        return furniture;
    }

    public SeatManager seats() {
        return seats;
    }

    public SwingManager swings() {
        return swings;
    }

    public BookshelfManager bookshelves() {
        return bookshelves;
    }

    public BedManager beds() {
        return beds;
    }

    public Recipes recipes() {
        return recipes;
    }
}
