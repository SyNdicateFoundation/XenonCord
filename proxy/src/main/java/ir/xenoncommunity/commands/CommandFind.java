package ir.xenoncommunity.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.command.PlayerCommand;

public class CommandFind extends Command implements TabExecutor
{

    public CommandFind()
    {
        super( "find", "bungeecord.command.find" );
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if ( args.length != 1 )
        {
            sender.sendMessage( ProxyServer.getInstance().getTranslation( "username_needed" ) );
        } else
        {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer( args[0] );
            if ( player == null || player.getServer() == null )
            {
                sender.sendMessage( ProxyServer.getInstance().getTranslation( "user_not_online" ) );
            } else
            {
                boolean moduleLoaded = ProxyServer.getInstance().getPluginManager().getPlugin( "cmd_server" ) != null;
                ServerInfo server = player.getServer().getInfo();
                ComponentBuilder componentBuilder = new ComponentBuilder().appendLegacy( ProxyServer.getInstance().getTranslation( "user_online_at", player.getName(), server.getName() ) );

                if ( moduleLoaded && server.canAccess( sender ) )
                {
                    componentBuilder.event( new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder().appendLegacy( ProxyServer.getInstance().getTranslation( "click_to_connect" ) ).create() )
                    );
                    componentBuilder.event( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/server " + server.getName() ) );
                }

                sender.sendMessage( componentBuilder.create() );
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? Lists.newArrayList(args) : Collections.emptyList();
    }
}
