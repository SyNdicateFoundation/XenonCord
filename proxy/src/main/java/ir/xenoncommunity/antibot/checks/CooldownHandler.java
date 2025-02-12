package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@AntibotCheck(name = "Still On Cooldown")
public class CooldownHandler extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPostPlayerHandshake(PostPlayerHandshakeEvent event) {
        ++joinsPerSecond;
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        cooldownMap.put(playerIp, System.currentTimeMillis());
        final long currentTime = System.currentTimeMillis();
        final long firstJoinTime = this.firstJoinTimestamps.getOrDefault(playerIp, -1L);
        if (firstJoinTime == -1L) {
            this.firstJoinTimestamps.put(playerIp, currentTime);
        } else if (currentTime - firstJoinTime < XenonCore.instance.getConfigData().getAntibot().getPlayerspecifiedcooldown()) {
            cancelPostHandshake(event, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPreLoginCooldownHandle(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final Long lastAttemptTime = cooldownMap.get(playerIp);
        if (lastAttemptTime != null && (System.currentTimeMillis() - lastAttemptTime) < XenonCore.instance.getConfigData().getAntibot().getPlayerspecifiedcooldown()) {
            cancelPostHandshake(event, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
            return;
        }
        resetPlayerData(playerIp, System.currentTimeMillis());
    }
}
