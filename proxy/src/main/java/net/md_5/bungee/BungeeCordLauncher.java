package net.md_5.bungee;

import java.io.FileWriter;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.command.ConsoleCommandSender;

public class BungeeCordLauncher
{

    public static void main(final String[] args) throws Exception
    {
        final OptionParser parser = new OptionParser();
        final OptionSet options = parser.parse( args );
        Security.setProperty( "networkaddress.cache.ttl", "30" );
        Security.setProperty( "networkaddress.cache.negative.ttl", "10" );

        // For JDK9+ we force-enable multi-release jar file support #3087
        System.setProperty("jdk.util.jar.enableMultiRelease",
                System.getProperty( "jdk.util.jar.enableMultiRelease" ) == null  ? "force" : System.getProperty( "jdk.util.jar.enableMultiRelease" ));

        parser.allowsUnrecognizedOptions();
        parser.acceptsAll(Collections.singletonList("help"), "Show the help" );
        parser.acceptsAll( Arrays.asList( "v", "version" ), "Print version and exit" );
        parser.acceptsAll(Collections.singletonList("noconsole"), "Disable console input" );

        if ( options.has( "help" ) || options.has("version"))
        {
            parser.printHelpOn(System.out);
            System.out.println( BungeeCord.class.getPackage().getImplementationVersion() );
            return;
        }
        new BungeeCord().start(System.currentTimeMillis());

        if (options.has( "noconsole")) return;

        new io.github.waterfallmc.waterfall.console.WaterfallConsole().start();
    }
}
