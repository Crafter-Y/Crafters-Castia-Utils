package de.craftery.castiautils.config;

import de.craftery.castiautils.api.RefetchPeriod;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "castiautils")
public class CastiaConfig implements ConfigData {

    // Adblock Options --------------------------------------------------------------------->>>>
    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean voteMessage = false;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean voteReminderMessage = false;

    @ConfigEntry.Category("adblock")
    public boolean storeAdvertisements = false;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean emptyLines = false;

    @ConfigEntry.Category("adblock")
    public boolean joinMessage = true;

    @ConfigEntry.Category("adblock")
    public boolean leaveMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean firstJoinMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean rankupMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean resourcePackMessage = false;

    @ConfigEntry.Category("adblock")
    public boolean deathMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean afkEnterMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean afkEndMessage = true;

    @ConfigEntry.Category("adblock")
    public boolean showcaseMessage = true;

    @ConfigEntry.Category("adblock")
    public boolean tips = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean voteStreakMessage = false;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean playerFoundMessage = true;

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean gemstoneFoundMessage = true;

    @ConfigEntry.Category("adblock")
    public boolean chatMessage = true;

    // Chestshop Options --------------------------------------------------------------------->>>>
    @ConfigEntry.Category("chestshop")
    public boolean chestshopDataCollection = true;

    @ConfigEntry.Category("chestshop")
    @ConfigEntry.Gui.Tooltip
    public boolean enableContainerValue = true;

    @ConfigEntry.Category("chestshop")
    public boolean fallbackToBuyOnContainerValue = true;

    @ConfigEntry.Category("chestshop")
    public boolean hideEmptyInShopBuyCommand = false;

    @ConfigEntry.Category("chestshop")
    public boolean hideFullInShopSellCommand = false;

    // Tooltips --------------------------------------------------------------------->>>>

    @ConfigEntry.Category("tooltips")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTooltipInfo = true;

    @ConfigEntry.Category("tooltips")
    @ConfigEntry.Gui.Tooltip
    public boolean queryAdditionalTooltip = true;

    @ConfigEntry.Category("tooltips")
    @ConfigEntry.Gui.Tooltip
    public boolean displayComplexPriceMatrix = false;

    // Relic Price estimation --------------------------------------------------------------------->>>>
    @ConfigEntry.Category("relic")
    public boolean enableEstimateRelicPrices = true;

    @ConfigEntry.Category("relic")
    public boolean fallbackToRelicOnContainerValue = true;

    @ConfigEntry.Category("relic")
    @ConfigEntry.Gui.Tooltip
    public boolean playSoundOnMithrilFind = false;

    // API Options --------------------------------------------------------------------->>>>
    @ConfigEntry.Category("api")
    public boolean apiEnabled = true;

    @ConfigEntry.Category("api")
    @ConfigEntry.Gui.Tooltip
    public String apiUrl = "https://castiaapi.crafter-y.de/";

    @ConfigEntry.Category("api")
    @ConfigEntry.Gui.Tooltip
    public String token = "public";

    @ConfigEntry.Category("api")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public RefetchPeriod periodicallyRefetchData = RefetchPeriod.EVERY_10_MINUTES;

    // Developer Options --------------------------------------------------------------------->>>>
    @ConfigEntry.Category("dev")
    public boolean devMode = false;
}
