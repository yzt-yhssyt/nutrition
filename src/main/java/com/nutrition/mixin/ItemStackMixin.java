package com.nutrition.mixin;

import com.nutrition.NutritionValues;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void nutrition$addNutritionTooltip(Item.TooltipContext context, PlayerEntity player,
                                               TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) {
            return;
        }

        int value = NutritionValues.valueOf(food);
        Text line = NutritionValues.isMeat(stack)
                ? Text.translatable("tooltip.nutrition.meat", value).formatted(Formatting.RED)
                : Text.translatable("tooltip.nutrition.plant", value).formatted(Formatting.GREEN);

        // getTooltip() is not cancellable. Its returned list is a fresh mutable
        // list, so append in place instead of calling setReturnValue().
        List<Text> lines = cir.getReturnValue();
        try {
            lines.add(line);
        } catch (UnsupportedOperationException ignored) {
            // A few mods return immutable tooltip lists; never crash the client.
        }
    }
}
