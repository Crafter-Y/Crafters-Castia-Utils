package de.craftery.castiautils;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Messages {
    private static MutableText getPrefix() {
        MutableText prefix = Text.literal("[Castia Utils] ").formatted(Formatting.LIGHT_PURPLE);
        return Text.empty().append(prefix);
    }

    private static MutableText buildMessage(String translatable, String... arguments) {
        MutableText response = getPrefix();

        response.append(Text.translatable("castiautils." + translatable, (Object[]) arguments));
        return response;
    }

    public static void sendPlayerMessage(ClientPlayerEntity player, String translatable, String... arguments) {
        player.sendMessage(buildMessage(translatable, arguments));
    }

    public static void sendPlayerActionBar(ClientPlayerEntity player, String translatable, String... arguments) {
        player.sendMessage(buildMessage(translatable, arguments), true);
    }

    public static void sendCommandFeedback(CommandContext<FabricClientCommandSource> context, String translatable, String... arguments) {
        context.getSource().sendFeedback(buildMessage(translatable, arguments));
    }
}
