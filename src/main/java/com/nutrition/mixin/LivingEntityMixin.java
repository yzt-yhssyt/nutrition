package com.nutrition.mixin;

import com.nutrition.NutritionConfig;
import com.nutrition.NutritionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true)
    private float nutrition$amplifyDamageTaken(float amount) {
        if ((Object) this instanceof ServerPlayerEntity player && NutritionManager.isMalnourished(player)) {
            return amount * NutritionConfig.MALNUTRITION_DAMAGE_TAKEN_MULTIPLIER;
        }
        return amount;
    }
}
