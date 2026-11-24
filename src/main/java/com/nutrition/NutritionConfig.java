package com.nutrition;

public final class NutritionConfig {
    public static final int MAX_NUTRITION = 96_000;
    public static final int MALNUTRITION_THRESHOLD = 12_000;
    public static final int DRAIN_PER_SECOND = 30;
    // Hunger Shield drains about 20 food points per second normally; a zero bar
    // makes that 30x faster. Direct food/saturation drain avoids the exhaustion cap.
    public static final int ZERO_HUNGER_DRAIN_PER_SECOND = 600;
    public static final float MALNUTRITION_MOVEMENT_MULTIPLIER = -0.10F;
    public static final float MALNUTRITION_MINING_MULTIPLIER = -0.10F;
    public static final float MALNUTRITION_ATTACK_MULTIPLIER = -0.20F;
    public static final float MALNUTRITION_DAMAGE_TAKEN_MULTIPLIER = 1.20F;

    public static final String MEAT_OBJECTIVE = "nutrition_meat";
    public static final String PLANT_OBJECTIVE = "nutrition_plant";

    private NutritionConfig() {
    }
}
