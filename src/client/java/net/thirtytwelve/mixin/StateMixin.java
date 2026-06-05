package net.thirtytwelve.mixin;

import io.dampen59.mineboxadditions.state.State;
import io.dampen59.mineboxadditions.features.harvestable.Harvestable;
import net.thirtytwelve.harvestable.HarvestableWaypoints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = State.class, remap = false)
public class StateMixin {

    @Inject(method = "addMineboxHarvestables", at = @At("RETURN"), remap = false)
    private void onHarvestablesAdded(String islandName, List<Harvestable> data, CallbackInfo ci) {
        HarvestableWaypoints.onHarvestablesAdded(islandName, data);
    }
}