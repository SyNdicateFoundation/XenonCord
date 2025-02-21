package io.github.waterfallmc.waterfall.forwarding;

import io.github.waterfallmc.waterfall.conf.WaterfallConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Property;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public enum VelocityForwardingUtil {
    ;

    public static final String VELOCITY_IP_FORWARDING_CHANNEL = "velocity:player_info";
    public static final int FORWARDING_VERSION = 1;
    public static final String MODERN_IP_FORWARDING_FAILURE =
            "Your server did not send a forwarding request to the proxy. Is it set up correctly?";

    public static byte[] writeForwardingData(String address, String name, UUID playerUUID, Property[] properties) {
        ByteBuf buf = Unpooled.buffer(2048);
        try {
            DefinedPacket.writeVarInt(FORWARDING_VERSION, buf);
            DefinedPacket.writeString(address, buf);
            DefinedPacket.writeUUID(playerUUID, buf);
            DefinedPacket.writeString(name, buf);
            DefinedPacket.writeVarInt(properties.length, buf);

            for (Property property : properties) {
                DefinedPacket.writeString(property.getName(), buf);
                DefinedPacket.writeString(property.getValue(), buf);
                String signature = property.getSignature();
                if (signature != null && !signature.isEmpty()) {
                    buf.writeBoolean(true);
                    DefinedPacket.writeString(signature, buf);
                } else {
                    buf.writeBoolean(false);
                }
            }

            byte[] forwardingSecret = ((WaterfallConfiguration) BungeeCord.getInstance().config).getForwardingSecret();
            SecretKey key = new SecretKeySpec(forwardingSecret, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(buf.array(), buf.arrayOffset(), buf.readableBytes());
            byte[] sig = mac.doFinal();

            ByteBuf finished = Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), buf);
            byte[] encoded = ByteBufUtil.getBytes(finished);
            finished.release();
            return encoded;
        } catch (InvalidKeyException e) {
            buf.release();
            throw new RuntimeException("Unable to authenticate data", e);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            buf.release();
            throw new AssertionError(e);
        }
    }
}
