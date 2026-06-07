package net.thirtytwelve.xaero;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MbxmaphookXaero {

    private static final Path TEMP_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mbxmaphook")
            .resolve("minimap");

    private static String cachedWorldFolder = null;
    private static String cachedDimension = null;
    private static Path cachedFile = null;

    private static Path resolveFile(String dimension) {
        if (cachedWorldFolder == null) {
            var server = Minecraft.getInstance().getCurrentServer();
            cachedWorldFolder = server != null ? "Multiplayer_" + server.ip.replace(":", "_") : "Multiplayer_play.minebox.co";
        }

        if (!dimension.equals(cachedDimension)) {
            cachedDimension = dimension;
            String dimFolder = switch (dimension) {
                case "minecraft:overworld"  -> "dim%0";
                case "minecraft:the_nether" -> "dim%-1";
                case "minecraft:the_end"    -> "dim%1";
                default -> dimension.replace(":", "$").replace("/", "%");
            };
            cachedFile = TEMP_DIR.resolve(cachedWorldFolder).resolve(dimFolder).resolve("mw$default_1.txt");
        }

        return cachedFile;
    }

    public static void makeWaypoint(String name, String dimension, int x, int y, int z) {
        Path file = resolveFile(dimension);

        String initials = name.substring(0, 1).toUpperCase();
        String namedLine = String.format(
                "waypoint:%s:%s:%d:%d:%d:7:false:0:%s:false:0:1:false",
                name, initials, x, y, z, name
        );
        String allLine = String.format(
                "waypoint:%s:%s:%d:%d:%d:7:false:0:all:false:0:1:false",
                name, initials, x, y, z
        );

        List<String> existingWaypoints = new ArrayList<>();
        Set<String> sets = new LinkedHashSet<>();
        sets.add("all");

        if (file.toFile().exists()) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (!line.startsWith("waypoint:")) continue;
                    existingWaypoints.add(line);
                    sets.add(line.split(":")[9]);
                }
            } catch (IOException e) { throw new RuntimeException(e); }
        }

        existingWaypoints.add(namedLine);
        existingWaypoints.add(allLine);
        sets.add(name);

        StringBuilder sb = new StringBuilder();
        sb.append("sets:").append(String.join(":", sets)).append(":gui.xaero_default\n#\n");
        sb.append("#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination\n#\n");
        existingWaypoints.forEach(wp -> sb.append(wp).append("\n"));

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void flushToXaero(Minecraft client) throws IOException {
        if (!TEMP_DIR.toFile().exists()) return;
        Path xaeroDir = client.gameDirectory.toPath().resolve("xaero").resolve("minimap");
        copyRecursively(TEMP_DIR, xaeroDir);
    }

    private static void copyRecursively(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (var stream = Files.list(src)) {
            for (Path entry : stream.toList()) {
                Path target = dest.resolve(entry.getFileName());
                if (Files.isDirectory(entry)) {
                    copyRecursively(entry, target);
                } else {
                    Files.copy(entry, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}