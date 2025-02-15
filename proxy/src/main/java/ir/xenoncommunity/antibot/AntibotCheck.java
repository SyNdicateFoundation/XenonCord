package ir.xenoncommunity.antibot;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.commands.XenonCord;
import ir.xenoncommunity.utils.SQLManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "deprecation"})
public abstract class AntibotCheck {
    //private final SQLManager sqlManager = new SQLManager();
    public static final AtomicInteger blockedPlayersCount = new AtomicInteger(0);
    public final Map<String, Long> blockedIPs = new ConcurrentHashMap<>();
    public final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    public final Map<String, Long> joinTimeMap = new ConcurrentHashMap<>();
    public final Map<String, Long> firstJoinTimestamps = new ConcurrentHashMap<>();
    public static int joinsPerSecond = 0;
    public static int pingsPerSecond = 0;

    public void blockPlayer(PostPlayerHandshakeEvent event, String playerIP, String reason) {
        cancelPostHandshake(event, reason);
        blockedPlayersCount.incrementAndGet();
        final Long currentTime = System.currentTimeMillis();
        cooldownMap.put(playerIP, currentTime);
        blockedIPs.put(playerIP, currentTime);
        //appendToBlacklistFile(playerName);
    }

    public void blockPlayer(PreLoginEvent event, String playerIP, String reason) {
        cancelPreLogin(event, reason);
        blockedPlayersCount.incrementAndGet();
        final Long currentTime = System.currentTimeMillis();
        cooldownMap.put(playerIP, currentTime);
        blockedIPs.put(playerIP, currentTime);
        //appendToBlacklistFile(playerName);
    }

    public void blockPlayer(LoginEvent event, String playerIP, String reason) {
        cancelLogin(event, reason);
        blockedPlayersCount.incrementAndGet();
        final Long currentTime = System.currentTimeMillis();
        cooldownMap.put(playerIP, currentTime);
        blockedIPs.put(playerIP, currentTime);
        //appendToBlacklistFile(playerName);
    }

//    public void cancelProxyPing(ProxyPingEvent event, String reason) {
//    }
    private static boolean isLogging = false;
    public void cancelPing(ProxyPingEvent event) {
        log();
        event.setResponse(null);
    }
    public void cancelHandshake(PlayerHandshakeEvent event) {
        log();
        event.setCancelled(true);
    }
    public void cancelPostHandshake(PostPlayerHandshakeEvent event, String reason) {
        log();
        event.setCancelled(true);
        event.setReason(reason);
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

    private void cleanupExpiredEntries() {
        joinTimeMap.clear();
        firstJoinTimestamps.clear();
        cooldownMap.clear();
    }

    public static void log(){
        if(isLogging) return;

        XenonCore.instance.getTaskManager().add(() -> {
            isLogging = true;
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 5000) {
                sendStats();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                pingsPerSecond = 0;
                joinsPerSecond = 0;
            }
            isLogging = false;
        });
    }

    public void resetPlayerData(String playerName, long currentTime) {
        joinTimeMap.put(playerName, currentTime);
        cooldownMap.remove(playerName);
    }

    public static void sendStats() {
        final Runtime runtime = Runtime.getRuntime();
        final String stats = "§b§lXenonCord §8» §7CPS/s§8: §f" +
                joinsPerSecond +
                " §8| §7CPU§8: §f" + String.format("%.2f", getCpuUsage()) + "%" +
                " §8| §7MEMORY§8: §f" + formatMemory(runtime.totalMemory() - runtime.freeMemory()) + "/" + formatMemory(runtime.totalMemory()) +
                " §8| §7PING/s§8: §f" +
                pingsPerSecond +
                " §8| §7Blacklist§8: §f" +
                blockedPlayersCount +
                " §8" + " §7Total CPS§8: §f" +
                (joinsPerSecond + pingsPerSecond);
        for (ProxiedPlayer proxyPlayer : ProxyServer.getInstance().getPlayers()) {
            //if (proxyPlayer.hasPermission("alphaguard.stats") && Main.msgSee.contains(proxyPlayer.getUniqueId())) {
            if(!XenonCord.ABstatusPlayers.contains(proxyPlayer.getName()))  return;
            proxyPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    stats
            ));

            //}
        }
        XenonCore.instance.getLogger().warn(stats);
    }
    private static long prevProcessCpuTime;

    private static double getCpuUsage() {
        final long prevUpTime;
        final long upTime = getUptime();
        final long processCpuTime = getProcessCpuTime();

        final long processCpuTimeDiff = processCpuTime - prevProcessCpuTime;

        prevProcessCpuTime = processCpuTime;

        return (double) processCpuTimeDiff / (upTime * 10000) * 100;
    }
    private static String formatMemory(long bytes) {
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double value = (double) bytes;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", value, units[unitIndex]);
    }

    private static long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private static long getProcessCpuTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }
}
