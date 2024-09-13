package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.abstracts.ModuleListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;

public class CommandSpy extends ModuleListener implements Listener {
    @EventHandler
    public void onCommand(final ChatEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer) || !e.getMessage().startsWith("/")
                || ((ProxiedPlayer) e.getSender()).hasPermission(XenonCore.instance.getConfigData().getModules().getSpybypass())) return;


        if (Arrays.asList(XenonCore.instance.getConfigData().getModules().getSpyexceptions()).contains(e.getMessage().replace("/", ""))) return;

        XenonCore.instance.getTaskManager().add(() -> {
            XenonCore.instance.getBungeeInstance().getPlayers().stream()
                    .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getModules().getSpyperm()))
                    .forEach(player -> {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getSpymessage().replace("PLAYER", ((ProxiedPlayer)e.getSender()).getName()).replace("COMMAND", e.getMessage())));
                    });
        });
    }
}
