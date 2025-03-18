package ir.xenoncommunity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.xenoncommunity.utils.ClassHelper;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.HttpClient;
import ir.xenoncommunity.utils.TaskManager;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.md_5.bungee.BungeeCord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
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
    @Setter
    private Configuration.ConfigData configData;
    private String version;
    /**
     * Initializes all required variables.
     */
    public XenonCore(boolean isDev) {
        instance = this;
        this.logger = LogManager.getLogger(this.getClass().getSimpleName());
        this.taskManager = new TaskManager();
        this.bungeeInstance = BungeeCord.getInstance();
        this.configuration = new Configuration();
        final StringBuilder sb = new StringBuilder();
        try {
            HttpClient.get(new URL("https://api.github.com/repos/SyNdicateFoundation/XenonCord/releases/latest")).get().forEach(
                    sb::append
            );

            this.version = JsonParser.parseString(sb.toString()).getAsJsonObject().get("tag_name").getAsString();
        } catch(Exception e){
            this.version = "unknown";
            e.printStackTrace();
        }

        if(!isDev)
            new Metrics(this.logger, 25130);

    }

    /**
     * Called when proxy is loaded.
     */
    public void init(long startTime) {
        ClassHelper.registerModules();
        getLogger().info("Successfully booted! Loading the proxy server with plugins took: {}ms", System.currentTimeMillis() - startTime);

        if (configData.isSocket_backend()) XenonCore.instance.getTaskManager().async(this::initBackend);
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

    /**
     * Initializes command execution backend support
     */
    @SneakyThrows
    private void initBackend() {
        @Cleanup final ServerSocket serverSocket = new ServerSocket(20019, 50, InetAddress.getByName("127.0.0.1"));

        while (true) {
            @Cleanup final Socket socket = serverSocket.accept();
            @Cleanup final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String req;
            while ((req = br.readLine()) != null) {
                XenonCore.instance.getBungeeInstance().getPluginManager().dispatchCommand(
                        XenonCore.instance.getBungeeInstance().getConsole(), req
                );
            }
        }
    }
}
