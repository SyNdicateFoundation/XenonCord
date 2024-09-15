package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.modules.ModuleManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@ModuleListener public class MotdChanger implements Listener {
    @EventHandler public void proxyPingEvent(final ProxyPingEvent e){
        final ServerPing serverPing = e.getResponse();
        serverPing.setDescriptionComponent(new TextComponent(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMotd())));
        e.setResponse(serverPing);
    }
}
