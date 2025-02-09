package net.md_5.bungee.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import se.llbit.nbt.SpecificTag;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagUtilTest {

    private static final Gson GSON = new Gson();

    private static void testDissembleReassemble(String json) {
        JsonElement parsedJson = GSON.fromJson(json, JsonElement.class);
        SpecificTag nbt = TagUtil.fromJson(parsedJson);
        JsonElement convertedElement = TagUtil.toJson(nbt);

        String convertedJson = GSON.toJson(convertedElement);
        assertEquals(json, convertedJson);
    }

    @Test
    public void testStringLiteral() {
        testDissembleReassemble("{\"text\":\"\",\"extra\":[\"hello\",{\"text\":\"there\",\"color\":\"#ff0000\"},{\"text\":\"friend\",\"font\":\"minecraft:default\"}]}");
    }
}
