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
import ir.xenoncommunity.commands.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.chat.*;
import net.md_5.bungee.command.*;
import net.md_5.bungee.compress.CompressFactory;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.conf.YamlConfig;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.query.RemoteQuery;
import net.md_5.bungee.scheduler.BungeeScheduler;
import net.md_5.bungee.util.CaseInsensitiveMap;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main BungeeCord proxy class.
 */
public class BungeeCord extends ProxyServer
{

    /**
     * Current operation state.
     */
    public volatile boolean isRunning;
    /**
     * Configuration.
     */
    @Getter
    public final Configuration config = new WaterfallConfiguration();
    /**
     * Localization formats.
     */
    private Map<String, Format> messageFormats;
    public EventLoopGroup bossEventLoopGroup, workerEventLoopGroup;
    /**
     * locations.yml save thread.
     */
    private final Timer saveThread = new Timer( "Reconnect Saver" );
    // private final Timer metricsThread = new Timer( "Metrics Thread" ); // Waterfall: Disable Metrics
    /**
     * Server socket listener.
     */
    private final Collection<Channel> listeners = new HashSet<>();
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
    /**
     * Plugin manager.
     */
    @Getter
    public final PluginManager pluginManager;
    @Getter
    @Setter
    private ReconnectHandler reconnectHandler;
    @Getter
    @Setter
    private ConfigurationAdapter configurationAdapter = new YamlConfig();
    private final Collection<String> pluginChannels = new HashSet<>();
    @Getter
    private final File pluginsFolder = new File( "plugins" );
    @Getter
    private final BungeeScheduler scheduler = new BungeeScheduler();
    @Getter
    private final Logger logger;
    public final Gson gson = new GsonBuilder()
            .registerTypeAdapter( BaseComponent.class, new ComponentSerializer() )
            .registerTypeAdapter( TextComponent.class, new TextComponentSerializer() )
            .registerTypeAdapter( TranslatableComponent.class, new TranslatableComponentSerializer() )
            .registerTypeAdapter( KeybindComponent.class, new KeybindComponentSerializer() )
            .registerTypeAdapter( ScoreComponent.class, new ScoreComponentSerializer() )
            .registerTypeAdapter( SelectorComponent.class, new SelectorComponentSerializer() )
            .registerTypeAdapter( ComponentStyle.class, new ComponentStyleSerializer() )
            .registerTypeAdapter( ServerPing.PlayerInfo.class, new PlayerInfoSerializer() )
            .registerTypeAdapter( Favicon.class, Favicon.getFaviconTypeAdapter() ).create();
    @Getter
    private ConnectionThrottle connectionThrottle;
    public static BungeeCord getInstance()
    {
        return (BungeeCord) ProxyServer.getInstance();
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public BungeeCord() throws IOException
    {
        Preconditions.checkState( new File( "." ).getAbsolutePath().indexOf( '!' ) == -1, "Please, move XenonCord to another directory without ! sign in name." );

        reloadMessages();

        System.setProperty( "library.jansi.version", "BungeeCord" );

        logger = io.github.waterfallmc.waterfall.log4j.WaterfallLogger.create();

        pluginManager = new PluginManager( this );

        if ( Boolean.getBoolean( "net.md_5.bungee.native.disable" ) )return;

        logger.info(String.format("Using %s.", EncryptionUtil.nativeFactory.load() ? "mbed TLS based native cipher" : "Using standard Java JCE cipher"));
        logger.info(String.format("Using %s.", CompressFactory.zlib.load() ? "zlib based native compressor" : "standard Java compressor"));
    }

    /**
     * Start this proxy instance by loading the configuration, plugins and
     * starting the connect thread.
     *
     * @throws Exception any critical errors encountered
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void start(final long startTime) throws Exception
    {
        getLogger().info( "Enabled XenonCord!");
        ProxyServer.setInstance(this);
        // Initialize all variables
        new XenonCore();

        System.setProperty( "io.netty.selectorAutoRebuildThreshold", "0" ); // Seems to cause Bungee to stop accepting connections
        if ( System.getProperty( "io.netty.leakDetectionLevel" ) == null && System.getProperty( "io.netty.leakDetection.level" ) == null )
            ResourceLeakDetector.setLevel( ResourceLeakDetector.Level.DISABLED ); // Eats performance

        bossEventLoopGroup = PipelineUtils.newEventLoopGroup( 0, new ThreadFactoryBuilder().setNameFormat( "Netty Boss IO Thread #%1$d" ).build() );
        workerEventLoopGroup = PipelineUtils.newEventLoopGroup( 0, new ThreadFactoryBuilder().setNameFormat( "Netty Worker IO Thread #%1$d" ).build() );

        pluginsFolder.mkdir();
        config.load();

        registerChannel( ForgeConstants.FML_TAG );
        registerChannel( ForgeConstants.FML_HANDSHAKE_TAG );
        registerChannel( ForgeConstants.FORGE_REGISTER );

        isRunning = true;

        XenonCore.instance.getTaskManager().add(() ->{
            XenonCore.instance.getLogger().info("ASYNC task command registerer is starting...");
            new Reflections("ir.xenoncommunity.commands").getSubTypesOf(Command.class).stream().filter(
                    command -> !command.getSimpleName().toLowerCase().contains("playercommand")).forEach(command -> {
                try {
                    XenonCore.instance.getLogger().info(String.format("Command %s registered.", command.getSimpleName()));
                    this.getPluginManager().registerCommand(null, command.newInstance());
                } catch (Exception e) {
                    XenonCore.instance.getLogger().error(e.getMessage());
                }
            });
        });
        XenonCore.instance.getTaskManager().independentTask(() -> {
            XenonCore.instance.getLogger().info("ASYNC task plugin loader is starting...");
            pluginManager.detectPlugins( pluginsFolder );
            pluginManager.loadPlugins();
            XenonCore.instance.setProxyCompletlyLoaded(pluginManager.enablePlugins());
            XenonCore.instance.getLogger().info("Plugins are loaded!");
        });


        if ( config.getThrottle() > 0 )
            connectionThrottle = new ConnectionThrottle( config.getThrottle(), config.getThrottleLimit() );

        startListeners();

        saveThread.scheduleAtFixedRate( new TimerTask()
        {
            @Override
            public void run()
            {
                if ( getReconnectHandler() != null )
                {
                    getReconnectHandler().save();
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis( 5 ) );
        //metricsThread.scheduleAtFixedRate( new Metrics(), 0, TimeUnit.MINUTES.toMillis( Metrics.PING_INTERVAL ) ); // Waterfall: Disable Metrics

        Runtime.getRuntime().addShutdownHook(new Thread(() -> independentThreadStop( getTranslation( "restart" ), false )));

        XenonCore.instance.init(startTime);
    }

    public void startListeners()
    {
        config.getListeners().forEach(info -> {
            if ( info.isProxyProtocol() )
            {
                getLogger().log( Level.WARNING, "Using PROXY protocol for listener {0}, please ensure this listener is adequately firewalled & connection throttle is disabled.", info.getSocketAddress() );
                connectionThrottle = null;
            }

            ChannelFutureListener listener = future -> {
                if ( future.isSuccess() )
                    listeners.add(future.channel());

                getLogger().log( Level.INFO, String.format("%s", future.isSuccess() ? "Listening on {0}" : "Could not bind to host {0} " + future.cause()), info.getHost());
            };
            new ServerBootstrap()
                    .channelFactory( PipelineUtils.getServerChannelFactory( info.getSocketAddress() ) ) // Waterfall - netty reflection -> factory
                    .option( ChannelOption.SO_REUSEADDR, true ) // TODO: Move this elsewhere!
                    .childAttr( PipelineUtils.LISTENER, info )
                    .childHandler( PipelineUtils.SERVER_CHILD )
                    .group( bossEventLoopGroup, workerEventLoopGroup )
                    .localAddress( info.getSocketAddress() )
                    .bind().addListener( listener );

            if ( !info.isQueryEnabled() ) return;

            Preconditions.checkArgument( info.getSocketAddress() instanceof InetSocketAddress, "Can only create query listener on UDP address" );

            ChannelFutureListener bindListener = future -> {
                if ( future.isSuccess() )
                {
                    listeners.add( future.channel() );
                    getLogger().log( Level.INFO, "Started query on {0}", future.channel().localAddress() );
                } else
                    getLogger().log( Level.WARNING, "Could not bind to host " + info.getSocketAddress(), future.cause() );
            };

            new RemoteQuery( this, info ).start( PipelineUtils.getDatagramChannel(), new InetSocketAddress( info.getHost().getAddress(), info.getQueryPort() ), workerEventLoopGroup, bindListener );

        });
    }

    public void stopListeners()
    {
        listeners.forEach(listener -> {
            getLogger().log( Level.INFO, "Closing listener {0}", listener );
            try
            {
                listener.close().syncUninterruptibly();
            } catch ( ChannelException ex )
            {
                getLogger().severe( "Could not close listen thread" );
            }
        });
        listeners.clear();
    }

    @Override
    public void stop()
    {
        stop( getTranslation( "restart" ) );
    }

    @Override
    public void stop(final String reason)
    {
        new Thread( "Shutdown Thread" )
        {
            @Override
            public void run()
            {
                independentThreadStop( reason, true );
            }
        }.start();
    }

    // This must be run on a separate thread to avoid deadlock!
    @SuppressFBWarnings("DM_EXIT")
    @SuppressWarnings({"TooBroadCatch", "ResultOfMethodCallIgnored"})

    private void independentThreadStop(final String reason, boolean callSystemExit)
    {
        XenonCore.instance.shutdown();
        // Acquire the shutdown lock
        // This needs to actually block here, otherwise running 'end' and then ctrl+c will cause the thread to terminate prematurely
        shutdownLock.lock();

        // Acquired the shutdown lock
        if ( !isRunning )
        {
            // Server is already shutting down - nothing to do
            shutdownLock.unlock();
            return;
        }
        isRunning = false;

        stopListeners();
        getLogger().info( "Closing pending connections" );

        connectionLock.readLock().lock();
        try
        {
            getLogger().log( Level.INFO, "Disconnecting {0} connections", connections.size() );
            for ( UserConnection user : connections.values() )
                user.disconnect( reason );
        } finally
        {
            connectionLock.readLock().unlock();
        }

        try
        {
            Thread.sleep( 500 );
        } catch ( InterruptedException ignored)
        {
        }

        if ( reconnectHandler != null )
        {
            getLogger().info( "Saving reconnect locations" );
            reconnectHandler.save();
            reconnectHandler.close();
        }
        saveThread.cancel();
        //metricsThread.cancel(); // Waterfall: Disable Metrics

        getLogger().info( "Disabling plugins" );
        for ( Plugin plugin : Lists.reverse( new ArrayList<>( pluginManager.getPlugins() ) ) )
        {
            try
            {
                plugin.onDisable();
                for ( Handler handler : plugin.getLogger().getHandlers() )
                    handler.close();
            } catch ( Throwable t )
            {
                // Waterfall start - throw exception event
                String msg = "Exception while disabling plugin " + plugin.getDescription().getName();
                getLogger().log( Level.SEVERE, msg, t );
                pluginManager.callEvent( new ProxyExceptionEvent( new ProxyPluginEnableDisableException( msg, t, plugin) ) );
                // Waterfall end
            }
            getScheduler().cancel( plugin );
            plugin.getExecutorService().shutdownNow();
        }

        getLogger().info( "Closing IO threads" );
        bossEventLoopGroup.shutdownGracefully();
        workerEventLoopGroup.shutdownGracefully();
        while (true) {
            try {
                bossEventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                workerEventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException ignored) {}
        }

        getLogger().info( "Thank you and goodbye" );
        // Need to close loggers after last message!
        org.apache.logging.log4j.LogManager.shutdown(); // Waterfall

        // Unlock the thread before optionally calling system exit, which might invoke this function again.
        // If that happens, the system will obtain the lock, and then see that isRunning == false and return without doing anything.
        shutdownLock.unlock();

        if ( callSystemExit )
            System.exit( 0 );
    }

    /*
     * Broadcasts a packet to all clients that is connected to this instance.
     *
     * @param packet the packet to send
     */
    /*public void broadcast(DefinedPacket packet)
    {
        connectionLock.readLock().lock();
        try
        {
            for ( UserConnection con : connections.values() )
            {
                con.unsafe().sendPacket( packet );
            }
        } finally
        {
            connectionLock.readLock().unlock();
        }
    }*/

    @Override
    public String getName()
    {
        return "Waterfall";
    }

    @Override
    public String getVersion()
    {
        return ( BungeeCord.class.getPackage().getImplementationVersion() == null ) ? "unknown" : BungeeCord.class.getPackage().getImplementationVersion();
    }

    public final void reloadMessages()
    {
        Map<String, Format> cachedFormats = new HashMap<>();

        File file = new File( "messages.properties" );
        if ( file.isFile() )
        {
            try ( FileReader rd = new FileReader( file ) )
            {
                cacheResourceBundle( cachedFormats, new PropertyResourceBundle( rd ) );
            } catch ( IOException ex )
            {
                getLogger().log( Level.SEVERE, "Could not load custom messages.properties", ex );
            }
        }

        ResourceBundle baseBundle;
        try
        {
            baseBundle = ResourceBundle.getBundle( "messages" );
        } catch ( MissingResourceException ex )
        {
            baseBundle = ResourceBundle.getBundle( "messages", Locale.ENGLISH );
        }
        cacheResourceBundle( cachedFormats, baseBundle );

        messageFormats = Collections.unmodifiableMap( cachedFormats );
    }

    private void cacheResourceBundle(Map<String, Format> map, ResourceBundle resourceBundle)
    {
        Enumeration<String> keys = resourceBundle.getKeys();
        while ( keys.hasMoreElements() )
        {
            map.computeIfAbsent( keys.nextElement(), (key) -> new MessageFormat( resourceBundle.getString( key ) ) );
        }
    }

    @Override
    public String getTranslation(String name, Object... args)
    {
        Format format = messageFormats.get( name );
        return ( format != null ) ? format.format( args ) : "<translation '" + name + "' missing>";
    }

    @Override
    public Collection<ProxiedPlayer> getPlayers()
    {
        connectionLock.readLock().lock();
        try
        {
            return Collections.unmodifiableCollection( new HashSet<>( connections.values() ) );
        } finally
        {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public int getOnlineCount()
    {
        return connections.size();
    }

    @Override
    public ProxiedPlayer getPlayer(String name)
    {
        connectionLock.readLock().lock();
        try
        {
            return connections.get( name );
        } finally
        {
            connectionLock.readLock().unlock();
        }
    }

    public UserConnection getPlayerByOfflineUUID(final UUID uuid)
    {
        if ( uuid.version() != 3 )
        {
            return null;
        }
        connectionLock.readLock().lock();
        try
        {
            return connectionsByOfflineUUID.get( uuid );
        } finally
        {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public ProxiedPlayer getPlayer(final UUID uuid)
    {
        connectionLock.readLock().lock();
        try{
            return connectionsByUUID.get( uuid );
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ServerInfo> getServers()
    {
        return config.getServers();
    }

    // Waterfall start
    @Override
    public Map<String, ServerInfo> getServersCopy()
    {
        return config.getServersCopy();
    }
    // Waterfall end

    @Override
    public ServerInfo getServerInfo(String name)
    {
        return config.getServerInfo( name ); // Waterfall
    }

    @Override
    @Synchronized("pluginChannels")
    public void registerChannel(String channel)
    {
        pluginChannels.add( channel );
    }

    @Override
    @Synchronized("pluginChannels")
    public void unregisterChannel(String channel)
    {
        pluginChannels.remove( channel );
    }

    @Override
    @Synchronized("pluginChannels")
    public Collection<String> getChannels()
    {
        return Collections.unmodifiableCollection( pluginChannels );
    }

    public PluginMessage registerChannels(int protocolVersion)
    {
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 )
            return new PluginMessage( "minecraft:register", String.join( "\00", Iterables.transform( pluginChannels, PluginMessage.MODERNISE ) ).getBytes( StandardCharsets.UTF_8 ), false );

        return new PluginMessage( "REGISTER", String.join( "\00", pluginChannels ).getBytes( StandardCharsets.UTF_8 ), false );
    }

    @Override
    public int getProtocolVersion()
    {
        return ProtocolConstants.SUPPORTED_VERSION_IDS.get( ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1 );
    }

    @Override
    public String getGameVersion()
    {
        return getConfig().getGameVersion();
    }

    @Override
    public ServerInfo constructServerInfo(final String name, final InetSocketAddress address, final String motd, final boolean restricted)
    {
        return constructServerInfo( name, (SocketAddress) address, motd, restricted );
    }

    @Override
    public ServerInfo constructServerInfo(final String name, final SocketAddress address, final String motd, final boolean restricted)
    {
        return new BungeeServerInfo( name, address, motd, restricted );
    }

    @Override
    public CommandSender getConsole()
    {
        return ConsoleCommandSender.getInstance();
    }

    @Override
    public void broadcast(final String message)
    {
        broadcast( TextComponent.fromLegacy( message ) );
    }

    @Override
    public void broadcast(final BaseComponent... message)
    {
        getConsole().sendMessage( message );
        for ( final ProxiedPlayer player : getPlayers() )
            player.sendMessage( message );
    }

    @Override
    public void broadcast(final BaseComponent message)
    {
        getConsole().sendMessage( message );
        for ( final ProxiedPlayer player : getPlayers() )
            player.sendMessage( message );
    }

    public boolean addConnection(final UserConnection con)
    {
        final UUID offlineId = con.getPendingConnection().getOfflineId();
        if ( offlineId != null && offlineId.version() != 3 )
            throw new IllegalArgumentException( "Offline UUID must be a name-based UUID" );

        connectionLock.writeLock().lock();

        final String name = con.getName();
        final UUID uniqueID = con.getUniqueId();
        if ( connections.containsKey( name ) ||
                connectionsByUUID.containsKey( uniqueID ) ||
                connectionsByOfflineUUID.containsKey( offlineId ) )
            return false;

        try
        {
            connections.put( name, con );
            connectionsByUUID.put( uniqueID, con );
            connectionsByOfflineUUID.put( offlineId, con );
        } finally
        {
            connectionLock.writeLock().unlock();
        }
        return true;
    }

    public void removeConnection(final UserConnection con)
    {
        connectionLock.writeLock().lock();
        if (!(connections.get( con.getName() ) == con)) return;
        try
        {
            connections.remove( con.getName() );
            connectionsByUUID.remove( con.getUniqueId() );
            connectionsByOfflineUUID.remove( con.getPendingConnection().getOfflineId() );
        } finally
        {
            connectionLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<String> getDisabledCommands()
    {
        return config.getDisabledCommands();
    }

    @Override
    public Collection<ProxiedPlayer> matchPlayer(final String partialName)
    {
        Preconditions.checkNotNull( partialName, "partialName" );
        return getPlayer( partialName ) != null ?
                Collections.singletonList(getPlayer( partialName )) :
                Sets.newHashSet(Iterables.filter( getPlayers(),
                        input ->
                                input != null && input.getName().toLowerCase(Locale.ROOT).startsWith(partialName.toLowerCase(Locale.ROOT))) );
    }

    @Override
    public Title createTitle()
    {
        return new BungeeTitle();
    }
}
