package ir.xenoncommunity.antibot;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "deprecation"})
public abstract class AntibotCheck {
    public final AtomicInteger blockedPlayersCount = new AtomicInteger(0);
    public final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    public final Map<String, Long> joinTimeMap = new ConcurrentHashMap<>();
    public final Map<String, Long> firstJoinTimestamps = new ConcurrentHashMap<>();
    public int joinsPerSecond = 0;
    public int pingsPerSecond = 0;
    private final String[] arrows = new String[]{"✟"};
    private int animationFrame = 0;

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

//    public void cancelProxyPing(ProxyPingEvent event, String reason) {
//    }

    public void cancelPing(ProxyPingEvent event) {
        log();
        event.setResponse(null);
    }
    public void cancelHandshake(PlayerHandshakeEvent event, String reason) {
        log();
        event.setCancelled(true);
    }

    public void cancelPreLogin(PreLoginEvent event, String reason) {
        log();
        event.setCancelled(true);
        event.setCancelReason(reason);
    }

    public void cancelLogin(LoginEvent event, String reason) {
        log();
        event.setCancelled(true);
        event.setCancelReason(reason);
    }

    public void log(){
        XenonCore.instance.getTaskManager().add(() -> {
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 5000) {
                sendStats();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void resetPlayerData(String playerName, long currentTime) {
        joinTimeMap.put(playerName, currentTime);
        cooldownMap.remove(playerName);
    }

    private void sendStats() {
        this.animationFrame = (this.animationFrame + 1) % this.arrows.length;
        for (ProxiedPlayer proxyPlayer : ProxyServer.getInstance().getPlayers()) {
            //if (proxyPlayer.hasPermission("alphaguard.stats") && Main.msgSee.contains(proxyPlayer.getUniqueId())) {
            final String stats = "§b§lXenonCord §8» §7CPS/s§8: §f" +
                    joinsPerSecond +
                    " §8| §7PING/s§8: §f" +
                    pingsPerSecond +
                    " §8| §7Blacklist§8: §f" +
                    blockedPlayersCount +
                    " §8(" + " §7Total CPS§8: §f" +
                    (joinsPerSecond + pingsPerSecond) + " §8| §7Ping§8: §f" +
                    proxyPlayer.getPing() + "ms§8)" +
                    " §4§l" +
                    this.arrows[this.animationFrame];
            proxyPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    stats
            ));
            XenonCore.instance.getLogger().warn(stats);
            //}
        }
    }
}
