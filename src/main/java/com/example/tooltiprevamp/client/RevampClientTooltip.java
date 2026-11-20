package com.example.tooltiprevamp.client;

import com.example.tooltiprevamp.data.ClientDiscoveries;
import com.example.tooltiprevamp.data.ItemAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Рисует весь тултип предмета:
 *   [ рамка с названием, чуть выступающая над основной рамкой ]
 *   описание
 *   ---------------
 *   страница 1: оригинальный тултип   |   страница 2 (Ctrl): список действий
 *   ---------------
 *   индикатор страниц
 */
public class RevampClientTooltip implements ClientTooltipComponent {
    public static final int COLOR_BLACK = 0xFF000000;
    public static final int COLOR_BORDER = 0xFF3F3F3F;

    private static final int COLOR_TEXT = 0xFFAAAAAA;
    private static final int COLOR_HIGHLIGHT = 0xFF5555FF;
    private static final int COLOR_BAR = 0xFF555555;
    private static final int COLOR_BULLET = 0xFF777777;
    private static final int COLOR_DASH_ON = 0xFFFFFFFF;
    private static final int COLOR_DASH_OFF = 0xFF666666;

    /** Доп. отступ, чтобы расстояние от текста до рамки было 4 пикселя (у ваниллы 3). */
    private static final int PAD = 1;
    private static final int TITLE_PAD_X = 5;
    private static final int TITLE_BOX_H = 13;
    private static final int TITLE_BOX_TOP = -9;
    private static final int BODY_TOP = 7;
    private static final int LINE_H = 10;
    private static final int DIVIDER_H = 5;
    private static final int FOOTER_H = 5;
    private static final int BULLET_INDENT = 6;
    private static final int ACTION_GAP = 2;
    private static final int MIN_ACTION_W = 110;
    private static final int MAX_ACTION_W = 230;
    private static final int DASH_W = 5;
    private static final int DASH_GAP = 3;

    private static final Pattern TOKEN = Pattern.compile("\\S+\\s*|\\s+");

    @FunctionalInterface
    private interface RowRenderer {
        void render(GuiGraphics g, Font font, int x, int y, int w);
    }

    private record Row(int height, RowRenderer renderer) {}

    private record Layout(int width, int height, int contentWidth, int titleWidth, boolean hasBody, List<Row> rows) {}

    private static final class Tok {
        final String word;
        final boolean highlight;
        final boolean hidden;
        final int wordW;
        int fullW;

        Tok(String word, boolean highlight, boolean hidden, int wordW, int fullW) {
            this.word = word;
            this.highlight = highlight;
            this.hidden = hidden;
            this.wordW = wordW;
            this.fullW = fullW;
        }
    }

    private final RevampTooltipData data;
    private Layout layout;

    public RevampClientTooltip(RevampTooltipData data) {
        this.data = data;
    }

    @Override
    public int getWidth(Font font) {
        return layout(font).width();
    }

    @Override
    public int getHeight() {
        // Ванилла вычитает 2 из высоты, если в тултипе один компонент.
        return layout(Minecraft.getInstance().font).height() + (data.alone() ? 2 : 0);
    }

    // ------------------------------------------------------------------ layout

    private Layout layout(Font font) {
        if (layout != null) return layout;

        boolean hasActions = !data.actions().isEmpty();
        boolean showActions = hasActions && Screen.hasControlDown();

        List<FormattedCharSequence> desc = new ArrayList<>();
        if (data.description() != null) {
            desc.addAll(font.split(data.description(), data.maxLineWidth()));
        }
        List<FormattedCharSequence> body = new ArrayList<>();
        for (Component line : data.bodyLines()) {
            if (line.getString().isEmpty()) {
                body.add(FormattedCharSequence.EMPTY);
            } else {
                body.addAll(font.split(line, data.maxLineWidth()));
            }
        }

        boolean hasBody = !desc.isEmpty() || !body.isEmpty() || hasActions;
        int titleW = font.width(data.title());

        if (!hasBody) {
            layout = new Layout(titleW + PAD * 2, 8 + PAD * 2, titleW, titleW, false, List.of());
            return layout;
        }

        int titleBoxW = titleW + TITLE_PAD_X * 2 + 2;
        int baseW = Math.max(maxWidth(font, desc), maxWidth(font, body));
        int actionW = hasActions
                ? Math.min(Math.max(Math.max(baseW, titleBoxW), MIN_ACTION_W), MAX_ACTION_W)
                : 0;
        int contentW = Math.max(Math.max(baseW, titleBoxW), actionW);

        List<List<Row>> sections = new ArrayList<>();
        if (!desc.isEmpty()) sections.add(List.of(textRow(desc)));
        if (showActions) {
            sections.add(actionRows(font, actionW));
        } else if (!body.isEmpty()) {
            sections.add(List.of(textRow(body)));
        }
        if (hasActions) {
            sections.add(List.of(footerRow(!body.isEmpty(), showActions)));
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) rows.add(dividerRow());
            rows.addAll(sections.get(i));
        }

        int bodyH = 0;
        for (Row r : rows) bodyH += r.height();

        int height = PAD * 2 + BODY_TOP + bodyH - 1;
        layout = new Layout(contentW + PAD * 2, height, contentW, titleW, true, rows);
        return layout;
    }

    private static int maxWidth(Font font, List<FormattedCharSequence> lines) {
        int w = 0;
        for (FormattedCharSequence l : lines) w = Math.max(w, font.width(l));
        return w;
    }

    // ------------------------------------------------------------------ rows

    private static Row textRow(List<FormattedCharSequence> lines) {
        return new Row(lines.size() * LINE_H, (g, f, x, y, w) -> {
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                g.drawString(f, line, x + (w - f.width(line)) / 2, y + i * LINE_H, 0xFFFFFFFF, true);
            }
        });
    }

    private static Row dividerRow() {
        return new Row(DIVIDER_H, (g, f, x, y, w) ->
                g.fill(x - PAD - 3, y + 2, x + w + PAD + 3, y + 3, COLOR_BORDER));
    }

    /** Маленькие чёрточки: одна - есть действия; две - две страницы, белая показывает открытую. */
    private static Row footerRow(boolean twoPages, boolean actionsOpen) {
        return new Row(FOOTER_H, (g, f, x, y, w) -> {
            if (twoPages) {
                int total = DASH_W * 2 + DASH_GAP;
                int sx = x + (w - total) / 2;
                g.fill(sx, y + 2, sx + DASH_W, y + 3, actionsOpen ? COLOR_DASH_OFF : COLOR_DASH_ON);
                int sx2 = sx + DASH_W + DASH_GAP;
                g.fill(sx2, y + 2, sx2 + DASH_W, y + 3, actionsOpen ? COLOR_DASH_ON : COLOR_DASH_OFF);
            } else {
                int sx = x + (w - DASH_W) / 2;
                g.fill(sx, y + 2, sx + DASH_W, y + 3, actionsOpen ? COLOR_DASH_ON : COLOR_DASH_OFF);
            }
        });
    }

    private List<Row> actionRows(Font font, int wrapW) {
        List<Row> rows = new ArrayList<>();
        List<ItemAction> actions = data.actions();
        for (int i = 0; i < actions.size(); i++) {
            ItemAction action = actions.get(i);
            boolean known = ClientDiscoveries.has(action.id());
            List<List<Tok>> lines = wrap(font, ActionText.parse(ActionText.resolve(action.text())),
                    Math.max(20, wrapW - BULLET_INDENT));
            int gap = i < actions.size() - 1 ? ACTION_GAP : 0;
            rows.add(new Row(lines.size() * LINE_H + gap, (g, f, x, y, w) -> drawAction(g, f, x, y, lines, known)));
        }
        return rows;
    }

    private static List<List<Tok>> wrap(Font font, List<ActionText.Segment> segments, int maxW) {
        List<Tok> toks = new ArrayList<>();
        for (ActionText.Segment s : segments) {
            Matcher m = TOKEN.matcher(s.text());
            while (m.find()) {
                String raw = m.group();
                int k = 0;
                while (k < raw.length() && !Character.isWhitespace(raw.charAt(k))) k++;
                String word = raw.substring(0, k);
                String spaces = raw.substring(k);
                if (word.isEmpty()) {
                    if (!toks.isEmpty()) toks.get(toks.size() - 1).fullW += font.width(spaces);
                    continue;
                }
                int ww = font.width(word);
                toks.add(new Tok(word, s.highlight(), s.hidden(), ww, ww + font.width(spaces)));
            }
        }

        List<List<Tok>> lines = new ArrayList<>();
        List<Tok> cur = new ArrayList<>();
        int curW = 0;
        for (Tok t : toks) {
            if (!cur.isEmpty() && curW + t.wordW > maxW) {
                lines.add(cur);
                cur = new ArrayList<>();
                curW = 0;
            }
            cur.add(t);
            curW += t.fullW;
        }
        if (!cur.isEmpty()) lines.add(cur);
        if (lines.isEmpty()) lines.add(new ArrayList<>());
        return lines;
    }

    private static void drawAction(GuiGraphics g, Font f, int x, int y, List<List<Tok>> lines, boolean known) {
        for (int li = 0; li < lines.size(); li++) {
            int ty = y + li * LINE_H;
            if (li == 0) g.drawString(f, "\u2022", x, ty, COLOR_BULLET, false);

            int tx = x + BULLET_INDENT;
            List<Tok> line = lines.get(li);
            for (int ti = 0; ti < line.size(); ti++) {
                Tok t = line.get(ti);
                if (t.hidden && !known) {
                    // слово ещё не открыто - рисуем закрашенный блок
                    boolean merge = ti + 1 < line.size()
                            && line.get(ti + 1).hidden
                            && line.get(ti + 1).highlight == t.highlight;
                    int bw = merge ? t.fullW : t.wordW;
                    g.fill(tx, ty, tx + bw, ty + 8, t.highlight ? COLOR_HIGHLIGHT : COLOR_BAR);
                } else {
                    g.drawString(f, t.word, tx, ty, t.highlight ? COLOR_HIGHLIGHT : COLOR_TEXT, true);
                }
                tx += t.fullW;
            }
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        Layout l = layout(font);
        int cx = x + PAD;

        if (!l.hasBody()) {
            g.drawString(font, data.title(), cx + (l.contentWidth() - l.titleWidth()) / 2, y + PAD, 0xFFFFFFFF, true);
            return;
        }

        // Заголовок в своей рамке, налезающей на верхнюю границу тултипа
        int boxW = l.titleWidth() + TITLE_PAD_X * 2 + 2;
        int bx = cx + (l.contentWidth() - boxW) / 2;
        int by = y + TITLE_BOX_TOP;
        g.fill(bx, by, bx + boxW, by + TITLE_BOX_H, COLOR_BORDER);
        g.fill(bx + 1, by + 1, bx + boxW - 1, by + TITLE_BOX_H - 1, COLOR_BLACK);
        g.drawString(font, data.title(), bx + 1 + TITLE_PAD_X, by + 3, 0xFFFFFFFF, true);

        int ry = y + PAD + BODY_TOP;
        for (Row row : l.rows()) {
            row.renderer().render(g, font, cx, ry, l.contentWidth());
            ry += row.height();
        }
    }
}
