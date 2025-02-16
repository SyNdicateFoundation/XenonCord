package net.md_5.bungee.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class PacketReceiveEvent extends Event implements Cancellable {
    private Object packet;
    private boolean cancelled;

    public PacketReceiveEvent(Object packetIn, ProxiedPlayer playerIn) {
        this.packet = packetIn;
    }

    public PacketReceiveEvent(Object packetIn) {
        this.packet = packetIn;
    }
}
