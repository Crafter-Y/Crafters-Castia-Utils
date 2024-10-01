package de.craftery.castiautils.adblock;

import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.config.CastiaConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

public class Adblocker {

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;

            CastiaConfig config = AutoConfig.getConfigHolder(CastiaConfig.class).getConfig();

            if (handleChatMessage(message)) {
                return config.chatMessage;
            } else if (handleVoteMessage(message)) {
                return config.voteMessage;
            } else if (handleJoinMessage(message)) {
                return config.joinMessage;
            } else if (handleLeaveMessage(message)) {
                return config.leaveMessage;
            } else if (handleFirstTimeJoin(message)) {
                return config.firstJoinMessage;
            } else if (handleRankupMessage(message)) {
                return config.rankupMessage;
            } else if (handleRessourcePackMessage(message)) {
                return config.ressourcePackMessage;
            } else if (handleDeathMessage(message)) {
                return config.deathMessage;
            } else if (handleAfkMessage(message)) {
                return config.afkEnterMessage;
            } else if (handleAfkEndMessage(message)) {
                return config.afkEndMessage;
            } else if (handleShowcaseMessage(message)) {
                return config.showcaseMessage;
            } else if (handleEmptyLine(message)) {
                return config.emptyLines;
            } else if (handleTips(message)) {
                return config.tips;
            } else if (handleVoteStreakMessage(message)) {
                return config.voteStreakMessage;
            } else if (handleVoteReminderMessages(message)) {
                return config.voteReminderMessage;
            } else if (handleStoreAdvertisementMessages(message)) {
                return config.storeAdvertisements;
            } else if (handleFoundItemMessage(message)) {
                return config.playerFoundMessage;
            } else {
                CastiaUtils.LOGGER.info(message);
            }

            return true;
        });
    }

    private static boolean handleChatMessage(Text message) {
        if (message.getSiblings().size() != 6) return false;

        ClickEvent clickEvent = message.getStyle().getClickEvent();

        if (clickEvent == null) return false;
        if (clickEvent.getAction() != ClickEvent.Action.SUGGEST_COMMAND) return false;
        if (!clickEvent.getValue().startsWith("/msg")) return false;

        // TODO: do logging or stuff

        return true;
    }

    private static boolean handleVoteMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        if (message.getSiblings().get(0).getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getSiblings().get(0).getString().startsWith("has voted for cool rewards!");
    }

    private static boolean handleJoinMessage(Text message) {
        if (message.getSiblings().size() != 5) return false;
        if (!message.getSiblings().get(1).getString().equals("+")) return false;
        return message.getSiblings().get(4).getString().equals("joined!");
    }

    private static boolean handleLeaveMessage(Text message) {
        if (message.getSiblings().size() != 5) return false;
        if (!message.getSiblings().get(1).getString().equals("-")) return false;
        return message.getSiblings().get(4).getString().equals("left.");
    }

    private static boolean handleFirstTimeJoin(Text message) {
        if (message.getSiblings().size() != 3) return false;
        return message.getSiblings().get(1).getString().endsWith("for the first time! ");
    }

    private static boolean handleRankupMessage(Text message) {
        if (message.getSiblings().size() != 4) return false;
        return message.getSiblings().get(1).getString().endsWith("ranked up to ");
    }

    private static boolean handleRessourcePackMessage(Text message) {
        if (!message.getSiblings().isEmpty()) return false;
        return message.getString().equals("Resource pack successfully loaded.");
    }

    private static boolean handleDeathMessage(Text message) {
        // TODO: this could have false positives and could use more fingerprinting
        if (message.getSiblings().size() != 1) return false;
        if (message.getStyle().isItalic()) return false;
        return message.getSiblings().get(0).toString().startsWith("translation{");
    }

    private static boolean handleAfkMessage(Text message) {
        if (message.getSiblings().size() != 2) return false;
        return message.getSiblings().get(1).getString().equals("You are now AFK.");
    }

    private static boolean handleAfkEndMessage(Text message) {
        if (message.getSiblings().size() != 2) return false;
        return message.getSiblings().get(1).getString().equals("You are no longer AFK.");
    }

    private static boolean handleShowcaseMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        if (message.getSiblings().get(0).getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getSiblings().get(0).getString().startsWith("is showcasing ");
    }

    private static boolean handleEmptyLine(Text message) {
        if (!message.getSiblings().isEmpty()) return false;
        return message.getString().isEmpty();
    }

    private static boolean handleTips(Text message) {
        if (message.getSiblings().size() != 1) return false;
        if (message.getSiblings().get(0).getSiblings().size() != 3) return false;
        return message.getSiblings().get(0).getSiblings().get(0).getString().equals("Tip:");
    }

    private static boolean handleVoteStreakMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        if (message.getSiblings().get(0).getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getSiblings().get(0).getString().startsWith("has voted ");
    }

    private static boolean isChatImage(Text message) {
        if (!message.getSiblings().isEmpty()) return false;
        if (message.getString().length() != 1) return false;
        int charCode = message.getString().charAt(0);
        CastiaUtils.LOGGER.info(charCode);
        return switch (charCode) {
            case 57505: yield true; // Wumpus (hidden because message is broken otherwise
            default: yield false;
        };
    }

    private static boolean isNotVotedRecentlyMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("You have not voted recently:");
    }

    private static boolean isCurrentVoteMessage(Text message) {
        return message.getString().equals("                You currently have ");
    }

    private static boolean isPleaseVoteMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("/vote ");
    }

    private static boolean isVoteToHideThisMessageMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("Vote to hide this message...");
    }

    private static boolean handleVoteReminderMessages(Text message) {
        return isChatImage(message) ||
                isNotVotedRecentlyMessage(message) ||
                isCurrentVoteMessage(message) ||
                isPleaseVoteMessage(message) ||
                isVoteToHideThisMessageMessage(message);
    }

    private static boolean isShoutoutToMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("Shoutout to ");
    }

    private static boolean isTheyPurchasedMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("They purchased ");
    }

    private static boolean isYouCanSupportMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        return message.getSiblings().get(0).getString().equals("You can support CastiaMC by");
    }

    private static boolean isVisitShopMessage(Text message) {
        if (message.getSiblings().size() != 2) return false;
        return message.getSiblings().get(1).getString().equals("store.castiamc.com");
    }

    private static boolean handleStoreAdvertisementMessages(Text message) {
        return isChatImage(message) ||
                isShoutoutToMessage(message) ||
                isTheyPurchasedMessage(message) ||
                isYouCanSupportMessage(message) ||
                isVisitShopMessage(message);
    }

    private static boolean handleFoundItemMessage(Text message) {
        if (message.getSiblings().size() != 1) return false;
        if (message.getSiblings().get(0).getSiblings().size() != 2) return false;
        return message.getSiblings().get(0).getString().endsWith(" found a ");
    }
}
