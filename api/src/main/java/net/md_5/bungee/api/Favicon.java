package net.md_5.bungee.api;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Favicon shown in the server list.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Favicon {

    private static final TypeAdapter<Favicon> FAVICON_TYPE_ADAPTER = new TypeAdapter<Favicon>() {
        @Override
        public void write(JsonWriter out, Favicon value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.getEncoded());
            }
        }

        @Override
        public Favicon read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String enc = in.nextString();
            return enc == null ? null : create(enc);
        }
    };
    /**
     * The base64 encoded favicon, including MIME header.
     */
    @NonNull
    @Getter
    private final String encoded;

    public static TypeAdapter<Favicon> getFaviconTypeAdapter() {
        return FAVICON_TYPE_ADAPTER;
    }

    /**
     * Creates a favicon from an image.
     *
     * @param image the image to create on
     * @return the created favicon instance
     * @throws IllegalArgumentException if the favicon is larger than
     *                                  {@link Short#MAX_VALUE} or not of dimensions 64x64 pixels.
     */
    public static Favicon create(BufferedImage image) {
        Preconditions.checkArgument(image != null, "image is null");
        // check size
        if (image.getWidth() != 64 || image.getHeight() != 64) {
            throw new IllegalArgumentException("Server icon must be exactly 64x64 pixels");
        }

        // dump image PNG
        byte[] imageBytes;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", stream);
            imageBytes = stream.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream should never throw this
            throw new AssertionError(e);
        }

        // encode with header
        String encoded = "data:image/png;base64," + BaseEncoding.base64().encode(imageBytes);

        // check encoded image size
        if (encoded.length() > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Favicon file too large for server to process");
        }

        // create
        return new Favicon(encoded);
    }

    /**
     * Creates a Favicon from an encoded PNG.
     *
     * @param encodedString a base64 mime encoded PNG string
     * @return the created favicon
     * @deprecated Use #create(java.awt.image.BufferedImage) instead
     */
    @Deprecated
    public static Favicon create(String encodedString) {
        return new Favicon(encodedString);
    }
}
