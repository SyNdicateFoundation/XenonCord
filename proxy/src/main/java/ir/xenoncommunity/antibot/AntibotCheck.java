package ir.xenoncommunity.antibot;

import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "deprecation"})
public abstract class AntibotCheck {
    public final AtomicInteger blockedPlayersCount = new AtomicInteger(0);
    public final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    public final Map<String, Long> joinTimeMap = new ConcurrentHashMap<>();

    public void blockPlayer(PreLoginEvent event, String playerName, String reason) {
        cancelPreLogin(event, reason);
        blockedPlayersCount.incrementAndGet();
        cooldownMap.put(playerName, System.currentTimeMillis());
        //appendToBlacklistFile(playerName);
    }

    public void blockPlayer(LoginEvent event, String playerName, String reason) {
        cancelLogin(event, reason);
        blockedPlayersCount.incrementAndGet();
        cooldownMap.put(playerName, System.currentTimeMillis());
        //appendToBlacklistFile(playerName);
    }

    public void cancelPreLogin(PreLoginEvent event, String reason) {
        event.setCancelled(true);
        event.setCancelReason(reason);
    }

    public void cancelLogin(LoginEvent event, String reason) {
        event.setCancelled(true);
        event.setCancelReason(reason);
    }

    public void resetPlayerData(String playerName, long currentTime) {
        joinTimeMap.put(playerName, currentTime);
        cooldownMap.remove(playerName);
    }
}
