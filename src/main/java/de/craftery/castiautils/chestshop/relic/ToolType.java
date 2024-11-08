package de.craftery.castiautils.chestshop.relic;


import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import org.jetbrains.annotations.Nullable;

public enum ToolType {
    SWORD,
    PICKAXE,
    SHOVEL,
    AXE,
    HOE,
    BOW,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;

    public static @Nullable ToolType determine(ItemStack stack) {
        ItemStack defaultStack = stack.getItem().getDefaultStack();

        if (defaultStack.isIn(ItemTags.SWORDS)) {
            return SWORD;
        } else if (defaultStack.isIn(ItemTags.PICKAXES)) {
            return PICKAXE;
        } else if (defaultStack.isIn(ItemTags.SHOVELS)) {
            return SHOVEL;
        } else if (defaultStack.isIn(ItemTags.AXES)) {
            return AXE;
        } else if (defaultStack.isIn(ItemTags.HOES)) {
            return HOE;
        } else if (defaultStack.isIn(ItemTags.HEAD_ARMOR))  {
            return HELMET;
        } else if (defaultStack.isIn(ItemTags.CHEST_ARMOR)) {
            return CHESTPLATE;
        } else if (defaultStack.isIn(ItemTags.LEG_ARMOR)) {
            return LEGGINGS;
        } else if (defaultStack.isIn(ItemTags.FOOT_ARMOR)) {
            return BOOTS;
        } else if (stack.getItem() != Items.BOW) {
            return BOW;
        }
        return null;
    }
}
