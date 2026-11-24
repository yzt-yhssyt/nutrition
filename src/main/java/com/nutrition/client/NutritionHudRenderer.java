package com.nutrition.client;

import com.nutrition.NutritionConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class NutritionHudRenderer {
    private static final int BAR_HEIGHT = 9;
    private static final int MAX_BAR_WIDTH = 100;

    private NutritionHudRenderer() {
    }

    public static void render(DrawContext context, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || player.isSpectator()) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarRight = screenWidth / 2 + 91;

        int availableSide = (screenWidth - 182) / 2;
        int barWidth = MathHelper.clamp(availableSide - 10, 48, MAX_BAR_WIDTH);
        int barY = screenHeight - 10;

        int leftX = hotbarLeft - barWidth - 5;
        int rightX = hotbarRight + 5;
        if (leftX < 2) {
            leftX = 2;
        }
        if (rightX + barWidth > screenWidth - 2) {
            rightX = screenWidth - barWidth - 2;
        }

        int meat = readScore(player, NutritionConfig.MEAT_OBJECTIVE);
        int plant = readScore(player, NutritionConfig.PLANT_OBJECTIVE);

        drawBar(context, leftX, barY, barWidth, meat,
                "肉类营养", 0xFFE05A5A, 0xFF6B1F1F, 0xFFFFC0C0);
        drawBar(context, rightX, barY, barWidth, plant,
                "植物营养", 0xFF5FD35F, 0xFF1F6B1F, 0xFFC0FFC0);
    }

    private static void drawBar(DrawContext context, int x, int y, int width, int value,
                                String label, int fillColor, int borderColor, int labelColor) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        boolean low = value < NutritionConfig.MALNUTRITION_THRESHOLD;
        int actualBorder = low ? 0xFFFF3030 : borderColor;
        int actualLabel = low ? 0xFFFF5555 : labelColor;

        context.fill(x, y, x + width, y + BAR_HEIGHT, 0xFF101010);
        int innerWidth = width - 2;
        int filled = MathHelper.clamp(
                Math.round(innerWidth * (value / (float) NutritionConfig.MAX_NUTRITION)),
                0,
                innerWidth
        );
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, fillColor);
        }
        context.drawBorder(x, y, width, BAR_HEIGHT, actualBorder);

        context.drawTextWithShadow(textRenderer, Text.literal(label), x, y - 10, actualLabel);

        String valueText = value + "/" + NutritionConfig.MAX_NUTRITION;
        int textWidth = textRenderer.getWidth(valueText);
        context.drawTextWithShadow(textRenderer, valueText,
                x + (width - textWidth) / 2, y + 1, 0xFFFFFFFF);
    }

    private static int readScore(PlayerEntity player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);
        if (objective == null) {
            return NutritionConfig.MAX_NUTRITION;
        }
        ReadableScoreboardScore score = scoreboard.getScore(
                ScoreHolder.fromName(player.getGameProfile().getName()),
                objective
        );
        if (score == null) {
            return NutritionConfig.MAX_NUTRITION;
        }
        return MathHelper.clamp(score.getScore(), 0, NutritionConfig.MAX_NUTRITION);
    }
}
