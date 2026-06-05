package net.thirtytwelve.harvestable;

import io.dampen59.mineboxadditions.features.harvestable.Harvestable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.thirtytwelve.Mbxmaphook;
import net.thirtytwelve.jm.MbxmaphookJMPlugin;
import net.thirtytwelve.xaero.MbxmaphookXaero;

import java.io.IOException;
import java.util.List;

public class HarvestableWaypoints {

    private static MbxmaphookJMPlugin jmPlugin;

    public static void setJMPlugin(MbxmaphookJMPlugin plugin) {
        jmPlugin = plugin;
    }

    public static void onHarvestablesAdded(String dimension, List<Harvestable> harvestables) {
        boolean hasJM = FabricLoader.getInstance().isModLoaded("journeymap");
        boolean hasXaero = FabricLoader.getInstance().isModLoaded("xaerominimap");

        if (!hasJM && !hasXaero) return;

        for (Harvestable h : harvestables) {
            String name = h.getName();
            List<Double> coords = h.getCoordinates();
            int x = coords.get(0).intValue();
            int y = coords.get(1).intValue();
            int z = coords.get(2).intValue();

            if (hasJM && jmPlugin != null) {
                jmPlugin.makeWaypoint(name, dimension, x, y, z);
            }

            if (hasXaero) {
                MbxmaphookXaero.makeWaypoint(name, dimension, x, y, z);
            }
        }

        if (hasXaero) {
            try {
                MbxmaphookXaero.flushToXaero(Minecraft.getInstance());
            } catch (IOException e) {
                Mbxmaphook.LOGGER.error("[MbxMapHook] Failed to flush Xaero waypoints", e);
            }
        } else if (jmPlugin != null) {
            jmPlugin.fixWaypoints();
        }
    }
}