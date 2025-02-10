package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@AntibotCheck(name = "Still On Cooldown")
public class CooldownHandler extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPreLogin(PreLoginEvent event) {
        ++joinsPerSecond;
        cooldownMap.put(event.getConnection().getName(), System.currentTimeMillis());
        final String playerName = event.getConnection().getName();
        final long currentTime = System.currentTimeMillis();
        final long firstJoinTime = this.firstJoinTimestamps.getOrDefault(playerName, -1L);
        if (firstJoinTime == -1L) {
            this.firstJoinTimestamps.put(playerName, currentTime);
        } else if (currentTime - firstJoinTime < XenonCore.instance.getConfigData().getAntibot().getPlayerspecifiedcooldown()) {
            cancelPreLogin(event, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE - 1)
    public void onPreLoginCooldownHandle(PreLoginEvent event) {
        final String playerName = event.getConnection().getName();
        final Long lastAttemptTime = cooldownMap.get(playerName);
        if (lastAttemptTime != null && (System.currentTimeMillis() - lastAttemptTime) < XenonCore.instance.getConfigData().getAntibot().getPlayerspecifiedcooldown()) {
            cancelPreLogin(event, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
            return;
        }
        resetPlayerData(playerName, System.currentTimeMillis());
    }
}
