package ir.xenoncommunity.modules.impl;

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

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class AdminChat extends Command implements Listener {
    public static final Set<String> toggles = new HashSet<>();

    public AdminChat() {
        super("adminchat", XenonCore.instance.getConfigData().getAdmin_chat().getAdmin_chat_perm(), "ac");
    }

    private final String adminChatMessage = XenonCore.instance.getConfigData().getAdmin_chat().getAdmin_chat_message();
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            handleToggle(sender);
            return;
        }
        sendMessage(String.join(" ", args), sender.getName());
    }

    private void handleToggle(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return;

        String senderName = sender.getName();
        boolean isEnabled = toggles.contains(senderName);
        String state = isEnabled ? "disabled" : "enabled";

        Message.send(sender, adminChatMessage.replace("STATE", state), false);
        if (isEnabled) {
            toggles.remove(senderName);
        } else {
            toggles.add(senderName);
        }
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) return;

        final ProxiedPlayer sender = (ProxiedPlayer) e.getSender();
        final String senderName = sender.getName();

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getAdmin_chat().getAdmin_chat_perm())
                || !toggles.contains(senderName)
                || e.getMessage().startsWith("/")) {
            return;
        }

        e.setCancelled(true);

        if (StaffChat.toggles != null && StaffChat.toggles.contains(senderName)) return;

        sendMessage(e.getMessage(), senderName);
    }

    private void sendMessage(String msg, String senderName) {
        XenonCore.instance.getTaskManager().add(() -> XenonCore.instance.getBungeeInstance().getPlayers().stream()
                .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getAdmin_chat().getAdmin_chat_perm()))
                .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                        XenonCore.instance.getConfigData().getAdmin_chat().getAdmin_chat_message()
                                .replace("PLAYER", senderName)
                                .replace("MESSAGE", msg), false)));

        Message.send(adminChatMessage
                .replace("PLAYER", senderName)
                .replace("MESSAGE", msg));
    }
}
