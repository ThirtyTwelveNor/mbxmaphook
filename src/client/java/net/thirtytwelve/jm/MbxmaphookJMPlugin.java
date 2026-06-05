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

        // Track created groups by name
        Map<String, WaypointGroup> groupsByName = new HashMap<>();

        // First, collect all waypoint names and their counts
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

            if (waypoints.size() > 1) {
                WaypointGroup group = groupsByName.get(waypointName);
                if (group == null) {
                    group = WaypointFactory.createWaypointGroup(Mbxmaphook.MOD_ID, waypointName);
                    group.setEnabled(false);
                    group.setColorOverride(true);
                    jmAPI.addWaypointGroup(group);
                    groupsByName.put(waypointName, group);
                }

                // Add all waypoints to the group
                for (Waypoint waypoint : waypoints) {
                    group.addWaypoint(waypoint);
                }
            }
        }
    }

    public void makeWaypoint(String name, String dimension, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        Waypoint waypoint = WaypointFactory.createWaypoint(Mbxmaphook.MOD_ID, pos, name, dimension, true);
        // Set default colors (optional - you can modify these)
        waypoint.setColor(0x00FFFF); // Light blue color
        waypoint.setEnabled(true);

        // Add the waypoint to JourneyMap
        jmAPI.addWaypoint(Mbxmaphook.MOD_ID, waypoint);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public String getModId() {
        return Mbxmaphook.MOD_ID;
    }
}