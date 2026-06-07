package net.thirtytwelve.jm;

import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.minecraft.core.BlockPos;
import net.thirtytwelve.Mbxmaphook;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class MbxmaphookJMPlugin implements IClientPlugin {

    private IClientAPI jmAPI;

    @Override
    public void initialize(IClientAPI api) {
        this.jmAPI = api;
        net.thirtytwelve.harvestable.HarvestableWaypoints.setJMPlugin(this);
        Mbxmaphook.LOGGER.info("[MbxMapHook] JourneyMap plugin initialised");

    }

    public void fixWaypoints() {
        // Get all waypoints from our mod
        List<? extends Waypoint> myWaypoints = jmAPI.getWaypoints(Mbxmaphook.MOD_ID);

        // Collect ungrouped waypoints by name
        Map<String, List<Waypoint>> waypointsByName = new HashMap<>();

        for (Waypoint waypoint : myWaypoints) {
            // Skip if waypoint is already in one of our groups
            if (jmAPI.getWaypointGroup(waypoint.getGroupId()).getModId().equals(Mbxmaphook.MOD_ID)) {
                continue;
            }

            String waypointName = waypoint.getName();
            waypointsByName.computeIfAbsent(waypointName, _ -> new ArrayList<>()).add(waypoint);
        }

        // Process waypoints that have duplicates
        for (Map.Entry<String, List<Waypoint>> entry : waypointsByName.entrySet()) {
            String waypointName = entry.getKey();
            List<Waypoint> waypoints = entry.getValue();

            if (waypoints.size() <= 1) continue;

            WaypointGroup group = jmAPI.getWaypointGroupByName(Mbxmaphook.MOD_ID, waypointName);
            if (group == null) {
                group = WaypointFactory.createWaypointGroup(Mbxmaphook.MOD_ID, waypointName);
                group.setEnabled(false);
                group.setColorOverride(true);
                jmAPI.addWaypointGroup(group);
            }

            for (Waypoint waypoint : waypoints) {
                group.addWaypoint(waypoint);
            }
        }
    }

    public void makeWaypoint(String name, String dimension, int x, int y, int z) {
        makeWaypoint(name, dimension, x, y, z, null);
    }

    public void makeWaypoint(String name, String dimension, int x, int y, int z, java.util.Set<String> existingKeys) {
        BlockPos pos = new BlockPos(x, y, z);
        String key = name + "|" + dimension + "|" + x + "|" + y + "|" + z;

        if (existingKeys != null) {
            if (existingKeys.contains(key)) return;
        } else {
            boolean exists = jmAPI.getWaypoints(Mbxmaphook.MOD_ID).stream().anyMatch(wp ->
                    wp.getName().equals(name) &&
                    wp.getBlockPos().equals(pos) &&
                    wp.getPrimaryDimension().equals(dimension)
            );
            if (exists) return;
        }

        Waypoint waypoint = WaypointFactory.createWaypoint(Mbxmaphook.MOD_ID, pos, name, dimension, true);
        waypoint.setColor(0x00FFFF);
        waypoint.setEnabled(true);
        jmAPI.addWaypoint(Mbxmaphook.MOD_ID, waypoint);
    }

    public java.util.Set<String> buildExistingKeys() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Waypoint wp : jmAPI.getWaypoints(Mbxmaphook.MOD_ID)) {
            BlockPos p = wp.getBlockPos();
            keys.add(wp.getName() + "|" + wp.getPrimaryDimension() + "|" + p.getX() + "|" + p.getY() + "|" + p.getZ());
        }
        return keys;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public String getModId() {
        return Mbxmaphook.MOD_ID;
    }
}