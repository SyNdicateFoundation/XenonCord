package net.md_5.bungee.api.plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import lombok.ToString;
import net.md_5.bungee.api.ProxyServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

@ToString(of = "desc")
final class PluginClassloader extends URLClassLoader {

    private static final Set<PluginClassloader> allLoaders = new CopyOnWriteArraySet<>();

    static {
        ClassLoader.registerAsParallelCapable();
    }

    //
    private final ProxyServer proxy;
    private final PluginDescription desc;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;
    private final ClassLoader libraryLoader;
    //
    private Plugin plugin;

    public PluginClassloader(ProxyServer proxy, PluginDescription desc, File file, ClassLoader libraryLoader) throws IOException {
        super(new URL[]
                {
                        file.toURI().toURL()
                });
        this.proxy = proxy;
        this.desc = desc;
        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();
        this.libraryLoader = libraryLoader;

        allLoaders.add(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true, true);
    }

    private Class<?> loadClass0(String name, boolean resolve, boolean checkOther, boolean checkLibraries) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);

            // SPIGOT-6749: Library classes will appear in the above, but we don't want to return them to other plugins
            if (checkOther || result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ex) {
        }

        if (checkLibraries && libraryLoader != null) {
            try {
                return libraryLoader.loadClass(name);
            } catch (ClassNotFoundException ex) {
            }
        }

        if (checkOther) {
            for (PluginClassloader loader : allLoaders) {
                if (loader != this) {
                    try {
                        return loader.loadClass0(name, resolve, false, proxy.getPluginManager().isTransitiveDepend(desc, loader.desc));
                    } catch (ClassNotFoundException ex) {
                    }
                }
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = jar.getJarEntry(path);

        if (entry != null) {
            byte[] classBytes;

            try (InputStream is = jar.getInputStream(entry)) {
                classBytes = ByteStreams.toByteArray(is);
            } catch (IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }

            try
            {
                classBytes = remap( classBytes );
            } catch ( Exception ex )
            {
                Logger logger = ( plugin != null ) ? plugin.getLogger() : proxy.getLogger();
                logger.log( Level.SEVERE, "Error trying to remap class " + path, ex );
            }

            int dot = name.lastIndexOf('.');
            if (dot != -1) {
                String pkgName = name.substring(0, dot);
                if (getPackage(pkgName) == null) {
                    try {
                        if (manifest != null) {
                            definePackage(pkgName, manifest, url);
                        } else {
                            definePackage(pkgName, null, null, null, null, null, null, null);
                        }
                    } catch (IllegalArgumentException ex) {
                        if (getPackage(pkgName) == null) {
                            throw new IllegalStateException("Cannot find package " + pkgName);
                        }
                    }
                }
            }

            CodeSigner[] signers = entry.getCodeSigners();
            CodeSource source = new CodeSource(url, signers);

            return defineClass(name, classBytes, 0, classBytes.length, source);
        }

        return super.findClass(name);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    void init(Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "plugin");
        Preconditions.checkArgument(plugin.getClass().getClassLoader() == this, "Plugin has incorrect ClassLoader");
        if (this.plugin != null) {
            throw new IllegalArgumentException("Plugin already initialized!");
        }

        this.plugin = plugin;
        plugin.init(proxy, desc);
    }

    private static final Map<String, String> MAPPINGS = ImmutableMap.of(
            "net/md_5/bungee/protocol/ChatChain", "net/md_5/bungee/protocol/data/ChatChain",
            "net/md_5/bungee/protocol/Location", "net/md_5/bungee/protocol/data/Location",
            "net/md_5/bungee/protocol/NumberFormat", "net/md_5/bungee/protocol/data/NumberFormat",
            "net/md_5/bungee/protocol/PlayerPublicKey", "net/md_5/bungee/protocol/data/PlayerPublicKey",
            "net/md_5/bungee/protocol/Property", "net/md_5/bungee/protocol/data/Property",
            "net/md_5/bungee/protocol/SeenMessages", "net/md_5/bungee/protocol/data/SeenMessages",
            "net/md_5/bungee/protocol/Either", "net/md_5/bungee/protocol/util/Either",
            "net/md_5/bungee/protocol/TagUtil", "net/md_5/bungee/protocol/util/TagUtil"
    );

    private static byte[] remap(byte[] b)
    {
        final ClassReader cr = new ClassReader( b );
        final ClassWriter cw = new ClassWriter( cr, 0 );
        cr.accept( new ClassRemapper( cw, new SimpleRemapper( MAPPINGS ) ), 0 );
        return cw.toByteArray();
    }
}
