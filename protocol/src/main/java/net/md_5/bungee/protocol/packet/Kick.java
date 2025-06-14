package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.protocol.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Kick extends DefinedPacket {

    private BaseComponent message;

    @Override
    public void read(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocol == Protocol.LOGIN) {
            message = ChatSerializer.forVersion( protocolVersion ).deserialize(readString(buf));
        } else {
            message = readBaseComponent(buf, protocolVersion);
        }
    }

    @Override
    public void write(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocol == Protocol.LOGIN) {
            writeString(ChatSerializer.forVersion( protocolVersion ).toString(message), buf);
        } else {
            writeBaseComponent(message, buf, protocolVersion);
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        try {
            handler.handle(this);
        } catch (OutOfMemoryError e) {
            System.gc();
        }
    }
}
