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
    public void onPacketWrite(PacketSendEvent event) {/*
        try {
            if(DOMAIN) {
                final String domain = InetAddress.getByName(event.getPlayerIP()).getCanonicalHostName();
                final String domain2 = InetAddress.getByName(event.getPlayerIP()).getHostName();

                if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                        element.equals(domain) || element.equals(domain2)))
                    event.setCancelled(true);

                System.out.println(domain
                );
                System.out.println(domain2);
            } else {
                if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element -> element.equals(event.getPlayerIP())))
                    event.setCancelled(true);
            }
        } catch (Exception e) {
            event.setCancelled(true);
        }*/
    }

    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        if (DOMAIN) {
            final String domain = event.getConnection().getVirtualHost().getHostString();

            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(domain) /*|| element.equals(domain2))*/))
                event.setCancelled(true);

            System.out.println(domain
            );
        } else {
            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(event
                            .getConnection().getAddress().getAddress()) /*|| element.equals(domain2))*/))
                event.setCancelled(true);
        }
    }
}
