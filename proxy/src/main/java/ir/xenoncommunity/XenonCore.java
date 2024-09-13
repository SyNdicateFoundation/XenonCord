package ir.xenoncommunity;

//import ir.xenoncommunity.listener.JoinListener;

import ir.xenoncommunity.utils.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.util.ChatComponentTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Getter
public class XenonCore {
    public static XenonCore instance;
    @Setter private boolean isProxyCompletlyLoaded;
    private final Logger logger;
    private final TaskManager taskManager;
    private final BungeeCord bungeeInstance;
    /**
     * Initializes all required variables.
     */
    public XenonCore(){
        instance = this;
        this.logger = LogManager.getLogger(this);
        this.taskManager = new TaskManager();
        this.bungeeInstance = BungeeCord.getInstance();
    }
    /**
     * Called when proxy is loaded.
     */
    public void init(final long startTime){
        //bungeeInstance.getPluginManager().registerListener(null , new JoinListener());
        getTaskManager().independentTask(() -> {
            while(!isProxyCompletlyLoaded)
                bungeeInstance.getPlayers().forEach(proxiedPlayer -> proxiedPlayer.disconnect(TextComponent.fromLegacy("XenonCore is still loading plugins!\n\nYou got disconnected to prevent problems.")));
        });
        getLogger().info(String.format("Done loading! took %sMS to load!", System.currentTimeMillis() - startTime));
    }

    /**
     * Called when proxy is shutting down.
     */
    public void shutdown(){
    }
}
