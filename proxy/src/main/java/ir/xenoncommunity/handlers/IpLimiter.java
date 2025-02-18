package ir.xenoncommunity.handlers;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.event.PacketSendEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.util.Arrays;

@SuppressWarnings("unused")
public class IpLimiter implements Listener {
    private final boolean DOMAIN = XenonCore.instance.getConfigData().getWhitelist_ip_mode().equals("DOMAIN");
    @EventHandler
    public void onPacketWrite(PacketSendEvent event) {
        try {
            if(DOMAIN) {
                final String domain = InetAddress.getByName(event.getPlayerIP()).getCanonicalHostName();

                if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element -> element.equals(domain)))
                    event.setCancelled(true);

                System.out.println(domain
                );
            } else {
                if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element -> element.equals(event.getPlayerIP())))
                    event.setCancelled(true);
            }
        } catch (Exception e) {
            event.setCancelled(true);
        }
    }
}
