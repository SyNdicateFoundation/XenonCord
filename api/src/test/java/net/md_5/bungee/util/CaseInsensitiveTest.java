package net.md_5.bungee.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CaseInsensitiveTest {

    @Test
    public void testMaps() {
        Object obj = new Object();
        CaseInsensitiveMap<Object> map = new CaseInsensitiveMap<>();

        map.put("FOO", obj);
        assertTrue(map.contains("foo")); // Assert that contains is case insensitive
        assertEquals("FOO", map.entrySet().iterator().next().getKey()); // Assert that case is preserved

        // Assert that remove is case insensitive
        map.remove("FoO");
        assertFalse(map.contains("foo"));
    }

    @Test
    public void testSets() {
        CaseInsensitiveSet set = new CaseInsensitiveSet();

        set.add("FOO");
        assertTrue(set.contains("foo")); // Assert that contains is case insensitive
        set.remove("FoO");
        assertFalse(set.contains("foo")); // Assert that remove is case insensitive
    }
}
