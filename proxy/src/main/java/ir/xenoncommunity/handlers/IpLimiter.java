package ir.xenoncommunity.handlers;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;

@SuppressWarnings("unused")
public class IpLimiter implements Listener {
    private final boolean isDomainMode = XenonCore.instance.getConfigData().getWhitelist_ip_mode().equals("DOMAIN");

    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        if (isDomainMode) {
            final String domain = (event.getConnection().getVirtualHost() != null &&
                    event.getConnection().getVirtualHost().getHostString() != null)
                    ? event.getConnection().getVirtualHost().getHostString().trim().toLowerCase()
                    : "";

            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips())
                    .noneMatch(element -> element.trim().toLowerCase().equals(domain)))
                event.setIgnored(true);

        } else {
            if (Arrays.stream(XenonCore.instance.getConfigData().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(event
                            .getConnection().getAddress().getAddress()) /*|| element.equals(domain2))*/))
                event.setIgnored(true);
        }
    }
}
