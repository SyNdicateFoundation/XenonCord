package ir.xenoncommunity.handlers;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.event.PacketSendEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

@SuppressWarnings("unused")
public class IpLimiter implements Listener {
    private final boolean DOMAIN = XenonCore.instance.getConfigData().getWhitelist_ip_mode().equals("DOMAIN");

    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        if (DOMAIN) {
            final String domain = event.getConnection().getVirtualHost().getHostString();

            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(domain) /*|| element.equals(domain2))*/))
                event.setIgnored(true);

        } else {
            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(event
                            .getConnection().getAddress().getAddress()) /*|| element.equals(domain2))*/))
                event.setIgnored(true);
        }
    }
}
