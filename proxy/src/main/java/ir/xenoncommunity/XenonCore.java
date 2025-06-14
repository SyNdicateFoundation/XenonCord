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
        } catch (Exception e) {
            this.version = "unknown";
            e.printStackTrace();
        }
        if (!isDev)
            new Metrics(this.logger, 25130);

        System.out.println((Colorize.console(String.format(
                "&b\n__   __                       _____               _  \n" +
                        "\\ \\ / /                      /  __ \\             | | \n" +
                        " \\ V /  ___ _ __   ___  _ __ | /  \\/ ___  _ __ __| | \n" +
                        " /   \\ / _ \\ '_ \\ / _ \\| '_ \\| |    / _ \\| '__/ _` | \n" +
                        "/ /^\\ \\  __/ | | | (_) | | | | \\__/\\ (_) | | | (_| | \n" +
                        "\\/   \\/\\___|_| |_|\\___/|_| |_|\\____/\\___/|_|  \\__,_| \n" +
                        "        \n" +
                        "       &av%s - &cBy SyNdicateFoundation\n", this.version))));


    }

    /**
     * Called when proxy is loaded.
     */
    public void init(long startTime) {
        ClassHelper.registerModules();
        getLogger().info("Successfully booted! Loading the proxy server with plugins took: {}ms", System.currentTimeMillis() - startTime);
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
}
