package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import ir.xenoncommunity.utils.Configuration;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

import javax.imageio.spi.ServiceRegistry;

@SuppressWarnings("unused")
@AntibotCheck(name = "Still On Cooldown")
public class CooldownHandler extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {

    private final long cooldownThreshold;
    private final String disconnectCooldown;
    private final long fastJoinThreshold;

    public CooldownHandler() {
        final Configuration.AntiBotData config = XenonCore.instance.getConfigData().getAntibot();
        this.cooldownThreshold = config.getPlayer_specified_cooldown();
        this.disconnectCooldown = config.getDisconnect_cooldown();
        this.fastJoinThreshold = config.getFast_join_threshold();
    }
    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        final String playerIP = e.getConnection().getAddress().getAddress().getHostAddress();
        final Long blockStart = blockedIPs.get(playerIP);

        if(blockStart == null) return;

        if(blockStart >= System.currentTimeMillis() - XenonCore.instance.getConfigData().getAntibot().getBlock_duration_millis()){
            e.setResponse(null);
        } else {
            blockedIPs.remove(playerIP);
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPlayerHandshake(PlayerHandshakeEvent event){
        final String playerIP = event.getConnection().getAddress().getAddress().getHostAddress();
        final Long blockStart = blockedIPs.get(playerIP);

        if(blockStart == null) return;

        if(blockStart >= System.currentTimeMillis() - XenonCore.instance.getConfigData().getAntibot().getBlock_duration_millis()){
            event.setCancelled(true);
        } else {
            blockedIPs.remove(playerIP);
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPostPlayerHandshake(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final long currentTime = System.currentTimeMillis();

        cooldownMap.put(playerIp, currentTime);

        final long firstJoinTime = this.firstJoinTimestamps.getOrDefault(playerIp, -1L);
        if (firstJoinTime == -1L) {
            this.firstJoinTimestamps.put(playerIp, currentTime);
        } else if (currentTime - firstJoinTime < cooldownThreshold) {
            blockPlayer(event, playerIp, disconnectCooldown);
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE - 1)
    public void onPostPostPlayerHandshakeEvent(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final long currentTime = System.currentTimeMillis();

        final Long lastAttemptTime = cooldownMap.get(playerIp);
        if (lastAttemptTime != null && (currentTime - lastAttemptTime) < cooldownThreshold) {
            blockPlayer(event, playerIp, disconnectCooldown);

        } else {
            resetPlayerData(playerIp, currentTime);
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE - 2)
    public void onPostPostPostPlayerHandshake(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final long currentTime = System.currentTimeMillis();

        final Long lastAttemptTime = cooldownMap.get(playerIp);
        if (lastAttemptTime != null && (currentTime - lastAttemptTime) < fastJoinThreshold) {
            blockPlayer(event, playerIp, disconnectCooldown);
        }
    }
}
