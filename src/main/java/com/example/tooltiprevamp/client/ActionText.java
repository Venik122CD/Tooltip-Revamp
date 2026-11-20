package com.example.tooltiprevamp.client;

import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;

public final class ActionText {
    private ActionText() {}

    public record Segment(String text, boolean highlight, boolean hidden) {}

    public static String resolve(String keyOrMarkup) {
        Language lang = Language.getInstance();
        return lang.has(keyOrMarkup) ? lang.getOrDefault(keyOrMarkup) : keyOrMarkup;
    }

    public static List<Segment> parse(String s) {
        List<Segment> out = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            char close = switch (c) {
                case '[' -> ']';
                case '{' -> '}';
                case '<' -> '>';
                default -> '\0';
            };
            int end = close != '\0' ? s.indexOf(close, i + 1) : -1;
            if (end > 0) {
                if (plain.length() > 0) {
                    out.add(new Segment(plain.toString(), false, true));
                    plain.setLength(0);
                }
                String inner = s.substring(i + 1, end);
                switch (c) {
                    case '[' -> out.add(new Segment(inner, true, false));
                    case '{' -> out.add(new Segment(inner, true, true));
                    default -> out.add(new Segment(inner, false, false));
                }
                i = end + 1;
            } else {
                plain.append(c);
                i++;
            }
        }
        if (plain.length() > 0) {
            out.add(new Segment(plain.toString(), false, true));
        }
        return out;
    }
}
