package com.nutrition.mixin;

import com.nutrition.NutritionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Unique
    private boolean nutrition$synced;

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void nutrition$tick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!nutrition$synced) {
            NutritionManager.sync(player);
            nutrition$synced = true;
        }
        NutritionManager.tick(player);
    }
}
