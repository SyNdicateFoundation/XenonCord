package ir.xenoncommunity.modules.impl;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class CommandSpy extends Command implements Listener {
    private static final Set<String> spyPlayers = new HashSet<>();

    public CommandSpy() {
        super("spy", XenonCore.instance.getConfigData().getCommandspy().getSpyperm(), "cmdspy");
    }

    @Override
    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            toggleSpyMode(sender, sender.getName());
        }
    }

    private void toggleSpyMode(CommandSender sender, String senderName) {
        if (spyPlayers.add(senderName)) {
            Message.send(sender, XenonCore.instance.getConfigData().getCommandspy().getSpytogglemessage().replace("STATE", "enabled"), false);
        } else {
            spyPlayers.remove(senderName);
            Message.send(sender, XenonCore.instance.getConfigData().getCommandspy().getSpytogglemessage().replace("STATE", "disabled"), false);
        }
    }

    @EventHandler
    public void onCommand(ChatEvent e) {
        if (!(e.getMessage().startsWith("/") && e.getSender() instanceof ProxiedPlayer)) return;

        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        if (player.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpybypass())) return;

        final String rawCommand = e.getMessage();

        XenonCore.instance.getTaskManager().add(() -> {
            if (isSpyException(rawCommand)) return;

            spyPlayers.stream()
                    .filter(spyPlayerName -> {
                        ProxiedPlayer spyPlayer = XenonCore.instance.getBungeeInstance().getPlayer(spyPlayerName);
                        return spyPlayer != null && spyPlayer.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm());
                    })
                    .forEach(spyPlayerName -> {
                        ProxiedPlayer spyPlayer = XenonCore.instance.getBungeeInstance().getPlayer(spyPlayerName);
                        if (spyPlayer != null) {
                            Message.send(spyPlayer,
                                    XenonCore.instance.getConfigData().getCommandspy().getSpymessage()
                                            .replace("PLAYER", player.getDisplayName())
                                            .replace("COMMAND", rawCommand), true);
                        }
                    });
        });
    }

    private boolean isSpyException(String rawCommand) {
        return Arrays.stream(XenonCore.instance.getConfigData().getCommandspy().getSpyexceptions())
                .map(String::toLowerCase)
                .anyMatch(rawCommand.substring(1).toLowerCase().split(" ")[0]::equals);
    }
}
