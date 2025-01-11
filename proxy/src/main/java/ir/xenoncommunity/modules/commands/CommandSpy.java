package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener
public class CommandSpy extends Command implements Listener {
    public static ArrayList<String> spyPlayers;

    public CommandSpy() {
        super("spy", XenonCore.instance.getConfigData().getCommandspy().getSpyperm(), "cmdspy");
        spyPlayers = new ArrayList<>();
        XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null , this);
    }

    @Override
    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm()))
            return;

        final String senderName = sender.getName();
        if (args.length == 0) {
            if (!spyPlayers.contains(senderName)) {
                spyPlayers.add(senderName);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getCommandspy().getSpytogglemessage()
                                .replace("STATE", "enabled")
                        , false);
            } else {
                spyPlayers.remove(senderName);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getCommandspy().getSpytogglemessage()
                                .replace("STATE", "disabled")
                        , false);
            }
        }
    }

    @EventHandler
    public void onCommand(ChatEvent e) {
        if (!e.getMessage().startsWith("/") || !(e.getSender() instanceof ProxiedPlayer)) return;

        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (player.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpybypass())) return;

        final String rawCommand = e.getMessage();

        XenonCore.instance.getTaskManager().add(() -> {
            if (Arrays.stream(XenonCore.instance.getConfigData().getCommandspy().getSpyexceptions())
                    .map(String::toLowerCase)
                    .anyMatch(rawCommand.substring(1).toLowerCase().split(" ")[0]::equals)) return;

            XenonCore.instance.getBungeeInstance().getPlayers().stream()
                    .filter(proxiedPlayer ->
                            proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm())
                    )
                    .filter(proxiedPlayer -> spyPlayers.contains(proxiedPlayer.getName()))
                    .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                            XenonCore.instance.getConfigData().getCommandspy().getSpymessage()
                                    .replace("PLAYER", player.getDisplayName())
                                    .replace("COMMAND", rawCommand), true));
        });
    }
}
