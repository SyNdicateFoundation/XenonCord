package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Handler;

public class BPlugins extends Command implements Listener {
    public BPlugins() {
        super("BPlugins", XenonCore.instance.getConfigData().getModules().getPingperm());
    }

    @Override
    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginsperm())) {
            return;
        }

        PluginManager manager = XenonCore.instance.getBungeeInstance().getPluginManager();
        if (args.length == 0) {
            StringBuilder sb = new StringBuilder();

            manager.getPlugins().forEach(plugin -> sb.append(plugin.getDescription().getName()).append(", "));

            String prefix = XenonCore.instance.getConfigData().getPrefix();
            Message.send(sender, String.format("%s &cPLUGINS: %s.", prefix, sb.substring(0, sb.length() - 2)), false);
            return;
        }

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginstoggleperm())) {
            return;
        }

        File plFile = new File(String.format("plugins/%s", args[1]));
        if (!plFile.exists()) {
            Message.send(sender, String.format("%s &cPlugin does not exist in folder.", XenonCore.instance.getConfigData().getPrefix()), false);
            return;
        }

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                switch (args[0].toLowerCase()) {
                    case "load":
                        Field yamlField = PluginManager.class.getDeclaredField("yaml");
                        Field toLoadField = PluginManager.class.getDeclaredField("toLoad");
                        JarFile jar = new JarFile(plFile);

                        yamlField.setAccessible(true);
                        toLoadField.setAccessible(true);

                        @SuppressWarnings("unchecked")
                        Map<String, PluginDescription> toLoad =
                                Optional.ofNullable((Map<String, PluginDescription>) toLoadField.get(manager))
                                        .orElse(new HashMap<>());

                        PluginDescription desc = ((Yaml) yamlField.get(manager)).loadAs(
                                jar.getInputStream(
                                        Optional.ofNullable(jar.getJarEntry("bungee.yml"))
                                                .orElse(jar.getJarEntry("plugin.yml"))),
                                PluginDescription.class);

                        desc.setFile(plFile);
                        toLoad.put(desc.getName(), desc);

                        toLoadField.set(manager, toLoad);
                        manager.loadPlugins();
                        manager.getPlugin(desc.getName()).onEnable();

                        Message.send(sender, String.format("%s &cLoading %s.",
                                XenonCore.instance.getConfigData().getPrefix(), args[1]), true);
                        break;
                    case "unload":
                        Plugin plugin = manager.getPlugin(args[1]);
                        Field pluginsField = PluginManager.class.getDeclaredField("plugins");
                        pluginsField.setAccessible(true);

                        @SuppressWarnings("unchecked")
                        Map<String, Plugin> plugins = (Map<String, Plugin>) pluginsField.get(manager);
                        plugin.onDisable();

                        Arrays.stream(plugin.getLogger().getHandlers()).forEach(Handler::close);
                        manager.unregisterCommands(plugin);
                        manager.unregisterListeners(plugin);
                        ProxyServer.getInstance().getScheduler().cancel(plugin);

                        plugin.getExecutorService().shutdownNow();
                        plugins.remove(plugin.getDescription().getName());

                        ClassLoader cl = plugin.getClass().getClassLoader();

                        if (cl instanceof URLClassLoader) {
                            Field pluginField = cl.getClass().getDeclaredField("plugin");
                            Field pluginInitField = cl.getClass().getDeclaredField("desc");

                            pluginField.setAccessible(true);
                            pluginInitField.setAccessible(true);

                            pluginField.set(cl, null);
                            pluginInitField.set(cl, null);

                            Field allLoadersField = cl.getClass().getDeclaredField("allLoaders");
                            allLoadersField.setAccessible(true);
                            ((Set<?>) allLoadersField.get(cl)).remove(cl);
                        }

                        ((URLClassLoader) cl).close();
                        System.gc();

                        Message.send(sender, String.format("%s &cUnloading %s.",
                                XenonCore.instance.getConfigData().getPrefix(), args[1]), true);
                        break;
                    default:
                        Message.send(sender, String.format("%s &cUnknown option, available: load, unload, blank (to see plugins list)",
                                XenonCore.instance.getConfigData().getPrefix()), false);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message.send(sender, String.format("%s &cAn error happened while trying to process the plugin. Check the console for details.",
                        XenonCore.instance.getConfigData().getPrefix()), true);
            }
        });
    }
}
