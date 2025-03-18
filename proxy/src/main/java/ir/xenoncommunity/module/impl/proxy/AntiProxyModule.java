package ir.xenoncommunity.module.impl.proxy;

import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import ir.xenoncommunity.utils.Colorize;
import ir.xenoncommunity.utils.HttpClient;
import lombok.Getter;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "AntiProxy", version = 1.0, description = "Restricts ")
public class AntiProxyModule extends ModuleBase {


    private final Pattern ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    @Getter
    private final ConcurrentLinkedQueue<String> proxyList = new ConcurrentLinkedQueue<>();


    @Override
    public void onInit() {
        if (!getConfig().getModules().getAnti_proxy_module().isEnabled())
            return;
        getServer().getPluginManager().registerListener(null, this);

        getTaskManager().repeatingTask(this::fetchProxies, 0, getConfig().getModules().getAnti_proxy_module().getUpdate_interval(), TimeUnit.MINUTES);

    }


    public void fetchProxies() {
        getLogger().info(Colorize.console("&bFetching proxies from config links...."));
        proxyList.clear();
        for (String s : getConfig().getModules().getAnti_proxy_module().getProxy_links()) {
            try {
                final ArrayList<String> fetchList = HttpClient.get(new URL(s)).get();
                for (String line : fetchList) {
                    final Matcher matcher = ipPattern.matcher(line);
                    while (matcher.find()) {
                        proxyList.add(matcher.group());
                    }
                }
                getLogger().info(Colorize.console(String.format("&6Fetched &c%s &aTotal: &4%d", s, fetchList.size())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        getLogger().info(Colorize.console(String.format("&bFetching DONE! total cached proxies: %d", proxyList.size())));
    }


    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        event.setCancelled(proxyList.contains(event.getConnection().getAddress().getAddress().getHostAddress()));
    }
}
