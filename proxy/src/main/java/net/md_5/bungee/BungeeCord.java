package net.md_5.bungee;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.waterfallmc.waterfall.conf.WaterfallConfiguration;
import io.github.waterfallmc.waterfall.event.ProxyExceptionEvent;
import io.github.waterfallmc.waterfall.exception.ProxyPluginEnableDisableException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.util.ResourceLeakDetector;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.chat.*;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.compress.CompressFactory;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.conf.YamlConfig;
import net.md_5.bungee.connection.ConnectionThrottle;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.query.RemoteQuery;
import net.md_5.bungee.scheduler.BungeeScheduler;
import net.md_5.bungee.util.CaseInsensitiveMap;
import net.md_5.bungee.util.EncryptionUtil;
import net.md_5.bungee.util.PlayerInfoSerializer;
import org.reflections.Reflections;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main BungeeCord proxy class.
 */
public class BungeeCord extends ProxyServer {

    /**
     * Configuration.
     */
    @Getter
    public final Configuration config = new WaterfallConfiguration();
    /**
     * Plugin manager.
     */
    @Getter
    public final PluginManager pluginManager;
    public final Gson gson = new GsonBuilder()
            .registerTypeAdapter(BaseComponent.class, new ComponentSerializer())
            .registerTypeAdapter(TextComponent.class, new TextComponentSerializer())
            .registerTypeAdapter(TranslatableComponent.class, new TranslatableComponentSerializer())
            .registerTypeAdapter(KeybindComponent.class, new KeybindComponentSerializer())
            .registerTypeAdapter(ScoreComponent.class, new ScoreComponentSerializer())
            .registerTypeAdapter(SelectorComponent.class, new SelectorComponentSerializer())
            .registerTypeAdapter(ComponentStyle.class, new ComponentStyleSerializer())
            .registerTypeAdapter(ServerPing.PlayerInfo.class, new PlayerInfoSerializer())
            .registerTypeAdapter(Favicon.class, Favicon.getFaviconTypeAdapter()).create();
    /**
     * locations.yml save thread.
     */
    private final Timer saveThread = new Timer("Reconnect Saver");
    /**
     * Server socket listener.
     */
    private final Collection<Channel> listeners = new HashSet<>();
    // private final Timer metricsThread = new Timer( "Metrics Thread" ); // Waterfall: Disable Metrics
    /**
     * Fully qualified connections.
     */
    private final Map<String, UserConnection> connections = new CaseInsensitiveMap<>();
    // Used to help with packet rewriting
    private final Map<UUID, UserConnection> connectionsByOfflineUUID = new HashMap<>();
    private final Map<UUID, UserConnection> connectionsByUUID = new HashMap<>();
    private final ReadWriteLock connectionLock = new ReentrantReadWriteLock();
    /**
     * Lock to protect the shutdown process from being triggered simultaneously
     * from multiple sources.
     */
    private final ReentrantLock shutdownLock = new ReentrantLock();
    private final Collection<String> pluginChannels = new HashSet<>();
    @Getter
    private final File pluginsFolder = new File("plugins");
    @Getter
    private final BungeeScheduler scheduler = new BungeeScheduler();
    // Waterfall start - Remove ConsoleReader for JLine 3 update
    /*
    @Getter
    private final ConsoleReader consoleReader;
    */
    // Waterfall end
    @Getter
    private final Logger logger;
    @Getter
    private final XenonCore xenonInstance;
    /**
     * Current operation state.
     */
    public volatile boolean isRunning;
    public EventLoopGroup bossEventLoopGroup, workerEventLoopGroup;
    /**
     * Localization formats.
     */
    private Map<String, Format> messageFormats;
    @Getter
    @Setter
    private ConfigurationAdapter configurationAdapter = new YamlConfig();
    @Getter
    private ConnectionThrottle connectionThrottle;

    @Getter
    @Setter
    private ReconnectHandler reconnectHandler;

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public BungeeCord(boolean isDev) throws IOException {
        ProxyServer.setInstance(this);
        this.xenonInstance = new XenonCore(isDev);
        // Java uses ! to indicate a resource inside of a jar/zip/other container. Running Bungee from within a directory that has a ! will cause this to muck up.
        Preconditions.checkState(new File(".").getAbsolutePath().indexOf('!') == -1, "Cannot use Waterfall in directory with ! in path.");

        reloadMessages();

        System.setProperty("library.jansi.version", "BungeeCord");

        logger = io.github.waterfallmc.waterfall.log4j.WaterfallLogger.create();
        // Waterfall end

        pluginManager = new PluginManager(this);

        xenonInstance.setConfigData(xenonInstance.getConfiguration().init());

        Preconditions.checkState(xenonInstance.getConfigData() != null,
                "Something caused config to be null? Maybe config is for older version. delete it once.");

        if (Boolean.getBoolean("net.md_5.bungee.native.disable")) return;

        xenonInstance.logdebuginfo("Using " + (EncryptionUtil.nativeFactory.load() ? "mbed TLS based native" : "standard Java JCE") + " cipher.");
        xenonInstance.logdebuginfo("Using " + (CompressFactory.zlib.load() ? "zlib based native" : "standard Java") + " compressor.");
    }

    public static BungeeCord getInstance() {
        return (BungeeCord) ProxyServer.getInstance();
    }

    /**
     * Start this proxy instance by loading the configuration, plugins and
     * starting the connect thread.
     *
     * @throws Exception any critical errors encountered
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void start(long startTime) throws Exception {
        xenonInstance.getLogger().info("Enabled XenonCord {}", XenonCore.instance.getVersion());

        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0"); // Seems to cause Bungee to stop accepting connections

        if (System.getProperty("io.netty.leakDetectionLevel") == null && System.getProperty("io.netty.leakDetection.level") == null)
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED); // Eats performance

        bossEventLoopGroup = PipelineUtils.newEventLoopGroup(
                0, new ThreadFactoryBuilder().setNameFormat("Netty Boss IO Thread #%1$d").build());
        workerEventLoopGroup = PipelineUtils.newEventLoopGroup(
                0, new ThreadFactoryBuilder().setNameFormat("Netty Worker IO Thread #%1$d").build());

        xenonInstance.logdebuginfo("Loading bungee config and checking plugins folder...");

        pluginsFolder.mkdir();
        config.load();

        xenonInstance.logdebuginfo("Registering channels...");
        registerChannel(PluginMessage.BUNGEE_CHANNEL_LEGACY);
        registerChannel(ForgeConstants.FML_TAG);
        registerChannel(ForgeConstants.FML_HANDSHAKE_TAG);
        registerChannel(ForgeConstants.FORGE_REGISTER);

        isRunning = true;

        XenonCore.instance.getTaskManager().cachedAsync(() -> {
            xenonInstance.logdebuginfo("ASYNC task command registerer is starting...");
            new Reflections("ir.xenoncommunity.commands").getSubTypesOf(Command.class).stream().filter(
                    command -> !command.getSimpleName().toLowerCase().contains("playercommand")).forEach(command -> {
                try {
                    this.getPluginManager().registerCommand(null, command.newInstance());
                } catch (Exception e) {
                    XenonCore.instance.getLogger().error(e.getMessage());
                }
            });
            xenonInstance.logdebuginfo("Commands are loaded!");
            xenonInstance.logdebuginfo("ASYNC task command registerer is shutting down...");
        });

        xenonInstance.logdebuginfo("plugin loader is starting...");
        pluginManager.detectPlugins(pluginsFolder);
        pluginManager.loadPlugins();
        pluginManager.enablePlugins();
        xenonInstance.logdebuginfo("Plugins are loaded!");

        if (config.getThrottle() > 0)
            connectionThrottle = new ConnectionThrottle(config.getThrottle(), config.getThrottleLimit());

        startListeners();

        xenonInstance.logdebuginfo("Adding shutdown hook...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> independentThreadStop(getTranslation("restart"), false)));

        XenonCore.instance.init(startTime);
    }


    public void startListeners() {
        xenonInstance.getTaskManager().add(() -> {
            xenonInstance.logdebuginfo("ASYNC task start listeners is starting...");
            config.getListeners().forEach(info -> {
                if (info.isProxyProtocol()) {
                    xenonInstance.logdebuginfo(String.format("Using PROXY protocol for listener %s, please ensure this listener is adequately firewalled.", info.getSocketAddress().toString()));

                    if (connectionThrottle != null) {
                        connectionThrottle = null;
                        xenonInstance.logdebuginfo("Since PROXY protocol is in use, internal connection throttle has been disabled.");
                    }
                }

                final ChannelFutureListener listener = future -> {
                    if (future.isSuccess()) {
                        listeners.add(future.channel());
                        xenonInstance.logdebuginfo(String.format("Listening on %s", info.getSocketAddress()));
                    } else {
                        xenonInstance.logdebuginfo("Could not bind to host " + info.getSocketAddress());
                    }
                };

                new ServerBootstrap()
                        .channelFactory(PipelineUtils.getServerChannelFactory(info.getSocketAddress()))
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .childAttr(PipelineUtils.LISTENER, info)
                        .childHandler(PipelineUtils.SERVER_CHILD)
                        .group(bossEventLoopGroup, workerEventLoopGroup)
                        .localAddress(info.getSocketAddress())
                        .bind().addListener(listener);

                if (!info.isQueryEnabled()) return;

                Preconditions.checkArgument(info.getSocketAddress() instanceof InetSocketAddress, "Can only create query listener on UDP address");

                final ChannelFutureListener bindListener = future -> {
                    if (future.isSuccess()) {
                        listeners.add(future.channel());
                        xenonInstance.logdebuginfo(String.format("Started query on %s", future.channel().localAddress()));
                    } else {
                        xenonInstance.logdebuginfo(String.format("Could not bind to host %s %s", info.getSocketAddress(), future.cause()));
                    }
                };

                new RemoteQuery(this, info).start(
                        PipelineUtils.getDatagramChannel(),
                        new InetSocketAddress(info.getHost().getAddress(), info.getQueryPort()),
                        workerEventLoopGroup,
                        bindListener
                );
            });
            xenonInstance.logdebuginfo("ASYNC task start listeners is shutting down...");
        });
    }


    public void stopListeners() {
        xenonInstance.logdebuginfo("Closing listeners...");
        listeners.forEach(listener -> {
            xenonInstance.logdebuginfo(String.format("Closing listener %s", listener));
            try {
                listener.close().syncUninterruptibly();
            } catch (ChannelException ex) {
                getLogger().severe("Could not close listen thread");
            }
        });
        listeners.clear();
    }

    @Override
    public void stop() {
        stop(getTranslation("restart"));
    }

    @Override
    public void stop(String reason) {
        new Thread("Shutdown Thread") {
            @Override
            public void run() {
                independentThreadStop(reason, true);
            }
        }.start();
    }

    // This must be run on a separate thread to avoid deadlock!
    @SuppressFBWarnings("DM_EXIT")
    @SuppressWarnings("TooBroadCatch")
    private void independentThreadStop(String reason, boolean callSystemExit) {
        xenonInstance.shutdown();

        shutdownLock.lock();

        // Acquired the shutdown lock
        if (!isRunning) {
            shutdownLock.unlock();
            return;
        }
        isRunning = false;

        stopListeners();
        xenonInstance.logdebuginfo("Closing pending connections");

        connectionLock.readLock().lock();
        try {
            xenonInstance.logdebuginfo(String.format("Disconnecting %s connections", connections.size()));
            connections.values().forEach(user -> user.disconnect(reason));
        } finally {
            connectionLock.readLock().unlock();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }


        saveThread.cancel();

        getLogger().info("Disabling plugins");
        Lists.reverse(new ArrayList<>(pluginManager.getPlugins())).forEach(plugin -> {
            try {
                plugin.onDisable();
                Arrays.stream(plugin.getLogger().getHandlers()).forEach(Handler::close);
            } catch (Throwable t) {
                final String msg = "Exception disabling plugin " + plugin.getDescription().getName();
                xenonInstance.logdebuginfo(String.format("%s %s", msg, t));
                pluginManager.callEvent(new ProxyExceptionEvent(new ProxyPluginEnableDisableException(msg, t, plugin)));
            }
            getScheduler().cancel(plugin);
            plugin.getExecutorService().shutdownNow();
        });

        getLogger().info("Closing IO threads");
        bossEventLoopGroup.shutdownGracefully();
        workerEventLoopGroup.shutdownGracefully();
        while (true) {
            try {
                bossEventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                workerEventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }

        getLogger().info("Thank you for using XenonCord!");
        // Need to close loggers after last message!
        org.apache.logging.log4j.LogManager.shutdown(); // Waterfall

        // Unlock the thread before optionally calling system exit, which might invoke this function again.
        // If that happens, the system will obtain the lock, and then see that isRunning == false and return without doing anything.
        shutdownLock.unlock();

        if (callSystemExit) {
            System.exit(0);
        }
    }

    /**
     * Broadcasts a packet to all clients that is connected to this instance.
     *
     * @param packet the packet to send
     */
    public void broadcast(DefinedPacket packet) {
        connectionLock.readLock().lock();
        try {
            connections.values().forEach(con -> {
                con.unsafe().sendPacket(packet);
            });
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public String getName() {
        return "XenonCord";
    }

    @Override
    public String getVersion() {
        return xenonInstance.getVersion();
    }

    public final void reloadMessages() {
        final Map<String, Format> cachedFormats = new HashMap<>();
        final File file = new File("messages.properties");

        if (file.isFile()) {
            try (FileReader rd = new FileReader(file)) {
                cacheResourceBundle(cachedFormats, new PropertyResourceBundle(rd));
            } catch (IOException ex) {
                xenonInstance.logdebuginfo(String.format("Could not load custom messages.properties %s", ex));
            }
        }

        ResourceBundle baseBundle;
        try {
            baseBundle = ResourceBundle.getBundle("messages");
        } catch (MissingResourceException ex) {
            baseBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
        cacheResourceBundle(cachedFormats, baseBundle);

        messageFormats = Collections.unmodifiableMap(cachedFormats);
    }

    private void cacheResourceBundle(Map<String, Format> map, ResourceBundle resourceBundle) {
        final Enumeration<String> keys = resourceBundle.getKeys();
        while (keys.hasMoreElements()) {
            map.computeIfAbsent(keys.nextElement(), (key) -> new MessageFormat(resourceBundle.getString(key)));
        }
    }

    @Override
    public String getTranslation(String name, Object... args) {
        final Format format = messageFormats.get(name);
        return (format != null) ? format.format(args) : "<translation '" + name + "' missing>";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ProxiedPlayer> getPlayers() {
        connectionLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new HashSet(connections.values()));
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public int getOnlineCount() {
        return connections.size();
    }

    @Override
    public ProxiedPlayer getPlayer(String name) {
        connectionLock.readLock().lock();
        try {
            return connections.get(name);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    public UserConnection getPlayerByOfflineUUID(UUID uuid) {
        if (uuid.version() != 3) {
            return null;
        }
        connectionLock.readLock().lock();
        try {
            return connectionsByOfflineUUID.get(uuid);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public ProxiedPlayer getPlayer(UUID uuid) {
        connectionLock.readLock().lock();
        try {
            return connectionsByUUID.get(uuid);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ServerInfo> getServers() {
        return config.getServers();
    }

    // Waterfall start
    @Override
    public Map<String, ServerInfo> getServersCopy() {
        return config.getServersCopy();
    }
    // Waterfall end

    @Override
    public ServerInfo getServerInfo(String name) {
        return config.getServerInfo(name); // Waterfall
    }

    @Override
    @Synchronized("pluginChannels")
    public void registerChannel(String channel) {
        pluginChannels.add(channel);
    }

    @Override
    @Synchronized("pluginChannels")
    public void unregisterChannel(String channel) {
        pluginChannels.remove(channel);
    }

    @Override
    @Synchronized("pluginChannels")
    public Collection<String> getChannels() {
        return Collections.unmodifiableCollection(pluginChannels);
    }

    public PluginMessage registerChannels(int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            return new PluginMessage("minecraft:register", pluginChannels.stream()
                    .map(PluginMessage.MODERNISE)
                    .collect(Collectors.joining("\00")).getBytes(StandardCharsets.UTF_8), false);
        }

        return new PluginMessage("REGISTER", String.join("\00", pluginChannels).getBytes(StandardCharsets.UTF_8), false);
    }

    @Override
    public int getProtocolVersion() {
        return ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1);
    }

    @Override
    public String getGameVersion() {
        return getConfig().getGameVersion(); // Waterfall
    }

    @Override
    public ServerInfo constructServerInfo(String name, InetSocketAddress address, String motd, boolean restricted) {
        return constructServerInfo(name, (SocketAddress) address, motd, restricted);
    }

    @Override
    public ServerInfo constructServerInfo(String name, SocketAddress address, String motd, boolean restricted) {
        return new BungeeServerInfo(name, address, motd, restricted);
    }

    @Override
    public CommandSender getConsole() {
        return ConsoleCommandSender.getInstance();
    }

    @Override
    public void broadcast(String message) {
        broadcast(TextComponent.fromLegacy(message));
    }

    @Override
    public void broadcast(BaseComponent... message) {
        getConsole().sendMessage(message);
        getPlayers().forEach(player -> player.sendMessage(message));
    }

    @Override
    public void broadcast(BaseComponent message) {
        getConsole().sendMessage(message);
        getPlayers().forEach(player -> player.sendMessage(message));
    }

    public boolean addConnection(UserConnection con) {
        UUID offlineId = con.getPendingConnection().getOfflineId();
        if (offlineId != null && offlineId.version() != 3)
            throw new IllegalArgumentException("Offline UUID must be a name-based UUID");

        final String name = con.getName();

        connectionLock.writeLock().lock();
        try {
            if (connections.containsKey(name) || connectionsByUUID.containsKey(con.getUniqueId()) || connectionsByOfflineUUID.containsKey(offlineId))
                return false;

            connections.put(name, con);
            connectionsByUUID.put(con.getUniqueId(), con);
            connectionsByOfflineUUID.put(offlineId, con);
        } finally {
            connectionLock.writeLock().unlock();
        }
        return true;
    }

    public void removeConnection(UserConnection con) {
        connectionLock.writeLock().lock();
        try {
            // TODO See #1218
            if (connections.get(con.getName()) == con) {
                connections.remove(con.getName());
                connectionsByUUID.remove(con.getUniqueId());
                connectionsByOfflineUUID.remove(con.getPendingConnection().getOfflineId());
            }
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<String> getDisabledCommands() {
        return config.getDisabledCommands();
    }

    @Override
    public Collection<ProxiedPlayer> matchPlayer(String partialName) {
        Preconditions.checkNotNull(partialName, "partialName");
        final ProxiedPlayer exactMatch = getPlayer(partialName);

        return exactMatch != null ? Collections.singleton(exactMatch) : Sets.newHashSet(Iterables.filter(getPlayers(), input -> input != null && input.getName().toLowerCase(Locale.ROOT).startsWith(partialName.toLowerCase(Locale.ROOT))));
    }

    @Override
    public Title createTitle() {
        return new BungeeTitle();
    }
}
