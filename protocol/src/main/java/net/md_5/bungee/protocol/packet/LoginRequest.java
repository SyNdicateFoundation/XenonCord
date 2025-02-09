package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PlayerPublicKey;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoginRequest extends DefinedPacket {

    private String data;
    private PlayerPublicKey publicKey;
    private UUID uuid;

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        data = readString(buf, 16);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19 && protocolVersion < ProtocolConstants.MINECRAFT_1_19_3) {
            publicKey = readPublicKey(buf);
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_2 || buf.readBoolean()) {
                uuid = readUUID(buf);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        writeString(data, buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19 && protocolVersion < ProtocolConstants.MINECRAFT_1_19_3) {
            writePublicKey(publicKey, buf);
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_2) {
                writeUUID(uuid, buf);
            } else {
                if (uuid != null) {
                    buf.writeBoolean(true);
                    writeUUID(uuid, buf);
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

    // Waterfall start: Additional DoS mitigations, courtesy of Velocity
    public int expectedMaxLength(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        // Accommodate the rare (but likely malicious) use of UTF-8 usernames, since it is technically
        // legal on the protocol level.
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) return -1;
        return 1 + (16 * 3);
    }
    // Waterfall end
}
