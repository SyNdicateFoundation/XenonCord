package net.md_5.bungee.protocol.packet;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TabCompleteResponse extends DefinedPacket {

    private int transactionId;
    private Suggestions suggestions;
    //
    private List<String> commands;

    public TabCompleteResponse(int transactionId, Suggestions suggestions) {
        this.transactionId = transactionId;
        this.suggestions = suggestions;
    }

    public TabCompleteResponse(List<String> commands) {
        this.commands = commands;
    }

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            transactionId = readVarInt(buf);
            int start = readVarInt(buf);
            int length = readVarInt(buf);
            StringRange range = StringRange.between(start, start + length);

            int cnt = readVarInt(buf);
            List<Suggestion> matches = new LinkedList<>();
            for (int i = 0; i < cnt; i++) {
                String match = readString(buf);
                BaseComponent tooltip = buf.readBoolean() ? readBaseComponent(buf, protocolVersion) : null;

                matches.add(new Suggestion(range, match, (tooltip != null) ? new ComponentMessage(tooltip) : null));
            }

            suggestions = new Suggestions(range, matches);
        } else {
            commands = readStringArray(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            writeVarInt(transactionId, buf);
            writeVarInt(suggestions.getRange().getStart(), buf);
            writeVarInt(suggestions.getRange().getLength(), buf);

            writeVarInt(suggestions.getList().size(), buf);
            for (Suggestion suggestion : suggestions.getList()) {
                writeString(suggestion.getText(), buf);
                buf.writeBoolean(suggestion.getTooltip() != null);
                if (suggestion.getTooltip() != null) {
                    writeBaseComponent(((ComponentMessage) suggestion.getTooltip()).getComponent(), buf, protocolVersion);
                }
            }
        } else {
            writeStringArray(commands, buf);
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

    @Data
    private static class ComponentMessage implements Message {

        private final BaseComponent component;

        @Override
        public String getString() {
            return component.toPlainText();
        }
    }
}
