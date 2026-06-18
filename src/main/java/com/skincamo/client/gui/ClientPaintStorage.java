package com.skincamo.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Salva preferências da GUI (histórico de cores, favoritos, opacidade) em um
 * JSON pequeno em config/skincamo/<uuid>.json. Isso é independente da
 * capability do servidor (que guarda a cor "oficial" da skin) - aqui é só
 * conveniência de interface, então pode ficar 100% client-side.
 */
public final class ClientPaintStorage {

    private ClientPaintStorage() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_HISTORY = 16;

    public static class Model {
        public List<Integer> history = new ArrayList<>();
        public List<Integer> favorites = new ArrayList<>();
        public int opacity = 255; // reservado para expansões futuras (transparência na skin)
    }

    private static Model cached;

    private static Path file() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("skincamo");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        String uuid = Minecraft.getInstance().getUser() != null
                ? Minecraft.getInstance().getUser().getProfileId().toString()
                : "default";
        return dir.resolve(uuid + ".json");
    }

    public static Model get() {
        if (cached != null) return cached;
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                cached = GSON.fromJson(reader, Model.class);
            } catch (Exception e) {
                cached = null;
            }
        }
        if (cached == null) cached = new Model();
        return cached;
    }

    public static void save() {
        if (cached == null) return;
        try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            GSON.toJson(cached, writer);
        } catch (IOException ignored) {}
    }

    public static void pushHistory(int rgb) {
        Model model = get();
        // Remove duplicata existente para trazer pra frente, sem repetir.
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        set.add(rgb);
        set.addAll(model.history);
        model.history = new ArrayList<>(set);
        while (model.history.size() > MAX_HISTORY) {
            model.history.remove(model.history.size() - 1);
        }
        save();
    }

    public static void addFavorite(int rgb) {
        Model model = get();
        if (!model.favorites.contains(rgb)) {
            model.favorites.add(rgb);
            save();
        }
    }

    public static void removeFavorite(int rgb) {
        Model model = get();
        model.favorites.remove(Integer.valueOf(rgb));
        save();
    }

    public static void setOpacity(int opacity) {
        get().opacity = opacity;
        save();
    }
}
