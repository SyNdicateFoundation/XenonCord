package ir.xenoncommunity;

import ir.xenoncommunity.antibot.AntibotManager;
import ir.xenoncommunity.gui.SwingManager;
import ir.xenoncommunity.handlers.IpLimiter;
import ir.xenoncommunity.modules.ModuleManager;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Getter
public class XenonCore {
    /**
     * Declare all required variables
     */
    public static XenonCore instance;
    private final Logger logger;
    private final TaskManager taskManager;
    private final BungeeCord bungeeInstance;
    private final Configuration configuration;
    private final ModuleManager moduleManager;
    private final AntibotManager antibotManager;
    @Setter
    private boolean isProxyCompletlyLoaded;
    @Setter
    private Configuration.ConfigData configData;
    @Setter
    private String currentMotd = "def";

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
        this.antibotManager = new AntibotManager();
      //  if(currentMotd.equals("def")) currentMotd = getBungeeInstance().getConfig().get
    }

    /**
     * Called when proxy is loaded.
     */
    public void init(long startTime) {
        if(Arrays.stream(getConfigData().getWhitelisted_ips()).noneMatch(element -> element.equals("")))
            getBungeeInstance().getPluginManager().registerListener(null, new IpLimiter());
        getLogger().info("Loading the proxy server itself has been done. took: {}ms", System.currentTimeMillis() - startTime);
        getTaskManager().async(() -> {
            while (!isProxyCompletlyLoaded)
                bungeeInstance.getPlayers().forEach(proxiedPlayer -> proxiedPlayer.disconnect(configData.getLoading_message()));

            moduleManager.init();
            SwingManager.createAndShowGUI();
            getLogger().info("Successfully booted! Loading the proxy server with plugins took: {}ms", System.currentTimeMillis() - startTime);
        });
    }

    /**
     * Called when proxy is shutting down.
     */
    public void shutdown() {
        getTaskManager().shutdown();
    }

    /**
     * Returns a list of online players names
     */
    public List<String> getPlayerNames() {
        List<String> players = new ArrayList<>();
        bungeeInstance.getPlayers().forEach(player -> players.add(player.getName()));
        return players;
    }

    /**
     * Returns XenonCord's version
     * TODO: add github integration
     */
    public String getVersion() {
        return "V1";
    }

    /**
     * checks config data for debugging.
     */
    public void logdebuginfo(String msg) {
        if (configData.isDebug())
            logger.info(msg);
    }

    /**
     * checks config data for debugging.
     */
    public void logdebugerror(String msg) {
        if (configData.isDebug())
            logger.error(msg);
    }
}
