package ir.xenoncommunity;

import com.google.gson.JsonParser;
import ir.xenoncommunity.utils.*;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

        if (configData.isSocket_backend()) {
            XenonCore.instance.getTaskManager().async(this::initBackend);
        }
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
        if(!getConfiguration().getSocketBackendSecretFile().exists())
            getConfiguration().getSocketBackendSecretFile().createNewFile();

        if(getConfiguration().getSocketBackendSecretFile().length() == 0){
            getLogger().info(Colorize.console("&c[NOTICE] &rsocket-backend-secret.txt is empty"));
            getLogger().info(Colorize.console("&c[NOTICE] &rXenonCord &fwill generate a secret inside this file"));
            getLogger().info(Colorize.console("&c[NOTICE] &rplease configure your plugins/XenonBanBackend with this secret, to avoid issues."));
            @Cleanup final BufferedWriter writer = new BufferedWriter(new FileWriter(getConfiguration().getSocketBackendSecretFile()));
            writer.write(new String(Util.randomAlphanumericSequence(12), StandardCharsets.UTF_8));
        }
        @Cleanup final BufferedReader reader = new BufferedReader(new FileReader(getConfiguration().getSocketBackendSecretFile()));
        final String secret = reader.readLine();
        @Cleanup final ServerSocket serverSocket = new ServerSocket(20019, 50, InetAddress.getByName("127.0.0.1"));

        while (true) {
            @Cleanup final Socket socket = serverSocket.accept();
            @Cleanup final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String req;
            while ((req = br.readLine()) != null) {
                final String reqWithoutSecret = req.replaceAll(secret, "").startsWith(" ") ? req.replaceAll(secret, "").substring(1) : req.replaceAll(secret, "");
                getLogger().info("Received a request from socket backend, request: " + reqWithoutSecret);
                if(!req.contains(secret)) {
                    getLogger().info(Colorize.console("&c[NOTICE] &rBlocked a request without secret via command exec backend. please be careful with what's happening."));
                    break;
                }
                // remove secret & space from request
                XenonCore.instance.getBungeeInstance().getPluginManager().dispatchCommand(
                        XenonCore.instance.getBungeeInstance().getConsole(), reqWithoutSecret
                );
            }
        }
    }
}
