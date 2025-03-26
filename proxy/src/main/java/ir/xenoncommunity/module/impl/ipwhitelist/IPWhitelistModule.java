package ir.xenoncommunity.module.impl.ipwhitelist;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Optional;

@ModuleInfo(name = "IPWhiteList", version = 1.0, description = "Restricts connections to whitelisted IPs or domains.")
public class IPWhitelistModule extends ModuleBase {


    /**
     * Called when the module is initialized.
     */
    @Override
    public void onInit() {
        // Check if the module is enabled in the configuration
        if (!getConfig().getModules().getIp_whitelist_module().isEnabled()) {
            return;
        }
        // Register the listener for the player handshake event
        getServer().getPluginManager().registerListener(null, this);
    }

    private final boolean isDomainMode = XenonCore.instance.getConfigData().getModules().getIp_whitelist_module().getWhitelist_ip_mode().equals("DOMAIN");

    /**
     * Handles the PlayerHandshakeEvent to enforce IP or domain whitelist rules.
     *
     * @param event the PlayerHandshakeEvent triggered during a player's handshake
     */
    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        // Determine the address based on the whitelist mode (IP or domain)
        final String address = isDomainMode
                ? Optional.ofNullable(event.getConnection().getVirtualHost())
                .map(InetSocketAddress::getHostString)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("")
                : event.getConnection().getAddress().getAddress().getHostAddress();

        // Check if the address is in the whitelist
        final boolean isWhitelisted = Arrays.stream(XenonCore.instance.getConfigData()
                        .getModules().getIp_whitelist_module().getWhitelisted_ips())
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(address::equals);

        // If the address is not whitelisted, mark the event to be ignored
        if (!isWhitelisted) {
            event.setIgnored(true);
        }
    }
}
