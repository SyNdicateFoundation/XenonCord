package ir.xenoncommunity.utils;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Colorize {
    private static final String LIGHT = "\u001b[0;%d;1m";
    private static final String DARK = "\u001b[0;%d;22m";
    private static final String ANSI_256_COLOR = "\u001b[38;5;%dm";
    private static final char COLOR_CHAR = '§';
    private static final String BUKKIT = COLOR_CHAR + "%s";
    private static final Pattern COLOR_PATTERN = Pattern.compile("(&([0-9a-fk-or]))(?!.*\\1)");
    private static final Pattern COLORIZED_PATTERN = Pattern.compile(COLOR_CHAR + "([0-9a-fk-or])(?!.*\\1)");

    private static final int[] ANSI256 = {0, 95, 135, 175, 215, 255};
    private static final int[] PRECOMPUTED_RED = new int[256];
    private static final int[] PRECOMPUTED_GREEN = new int[256];
    private static final int[] PRECOMPUTED_BLUE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            PRECOMPUTED_RED[i] = findNearestColor(i, ANSI256);
            PRECOMPUTED_GREEN[i] = findNearestColor(i, ANSI256);
            PRECOMPUTED_BLUE[i] = findNearestColor(i, ANSI256);
        }
    }


    public String clear(String text) {
        return text
                .replaceAll(COLORIZED_PATTERN.pattern(), "")
                .replaceAll(COLOR_PATTERN.pattern(), "");
    }

    public String clean(String text) {
        return text.replaceAll(COLORIZED_PATTERN.pattern(), "&$1");
    }

    public String console(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = COLOR_PATTERN.matcher(message);

        while (matcher.find()) {
            String result = matcher.group(1);
            Colors color = Colors.get(result.charAt(1));
            message = message.replace(result, color.toConsole());
        }

        return message + Colors.RESET.toConsole();
    }

    public Colors thermometer(int current, int low, int medium, int high, int veryHigh) {
        if (current <= low) {
            return Colors.GREEN;
        } else if (current <= medium) {
            return Colors.YELLOW;
        } else if (current <= high) {
            return Colors.GOLD;
        } else if (current <= veryHigh) {
            return Colors.RED;
        } else {
            return Colors.DARK_RED;
        }
    }

    public String color256(Color color, String message) {
        int colorCode = rgbToAnsi256(color);
        return String.format(ANSI_256_COLOR, colorCode) + message + Colors.RESET.toConsole();
    }

    private int rgbToAnsi256(Color color) {
        int red = PRECOMPUTED_RED[color.getRed()];
        int green = PRECOMPUTED_GREEN[color.getGreen()];
        int blue = PRECOMPUTED_BLUE[color.getBlue()];

        return 16 + (36 * red) + (6 * green) + blue;
    }

    private static int findNearestColor(int value, int[] ansi256) {
        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < ansi256.length; i++) {
            int distance = Math.abs(value - ansi256[i]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    @RequiredArgsConstructor
    public enum Colors {
        AQUA('b', 36, LIGHT),
        BLACK('0', 30, DARK),
        BLUE('9', 34, LIGHT),
        BOLD('l', 1, DARK),
        DARK_AQUA('3', 36, DARK),
        DARK_BLUE('1', 34, DARK),
        DARK_GREEN('2', 32, DARK),
        DARK_GRAY('8', 30, LIGHT),
        DARK_PURPLE('5', 35, DARK),
        DARK_RED('4', 31, DARK),
        GOLD('6', 33, DARK),
        GRAY('7', 37, DARK),
        GREEN('a', 32, LIGHT),
        ITALIC('o', 3, DARK),
        PURPLE('d', 35, LIGHT),
        RED('c', 31, LIGHT),
        RESET('r', 0, DARK),
        STRIKETHROUGH('m', 9, DARK),
        UNDERLINE('n', 4, DARK),
        WHITE('f', 37, LIGHT),
        YELLOW('e', 33, LIGHT);

        private final char bukkit;
        private final int ansi;
        private final String pattern;

        public static Colors get(char code) {
            for (Colors color : values()) {
                if (color.bukkit == code) {
                    return color;
                }
            }
            throw new IllegalArgumentException("Color with code " + code + " does not exist");
        }

        public String toBukkit() {
            return String.format(BUKKIT, this.ansi);
        }

        public String toConsole() {
            return String.format(this.pattern, this.ansi);
        }

        public @NotNull String colorizeConsole(String... text) {
            Check.checkNull((Object[]) text);
            return this.toConsole() + String.join(" ", text);
        }

        public @NotNull String colorize(String... text) {
            Check.checkNull((Object[]) text);
            return this.toBukkit() + String.join(" ", text);
        }

        @Override
        public String toString() {
            return this.toBukkit();
        }
    }
}
