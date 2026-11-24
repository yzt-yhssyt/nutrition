package com.nutrition;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class NutritionManager {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("nutrition", "malnutrition_speed");
    private static final Identifier MINING_MODIFIER_ID = Identifier.of("nutrition", "malnutrition_mining");
    private static final Identifier ATTACK_MODIFIER_ID = Identifier.of("nutrition", "malnutrition_attack");
    private static final Map<ServerPlayNetworkHandler, Boolean> SYNCED_HANDLERS = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, Double> DRAIN_ACCUMULATORS = new WeakHashMap<>();

    private NutritionManager() {
    }

    public static void tick(ServerPlayerEntity player) {
        if (player.isSpectator()) {
            removeDebuffs(player);
            return;
        }

        Scoreboard scoreboard = player.getScoreboard();
        ScoreAccess meat = getOrCreateScore(scoreboard, NutritionConfig.MEAT_OBJECTIVE, player);
        ScoreAccess plant = getOrCreateScore(scoreboard, NutritionConfig.PLANT_OBJECTIVE, player);

        int meatValue = meat.getScore();
        int plantValue = plant.getScore();

        if (!player.isCreative()) {
            // 30 points/second at 20 ticks/second = 1.5 points per tick. Remove
            // whole points one at a time (sometimes 2 in a tick, because 30 is
            // not divisible by 20), never a single 15-point jump.
            double accumulator = DRAIN_ACCUMULATORS.getOrDefault(player, 0.0)
                    + NutritionConfig.DRAIN_PER_SECOND / 20.0;
            int drops = 0;
            while (accumulator >= 1.0) {
                accumulator -= 1.0;
                drops++;
            }
            if (drops > 0) {
                int newMeat = Math.max(0, meatValue - drops);
                int newPlant = Math.max(0, plantValue - drops);
                if (newMeat != meatValue || newPlant != plantValue) {
                    meatValue = newMeat;
                    plantValue = newPlant;
                    meat.setScore(meatValue);
                    plant.setScore(plantValue);
                    broadcast(player, NutritionConfig.MEAT_OBJECTIVE, meatValue);
                    broadcast(player, NutritionConfig.PLANT_OBJECTIVE, plantValue);
                }
            }
            DRAIN_ACCUMULATORS.put(player, accumulator);

            // Zero-bar punishment stays on a one-second cadence.
            if ((meatValue <= 0 || plantValue <= 0) && player.age % 20 == 0) {
                drainHunger(player, NutritionConfig.ZERO_HUNGER_DRAIN_PER_SECOND);

                if (player.age % 40 == 0) {
                    player.damage(player.getDamageSources().starve(), 1.0F);
                }
            }
        }

        if (player.isCreative()) {
            removeDebuffs(player);
            return;
        }

        if (meatValue < NutritionConfig.MALNUTRITION_THRESHOLD
                || plantValue < NutritionConfig.MALNUTRITION_THRESHOLD) {
            applyDebuffs(player);
        } else {
            removeDebuffs(player);
        }
    }

    public static void onEat(ServerPlayerEntity player, ItemStack stack, FoodComponent food) {
        if (food == null) {
            return;
        }

        String objective = NutritionValues.isMeat(stack)
                ? NutritionConfig.MEAT_OBJECTIVE
                : NutritionConfig.PLANT_OBJECTIVE;
        int value = NutritionValues.valueOf(food);
        ScoreAccess score = getOrCreateScore(player.getScoreboard(), objective, player);
        int newValue = Math.min(NutritionConfig.MAX_NUTRITION, score.getScore() + value);
        score.setScore(newValue);
        broadcast(player, objective, newValue);
    }

    /**
     * Sends the hidden objectives and current values to one player. Hidden
     * objectives are not part of the normal display-slot login sync, so this is
     * needed for every player that joins after the objectives were created.
     */
    public static void sync(ServerPlayerEntity player) {
        if (SYNCED_HANDLERS.containsKey(player.networkHandler)) {
            // Respawn reuses the connection and the client keeps the objectives.
            return;
        }

        Scoreboard scoreboard = player.getScoreboard();
        for (String objectiveName : new String[]{NutritionConfig.MEAT_OBJECTIVE, NutritionConfig.PLANT_OBJECTIVE}) {
            ScoreboardObjective objective = ensureObjective(scoreboard, objectiveName);
            ScoreAccess score = getOrCreateScore(scoreboard, objectiveName, player);
            player.networkHandler.sendPacket(
                    new ScoreboardObjectiveUpdateS2CPacket(objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE)
            );
            player.networkHandler.sendPacket(
                    new ScoreboardScoreUpdateS2CPacket(
                            player.getGameProfile().getName(),
                            objectiveName,
                            score.getScore(),
                            Optional.empty(),
                            Optional.empty()
                    )
            );
        }
        SYNCED_HANDLERS.put(player.networkHandler, Boolean.TRUE);
    }

    private static void broadcast(ServerPlayerEntity owner, String objectiveName, int value) {
        String holderName = owner.getGameProfile().getName();
        for (ServerPlayerEntity target : owner.server.getPlayerManager().getPlayerList()) {
            if (!SYNCED_HANDLERS.containsKey(target.networkHandler)) {
                continue;
            }
            target.networkHandler.sendPacket(
                    new ScoreboardScoreUpdateS2CPacket(
                            holderName,
                            objectiveName,
                            value,
                            Optional.empty(),
                            Optional.empty()
                    )
            );
        }
    }

    private static void drainHunger(ServerPlayerEntity player, int amount) {
        float saturation = player.getHungerManager().getSaturationLevel();
        int remaining = amount;

        if (saturation > 0.0F) {
            float taken = Math.min(saturation, (float) remaining);
            player.getHungerManager().setSaturationLevel(saturation - taken);
            remaining -= (int) Math.ceil(taken);
        }

        if (remaining > 0) {
            int food = Math.max(0, player.getHungerManager().getFoodLevel() - remaining);
            player.getHungerManager().setFoodLevel(food);
        }
    }

    public static boolean isMalnourished(ServerPlayerEntity player) {
        int meat = readScore(player.getScoreboard(), NutritionConfig.MEAT_OBJECTIVE, player);
        int plant = readScore(player.getScoreboard(), NutritionConfig.PLANT_OBJECTIVE, player);
        return meat < NutritionConfig.MALNUTRITION_THRESHOLD
                || plant < NutritionConfig.MALNUTRITION_THRESHOLD;
    }

    public static int readScore(Scoreboard scoreboard, String objectiveName, ServerPlayerEntity player) {
        ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);
        if (objective == null) {
            return NutritionConfig.MAX_NUTRITION;
        }
        ReadableScoreboardScore score = scoreboard.getScore(scoreHolder(player), objective);
        return score == null ? NutritionConfig.MAX_NUTRITION : score.getScore();
    }

    private static ScoreAccess getOrCreateScore(Scoreboard scoreboard, String objectiveName, ServerPlayerEntity player) {
        ScoreboardObjective objective = ensureObjective(scoreboard, objectiveName);
        ScoreHolder holder = scoreHolder(player);
        ReadableScoreboardScore existing = scoreboard.getScore(holder, objective);
        ScoreAccess score = scoreboard.getOrCreateScore(holder, objective);
        if (existing == null) {
            score.setScore(NutritionConfig.MAX_NUTRITION);
        }
        return score;
    }

    private static ScoreboardObjective ensureObjective(Scoreboard scoreboard, String objectiveName) {
        ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    objectiveName,
                    ScoreboardCriterion.DUMMY,
                    Text.literal(objectiveName.equals(NutritionConfig.MEAT_OBJECTIVE) ? "肉类营养" : "植物营养"),
                    ScoreboardCriterion.RenderType.INTEGER,
                    false,
                    null
            );
        }

        return objective;
    }

    private static ScoreHolder scoreHolder(ServerPlayerEntity player) {
        return ScoreHolder.fromName(player.getGameProfile().getName());
    }

    private static void applyDebuffs(ServerPlayerEntity player) {
        setModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER_ID,
                NutritionConfig.MALNUTRITION_MOVEMENT_MULTIPLIER);
        setModifier(player, EntityAttributes.PLAYER_BLOCK_BREAK_SPEED, MINING_MODIFIER_ID,
                NutritionConfig.MALNUTRITION_MINING_MULTIPLIER);
        setModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER_ID,
                NutritionConfig.MALNUTRITION_ATTACK_MULTIPLIER);
    }

    private static void removeDebuffs(ServerPlayerEntity player) {
        removeModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER_ID);
        removeModifier(player, EntityAttributes.PLAYER_BLOCK_BREAK_SPEED, MINING_MODIFIER_ID);
        removeModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER_ID);
    }

    private static void setModifier(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                                    Identifier id, float amount) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }
        instance.addTemporaryModifier(new EntityAttributeModifier(
                id,
                amount,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void removeModifier(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                                       Identifier id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
