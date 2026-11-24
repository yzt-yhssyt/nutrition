package com.nutrition;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

public final class NutritionValues {
    private NutritionValues() {
    }

    /**
     * A simple, food-data-driven value. Normal foods land in the hundreds to low
     * thousands; compressed foods naturally scale with their combined nutrition.
     */
    public static int valueOf(FoodComponent food) {
        int nutrition = Math.max(0, food.nutrition());
        int saturation = Math.round(Math.max(0.0F, food.saturation()) * 10.0F);
        return Math.max(100, nutrition * 100 + saturation);
    }

    public static boolean isMeat(ItemStack stack) {
        if (stack.isIn(ItemTags.MEAT) || stack.isIn(ItemTags.FISHES)) {
            return true;
        }

        // Compressed foods are custom items and are not in the vanilla meat tags,
        // so fall back to their registry path (e.g. compressed_foods:cooked_beef_1).
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.contains("beef")
                || path.contains("pork")
                || path.contains("chicken")
                || path.contains("mutton")
                || path.contains("rabbit")
                || path.contains("cod")
                || path.contains("salmon")
                || path.contains("fish")
                || path.contains("flesh")
                || path.contains("meat");
    }
}
