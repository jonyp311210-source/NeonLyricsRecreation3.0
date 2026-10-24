package com.example.skylyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks up time-synced lyrics for the current song from LRCLIB (https://lrclib.net),
 * a free community lyrics database. Nothing is stored in the mod itself.
 */
public class Lyrics {
    public record Line(long timeMs, String text) {}

    public static volatile List<Line> lines = List.of();
    public static volatile String status = "";

    private static volatile String loadedKey = "";
    private static volatile long generation = 0;

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final ExecutorService EX = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "skylyrics-lyrics");
        t.setDaemon(true);
        return t;
    });
    private static final Pattern TS = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");

    /** Called every client tick; starts a lookup when the song changes. */
    public static void tick() {
        NowPlaying.Snap s = NowPlaying.snap;
        String key = s == null ? "" : s.title() + "|" + s.artist();
        if (key.equals(loadedKey)) return;
        loadedKey = key;
        lines = List.of();
        if (s == null) { status = ""; return; }
        status = "Loading lyrics...";
        final long gen = ++generation;
        EX.submit(() -> fetch(s, gen));
    }

    /** Forces a fresh lookup for the current song. */
    public static void reload() { loadedKey = "\0reload"; }

    public static int indexAt(List<Line> ls, long posMs) {
        int idx = -1;
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).timeMs() <= posMs) idx = i; else break;
        }
        return idx;
    }

    private static void fetch(NowPlaying.Snap s, long gen) {
        try {
            String cleanTitle = s.title().replaceAll("\\s*[\\(\\[].*?[\\)\\]]", "").replaceAll("\\s+-\\s+.*$", "").trim();
            String firstArtist = s.artist().split("[,&;]")[0].trim();
            List<String[]> tries = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            String[][] variants = {{s.title(), s.artist()}, {cleanTitle, s.artist()}, {cleanTitle, firstArtist}};
            for (String[] v : variants) if (seen.add(v[0] + "|" + v[1])) tries.add(v);

            List<Line> found = List.of();
            for (String[] t : tries) {
                if (gen != generation) return;
                found = search(t[0], t[1], s.durationMs());
                if (!found.isEmpty()) break;
            }
            if (gen != generation) return;
            lines = found;
            status = found.isEmpty() ? "No synced lyrics found for this song" : "";
        } catch (Exception e) {
            if (gen == generation) status = "Lyrics lookup failed";
        }
    }

    private static List<Line> search(String title, String artist, long durMs) throws Exception {
        String url = "https://lrclib.net/api/search?track_name=" + enc(title) + "&artist_name=" + enc(artist);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "SkyLyrics/0.2 (Minecraft Fabric mod)")
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) return List.of();
        JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();

        JsonObject best = null;
        double bestDiff = Double.MAX_VALUE;
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            JsonElement synced = o.get("syncedLyrics");
            if (synced == null || synced.isJsonNull() || synced.getAsString().isBlank()) continue;
            double d = o.has("duration") && !o.get("duration").isJsonNull() ? o.get("duration").getAsDouble() : 0;
            double diff = durMs > 0 ? Math.abs(d * 1000.0 - durMs) : 0;
            if (diff < bestDiff) { bestDiff = diff; best = o; }
        }
        return best == null ? List.of() : parseLrc(best.get("syncedLyrics").getAsString());
    }

    static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = TS.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find()) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long ms = 0;
                if (m.group(3) != null) ms = Long.parseLong((m.group(3) + "00").substring(0, 3));
                times.add(min * 60_000 + sec * 1000 + ms);
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (long t : times) out.add(new Line(t, text));
        }
        out.sort(Comparator.comparingLong(Line::timeMs));
        return out;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
