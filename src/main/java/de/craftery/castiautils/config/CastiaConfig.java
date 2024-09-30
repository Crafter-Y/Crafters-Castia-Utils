package de.craftery.castiautils.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "castiautils")
public class CastiaConfig implements ConfigData {

    @ConfigEntry.Category("adblock")
    @ConfigEntry.Gui.Tooltip
    public boolean voteMessage = false;

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
    public boolean ressourcePackMessage = false;

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
    @ConfigEntry.Gui.Tooltip
    public boolean emptyLines = false;

    @ConfigEntry.Category("adblock")
    public boolean chatMessage = true;
}
