package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@ModuleListener(isExtended = false, isImplemented = true)
public class MotdChanger implements Listener {

    @EventHandler
    public void proxyPingEvent(ProxyPingEvent e) {
        final String motd = getMotd();
        final ServerPing serverPing = e.getResponse();

        XenonCore.instance.setCurrentMotd(motd);

        serverPing.setDescriptionComponent(new TextComponent(ChatColor.translateAlternateColorCodes('&', motd)));

        if (XenonCore.instance.getConfigData().getMotdchanger().getOnemoreplayer())
            serverPing.getPlayers().setMax(XenonCore.instance.getBungeeInstance().getOnlineCount() + 1);

        e.setResponse(serverPing);
    }

    private String getMotd() {
        return (Maintenance.downServers != null && Maintenance.downServers.contains("proxy"))
                ? XenonCore.instance.getConfigData().getMotdchanger().getMaintenancemotd()
                : XenonCore.instance.getConfigData().getMotdchanger().getMotd().replace("ONLINE", String.valueOf(XenonCore.instance.getBungeeInstance().getOnlineCount()));
    }
}
