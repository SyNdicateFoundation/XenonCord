package net.md_5.bungee.api.plugin;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Represents any Plugin that may be loaded at runtime to enhance existing
 * functionality.
 */
public class Plugin {

    @Getter
    private PluginDescription description;
    @Getter
    private ProxyServer proxy;
    @Getter
    private File file;
    @Getter
    private Logger logger;
    //
    private ExecutorService service;

    public Plugin() {
        ClassLoader classLoader = getClass().getClassLoader();
        Preconditions.checkState(classLoader instanceof PluginClassloader, "Plugin requires " + PluginClassloader.class.getName());

        ((PluginClassloader) classLoader).init(this);
    }

    protected Plugin(ProxyServer proxy, PluginDescription description) {
        ClassLoader classLoader = getClass().getClassLoader();
        Preconditions.checkState(!(classLoader instanceof PluginClassloader), "Cannot use initialization constructor at runtime");

        // init( proxy, description );
    }
    // Waterfall end

    // Waterfall start - Allow plugins to use SLF4J for logging
    public org.slf4j.Logger getSLF4JLogger() {
        return org.slf4j.LoggerFactory.getLogger(logger.getName());
    }

    /**
     * Called when the plugin has just been loaded. Most of the proxy will not
     * be initialized, so only use it for registering
     * {@link ConfigurationAdapter}'s and other predefined behavior.
     */
    public void onLoad() {
    }

    /**
     * Called when this plugin is enabled.
     */
    public void onEnable() {
    }

    /**
     * Called when this plugin is disabled.
     */
    public void onDisable() {
    }

    /**
     * Gets the data folder where this plugin may store arbitrary data. It will
     * be a child of {@link ProxyServer#getPluginsFolder()}.
     *
     * @return the data folder of this plugin
     */
    public final File getDataFolder() {
        return new File(getProxy().getPluginsFolder(), getDescription().getName());
    }

    /**
     * Get a resource from within this plugins jar or container. Care must be
     * taken to close the returned stream.
     *
     * @param name the full path name of this resource
     * @return the stream for getting this resource, or null if it does not
     * exist
     */
    public final InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    /**
     * Called by the loader to initialize the fields in this plugin.
     *
     * @param proxy       current proxy instance
     * @param description the description that describes this plugin
     */
    final void init(ProxyServer proxy, PluginDescription description) {
        this.proxy = proxy;
        this.description = description;
        this.file = description.getFile();
        this.logger = Logger.getLogger(description.getName()); // Waterfall - Handle plugin prefixes in implementation
    }

    @Deprecated
    public ExecutorService getExecutorService() {
        if (service == null) {
            String name = (getDescription() == null) ? "unknown" : getDescription().getName();
            service = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(name + " Pool Thread #%1$d")
                    .setThreadFactory(new GroupedThreadFactory(this, name)).build());
        }
        return service;
    }
    //
}
