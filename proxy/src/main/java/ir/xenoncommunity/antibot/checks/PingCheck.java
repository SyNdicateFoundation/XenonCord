package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * After I realized that XenonCord's ping handler is multi-threaded, and does NOT function as same as bungeecord,
 * Decided not to add ping blocking check
 */

@SuppressWarnings("unused")
@AntibotCheck(name = /*"Too Many Pings"*/ "Ping Handler")
public class PingCheck extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        ++pingsPerSecond;
    }
}
