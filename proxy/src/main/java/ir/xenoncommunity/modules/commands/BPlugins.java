package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;

public class BPlugins extends Command implements Listener {
    public BPlugins(){
        super("BPlugins", XenonCore.instance.getConfigData().getModules().getPingperm());
    }
    @Override@SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginsperm()))
            return;

        PluginManager manager = XenonCore.instance.getBungeeInstance().getPluginManager();
        if(args.length == 0){
            final StringBuilder sb = new StringBuilder();

            manager.getPlugins().forEach(e -> sb.append(e.getDescription().getName()).append(", "));

            Message.send(sender, String.format("%s &cPLUGINS: %s.",
                    XenonCore.instance.getConfigData().getPrefix(),
                    sb.substring(0, sb.length() - 2)) , false);
            return;
        }

        if(!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginstoggleperm())) return;


        final File plFile = new File(String.format("plugins/" + args[1]));
        if(!plFile.exists())
        {
            Message.send(sender,
                    "PREFIX &cPlugin does not exist in folder."
                            .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix()),
                    false);
            return;
        }
        /***
         * credit to plugmanX creators.
         */
        XenonCore.instance.getTaskManager().add(() -> {
            try {
                switch (args[0]) {
                    case "load":
                        final Field yamlField = PluginManager.class.getDeclaredField("yaml");
                        final Field toLoadField = PluginManager.class.getDeclaredField("toLoad");
                        final JarFile jar = new JarFile(plFile);
                        yamlField.setAccessible(true);
                        toLoadField.setAccessible(true);

                        final HashMap<String, PluginDescription> toLoad  =
                                (toLoadField.get(manager) == null) ?
                                new HashMap<>() :
                                        (HashMap<String, PluginDescription>) toLoadField.get(manager);

                        PluginDescription desc;

                        desc = ((Yaml) yamlField.get(manager)).loadAs(
                                jar.getInputStream(
                                        (jar.getJarEntry("bungee.yml") == null) ?
                                                jar.getJarEntry("plugin.yml") :
                                                jar.getJarEntry("bungee.yml")),
                                PluginDescription.class);

                        desc.setFile(plFile);
                        toLoad.put(desc.getName(), desc);

                        toLoadField.set(manager, toLoad);

                        manager.loadPlugins();

                        manager.getPlugin(desc.getName()).onEnable();

                        Message.send(sender,
                                "PREFIX &cLoading PLUGIN."
                                        .replace("PREFIX", XenonCore.instance.getConfigData().getPrefix())
                                        .replace("PLUGIN", args[1]),
                                true);
                        break;
                    case "unload":
                        final Plugin plugin = XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]);
                        final Field pluginsField = PluginManager.class.getDeclaredField("plugins");
                        pluginsField.setAccessible(true);
                        final Map<String, Plugin> plugins = (Map<String, Plugin>) pluginsField.get(manager);
                        plugin.onDisable();
                        Arrays.stream(plugin.getLogger().getHandlers()).forEach(Handler::close);
                        manager.unregisterCommands(plugin);
                        manager.unregisterListeners(plugin);

                        XenonCore.instance.getBungeeInstance().getScheduler().cancel(plugin);

                        plugin.getExecutorService().shutdownNow();

                        plugins.remove(plugin.getDescription().getName());

                        final ClassLoader cl = plugin.getClass().getClassLoader();

                        if (cl instanceof URLClassLoader) {
                            final Field pluginField = cl.getClass().getDeclaredField("plugin");
                            final Field pluginInitField = cl.getClass().getDeclaredField("desc");

                            pluginField.setAccessible(true);
                            pluginInitField.setAccessible(true);
                            pluginField.set(cl, null);
                            pluginInitField.set(cl, null);

                            final Field allLoadersField = cl.getClass().getDeclaredField("allLoaders");
                            allLoadersField.setAccessible(true);
                            ((Set) allLoadersField.get(cl)).remove(cl);
                        }
                        ((URLClassLoader) cl).close();

                        System.gc();

                        Message.send(sender,
                                "PREFIX &cUnloading PLUGIN."
                                        .replace("PREFIX", XenonCore.instance.getConfigData().getPrefix())
                                        .replace("PLUGIN", args[1]),
                                true);
                        break;
                    default:
                        Message.send(sender,
                                "PREFIX &cUnknown option, available: load, unload, blank (to see plugins list)"
                                        .replace("PREFIX", XenonCore.instance.getConfigData().getPrefix()),
                                false);
                        break;
                }
            }  catch (final Exception e){
                e.printStackTrace();
                Message.send(sender,
                        "PREFIX &cAn error happened while trying to load the plugin. check console for details.."
                                .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix())
                                .replace("PLUGIN", args[1]),
                        true);
            }
        });
    }
}
