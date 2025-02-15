package ir.xenoncommunity.antibot.checks;

//import ir.xenoncommunity.XenonCore;

import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;

/**
 * After I realized that XenonCord's ping handler is multi-threaded, and does NOT function as same as bungeecord,
 * Decided not to add this check.
 */

@SuppressWarnings("unused")
@AntibotCheck(name = /*"Too Many Pings"*/ "Ping Handler")
public class PingCheck extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
//    private final Map<String, Integer> pingCounts = new HashMap();
//    private final Map<String, Long> blockedIPs = new HashMap();
//
//this.blockedIPs.containsKey(ip) && (Long)this.blockedIPs.get(ip) > System.currentTimeMillis()
//
//    public PingCheck(){
//        XenonCore.instance.getTaskManager().repeatingTask(this::resetPings,
//                XenonCore.instance.getConfigData().getAntibot().getPingresetinterval(),
//                XenonCore.instance.getConfigData().getAntibot().getPingresetinterval(),
//                TimeUnit.MILLISECONDS);
//        //this.getProxy().getScheduler().schedule((Plugin)this, handshakeCheck::resetHandshakes, (long)ConfigManager.getPingResetInterval(), (long)ConfigManager.getPingResetInterval(), TimeUnit.SECONDS);
//    }
    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        ++pingsPerSecond;
//        final String ip = e.getConnection().getSocketAddress().toString();
//        this.pingCounts.put(ip, this.pingCounts.getOrDefault(ip, 0) + 1);
//        if (this.pingCounts.get(ip) > XenonCore.instance.getConfigData().getAntibot().getMaxpings()) {
//            this.blockedIPs.put(ip, System.currentTimeMillis() + XenonCore.instance.getConfigData().getAntibot().getBlockdurationmillis());
//            this.pingCounts.remove(ip);
//        }
    }

//    public void resetPings() {
//        this.pingCounts.clear();
//    }
}
