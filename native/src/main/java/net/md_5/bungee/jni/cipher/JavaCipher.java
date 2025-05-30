package net.md_5.bungee.jni.cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.FastThreadLocal;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;

public class JavaCipher implements BungeeCipher {

    private static final FastThreadLocal<byte[]> heapInLocal = new EmptyByteThreadLocal();
    private static final FastThreadLocal<byte[]> heapOutLocal = new EmptyByteThreadLocal();
    private final Cipher cipher;

    public JavaCipher() {
        try {
            this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void init(boolean forEncryption, SecretKey key) throws GeneralSecurityException {
        int mode = forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        cipher.init(mode, key, new IvParameterSpec(key.getEncoded()));
    }

    @Override
    public void cipher(ByteBuf in, ByteBuf out) throws ShortBufferException {
        int readableBytes = in.readableBytes();
        byte[] heapIn = bufToByte(in);

        byte[] heapOut = heapOutLocal.get();
        int outputSize = cipher.getOutputSize(readableBytes);
        if (heapOut.length < outputSize) {
            heapOut = new byte[outputSize];
            heapOutLocal.set(heapOut);
        }
        out.writeBytes(heapOut, 0, cipher.update(heapIn, 0, readableBytes, heapOut));
    }

    @Override
    public ByteBuf cipher(ChannelHandlerContext ctx, ByteBuf in) throws ShortBufferException {
        int readableBytes = in.readableBytes();
        byte[] heapIn = bufToByte(in);

        ByteBuf heapOut = ctx.alloc().heapBuffer(cipher.getOutputSize(readableBytes));
        heapOut.writerIndex(cipher.update(heapIn, 0, readableBytes, heapOut.array(), heapOut.arrayOffset()));

        return heapOut;
    }

    @Override
    public void free() {
    }

    private byte[] bufToByte(ByteBuf in) {
        byte[] heapIn = heapInLocal.get();
        int readableBytes = in.readableBytes();
        if (heapIn.length < readableBytes) {
            heapIn = new byte[readableBytes];
            heapInLocal.set(heapIn);
        }
        in.readBytes(heapIn, 0, readableBytes);
        return heapIn;
    }

    @Override
    public boolean allowComposite() {
        return true;
    }

    private static class EmptyByteThreadLocal extends FastThreadLocal<byte[]> {

        @Override
        protected byte[] initialValue() {
            return new byte[0];
        }
    }
}
