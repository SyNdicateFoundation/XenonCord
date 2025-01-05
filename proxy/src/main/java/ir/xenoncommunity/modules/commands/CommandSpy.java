package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener
public class CommandSpy extends Command implements Listener {
    private ArrayList<String> spyOffPlayers;
    public CommandSpy() {
        super("cmdspy", XenonCore.instance.getConfigData().getCommandspy().getSpyperm(), "spyoff");
        spyOffPlayers = new ArrayList<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0
        && sender.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm())) {
            if(sender instanceof ConsoleCommandSender) return;

            if(spyOffPlayers.contains(sender.getName())) spyOffPlayers.remove(sender.getName());
            else spyOffPlayers.add(sender.getName());
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
                    .anyMatch(rawCommand.substring(1).toLowerCase()::contains)) return;

            XenonCore.instance.getBungeeInstance().getPlayers().stream()
                    .filter(proxiedPlayer ->
                            proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm())
                                    && spyOffPlayers.contains(proxiedPlayer.getName()))
                    .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                            XenonCore.instance.getConfigData().getCommandspy().getSpymessage()
                                    .replace("PLAYER", player.getDisplayName())
                                    .replace("COMMAND", rawCommand), true));
        });
    }
}
