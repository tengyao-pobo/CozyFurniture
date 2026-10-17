package tw.cozy.furniture;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 訊息輸出。 */
public final class Msg {

    private static final Component PREFIX = Component.text("[傢俱] ", NamedTextColor.GOLD);

    private Msg() {
    }

    public static void info(CommandSender to, String text) {
        to.sendMessage(PREFIX.append(Component.text(text, NamedTextColor.YELLOW)));
    }

    public static void error(CommandSender to, String text) {
        to.sendMessage(PREFIX.append(Component.text(text, NamedTextColor.RED)));
    }

    public static void bar(Player to, String text) {
        to.sendActionBar(Component.text(text, NamedTextColor.YELLOW));
    }

    public static void barError(Player to, String text) {
        to.sendActionBar(Component.text(text, NamedTextColor.RED));
    }
}
