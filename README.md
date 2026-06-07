# mbxmaphook

A client-side Fabric mod that hooks into [MineboxAdditions](https://modrinth.com/mod/mineboxadditions) for Harvestables and automatically creates waypoints to map mods.

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 26.1.2 |
| MineboxAdditions | 1.15.4 |

## Map Mod Support

At least one of the following is needed for waypoints to appear:

- **JourneyMap** — waypoints are registered through the official JourneyMap API.
- **Xaero's Minimap + Xaero's World Map** — waypoints are written directly to Xaero files. **Both mods are required**, and you need to relogin after waypoint creation for my Xaero integration to work.

unsure why yet, but sometimes you have to delete minecraft/journeymap or minecraft/xaero+backup

Both integrations are optional — the mod loads fine without either.
No user interaction needed