package net.md_5.bungee.api.xenoncord;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.protocol.packet.Handshake;

/**
 * Event called to represent a player first making their presence and username
 * known.
 */
@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class PostPlayerHandshakeEvent extends Event implements Cancellable {

    /**
     * Connection attempting to login.
     */
    private final PendingConnection connection;
    /**
     * The handshake.
     */
    private final Handshake handshake;
    private boolean cancelled;
    private String reason;
    public PostPlayerHandshakeEvent(PendingConnection connection, Handshake handshake) {
        this.connection = connection;
        this.handshake = handshake;
    }
}
