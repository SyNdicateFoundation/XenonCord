package ir.xenoncommunity;

import ir.xenoncommunity.gui.SwingManager;
import ir.xenoncommunity.modules.ModuleManager;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


@Getter
public class XenonCore {
    public static XenonCore instance;
    @Setter
    private boolean isProxyCompletlyLoaded;
    private final Logger logger;
    private final TaskManager taskManager;
    private final BungeeCord bungeeInstance;
    private final Configuration configuration;
    @Setter
    private Configuration.ConfigData configData;
    private final ModuleManager moduleManager;
    @Setter
    private String currentMotd;

    /**
     * Initializes all required variables.
     */
    public XenonCore() {
        instance = this;
        this.logger = LogManager.getLogger(this.getClass().getSimpleName());
        this.taskManager = new TaskManager();
        this.bungeeInstance = BungeeCord.getInstance();
        this.configuration = new Configuration();
        this.moduleManager = new ModuleManager();
    }

    /**
     * Called when proxy is loaded.
     */
    public void init(long startTime) {
        getLogger().info("Loading the proxy server itself has been done. took: {}ms", System.currentTimeMillis() - startTime);
        getTaskManager().async(() -> {
            while (!isProxyCompletlyLoaded)
                bungeeInstance.getPlayers().forEach(proxiedPlayer -> proxiedPlayer.disconnect(ChatColor.translateAlternateColorCodes('&', configData.getLoadingmessage())));

            moduleManager.init();
            SwingManager.createAndShowGUI();
            getLogger().info("Successfully booted! Loading the proxy server with plugins took: {}ms", System.currentTimeMillis() - startTime);
        });
    }

    /**
     * Called when proxy is shutting down.
     */
    public void shutdown() {
    }

    public List<String> getPlayerNames() {
        List<String> players = new ArrayList<>();
        bungeeInstance.getPlayers().forEach(player -> players.add(player.getName()));
        return players;
    }

    public String getVersion() {
        return "V1";
    }

    public void logdebuginfo(String msg) {
        if (configData.isDebug())
            logger.info(msg);
    }

    public void logdebugerror(String msg) {
        if (configData.isDebug())
            logger.error(msg);
    }
}
