package net.md_5.bungee.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;

@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class PacketReceiveEvent extends Event implements Cancellable {
    private PacketWrapper packet;
    private ProxiedPlayer player;
    private boolean cancelled;
    public PacketReceiveEvent(final PacketWrapper packetIn, final ProxiedPlayer playerIn){
        this.packet = packetIn;
        this.player = playerIn;
    }
    public PacketReceiveEvent(final PacketWrapper packetIn){
        this.packet = packetIn;
        this.player = null;
    }
}
