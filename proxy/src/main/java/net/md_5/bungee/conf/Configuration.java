package net.md_5.bungee.conf;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import gnu.trove.map.TMap;
import lombok.Getter;
import lombok.Synchronized;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.util.CaseInsensitiveMap;
import net.md_5.bungee.util.CaseInsensitiveSet;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Core configuration for the proxy.
 */
@Getter
public abstract class Configuration implements ProxyConfig {

    private final Object serversLock = new Object(); // Waterfall
    /**
     * Time before users are disconnected due to no network activity.
     */
    private int timeout = 30000;
    /**
     * UUID used for metrics.
     */
    private String uuid = UUID.randomUUID().toString();
    /**
     * Set of all listeners.
     */
    private Collection<ListenerInfo> listeners;
    /**
     * Set of all servers.
     */
    private TMap<String, ServerInfo> servers;
    /**
     * Should we check minecraft.net auth.
     */
    private boolean onlineMode = false;
    /**
     * Whether to check the authentication server public key.
     */
    private boolean enforceSecureProfile;
    private boolean ignoreSecureProfileForOlderVersions;
    private int remotePingCache = -1;
    private int playerLimit = -1;
    private int serverConnectTimeout = 5000;
    private int remotePingTimeout = 5000;
    private int throttle = 4000;
    private int throttleLimit = 3;
    private boolean ipForward;
    private Favicon favicon;
    private int compressionThreshold = 256;
    private boolean preventProxyConnections;
    private boolean rejectTransfers;
    private boolean forgeSupport = true; // Waterfall: default to enabled
    private Collection<String> disabledCommands;

    @Synchronized("serversLock") // Waterfall
    public void load() {
        ConfigurationAdapter adapter = ProxyServer.getInstance().getConfigurationAdapter();
        adapter.load();

        File fav = new File("server-icon.png");
        if (fav.exists()) {
            try {
                favicon = Favicon.create(ImageIO.read(fav));
            } catch (IOException | IllegalArgumentException ex) {
                ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not load server icon", ex);
            }
        }

        listeners = adapter.getListeners();
        timeout = adapter.getInt("timeout", timeout);
        uuid = adapter.getString("stats", uuid);
        onlineMode = adapter.getBoolean("online_mode", onlineMode);
        enforceSecureProfile = adapter.getBoolean("enforce_secure_profile", enforceSecureProfile);
        ignoreSecureProfileForOlderVersions = adapter.getBoolean("ignore_secure_profile_for_older_versions", ignoreSecureProfileForOlderVersions);
        remotePingCache = adapter.getInt("remote_ping_cache", remotePingCache);
        playerLimit = adapter.getInt("player_limit", playerLimit);
        serverConnectTimeout = adapter.getInt("server_connect_timeout", serverConnectTimeout);
        remotePingTimeout = adapter.getInt("remote_ping_timeout", remotePingTimeout);
        throttle = adapter.getInt("connection_throttle", throttle);
        throttleLimit = adapter.getInt("connection_throttle_limit", throttleLimit);
        ipForward = adapter.getBoolean("ip_forward", ipForward);
        compressionThreshold = adapter.getInt("network_compression_threshold", compressionThreshold);
        preventProxyConnections = adapter.getBoolean("prevent_proxy_connections", preventProxyConnections);
        forgeSupport = adapter.getBoolean("forge_support", forgeSupport);
        rejectTransfers = adapter.getBoolean("reject_transfers", rejectTransfers);
        disabledCommands = new CaseInsensitiveSet((Collection<String>) adapter.getList("disabled_commands", new ArrayList<>()));

        Preconditions.checkArgument(listeners != null && !listeners.isEmpty(), "No listeners defined.");

        Map<String, ServerInfo> newServers = adapter.getServers();
        Preconditions.checkArgument(newServers != null && !newServers.isEmpty(), "No servers defined");

        if (servers == null) {
            servers = new CaseInsensitiveMap<>(newServers);
        } else {
            Map<String, ServerInfo> oldServers = getServersCopy();

            for (ServerInfo oldServer : oldServers.values()) {
                ServerInfo newServer = newServers.get(oldServer.getName());
                if ((newServer == null || !oldServer.getAddress().equals(newServer.getAddress())) && !oldServer.getPlayers().isEmpty()) {
                    BungeeCord.getInstance().getLogger().info("Moving players off of server: " + oldServer.getName());
                    // The server is being removed, or having it's address changed
                    for (ProxiedPlayer player : oldServer.getPlayers()) {
                        ListenerInfo listener = player.getPendingConnection().getListener();
                        String destinationName = newServers.get(listener.getDefaultServer()) == null ? listener.getDefaultServer() : listener.getFallbackServer();
                        ServerInfo destination = newServers.get(destinationName);
                        if (destination == null) {
                            BungeeCord.getInstance().getLogger().severe("Couldn't find server " + listener.getDefaultServer() + " or " + listener.getFallbackServer() + " to put player " + player.getName() + " on");
                            player.disconnect(BungeeCord.getInstance().getTranslation("fallback_kick", "Not found on reload"));
                            continue;
                        }
                        player.connect(destination, (success, cause) -> {
                            if (!success) {
                                BungeeCord.getInstance().getLogger().log(Level.WARNING, "Failed to connect " + player.getName() + " to " + destination.getName(), cause);
                                player.disconnect(BungeeCord.getInstance().getTranslation("fallback_kick", cause.getCause().getClass().getName()));
                            }
                        });
                    }
                } else {
                    // This server isn't new or removed, we'll use bungees behavior of just ignoring
                    // any changes to info outside of the address, this is not ideal, but the alternative
                    // requires resetting multiple objects of which have no proper identity
                    newServers.put(oldServer.getName(), oldServer);
                }
            }
            this.servers = new CaseInsensitiveMap<>(newServers);
        }

        for (ListenerInfo listener : listeners) {
            for (int i = 0; i < listener.getServerPriority().size(); i++) {
                String server = listener.getServerPriority().get(i);
                Preconditions.checkArgument(servers.containsKey(server), "Server %s (priority %s) is not defined", server, i);
            }
            for (String server : listener.getForcedHosts().values()) {
                if (!servers.containsKey(server)) {
                    ProxyServer.getInstance().getLogger().log(Level.WARNING, "Forced host server {0} is not defined", server);
                }
            }
        }
    }

    @Override
    @Deprecated
    public String getFavicon() {
        return getFaviconObject().getEncoded();
    }

    @Override
    public Favicon getFaviconObject() {
        return favicon;
    }

    // Waterfall start
    @Override
    @Synchronized("serversLock")
    public Map<String, ServerInfo> getServersCopy() {
        return ImmutableMap.copyOf(servers);
    }

    @Override
    @Synchronized("serversLock")
    public ServerInfo getServerInfo(String name) {
        return this.servers.get(name);
    }

    @Override
    @Synchronized("serversLock")
    public ServerInfo addServer(ServerInfo server) {
        return this.servers.put(server.getName(), server);
    }

    @Override
    @Synchronized("serversLock")
    public boolean addServers(Collection<ServerInfo> servers) {
        boolean changed = false;
        for (ServerInfo server : servers) {
            if (server != this.servers.put(server.getName(), server)) changed = true;
        }
        return changed;
    }

    @Override
    @Synchronized("serversLock")
    public ServerInfo removeServerNamed(String name) {
        return this.servers.remove(name);
    }

    @Override
    @Synchronized("serversLock")
    public ServerInfo removeServer(ServerInfo server) {
        return this.servers.remove(server.getName());
    }

    @Override
    @Synchronized("serversLock")
    public boolean removeServersNamed(Collection<String> names) {
        return this.servers.keySet().removeAll(names);
    }

    @Override
    @Synchronized("serversLock")
    public boolean removeServers(Collection<ServerInfo> servers) {
        boolean changed = false;
        for (ServerInfo server : servers) {
            if (null != this.servers.remove(server.getName())) changed = true;
        }
        return changed;
    }
    // Waterfall end
}
