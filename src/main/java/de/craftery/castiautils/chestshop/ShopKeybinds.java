package de.craftery.castiautils.chestshop;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class ShopKeybinds {
    private static KeyBinding sellKeybind;
    private static KeyBinding buyKeybind;

    private static boolean inGame = false;

    public static void register() {
        sellKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("castiautils.keybinds.sell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_S, "castiautils.keybinds.category"));
        buyKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("castiautils.keybinds.buy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "castiautils.keybinds.category"));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> inGame = true);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> inGame = false);

        ScreenEvents.BEFORE_INIT.register((client, _screen, scaledWidth, scaledHeight) -> {
            if (!inGame) return;

            ScreenKeyboardEvents.afterKeyPress(_screen).register((screen, key, scancode, modifiers) -> {
                if (sellKeybind.matchesKey(key, scancode)) {
                    ItemStack at = getHoveredSlot(client);
                    if (at != null) {
                        screen.close();
                        ShopCommand.sellItem(client.player, ShopLogger.getItemId(at));
                    }
                }
                if (buyKeybind.matchesKey(key, scancode)) {
                    ItemStack at = getHoveredSlot(client);
                    if (at != null) {
                        screen.close();
                        ShopCommand.buyItem(client.player, ShopLogger.getItemId(at));
                    }
                }
            });
        });
    }


    private static @Nullable ItemStack getHoveredSlot(MinecraftClient client) {
        Screen currentScreen = client.currentScreen;

        if (!(currentScreen instanceof HandledScreen<?> handledScreen)) {
            return null;
        }

        int mouseX = (int) (client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth());
        int mouseY = (int) (client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight());

        Slot slot = handledScreen.getSlotAt(mouseX, mouseY);
        if (slot != null && slot.getStack() != null && !slot.getStack().isEmpty()) {
            return slot.getStack();
        } else {
            return null;
        }
    }
}
