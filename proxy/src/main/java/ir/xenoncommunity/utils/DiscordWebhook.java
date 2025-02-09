package ir.xenoncommunity.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("unused") public class DiscordWebhook {
    private final String url;
    private String content;
    private String username;
    private String avatarUrl;
    public final List<EmbedObject> embeds = new ArrayList<>();

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public DiscordWebhook setContent(String content) {
        this.content = content;
        return this;
    }

    public DiscordWebhook setUsername(String username) {
        this.username = username;
        return this;
    }

    public DiscordWebhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public DiscordWebhook addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
        return this;
    }

    public void execute() throws IOException {
        if (this.content == null && this.embeds.isEmpty())
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");

        final JSONObject json = new JSONObject();
        json.put("content", this.content);
        json.put("username", this.username);
        json.put("avatar_url", this.avatarUrl);
        if (!this.embeds.isEmpty()) {
            final ArrayList<JSONObject> embedObjects = new ArrayList<>();
            for (EmbedObject embed : this.embeds) {
                final JSONObject jsonEmbed = new JSONObject();
                jsonEmbed.put("title", embed.getTitle());
                jsonEmbed.put("description", embed.getDescription());
                jsonEmbed.put("url", embed.getUrl());
                if (embed.getColor() != null) {
                    jsonEmbed.put("color", (embed.getColor().getRed() << 16) + (embed.getColor().getGreen() << 8) + embed.getColor().getBlue());
                }
                addOptionalFields(jsonEmbed, embed);
                embedObjects.add(jsonEmbed);
            }
            json.put("embeds", embedObjects.toArray());
        }
        json.put("attachments", new String[0]);
        sendRequest(json);
    }

    private void addOptionalFields(JSONObject jsonEmbed, EmbedObject embed) {
        if (embed.getFooter() != null) {
            final JSONObject jsonFooter = new JSONObject();
            jsonFooter.put("text", embed.getFooter().getText());
            jsonFooter.put("icon_url", embed.getFooter().getIconUrl());
            jsonEmbed.put("footer", jsonFooter);
        }
        if (embed.getImage() != null) {
            final JSONObject jsonImage = new JSONObject();
            jsonImage.put("url", embed.getImage().getUrl());
            jsonEmbed.put("image", jsonImage);
        }
        if (embed.getThumbnail() != null) {
            final JSONObject jsonThumbnail = new JSONObject();
            jsonThumbnail.put("url", embed.getThumbnail().getUrl());
            jsonEmbed.put("thumbnail", jsonThumbnail);
        }
        if (embed.getAuthor() != null) {
            final JSONObject jsonAuthor = new JSONObject();
            jsonAuthor.put("name", embed.getAuthor().getName());
            jsonAuthor.put("url", embed.getAuthor().getUrl());
            jsonAuthor.put("icon_url", embed.getAuthor().getIconUrl());
            jsonEmbed.put("author", jsonAuthor);
        }
        final ArrayList<JSONObject> jsonFields = new ArrayList<>();
        for (EmbedObject.Field field : embed.getFields()) {
            final JSONObject jsonField = new JSONObject();
            jsonField.put("name", field.getName());
            jsonField.put("value", field.getValue());
            jsonField.put("inline", field.isInline());
            jsonFields.add(jsonField);
        }
        if (!jsonFields.isEmpty()) jsonEmbed.put("fields", jsonFields.toArray());
    }

    private void sendRequest(JSONObject json) throws IOException {
        final HttpsURLConnection connection = (HttpsURLConnection) new URL(this.url).openConnection();
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("User-Agent", "Java-DiscordWebhook");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(json.toString().getBytes());
            stream.flush();
        }
        connection.getInputStream().close();
        connection.disconnect();
    }

    private static class JSONObject {
        private final HashMap<String, Object> map = new HashMap<>();

        void put(String key, Object value) {
            if (value != null) {
                this.map.put(key, value);
            }
        }

        public String toString() {
            final StringBuilder builder = new StringBuilder();
            final Set<Map.Entry<String, Object>> entrySet = this.map.entrySet();
            builder.append("{");

            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                Object val = entry.getValue();
                builder.append(quote(entry.getKey())).append(":");
                if (val instanceof String) {
                    builder.append(quote(String.valueOf(val)));
                } else if (val instanceof Integer || val instanceof Boolean) {
                    builder.append(val);
                } else if (val instanceof JSONObject) {
                    builder.append(val);
                } else if (val.getClass().isArray()) {
                    builder.append("[");
                    int len = Array.getLength(val);
                    for (int j = 0; j < len; j++) {
                        builder.append(Array.get(val, j).toString()).append((j != len - 1) ? "," : "");
                    }
                    builder.append("]");
                }
                builder.append((++i == entrySet.size()) ? "}" : ",");
            }
            return builder.toString();
        }

        private String quote(String string) {
            return "\"" + string + "\"";
        }

        private JSONObject() {}
    }

    @Getter
    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private Color color;
        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private final List<Field> fields = new ArrayList<>();

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(Color color) {
            this.color = color;
            return this;
        }

        public EmbedObject setFooter(String text, String icon) {
            this.footer = new Footer(text, icon);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String icon) {
            this.author = new Author(name, url, icon);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        @AllArgsConstructor
        @Getter
        private static class Field {
            private final String name;
            private final String value;
            private final boolean inline;
        }

        @AllArgsConstructor
        @Getter
        private static class Author {
            private final String name;
            private final String url;
            private final String iconUrl;
        }

        @AllArgsConstructor
        @Getter
        private static class Image {
            private final String url;
        }

        @AllArgsConstructor
        @Getter
        private static class Thumbnail {
            private final String url;
        }

        @AllArgsConstructor
        @Getter
        private static class Footer {
            private final String text;
            private final String iconUrl;
        }
    }
}
