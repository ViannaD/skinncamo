package com.skincamo.client.gui;

import com.skincamo.capability.BodyPart;
import com.skincamo.client.Eyedropper;
import com.skincamo.client.SkinCamoTextureManager;
import com.skincamo.client.eyedropper.ColorSampler;
import com.skincamo.client.eyedropper.MimicryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela principal de pintura da skin. Aberta com a tecla P (ou reaberta
 * automaticamente após o uso do conta-gotas no mundo).
 *
 * Decisão de implementação: em vez de criar várias subclasses de
 * AbstractWidget para o quadrado de saturação/valor, a barra de matiz e os
 * "swatches" de histórico/favoritos (o que exigiria acertar a assinatura
 * exata de métodos protegidos que variam entre versões do Forge/Mojang
 * mappings), esses elementos são desenhados e tratados manualmente dentro
 * desta própria Screen (render/mouseClicked/mouseDragged). Isso reduz a
 * superfície de risco de compilação, já que usamos só APIs muito estáveis:
 * GuiGraphics#fill, Button.builder e EditBox. Os únicos widgets "de verdade"
 * (Button/EditBox) são adicionados via addRenderableWidget, como em qualquer
 * tela vanilla.
 */
public class SkinPainterScreen extends Screen {

    // ---- paleta visual (inspirada em Photoshop/Krita/Blender) ----
    private static final int COL_PANEL_BG = 0xE6202225;
    private static final int COL_PANEL_BORDER = 0xFF3F4248;
    private static final int COL_SECTION_BG = 0xCC2B2D31;
    private static final int COL_TEXT = 0xFFE7E9EC;
    private static final int COL_TEXT_MUTED = 0xFFA0A4AA;
    private static final int COL_ACCENT = 0xFF6FC3F0;
    private static final int COL_SELECTED_BORDER = 0xFF6FC3F0;
    private static final int COL_SWATCH_BORDER = 0xFF15161A;

    // ---- layout do painel ----
    private int panelX, panelY;
    private final int panelW = 384;
    private final int panelH = 304;

    // ---- estado de cor atual ----
    private int currentRgb = 0xFFFFFF;
    private float hue = 0f, sat = 0f, val = 1f;
    private boolean suppressHexCallback = false;

    private BodyPart selectedPart = BodyPart.HEAD;

    // ---- regiões interativas calculadas no init() ----
    private int svX, svY, svSize, svCells, svCellPx;
    private int hueX, hueY, hueW, hueH;
    private int oppX, oppY, oppW, oppH;
    private int pendingOpacity = 255;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingOpacity = false;

    private EditBox hexField;
    private Button mimicryButton;
    private Button eyedropperButton;

    private final List<PartEntry> partEntries = new ArrayList<>();

    private static final int HISTORY_SLOTS = 8;
    private static final int FAVORITE_SLOTS = 8;
    private static final int SWATCH_SIZE = 16;
    private static final int SWATCH_GAP = 2;

    private int historyRowX, historyRowY;
    private int favoriteRowX, favoriteRowY;
    private int addFavoriteX, addFavoriteY;

    public SkinPainterScreen() {
        super(Component.literal("Skin Camo"));
    }

    @Override
    protected void init() {
        partEntries.clear();

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        // ---------- cor inicial ----------
        Integer captured = Eyedropper.consumeLastColor();
        if (captured != null) {
            currentRgb = captured;
        } else {
            Minecraft mc = Minecraft.getInstance();
            Integer last = mc.player != null
                    ? SkinCamoTextureManager.getLastColor(mc.player.getUUID(), selectedPart)
                    : null;
            currentRgb = last != null ? last : 0xFFFFFF;
        }
        float[] hsv = ColorUtil.rgbToHsv(currentRgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        pendingOpacity = ClientPaintStorage.get().opacity;

        // ---------- coluna esquerda: seletor de partes ----------
        int leftX = panelX + 16;
        int partY = panelY + 42;
        int partW = 112;
        int partH = 17;
        int partGap = 19;

        addPartEntry(BodyPart.HEAD, "Cabeça", leftX, partY, partW, partH);
        addPartEntry(BodyPart.BODY, "Tronco", leftX, partY + partGap, partW, partH);
        addPartEntry(BodyPart.RIGHT_ARM, "Braço D.", leftX, partY + partGap * 2, partW, partH);
        addPartEntry(BodyPart.LEFT_ARM, "Braço E.", leftX, partY + partGap * 3, partW, partH);
        addPartEntry(BodyPart.RIGHT_LEG, "Perna D.", leftX, partY + partGap * 4, partW, partH);
        addPartEntry(BodyPart.LEFT_LEG, "Perna E.", leftX, partY + partGap * 5, partW, partH);

        int actionsY = partY + partGap * 6 + 12;
        int actionH = 18;
        int actionGap = 21;

        eyedropperButton = addRenderableWidget(Button.builder(
                        Component.literal("Conta-gotas"),
                        b -> onEyedropper())
                .bounds(leftX, actionsY, partW, actionH).build());

        addRenderableWidget(Button.builder(
                        Component.literal("Auto-Camuflagem"),
                        b -> onAutoCamo())
                .bounds(leftX, actionsY + actionGap, partW, actionH).build());

        mimicryButton = addRenderableWidget(Button.builder(
                        Component.literal("Mimetismo: OFF"),
                        b -> onToggleMimicry())
                .bounds(leftX, actionsY + actionGap * 2, partW, actionH).build());

        addRenderableWidget(Button.builder(
                        Component.literal("Preencher Tudo"),
                        b -> PaintActions.fillWholeSkin(currentRgb))
                .bounds(leftX, actionsY + actionGap * 3, partW, actionH).build());

        addRenderableWidget(Button.builder(
                        Component.literal("Aplicar na Parte"),
                        b -> PaintActions.paintPart(selectedPart, currentRgb))
                .bounds(leftX, actionsY + actionGap * 4, partW, actionH).build());

        // ---------- coluna direita: seletor de cor ----------
        int rightX = panelX + 144;

        svX = rightX;
        svY = panelY + 30;
        svSize = 120;
        svCells = 20;
        svCellPx = svSize / svCells;

        hueX = svX + svSize + 10;
        hueY = svY;
        hueW = 16;
        hueH = svSize;

        int previewX = hueX + hueW + 16;
        int previewY = svY;

        hexField = new EditBox(this.font, previewX, previewY + 48, 70, 16, Component.literal("hex"));
        hexField.setMaxLength(7);
        hexField.setValue(ColorUtil.toHex(currentRgb));
        hexField.setResponder(this::onHexChanged);
        addRenderableWidget(hexField);

        oppX = rightX;
        oppY = svY + svSize + 34;
        oppW = 196;
        oppH = 10;

        historyRowX = rightX;
        historyRowY = oppY + 30;

        favoriteRowX = rightX;
        favoriteRowY = historyRowY + SWATCH_SIZE + 20;

        addFavoriteX = favoriteRowX + (FAVORITE_SLOTS * (SWATCH_SIZE + SWATCH_GAP));
        addFavoriteY = favoriteRowY;
        addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        b -> ClientPaintStorage.addFavorite(currentRgb))
                .bounds(addFavoriteX, addFavoriteY, 16, SWATCH_SIZE).build());

        // guarda a posição do preview pra usar no render()
        this.previewBoxX = previewX;
        this.previewBoxY = previewY;
    }

    // posição do quadrado de preview grande (calculada no init, usada no render)
    private int previewBoxX, previewBoxY;

    private void addPartEntry(BodyPart part, String label, int x, int y, int w, int h) {
        partEntries.add(new PartEntry(part, label, x, y, w, h));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================
    // Ações dos botões
    // =====================================================================

    private void onEyedropper() {
        Eyedropper.arm();
        this.onClose();
    }

    private void onAutoCamo() {
        Minecraft mc = Minecraft.getInstance();
        ColorSampler.averageNearbyBlocks(mc, 3).ifPresent(rgb -> {
            setColorFromRgb(rgb, true);
            PaintActions.fillWholeSkin(rgb);
        });
    }

    private void onToggleMimicry() {
        MimicryHandler.toggle();
    }

    private void onHexChanged(String text) {
        if (suppressHexCallback) return;
        Integer parsed = ColorUtil.parseHex(text);
        if (parsed != null) {
            setColorFromRgb(parsed, false);
        }
    }

    private void setColorFromRgb(int rgb, boolean updateHexText) {
        currentRgb = rgb & 0xFFFFFF;
        float[] hsv = ColorUtil.rgbToHsv(currentRgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        if (updateHexText && hexField != null) {
            suppressHexCallback = true;
            hexField.setValue(ColorUtil.toHex(currentRgb));
            suppressHexCallback = false;
        }
    }

    // =====================================================================
    // Entrada do mouse
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            if (inRect(mouseX, mouseY, svX, svY, svSize, svSize)) {
                draggingSV = true;
                updateSvFromMouse(mouseX, mouseY);
                return true;
            }
            if (inRect(mouseX, mouseY, hueX, hueY, hueW, hueH)) {
                draggingHue = true;
                updateHueFromMouse(mouseY);
                return true;
            }
            if (inRect(mouseX, mouseY, oppX, oppY - 4, oppW, oppH + 8)) {
                draggingOpacity = true;
                updateOpacityFromMouse(mouseX);
                return true;
            }
            for (PartEntry entry : partEntries) {
                if (inRect(mouseX, mouseY, entry.x, entry.y, entry.w, entry.h)) {
                    selectedPart = entry.part;
                    return true;
                }
            }
            List<Integer> history = ClientPaintStorage.get().history;
            for (int i = 0; i < Math.min(history.size(), HISTORY_SLOTS); i++) {
                int sx = historyRowX + i * (SWATCH_SIZE + SWATCH_GAP);
                if (inRect(mouseX, mouseY, sx, historyRowY, SWATCH_SIZE, SWATCH_SIZE)) {
                    setColorFromRgb(history.get(i), true);
                    return true;
                }
            }
            List<Integer> favorites = ClientPaintStorage.get().favorites;
            for (int i = 0; i < Math.min(favorites.size(), FAVORITE_SLOTS); i++) {
                int sx = favoriteRowX + i * (SWATCH_SIZE + SWATCH_GAP);
                if (inRect(mouseX, mouseY, sx, favoriteRowY, SWATCH_SIZE, SWATCH_SIZE)) {
                    setColorFromRgb(favorites.get(i), true);
                    return true;
                }
            }
        } else if (button == 1) {
            // clique direito em um favorito remove
            List<Integer> favorites = ClientPaintStorage.get().favorites;
            for (int i = 0; i < Math.min(favorites.size(), FAVORITE_SLOTS); i++) {
                int sx = favoriteRowX + i * (SWATCH_SIZE + SWATCH_GAP);
                if (inRect(mouseX, mouseY, sx, favoriteRowY, SWATCH_SIZE, SWATCH_SIZE)) {
                    ClientPaintStorage.removeFavorite(favorites.get(i));
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSV) {
            updateSvFromMouse(mouseX, mouseY);
            return true;
        }
        if (draggingHue) {
            updateHueFromMouse(mouseY);
            return true;
        }
        if (draggingOpacity) {
            updateOpacityFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingOpacity) {
            draggingOpacity = false;
            ClientPaintStorage.setOpacity(pendingOpacity);
        }
        draggingSV = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSvFromMouse(double mx, double my) {
        float s = (float) ((mx - svX) / (double) svSize);
        float v = 1f - (float) ((my - svY) / (double) svSize);
        sat = clamp01(s);
        val = clamp01(v);
        currentRgb = ColorUtil.hsvToRgb(hue, sat, val);
        if (hexField != null) {
            suppressHexCallback = true;
            hexField.setValue(ColorUtil.toHex(currentRgb));
            suppressHexCallback = false;
        }
    }

    private void updateHueFromMouse(double my) {
        float h = (float) ((my - hueY) / (double) hueH) * 360f;
        hue = h < 0 ? 0 : (h > 360 ? 360 : h);
        currentRgb = ColorUtil.hsvToRgb(hue, sat, val);
        if (hexField != null) {
            suppressHexCallback = true;
            hexField.setValue(ColorUtil.toHex(currentRgb));
            suppressHexCallback = false;
        }
    }

    private void updateOpacityFromMouse(double mx) {
        double fraction = (mx - oppX) / (double) oppW;
        fraction = fraction < 0 ? 0 : (fraction > 1 ? 1 : fraction);
        pendingOpacity = (int) Math.round(fraction * 255);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // =====================================================================
    // Renderização
    // =====================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // painel de fundo, semi-transparente, cantos retos com borda sutil
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL_BG);
        outline(g, panelX, panelY, panelW, panelH, COL_PANEL_BORDER);

        g.drawCenteredString(this.font, "Skin Camo - Pintura de Pele", panelX + panelW / 2, panelY + 10, COL_TEXT);

        // ---- coluna esquerda ----
        g.drawString(this.font, "PARTES DO CORPO", panelX + 16, panelY + 30, COL_TEXT_MUTED, false);
        for (PartEntry entry : partEntries) {
            boolean selected = entry.part == selectedPart;
            int bg = selected ? 0xFF35424C : COL_SECTION_BG;
            int border = selected ? COL_SELECTED_BORDER : COL_PANEL_BORDER;
            g.fill(entry.x, entry.y, entry.x + entry.w, entry.y + entry.h, bg);
            outline(g, entry.x, entry.y, entry.w, entry.h, border);
            g.drawString(this.font, entry.label, entry.x + 6, entry.y + 4, selected ? COL_ACCENT : COL_TEXT, false);
        }

        // ---- quadrado de saturação/valor ----
        for (int cy = 0; cy < svCells; cy++) {
            float v = 1f - (cy / (float) (svCells - 1));
            for (int cx = 0; cx < svCells; cx++) {
                float s = cx / (float) (svCells - 1);
                int color = 0xFF000000 | ColorUtil.hsvToRgb(hue, s, v);
                int px = svX + cx * svCellPx;
                int py = svY + cy * svCellPx;
                g.fill(px, py, px + svCellPx, py + svCellPx, color);
            }
        }
        outline(g, svX, svY, svSize, svSize, COL_PANEL_BORDER);
        int cursorX = svX + (int) (sat * svSize);
        int cursorY = svY + (int) ((1f - val) * svSize);
        drawCursorDot(g, cursorX, cursorY);

        // ---- barra de matiz ----
        int hueRows = 60;
        int rowH = Math.max(1, hueH / hueRows);
        for (int i = 0; i < hueRows; i++) {
            float h = (i / (float) (hueRows - 1)) * 360f;
            int color = 0xFF000000 | ColorUtil.hsvToRgb(h, 1f, 1f);
            int py = hueY + i * rowH;
            g.fill(hueX, py, hueX + hueW, py + rowH + 1, color);
        }
        outline(g, hueX, hueY, hueW, hueH, COL_PANEL_BORDER);
        int hueMarkerY = hueY + (int) ((hue / 360f) * hueH);
        g.fill(hueX - 2, hueMarkerY - 1, hueX + hueW + 2, hueMarkerY + 1, 0xFFFFFFFF);

        // ---- preview grande + leitura RGB/HEX ----
        int pvSize = 40;
        g.fill(previewBoxX, previewBoxY, previewBoxX + pvSize, previewBoxY + pvSize, 0xFF000000 | currentRgb);
        outline(g, previewBoxX, previewBoxY, pvSize, pvSize, COL_PANEL_BORDER);

        int r = (currentRgb >> 16) & 0xFF;
        int gg = (currentRgb >> 8) & 0xFF;
        int b = currentRgb & 0xFF;
        g.drawString(this.font, "R:" + r + " G:" + gg + " B:" + b, previewBoxX, previewBoxY + 22, COL_TEXT_MUTED, false);
        g.drawString(this.font, "#", previewBoxX - 10, previewBoxY + 52, COL_TEXT_MUTED, false);

        // ---- opacidade ----
        g.drawString(this.font, "OPACIDADE", oppX, oppY - 12, COL_TEXT_MUTED, false);
        g.fill(oppX, oppY, oppX + oppW, oppY + oppH, COL_SECTION_BG);
        int filled = (int) ((pendingOpacity / 255f) * oppW);
        g.fill(oppX, oppY, oppX + filled, oppY + oppH, 0xFF4C5460);
        outline(g, oppX, oppY, oppW, oppH, COL_PANEL_BORDER);
        g.drawString(this.font, String.valueOf(pendingOpacity), oppX + oppW + 8, oppY, COL_TEXT_MUTED, false);

        // ---- histórico ----
        g.drawString(this.font, "HISTÓRICO", historyRowX, historyRowY - 12, COL_TEXT_MUTED, false);
        drawSwatchRow(g, ClientPaintStorage.get().history, historyRowX, historyRowY, HISTORY_SLOTS);

        // ---- favoritos ----
        g.drawString(this.font, "FAVORITOS", favoriteRowX, favoriteRowY - 12, COL_TEXT_MUTED, false);
        drawSwatchRow(g, ClientPaintStorage.get().favorites, favoriteRowX, favoriteRowY, FAVORITE_SLOTS);

        g.drawCenteredString(this.font, "ESC ou P para fechar - clique direito remove favorito",
                panelX + panelW / 2, panelY + panelH - 12, COL_TEXT_MUTED);

        // atualiza textos dinâmicos dos botões antes de desenhá-los
        if (mimicryButton != null) {
            mimicryButton.setMessage(Component.literal(
                    MimicryHandler.isActive() ? "Mimetismo: ON" : "Mimetismo: OFF"));
        }
        if (eyedropperButton != null) {
            eyedropperButton.setMessage(Component.literal(
                    Eyedropper.isArmed() ? "Conta-gotas: ARMADO" : "Conta-gotas"));
        }

        // desenha os widgets reais (botões, campo de hex) por cima do painel
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSwatchRow(GuiGraphics g, List<Integer> colors, int x, int y, int slots) {
        for (int i = 0; i < slots; i++) {
            int sx = x + i * (SWATCH_SIZE + SWATCH_GAP);
            if (i < colors.size()) {
                g.fill(sx, y, sx + SWATCH_SIZE, y + SWATCH_SIZE, 0xFF000000 | colors.get(i));
                outline(g, sx, y, SWATCH_SIZE, SWATCH_SIZE, COL_SWATCH_BORDER);
            } else {
                g.fill(sx, y, sx + SWATCH_SIZE, y + SWATCH_SIZE, COL_SECTION_BG);
                outline(g, sx, y, SWATCH_SIZE, SWATCH_SIZE, COL_PANEL_BORDER);
            }
        }
    }

    private void drawCursorDot(GuiGraphics g, int x, int y) {
        g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000);
        g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFFFFFF);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Entrada simples representando um botão clicável de seleção de parte do corpo. */
    private static final class PartEntry {
        final BodyPart part;
        final String label;
        final int x, y, w, h;

        PartEntry(BodyPart part, String label, int x, int y, int w, int h) {
            this.part = part;
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
