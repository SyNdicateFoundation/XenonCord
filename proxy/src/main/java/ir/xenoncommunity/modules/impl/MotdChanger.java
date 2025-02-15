package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
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
        if(e.getResponse() == null) return;
        final ServerPing serverPing = e.getResponse();

        XenonCore.instance.setCurrentMotd(motd);

        serverPing.setDescriptionComponent(new TextComponent(motd));

        if (XenonCore.instance.getConfigData().getMotd_changer().getOne_more_player())
            serverPing.getPlayers().setMax(XenonCore.instance.getBungeeInstance().getOnlineCount() + 1);

        e.setResponse(serverPing);
    }

    private String getMotd() {
        return (Maintenance.downServers != null && Maintenance.downServers.contains("proxy"))
                ? XenonCore.instance.getConfigData().getMotd_changer().getMaintenance_motd()
                : XenonCore.instance.getConfigData().getMotd_changer().getMotd().replace("ONLINE", String.valueOf(XenonCore.instance.getBungeeInstance().getOnlineCount()));
    }
}
