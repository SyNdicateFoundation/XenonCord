package net.md_5.bungee.conf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import ir.xenoncommunity.utils.Message;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.util.CaseInsensitiveMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class YamlConfig implements ConfigurationAdapter {

    private final Yaml yaml;
    private final File file;
    private Map<String, Object> config;
    public YamlConfig() {
        this(new File("config.yml"));
    }

    public YamlConfig(File file) {
        this.file = file;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    @Override
    public void load() {
        try {
            file.createNewFile();

            try (InputStream is = Files.newInputStream(file.toPath())) {
                try {
                    config = yaml.load(is);
                } catch (YAMLException ex) {
                    throw new RuntimeException("Invalid configuration encountered - this is a configuration error and NOT a bug! Please attempt to fix the error or see https://www.spigotmc.org/ for help.", ex);
                }
            }

            if (config == null) {
                config = new CaseInsensitiveMap<>();
            } else {
                config = new CaseInsensitiveMap<>(config);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not load configuration!", ex);
        }
    }

    private <T> T get(String path, T def) {
        return get(path, def, config);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T def, Map submap) {
        int index = path.indexOf('.');
        if (index == -1) {
            Object val = submap.get(path);
            if (val == null && def != null) {
                val = def;
                submap.put(path, def);
                save();
            }
            return (T) val;
        } else {
            String first = path.substring(0, index);
            String second = path.substring(index + 1);
            Map sub = (Map) submap.get(first);
            if (sub == null) {
                sub = new LinkedHashMap();
                submap.put(first, sub);
            }
            return get(second, def, sub);
        }
    }

    private void set(String path, Object val) {
        set(path, val, config);
    }

    @SuppressWarnings("unchecked")
    private void set(String path, Object val, Map submap) {
        int index = path.indexOf('.');
        if (index == -1) {
            if (val == null) {
                submap.remove(path);
            } else {
                submap.put(path, val);
            }
            save();
        } else {
            String first = path.substring(0, index);
            String second = path.substring(index + 1);
            Map sub = (Map) submap.get(first);
            if (sub == null) {
                sub = new LinkedHashMap();
                submap.put(first, sub);
            }
            set(second, val, sub);
        }
    }

    private void save() {
        try {
            try (Writer wr = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
                yaml.dump(config, wr);
            }
        } catch (IOException ex) {
            ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not save config", ex);
        }
    }

    @Override
    public int getInt(String path, int def) {
        return get(path, def).intValue();
    }

    @Override
    public String getString(String path, String def) {
        return get(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return get(path, def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ServerInfo> getServers() {
        Map<String, Map<String, Object>> base = get("servers", (Map) Collections.singletonMap("lobby", new HashMap<>()));
        Map<String, ServerInfo> ret = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : base.entrySet()) {
            Map<String, Object> val = entry.getValue();
            String name = entry.getKey();
            String addr = get("address", "localhost:25565", val);
            String motd = ChatColor.translateAlternateColorCodes('&', get("motd", "&1Just another Waterfall - Forced Host", val));
            boolean restricted = get("restricted", false, val);
            SocketAddress address = Util.getAddr(addr);
            ServerInfo info = ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted);
            ret.put(name, info);
        }

        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public Collection<ListenerInfo> getListeners() {
        Collection<Map<String, Object>> base = get("listeners", (Collection) Arrays.asList(new Map[]
                {
                        new HashMap()
                }));
        Map<String, String> forcedDef = new HashMap<>();

        Collection<ListenerInfo> ret = new HashSet<>();

        for (Map<String, Object> val : base) {
            String default_bungee_motd = get("default_bungee_motd", "Another bungee server", val);
            default_bungee_motd = Message.translateColor(default_bungee_motd);

            int maxPlayers = get("max_players", 1, val);
            boolean forceDefault = get("force_default_server", false, val);
            String host = get("host", "0.0.0.0:25565", val);
            int tabListSize = get("tab_size", 60, val);
            SocketAddress address = Util.getAddr(host);
            Map<String, String> forced = new CaseInsensitiveMap<>(get("forced_hosts", forcedDef, val));
            String tabListName = get("tab_list", "GLOBAL_PING", val);
            DefaultTabList value = DefaultTabList.valueOf(tabListName.toUpperCase(Locale.ROOT));
            if (value == null) {
                value = DefaultTabList.GLOBAL_PING;
            }
            boolean setLocalAddress = get("bind_local_address", true, val);
            boolean pingPassthrough = get("ping_passthrough", false, val);

            boolean query = get("query_enabled", false, val);
            int queryPort = get("query_port", 25577, val);

            boolean proxyProtocol = get("proxy_protocol", false, val);
            List<String> serverPriority = new ArrayList<>(get("priorities", Collections.EMPTY_LIST, val));

            // Default server list migration
            // TODO: Remove from submap
            String defaultServer = get("default_server", null, val);
            String fallbackServer = get("fallback_server", null, val);
            if (defaultServer != null) {
                serverPriority.add(defaultServer);
                set("default_server", null, val);
            }
            if (fallbackServer != null) {
                serverPriority.add(fallbackServer);
                set("fallback_server", null, val);
            }

            // Add defaults if required
            if (serverPriority.isEmpty()) {
                serverPriority.add("lobby");
            }
            set("priorities", serverPriority, val);

            ListenerInfo info = new ListenerInfo(address, default_bungee_motd, maxPlayers, tabListSize, serverPriority, forceDefault, forced, value.toString(), setLocalAddress, pingPassthrough, queryPort, query, proxyProtocol);
            ret.add(info);
        }

        return ret;
    }

    @Override
    public Collection<?> getList(String path, Collection<?> def) {
        return get(path, def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getGroups(String player) {
        return new HashSet<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getPermissions(String group) {
        return Collections.EMPTY_SET;
    }

    // Waterfall start: Forwarding rework
    public void regenerateForwardingSecret() {
        set("forwarding_secret", new String(Util.randomAlphanumericSequence(12), StandardCharsets.UTF_8));
    }

    /**
     * The default tab list options available for picking.
     */
    @RequiredArgsConstructor
    private enum DefaultTabList {

        GLOBAL(), GLOBAL_PING(), SERVER()
    }
    // Waterfall end: Forwarding rework

}
