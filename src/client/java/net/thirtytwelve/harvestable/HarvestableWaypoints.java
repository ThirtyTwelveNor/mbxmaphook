package net.thirtytwelve.harvestable;

import io.dampen59.mineboxadditions.features.harvestable.Harvestable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.thirtytwelve.Mbxmaphook;
import net.thirtytwelve.jm.MbxmaphookJMPlugin;
import net.thirtytwelve.xaero.MbxmaphookXaero;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HarvestableWaypoints {

    private record PendingWaypoint(String name, String dimension, int x, int y, int z) {}

    private static MbxmaphookJMPlugin jmPlugin;

    // JM hangs if you create a waypoint in a dimension it has never seen the player enter,
    // so waypoints for not-yet-visited dimensions are queued here and released once the
    // player has been there at least once (they don't need to still be there).
    private static final Set<String> visitedDimensions = ConcurrentHashMap.newKeySet();
    private static final Map<String, ConcurrentLinkedQueue<PendingWaypoint>> pendingJM = new ConcurrentHashMap<>();

    // fixWaypoints() scans every mod waypoint, so rather than running it after each
    // we debounce, run it once, this many ms after the LAST waypoint creation settles.
    private static final long FIX_DEBOUNCE_MS = 5000;
    private static volatile long lastJMWaypointCreatedAt = 0;
    private static volatile boolean fixWaypointsPending = false;

    // All JM waypoint work runs serially on this background thread so creating large
    // batches doesn't stutter the render thread on slower machines.
    private static final ExecutorService jmExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mbxmaphook-jm-waypoints");
        thread.setDaemon(true);
        return thread;
    });

    public static void setJMPlugin(MbxmaphookJMPlugin plugin) {
        jmPlugin = plugin;
    }

    public static void onHarvestablesAdded(String dimname, List<Harvestable> harvestables) {
        boolean hasJM = FabricLoader.getInstance().isModLoaded("journeymap");
        boolean hasXaero = FabricLoader.getInstance().isModLoaded("xaerominimap");

        if (!hasJM && !hasXaero) return;

        // Fired from the Socket.IO event-loop thread, not the render thread — hop over
        // to the main thread before touching the title HUD.
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> showLoadingTitle(client));

        String dimension = "minecraft:" + dimname;

        if (hasXaero) {
            for (Harvestable h : harvestables) {
                List<Double> coords = h.getCoordinates();
                MbxmaphookXaero.makeWaypoint(h.getName(), dimname,
                        coords.get(0).intValue(), coords.get(1).intValue(), coords.get(2).intValue());
            }
            try {
                MbxmaphookXaero.flushToXaero(Minecraft.getInstance());
            } catch (IOException e) {
                Mbxmaphook.LOGGER.error("[MbxMapHook] Failed to flush Xaero waypoints", e);
            }
        }

        if (hasJM && jmPlugin != null) {
            ConcurrentLinkedQueue<PendingWaypoint> queue = pendingJM.computeIfAbsent(dimension, k -> new ConcurrentLinkedQueue<>());
            for (Harvestable h : harvestables) {
                List<Double> coords = h.getCoordinates();
                queue.add(new PendingWaypoint(h.getName(), dimension,
                        coords.get(0).intValue(), coords.get(1).intValue(), coords.get(2).intValue()));
            }

            if (visitedDimensions.contains(dimension)) {
                jmExecutor.execute(() -> flushPendingJM(dimension));
            }
        }
    }

    /** Run every client tick to detect dimension entry and to fire the debounced fixWaypoints(). */
    public static void onClientTick(Minecraft client) {
        if (jmPlugin == null || client.level == null) return;

        String dimension = dimensionKey(client.level);
        if (visitedDimensions.add(dimension)) {
            ConcurrentLinkedQueue<PendingWaypoint> queue = pendingJM.get(dimension);
            if (queue != null && !queue.isEmpty()) {
                showLoadingTitle(client);
            }
            jmExecutor.execute(() -> flushPendingJM(dimension));
        }

        if (fixWaypointsPending && System.currentTimeMillis() - lastJMWaypointCreatedAt >= FIX_DEBOUNCE_MS) {
            fixWaypointsPending = false;
            jmExecutor.execute(jmPlugin::fixWaypoints);
        }
    }

    private static void flushPendingJM(String dimension) {
        ConcurrentLinkedQueue<PendingWaypoint> queue = pendingJM.get(dimension);
        if (queue == null || queue.isEmpty()) return;

        java.util.Set<String> existingKeys = jmPlugin.buildExistingKeys();
        PendingWaypoint wp;
        while ((wp = queue.poll()) != null) {
            jmPlugin.makeWaypoint(wp.name(), wp.dimension(), wp.x(), wp.y(), wp.z(), existingKeys);
            existingKeys.add(wp.name() + "|" + wp.dimension() + "|" + wp.x() + "|" + wp.y() + "|" + wp.z());
            lastJMWaypointCreatedAt = System.currentTimeMillis();
            fixWaypointsPending = true;
        }
    }

    private static void showLoadingTitle(Minecraft client) {
        client.gui.setTimes(10, 200, 20);
        client.gui.setTitle(Component.literal("Loading waypoints"));
        client.gui.setSubtitle(Component.literal("Adding new locations to your map"));
    }

    private static String dimensionKey(Level level) {
        String key = level.dimension().toString(); // "ResourceKey[minecraft:dimension / minecraft:island_tropical]"
        int sep = key.indexOf(" / ");
        return key.substring(sep + 3, key.length() - 1);
    }

    public static void fixJMWaypointsOnJoin() {
        if (jmPlugin != null) {
            jmExecutor.execute(jmPlugin::fixWaypoints);
        }
    }
}