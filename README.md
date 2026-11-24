# Nutrition

> 肉类与植物两条营养条，长期偏食会营养不良：变慢、挖掘变慢、伤害降低、受伤更多。
>
> Meat and plant nutrition bars: a one-sided diet causes malnutrition, costing speed, mining, damage and defense.

**Minecraft 1.21.1 · Fabric · 作者 yzt**

## 简介

只吃牛排的后果终于出现了。两条营养条分别记录肉类与植物摄入，长期单一饮食会让你明显变弱；均衡饮食则完全没有惩罚。

## 特性

- 两条营养条：肉类（nutrition_meat）与植物（nutrition_plant），上限 96,000
- 每秒自然流失 15 点，创造模式不流失
- 进食按 nutrition × 100 + saturation × 10 补充对应营养，压缩食物因此收益极高
- 低于 12,000 进入营养不良：移速 −10%、挖掘速度 −10%、攻击 −20%、受到伤害 +20%
- 任一营养归零：每秒额外流失 600 点饥饿值，并每 2 秒受 1 点伤害
- 营养条基于隐藏计分板实现，支持多人同步，HUD 与饥饿条并排显示
- 依赖「Hunger Shield」（1000 点饥饿值的那套系统）

## 安装

1. 安装 Minecraft 1.21.1 与 Fabric Loader（≥ 0.16.14）
2. 安装依赖：Hunger Shield（必需）
3. 把 `nutrition-1.0.0.jar` 放进 `mods` 文件夹

## 依赖

Hunger Shield（必需）

## English

Living on steak alone finally has consequences. Two bars track meat and plant intake; a one-sided diet makes you measurably weaker, while a balanced one costs nothing.

- Two bars: meat (nutrition_meat) and plant (nutrition_plant), each capped at 96,000
- Drains 15 points per second; creative mode is exempt
- Eating restores nutrition × 100 + saturation × 10, so compressed foods are extremely valuable
- Below 12,000 you become malnourished: −10% movement speed, −10% mining speed, −20% attack damage, +20% damage taken
- If either bar hits zero: 600 extra hunger drained per second and 1 damage every 2 seconds
- Bars are driven by hidden scoreboard objectives, synced to every player, drawn next to the hunger bar
- Requires Hunger Shield for the 1000-point hunger system

### Installation

1. Minecraft 1.21.1 with Fabric Loader (≥ 0.16.14)
2. Dependencies: Hunger Shield (required)
3. Drop `nutrition-1.0.0.jar` into your `mods` folder

## 链接

- 源码 / Source: https://github.com/yzt-yhssyt/nutrition
- 问题反馈 / Issues: https://github.com/yzt-yhssyt/nutrition/issues

## 许可

MIT License © 2026 yzt. 你可以自由使用、修改、再分发，包括放进整合包。
