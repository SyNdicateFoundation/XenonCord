package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
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
import java.util.List;

@SuppressWarnings("unused")
public class StaffChat extends Command implements Listener {
    private final ArrayList<String> toggles;
    public StaffChat() {
        super("staffchat", XenonCore.instance.getConfigData().getModules().getStaffchatperm(), "sc");
        toggles = new ArrayList<>();
        XenonCore.instance.getBungeeInstance().pluginManager.registerListener(null, this);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getStaffchatperm())) return;

        final String senderName = sender.getName();

        if(args.length == 0) {
            if(sender instanceof ConsoleCommandSender) return;

            if(toggles.contains(senderName)) toggles.remove(senderName);
            else toggles.add(senderName);

            return;
        }
            final StringBuilder stringBuilder = new StringBuilder();
            Arrays.stream(args).forEach(string -> stringBuilder.append(string).append(" "));
            sendMessage(stringBuilder.toString(), senderName);

    }
    @EventHandler
    public void onChat(ChatEvent e){
        final ProxiedPlayer sender = (ProxiedPlayer) e.getSender();
        final String senderName = sender.getName();

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getStaffchatperm())
                || !toggles.contains(senderName)
                || e.getMessage().startsWith("/")) {
            return;
        }

        e.setCancelled(true);

        sendMessage(e.getMessage(), senderName);
    }

    private void sendMessage(final String msg, final String senderName){
        XenonCore.instance.getTaskManager().add(() ->
                XenonCore.instance.getBungeeInstance().getPlayers().stream()
                .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getModules().getStaffchatperm()))
                .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                        XenonCore.instance.getConfigData().getModules().getStaffchatmessage()
                                .replace("PLAYER", senderName)
                                .replace("MESSAGE", msg), true)));
    }
}
