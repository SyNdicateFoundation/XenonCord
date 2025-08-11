package net.md_5.bungee.util;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Longs;
import net.md_5.bungee.jni.NativeCode;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.jni.cipher.JavaCipher;
import net.md_5.bungee.jni.cipher.NativeCipher;
import net.md_5.bungee.protocol.data.PlayerPublicKey;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class EncryptionUtil {

    public static final KeyPair keys;
    public static final NativeCode<BungeeCipher> nativeFactory = new NativeCode<>("native-cipher", JavaCipher::new, NativeCipher::new);
    private static final Random random = new Random();
    private static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));
    private static final PublicKey MOJANG_KEY;

    static {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keys = generator.generateKeyPair();
            MOJANG_KEY = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(ByteStreams.toByteArray(EncryptionUtil.class.getResourceAsStream("/yggdrasil_session_pubkey.der"))));
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static EncryptionRequest encryptRequest() {
        final byte[] verify = new byte[4];
        random.nextBytes(verify);
        return new EncryptionRequest(Long.toString(random.nextLong(), 16),  keys.getPublic().getEncoded(), verify, true);
    }

    public static boolean check(PlayerPublicKey publicKey, UUID uuid) throws GeneralSecurityException {
        final Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(MOJANG_KEY);
        signature.update((uuid != null) ? createCheckWithUUID(publicKey, uuid) : createCheckWithoutUUID(publicKey));
        return signature.verify(publicKey.getSignature());
    }

    private static byte[] createCheckWithUUID(PlayerPublicKey publicKey, UUID uuid) throws GeneralSecurityException {
        final byte[] encoded = getPubkey(publicKey.getKey()).getEncoded();
        final ByteBuffer buffer = ByteBuffer.allocate(24 + encoded.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).putLong(publicKey.getExpiry()).put(encoded);
        return buffer.array();
    }

    private static byte[] createCheckWithoutUUID(PlayerPublicKey publicKey) throws GeneralSecurityException {
        return (publicKey.getExpiry() + "-----BEGIN RSA PUBLIC KEY-----\n" + MIME_ENCODER.encodeToString(getPubkey(publicKey.getKey()).getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n").getBytes(StandardCharsets.US_ASCII);
    }

    public static boolean check(PlayerPublicKey publicKey, EncryptionResponse resp, EncryptionRequest request) throws GeneralSecurityException {
        return publicKey != null ? verifySignature(publicKey, resp, request) : verifyDecryption(resp, request);
    }

    private static boolean verifySignature(PlayerPublicKey publicKey, EncryptionResponse resp, EncryptionRequest request) throws GeneralSecurityException {
        final Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(getPubkey(publicKey.getKey()));
        signature.update(request.getVerifyToken());
        signature.update(Longs.toByteArray(resp.getEncryptionData().getSalt()));
        return signature.verify(resp.getEncryptionData().getSignature());
    }

    private static boolean verifyDecryption(EncryptionResponse resp, EncryptionRequest request) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
        return MessageDigest.isEqual(request.getVerifyToken(), cipher.doFinal(resp.getVerifyToken()));
    }

    public static SecretKey getSecret(EncryptionResponse resp, EncryptionRequest request) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
        return new SecretKeySpec(cipher.doFinal(resp.getSharedSecret()), "AES");
    }

    public static BungeeCipher getCipher(boolean forEncryption, SecretKey shared) throws GeneralSecurityException {
        final BungeeCipher cipher = nativeFactory.newInstance();
        cipher.init(forEncryption, shared);
        return cipher;
    }

    public static PublicKey getPubkey(EncryptionRequest request) throws GeneralSecurityException {
        return getPubkey(request.getPublicKey());
    }

    private static PublicKey getPubkey(byte[] b) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(b));
    }

    public static byte[] encrypt(Key key, byte[] b) throws GeneralSecurityException {
        final Cipher hasher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        hasher.init(Cipher.ENCRYPT_MODE, key);
        return hasher.doFinal(b);
    }
}
