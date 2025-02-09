package ir.xenoncommunity.antibot;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import ir.xenoncommunity.utils.Message;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/*
 * Credits: _Bycz3Q
 * This source code is provided to SyNdicate Foundation to improve XenonCord's features.
 */
@SuppressWarnings("unused")
@Getter
public class AntibotManager {
    private final Set<UUID> activeActionBars;
    private final ConcurrentHashMap<String, Integer> pingCounts;
    private final ConcurrentHashMap<String, Long> connectionTimes;
    private final ConcurrentHashMap<String, Long> blockedIPs;
    /**
     * Declare all required variables
     */
    private int joinPerSec;
    private int maxPings;
    private long blockDurationMillis;
    private long connectionIntervalMillis;

    /**
     * Initialize all required variables
     */
    public AntibotManager() {
        this.activeActionBars = new HashSet<>();
        this.pingCounts = new ConcurrentHashMap<>();
        this.connectionTimes = new ConcurrentHashMap<>();
        this.blockedIPs = new ConcurrentHashMap<>();
    }

    public void init() {
        XenonCore.instance.getTaskManager().repeatingTask(this::cleanupExpiredEntries, 1L, 1L, TimeUnit.MINUTES);
        this.maxPings = XenonCore.instance.getConfigData().getAntibot().getMaxpings();
        this.blockDurationMillis = XenonCore.instance.getConfigData().getAntibot().getBlockdurationmillis();
        this.connectionIntervalMillis = XenonCore.instance.getConfigData().getAntibot().getConnectionintervalmillis();

        new Reflections("ir.xenoncommunity.antibot.checks").getTypesAnnotatedWith(AntibotCheck.class).forEach(listener -> Arrays.stream(XenonCore.instance.getConfigData().getAntibot().getChecks()).filter(e -> e.equals(listener.getSimpleName())).forEach(check -> {
            try {
                XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, (Listener) listener.newInstance());
                XenonCore.instance.logdebuginfo(String.format("Antibot check %s loaded.", check));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        //this.getProxy().getScheduler().schedule((Plugin)this, pingCheck::resetPings, (long)ConfigManager.getPingResetInterval(), (long)ConfigManager.getPingResetInterval(), TimeUnit.SECONDS);
        //this.getProxy().getScheduler().schedule((Plugin)this, handshakeCheck::resetHandshakes, (long)ConfigManager.getPingResetInterval(), (long)ConfigManager.getPingResetInterval(), TimeUnit.SECONDS);
    }

    public boolean isBlocked(String ip) {
        final Long blockEndTime = this.blockedIPs.get(ip);
        if (blockEndTime != null && blockEndTime > System.currentTimeMillis())
            return true;

        this.blockedIPs.remove(ip);
        return false;
    }


    public void handlePing(String ip) {
        if (this.isBlocked(ip))
            return;

        this.pingCounts.merge(ip, 1, Integer::sum);
        if (this.pingCounts.get(ip) > this.maxPings) {
            this.blockIP(ip, "excessive pings");
            this.pingCounts.remove(ip);
        }
    }

    public boolean handleConnection(String ip) {
        if (this.isBlocked(ip))
            return false;

        final long now = System.currentTimeMillis();
        final Long lastConnectionTime = this.connectionTimes.get(ip);
        if (lastConnectionTime != null && now - lastConnectionTime < this.connectionIntervalMillis) {
            this.blockIP(ip, "rapid reconnection");
            return false;
        }
        this.connectionTimes.put(ip, now);
        return true;
    }

    public void resetPings() {
        this.pingCounts.clear();
    }

    private void blockIP(String ip, String reason) {
        this.blockedIPs.put(ip, System.currentTimeMillis() + this.blockDurationMillis);
        Message.send(String.format("Blocked IP %s | %s", ip, reason));
    }

    private void cleanupExpiredEntries() {
        final long now = System.currentTimeMillis();
        this.blockedIPs.entrySet().removeIf(entry -> entry.getValue() <= now);
        this.connectionTimes.entrySet().removeIf(entry -> now - entry.getValue() > this.connectionIntervalMillis * 2L);
        this.pingCounts.entrySet().removeIf(entry -> !this.connectionTimes.containsKey(entry.getKey()));
    }
}
