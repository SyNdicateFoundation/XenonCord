package ir.xenoncommunity;

//import ir.xenoncommunity.listener.JoinListener;

import ir.xenoncommunity.abstracts.ModuleListener;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Getter
public class XenonCore {
    public static XenonCore instance;
    @Setter private boolean isProxyCompletlyLoaded;
    private final Logger logger;
    private final TaskManager taskManager;
    private final BungeeCord bungeeInstance;
    private final Configuration configuration;
    @Setter private Configuration.ConfigData configData;
    /**
     * Initializes all required variables.
     */
    public XenonCore(){
        instance = this;
        this.logger = LogManager.getLogger(this.getClass().getSimpleName());
        this.taskManager = new TaskManager();
        this.bungeeInstance = BungeeCord.getInstance();
        this.configuration = new Configuration();
    }
    /**
     * Called when proxy is loaded.
     */
    public void init(final long startTime){
        setConfigData(configuration.init());
        //bungeeInstance.getPluginManager().registerListener(null , new JoinListener());
        getTaskManager().independentTask(() -> {
            configData.setLoadingmessage(configData.getLoadingmessage().replace("PREFIX", configData.getPrefix()));
            while(!isProxyCompletlyLoaded)
                bungeeInstance.getPlayers().forEach(proxiedPlayer -> proxiedPlayer.disconnect(ChatColor.translateAlternateColorCodes('&', configData.getLoadingmessage())));

            ModuleListener.init();
        });
        getLogger().info(String.format("Done loading! took %sMS to load!", System.currentTimeMillis() - startTime));
    }

    /**
     * Called when proxy is shutting down.
     */
    public void shutdown(){
    }
}
