package ir.xenoncommunity.module.impl.ipwhitelist;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;

@ModuleInfo(name = "IPWhiteList", version = 1.0, description = "Restricts connections to whitelisted IPs or domains.")
public class IPWhitelistModule extends ModuleBase {


    @Override
    public void onInit() {
        if (!getConfig().getModules().getIp_whitelist_module().isEnabled())
            return;
        getServer().getPluginManager().registerListener(null, this);
    }

    private final boolean isDomainMode = XenonCore.instance.getConfigData().getModules().getIp_whitelist_module().getWhitelist_ip_mode().equals("DOMAIN");

    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        if (isDomainMode) {
            final String domain = (event.getConnection().getVirtualHost() != null &&
                    event.getConnection().getVirtualHost().getHostString() != null)
                    ? event.getConnection().getVirtualHost().getHostString().trim().toLowerCase()
                    : "";

            if (Arrays.stream(XenonCore.instance.getConfigData().getModules().getIp_whitelist_module().getWhitelisted_ips())
                    .noneMatch(element -> element.trim().toLowerCase().equals(domain)))
                event.setIgnored(true);

        } else {
            if (Arrays.stream(XenonCore.instance.getConfigData().getModules().getIp_whitelist_module().getWhitelisted_ips()).noneMatch(element ->
                    element.equals(event
                            .getConnection().getAddress().getAddress()) /*|| element.equals(domain2))*/))
                event.setIgnored(true);
        }
    }
}
