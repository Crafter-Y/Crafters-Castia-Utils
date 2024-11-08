package de.craftery.castiautils.chestshop.relic;

import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class RelicPriceEstimation {
    public static void estimateItemValue(ItemStack stack, List<Text> lines) {
        ToolType toolType = ToolType.determine(stack);

        float itemValue = 0;
        RelicType relic = null;

        for (Component<?> component : stack.getComponents()) {
            if (component.type() == DataComponentTypes.LORE && component.value() instanceof LoreComponent lore) {
                for (Text line : lore.lines()) {
                    if (line.getString().length() == 2) {
                        relic = RelicType.of(line.getString().charAt(0));
                    }
                }
            }
        }
        if (relic == null) return;
        itemValue += relic.getValue();

        for (Text line : lines) {
            if (!line.getString().isEmpty() && line.getString().charAt(0) == relic.getCharIntValue() && line instanceof MutableText mutText) {
                mutText.append(getPriceAppendix(relic.getValue()));
            } else if (line instanceof MutableText mutText && mutText.getContent() instanceof TranslatableTextContent translatable) {
                if (translatable.getKey().startsWith("enchantment.minecraft.")) {
                    String enchantment = translatable.getKey().replace("enchantment.minecraft.", "");
                    int level = 1;
                    if (line.getSiblings().size() == 2 && line.getSiblings().get(1).getContent() instanceof TranslatableTextContent translatableLevel) {
                        if (translatableLevel.getKey().startsWith("enchantment.level.")) {
                            level = Integer.parseInt(translatableLevel.getKey().replace("enchantment.level.", ""));
                        }
                    }

                    float enchantmentValue = getEnchantmentValue(enchantment, level, relic, toolType);

                    if (stack.getEnchantments().getEnchantments().stream().anyMatch(enchant -> enchant.matchesKey(Enchantments.SILK_TOUCH)) && enchantment.equals("fortune")) continue;

                    if (enchantmentValue != 0) {
                        itemValue += enchantmentValue;
                        mutText.append(getPriceAppendix(enchantmentValue));
                    }
                }
            } else if (line instanceof MutableText mutText && line.getString().equals("Tree Feller")) {
                itemValue += 5000;
                mutText.append(getPriceAppendix(5000));
            } else if (line instanceof MutableText mutText && line.getString().equals("Collection")) {
                itemValue += 5000;
                mutText.append(getPriceAppendix(5000));
            } else if (line instanceof MutableText mutText && line.getString().equals("Vein Miner")) {
                itemValue += 5000;
                mutText.append(getPriceAppendix(5000));
            }
        }

        DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);

        MutableText containerText = Text.empty();
        containerText.append(Text.literal("Estimated value: ").formatted(Formatting.GRAY));
        containerText.append(Text.literal("$" + df.format(itemValue)).formatted(Formatting.GOLD));
        lines.add(containerText);
    }

    private static float getEnchantmentValue(String enchantment, Integer level, RelicType relicType, ToolType toolType) {
        if (enchantment.equals("mending")) {
            return 500;
        }
        if (enchantment.equals("infinity")) {
            return 500;
        }
        if (enchantment.equals("aqua_affinity")) {
            return 500;
        }
        if (enchantment.equals("sharpness")) {
            if (toolType == ToolType.SWORD) return (float) (800*Math.pow(1.5, level.doubleValue()));
            return (float) (600*Math.pow(1.5, level.doubleValue()));
        }
        if (enchantment.equals("protection")) {
            if (level < 6) return level*1000;
            return (float) (10000*Math.pow(2, (level.doubleValue()-6)));
        } else if (enchantment.contains("protection")) {
            return (float) (800*Math.pow(1.2, (level.doubleValue()-1)));
        }
        if (enchantment.equals("feather_falling")) {
            return (float) (400*Math.pow(3, (level.doubleValue()-1)));
        }
        if (enchantment.equals("thorns")) {
            return 1000*level;
        }
        if (enchantment.equals("looting")) {
            return (float) (1000*Math.pow(3, (level.doubleValue()-1)));
        }
        if (enchantment.equals("silk_touch")) {
            return 500;
        }
        if (enchantment.equals("fortune")) {
            return (float) (1000*Math.pow(3, (level.doubleValue()-1)));
        }
        if (enchantment.equals("efficiency")) {
            if (level < 6) return level*500;
            if (toolType == ToolType.PICKAXE) {
                if (level == 6) return 25000;
                if (level == 7) return 45000;
                if (level == 8) return 140000;
                if (level == 9) return 475000;
                if (level == 10) return 1000000;
                return (float) (1000000*Math.pow(2, (level.doubleValue()-10)));
            }
            if (level == 6) return 20000;
            if (level == 7) return 35000;
            if (level == 8) return 55000;
            if (level == 9) return 100000;
            if (level == 10) return 250000;
            return (float) (1000000*Math.pow(2, (level.doubleValue()-10)));
        }
        if (enchantment.equals("unbreaking")) {
            if (level < 4) return level*1000;
            if (level == 4) return 5000;
            if (level == 5) return 10000;
            if (level == 6) return 17500;
            if (level == 7) return 25000;
            if (level == 8) return 30000;
            if (level == 9) return 50000;
            if (level == 10) return 75000;
            return (float) (75000*Math.pow(2, (level.doubleValue()-10)));
        }
        if (enchantment.equals("fire_aspect")) {
            return (float) Math.min((500*Math.pow(4, (level.doubleValue()-1))), 4000);
        }
        if (enchantment.equals("flame")) {
            return (float) Math.min((500*Math.pow(4, (level.doubleValue()-1))), 6000);
        }
        if (enchantment.equals("sweeping_edge")) {
            return (float) (400*Math.pow(1.5, (level.doubleValue()-1)));
        }
        if (enchantment.equals("smite")) {
            return (float) (400*Math.pow(1.5, (level.doubleValue()-1)));
        }
        if (enchantment.equals("punch")) {
            return (float) (400*Math.pow(1.5, (level.doubleValue()-1)));
        }
        if (enchantment.equals("knockback")) {
            return (float) (400*Math.pow(1.5, (level.doubleValue()-1)));
        }
        if (enchantment.equals("lure")) {
            if (level < 3) return level*1000;
            return (float) (15000*Math.pow(3.5, (level.doubleValue()-3)));
        }
        if (enchantment.equals("luck_of_the_sea")) {
            return (float) (400*Math.pow(2, (level.doubleValue()-1)));
        }
        if (enchantment.equals("power")) {
            if (toolType == ToolType.SWORD) return (float) (800*Math.pow(1.5, level.doubleValue()));
            return (float) (600*Math.pow(1.5, level.doubleValue()));
        }
        if (enchantment.equals("soul_speed")) {
            return (float) (300*Math.pow(2, (level.doubleValue()-1)));
        }
        if (enchantment.equals("depth_strider")) {
            return (float) (1000*Math.pow(1.5, (level.doubleValue()-1)));
        }
        if (enchantment.equals("swift_sneak")) {
            return 1000*level;
        }
        if (enchantment.equals("respiration")) {
            return (float) (1000*Math.pow(1.5, (level.doubleValue()-1)));
        }

        return 0;
    }

    private static MutableText getPriceAppendix(float value) {
        DecimalFormat df = new DecimalFormat("#,###.#", new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);

        MutableText priceAppendix = Text.literal(" ");
        priceAppendix.append(Text.literal("(+").formatted(Formatting.GRAY));
        priceAppendix.append(Text.literal("$" + df.format(value))).formatted(Formatting.GOLD);
        priceAppendix.append(Text.literal(")").formatted(Formatting.GRAY));
        return priceAppendix;
    }
}
