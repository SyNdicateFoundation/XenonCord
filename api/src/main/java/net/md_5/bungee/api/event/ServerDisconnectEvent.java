package net.md_5.bungee.api.event;

import lombok.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * Called when the player is disconnected from a server, for example during
 * server switching.
 * <p>
 * If the player is kicked from a server, {@link ServerKickEvent} will be called
 * instead.
 */
@Data
@AllArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class ServerDisconnectEvent extends Event {

    /**
     * Player disconnecting from a server.
     */
    @NonNull
    private final ProxiedPlayer player;
    /**
     * Server the player is disconnecting from.
     */
    @NonNull
    private final ServerInfo target;
}
