package net.thirtytwelve;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.thirtytwelve.harvestable.HarvestableWaypoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mbxmaphook implements ClientModInitializer {
	public static final String MOD_ID = "mbxmaphook";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Hello Fabric world!");

		if (FabricLoader.getInstance().isModLoaded("journeymap")) {
			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				HarvestableWaypoints.fixJMWaypointsOnJoin()
			);
			ClientTickEvents.END_CLIENT_TICK.register(HarvestableWaypoints::onClientTick);
		}
	}
}